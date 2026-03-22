import { Form, Input, Select } from "antd";
import { useEffect } from "react";
import BaseModal from "../../../components/base/BaseModal";

export type SupplierFormValues = {
  supplierName: string;
  contactPerson: string;
  phoneNumber: string;
  email: string;
  address: string;
  taxCode: string;
  status: string;
};

type SupplierFormModalProps = {
  open: boolean;
  mode: "create" | "edit";
  loading?: boolean;
  initialValues?: Partial<SupplierFormValues>;
  onCancel: () => void;
  onSubmit: (values: SupplierFormValues) => Promise<void> | void;
};

const statusOptions = [
  { value: "ACTIVE", label: "Hoạt động" },
  { value: "INACTIVE", label: "Ngừng hoạt động" },
  { value: "SUSPENDED", label: "Tạm ngưng" },
];

const defaultValues: SupplierFormValues = {
  supplierName: "",
  contactPerson: "",
  phoneNumber: "",
  email: "",
  address: "",
  taxCode: "",
  status: "ACTIVE",
};

function SupplierFormModal({
  open,
  mode,
  loading = false,
  initialValues,
  onCancel,
  onSubmit,
}: SupplierFormModalProps) {
  const [form] = Form.useForm<SupplierFormValues>();

  useEffect(() => {
    if (!open) {
      return;
    }

    form.setFieldsValue({
      ...defaultValues,
      ...initialValues,
    });
  }, [open, form, initialValues]);

  const handleOk = async () => {
    const values = await form.validateFields();
    await onSubmit({
      ...values,
      supplierName: values.supplierName.trim(),
      contactPerson: values.contactPerson.trim(),
      phoneNumber: values.phoneNumber.trim(),
      email: values.email.trim(),
      address: values.address.trim(),
      taxCode: values.taxCode.trim(),
    });
  };

  return (
    <BaseModal
      open={open}
      title={mode === "create" ? "Thêm nhà cung cấp" : "Cập nhật nhà cung cấp"}
      onCancel={onCancel}
      onOk={handleOk}
      okText={mode === "create" ? "Tạo" : "Cập nhật"}
      confirmLoading={loading}
      width={720}
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="supplierName"
          label="Tên nhà cung cấp"
          rules={[
            { required: true, message: "Vui lòng nhập tên nhà cung cấp." },
            { max: 255, message: "Tên nhà cung cấp quá dài." },
          ]}
        >
          <Input placeholder="Ví dụ: Công ty Dược ABC" />
        </Form.Item>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <Form.Item
            name="contactPerson"
            label="Người liên hệ"
            rules={[{ max: 255, message: "Tên người liên hệ quá dài." }]}
          >
            <Input placeholder="Nguyễn Văn A" />
          </Form.Item>

          <Form.Item
            name="phoneNumber"
            label="Số điện thoại"
            rules={[{ max: 50, message: "Số điện thoại quá dài." }]}
          >
            <Input placeholder="0901234567" />
          </Form.Item>
        </div>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <Form.Item
            name="email"
            label="Email"
            rules={[
              { type: "email", message: "Email không hợp lệ." },
              { max: 255, message: "Email quá dài." },
            ]}
          >
            <Input placeholder="supplier@example.com" />
          </Form.Item>

          <Form.Item
            name="taxCode"
            label="Mã số thuế"
            rules={[{ max: 100, message: "Mã số thuế quá dài." }]}
          >
            <Input placeholder="0312345678" />
          </Form.Item>
        </div>

        <Form.Item
          name="address"
          label="Địa chỉ"
          rules={[{ max: 500, message: "Địa chỉ quá dài." }]}
        >
          <Input.TextArea rows={3} placeholder="Nhập địa chỉ nhà cung cấp..." />
        </Form.Item>

        <Form.Item name="status" label="Trạng thái">
          <Select options={statusOptions} />
        </Form.Item>
      </Form>
    </BaseModal>
  );
}

export default SupplierFormModal;
