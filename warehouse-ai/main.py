from extract_data_from_db import extract_historical_demand
from demand_model import DemandPredictor
from inventory_rl import InventoryAgent
import pandas as pd

def main():
    print("=== AI Demand Forecasting System ===")

    # 1. Extract historical data from database
    print("\n1. Extracting historical demand data...")
    data = extract_historical_demand(days=365)  # Last 365 days

    if data.empty:
        print("No data available for training")
        return

    print(f"Data shape: {data.shape}")
    print(f"Date range: {data['sale_date'].min()} to {data['sale_date'].max()}")
    print(f"Medicines: {data['medicine_name'].nunique()}")
    print(f"Total sales records: {len(data)}")

    # 2. Train demand prediction model
    print("\n2. Training Random Forest model...")
    predictor = DemandPredictor()
    metrics = predictor.train(data)

    print("\nModel Performance:")
    print(".2f")
    print(".2f")
    print(".2f")
    print(".2f")

    # 3. Test prediction for a sample medicine
    print("\n3. Testing prediction...")
    sample_medicine = data['medicine_name'].iloc[0] if not data.empty else 'Paracetamol 500mg'

    # Get recent features for prediction
    recent_data = data[data['medicine_name'] == sample_medicine].tail(1)
    if not recent_data.empty:
        features = {
            'temperature': recent_data['temperature'].iloc[0],
            'flu_season': recent_data['flu_season'].iloc[0],
            'rain': recent_data['rain'].iloc[0],
            'sales_lag_1': recent_data['sales_lag_1'].iloc[0],
            'sales_lag_7': recent_data['sales_lag_7'].iloc[0],
            'sales_lag_30': recent_data['sales_lag_30'].iloc[0],
            'day_of_week': recent_data['day_of_week'].iloc[0],
            'is_weekend': recent_data['is_weekend'].iloc[0],
            'manufacturer': 'DHG Pharma'  # Default, should get from medicine table
        }

        prediction = predictor.predict(sample_medicine, features)
        print(f"Predicted demand for {sample_medicine}: {prediction['prediction']:.1f} units")
        print(f"95% Confidence Interval: [{prediction['lower_bound']:.1f}, {prediction['upper_bound']:.1f}]")

    # 4. Train RL inventory agent (using simulated data for now)
    print("\n4. Training RL inventory agent...")
    agent = InventoryAgent()
    agent.train()

    current_stock = 30
    action = agent.best_action(current_stock)

    print(f"Current stock: {current_stock}")
    print(f"Recommended order quantity: {action}")

    print("\n=== Training Complete ===")

if __name__ == "__main__":
    main()