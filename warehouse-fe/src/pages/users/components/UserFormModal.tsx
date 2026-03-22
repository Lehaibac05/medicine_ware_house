import { Form, Input, Select } from "antd";
import { useEffect } from "react";
import BaseModal from "../../../components/base/BaseModal";

export type UserFormValues = {
  username: string;
  fullName: string;
  email: string;
  status: string;
  roleId?: number;
  roleName?: string;
};

type RoleOption = {
  value: number; // roleId
  label: string; // roleName
};

type UserFormModalProps = {
  open: boolean;
  loading?: boolean;
  roleOptions: RoleOption[];
  onCancel: () => void;
  onSubmit: (values: UserFormValues) => Promise<void> | void;
};

const statusOptions = [
  { value: "ACTIVE", label: "Hoạt động" },
  { value: "INACTIVE", label: "Không hoạt động" },
];

const defaultValues: UserFormValues = {
  username: "",
  fullName: "",
  email: "",
  status: "ACTIVE",
  roleId: undefined,
  roleName: "",
};

function UserFormModal({
  open,
  loading = false,
  roleOptions,
  onCancel,
  onSubmit,
}: UserFormModalProps) {
  const [form] = Form.useForm<UserFormValues>();

  useEffect(() => {
    if (open) {
      form.resetFields();
      form.setFieldsValue(defaultValues);
    }
  }, [open, form]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      await onSubmit(values);
      form.resetFields();
    } catch (err) {
      console.log("Validate failed:", err);
    }
  };

  return (
    <BaseModal
      open={open}
      title="Tạo người dùng"
      onCancel={() => {
        form.resetFields();
        onCancel();
      }}
      onOk={handleOk}
      okText="Tạo"
      confirmLoading={loading}
      width={640}
    >
      <Form form={form} layout="vertical">
        {/* Username */}
        <Form.Item
          name="username"
          label="Tên đăng nhập"
          rules={[
            { required: true, message: "Vui lòng nhập tên đăng nhập." },
            { max: 255, message: "Tên đăng nhập quá dài." },
          ]}
        >
          <Input placeholder="Nhập tên đăng nhập..." />
        </Form.Item>

        {/* Full Name */}
        <Form.Item
          name="fullName"
          label="Họ và tên"
          rules={[
            { required: true, message: "Vui lòng nhập họ và tên." },
            { max: 255, message: "Họ và tên quá dài." },
          ]}
        >
          <Input placeholder="Nhập họ và tên..." />
        </Form.Item>

        {/* Email */}
        <Form.Item
          name="email"
          label="Email"
          rules={[
            { required: true, message: "Vui lòng nhập email." },
            { type: "email", message: "Email không hợp lệ." },
            { max: 255, message: "Email quá dài." },
          ]}
        >
          <Input placeholder="Nhập email..." />
        </Form.Item>

        {/* Status */}
        <Form.Item
          name="status"
          label="Trạng thái"
          rules={[{ required: true, message: "Vui lòng chọn trạng thái." }]}
        >
          <Select options={statusOptions} />
        </Form.Item>

        {/* Role Select */}
        <Form.Item
          name="roleId"
          label="Vai trò"
          rules={[{ required: true, message: "Vui lòng chọn vai trò." }]}
        >
          <Select
            placeholder="Chọn vai trò"
            options={roleOptions}
            showSearch
            optionFilterProp="label"
            onChange={(value, option: any) => {
              form.setFieldsValue({
                roleId: value,
                roleName: option.label,
              });
            }}
          />
        </Form.Item>
      </Form>
    </BaseModal>
  );
}

export default UserFormModal;