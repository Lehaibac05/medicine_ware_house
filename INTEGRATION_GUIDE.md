# 📚 Hướng Dẫn Tích Hợp Mô Hình AI Vào Hệ Thống

## 📋 Tổng Quan

Tài liệu này hướng dẫn cách tích hợp mô hình AI dự báo nhu cầu dược phẩm vào hệ thống Warehouse bao gồm:
- **Backend Java**: Gọi API Flask để dự báo
- **Flask AI Server**: Cung cấp endpoints dự báo từ mô hình ML
- **Frontend**: Hiển thị kết quả dự báo (sẽ được cập nhật)

---

## 🏗️ Kiến Trúc Tích Hợp

```
┌─────────────────┐
│  Frontend React │
│  (warehouse-fe) │
└────────┬────────┘
         │ HTTP Request
         ▼
┌─────────────────────────┐
│  Backend Java Spring    │ (port 9090)
│  (warehouse-be)         │
│ ┌─────────────────────┐ │
│ │ ForecastController  │ │
│ └──────────┬──────────┘ │
│            │ RestClient│
└────────────┼────────────┘
             │ HTTP Request (:5000)
             ▼
┌─────────────────────────┐
│ Flask AI API Server     │ (port 5000)
│ (warehouse-ai/app.py)   │
│ ┌─────────────────────┐ │
│ │ DemandPredictor     │ │
│ │ InventoryAgent      │ │
│ └─────────────────────┘ │
└─────────────────────────┘
```

---

## 🚀 Hướng Dẫn Cài Đặt & Chạy

### 1️⃣ **Cài Đặt AI Model Service**

#### Bước 1: Vào thư mục warehouse-ai
```bash
cd warehouse-ai
```

#### Bước 2: Cài đặt Python dependencies
```bash
pip install -r requirements.txt
```

Hoặc nếu sử dụng virtual environment:
```bash
# Tạo venv
python -m venv venv

# Activate venv
# Windows:
venv\Scripts\activate
# Linux/Mac:
source venv/bin/activate

# Cài dependencies
pip install -r requirements.txt
```

#### Bước 3: Chạy Flask API Server
**Windows:**
```bash
start-ai-server.bat
```

**Linux/Mac:**
```bash
bash start-ai-server.sh
```

**Hoặc chạy trực tiếp:**
```bash
python app.py
```

**Kết quả mong đợi:**
```
🚀 Bắt đầu AI Model API Server trên 0.0.0.0:5000
 * Running on http://0.0.0.0:5000
```

### 2️⃣ **Cấu Hình Backend Java**

#### Bước 1: Cập nhật `application.properties`
Đã tự động cấu hình trong:
```properties
ai.service.url=http://localhost:5000
ai.service.timeout=30000
```

**Nếu AI Server chạy trên server khác:**
```properties
ai.service.url=http://<your-server-ip>:5000
```

#### Bước 2: Rebuild Backend
```bash
cd warehouse-be
mvn clean install
```

#### Bước 3: Chạy Backend
```bash
# Development
mvn spring-boot:run

# Hoặc chạy JAR trực tiếp
mvn clean package
java -jar target/warehouse-0.0.1-SNAPSHOT.jar
```

**Kết quả mong đợi** (port 9090):
```
🚀 Started Application in X.XXX seconds
```

---

## 📡 API Endpoints

### ✅ AI Server Endpoints (Flask)

#### 1. Health Check
```http
GET http://localhost:5000/api/health

Response:
{
    "status": "healthy",
    "timestamp": "2026-03-26T10:30:00",
    "service": "Warehouse AI Model API",
    "models_loaded": true
}
```

#### 2. Dự Báo Nhu Cầu (Single)
```http
POST http://localhost:5000/api/predict

Request Body:
{
    "medicineId": 1,
    "medicineName": "Paracetamol 500mg",
    "region": "Bắc",
    "temperature": 28,
    "fluSeason": 1,
    "rain": 0,
    "isHoliday": 0,
    "isWeekend": 0,
    "salesLag1": 45,
    "salesLag7": 48,
    "salesLag30": 45,
    "currentInventory": 50,
    "storageCondition": "Room temperature"
}

Response:
{
    "medicineId": 1,
    "medicineName": "Paracetamol 500mg",
    "region": "Bắc",
    "predictedQuantity": 52.3,
    "confidenceLevel": 0.89,
    "period": "Daily",
    "lowerBound": 42.0,
    "upperBound": 62.6,
    "recommendedOrder": 75,
    "currentInventory": 50,
    "warning": null,
    "timestamp": "2026-03-26T10:30:00"
}
```

#### 3. Dự Báo Hàng Loạt
```http
POST http://localhost:5000/api/predict/batch

Request Body:
{
    "predictions": [
        {
            "medicineId": 1,
            "medicineName": "Paracetamol 500mg",
            ...
        },
        {
            "medicineId": 2,
            "medicineName": "Amoxicillin 250mg",
            ...
        }
    ]
}

Response:
{
    "total": 2,
    "results": [
        {
            "medicineId": 1,
            "medicineName": "Paracetamol 500mg",
            "predictedQuantity": 52.3,
            "confidenceLevel": 0.89,
            "lowerBound": 42.0,
            "upperBound": 62.6,
            "recommendedOrder": 75,
            "status": "success"
        },
        ...
    ],
    "timestamp": "2026-03-26T10:30:00"
}
```

#### 4. Huấn Luyện Mô Hình
```http
POST http://localhost:5000/api/train

Request Body:
{
    "dataPath": "../data/pharmacy_training_final.csv"
}

Response:
{
    "status": "success",
    "message": "Model trained successfully",
    "dataRows": 15000,
    "uniqueMedicines": 120,
    "timestamp": "2026-03-26T10:30:00"
}
```

#### 5. Thông Tin Mô Hình
```http
GET http://localhost:5000/api/model/info

Response:
{
    "modelName": "Hybrid Random Forest + XGBoost",
    "version": "1.0",
    "accuracy": 0.89,
    "lastUpdated": "2026-03-26T10:30:00",
    "features": [
        "medicine_name",
        "region",
        "storage_condition",
        ...
    ],
    "predictions": ["1-day", "1-week", "1-month", "3-month"]
}
```

### ✅ Backend Java Endpoints (Spring Boot)

#### 1. Health Check
```http
GET http://localhost:9090/api/forecast/health

Response:
{
    "status": "healthy",
    "aiServiceAvailable": true,
    "timestamp": "2026-03-26T10:30:00"
}
```

#### 2. Dự Báo Nhu Cầu (Backend gọi AI Server)
```http
POST http://localhost:9090/api/forecast/predict

Request Body:
{
    "medicineId": 1,
    "medicineName": "Paracetamol 500mg",
    "region": "Bắc",
    "temperature": 28,
    "fluSeason": 1,
    "rain": 0,
    ...
}

Response:
(Kết quả từ AI Server, có thể có fallback dự báo)
```

#### 3. Dự Báo Hàng Loạt (Backend)
```http
POST http://localhost:9090/api/forecast/predict-batch

Request Body:
{
    "predictions": [...]
}
```

#### 4. Huấn Luyện (Backend)
```http
POST http://localhost:9090/api/forecast/train

Request Body:
{
    "dataPath": "../data/pharmacy_training_final.csv"
}
```

#### 5. Thông Tin Mô Hình (Backend)
```http
GET http://localhost:9090/api/forecast/model/info
```

---

## 🧪 Test Integration

### Test 1: Kiểm Tra AI Server
```bash
curl http://localhost:5000/api/health
```

### Test 2: Kiểm Tra Backend
```bash
curl http://localhost:9090/api/forecast/health
```

### Test 3: Dự Báo Thực Tế
```bash
curl -X POST http://localhost:9090/api/forecast/predict \
  -H "Content-Type: application/json" \
  -d '{
    "medicineId": 1,
    "medicineName": "Paracetamol 500mg",
    "region": "Bắc",
    "temperature": 28,
    "fluSeason": 1,
    "rain": 0,
    "currentInventory": 50,
    "salesLag1": 45,
    "salesLag7": 48,
    "salesLag30": 45
  }'
```

---

## 🔄 Fallback Mechanism

Nếu AI Server không khả dụng, backend sẽ:
1. ✅ Trả về dự báo fallback dựa trên trung bình di động
2. ✅ Đánh dấu `isFallback: true` và `confidence` giảm xuống 0.65
3. ✅ Log cảnh báo để monitoring
4. ✅ Tiếp tục hoạt động mà không crash

```json
{
    "predictedQuantity": 42.5,
    "confidenceLevel": 0.65,
    "warning": "⚠️ AI Service không khả dụng, sử dụng dự báo fallback",
    "isFallback": true
}
```

---

## 📊 Data Flow Example

### Scenario: Dự báo nhu cầu Paracetamol

1. **Frontend gửi request:**
   ```
   POST /api/forecast/predict
   {body}
   ```

2. **Backend nhận request, gọi AIModelService:**
   ```java
   Map<String, Object> prediction = aiModelService.predictDemand(request);
   ```

3. **AIModelService gọi Flask:**
   ```http
   POST http://localhost:5000/api/predict
   {request}
   ```

4. **Flask nhận request, load model:**
   ```python
   predictor = DemandPredictor()
   prediction = predictor.predict(medicine_name, region, features)
   ```

5. **Flask trả kết quả:**
   ```json
   {
       "predictedQuantity": 52.3,
       "confidenceLevel": 0.89,
       ...
   }
   ```

6. **Backend trả kết quả cho Frontend:**
   ```json
   {response từ Flask}
   ```

---

## 🛠️ Troubleshooting

### ❌ Error: "AI Service not available"
**Giải pháp:**
```bash
# 1. Kiểm tra Flask server chạy:
http://localhost:5000/api/health

# 2. Kiểm tra config application.properties:
ai.service.url=http://localhost:5000

# 3. Kiểm tra firewall không block port 5000
```

### ❌ Error: "Connection refused"
**Giải pháp:**
```bash
# 1. Kiểm tra Flask server đang chạy
# 2. Kiểm tra port 5000 không bị chiếm
netstat -an | grep 5000

# 3. Restart Flask server
```

### ❌ Error: "Model not loaded"
**Giải pháp:**
```bash
# 1. Kiểm tra file dữ liệu tồn tại
ls -la data/pharmacy_training_final.csv

# 2. Huấn luyện lại model:
POST http://localhost:5000/api/train
```

### ❌ Error: "Python dependencies missing"
**Giải pháp:**
```bash
# 1. Reinstall dependencies:
pip install --upgrade -r requirements.txt

# 2. Hoặc xóa cache:
pip install --no-cache-dir -r requirements.txt
```

---

## 📈 Performance Tips

### 1. **Batch Processing**
Thay vì dự báo từng thuốc, sử dụng batch endpoint:
```http
POST /api/predict/batch
```
Giảm overhead từ 100 request xuống 1 request.

### 2. **Model Caching**
Mô hình được load vào memory khi server khởi động, không reload mỗi request.

### 3. **Timeout Configuration**
```properties
ai.service.timeout=30000  # 30 giây
```

---

## 🔐 Deployment

### Docker (Optional)

**Dockerfile cho AI Server:**
```dockerfile
FROM python:3.9-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install -r requirements.txt

COPY . .

EXPOSE 5000

CMD ["python", "app.py"]
```

**Build & Run:**
```bash
docker build -t warehouse-ai:1.0 .
docker run -p 5000:5000 warehouse-ai:1.0
```

---

## ✅ Checklist Tích Hợp

- [ ] Cài đặt Python dependencies
- [ ] Flask server chạy trên port 5000
- [ ] Backend config `ai.service.url` = `http://localhost:5000`
- [ ] Backend rebuild & restart
- [ ] Test `/api/health` endpoints
- [ ] Test `/api/forecast/predict` endpoint
- [ ] Verify dữ liệu dự báo chính xác
- [ ] Monitoring logs để phát hiện lỗi early
- [ ] Cấu hình firewall nếu cần
- [ ] Document AI service URL cho team

---

## 📞 Support

Nếu gặp vấn đề, kiểm tra:
1. Logs từ Flask: `warehouse-ai/app.py output`
2. Logs từ Backend: `target/warehouse-*.jar console output`
3. Verify dữ liệu input request
4. Kiểm tra database connection

---

**Last Updated:** 2026-03-26  
**Version:** 1.0
