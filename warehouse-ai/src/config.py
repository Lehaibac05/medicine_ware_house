import pandas as pd
import numpy as np

# Cấu hình danh sách tính năng đồng nhất cho toàn hệ thống
FEATURE_COLUMNS = [
    'medicine_name_id', 'region_id', 'storage_condition_id', 
    'temperature', 'flu_season', 'rain', 'is_holiday', 'is_weekend',
    'sales_lag_1', 'sales_lag_7', 'sales_lag_30'
]

CAT_COLUMNS = ['medicine_name', 'region', 'storage_condition']

def clean_and_prepare_data(df):
    """
    Hàm chuẩn hóa dữ liệu thô từ mọi nguồn (CSV, SQL)
    """
    df_proc = df.copy()
    
    # 1. Chuẩn hóa ngày tháng
    if 'sale_date' in df_proc.columns:
        df_proc['sale_date'] = pd.to_datetime(df_proc['sale_date'])
        df_proc = df_proc.sort_values(['medicine_name', 'sale_date'])
        
        # 2. Tạo các tính năng thời gian
        df_proc['is_weekend'] = df_proc['sale_date'].dt.weekday.apply(lambda x: 1 if x >= 5 else 0)
        # Có thể thêm: df_proc['month'] = df_proc['sale_date'].dt.month
        
    # 3. Tạo Lag Features (Chỉ thực hiện nếu có đủ dữ lịch sử)
    # Lưu ý: Khi Predict 1 bản ghi đơn lẻ, bước này sẽ được xử lý thủ công từ input
    if 'quantity_sold' in df_proc.columns and df_proc.shape[0] > 30:
        for lag in [1, 7, 30]:
            df_proc[f'sales_lag_{lag}'] = df_proc.groupby('medicine_name')['quantity_sold'].shift(lag)
    
    # 4. Xử lý giá trị thiếu (Missing values)
    # Lấp đầy các cột lag bằng 0 hoặc giá trị trung bình nếu là dòng đầu tiên
    lag_cols = [c for c in df_proc.columns if 'lag' in c]
    df_proc[lag_cols] = df_proc[lag_cols].fillna(0)
    
    # Điền giá trị mặc định cho các cột thời tiết nếu thiếu
    for col in ['temperature', 'flu_season', 'rain', 'is_holiday']:
        if col in df_proc.columns:
            df_proc[col] = df_proc[col].fillna(0)

    return df_proc

def validate_features(df):
    """Kiểm tra xem dữ liệu đã đủ các cột để AI chạy chưa"""
    missing = [col for col in FEATURE_COLUMNS if col not in df.columns]
    if missing:
        # Nếu thiếu cột, tự động bổ sung cột đó với giá trị 0
        for col in missing:
            df[col] = 0
    return df[FEATURE_COLUMNS]