import { Typography, message } from "antd";
import { useEffect, useMemo, useState } from "react";
import BatchTable from "../../../components/erp/BatchTable";
import {
  getInventoryDetail,
  type InventoryDetailGroup,
} from "../../../services/inventory";

const { Text } = Typography;

type Props = {
  medicineId: number;
};

export default function InventoryDetailModal({ medicineId }: Props) {
  const [messageApi, contextHolder] = message.useMessage();
  const [groups, setGroups] = useState<InventoryDetailGroup[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const data = await getInventoryDetail(medicineId);
        setGroups(data);
      } catch {
        messageApi.error("Không thể tải chi tiết tồn kho");
      } finally {
        setLoading(false);
      }
    };

    void load();
  }, [medicineId, messageApi]);

  const medicineName = groups[0]?.medicineName ?? "Chi tiết tồn kho";

  const totalStock = useMemo(
    () => groups.reduce((acc, g) => acc + g.totalStock, 0),
    [groups],
  );

  const rows = useMemo(
    () =>
      groups.flatMap((group) =>
        group.batches.map((batch) => ({
          batchId: batch.batchId,
          lotNumber: batch.lotNumber,
          quantity: batch.quantity,
          manufactureDate: batch.manufactureDate,
          expiryDate: batch.expiryDate,
          warehouseName: group.warehouseName,
        })),
      ),
    [groups],
  );

  return (
    <>
      {contextHolder}

      <div className="space-y-4">
        <div>
          <Text className="text-xs text-slate-400">Thuốc</Text>
          <h2 className="text-xl font-bold">{medicineName}</h2>
          <p className="text-sm text-slate-500">
            Tổng tồn kho: <strong>{totalStock.toLocaleString()}</strong>
          </p>
        </div>

        <BatchTable rows={rows} />

        {loading && (
          <p className="text-sm text-slate-400">
            Đang tải chi tiết tồn kho...
          </p>
        )}
      </div>
    </>
  );
}