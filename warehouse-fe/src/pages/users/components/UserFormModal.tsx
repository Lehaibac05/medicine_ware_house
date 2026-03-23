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
  password?: string;
  confirmPassword?: string;
};

type RoleOption = {
  value: number; // roleId
  label: string; // roleName
};

type UserFormModalProps = {
  open: boolean;
  loading?: boolean;
  mode?: "create" | "edit";
  initialValues?: Partial<UserFormValues>;
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
  password: "",
  confirmPassword: "",
};

function UserFormModal({
  open,
  loading = false,
  mode = "create",
  initialValues,
  roleOptions,
  onCancel,
  onSubmit,
}: UserFormModalProps) {
  const [form] = Form.useForm<UserFormValues>();

  useEffect(() => {
    if (open) {
      form.resetFields();
      form.setFieldsValue({
        ...defaultValues,
        ...initialValues,
      });
    }
  }, [open, form, initialValues]);

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
      title={mode === "edit" ? "Chỉnh sửa người dùng" : "Tạo người dùng"}
      onCancel={() => {
        form.resetFields();
        onCancel();
      }}
      onOk={handleOk}
      okText={mode === "edit" ? "Lưu" : "Tạo"}
      confirmLoading={loading}
      width={640}
    >
      <Form form={form} layout="vertical">
        {/* Username */}
        <Form.Item
          name="username"
          label="Tên đăng nhập"
          rules={[
            { required: mode === "create", message: "Vui lòng nhập tên đăng nhập." },
            { max: 255, message: "Tên đăng nhập quá dài." },
          ]}
        >
          <Input
            placeholder="Nhập tên đăng nhập..."
            disabled={mode === "edit"}
          />
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

        {mode === "edit" && (
          <>
            <Form.Item
              name="password"
              label="Mật khẩu mới"
              rules={[
                { min: 6, message: "Mật khẩu phải có ít nhất 6 ký tự." },
              ]}
              extra="Để trống nếu không đổi mật khẩu"
            >
              <Input.Password placeholder="Nhập mật khẩu mới (tuỳ chọn)" />
            </Form.Item>

            <Form.Item
              name="confirmPassword"
              label="Xác nhận mật khẩu mới"
              dependencies={["password"]}
              rules={[
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    const password = getFieldValue("password") as string | undefined;
                    if (!password && !value) {
                      return Promise.resolve();
                    }
                    if (password && !value) {
                      return Promise.reject(new Error("Vui lòng xác nhận mật khẩu mới."));
                    }
                    if (password !== value) {
                      return Promise.reject(new Error("Mật khẩu xác nhận không khớp."));
                    }
                    return Promise.resolve();
                  },
                }),
              ]}
            >
              <Input.Password placeholder="Nhập lại mật khẩu mới" />
            </Form.Item>
          </>
        )}
      </Form>
    </BaseModal>
  );
}

export default UserFormModal;