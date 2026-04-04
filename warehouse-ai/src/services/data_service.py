import pandas as pd
import numpy as np
import pymysql
from datetime import datetime, timedelta

# Cấu hình danh sách tính năng đồng nhất cho toàn hệ thống
FEATURE_COLUMNS = [
    'medicine_name_id', 'region_id', 'storage_condition_id', 
    'temperature', 'flu_season', 'rain', 'is_holiday', 'is_weekend',
    'sales_lag_1', 'sales_lag_7', 'sales_lag_30'
]

CAT_COLUMNS = ['medicine_name', 'region', 'storage_condition']


def clean_and_prepare_data(df: pd.DataFrame) -> pd.DataFrame:
    """Chuẩn hóa dữ liệu đầu vào từ mọi nguồn (CSV, SQL)"""
    
    print(f"🔍 clean_and_prepare_data input: {len(df)} rows, columns: {df.columns.tolist()}")
    
    df = df.copy()

    # 1. Xử lý ngày tháng + sort
    if 'sale_date' in df.columns:
        df['sale_date'] = pd.to_datetime(df['sale_date'], errors='coerce')
        df.sort_values(['medicine_name', 'sale_date'], inplace=True)
        print(f"✅ Đã xử lý sale_date, có {len(df)} rows sau sort")

        # 2. Feature thời gian (vectorized nhanh hơn apply)
        df['is_weekend'] = (df['sale_date'].dt.weekday >= 5).astype(int)

    # 3. Lag features (tối ưu loop)
    if 'quantity_sold' in df.columns and len(df) > 30:
        grouped = df.groupby('medicine_name')['quantity_sold']
        for lag in (1, 7, 30):
            df[f'sales_lag_{lag}'] = grouped.shift(lag)
        print(f"✅ Đã tạo lag features: {len(df.filter(like='lag').columns)} cột")

    # 4. Fill missing values
    # Lag columns
    lag_cols = df.filter(like='lag').columns
    if len(lag_cols) > 0:
        df[lag_cols] = df[lag_cols].fillna(0)

    # Weather & flags (xử lý gọn hơn)
    fill_zero_cols = ['temperature', 'flu_season', 'rain', 'is_holiday']
    existing_cols = df.columns.intersection(fill_zero_cols)
    df[existing_cols] = df[existing_cols].fillna(0)

    print(f"🔍 clean_and_prepare_data output: {len(df)} rows, columns: {df.columns.tolist()}")
    return df


def validate_features(df: pd.DataFrame) -> pd.DataFrame:
    """Đảm bảo đủ feature cho model"""
    
    missing_cols = list(set(FEATURE_COLUMNS) - set(df.columns))
    
    if missing_cols:
        df = df.assign(**{col: 0 for col in missing_cols})

    return df.reindex(columns=FEATURE_COLUMNS, fill_value=0)


def get_db_connection():
    return pymysql.connect(
        host='localhost',
        user='root',
        password='Sampro2k5@123',
        database='pharmacy_warehouse',
        charset='utf8mb4',
        cursorclass=pymysql.cursors.DictCursor
    )


def extract_historical_demand(days=365):
    connection = get_db_connection()

    try:
        query = """
        SELECT
            DATE(o.order_date) as sale_date,
            m.name as medicine_name,
            m.manufacturer,
            m.storage_condition,
            SUM(oi.quantity) as quantity_sold
        FROM orders o
        JOIN order_item oi ON o.order_id = oi.order_id
        JOIN batch b ON oi.batch_id = b.batch_id
        JOIN medicine m ON b.medicine_id = m.medicine_id
        WHERE o.status = 'COMPLETED'
        AND o.order_date >= DATE_SUB(CURDATE(), INTERVAL %s DAY)
        GROUP BY DATE(o.order_date), m.medicine_id
        ORDER BY sale_date
        """

        df = pd.read_sql(query, connection, params=(days,))

        if df.empty:
            return generate_fallback_data(days)

        df['sale_date'] = pd.to_datetime(df['sale_date'])
        df.sort_values(['medicine_name', 'sale_date'], inplace=True)

        df['temperature'] = 30
        df['flu_season'] = (df['sale_date'].dt.month.isin([1, 2, 12])).astype(int)
        df['rain'] = (df['sale_date'].dt.month.isin([6, 7, 8])).astype(int)

        grouped = df.groupby('medicine_name')['quantity_sold']
        for lag in (1, 7, 30):
            df[f'sales_lag_{lag}'] = grouped.shift(lag)

        df[['sales_lag_1', 'sales_lag_7', 'sales_lag_30']] = df[
            ['sales_lag_1', 'sales_lag_7', 'sales_lag_30']
        ].fillna(0)

        df['day_of_week'] = df['sale_date'].dt.dayofweek
        df['is_weekend'] = (df['day_of_week'] >= 5).astype(int)

        return df

    except Exception as e:
        print(f"DB error: {e}")
        return generate_fallback_data(days)

    finally:
        connection.close()


def generate_fallback_data(days=365):
    np.random.seed(42)

    dates = pd.date_range(end=datetime.now(), periods=days)

    medicines = ['Paracetamol', 'Amoxicillin', 'Vitamin C', 'Insulin']

    data = []

    for med in medicines:
        base = np.random.randint(20, 40)

        for i, date in enumerate(dates):
            trend = i * 0.05
            season = 20 if date.month in [1,2,12] else 0

            quantity = base + trend + season + np.random.randint(0, 10)

            data.append({
                'sale_date': date,
                'medicine_name': med,
                'quantity_sold': int(quantity),
                'temperature': 30,
                'flu_season': int(date.month in [1,2,12]),
                'rain': int(date.month in [6,7,8]),
                'day_of_week': date.weekday(),
                'is_weekend': int(date.weekday() >= 5)
            })

    df = pd.DataFrame(data)

    grouped = df.groupby('medicine_name')['quantity_sold']
    for lag in (1, 7, 30):
        df[f'sales_lag_{lag}'] = grouped.shift(lag).fillna(0)

    return df


if __name__ == "__main__":
    data = extract_historical_demand(days=30)
    print(data.head())
    print(f"Data shape: {data.shape}")
    print(f"Columns: {data.columns.tolist()}")
