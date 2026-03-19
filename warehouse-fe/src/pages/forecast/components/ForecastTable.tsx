import type { ColumnsType } from "antd/es/table";
import BaseTable from "../../../components/base/BaseTable";
import { Flex, Typography } from "antd";

const { Text } = Typography;

type ForecastRow = {
  key: string;
  medicine_name: string;
  forecast_period: string;
  predicted_quantity: string;
  confidence_level: string;
  risk_level: string;
  suggested_action: string;
};

const data: ForecastRow[] = [
  {
    key: "FC-001",
    medicine_name: "Paracetamol 500mg",
    forecast_period: "Q1 2026",
    predicted_quantity: "5,200 units",
    confidence_level: "92%",
    risk_level: "Low",
    suggested_action: "Maintain current stock level",
  },
  {
    key: "FC-002",
    medicine_name: "Amoxicillin 250mg",
    forecast_period: "Q1 2026",
    predicted_quantity: "1,150 units",
    confidence_level: "85%",
    risk_level: "Medium",
    suggested_action: "Increase stock by 10%",
  },
  {
    key: "FC-003",
    medicine_name: "Insulin Glargine",
    forecast_period: "Q1 2026",
    predicted_quantity: "780 units",
    confidence_level: "88%",
    risk_level: "High",
    suggested_action: "Urgent restock required",
  },
  {
    key: "FC-004",
    medicine_name: "Vitamin C 500mg",
    forecast_period: "Q1 2026",
    predicted_quantity: "6,900 units",
    confidence_level: "90%",
    risk_level: "Low",
    suggested_action: "Monitor demand trend",
  },
];

const columns: ColumnsType<ForecastRow> = [
  {
    title: "Medicine",
    dataIndex: "medicine_name",
    key: "medicine_name",
  },
  {
    title: "Forecast Period",
    dataIndex: "forecast_period",
    key: "forecast_period",
  },
  {
    title: "Predicted Quantity",
    dataIndex: "predicted_quantity",
    key: "predicted_quantity",
  },
  {
    title: "Confidence",
    dataIndex: "confidence_level",
    key: "confidence_level",
  },
  {
    title: "Risk Level",
    dataIndex: "risk_level",
    key: "risk_level",
  },
  {
    title: "Suggested Action",
    dataIndex: "suggested_action",
    key: "suggested_action",
  },
];

function ForecastTable() {
  const tableHeader = (
    <Flex justify="space-between" align="center">
      <div className="flex flex-col">
        <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
          Forecast
        </Text>
      </div>
      {/* <div className="w-[200px]">
        <Input.Search
          placeholder="Search by payment ID..."
          className="w-[320px]"
          allowClear
          // onSearch={onSearch}
          // onChange={(e) => !e.target.value && onSearch("")}
        />
      </div> */}
    </Flex>
  );
  return (
    <BaseTable title={() => tableHeader} columns={columns} dataSource={data} />
  );
}

export default ForecastTable;
