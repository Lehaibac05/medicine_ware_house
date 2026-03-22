import {
  Button,
  Form,
  Input,
  InputNumber,
  Select,
  Typography,
  message,
} from "antd";
import { useEffect, useMemo, useState } from "react";
import { getBatchesByMedicine, updateBatch } from "../../../services/batches";
import type { Batch } from "../../../services/types";

const { Text } = Typography;

type Props = {
  medicineId: number;
  warehouseId?: number;
  onSuccess?: () => void;
};

type FormValues = {
  batchId: number;
  newQuantity: number;
  reason: string;
};

export default function InventoryAdjustmentModal({
  medicineId,
  warehouseId,
  onSuccess,
}: Props) {
  const [form] = Form.useForm<FormValues>();
  const [messageApi, contextHolder] = message.useMessage();

  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [batches, setBatches] = useState<Batch[]>([]);

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        const data = await getBatchesByMedicine(medicineId);

        const filtered = warehouseId
          ? data.filter((b) => b.warehouse?.warehouseId === warehouseId)
          : data;

        setBatches(filtered);

        if (filtered.length > 0) {
          form.setFieldsValue({
            batchId: filtered[0].batchId,
            newQuantity: filtered[0].quantity,
          });
        }
      } catch {
        messageApi.error("Không thể tải danh sách lô");
      } finally {
        setLoading(false);
      }
    };

    void load();
  }, [medicineId, warehouseId, form, messageApi]);

  const batchId = Form.useWatch("batchId", form);

  const selectedBatch = useMemo(() => {
    return batches.find((batch) => batch.batchId === batchId) || null;
  }, [batches, batchId]);

  const onFinish = async (values: FormValues) => {
    try {
      setSaving(true);

      await updateBatch(medicineId, values.batchId, {
        quantity: values.newQuantity,
      });

      messageApi.success("Đã cập nhật số lượng tồn kho");
      onSuccess?.();
    } catch {
      messageApi.error("Cập nhật thất bại");
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      {contextHolder}

      <div className="space-y-4">
        <Text className="text-xs text-slate-400">
          Điều chỉnh tồn kho thủ công
        </Text>

        <Form form={form} layout="vertical" onFinish={onFinish}>
          <Form.Item
            label="Lô hàng"
            name="batchId"
            rules={[{ required: true, message: "Vui lòng chọn lô" }]}
          >
            <Select
              loading={loading}
              options={batches.map((b) => ({
                value: b.batchId,
                label: `${b.lotNumber} | SL: ${b.quantity} | HSD: ${
                  b.expiryDate || "-"
                }`,
              }))}
            />
          </Form.Item>

          <Form.Item label="Số lượng hiện tại">
            <InputNumber
              value={selectedBatch?.quantity ?? 0}
              disabled
              className="!w-full"
            />
          </Form.Item>

          <Form.Item
            label="Số lượng mới"
            name="newQuantity"
            rules={[
              { required: true, message: "Nhập số lượng mới" },
              { type: "number", min: 0, message: ">= 0" },
            ]}
          >
            <InputNumber min={0} className="!w-full" />
          </Form.Item>

          <Form.Item
            label="Lý do"
            name="reason"
            rules={[{ required: true, message: "Nhập lý do" }]}
          >
            <Input.TextArea rows={3} placeholder="Nhập lý do điều chỉnh..." />
          </Form.Item>

          <div className="flex justify-end">
            <Button type="primary" htmlType="submit" loading={saving}>
              Xác nhận
            </Button>
          </div>
        </Form>
      </div>
    </>
  );
}
