import {
  DeleteOutlined,
  PlusOutlined,
  UploadOutlined,
} from "@ant-design/icons";
import {
  Button,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Select,
  Typography,
  Upload,
  type UploadProps,
  message,
} from "antd";
import dayjs from "dayjs";
import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { getAllMedicines } from "../../services/medicines";
import { createMedicineRequest } from "../../services/medicineRequests";
import type { Medicine, Warehouse } from "../../services/types";
import { getWarehouses } from "../../services/warehouses";
import MainLayout from "../../layouts/MainLayout";

const { Text } = Typography;

type FormValues = {
  warehouseId: number;
  requiredDate?: dayjs.Dayjs;
  notes?: string;
  items: Array<{
    medicineId: number;
    quantity: number;
    notes?: string;
  }>;
};

export default function CreateMedicineRequestPage() {
  const [form] = Form.useForm<FormValues>();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [messageApi, contextHolder] = message.useMessage();
  const [submitting, setSubmitting] = useState(false);
  const [medicines, setMedicines] = useState<Medicine[]>([]);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);

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

  const parseMedicineId = (
    rawValue: string,
    medicineByName: Map<string, Medicine>,
  ) => {
    const asNumber = Number(rawValue);
    if (Number.isFinite(asNumber) && asNumber > 0) {
      const byId = medicines.find((medicine) => medicine.medicineId === asNumber);
      if (byId) {
        return byId.medicineId;
      }
    }

    const byName = medicineByName.get(normalize(rawValue));
    return byName?.medicineId;
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

      const medicineByName = new Map<string, Medicine>(
        medicines.map((medicine) => [normalize(medicine.name), medicine]),
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

        const medicineId = parseMedicineId(cols[0], medicineByName);
        if (!medicineId) {
          errors.push(`Dòng ${idx + 1}: không tìm thấy thuốc '${cols[0]}'`);
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
    const load = async () => {
      const [medData, whData] = await Promise.all([
        getAllMedicines(),
        getWarehouses(),
      ]);
      setMedicines(medData);
      setWarehouses(whData);

      const prefillMedicineId = Number(searchParams.get("medicineId"));
      const prefillWarehouseId = Number(searchParams.get("warehouseId"));
      const suggestedQty = Number(searchParams.get("suggestedQty"));
      const fromAlertId = searchParams.get("fromAlert");

      form.setFieldsValue({
        warehouseId:
          Number.isFinite(prefillWarehouseId) && prefillWarehouseId > 0
            ? prefillWarehouseId
            : whData[0]?.warehouseId,
        notes: fromAlertId ? `Created from alert #${fromAlertId}` : undefined,
        items: [
          {
            medicineId:
              Number.isFinite(prefillMedicineId) && prefillMedicineId > 0
                ? prefillMedicineId
                : medData[0]?.medicineId,
            quantity:
              Number.isFinite(suggestedQty) && suggestedQty > 0
                ? suggestedQty
                : 1,
          },
        ],
      });
    };

    void load();
  }, [form, searchParams]);

  const onFinish = async (values: FormValues) => {
    try {
      setSubmitting(true);
      await createMedicineRequest({
        warehouseId: values.warehouseId,
        requiredDate: values.requiredDate
          ? values.requiredDate.format("YYYY-MM-DD")
          : undefined,
        notes: values.notes,
        items: values.items.map((item) => ({
          medicineId: item.medicineId,
          quantity: item.quantity,
          notes: item.notes,
        })),
      });
      messageApi.success("Medicine request created");
      navigate("/requests");
    } catch {
      messageApi.error("Failed to create medicine request");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <MainLayout>
      {contextHolder}
      <div className="rounded-2xl bg-white p-6 shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
        <Form<FormValues> form={form} layout="vertical" onFinish={onFinish}>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <Form.Item
              label="Kho"
              name="warehouseId"
              rules={[{ required: true }]}
            >
              <Select
                options={warehouses.map((warehouse) => ({
                  value: warehouse.warehouseId,
                  label: warehouse.name || `Kho ${warehouse.warehouseId}`,
                }))}
              />
            </Form.Item>

            <Form.Item label="Ngày giao hàng" name="requiredDate">
              <DatePicker className="w-full" />
            </Form.Item>
          </div>

          <Form.Item label="Ghi chú" name="notes">
            <Input.TextArea rows={3} placeholder="Request notes" />
          </Form.Item>

          <Text className="mb-2 block text-[11px] uppercase tracking-[0.12em] text-slate-400">
            Danh sách thuốc yêu cầu
          </Text>

          <Form.List
            name="items"
            rules={[
              {
                validator: async (_, value) => {
                  if (!value || value.length < 1) {
                    throw new Error("Vui lòng chọn ít nhất một thuốc");
                  }
                },
              },
            ]}
          >
            {(fields, { add, remove }) => (
              <div className="space-y-3">
                {fields.map((field) => (
                  <div
                    key={field.key}
                    className="grid grid-cols-1 gap-3 rounded-xl border border-slate-200 p-3 md:grid-cols-[2fr_1fr_2fr_auto]"
                  >
                    <Form.Item
                      label="Thuốc"
                      name={[field.name, "medicineId"]}
                      rules={[{ required: true }]}
                      className="!mb-0"
                    >
                      <Select
                        options={medicines.map((medicine) => ({
                          value: medicine.medicineId,
                          label: medicine.name,
                        }))}
                        showSearch
                        optionFilterProp="label"
                      />
                    </Form.Item>

                    <Form.Item
                      label="Số lượng"
                      name={[field.name, "quantity"]}
                      rules={[{ required: true }]}
                      className="!mb-0"
                    >
                      <InputNumber min={1} className="w-full" />
                    </Form.Item>

                    <Form.Item
                      label="Ghi chú sản phẩm"
                      name={[field.name, "notes"]}
                      className="!mb-0"
                    >
                      <Input placeholder="Optional" />
                    </Form.Item>

                    <div className="flex items-end">
                      <Button
                        danger
                        icon={<DeleteOutlined />}
                        onClick={() => remove(field.name)}
                      />
                    </div>
                  </div>
                ))}

                <div className="flex flex-wrap gap-2">
                  <Upload
                    accept=".csv,text/csv"
                    showUploadList={false}
                    beforeUpload={importCsv}
                  >
                    <Button icon={<UploadOutlined />}>Import CSV</Button>
                  </Upload>
                  <Button
                    type="dashed"
                    icon={<PlusOutlined />}
                    onClick={() => add({ quantity: 1 })}
                  >
                    Thêm thuốc
                  </Button>
                </div>
              </div>
            )}
          </Form.List>

          <div className="mt-6 flex justify-end gap-2">
            <Button onClick={() => navigate(-1)}>Cancel</Button>
            <Button type="primary" htmlType="submit" loading={submitting}>
              Gửi yêu cầu
            </Button>
          </div>
        </Form>
      </div>
    </MainLayout>
  );
}
