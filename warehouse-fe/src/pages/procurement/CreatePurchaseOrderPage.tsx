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
  Layout,
  Select,
  Space,
  Typography,
  Upload,
  type UploadProps,
  message,
} from "antd";
import dayjs from "dayjs";
import { useEffect, useMemo, useState } from "react";
import {
  Link,
  useNavigate,
  useParams,
  useSearchParams,
} from "react-router-dom";
import SidebarNav from "../../layouts/SidebarNav";
import TopBar from "../../layouts/TopBar";
import { getAllMedicines } from "../../services/medicines";
import {
  createPurchaseOrder,
  getPurchaseOrderById,
  type CreatePurchaseOrderPayload,
  updatePurchaseOrder,
} from "../../services/purchaseOrders";
import { getMedicineRequests } from "../../services/medicineRequests";
import { getActiveSuppliers, type Supplier } from "../../services/suppliers";
import type { Medicine, Warehouse } from "../../services/types";
import { getWarehouses } from "../../services/warehouses";

const { Content, Sider } = Layout;
const { Text } = Typography;

type FormValues = {
  supplierId: number;
  warehouseId: number;
  expectedDeliveryDate?: dayjs.Dayjs;
  notes?: string;
  items: Array<{
    medicineId: number;
    requestedQuantity: number;
    unitPrice: number;
    expectedExpiryDate?: dayjs.Dayjs;
    notes?: string;
  }>;
};

export default function CreatePurchaseOrderPage() {
  const [form] = Form.useForm<FormValues>();
  const navigate = useNavigate();
  const { id } = useParams();
  const [searchParams] = useSearchParams();
  const requestIdParam = Number(searchParams.get("requestId"));
  const editOrderId = Number(id);
  const isEditMode = !Number.isNaN(editOrderId);

  const [messageApi, contextHolder] = message.useMessage();
  const [submitting, setSubmitting] = useState(false);
  const [loadingPrefill, setLoadingPrefill] = useState(false);
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
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

  const parseExpiryDate = (rawValue: string) => {
    if (!rawValue) {
      return undefined;
    }

    const normalized = rawValue.trim();
    const direct = dayjs(normalized, "YYYY-MM-DD", true);
    if (direct.isValid()) {
      return direct;
    }

    const slash = dayjs(normalized, "DD/MM/YYYY", true);
    if (slash.isValid()) {
      return slash;
    }

    return null;
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
        firstCols.length >= 3 &&
        (firstCols[0].includes("medicine") ||
          firstCols[0].includes("thuoc") ||
          firstCols[1].includes("quantity") ||
          firstCols[2].includes("price"));

      const dataRows = hasHeader ? rows.slice(1) : rows;
      const importedItems: Array<{
        medicineId: number;
        requestedQuantity: number;
        unitPrice: number;
        expectedExpiryDate?: dayjs.Dayjs;
        notes?: string;
      }> = [];
      const errors: string[] = [];

      dataRows.forEach((row, idx) => {
        const cols = parseCsvLine(row);
        if (cols.length < 3) {
          errors.push(`Dòng ${idx + 1}: thiếu cột bắt buộc`);
          return;
        }

        const medicineId = parseMedicineId(cols[0], medicineByName);
        if (!medicineId) {
          errors.push(`Dòng ${idx + 1}: không tìm thấy thuốc '${cols[0]}'`);
          return;
        }

        const requestedQuantity = Number(cols[1]);
        if (!Number.isFinite(requestedQuantity) || requestedQuantity <= 0) {
          errors.push(`Dòng ${idx + 1}: số lượng không hợp lệ`);
          return;
        }

        const unitPrice = Number(cols[2]);
        if (!Number.isFinite(unitPrice) || unitPrice < 0) {
          errors.push(`Dòng ${idx + 1}: đơn giá không hợp lệ`);
          return;
        }

        const parsedExpiryDate = parseExpiryDate(cols[3] || "");
        if (parsedExpiryDate === null) {
          errors.push(
            `Dòng ${idx + 1}: hạn dùng không hợp lệ (định dạng YYYY-MM-DD hoặc DD/MM/YYYY)`,
          );
          return;
        }

        importedItems.push({
          medicineId,
          requestedQuantity,
          unitPrice,
          expectedExpiryDate: parsedExpiryDate || undefined,
          notes: cols[4] || undefined,
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

  const medicineNameById = useMemo(() => {
    return medicines.reduce<Record<number, string>>((acc, medicine) => {
      acc[medicine.medicineId] = medicine.name;
      return acc;
    }, {});
  }, [medicines]);

  useEffect(() => {
    const init = async () => {
      try {
        const [supplierData, medicineData, warehouseData] = await Promise.all([
          getActiveSuppliers(),
          getAllMedicines(),
          getWarehouses(),
        ]);

        setSuppliers(supplierData);
        setMedicines(medicineData);
        setWarehouses(warehouseData);

        if (isEditMode) {
          setLoadingPrefill(true);
          const order = await getPurchaseOrderById(editOrderId);

          if (order.status !== "PENDING") {
            messageApi.warning(
              "Chỉ có thể chỉnh sửa các đơn mua hàng đang chờ xử lý",
            );
            navigate(`/purchase-orders/${editOrderId}`);
            return;
          }

          form.setFieldsValue({
            supplierId: order.supplier?.supplierId,
            warehouseId: order.warehouse?.warehouseId,
            notes: order.notes,
            expectedDeliveryDate: order.expectedDeliveryDate
              ? dayjs(order.expectedDeliveryDate)
              : undefined,
            items: (order.items || []).map((item) => ({
              medicineId: item.medicine?.medicineId || 0,
              requestedQuantity: item.requestedQuantity || 1,
              unitPrice: item.unitPrice || 0,
              expectedExpiryDate: item.expectedExpiryDate
                ? dayjs(item.expectedExpiryDate)
                : undefined,
              notes: item.notes,
            })),
          });
          return;
        }

        if (!requestIdParam || Number.isNaN(requestIdParam)) {
          form.setFieldsValue({
            warehouseId: warehouseData[0]?.warehouseId,
            items: [
              {
                medicineId: medicineData[0]?.medicineId,
                requestedQuantity: 1,
                unitPrice: 0,
              },
            ],
          });
          return;
        }

        setLoadingPrefill(true);
        const requests = await getMedicineRequests();
        const sourceRequest = requests.find(
          (request) => request.requestId === requestIdParam,
        );

        if (!sourceRequest) {
          messageApi.warning(
            "Không tìm thấy yêu cầu. Vui lòng tạo đơn mua hàng thủ công",
          );
          return;
        }

        form.setFieldsValue({
          warehouseId: sourceRequest.warehouseId,
          notes: sourceRequest.notes,
          expectedDeliveryDate: sourceRequest.requiredDate
            ? dayjs(sourceRequest.requiredDate)
            : undefined,
          items: sourceRequest.items.map((item) => ({
            medicineId: item.medicineId,
            requestedQuantity: item.quantity,
            unitPrice: 0,
            notes: item.notes,
          })),
        });
      } catch {
        messageApi.error("Lỗi khi tải dữ liệu");
      } finally {
        setLoadingPrefill(false);
      }
    };

    void init();
  }, [editOrderId, form, isEditMode, messageApi, navigate, requestIdParam]);

  const onFinish = async (values: FormValues) => {
    try {
      setSubmitting(true);

      const payload: CreatePurchaseOrderPayload = {
        supplierId: values.supplierId,
        warehouseId: values.warehouseId,
        expectedDeliveryDate: values.expectedDeliveryDate
          ? values.expectedDeliveryDate.format("YYYY-MM-DD")
          : undefined,
        notes: values.notes,
        items: values.items.map((item) => ({
          medicineId: item.medicineId,
          requestedQuantity: item.requestedQuantity,
          unitPrice: item.unitPrice,
          expectedExpiryDate: item.expectedExpiryDate
            ? item.expectedExpiryDate.format("YYYY-MM-DD")
            : undefined,
          notes: item.notes,
        })),
      };
      if (isEditMode) {
        const updated = await updatePurchaseOrder(editOrderId, payload);
        messageApi.success("Cập nhật đơn mua hàng thành công");
        navigate(`/purchase-orders/${updated.purchaseOrderId}`);
      } else {
        const created = await createPurchaseOrder(payload);
        messageApi.success("Tạo đơn mua hàng thành công");
        navigate(`/purchase-orders/${created.purchaseOrderId}`);
      }
    } catch {
      messageApi.error(
        isEditMode
          ? "Cập nhật đơn mua hàng thất bại"
          : "Tạo đơn mua hàng thất bại",
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Layout className="min-h-screen bg-slate-100">
      {contextHolder}
      <Sider
        width={260}
        className="hidden lg:block !bg-white border-r border-slate-200 px-4 py-6 !fixed left-0 top-0 h-screen"
      >
        <SidebarNav />
      </Sider>

      <Layout className="lg:ml-[260px]">
        <div className="fixed left-0 top-0 z-20 w-full lg:pl-[260px]">
          <TopBar />
        </div>

        <Content className="p-6 pt-[114px]">
          <div className="rounded-2xl bg-white p-6 shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
            <Form<FormValues> form={form} layout="vertical" onFinish={onFinish}>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                <Form.Item
                  label="Nhà cung cấp"
                  name="supplierId"
                  rules={[{ required: true }]}
                >
                  <Select
                    showSearch
                    optionFilterProp="label"
                    options={suppliers.map((supplier) => ({
                      value: supplier.supplierId,
                      label: supplier.supplierName,
                    }))}
                  />
                </Form.Item>

                <Form.Item
                  label="Kho"
                  name="warehouseId"
                  rules={[{ required: true }]}
                >
                  <Select
                    options={warehouses.map((warehouse) => ({
                      value: warehouse.warehouseId,
                      label:
                        warehouse.name || `Warehouse ${warehouse.warehouseId}`,
                    }))}
                  />
                </Form.Item>

                <Form.Item
                  label="Ngày giao dự kiến"
                  name="expectedDeliveryDate"
                >
                  <DatePicker className="w-full" />
                </Form.Item>
              </div>

              <Form.Item label="Ghi chú" name="notes">
                <Input.TextArea rows={3} placeholder="PO notes" />
              </Form.Item>

              <Space className="mb-2 w-full justify-between">
                <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
                  Danh sách sản phẩm trong đơn mua hàng
                </Text>
                {requestIdParam ? (
                  <Link to="/medicine-requests">
                    <Button size="small">Quay lại yêu cầu</Button>
                  </Link>
                ) : null}
              </Space>

              <Form.List
                name="items"
                rules={[
                  {
                    validator: async (_, value) => {
                      if (!value || value.length < 1) {
                        throw new Error("Phải có ít nhất 1 sản phẩm");
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
                        className="grid grid-cols-1 gap-3 rounded-xl border border-slate-200 p-3 md:grid-cols-[2fr_1fr_1fr_1fr_2fr_auto]"
                      >
                        <Form.Item
                          label="Thuốc"
                          name={[field.name, "medicineId"]}
                          rules={[{ required: true }]}
                          className="!mb-0"
                        >
                          <Select
                            showSearch
                            optionFilterProp="label"
                            options={medicines.map((medicine) => ({
                              value: medicine.medicineId,
                              label: medicine.name,
                            }))}
                          />
                        </Form.Item>

                        <Form.Item
                          label="Số lượng"
                          name={[field.name, "requestedQuantity"]}
                          rules={[{ required: true }]}
                          className="!mb-0"
                        >
                          <InputNumber min={1} className="w-full" />
                        </Form.Item>

                        <Form.Item
                          label="Đơn giá"
                          name={[field.name, "unitPrice"]}
                          rules={[{ required: true }]}
                          className="!mb-0"
                        >
                          <InputNumber min={0} className="w-full" />
                        </Form.Item>

                        <Form.Item
                          label="Hạn sử dụng dự kiến"
                          name={[field.name, "expectedExpiryDate"]}
                          className="!mb-0"
                        >
                          <DatePicker className="w-full" />
                        </Form.Item>

                        <Form.Item
                          label="Ghi chú"
                          name={[field.name, "notes"]}
                          className="!mb-0"
                        >
                          <Input
                            placeholder={
                              form.getFieldValue([
                                "items",
                                field.name,
                                "medicineId",
                              ])
                                ? medicineNameById[
                                    form.getFieldValue([
                                      "items",
                                      field.name,
                                      "medicineId",
                                    ])
                                  ]
                                : "Optional"
                            }
                          />
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
                        onClick={() =>
                          add({
                            medicineId: medicines[0]?.medicineId,
                            requestedQuantity: 1,
                            unitPrice: 0,
                          })
                        }
                      >
                        Thêm sản phẩm
                      </Button>
                    </div>
                  </div>
                )}
              </Form.List>

              <div className="mt-6 flex items-center justify-end gap-2">
                <Button onClick={() => navigate(-1)}>Cancel</Button>
                <Button
                  type="primary"
                  htmlType="submit"
                  loading={submitting || loadingPrefill}
                >
                  {isEditMode ? "Lưu thay đổi" : "Tạo đơn mua hàng"}
                </Button>
              </div>
            </Form>
          </div>
        </Content>
      </Layout>
    </Layout>
  );
}
