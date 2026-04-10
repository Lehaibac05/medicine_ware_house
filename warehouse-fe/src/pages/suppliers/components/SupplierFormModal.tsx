import { QRCodeSVG } from "@rc-component/qrcode";
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
  qrBankTransferLink: string;
  status: string;
};

type SupplierFormModalProps = {
  open: boolean;
  mode: "create" | "edit";
  loading?: boolean;
  initialValues?: Partial<SupplierFormValues>;
  onCancel: () => void;
  onSubmit: (values: SupplierFormValues) => Promise<void> | void;
  canEditDetails: boolean;
  canEditQr: boolean;
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
  qrBankTransferLink: "",
  status: "ACTIVE",
};

function SupplierFormModal({
  open,
  mode,
  loading = false,
  initialValues,
  onCancel,
  onSubmit,
  canEditDetails,
  canEditQr,
}: SupplierFormModalProps) {
  const [form] = Form.useForm<SupplierFormValues>();

  useEffect(() => {
    if (!open) return;

    form.setFieldsValue({
      ...defaultValues,
      ...initialValues,
    });
  }, [open, form, initialValues]);

  const handleOk = async () => {
    const values = await form.validateFields();

    await onSubmit({
      ...values,
      supplierName: canEditDetails ? values.supplierName.trim() : values.supplierName,
      contactPerson: canEditDetails ? values.contactPerson.trim() : values.contactPerson,
      phoneNumber: canEditDetails ? values.phoneNumber.trim() : values.phoneNumber,
      email: canEditDetails ? values.email.trim() : values.email,
      address: canEditDetails ? values.address.trim() : values.address,
      taxCode: canEditDetails ? values.taxCode.trim() : values.taxCode,
      qrBankTransferLink: canEditQr
        ? values.qrBankTransferLink?.trim() || ""
        : values.qrBankTransferLink || "",
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
          <Input placeholder="Ví dụ: Công ty Dược ABC" disabled={!canEditDetails} />
        </Form.Item>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <Form.Item name="contactPerson" label="Người liên hệ">
            <Input placeholder="Nguyễn Văn A" disabled={!canEditDetails} />
          </Form.Item>

          <Form.Item name="phoneNumber" label="Số điện thoại">
            <Input placeholder="0901234567" disabled={!canEditDetails} />
          </Form.Item>
        </div>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <Form.Item name="email" label="Email">
            <Input placeholder="supplier@example.com" disabled={!canEditDetails} />
          </Form.Item>

          <Form.Item name="taxCode" label="Mã số thuế">
            <Input placeholder="0312345678" disabled={!canEditDetails} />
          </Form.Item>
        </div>

        <Form.Item name="address" label="Địa chỉ">
          <Input.TextArea
            rows={3}
            placeholder="Nhập địa chỉ nhà cung cấp..."
            disabled={!canEditDetails}
          />
        </Form.Item>

        {/* INPUT QR */}
        <Form.Item
          name="qrBankTransferLink"
          label="Nội dung/Link QR chuyển khoản"
          extra="Dán chuỗi nội dung QR hoặc link ảnh QR"
        >
          <Input.TextArea
            rows={3}
            placeholder="Nhập QR content hoặc link ảnh..."
            disabled={!canEditQr}
          />
        </Form.Item>

        {/* PREVIEW QR (FIX Ở ĐÂY) */}
        <Form.Item shouldUpdate={(prev, curr) => prev.qrBankTransferLink !== curr.qrBankTransferLink}>
          {({ getFieldValue }) => {
            const qrValue = (getFieldValue("qrBankTransferLink") || "").trim();

            if (!qrValue) return null;

            const isLikelyImageLink =
              /^data:image\//i.test(qrValue) ||
              (() => {
                try {
                  const url = new URL(qrValue);
                  return (
                    /^https?:\/\//i.test(url.toString()) &&
                    /\.(png|jpe?g|gif|webp|bmp|svg)(\?.*)?$/i.test(url.pathname)
                  );
                } catch {
                  return false;
                }
              })();

            return (
              <div className="mt-3 flex items-center gap-4">
                {isLikelyImageLink ? (
                  <img
                    src={qrValue}
                    alt="QR"
                    className="h-[160px] w-[160px] object-contain rounded border border-slate-200 bg-white"
                  />
                ) : (
                  <QRCodeSVG value={qrValue} size={160} level="M" />
                )}
                <div className="text-xs text-slate-500">
                  {isLikelyImageLink
                    ? "Đang hiển thị ảnh QR từ link."
                    : "Đang sinh QR từ chuỗi nội dung."}
                </div>
              </div>
            );
          }}
        </Form.Item>

        <Form.Item name="status" label="Trạng thái">
          <Select options={statusOptions} disabled={!canEditDetails} />
        </Form.Item>
      </Form>
    </BaseModal>
  );
}

export default SupplierFormModal;
