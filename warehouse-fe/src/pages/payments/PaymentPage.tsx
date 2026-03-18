import { useMemo, useState } from "react";
import {
  Alert,
  Button,
  DatePicker,
  Divider,
  Input,
  InputNumber,
  Layout,
  Modal,
  Select,
  Space,
  Table,
  Typography,
  message,
} from "antd";
import dayjs from "dayjs";
import { AxiosError } from "axios";
import SidebarNav from "../../layouts/SidebarNav";
import TopBar from "../../layouts/TopBar";
import BaseFilterCard from "../../components/base/BaseFilterCard";
import PaymentTable from "./components/PaymentTable";
import BaseStatsGrid from "../../components/base/BaseStatsGrid";
import {
  useCreateSupplierInvoiceMutation,
  useGoodsReceiptsQuery,
  usePaySupplierInvoiceMutation,
  useRejectSupplierInvoiceMutation,
  useSupplierInvoicesQuery,
  useVerifySupplierInvoiceMutation,
} from "../../hooks/useWorkflow";
import type { GoodsReceipt, SupplierInvoice } from "../../services/workflow";
import { getPrimaryRole, getRoleLabel, hasAnyRole } from "../../utils/auth";

const { Content, Sider } = Layout;
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
  { value: "all", label: "All status" },
  { value: "PENDING_VERIFICATION", label: "PENDING_VERIFICATION" },
  { value: "VERIFIED", label: "VERIFIED" },
  { value: "PARTIALLY_PAID", label: "PARTIALLY_PAID" },
  { value: "PAID", label: "PAID" },
  { value: "REJECTED", label: "REJECTED" },
];

const methodOptions = [
  { value: "BANK_TRANSFER", label: "Bank Transfer" },
  { value: "CASH", label: "Cash" },
  { value: "CREDIT_CARD", label: "Credit Card" },
  { value: "E_WALLET", label: "E-Wallet" },
];

const paymentTermOptions = [
  { value: "DUE_ON_RECEIPT", label: "Due on receipt (thanh toan ngay khi nhan hoa don)" },
  { value: "NET_7", label: "Net 7 (thanh toan trong 7 ngay)" },
  { value: "NET_15", label: "Net 15 (thanh toan trong 15 ngay)" },
  { value: "NET_30", label: "Net 30 (thanh toan trong 30 ngay)" },
  { value: "CUSTOM", label: "Custom" },
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
    paymentTerms: "NET_30",
    customPaymentTerms: "",
    notes: "",
  });
  const [draftItems, setDraftItems] = useState<InvoiceDraftItem[]>([]);

  const [verifyTarget, setVerifyTarget] = useState<SupplierInvoice | null>(null);
  const [verifyNotes, setVerifyNotes] = useState("");
  const [rejectTarget, setRejectTarget] = useState<SupplierInvoice | null>(null);
  const [rejectReason, setRejectReason] = useState("");
  const [payTarget, setPayTarget] = useState<SupplierInvoice | null>(null);
  const [payAmount, setPayAmount] = useState<number | null>(null);
  const [payMethod, setPayMethod] = useState("BANK_TRANSFER");
  const [payTransactionReference, setPayTransactionReference] = useState("");
  const [payNotes, setPayNotes] = useState("");

  const canManageInvoices = hasAnyRole(["ROLE_ACCOUNTANT"]);
  const currentRoleLabel = getRoleLabel(getPrimaryRole());

  const showPermissionError = () => {
    messageApi.error(`403 Forbidden: only Accountant can create/verify/reject/pay invoice. Current role: ${currentRoleLabel}`);
  };

  const { data: invoices = [], isLoading } = useSupplierInvoicesQuery();
  const { data: goodsReceipts = [] } = useGoodsReceiptsQuery();
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
        return !date.isBefore(range[0].startOf("day")) && !date.isAfter(range[1].endOf("day"));
      });
  }, [invoices, search, status, range]);

  const paymentStats = useMemo(() => {
    const totalInvoices = invoices.length;
    const totalValue = invoices.reduce((sum, row) => sum + Number(row.totalAmount || 0), 0);
    const paidValue = invoices.reduce((sum, row) => sum + Number(row.paidAmount || 0), 0);
    const pendingVerification = invoices.filter(
      (row) => (row.status || "").toUpperCase() === "PENDING_VERIFICATION"
    ).length;
    const mismatchCount = invoices.filter((row) => !!row.hasMismatch).length;

    return [
      {
        label: "Total invoices",
        value: totalInvoices,
        note: `${filteredInvoices.length} invoices in current view`,
      },
      {
        label: "Invoice value",
        value: totalValue.toLocaleString("en-US"),
        note: "Sum of invoice totals",
      },
      {
        label: "Paid amount",
        value: paidValue.toLocaleString("en-US"),
        note: `${pendingVerification} waiting verification`,
      },
      {
        label: "Mismatch warnings",
        value: mismatchCount,
        note: "Must be fixed before verify",
      },
    ];
  }, [invoices, filteredInvoices.length]);

  const normalizeStatus = (status?: string) =>
    status?.trim().toUpperCase().replace(/[\s-]+/g, "_") ?? "";

  const existingInvoiceReceiptIds = useMemo(
    () => new Set(invoices.map((invoice) => invoice.goodsReceipt?.receiptId).filter((id): id is number => !!id)),
    [invoices]
  );

  const availableGoodsReceipts = useMemo(
    () =>
      goodsReceipts.filter(
        (receipt) =>
          normalizeStatus(receipt.status) === "APPROVED" && !existingInvoiceReceiptIds.has(receipt.receiptId)
      ),
    [goodsReceipts, existingInvoiceReceiptIds]
  );

  const selectedReceipt = useMemo(
    () => goodsReceipts.find((receipt) => receipt.receiptId === createPayload.goodsReceiptId),
    [goodsReceipts, createPayload.goodsReceiptId]
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

        const receivedQty = Number(item.receivedQuantity ?? item.requestedQuantity ?? 0);
        const poUnitPrice = Number(item.unitPrice ?? 0);
        return {
          key: String(medicineId),
          medicineId,
          medicineName: item.medicine?.medicineName || `Medicine #${medicineId}`,
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
    const qtyMismatch = draftItems.filter((item) => item.invoiceQuantity !== item.receivedQuantity).length;
    const priceMismatch = draftItems.filter((item) => item.invoiceUnitPrice !== item.poUnitPrice).length;
    return { qtyMismatch, priceMismatch };
  }, [draftItems]);

  const draftInvoiceTotal = useMemo(
    () => draftItems.reduce((sum, item) => sum + Number(item.invoiceQuantity || 0) * Number(item.invoiceUnitPrice || 0), 0),
    [draftItems]
  );

  const referenceTotal = useMemo(
    () =>
      draftItems.reduce(
        (sum, item) => sum + Number(item.receivedQuantity || 0) * Number(item.poUnitPrice || 0),
        0
      ),
    [draftItems]
  );

  const supplierInvoiceAmount = Number(createPayload.supplierInvoiceAmount || 0);
  const amountDiffWithLines = supplierInvoiceAmount - draftInvoiceTotal;
  const amountDiffWithReference = supplierInvoiceAmount - referenceTotal;
  const amountMatchesLines = Math.abs(amountDiffWithLines) < 0.01;
  const amountMatchesReference = Math.abs(amountDiffWithReference) < 0.01;

  const invoiceDateObj = createPayload.invoiceDate ? dayjs(createPayload.invoiceDate) : null;
  const dueDateObj = createPayload.dueDate ? dayjs(createPayload.dueDate) : null;
  const invalidDateRange = !!invoiceDateObj && !!dueDateObj && dueDateObj.isBefore(invoiceDateObj, "day");

  const handleCreateInvoice = async () => {
    if (!createPayload.goodsReceiptId) {
      messageApi.error("Please select an approved goods receipt");
      return;
    }

    if (!draftItems.length) {
      messageApi.error("No invoice items found from selected goods receipt");
      return;
    }

    if (!createPayload.supplierInvoiceAmount || createPayload.supplierInvoiceAmount <= 0) {
      messageApi.error("Please input supplier invoice amount");
      return;
    }

    if (invalidDateRange) {
      messageApi.error("Due date must be on or after invoice date");
      return;
    }

    if (!amountMatchesLines) {
      messageApi.error("Supplier invoice amount must match total of invoice lines");
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
      messageApi.error("Invoice must contain at least one valid item");
      return;
    }

    try {
      const selectedPaymentTerms =
        createPayload.paymentTerms === "CUSTOM"
          ? (createPayload.customPaymentTerms || "Custom payment terms").trim()
          : paymentTermOptions.find((item) => item.value === createPayload.paymentTerms)?.label || createPayload.paymentTerms;

      const mergedNotes = [
        createPayload.notes?.trim(),
        `Supplier stated payable amount: ${supplierInvoiceAmount.toLocaleString("en-US", {
          minimumFractionDigits: 2,
          maximumFractionDigits: 2,
        })}`,
        `Payment terms: ${selectedPaymentTerms}`,
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
        paymentTerms: "NET_30",
        customPaymentTerms: "",
        notes: "",
      });
      setDraftItems([]);
      messageApi.success("Supplier invoice created");
    } catch (error) {
      if (error instanceof AxiosError && error.response?.status === 403) {
        showPermissionError();
        return;
      }
      messageApi.error("Failed to create supplier invoice");
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
      messageApi.success("Invoice rejected");
    } catch (error) {
      if (error instanceof AxiosError && error.response?.status === 403) {
        showPermissionError();
        return;
      }
      messageApi.error("Failed to reject invoice");
    }
  };

  const handlePay = async () => {
    if (!payTarget || !payAmount || payAmount <= 0) {
      messageApi.error("Please enter valid amount");
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
      messageApi.success("Payment recorded");
    } catch (error) {
      if (error instanceof AxiosError && error.response?.status === 403) {
        showPermissionError();
        return;
      }
      messageApi.error("Failed to process payment");
    }
  };

  return (
   <Layout className="min-h-screen bg-slate-100">
      {contextHolder}
      <Sider
        width={260}
        className="hidden lg:block !bg-white border-r border-slate-200 px-4 py-6 !fixed left-0 top-0 h-screen"
      >
        <SidebarNav />
      </Sider>
      <Layout className="lg:ml-[260px]">
        <div className="fixed left-0 top-0 z-20 w-full lg:pl-[260px]">
          <TopBar title="Supplier Invoices" subtitle="Procurement Finance" />
        </div>
        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          {!canManageInvoices && (
            <Alert
              type="warning"
              showIcon
              message={`You can view invoices only. Role ${currentRoleLabel} does not have permission to create/verify/reject/pay.`}
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
                  Create Invoice
                </Button>
                <Button
                  className="h-[40px]"
                  onClick={() => {
                    setSearch("");
                    setStatus("all");
                    setRange(null);
                  }}
                >
                  Reset
                </Button>
              </Space>
            }
          >
            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Invoice / Supplier</Text>
              <Input
                placeholder="Enter invoice code or supplier"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Process Status</Text>
              <Select options={statusOptions} value={status} onChange={setStatus} />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Payment Method (for modal)</Text>
              <Select options={methodOptions} value={payMethod} onChange={setPayMethod} />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Invoice Date Range</Text>
              <RangePicker
                className="w-full"
                format="DD/MM/YYYY"
                placeholder={["Start date", "End date"]}
                value={range}
                onChange={(values) => setRange(values as [dayjs.Dayjs, dayjs.Dayjs] | null)}
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
        </Content>
      </Layout>

      <Modal
        title="Create Supplier Invoice"
        open={createOpen}
        onCancel={() => {
          setCreateOpen(false);
          setCreatePayload({
            goodsReceiptId: undefined,
            invoiceDate: undefined,
            dueDate: undefined,
            supplierInvoiceAmount: undefined,
            paymentTerms: "NET_30",
            customPaymentTerms: "",
            notes: "",
          });
          setDraftItems([]);
        }}
        onOk={() => void handleCreateInvoice()}
        okText="Create"
        confirmLoading={createMutation.isPending}
        width={980}
      >
        <div className="grid gap-3">


          <Select
            className="w-full"
            placeholder="Select approved goods receipt"
            value={createPayload.goodsReceiptId}
            options={availableGoodsReceipts.map((receipt) => ({
              value: receipt.receiptId,
              label: `${receipt.receiptCode} | PO: ${receipt.purchaseOrder?.orderCode || "-"}`,
            }))}
            onChange={(value) => {
              const receiptId = Number(value || 0) || undefined;
              const receipt = goodsReceipts.find((item) => item.receiptId === receiptId);
              setCreatePayload((prev) => ({ ...prev, goodsReceiptId: receiptId }));
              buildDraftItemsFromReceipt(receipt);
            }}
          />

          {selectedReceipt && (
            <div className="rounded-xl border border-slate-200 bg-slate-50 p-3">
              <Text className="block text-xs text-slate-500">Selected Goods Receipt</Text>
              <Text className="block text-sm text-slate-700">
                {selectedReceipt.receiptCode} | PO: {selectedReceipt.purchaseOrder?.orderCode || "-"}
              </Text>
            </div>
          )}

          <DatePicker
            className="w-full"
            placeholder="Invoice Date"
            value={createPayload.invoiceDate ? dayjs(createPayload.invoiceDate) : null}
            onChange={(value) =>
              setCreatePayload((prev) => ({
                ...prev,
                invoiceDate: value ? value.format("YYYY-MM-DD") : undefined,
              }))
            }
          />

          <DatePicker
            className="w-full"
            placeholder="Due Date"
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
            placeholder="Supplier invoice amount (so tien phai tra tren hoa don NCC)"
            value={createPayload.supplierInvoiceAmount}
            onChange={(value) =>
              setCreatePayload((prev) => ({
                ...prev,
                supplierInvoiceAmount: value == null ? undefined : Number(value),
              }))
            }
          />

          <Select
            className="w-full"
            placeholder="Payment terms"
            value={createPayload.paymentTerms}
            options={paymentTermOptions}
            onChange={(value) =>
              setCreatePayload((prev) => ({
                ...prev,
                paymentTerms: value,
                customPaymentTerms: value === "CUSTOM" ? prev.customPaymentTerms : "",
              }))
            }
          />

          {createPayload.paymentTerms === "CUSTOM" && (
            <Input
              placeholder="Enter custom payment terms"
              value={createPayload.customPaymentTerms}
              onChange={(e) =>
                setCreatePayload((prev) => ({ ...prev, customPaymentTerms: e.target.value }))
              }
            />
          )}

          {invalidDateRange && (
            <Alert
              type="warning"
              showIcon
              message="Due date is earlier than invoice date"
            />
          )}

          <TextArea
            rows={2}
            placeholder="Invoice notes"
            value={createPayload.notes}
            onChange={(e) => setCreatePayload((prev) => ({ ...prev, notes: e.target.value }))}
          />

          <Divider className="!my-1" />

          <div className="rounded-xl border border-slate-200 bg-slate-50 p-3">
            <Text className="block text-xs text-slate-500">Invoice Amount Summary</Text>
            <Text className="block text-sm text-slate-700">Total lines: {draftItems.length}</Text>
            <Text className="block text-lg font-semibold text-slate-900">
              Total invoice amount: {draftInvoiceTotal.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </Text>
            <Text className="block text-sm text-slate-700">
              Supplier stated amount: {supplierInvoiceAmount.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </Text>
            <Text className="block text-sm text-slate-700">
              Reference amount (GR/PO): {referenceTotal.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </Text>
          </div>

          <Alert
            showIcon
            type={amountMatchesLines ? "success" : "warning"}
            message={
              amountMatchesLines
                ? "Supplier amount matches invoice line total"
                : `Supplier amount differs from invoice line total by ${amountDiffWithLines.toLocaleString("en-US", {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2,
                  })}`
            }
          />

          <Alert
            showIcon
            type={amountMatchesReference ? "success" : "warning"}
            message={
              amountMatchesReference
                ? "Supplier amount matches Goods Receipt/PO reference amount"
                : `Supplier amount differs from Goods Receipt/PO reference by ${amountDiffWithReference.toLocaleString("en-US", {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2,
                  })}`
            }
          />

          <Text className="text-xs uppercase tracking-[0.12em] text-slate-500">Invoice Items (doi chieu voi Goods Receipt)</Text>

          <Table<InvoiceDraftItem>
            rowKey="key"
            pagination={false}
            size="small"
            dataSource={draftItems}
            locale={{ emptyText: "Select an approved goods receipt to load items" }}
            columns={[
              {
                title: "Medicine",
                dataIndex: "medicineName",
                width: 200,
              },
              {
                title: "GR Qty",
                dataIndex: "receivedQuantity",
                width: 110,
              },
              {
                title: "PO Unit Price",
                dataIndex: "poUnitPrice",
                width: 140,
                render: (value: number) => Number(value || 0).toLocaleString("en-US"),
              },
              {
                title: "Invoice Qty",
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
                            : item
                        )
                      )
                    }
                  />
                ),
              },
              {
                title: "Invoice Unit Price",
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
                            : item
                        )
                      )
                    }
                  />
                ),
              },
              {
                title: "Item Notes",
                key: "notes",
                render: (_, record) => (
                  <Input
                    value={record.notes}
                    onChange={(e) =>
                      setDraftItems((prev) =>
                        prev.map((item) =>
                          item.key === record.key ? { ...item, notes: e.target.value } : item
                        )
                      )
                    }
                  />
                ),
              },
            ]}
          />

          {draftItems.length > 0 && (
            <Alert
              type={compareSummary.qtyMismatch || compareSummary.priceMismatch ? "warning" : "success"}
              showIcon
              message={
                compareSummary.qtyMismatch || compareSummary.priceMismatch
                  ? `Detected ${compareSummary.qtyMismatch} quantity mismatch and ${compareSummary.priceMismatch} unit-price mismatch before verification`
                  : "Current invoice data matches Goods Receipt and PO reference values"
              }
            />
          )}
        </div>
      </Modal>

      <Modal
        title={`Verify ${verifyTarget?.invoiceCode || "invoice"}`}
        open={!!verifyTarget}
        onCancel={() => setVerifyTarget(null)}
        onOk={() => void handleVerify()}
        okText="Verify"
        confirmLoading={verifyMutation.isPending}
      >
        <TextArea
          rows={3}
          placeholder="Verification notes"
          value={verifyNotes}
          onChange={(e) => setVerifyNotes(e.target.value)}
        />
      </Modal>

      <Modal
        title={`Reject ${rejectTarget?.invoiceCode || "invoice"}`}
        open={!!rejectTarget}
        onCancel={() => setRejectTarget(null)}
        onOk={() => void handleReject()}
        okText="Reject"
        okButtonProps={{ danger: true }}
        confirmLoading={rejectMutation.isPending}
      >
        <TextArea
          rows={3}
          placeholder="Rejection reason"
          value={rejectReason}
          onChange={(e) => setRejectReason(e.target.value)}
        />
      </Modal>

      <Modal
        title={`Pay ${payTarget?.invoiceCode || "invoice"}`}
        open={!!payTarget}
        onCancel={() => setPayTarget(null)}
        onOk={() => void handlePay()}
        okText="Confirm Payment"
        confirmLoading={payMutation.isPending}
      >
        <div className="grid gap-3">
          <InputNumber
            className="w-full"
            min={0.01}
            placeholder="Payment amount"
            value={payAmount ?? undefined}
            onChange={(value) => setPayAmount(value == null ? null : Number(value))}
          />
          <Select options={methodOptions} value={payMethod} onChange={setPayMethod} />
          <Input
            placeholder="Transaction reference (optional)"
            value={payTransactionReference}
            onChange={(e) => setPayTransactionReference(e.target.value)}
          />
          <TextArea
            rows={3}
            placeholder="Payment notes"
            value={payNotes}
            onChange={(e) => setPayNotes(e.target.value)}
          />
        </div>
      </Modal>
    </Layout>
  );
};

export default PaymentPage;
