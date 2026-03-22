import {
  Modal,
  Form,
  Select,
  DatePicker,
  Input,
  InputNumber,
  Button,
  Typography,
  message,
} from "antd";
import { DeleteOutlined, PlusOutlined } from "@ant-design/icons";
import { useEffect, useMemo, useState } from "react";
import dayjs from "dayjs";
import {
  createPurchaseOrder,
  updatePurchaseOrder,
  getPurchaseOrderById,
} from "../../../services/purchaseOrders";
import { getActiveSuppliers } from "../../../services/suppliers";
import { getAllMedicines } from "../../../services/medicines";
import { getWarehouses } from "../../../services/warehouses";

const { Text } = Typography;

export default function CreatePurchaseOrderModal({
  open,
  onClose,
  orderId,
  requestData,
  onSuccess,
}: {
  open: boolean;
  onClose: () => void;
  orderId?: number;
  requestData?: any;
  onSuccess?: () => void;
}) {
  const [form] = Form.useForm();
  const [suppliers, setSuppliers] = useState<any[]>([]);
  const [medicines, setMedicines] = useState<any[]>([]);
  const [warehouses, setWarehouses] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [messageApi, contextHolder] = message.useMessage();

  const isEditMode = !!orderId;

  const medicineNameById = useMemo(() => {
    return medicines.reduce((acc, m) => {
      acc[m.medicineId] = m.name;
      return acc;
    }, {} as Record<number, string>);
  }, [medicines]);

  useEffect(() => {
    if (!open) return;

    const load = async () => {
      const [sup, med, wh] = await Promise.all([
        getActiveSuppliers(),
        getAllMedicines(),
        getWarehouses(),
      ]);

      setSuppliers(sup);
      setMedicines(med);
      setWarehouses(wh);

      // EDIT MODE
      if (orderId) {
        const order = await getPurchaseOrderById(orderId);

        form.setFieldsValue({
          supplierId: order.supplier?.supplierId,
          warehouseId: order.warehouse?.warehouseId,
          expectedDeliveryDate: order.expectedDeliveryDate
            ? dayjs(order.expectedDeliveryDate)
            : undefined,
          notes: order.notes,
          items: order.items.map((i: any) => ({
            medicineId: i.medicine?.medicineId,
            requestedQuantity: i.requestedQuantity,
            unitPrice: i.unitPrice,
            expectedExpiryDate: i.expectedExpiryDate
              ? dayjs(i.expectedExpiryDate)
              : undefined,
            notes: i.notes,
          })),
        });

        return;
      }

      // CREATE FROM REQUEST
      if (requestData) {
        form.setFieldsValue({
          warehouseId: requestData.warehouseId,
          notes: requestData.notes,
          expectedDeliveryDate: requestData.requiredDate
            ? dayjs(requestData.requiredDate)
            : undefined,
          items: requestData.items.map((i: any) => ({
            medicineId: i.medicineId,
            requestedQuantity: i.quantity,
            unitPrice: 0,
          })),
        });
        return;
      }

      // DEFAULT
      form.setFieldsValue({
        warehouseId: wh[0]?.warehouseId,
        items: [{ requestedQuantity: 1, unitPrice: 0 }],
      });
    };

    load();
  }, [open]);

  const onFinish = async (values: any) => {
    try {
      setLoading(true);

      const payload = {
        ...values,
        expectedDeliveryDate: values.expectedDeliveryDate
          ? values.expectedDeliveryDate.format("YYYY-MM-DD")
          : undefined,
        items: values.items.map((i: any) => ({
          ...i,
          expectedExpiryDate: i.expectedExpiryDate
            ? i.expectedExpiryDate.format("YYYY-MM-DD")
            : undefined,
        })),
      };

      if (isEditMode) {
        await updatePurchaseOrder(orderId!, payload);
        messageApi.success("Cập nhật thành công");
      } else {
        await createPurchaseOrder(payload);
        messageApi.success("Tạo đơn thành công");
      }

      onSuccess?.();
      onClose();
      form.resetFields();
    } catch {
      messageApi.error("Thao tác thất bại");
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
        width={1000}
      >
        <div className="rounded-2xl bg-white p-6 shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
          <Form form={form} layout="vertical" onFinish={onFinish}>
            {/* TOP */}
            <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
              <Form.Item name="supplierId" label="Nhà cung cấp" rules={[{ required: true }]}>
                <Select
                  showSearch
                  options={suppliers.map((s) => ({
                    value: s.supplierId,
                    label: s.supplierName,
                  }))}
                />
              </Form.Item>

              <Form.Item name="warehouseId" label="Kho" rules={[{ required: true }]}>
                <Select
                  options={warehouses.map((w) => ({
                    value: w.warehouseId,
                    label: w.name,
                  }))}
                />
              </Form.Item>

              <Form.Item name="expectedDeliveryDate" label="Ngày giao dự kiến">
                <DatePicker className="w-full" />
              </Form.Item>
            </div>

            <Form.Item name="notes" label="Ghi chú">
              <Input.TextArea rows={3} />
            </Form.Item>

            <Text className="text-xs text-slate-400">
              Danh sách sản phẩm
            </Text>

            <Form.List name="items">
              {(fields, { add, remove }) => (
                <div className="space-y-3">
                  {fields.map((field) => (
                    <div
                      key={field.key}
                      className="grid grid-cols-1 gap-3 rounded-xl border p-4 md:grid-cols-[2fr_1fr_1fr_1fr_2fr_auto]"
                    >
                      <Form.Item name={[field.name, "medicineId"]} rules={[{ required: true }]} className="!mb-0">
                        <Select
                          options={medicines.map((m) => ({
                            value: m.medicineId,
                            label: m.name,
                          }))}
                          showSearch
                        />
                      </Form.Item>

                      <Form.Item name={[field.name, "requestedQuantity"]} rules={[{ required: true }]} className="!mb-0">
                        <InputNumber min={1} className="w-full" />
                      </Form.Item>

                      <Form.Item name={[field.name, "unitPrice"]} rules={[{ required: true }]} className="!mb-0">
                        <InputNumber min={0} className="w-full" />
                      </Form.Item>

                      <Form.Item name={[field.name, "expectedExpiryDate"]} className="!mb-0">
                        <DatePicker className="w-full" />
                      </Form.Item>

                      <Form.Item name={[field.name, "notes"]} className="!mb-0">
                        <Input placeholder="Optional" />
                      </Form.Item>

                      <div className="flex items-end">
                        <Button danger icon={<DeleteOutlined />} onClick={() => remove(field.name)} />
                      </div>
                    </div>
                  ))}

                  <Button icon={<PlusOutlined />} onClick={() => add()}>
                    Thêm sản phẩm
                  </Button>
                </div>
              )}
            </Form.List>

            <div className="mt-6 flex justify-end gap-2">
              <Button onClick={onClose}>Cancel</Button>
              <Button type="primary" htmlType="submit" loading={loading}>
                {isEditMode ? "Lưu" : "Tạo đơn"}
              </Button>
            </div>
          </Form>
        </div>
      </Modal>
    </>
  );
}