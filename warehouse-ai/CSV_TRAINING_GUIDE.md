# Hướng Dẫn Train AI Model Từ File CSV/Excel

## Cấu Trúc Dữ Liệu Cần Thiết

Để train AI model dự đoán nhu cầu medicine, file CSV/Excel của bạn cần có **các cột sau**:

### Cột Bắt Buộc:
1. **`sale_date`** - Ngày bán hàng (định dạng: YYYY-MM-DD)
2. **`medicine_name`** - Tên thuốc
3. **`manufacturer`** - Nhà sản xuất
4. **`storage_condition`** - Điều kiện bảo quản (Room temperature, Cool place, Refrigerator, etc.)
5. **`quantity_sold`** - Số lượng bán được (số nguyên)

### Cột Tùy Chọn (Khuyến Nghị):
6. **`temperature`** - Nhiệt độ (°C) - ảnh hưởng đến nhu cầu
7. **`flu_season`** - Mùa cúm (0 hoặc 1) - 1 nếu đang trong mùa cúm
8. **`rain`** - Có mưa không (0 hoặc 1) - 1 nếu có mưa

## Ví Dụ File CSV

```csv
sale_date,medicine_name,manufacturer,storage_condition,quantity_sold,temperature,flu_season,rain
2024-01-01,Paracetamol 500mg,DHG Pharma,Room temperature,45,28,0,0
2024-01-02,Paracetamol 500mg,DHG Pharma,Room temperature,52,29,0,1
2024-01-03,Paracetamol 500mg,DHG Pharma,Room temperature,48,27,0,0
2024-01-01,Amoxicillin 250mg,US Pharma,Cool place,25,28,0,0
2024-01-02,Amoxicillin 250mg,US Pharma,Cool place,28,29,0,1
```

## Cách Train Model

### Bước 1: Chuẩn Bị File Dữ Liệu
- Tạo file CSV với cấu trúc như trên
- Đảm bảo có ít nhất 30-60 ngày dữ liệu cho mỗi loại thuốc
- Càng nhiều dữ liệu lịch sử càng tốt (tối thiểu 6 tháng)

### Bước 2: Chạy Script Training

```bash
cd warehouse-ai
python train_from_csv.py
```

Script sẽ:
- Tự động load dữ liệu từ `sample_training_data.csv`
- Xử lý và chuẩn bị features
- Train model Random Forest
- Lưu model đã train
- Hiển thị kết quả đánh giá

### Bước 3: Kiểm Tra Kết Quả
- **R² Score**: Độ chính xác (0.6-0.9 là tốt)
- **MAE (Mean Absolute Error)**: Sai số trung bình
- **Feature Importance**: Các yếu tố ảnh hưởng nhất

## Lưu Ý Quan Trọng

### Số Lượng Dữ Liệu:
- **Tối thiểu**: 30 ngày × 3-5 loại thuốc = 90-150 bản ghi
- **Khuyến nghị**: 180 ngày × 5-10 loại thuốc = 900-1800 bản ghi
- **Tối ưu**: 365 ngày × 10+ loại thuốc = 3650+ bản ghi

### Chất Lượng Dữ Liệu:
- Không có missing values trong các cột bắt buộc
- Định dạng ngày chính xác
- Tên thuốc và manufacturer consistent
- Quantity_sold phải là số dương

### Features Quan Trọng:
1. **sales_lag_1**: Doanh số ngày trước (ảnh hưởng lớn nhất ~75%)
2. **Temperature**: Nhiệt độ
3. **Manufacturer**: Thương hiệu thuốc
4. **Storage Condition**: Điều kiện bảo quản
5. **Seasonal Factors**: Mùa cúm, thời tiết

## Tùy Chỉnh Model

Nếu muốn tùy chỉnh model, sửa file `demand_model.py`:
- `n_estimators`: Số cây quyết định (mặc định 200)
- `max_depth`: Độ sâu tối đa (mặc định 20)
- `min_samples_split`: Số mẫu tối thiểu để chia nhánh

## Dự Đoán

Sau khi train, model sẽ tự động lưu và có thể dùng để dự đoán nhu cầu cho các ngày tiếp theo.