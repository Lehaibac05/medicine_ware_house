import {
    Modal,
    Form,
    Select,
    DatePicker,
    Input,
    InputNumber,
    Button,
    Upload,
    type UploadProps,
    message,
} from "antd";
import { DeleteOutlined, PlusOutlined, UploadOutlined } from "@ant-design/icons";
import { useEffect, useState } from "react";
import { getAllMedicines } from "../../../services/medicines";
import { getWarehouses } from "../../../services/warehouses";
import { createMedicineRequest } from "../../../services/medicineRequests";

export default function CreateMedicineRequestModal({
    open,
    onClose,
    onSuccess,
}: {
    open: boolean;
    onClose: () => void;
    onSuccess?: () => void;
}) {
    const [form] = Form.useForm();
    const [medicines, setMedicines] = useState<any[]>([]);
    const [warehouses, setWarehouses] = useState<any[]>([]);
    const [_loading, setLoading] = useState(false);
    const [messageApi, contextHolder] = message.useMessage();

    const normalize = (value: string) => value.trim().toLowerCase();

    const parseCsvLine = (line: string): string[] => {
        const cols: string[] = [];
        let current = "";
        let inQuotes = false;

        for (let i = 0; i < line.length; i += 1) {
            const char = line[i];
            if (char === '"') {
                if (inQuotes && line[i + 1] === '"') {
                    current += '"';
                    i += 1;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }

            if (char === "," && !inQuotes) {
                cols.push(current.trim());
                current = "";
                continue;
            }

            current += char;
        }

        cols.push(current.trim());
        return cols;
    };

    const importCsv: UploadProps["beforeUpload"] = async (file) => {
        try {
            if (!medicines.length) {
                messageApi.error("Chưa tải xong danh mục thuốc");
                return Upload.LIST_IGNORE;
            }

            const content = await file.text();
            const rows = content
                .replace(/^\uFEFF/, "")
                .split(/\r?\n/)
                .map((line) => line.trim())
                .filter((line) => line.length > 0);

            if (!rows.length) {
                messageApi.error("File CSV rỗng");
                return Upload.LIST_IGNORE;
            }

            const medicineByName = new Map<string, any>(
                medicines.map((medicine) => [normalize(medicine.name || ""), medicine]),
            );

            const firstCols = parseCsvLine(rows[0]).map((col) => normalize(col));
            const hasHeader =
                firstCols.length >= 2 &&
                (firstCols[0].includes("medicine") ||
                    firstCols[0].includes("thuoc") ||
                    firstCols[1].includes("quantity") ||
                    firstCols[1].includes("so luong"));

            const dataRows = hasHeader ? rows.slice(1) : rows;
            const importedItems: Array<{
                medicineId: number;
                quantity: number;
                notes?: string;
            }> = [];
            const errors: string[] = [];

            dataRows.forEach((row, idx) => {
                const cols = parseCsvLine(row);
                if (cols.length < 2) {
                    errors.push(`Dòng ${idx + 1}: thiếu cột bắt buộc`);
                    return;
                }

                const rawMedicine = cols[0];
                const asNumber = Number(rawMedicine);
                let medicineId: number | undefined;

                if (Number.isFinite(asNumber) && asNumber > 0) {
                    const byId = medicines.find((medicine) => medicine.medicineId === asNumber);
                    medicineId = byId?.medicineId;
                } else {
                    medicineId = medicineByName.get(normalize(rawMedicine))?.medicineId;
                }

                if (!medicineId) {
                    errors.push(`Dòng ${idx + 1}: không tìm thấy thuốc '${rawMedicine}'`);
                    return;
                }

                const quantity = Number(cols[1]);
                if (!Number.isFinite(quantity) || quantity <= 0) {
                    errors.push(`Dòng ${idx + 1}: số lượng không hợp lệ`);
                    return;
                }

                importedItems.push({
                    medicineId,
                    quantity,
                    notes: cols[2] || undefined,
                });
            });

            if (!importedItems.length) {
                messageApi.error(errors[0] || "Không có dữ liệu hợp lệ để import");
                return Upload.LIST_IGNORE;
            }

            form.setFieldValue("items", importedItems);
            if (errors.length) {
                messageApi.warning(
                    `Đã import ${importedItems.length} dòng hợp lệ, bỏ qua ${errors.length} dòng lỗi`,
                );
            } else {
                messageApi.success(`Đã import ${importedItems.length} dòng từ CSV`);
            }
        } catch {
            messageApi.error("Đọc file CSV thất bại");
        }

        return Upload.LIST_IGNORE;
    };

    useEffect(() => {
        if (!open) return;

        const load = async () => {
            const [med, wh] = await Promise.all([
                getAllMedicines(),
                getWarehouses(),
            ]);
            setMedicines(med);
            setWarehouses(wh);

            form.setFieldsValue({
                warehouseId: wh[0]?.warehouseId,
                items: [{ quantity: 1 }],
            });
        };

        load();
    }, [open]);

    const onFinish = async (values: any) => {
        try {
            setLoading(true);

            await createMedicineRequest({
                ...values,
                requiredDate: values.requiredDate
                    ? values.requiredDate.format("YYYY-MM-DD")
                    : undefined,
            });

            messageApi.success("Tạo yêu cầu thành công");

            onSuccess?.();
            onClose();
            form.resetFields();
        } catch {
            messageApi.error("Tạo thất bại");
        } finally {
            setLoading(false);
        }
    };

    return (
        <>
            {contextHolder}
            <Modal
                title={null}
                open={open}
                onCancel={onClose}
                footer={null}
                width={900}
            >

                <Form form={form} layout="vertical" onFinish={onFinish}>

                    {/* TOP */}
                    <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                        <Form.Item
                            label={<span className="text-sm font-medium">Kho</span>}
                            name="warehouseId"
                            rules={[{ required: true }]}
                        >
                            <Select
                                options={warehouses.map((w) => ({
                                    value: w.warehouseId,
                                    label: w.name,
                                }))}
                            />
                        </Form.Item>

                        <Form.Item label="Ngày giao hàng" name="requiredDate">
                            <DatePicker className="w-full" />
                        </Form.Item>
                    </div>

                    {/* NOTE */}
                    <Form.Item label="Ghi chú" name="notes">
                        <Input.TextArea
                            rows={3}
                            placeholder="Request notes"
                            className="rounded-lg"
                        />
                    </Form.Item>

                    {/* TITLE */}
                    <div className="mb-2 text-[11px] uppercase tracking-[0.12em] text-slate-400">
                        Danh sách thuốc yêu cầu
                    </div>

                    {/* LIST */}
                    <Form.List name="items">
                        {(fields, { add, remove }) => (
                            <div className="space-y-3">
                                {fields.map((field) => (
                                    <div
                                        key={field.key}
                                        className="grid grid-cols-1 gap-3 rounded-xl border border-slate-200 p-4 md:grid-cols-[2fr_1fr_2fr_auto]"
                                    >
                                        {/* Medicine */}
                                        <Form.Item
                                            label="Thuốc"
                                            name={[field.name, "medicineId"]}
                                            rules={[{ required: true }]}
                                            className="!mb-0"
                                        >
                                            <Select
                                                options={medicines.map((m) => ({
                                                    value: m.medicineId,
                                                    label: m.name,
                                                }))}
                                                showSearch
                                                optionFilterProp="label"
                                            />
                                        </Form.Item>

                                        {/* Quantity */}
                                        <Form.Item
                                            label="Số lượng"
                                            name={[field.name, "quantity"]}
                                            rules={[{ required: true }]}
                                            className="!mb-0"
                                        >
                                            <InputNumber min={1} className="w-full" />
                                        </Form.Item>

                                        {/* Notes */}
                                        <Form.Item
                                            label="Ghi chú sản phẩm"
                                            name={[field.name, "notes"]}
                                            className="!mb-0"
                                        >
                                            <Input placeholder="Optional" />
                                        </Form.Item>

                                        {/* Delete */}
                                        <div className="flex items-end">
                                            <Button
                                                danger
                                                className="h-[40px] w-[40px] flex items-center justify-center"
                                                onClick={() => remove(field.name)}
                                            >
                                                <DeleteOutlined />
                                            </Button>
                                        </div>
                                    </div>
                                ))}

                                <div className="flex flex-wrap gap-2">
                                    <Upload
                                        accept=".csv,text/csv"
                                        showUploadList={false}
                                        beforeUpload={importCsv}
                                    >
                                        <Button className="rounded-lg" icon={<UploadOutlined />}>
                                            Import CSV
                                        </Button>
                                    </Upload>
                                    <Button
                                        type="dashed"
                                        className="rounded-lg"
                                        onClick={() => add({ quantity: 1 })}
                                    >
                                        <PlusOutlined /> Thêm thuốc
                                    </Button>
                                </div>
                            </div>
                        )}
                    </Form.List>

                    {/* ACTIONS */}
                    <div className="mt-6 flex justify-end gap-2">
                        <Button onClick={onClose}>Cancel</Button>
                        <Button type="primary" htmlType="submit">
                            Gửi yêu cầu
                        </Button>
                    </div>
                </Form>
            </Modal>
        </>
    );
}