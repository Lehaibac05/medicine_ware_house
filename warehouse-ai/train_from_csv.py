import pandas as pd
import numpy as np
from demand_model import DemandPredictor
from datetime import datetime
import sys

def load_csv_data(csv_file_path):
    """
    Load and preprocess data from CSV file for training
    """
    print(f"Loading data from {csv_file_path}...")

    # Read CSV file
    df = pd.read_csv(csv_file_path)

    # Convert date column
    df['sale_date'] = pd.to_datetime(df['sale_date'])

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
    df = pd.get_dummies(df, columns=['manufacturer', 'storage_condition', 'month'],
                       prefix=['manuf', 'storage', 'month'])

    print(f"Loaded {len(df)} records with {len(df.columns)} columns")
    print(f"Columns: {df.columns.tolist()}")

    return df

def train_from_csv(csv_file_path):
    """
    Train the AI model using data from CSV file
    """
    # Load data
    data = load_csv_data(csv_file_path)

    # Initialize predictor
    predictor = DemandPredictor()

    # Train model
    print("Training model...")
    results = predictor.train(data)

    print("\nTraining completed!")
    print(f"Train MAE: {results['train_mae']:.2f}")
    print(f"Test MAE: {results['test_mae']:.2f}")
    print(f"Test RMSE: {results['test_rmse']:.2f}")
    print(f"Test R²: {results['test_r2']:.2f}")

    return predictor, results

if __name__ == "__main__":
    # Get CSV file name from command line or use default
    if len(sys.argv) > 1:
        csv_file = sys.argv[1]
    else:
        csv_file = "sample_training_data.csv"

    try:
        predictor, results = train_from_csv(csv_file)

        # Test prediction
        test_features = {
            'temperature': 30,
            'flu_season': 1,
            'rain': 0,
            'sales_lag_1': 50,
            'sales_lag_7': 45,
            'sales_lag_30': 40,
            'day_of_week': 2,  # Wednesday
            'is_weekend': 0,
            'manufacturer': 'DHG Pharma'
        }

        prediction = predictor.predict('Paracetamol 500mg', test_features)
        print("\nTest prediction for Paracetamol 500mg:")
        print(f"Predicted quantity: {prediction['prediction']:.1f}")
        print(f"Confidence interval: [{prediction['lower_bound']:.1f}, {prediction['upper_bound']:.1f}]")

    except FileNotFoundError:
        print(f"Error: File {csv_file} not found!")
        print("Please make sure the CSV file exists in the current directory.")
    except Exception as e:
        print(f"Error training model: {e}")