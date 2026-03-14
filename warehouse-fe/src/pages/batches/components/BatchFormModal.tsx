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
      title={mode === "create" ? "Create Batch" : "Edit Batch"}
      onCancel={onCancel}
      onOk={handleOk}
      okText={mode === "create" ? "Create" : "Update"}
      confirmLoading={loading}
      width={720}
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="medicineId"
          label="Medicine"
          rules={[{ required: true, message: "Please select medicine" }]}
        >
          <Select
            placeholder="Select medicine"
            options={medicineOptions}
            disabled={mode === "edit"}
          />
        </Form.Item>

        <Form.Item
          name="lotNumber"
          label="Batch Number"
          rules={[
            { required: true, message: "Please enter batch number" },
            { max: 255, message: "Batch number is too long" },
          ]}
        >
          <Input placeholder="e.g. PCM-0423" />
        </Form.Item>

        <Form.Item
          name="manufactureDate"
          label="Manufacture Date"
          rules={[{ required: true, message: "Please select manufacture date" }]}
        >
          <DatePicker className="w-full" format="DD/MM/YYYY" />
        </Form.Item>

        <Form.Item
          name="expiryDate"
          label="Expiry Date"
          rules={[{ required: true, message: "Please select expiry date" }]}
        >
          <DatePicker className="w-full" format="DD/MM/YYYY" />
        </Form.Item>

        <Form.Item
          name="quantity"
          label="Quantity"
          rules={[{ required: true, message: "Please enter quantity" }]}
        >
          <InputNumber
            className="w-full"
            min={0}
            precision={0}
            placeholder="Enter quantity"
          />
        </Form.Item>

        <Form.Item
          name="warehouseId"
          label="Warehouse"
          rules={[{ required: true, message: "Please select warehouse" }]}
        >
          <Select placeholder="Select warehouse" options={warehouseOptions} />
        </Form.Item>
      </Form>
    </BaseModal>
  );
}

export default BatchFormModal;
