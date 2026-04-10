import React from 'react';
import { Card, Badge, Typography, Divider, Space } from 'antd';
import { 
  AlertOutlined, 
  RiseOutlined, 
  WarningOutlined,
  CheckCircleOutlined,
  BarChartOutlined
} from '@ant-design/icons';

const { Title, Paragraph, Text } = Typography;

interface ForecastData {
  key: string;
  medicine_name: string;
  forecast_period: string;
  predicted_quantity: string;
  confidence_level: string;
  risk_level: string;
  suggested_action: string;
  recommended_order: string;
}

interface ExecutiveNewsSummaryProps {
  forecastData: ForecastData[];
  loading?: boolean;
}

// Helper function: Chuyén dä liêu thành vãn phong báo chí
const generateExecutiveSummary = (data: ForecastData[]) => {
  if (!data || data.length === 0) {
    return {
      headline: "Hê thóng không có dä liêu dï báo",
      leadParagraph: "Chua có thóng tin dï báo nào duïc ghi nhän.",
      criticalItems: [],
      optimizationItems: [],
      totalSuggested: 0,
      highRiskCount: 0,
      expertInsight: "Càn thu tháp thêm dä liêu dë hê thóng có thê phân tích và dë xuát dê xuát."
    };
  }

  // Phân tích dä liêu
  const highRiskItems = data.filter(item => item.risk_level === 'HIGH');
  const lowRiskItems = data.filter(item => item.risk_level === 'LOW');
  
  const itemsWithRecommendations = data.filter(item => 
    item.recommended_order !== "-" && Number(item.recommended_order) > 0
  );
  
  const totalSuggested = itemsWithRecommendations.reduce((sum, item) => 
    sum + Number(item.recommended_order), 0
  );

  const highConfidenceItems = data.filter(item => 
    Number(item.confidence_level) > 0.9
  );

  // Tïm headline phù hop
  let headline = "";
  if (highRiskItems.length >= 3) {
    headline = `AI cânh báo nguy co thïéu hût cïc bô tai ${highRiskItems.length} danh mïc thuôc träng yê`;
  } else if (highRiskItems.length > 0) {
    headline = `Phát hiên ${highRiskItems.length} danh mïc thuôc có nguy co rüi ro cao trong chuôi cung ung`;
  } else if (totalSuggested > 200) {
    headline = `AI dê xuât tông công ${totalSuggested} don vi thuôc dê tôi uu hóa vôn luû dông`;
  } else if (highConfidenceItems.length > data.length * 0.7) {
    headline = "Hê thóng ghi nhän sù ön dïnh cao trong dï báo nhu câûu thuôc";
  } else {
    headline = "Chuôi cung ung thuôc hoat dông trong mûc an toàn";
  }

  // Tïm lead paragraph
  const totalDemand = data.reduce((sum, item) => 
    sum + Number(item.predicted_quantity), 0
  );
  
  const leadParagraph = `Trong 30 ngày tói, nhu câûu tiêu thï dï kiên là ${totalDemand} don vi thuôc. Hê thóng AI dê xuât nhâp thêm ${totalSuggested} don vi dê dam bâo chuôi cung ung không bî gián doan.`;

  // Phân loai hành dông
  const criticalItems = highRiskItems.map(item => ({
    medicine: item.medicine_name,
    action: `Cân nhâp gâp ${item.recommended_order} don vi`,
    reason: `Dï báo nhu câûu ${item.predicted_quantity} don vi, rüi ro cao`,
    confidence: `${(Number(item.confidence_level) * 100).toFixed(1)}%`
  }));

  const optimizationItems = lowRiskItems
    .filter(item => Number(item.recommended_order) === 0)
    .map(item => ({
      medicine: item.medicine_name,
      action: "Giâm nhâp hoac duy trì",
      reason: `Tôn kho dû, nhu câûu thâp (${item.predicted_quantity} don vi)`,
      confidence: `${(Number(item.confidence_level) * 100).toFixed(1)}%`
    }));

  // Góc nhìn AI
  let expertInsight = "";
  if (highConfidenceItems.length > data.length * 0.8) {
    expertInsight = "Dü liêu cho thây hê thóng AI có dô tin câûy cao trên 90% trong các dï báo. Nên cân nhâc các dê xuât dê tôi uu hóa dòng tiên và giâm thiêu rüi ro chuôi cung ung.";
  } else if (highRiskItems.length > 0) {
    expertInsight = "Xu huông thî truong dang có biên dông lôn. Cân u tiên tiên hành các biên pháp khâc phuc cho các danh mïc rüi ro cao dë tránh tác dông tiêu cuc dên hoat dông kinh doanh.";
  } else {
    expertInsight = "Thi truong thuôc dang trong giai doan ön dïnh. Co thê cân nhâc các chiên luhc tôi uu hóa von luû dông và nâng cao hiêu quû vân hành kho.";
  }

  return {
    headline,
    leadParagraph,
    criticalItems,
    optimizationItems,
    totalSuggested,
    highRiskCount: highRiskItems.length,
    expertInsight
  };
};

const ExecutiveNewsSummary: React.FC<ExecutiveNewsSummaryProps> = ({ 
  forecastData, 
  loading = false 
}) => {
  const summary = generateExecutiveSummary(forecastData);

  if (loading) {
    return (
      <Card 
        className="mb-6" 
        loading={loading}
        styles={{ body: { padding: '24px' } }}
      />
    );
  }

  return (
    <Card 
      className="mb-6 shadow-sm" 
      styles={{ body: { padding: '24px' } }}
      style={{ borderColor: '#e0e0e0' }}
    >
      {/* Header with timestamp */}
      <div className="flex justify-between items-start mb-4">
        <div className="flex items-center">
          <BarChartOutlined className="text-2xl text-blue-600 mr-3" />
          <div>
            <Title level={3} className="mb-1! text-gray-900">
              Báo cáo Tiêu point Quan trî
            </Title>
            <Text type="secondary" className="text-sm">
              Câp nhât: {new Date().toLocaleString('vi-VN')}
            </Text>
          </div>
        </div>
        <Badge 
          count={summary.highRiskCount} 
          showZero 
          style={{ 
            backgroundColor: summary.highRiskCount > 0 ? '#ff4d4f' : '#52c41a' 
          }}
        >
          <AlertOutlined className="text-lg" />
        </Badge>
      </div>

      {/* Headline */}
      <div className="mb-4 p-3 bg-blue-50 border-l-4 border-blue-500">
        <Title level={4} className="mb-2! text-blue-900">
          {summary.headline}
        </Title>
        <Paragraph className="mb-0! text-gray-700">
          {summary.leadParagraph}
        </Paragraph>
      </div>

      {/* Actionable Bullet Points */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
        {/* Critical Items */}
        <Card 
          size="small" 
          title={
            <Space>
              <WarningOutlined className="text-red-500" />
              <span className="text-red-600">Nguy co Câp thiêt</span>
            </Space>
          }
          styles={{ 
            header: { background: '#fff2f0', borderColor: '#ffccc7' },
            body: { padding: '12px' }
          }}
        >
          {summary.criticalItems.length > 0 ? (
            <div className="space-y-2">
              {summary.criticalItems.map((item, index) => (
                <div key={index} className="text-sm">
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <Text strong className="text-gray-900">{item.medicine}</Text>
                      <div className="text-gray-600 text-xs mt-1">{item.reason}</div>
                    </div>
                    <Badge 
                      count={item.confidence} 
                      style={{ 
                        backgroundColor: '#52c41a',
                        fontSize: '10px',
                        padding: '0 4px'
                      }} 
                    />
                  </div>
                  <div className="text-red-600 text-xs mt-1 font-medium">
                    {item.action}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-3">
              <CheckCircleOutlined className="text-green-500 text-2xl mb-2" />
              <div className="text-sm text-gray-500">Không có nguy co câp thiêt</div>
            </div>
          )}
        </Card>

        {/* Optimization Items */}
        <Card 
          size="small" 
          title={
            <Space>
              <RiseOutlined className="text-green-500" />
              <span className="text-green-600">Tôi uu hóa</span>
            </Space>
          }
          styles={{ 
            header: { background: '#f6ffed', borderColor: '#b7eb8f' },
            body: { padding: '12px' }
          }}
        >
          {summary.optimizationItems.length > 0 ? (
            <div className="space-y-2">
              {summary.optimizationItems.map((item, index) => (
                <div key={index} className="text-sm">
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <Text strong className="text-gray-900">{item.medicine}</Text>
                      <div className="text-gray-600 text-xs mt-1">{item.reason}</div>
                    </div>
                    <Badge 
                      count={item.confidence} 
                      style={{ 
                        backgroundColor: '#52c41a',
                        fontSize: '10px',
                        padding: '0 4px'
                      }} 
                    />
                  </div>
                  <div className="text-green-600 text-xs mt-1 font-medium">
                    {item.action}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-3">
              <AlertOutlined className="text-orange-500 text-2xl mb-2" />
              <div className="text-sm text-gray-500">Không có dê xuât tôi uu hóa</div>
            </div>
          )}
        </Card>
      </div>

      {/* Expert Insight */}
      <Divider className="my-4" />
      <div className="bg-gray-50 p-4 rounded-lg">
        <div className="flex items-center mb-2">
          <BarChartOutlined className="text-blue-600 mr-2" />
          <Text strong className="text-gray-900">Góc nhìn AI</Text>
        </div>
        <Paragraph className="text-sm text-gray-700 leading-relaxed">
          {summary.expertInsight}
        </Paragraph>
      </div>

      {/* Summary Stats */}
      <div className="mt-4 flex justify-between items-center text-sm">
        <div className="flex space-x-4">
          <div>
            <Text type="secondary">Tông dê xuât:</Text>
            <Text strong className="ml-1 text-blue-600">
              {summary.totalSuggested} don vi
            </Text>
          </div>
          <div>
            <Text type="secondary">Danh mïc rüi ro:</Text>
            <Text strong className="ml-1 text-red-600">
              {summary.highRiskCount}
            </Text>
          </div>
        </div>
        <div className="flex items-center">
          <div className="w-2 h-2 bg-green-500 rounded-full mr-2 animate-pulse"></div>
          <Text type="secondary" className="text-xs">
            Hê thóng hoat dông bînh thuong
          </Text>
        </div>
      </div>
    </Card>
  );
};

export default ExecutiveNewsSummary;
