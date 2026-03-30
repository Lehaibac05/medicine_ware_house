import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Button,
  DatePicker,
  Divider,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Table,
  Typography,
  message,
} from "antd";
import {
  FileTextOutlined,
  DollarOutlined,
  CheckCircleOutlined,
  WarningOutlined,
} from "@ant-design/icons";
import { QRCodeSVG } from "@rc-component/qrcode";
import dayjs from "dayjs";
import { AxiosError } from "axios";
import BaseFilterCard from "../../components/base/BaseFilterCard";
import PaymentTable from "./components/PaymentTable";
import BaseStatsGrid from "../../components/base/BaseStatsGrid";
import MainLayout from "../../layouts/MainLayout";
import {
  useCreateSupplierInvoiceMutation,
  useGoodsReceiptsQuery,
  usePaySupplierInvoiceMutation,
  useRejectSupplierInvoiceMutation,
  useSupplierInvoicesQuery,
  useSupplierInvoiceDetailQuery,
  useVerifySupplierInvoiceMutation,
} from "../../hooks/useWorkflow";
import type { GoodsReceipt, SupplierInvoice } from "../../services/workflow";
import { getPrimaryRole, getRoleLabel, hasAnyRole } from "../../utils/auth";

const { Text } = Typography;
const { RangePicker } = DatePicker;
const { TextArea } = Input;

type InvoiceDraftItem = {
  key: string;
  medicineId: number;
  medicineName: string;
  receivedQuantity: number;
  poUnitPrice: number;
  invoiceQuantity: number;
  invoiceUnitPrice: number;
  notes: string;
};

const statusOptions = [
  { value: "all", label: "Tất cả trạng thái" },
  { value: "PENDING_VERIFICATION", label: "Chờ xác minh" },
  { value: "VERIFIED", label: "Đã xác minh" },
  { value: "PAID", label: "Đã thanh toán" },
  { value: "REJECTED", label: "Đã từ chối" },
];

const methodOptions = [
  { value: "BANK_TRANSFER", label: "Chuyển khoản ngân hàng" },
  { value: "CASH", label: "Tiền mặt" },
  { value: "CREDIT_CARD", label: "Thẻ tín dụng" },
  { value: "E_WALLET", label: "Ví điện tử" },
];

const PaymentPage = () => {
  const [messageApi, contextHolder] = message.useMessage();
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("all");
  const [range, setRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);

  const [createOpen, setCreateOpen] = useState(false);
  const [createPayload, setCreatePayload] = useState({
    goodsReceiptId: undefined as number | undefined,
    invoiceDate: undefined as string | undefined,
    dueDate: undefined as string | undefined,
    supplierInvoiceAmount: undefined as number | undefined,
    notes: "",
  });
  const [draftItems, setDraftItems] = useState<InvoiceDraftItem[]>([]);

  const [verifyTarget, setVerifyTarget] = useState<SupplierInvoice | null>(
    null,
  );
  const [verifyNotes, setVerifyNotes] = useState("");
  const [rejectTarget, setRejectTarget] = useState<SupplierInvoice | null>(
    null,
  );
  const [rejectReason, setRejectReason] = useState("");
  const [payTarget, setPayTarget] = useState<SupplierInvoice | null>(null);
  const [payAmount, setPayAmount] = useState<number | null>(null);
  const [payMethod, setPayMethod] = useState("BANK_TRANSFER");
  const [payTransactionReference, setPayTransactionReference] = useState("");
  const [payNotes, setPayNotes] = useState("");

  const canManageInvoices = hasAnyRole(["ROLE_ACCOUNTANT"]);
  const currentRoleLabel = getRoleLabel(getPrimaryRole());

  const showPermissionError = () => {
    messageApi.error(
      `403: Chỉ kế toán mới có quyền tạo/xác minh/từ chối/thanh toán hóa đơn. Vai trò hiện tại: ${currentRoleLabel}`,
    );
  };
  const { data: invoices = [], isLoading } = useSupplierInvoicesQuery();
  const { data: goodsReceipts = [] } = useGoodsReceiptsQuery();
  const {
    data: payTargetDetail,
  } = useSupplierInvoiceDetailQuery(payTarget?.invoiceId ?? 0);
  const createMutation = useCreateSupplierInvoiceMutation();
  const verifyMutation = useVerifySupplierInvoiceMutation();
  const rejectMutation = useRejectSupplierInvoiceMutation();
  const payMutation = usePaySupplierInvoiceMutation();

  const filteredInvoices = useMemo(() => {
    return invoices
      .filter((invoice) => {
        if (!search.trim()) return true;
        const keyword = search.trim().toLowerCase();
        return (
          invoice.invoiceCode?.toLowerCase().includes(keyword) ||
          invoice.supplier?.supplierName?.toLowerCase().includes(keyword)
        );
      })
      .filter((invoice) => {
        if (status === "all") return true;
        return (invoice.status || "").toUpperCase() === status;
      })
      .filter((invoice) => {
        if (!range || !invoice.invoiceDate) return true;
        const date = dayjs(invoice.invoiceDate);
        return (
          !date.isBefore(range[0].startOf("day")) &&
          !date.isAfter(range[1].endOf("day"))
        );
      });
  }, [invoices, search, status, range]);

  // Đồng bộ hóa payTarget với dữ liệu hóa đơn mới nhất (để QR đổi ngay sau khi cập nhật supplier).
  useEffect(() => {
    if (!payTarget) return;

    const latest = invoices.find((i) => i.invoiceId === payTarget.invoiceId);
    if (!latest) return;

    const latestQr = latest.supplier?.qrBankTransferLink;
    const currentQr = payTarget.supplier?.qrBankTransferLink;

    if (latestQr !== currentQr) {
      setPayTarget(latest);
    }
  }, [invoices, payTarget?.invoiceId, payTarget?.supplier?.qrBankTransferLink]);

  const paymentStats = useMemo(() => {
    const totalInvoices = invoices.length;

    const totalValue = invoices.reduce(
      (sum, row) => sum + Number(row.totalAmount || 0),
      0,
    );

    const paidValue = invoices.reduce(
      (sum, row) => sum + Number(row.paidAmount || 0),
      0,
    );

    const pendingVerification = invoices.filter(
      (row) => (row.status || "").toUpperCase() === "PENDING_VERIFICATION",
    ).length;

    const mismatchCount = invoices.filter((row) => !!row.hasMismatch).length;

    return [
      {
        label: "Tổng số hóa đơn",
        value: totalInvoices,
        note: `${filteredInvoices.length} hóa đơn trong danh sách`,
        icon: <FileTextOutlined />,
        color: "text-blue-600",
        bg: "bg-[#eff6ff]",
        trend: "+5%",
        trendUp: true,
      },
      {
        label: "Tổng giá trị hóa đơn",
        value: totalValue,
        note: "Tổng giá trị tất cả hóa đơn",
        icon: <DollarOutlined />,
        color: "text-emerald-600",
        bg: "bg-[#f0fdf4]",
        trend: "+8%",
        trendUp: true,
      },
      {
        label: "Đã thanh toán",
        value: paidValue,
        note: `${pendingVerification} hóa đơn chờ xác minh`,
        icon: <CheckCircleOutlined />,
        color: "text-green-600",
        bg: "bg-[#ecfdf5]",
        trend: "+3%",
        trendUp: true,
      },
      {
        label: "Sai lệch",
        value: mismatchCount,
        note: "Cần xử lý trước khi xác minh",
        icon: <WarningOutlined />,
        color: "text-red-600",
        bg: "bg-[#fef2f2]",
        trend: "-2%",
        trendUp: false,
      },
    ];
  }, [invoices, filteredInvoices.length]);

  const normalizeStatus = (status?: string) =>
    status
      ?.trim()
      .toUpperCase()
      .replace(/[\s-]+/g, "_") ?? "";

  const existingInvoiceReceiptIds = useMemo(
    () =>
      new Set(
        invoices
          .map((invoice) => invoice.goodsReceipt?.receiptId)
          .filter((id): id is number => !!id),
      ),
    [invoices],
  );

  const availableGoodsReceipts = useMemo(
    () =>
      goodsReceipts.filter(
        (receipt: GoodsReceipt) =>
          normalizeStatus(receipt.status) === "APPROVED" &&
          !existingInvoiceReceiptIds.has(receipt.receiptId),
      ),
    [goodsReceipts, existingInvoiceReceiptIds],
  );

  const selectedReceipt = useMemo(
    () =>
      goodsReceipts.find(
        (receipt: GoodsReceipt) => receipt.receiptId === createPayload.goodsReceiptId,
      ),
    [goodsReceipts, createPayload.goodsReceiptId],
  );

  const buildDraftItemsFromReceipt = (receipt?: GoodsReceipt) => {
    if (!receipt?.purchaseOrder?.items?.length) {
      setDraftItems([]);
      return;
    }

    const items = receipt.purchaseOrder.items
      .map((item) => {
        const medicineId = item.medicine?.medicineId;
        if (!medicineId) return null;

        const receivedQty = Number(
          item.receivedQuantity ?? item.requestedQuantity ?? 0,
        );
        const poUnitPrice = Number(item.unitPrice ?? 0);
        return {
          key: String(medicineId),
          medicineId,
          medicineName:
            item.medicine?.medicineName || `Medicine #${medicineId}`,
          receivedQuantity: receivedQty,
          poUnitPrice,
          invoiceQuantity: receivedQty,
          invoiceUnitPrice: poUnitPrice,
          notes: "",
        } as InvoiceDraftItem;
      })
      .filter((item): item is InvoiceDraftItem => item !== null);

    setDraftItems(items);
  };

  const compareSummary = useMemo(() => {
    const qtyMismatch = draftItems.filter(
      (item) => item.invoiceQuantity !== item.receivedQuantity,
    ).length;
    const priceMismatch = draftItems.filter(
      (item) => item.invoiceUnitPrice !== item.poUnitPrice,
    ).length;
    return { qtyMismatch, priceMismatch };
  }, [draftItems]);

  const draftInvoiceTotal = useMemo(
    () =>
      draftItems.reduce(
        (sum, item) =>
          sum +
          Number(item.invoiceQuantity || 0) *
            Number(item.invoiceUnitPrice || 0),
        0,
      ),
    [draftItems],
  );

  const referenceTotal = useMemo(
    () =>
      draftItems.reduce(
        (sum, item) =>
          sum +
          Number(item.receivedQuantity || 0) * Number(item.poUnitPrice || 0),
        0,
      ),
    [draftItems],
  );

  const supplierInvoiceAmount = Number(
    createPayload.supplierInvoiceAmount || 0,
  );
  const amountDiffWithLines = supplierInvoiceAmount - draftInvoiceTotal;
  const amountDiffWithReference = supplierInvoiceAmount - referenceTotal;
  const amountMatchesLines = Math.abs(amountDiffWithLines) < 0.01;
  const amountMatchesReference = Math.abs(amountDiffWithReference) < 0.01;

  const invoiceDateObj = createPayload.invoiceDate
    ? dayjs(createPayload.invoiceDate)
    : null;
  const dueDateObj = createPayload.dueDate
    ? dayjs(createPayload.dueDate)
    : null;
  const invalidDateRange =
    !!invoiceDateObj &&
    !!dueDateObj &&
    dueDateObj.isBefore(invoiceDateObj, "day");

  const handleCreateInvoice = async () => {
    if (!createPayload.goodsReceiptId) {
      messageApi.error("Vui lòng chọn phiếu nhập đã được duyệt");
      return;
    }

    if (!draftItems.length) {
      messageApi.error("Không tìm thấy sản phẩm hóa đơn từ phiếu nhập đã chọn");
      return;
    }

    if (
      !createPayload.supplierInvoiceAmount ||
      createPayload.supplierInvoiceAmount <= 0
    ) {
      messageApi.error("Vui lòng nhập số tiền hóa đơn nhà cung cấp");
      return;
    }

    if (invalidDateRange) {
      messageApi.error("Ngày đến hạn phải bằng hoặc sau ngày hóa đơn");
      return;
    }

    if (!amountMatchesLines) {
      messageApi.error("Số tiền hóa đơn phải khớp với tổng các dòng hóa đơn");
      return;
    }

    const payloadItems = draftItems
      .filter((item) => item.invoiceQuantity > 0)
      .map((item) => ({
        medicineId: item.medicineId,
        quantity: item.invoiceQuantity,
        unitPrice: item.invoiceUnitPrice,
        notes: item.notes || undefined,
      }));

    if (!payloadItems.length) {
      messageApi.error("Hóa đơn phải có ít nhất một mặt hàng hợp lệ");
      return;
    }

    try {
      const mergedNotes = [
        createPayload.notes?.trim(),
        `Số tiền phải thanh toán theo nhà cung cấp: ${supplierInvoiceAmount.toLocaleString(
          "vi-VN",
          {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
          },
        )}`,
        "Điều khoản thanh toán: Thanh toán ngay khi nhận hóa đơn",
      ]
        .filter((value) => !!value)
        .join(" | ");

      await createMutation.mutateAsync({
        goodsReceiptId: createPayload.goodsReceiptId,
        invoiceDate: createPayload.invoiceDate,
        dueDate: createPayload.dueDate,
        notes: mergedNotes || undefined,
        items: payloadItems,
      });

      setCreateOpen(false);
      setCreatePayload({
        goodsReceiptId: undefined,
        invoiceDate: undefined,
        dueDate: undefined,
        supplierInvoiceAmount: undefined,
        notes: "",
      });
      setDraftItems([]);
      messageApi.success("Tạo hóa đơn nhà cung cấp thành công");
    } catch (error) {
      if (error instanceof AxiosError && error.response?.status === 403) {
        showPermissionError();
        return;
      }
      messageApi.error("Lỗi khi tạo hóa đơn nhà cung cấp");
    }
  };

  const handleVerify = async () => {
    if (!verifyTarget) return;
    try {
      await verifyMutation.mutateAsync({
        id: verifyTarget.invoiceId,
        verificationNotes: verifyNotes || undefined,
      });
      setVerifyTarget(null);
      setVerifyNotes("");
      messageApi.success("Invoice verified");
    } catch (error) {
      if (error instanceof AxiosError && error.response?.status === 403) {
        showPermissionError();
        return;
      }
      messageApi.error("Failed to verify invoice");
    }
  };

  const handleReject = async () => {
    if (!rejectTarget) return;
    try {
      await rejectMutation.mutateAsync({
        id: rejectTarget.invoiceId,
        reason: rejectReason || undefined,
      });
      setRejectTarget(null);
      setRejectReason("");
      messageApi.success("Hóa đơn đã bị từ chối");
    } catch (error) {
      if (error instanceof AxiosError && error.response?.status === 403) {
        showPermissionError();
        return;
      }
      messageApi.error("Không thể từ chối hóa đơn");
    }
  };

  const handlePay = async () => {
    if (!payTarget || !payAmount || payAmount <= 0) {
      messageApi.error("Vui lòng nhập số tiền hợp lệ");
      return;
    }

    try {
      await payMutation.mutateAsync({
        id: payTarget.invoiceId,
        amount: payAmount,
        method: payMethod,
        transactionReference: payTransactionReference.trim() || undefined,
        notes: payNotes || undefined,
      });
      setPayTarget(null);
      setPayAmount(null);
      setPayNotes("");
      setPayTransactionReference("");
      setPayMethod("BANK_TRANSFER");
      messageApi.success("Ghi nhận thanh toán thành công");
    } catch (error) {
      if (error instanceof AxiosError && error.response?.status === 403) {
        showPermissionError();
        return;
      }
      messageApi.error("Không thể xử lý thanh toán");
    }
  };

  const qrValue =
    payTargetDetail?.supplier?.qrBankTransferLink?.trim() ||
    payTarget?.supplier?.qrBankTransferLink?.trim() ||
    "";
  const isLikelyImageLink =
    qrValue.startsWith("data:image/") ||
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
    <MainLayout>
      {contextHolder}
      {!canManageInvoices && (
        <Alert
          type="warning"
          showIcon
          message={`Bạn chỉ có thể xem hóa đơn. Vai trò ${currentRoleLabel} không có quyền tạo/xác minh/từ chối/thanh toán.`}
        />
      )}

      <BaseStatsGrid stats={paymentStats} />

      <BaseFilterCard
        actions={
          <Space>
            <Button
              type="primary"
              className="h-[40px]"
              onClick={() => {
                if (!canManageInvoices) {
                  showPermissionError();
                  return;
                }
                setCreateOpen(true);
              }}
              disabled={!canManageInvoices}
            >
              Tạo hóa đơn
            </Button>
            <Button
              className="h-[40px]"
              onClick={() => {
                setSearch("");
                setStatus("all");
                setRange(null);
              }}
            >
              Khôi phục
            </Button>
          </Space>
        }
      >
        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Hóa đơn / Nhà cung cấp</Text>
          <Input
            placeholder="Nhập mã hóa đơn hoặc tên nhà cung cấp"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Trạng thái xử lý</Text>
          <Select options={statusOptions} value={status} onChange={setStatus} />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Phương thức thanh toán</Text>
          <Select
            options={methodOptions}
            value={payMethod}
            onChange={setPayMethod}
          />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">
            Khoảng thời gian hóa đơn
          </Text>
          <RangePicker
            className="w-full"
            format="DD/MM/YYYY"
            placeholder={["Start date", "End date"]}
            value={range}
            onChange={(values) =>
              setRange(values as [dayjs.Dayjs, dayjs.Dayjs] | null)
            }
          />
        </div>
      </BaseFilterCard>

      <PaymentTable
        data={filteredInvoices}
        loading={isLoading}
        search={search}
        onSearchChange={setSearch}
        onVerify={(invoice) => {
          if (!canManageInvoices) {
            showPermissionError();
            return;
          }
          setVerifyTarget(invoice);
        }}
        onReject={(invoice) => {
          if (!canManageInvoices) {
            showPermissionError();
            return;
          }
          setRejectTarget(invoice);
        }}
        onPay={(invoice) => {
          if (!canManageInvoices) {
            showPermissionError();
            return;
          }
          setPayTarget(invoice);
          setPayAmount(Number(invoice.remainingAmount || 0));
          setPayTransactionReference("");
        }}
      />
      <Modal
        title="Tạo hóa đơn nhà cung cấp"
        open={createOpen}
        onCancel={() => {
          setCreateOpen(false);
          setCreatePayload({
            goodsReceiptId: undefined,
            invoiceDate: undefined,
            dueDate: undefined,
            supplierInvoiceAmount: undefined,
            notes: "",
          });
          setDraftItems([]);
        }}
        onOk={() => void handleCreateInvoice()}
        okText="Tạo"
        confirmLoading={createMutation.isPending}
        width={980}
      >
        <div className="grid gap-3">
          <Select
            className="w-full"
            placeholder="Chọn phiếu nhập đã được duyệt"
            value={createPayload.goodsReceiptId}
            options={availableGoodsReceipts.map((receipt: GoodsReceipt) => ({
              value: receipt.receiptId,
              label: `${receipt.receiptCode} | PO: ${receipt.purchaseOrder?.orderCode || "-"}`,
            }))}
            onChange={(value) => {
              const receiptId = Number(value || 0) || undefined;
              const receipt = goodsReceipts.find(
                (item: GoodsReceipt) => item.receiptId === receiptId,
              );
              setCreatePayload((prev) => ({
                ...prev,
                goodsReceiptId: receiptId,
              }));
              buildDraftItemsFromReceipt(receipt);
            }}
          />

          {selectedReceipt && (
            <div className="rounded-xl border border-slate-200 bg-slate-50 p-3">
              <Text className="block text-xs text-slate-500">
                Phiếu nhập đã chọn
              </Text>
              <Text className="block text-sm text-slate-700">
                {selectedReceipt.receiptCode} | Đơn mua:{" "}
                {selectedReceipt.purchaseOrder?.orderCode || "-"}
              </Text>
            </div>
          )}

          <DatePicker
            className="w-full"
            placeholder="Ngày lập hóa đơn"
            value={
              createPayload.invoiceDate
                ? dayjs(createPayload.invoiceDate)
                : null
            }
            onChange={(value) =>
              setCreatePayload((prev) => ({
                ...prev,
                invoiceDate: value ? value.format("YYYY-MM-DD") : undefined,
              }))
            }
          />

          <DatePicker
            className="w-full"
            placeholder="Hạn thanh toán"
            value={createPayload.dueDate ? dayjs(createPayload.dueDate) : null}
            onChange={(value) =>
              setCreatePayload((prev) => ({
                ...prev,
                dueDate: value ? value.format("YYYY-MM-DD") : undefined,
              }))
            }
          />

          <InputNumber
            className="w-full"
            min={0.01}
            precision={2}
            placeholder="Số tiền phải thanh toán (theo NCC)"
            value={createPayload.supplierInvoiceAmount}
            onChange={(value) =>
              setCreatePayload((prev) => ({
                ...prev,
                supplierInvoiceAmount:
                  value == null ? undefined : Number(value),
              }))
            }
          />

          {invalidDateRange && (
            <Alert
              type="warning"
              showIcon
              message="Hạn thanh toán không được trước ngày hóa đơn"
            />
          )}

          <TextArea
            rows={2}
            placeholder="Ghi chú hóa đơn"
            value={createPayload.notes}
            onChange={(e) =>
              setCreatePayload((prev) => ({ ...prev, notes: e.target.value }))
            }
          />

          <Divider className="!my-1" />

          <div className="rounded-xl border border-slate-200 bg-slate-50 p-3">
            <Text className="block text-xs text-slate-500">
              Tổng hợp số tiền hóa đơn
            </Text>
            <Text className="block text-sm text-slate-700">
              Tổng số dòng: {draftItems.length}
            </Text>
            <Text className="block text-lg font-semibold text-slate-900">
              Tổng tiền hóa đơn:{" "}
              {draftInvoiceTotal.toLocaleString("vi-VN", {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
              })}
            </Text>
            <Text className="block text-sm text-slate-700">
              Số tiền theo nhà cung cấp:{" "}
              {supplierInvoiceAmount.toLocaleString("vi-VN", {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
              })}
            </Text>
            <Text className="block text-sm text-slate-700">
              "Số tiền tham chiếu (phiếu nhập hàng / đơn mua hàng)":{" "}
              {referenceTotal.toLocaleString("vi-VN", {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
              })}
            </Text>
          </div>

          <Alert
            showIcon
            type={amountMatchesLines ? "success" : "warning"}
            message={
              amountMatchesLines
                ? "Số tiền theo nhà cung cấp khớp với tổng chi tiết hóa đơn"
                : `Số tiền theo nhà cung cấp lệch so với tổng chi tiết hóa đơn: ${amountDiffWithLines.toLocaleString(
                    "vi-VN",
                    {
                      minimumFractionDigits: 2,
                      maximumFractionDigits: 2,
                    },
                  )}`
            }
          />

          <Alert
            showIcon
            type={amountMatchesReference ? "success" : "warning"}
            message={
              amountMatchesReference
                ? "Số tiền theo nhà cung cấp khớp với số tiền tham chiếu từ phiếu nhập hàng và đơn mua hàng"
                : `Số tiền theo nhà cung cấp lệch so với số tiền tham chiếu từ phiếu nhập hàng và đơn mua hàng: ${amountDiffWithReference.toLocaleString(
                    "vi-VN",
                    {
                      minimumFractionDigits: 2,
                      maximumFractionDigits: 2,
                    },
                  )}`
            }
          />

          <Text className="text-xs uppercase tracking-[0.12em] text-slate-500">
            Chi tiết hóa đơn (đối chiếu với phiếu nhập hàng)
          </Text>

          <Table<InvoiceDraftItem>
            rowKey="key"
            pagination={false}
            size="small"
            dataSource={draftItems}
            locale={{
              emptyText:
                "Vui lòng chọn phiếu nhập đã duyệt để tải các mặt hàng",
            }}
            columns={[
              {
                title: "Thuốc",
                dataIndex: "medicineName",
                width: 200,
              },
              {
                title: "Số lượng nhập",
                dataIndex: "receivedQuantity",
                width: 110,
              },
              {
                title: "Đơn giá theo đơn mua hàng",
                dataIndex: "poUnitPrice",
                width: 140,
                render: (value: number) =>
                  Number(value || 0).toLocaleString("en-US"),
              },
              {
                title: "Số lượng theo hóa đơn",
                key: "invoiceQuantity",
                width: 130,
                render: (_, record) => (
                  <InputNumber
                    className="w-full"
                    min={0}
                    value={record.invoiceQuantity}
                    onChange={(value) =>
                      setDraftItems((prev) =>
                        prev.map((item) =>
                          item.key === record.key
                            ? { ...item, invoiceQuantity: Number(value ?? 0) }
                            : item,
                        ),
                      )
                    }
                  />
                ),
              },
              {
                title: "Đơn giá theo hóa đơn",
                key: "invoiceUnitPrice",
                width: 160,
                render: (_, record) => (
                  <InputNumber
                    className="w-full"
                    min={0}
                    value={record.invoiceUnitPrice}
                    onChange={(value) =>
                      setDraftItems((prev) =>
                        prev.map((item) =>
                          item.key === record.key
                            ? { ...item, invoiceUnitPrice: Number(value ?? 0) }
                            : item,
                        ),
                      )
                    }
                  />
                ),
              },
              {
                title: "Ghi chú",
                key: "notes",
                render: (_, record) => (
                  <Input
                    value={record.notes}
                    onChange={(e) =>
                      setDraftItems((prev) =>
                        prev.map((item) =>
                          item.key === record.key
                            ? { ...item, notes: e.target.value }
                            : item,
                        ),
                      )
                    }
                  />
                ),
              },
            ]}
          />

          {draftItems.length > 0 && (
            <Alert
              type={
                compareSummary.qtyMismatch || compareSummary.priceMismatch
                  ? "warning"
                  : "success"
              }
              showIcon
              message={
                compareSummary.qtyMismatch || compareSummary.priceMismatch
                  ? `Phát hiện ${compareSummary.qtyMismatch} sai lệch về số lượng và ${compareSummary.priceMismatch} sai lệch về đơn giá trước khi xác minh`
                  : "Dữ liệu hóa đơn hiện tại khớp với giá trị tham chiếu từ phiếu nhập hàng và đơn mua hàng"
              }
            />
          )}
        </div>
      </Modal>

      <Modal
        title={`Xác minh ${verifyTarget?.invoiceCode || "hóa đơn"}`}
        open={!!verifyTarget}
        onCancel={() => setVerifyTarget(null)}
        onOk={() => void handleVerify()}
        okText="Xác minh"
        confirmLoading={verifyMutation.isPending}
      >
        <TextArea
          rows={3}
          placeholder="Ghi chú xác minh"
          value={verifyNotes}
          onChange={(e) => setVerifyNotes(e.target.value)}
        />
      </Modal>

      <Modal
        title={`Từ chối ${rejectTarget?.invoiceCode || "hóa đơn"}`}
        open={!!rejectTarget}
        onCancel={() => setRejectTarget(null)}
        onOk={() => void handleReject()}
        okText="Từ chối"
        okButtonProps={{ danger: true }}
        confirmLoading={rejectMutation.isPending}
      >
        <TextArea
          rows={3}
          placeholder="Lý do từ chối"
          value={rejectReason}
          onChange={(e) => setRejectReason(e.target.value)}
        />
      </Modal>

      <Modal
        title={`Thanh toán ${payTarget?.invoiceCode || "hóa đơn"}`}
        open={!!payTarget}
        onCancel={() => setPayTarget(null)}
        onOk={() => void handlePay()}
        okText="Xác nhận thanh toán"
        confirmLoading={payMutation.isPending}
      >
        <div className="grid gap-3">
          {payMethod === "BANK_TRANSFER" && (
            <div className="rounded-xl border border-slate-200 bg-slate-50 p-3">
              <Text className="block text-xs text-slate-500">
                Mã QR chuyển khoản
              </Text>

              {!qrValue ? (
                <Alert
                  type="warning"
                  showIcon
                  className="mt-2"
                  message="Chưa cấu hình mã QR cho nhà cung cấp này"
                />
              ) : (
                <div className="mt-3 flex flex-col items-start gap-2">
                  {isLikelyImageLink ? (
                    <img
                      key={`qr-img-${qrValue}`}
                      src={qrValue}
                      alt="QR"
                      className="h-[180px] w-[180px] object-contain rounded border border-slate-200 bg-white"
                    />
                  ) : (
                    <QRCodeSVG key={`qr-${qrValue}`} value={qrValue} size={180} level="M" />
                  )}
                  <Text className="text-xs text-slate-500">
                    {isLikelyImageLink
                      ? "Hiển thị ảnh QR từ link"
                      : "Sinh QR từ chuỗi nội dung"}
                  </Text>
                </div>
              )}
            </div>
          )}
          <InputNumber
            className="w-full"
            min={0.01}
            placeholder="Số tiền thanh toán"
            value={payAmount ?? undefined}
            onChange={(value) =>
              setPayAmount(value == null ? null : Number(value))
            }
          />
          <Select
            options={methodOptions}
            value={payMethod}
            onChange={setPayMethod}
          />
          <Input
            placeholder="Mã giao dịch (không bắt buộc)"
            value={payTransactionReference}
            onChange={(e) => setPayTransactionReference(e.target.value)}
          />
          <TextArea
            rows={3}
            placeholder="Ghi chú thanh toán"
            value={payNotes}
            onChange={(e) => setPayNotes(e.target.value)}
          />
        </div>
      </Modal>
    </MainLayout>
  );
};

export default PaymentPage;
