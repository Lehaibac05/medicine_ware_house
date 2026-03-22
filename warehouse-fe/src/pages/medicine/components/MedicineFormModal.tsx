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
      title={mode === "create" ? "Thêm thuốc" : "Sửa thông tin thuốc"}
      onCancel={onCancel}
      onOk={handleOk}
      okText={mode === "create" ? "Tạo" : "Cập nhật"}
      confirmLoading={loading}
      width={680}
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="name"
          label="Tên thuốc"
          rules={[
            { required: true, message: "Vui lòng nhập tên thuốc." },
            { max: 255, message: "Tên thuốc quá dài." },
          ]}
        >
          <Input placeholder="e.g. Paracetamol 500mg" />
        </Form.Item>

        <Form.Item
          name="manufacturer"
          label="Nhà cung cấp"
          rules={[
            { required: true, message: "Please enter manufacturer" },
            { max: 255, message: "Manufacturer is too long" },
          ]}
        >
          <Input placeholder="e.g. DHG Pharma" />
        </Form.Item>

        <Form.Item
          name="storageCondition"
          label="Điều kiện bảo quản"
          rules={[
            { required: true, message: "Vui lòng nhập điều kiện bảo quản." },
            { max: 255, message: "Điều kiện bảo quản quá dài." },
          ]}
        >
          <Input placeholder="e.g. Nhiệt độ phòng..." />
        </Form.Item>

        <Form.Item
          name="description"
          label="Mô tả"
        >
          <Input.TextArea
            rows={4}
            placeholder="Nhập mô tả thuốc..."
            showCount
            maxLength={1000}
          />
        </Form.Item>
      </Form>
    </BaseModal>
  );
}

export default MedicineFormModal;
