from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.preprocessing import StandardScaler
import pandas as pd
import numpy as np
import joblib
import os

class DemandPredictor:
    def __init__(self, model_path='demand_model.pkl', scaler_path='scaler.pkl'):
        self.model_path = model_path
        self.scaler_path = scaler_path
        self.model = None
        self.scaler = StandardScaler()
        self.feature_columns = None
        self.load_model()

    def load_model(self):
        """Load trained model if exists"""
        if os.path.exists(self.model_path):
            try:
                self.model = joblib.load(self.model_path)
                self.scaler = joblib.load(self.scaler_path)
                print("Loaded existing model")
            except:
                print("Could not load existing model, will create new one")
                self.model = RandomForestRegressor(
                    n_estimators=200,
                    max_depth=20,
                    min_samples_split=5,
                    min_samples_leaf=2,
                    random_state=42,
                    n_jobs=-1
                )
        else:
            self.model = RandomForestRegressor(
                n_estimators=200,
                max_depth=20,
                min_samples_split=5,
                min_samples_leaf=2,
                random_state=42,
                n_jobs=-1
            )

    def save_model(self):
        """Save trained model"""
        joblib.dump(self.model, self.model_path)
        joblib.dump(self.scaler, self.scaler_path)
        print("Model saved")

    def prepare_features(self, data):
        """Prepare features for training/prediction"""
        # Select numeric features
        numeric_features = [
            'temperature', 'flu_season', 'rain', 'sales_lag_1', 'sales_lag_7', 'sales_lag_30',
            'day_of_week', 'is_weekend'
        ]

        # Add manufacturer and storage condition features (one-hot encoded)
        manufacturer_cols = [col for col in data.columns if col.startswith('manuf_')]
        storage_cols = [col for col in data.columns if col.startswith('storage_')]
        month_cols = [col for col in data.columns if col.startswith('month_')]

        self.feature_columns = numeric_features + manufacturer_cols + storage_cols + month_cols

        X = data[self.feature_columns].fillna(0)

        return X

    def train(self, data):
        """Train the model with historical data"""
        print("Preparing training data...")

        # Prepare features
        X = self.prepare_features(data)
        y = data['quantity_sold']

        # Scale features
        X_scaled = self.scaler.fit_transform(X)

        # Split data
        X_train, X_test, y_train, y_test = train_test_split(
            X_scaled, y, test_size=0.2, random_state=42
        )

        print(f"Training with {len(X_train)} samples, testing with {len(X_test)} samples")

        # Train model
        self.model.fit(X_train, y_train)

        # Evaluate
        train_pred = self.model.predict(X_train)
        test_pred = self.model.predict(X_test)

        train_mae = mean_absolute_error(y_train, train_pred)
        test_mae = mean_absolute_error(y_test, test_pred)
        test_rmse = np.sqrt(mean_squared_error(y_test, test_pred))
        test_r2 = r2_score(y_test, test_pred)

        print(".2f")
        print(".2f")
        print(".2f")
        print(".2f")

        # Feature importance
        feature_importance = pd.DataFrame({
            'feature': self.feature_columns,
            'importance': self.model.feature_importances_
        }).sort_values('importance', ascending=False)

        print("\nTop 10 important features:")
        print(feature_importance.head(10))

        # Save model
        self.save_model()

        return {
            'train_mae': train_mae,
            'test_mae': test_mae,
            'test_rmse': test_rmse,
            'test_r2': test_r2
        }

    def predict(self, medicine_name, features_dict):
        """Predict demand for a specific medicine"""
        if self.model is None:
            raise ValueError("Model not trained yet")

        # Create feature vector
        features = pd.DataFrame([features_dict])

        # Add medicine-specific features (manufacturer, storage from medicine data)
        # For now, assume default values - in production, get from medicine table
        features['manuf_DHG_Pharma'] = 1 if 'manufacturer' in features_dict and features_dict['manufacturer'] == 'DHG Pharma' else 0
        features['storage_Room_temperature'] = 1  # Default

        # Ensure all feature columns are present
        for col in self.feature_columns:
            if col not in features.columns:
                features[col] = 0

        # Select and order features
        X = features[self.feature_columns]

        # Scale
        X_scaled = self.scaler.transform(X)

        # Predict
        prediction = self.model.predict(X_scaled)[0]

        # Get prediction interval (simple approximation)
        # In production, use quantile regression or other methods
        std = np.std([tree.predict(X_scaled) for tree in self.model.estimators_])
        lower_bound = max(0, prediction - 1.96 * std)
        upper_bound = prediction + 1.96 * std

        return {
            'prediction': float(prediction),
            'lower_bound': float(lower_bound),
            'upper_bound': float(upper_bound),
            'confidence': 0.95  # 95% confidence interval
        }

    def predict_batch(self, predictions_data):
        """Predict for multiple medicines/dates"""
        results = []
        for item in predictions_data:
            result = self.predict(item['medicine_name'], item['features'])
            result['medicine_name'] = item['medicine_name']
            result['date'] = item.get('date')
            results.append(result)
        return results

