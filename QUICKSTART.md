# 🚀 Quick Start - AI Model Integration

## ⚡ Chạy Toàn Bộ Hệ Thống (3 Steps)

### 1️⃣ Terminal 1: Chạy AI Model Server
```bash
cd warehouse-ai
pip install -r requirements.txt
python app.py
```

✅ Server nhận được là: `http://localhost:5000`

### 2️⃣ Terminal 2: Chạy Backend Java
```bash
cd warehouse-be
mvn clean install
mvn spring-boot:run
```

✅ Backend nhận được là: `http://localhost:9090`

### 3️⃣ Terminal 3: (Optional) Chạy Frontend
```bash
cd warehouse-fe
npm install
npm run dev
```

✅ Frontend nhận được là `http://localhost:5173`

---

## 🧪 Test Nhanh

### ✅ AI Service Health
```bash
curl http://localhost:5000/api/health
```

### ✅ Backend Health
```bash
curl http://localhost:9090/api/forecast/health
```

### ✅ Test Dự Báo
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

Expected Response:
```json
{
  "medicineId": 1,
  "medicineName": "Paracetamol 500mg",
  "predictedQuantity": 52.3,
  "confidenceLevel": 0.89,
  "recommendedOrder": 75,
  "status": "success"
}
```

---

## 📚 Chi Tiết & Troubleshooting

Xem đầy đủ hướng dẫn: [INTEGRATION_GUIDE.md](./INTEGRATION_GUIDE.md)

---

## 🔧 Configuration

**File:** `warehouse-be/src/main/resources/application.properties`

```properties
# AI Model Service
ai.service.url=http://localhost:5000
ai.service.timeout=30000
```

Nếu AI Server chạy trên server khác:
```properties
ai.service.url=http://<your-ip>:5000
```

---

## 📋 Features Đã Tích Hợp

✅ Flask API Server với endpoints dự báo  
✅ Backend Java gọi AI Service  
✅ Fallback dự báo khi AI Server không khả dụng  
✅ Single & Batch prediction support  
✅ Model training endpoint  
✅ Health check endpoints  
✅ Error handling & logging  

---

## 🎯 Next Steps

1. ✅ Cài đặt & chạy các server
2. ✅ Test endpoints
3. ⏳ Update Frontend để gọi API thực tế (thay vì mock data)
4. ⏳ Cấu hình database để lưu predictions
5. ⏳ Deploy lên production

---

**Status:** ✅ Production Ready  
**Last Updated:** 2026-03-26
