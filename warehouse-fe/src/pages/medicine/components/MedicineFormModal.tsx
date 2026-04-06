import { Form, Input, InputNumber, Select } from "antd";
import { useEffect, useMemo, useState } from "react";
import BaseModal from "../../../components/base/BaseModal";
import { getActiveSuppliers } from "../../../services/suppliers";
import type { Supplier } from "../../../services/types";

export type MedicineFormValues = {
  name: string;
  manufacturer: string;
  storageCondition: string;
  description: string;
  reorderLevel: number;
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
  reorderLevel: 10,
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
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [supplierLoading, setSupplierLoading] = useState(false);

  useEffect(() => {
    if (!open) return;

    const loadSuppliers = async () => {
      setSupplierLoading(true);
      try {
        const data = await getActiveSuppliers();
        setSuppliers(data);
      } catch {
        setSuppliers([]);
      } finally {
        setSupplierLoading(false);
      }
    };

    void loadSuppliers();

    form.setFieldsValue({
      ...defaultValues,
      ...initialValues,
    });
  }, [open, form, initialValues]);

  const supplierOptions = useMemo(() => {
    const options = suppliers.map((supplier) => ({
      value: supplier.supplierName,
      label: supplier.supplierName,
    }));

    const currentSupplier = form.getFieldValue("manufacturer");
    if (
      currentSupplier &&
      !options.some((option) => option.value === currentSupplier)
    ) {
      options.unshift({
        value: currentSupplier,
        label: `${currentSupplier} (không hoạt động)`,
      });
    }

    return options;
  }, [suppliers, form]);

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
            { required: true, message: "Vui lòng chọn nhà cung cấp." },
            { max: 255, message: "Tên nhà cung cấp quá dài." },
          ]}
        >
          <Select
            showSearch
            optionFilterProp="label"
            loading={supplierLoading}
            options={supplierOptions}
            placeholder="Chọn nhà cung cấp"
            notFoundContent="Không có nhà cung cấp phù hợp"
          />
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
          name="reorderLevel"
          label="Ngưỡng cảnh báo tồn kho (Reorder Level)"
          rules={[
            { required: true, message: "Vui lòng nhập reorder level." },
            { type: "number", min: 0, message: "Reorder level phải >= 0." },
          ]}
        >
          <InputNumber
            min={0}
            step={1}
            className="w-full"
            placeholder="Nhập ngưỡng tồn kho thấp"
          />
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
