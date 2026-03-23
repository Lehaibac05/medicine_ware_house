import {
    Modal,
    Form,
    Select,
    DatePicker,
    Input,
    InputNumber,
    Button,
    message,
} from "antd";
import { DeleteOutlined, PlusOutlined } from "@ant-design/icons";
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

                                {/* Add button */}
                                <Button
                                    type="dashed"
                                    className="rounded-lg"
                                    onClick={() => add({ quantity: 1 })}
                                >
                                    <PlusOutlined /> Thêm thuốc
                                </Button>
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