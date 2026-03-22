import { DatePicker, Form, Input, InputNumber, Select } from "antd";
import type { Dayjs } from "dayjs";
import { useEffect, useMemo } from "react";
import BaseModal from "../../../components/base/BaseModal";
import type { Medicine, Warehouse } from "../../../services/types";

export type BatchFormValues = {
  medicineId: number;
  lotNumber: string;
  manufactureDate: Dayjs;
  expiryDate: Dayjs;
  quantity: number;
  warehouseId: number;
};

type BatchFormModalProps = {
  open: boolean;
  mode: "create" | "edit";
  loading?: boolean;
  medicines: Medicine[];
  warehouses: Warehouse[];
  initialValues?: Partial<BatchFormValues>;
  onCancel: () => void;
  onSubmit: (values: BatchFormValues) => Promise<void> | void;
};

const defaultValues: Partial<BatchFormValues> = {
  medicineId: undefined,
  lotNumber: "",
  manufactureDate: undefined,
  expiryDate: undefined,
  quantity: undefined,
  warehouseId: undefined,
};

function BatchFormModal({
  open,
  mode,
  loading = false,
  medicines,
  warehouses,
  initialValues,
  onCancel,
  onSubmit,
}: BatchFormModalProps) {
  const [form] = Form.useForm<BatchFormValues>();

  const medicineOptions = useMemo(
    () =>
      medicines.map((medicine) => ({
        value: medicine.medicineId,
        label: medicine.name,
      })),
    [medicines]
  );

  const warehouseOptions = useMemo(
    () =>
      warehouses.map((warehouse) => ({
        value: warehouse.warehouseId,
        label: warehouse.name,
      })),
    [warehouses]
  );

  useEffect(() => {
    if (!open) return;
    form.setFieldsValue({
      ...defaultValues,
      ...initialValues,
    });
  }, [form, initialValues, open]);

  const handleOk = async () => {
    const values = await form.validateFields();
    await onSubmit(values);
  };

  return (
    <BaseModal
      open={open}
      title={mode === "create" ? "Tạo lô thuốc" : "Sửa lô thuốc"}
      onCancel={onCancel}
      onOk={handleOk}
      okText={mode === "create" ? "Tạo" : "Cập nhật"}
      confirmLoading={loading}
      width={720}
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="medicineId"
          label="Thuốc"
          rules={[{ required: true, message: "Vui lòng chọn thuốc." }]}
        >
          <Select
            placeholder="Chọn thuốc"
            options={medicineOptions}
            disabled={mode === "edit"}
          />
        </Form.Item>

        <Form.Item
          name="lotNumber"
          label="Số lô thuốc"
          rules={[
            { required: true, message: "Vui lòng nhập số lô thuốc." },
            { max: 255, message: "Số lô thuốc quá dài." },
          ]}
        >
          <Input placeholder="e.g. PCM-0423" />
        </Form.Item>

        <Form.Item
          name="manufactureDate"
          label="Ngày sản xuất"
          rules={[{ required: true, message: "Vui lòng chọn ngày sản xuất" }]}
        >
          <DatePicker className="w-full" format="DD/MM/YYYY" />
        </Form.Item>

        <Form.Item
          name="expiryDate"
          label="Hạn sử dụng"
          rules={[{ required: true, message: "Vui lòng chọn ngày hết hạn" }]}
        >
          <DatePicker className="w-full" format="DD/MM/YYYY" />
        </Form.Item>

        <Form.Item
          name="quantity"
          label="Số lượng"
          rules={[{ required: true, message: "Vui lòng nhập số lượng" }]}
        >
          <InputNumber
            className="!w-full"
            min={0}
            precision={0}
            placeholder="Nhập số lượng..."
            disabled={mode === "edit"}
          />
        </Form.Item>

        <Form.Item
          name="warehouseId"
          label="Kho"
          rules={[{ required: true, message: "Vui lòng chọn kho." }]}
        >
          <Select placeholder="Chọn kho thuốc" options={warehouseOptions} />
        </Form.Item>
      </Form>
    </BaseModal>
  );
}

export default BatchFormModal;
