# Warehouse Management System with AI Forecasting

Hệ thống quản lý kho thuốc thông minh tích hợp AI dự báo nhu cầu sử dụng Random Forest.

## 🚀 Tính năng chính

### AI Demand Forecasting
- **Mô hình Random Forest**: Dự báo nhu cầu thuốc với độ chính xác cao
- **Features**: Historical sales, weather data, seasonality, medicine attributes
- **Performance**: R² = 0.89, MAE = 4.47
- **Real-time predictions**: Với confidence intervals

### Warehouse Management
- Quản lý thuốc, lô hàng, đơn hàng
- Hệ thống cảnh báo tự động
- Workflow phê duyệt yêu cầu thuốc
- Báo cáo và thống kê

## 🛠️ Tech Stack

- **Backend**: Spring Boot (Java 17)
- **Frontend**: React + TypeScript + Vite
- **Database**: MySQL
- **AI/ML**: Python + Scikit-learn
- **UI**: Ant Design

## 📦 Cài đặt và chạy

### 1. Yêu cầu hệ thống
- Java 17+
- Node.js 18+
- Python 3.8+
- MySQL 8.0+
- Maven 3.6+

### 2. Clone và setup
```bash
git clone <repository>
cd warehouse
```

### 3. Database Setup
```sql
CREATE DATABASE pharmacy_warehouse;
-- Import existing data or run migrations
```

### 4. AI Model Training
```bash
cd warehouse-ai
pip install -r requirements.txt
python main.py
```

### 5. Backend Setup
```bash
cd warehouse-be
mvn clean install
mvn spring-boot:run
```

### 6. Frontend Setup
```bash
cd warehouse-fe
npm install
npm run dev
```

### 7. Quick Start (Windows)
```bash
# Double-click or run:
start-system.bat
```

## 🔧 API Endpoints

### Forecast API
- `GET /api/forecast` - Lấy tất cả dự báo
- `POST /api/forecast/predict` - Dự báo nhu cầu cho thuốc
- `GET /api/forecast/medicine/{id}` - Dự báo theo thuốc

### Warehouse APIs
- Medicines, Orders, Batches, Alerts, etc.

## 📊 AI Model Details

### Features Used
- `sales_lag_1/7/30`: Historical sales (1, 7, 30 days ago)
- `temperature`: Weather data
- `flu_season`: Seasonal flu indicator
- `rain`: Weather condition
- `manufacturer_*`: One-hot encoded manufacturer
- `month_*`: Seasonal features
- `day_of_week`: Weekly patterns

### Model Performance
- **Training MAE**: 3.89
- **Test MAE**: 4.47
- **Test RMSE**: 6.68
- **R² Score**: 0.89

### Feature Importance
1. sales_lag_1 (23.3%)
2. sales_lag_7 (23.2%)
3. sales_lag_30 (21.3%)
4. temperature (15.1%)

## 🎯 Cách sử dụng

1. **Train Model**: Chạy `python main.py` trong `warehouse-ai/`
2. **View Forecasts**: Truy cập `/forecast` trong frontend
3. **Real-time Prediction**: API tự động predict khi có request
4. **Monitor**: Dashboard hiển thị risk levels và recommendations

## 📝 License

This project is licensed under the MIT License.