import { Form, Input } from "antd";
import { useEffect } from "react";
import BaseModal from "../../../components/base/BaseModal";

export type MedicineFormValues = {
  name: string;
  manufacturer: string;
  storageCondition: string;
  description: string;
};

type MedicineFormModalProps = {
  open: boolean;
  mode: "create" | "edit";
  loading?: boolean;
  initialValues?: Partial<MedicineFormValues>;
  onCancel: () => void;
  onSubmit: (values: MedicineFormValues) => Promise<void> | void;
};

const defaultValues: MedicineFormValues = {
  name: "",
  manufacturer: "",
  storageCondition: "",
  description: "",
};

function MedicineFormModal({
  open,
  mode,
  loading = false,
  initialValues,
  onCancel,
  onSubmit,
}: MedicineFormModalProps) {
  const [form] = Form.useForm<MedicineFormValues>();

  useEffect(() => {
    if (!open) return;
    form.setFieldsValue({
      ...defaultValues,
      ...initialValues,
    });
  }, [open, form, initialValues]);

  const handleOk = async () => {
    const values = await form.validateFields();
    await onSubmit(values);
  };

  return (
    <BaseModal
      open={open}
      title={mode === "create" ? "Create Medicine" : "Edit Medicine"}
      onCancel={onCancel}
      onOk={handleOk}
      okText={mode === "create" ? "Create" : "Update"}
      confirmLoading={loading}
      width={680}
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="name"
          label="Medicine Name"
          rules={[
            { required: true, message: "Please enter medicine name" },
            { max: 255, message: "Medicine name is too long" },
          ]}
        >
          <Input placeholder="e.g. Paracetamol 500mg" />
        </Form.Item>

        <Form.Item
          name="manufacturer"
          label="Manufacturer"
          rules={[
            { required: true, message: "Please enter manufacturer" },
            { max: 255, message: "Manufacturer is too long" },
          ]}
        >
          <Input placeholder="e.g. DHG Pharma" />
        </Form.Item>

        <Form.Item
          name="storageCondition"
          label="Storage Condition"
          rules={[
            { required: true, message: "Please enter storage condition" },
            { max: 255, message: "Storage condition is too long" },
          ]}
        >
          <Input placeholder="e.g. Room temperature" />
        </Form.Item>

        <Form.Item
          name="description"
          label="Description"
          rules={[
            { required: true, message: "Please enter description" },
            { max: 1000, message: "Description is too long" },
          ]}
        >
          <Input.TextArea
            rows={4}
            placeholder="Medicine description"
            showCount
            maxLength={1000}
          />
        </Form.Item>
      </Form>
    </BaseModal>
  );
}

export default MedicineFormModal;
