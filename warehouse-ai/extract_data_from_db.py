import pymysql
import pandas as pd
from datetime import datetime, timedelta
import numpy as np

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
    """
    Extract historical demand data from database
    Returns DataFrame with columns: date, medicine_name, quantity_sold, temperature, flu_season, rain
    """
    connection = get_db_connection()

    try:
        with connection.cursor() as cursor:
            # Query to get daily sales per medicine
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
            ORDER BY sale_date, medicine_name
            """

            cursor.execute(query, (days,))
            results = cursor.fetchall()

        # Convert to DataFrame
        df = pd.DataFrame(results)

        if df.empty:
            print("No historical data found. Using simulated data instead.")
            return generate_fallback_data(days)

        # Convert date string to datetime
        df['sale_date'] = pd.to_datetime(df['sale_date'])

        # Add weather features (simulated for now, can be replaced with real weather API)
        df['temperature'] = np.random.randint(24, 36, len(df))
        df['flu_season'] = np.random.choice([0, 1], len(df), p=[0.8, 0.2])  # 20% flu season
        df['rain'] = np.random.choice([0, 1], len(df), p=[0.7, 0.3])  # 30% rainy days

        # Add lag features (previous days sales)
        df = df.sort_values(['medicine_name', 'sale_date'])
        df['sales_lag_1'] = df.groupby('medicine_name')['quantity_sold'].shift(1)
        df['sales_lag_7'] = df.groupby('medicine_name')['quantity_sold'].shift(7)
        df['sales_lag_30'] = df.groupby('medicine_name')['quantity_sold'].shift(30)

        # Fill NaN values with 0 for lag features
        df[['sales_lag_1', 'sales_lag_7', 'sales_lag_30']] = df[['sales_lag_1', 'sales_lag_7', 'sales_lag_30']].fillna(0)

        # Add seasonality features
        df['month'] = df['sale_date'].dt.month
        df['day_of_week'] = df['sale_date'].dt.dayofweek
        df['is_weekend'] = df['day_of_week'].isin([5, 6]).astype(int)

        # One-hot encode categorical features
        df = pd.get_dummies(df, columns=['manufacturer', 'storage_condition', 'month'], prefix=['manuf', 'storage', 'month'])

        print(f"Extracted {len(df)} records from database")
        return df

    except Exception as e:
        print(f"Error extracting data from database: {e}")
        print("Using simulated data instead.")
        return generate_fallback_data(days)
    finally:
        connection.close()

def generate_fallback_data(days=365):
    """
    Generate simulated data when no real data is available
    """
    np.random.seed(42)

    dates = pd.date_range(start=datetime.now() - timedelta(days=days), end=datetime.now(), freq='D')

    # Simulate data for a few medicines
    medicines = ['Paracetamol 500mg', 'Amoxicillin 250mg', 'Vitamin C 500mg', 'Insulin Glargine']

    data = []
    for date in dates:
        for medicine in medicines:
            base_sales = np.random.randint(10, 50)
            temp_effect = np.random.randint(24, 36) * 0.5
            flu_effect = np.random.choice([0, 20], p=[0.8, 0.2])
            rain_effect = np.random.choice([0, 15], p=[0.7, 0.3])

            quantity_sold = base_sales + temp_effect + flu_effect + rain_effect + np.random.randint(0, 10)

            data.append({
                'sale_date': date,
                'medicine_name': medicine,
                'quantity_sold': int(quantity_sold),
                'temperature': np.random.randint(24, 36),
                'flu_season': np.random.choice([0, 1], p=[0.8, 0.2]),
                'rain': np.random.choice([0, 1], p=[0.7, 0.3]),
                'sales_lag_1': 0,  # Will be filled later
                'sales_lag_7': 0,
                'sales_lag_30': 0,
                'day_of_week': date.weekday(),
                'is_weekend': int(date.weekday() >= 5)
            })

    df = pd.DataFrame(data)

    # Add lag features
    df = df.sort_values(['medicine_name', 'sale_date'])
    df['sales_lag_1'] = df.groupby('medicine_name')['quantity_sold'].shift(1).fillna(0)
    df['sales_lag_7'] = df.groupby('medicine_name')['quantity_sold'].shift(7).fillna(0)
    df['sales_lag_30'] = df.groupby('medicine_name')['quantity_sold'].shift(30).fillna(0)

    return df

if __name__ == "__main__":
    # Test the function
    data = extract_historical_demand(days=30)  # Last 30 days for testing
    print(data.head())
    print(f"Data shape: {data.shape}")
    print(f"Columns: {data.columns.tolist()}")