import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestRegressor, VotingRegressor
from xgboost import XGBRegressor
from sklearn.metrics import mean_absolute_error, r2_score, mean_squared_error
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
import joblib
import os

class DemandPredictor:
    def __init__(self, model_path=None):
        if model_path is None:
            # Đường dẫn đến thư mục warehouse-ai (lên 3 cấp từ src/model/)
            base_dir = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
            model_path = os.path.join(base_dir, 'model-ai', 'demand_hybrid_model.pkl')
        elif not os.path.isabs(model_path):
            base_dir = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
            model_path = os.path.join(base_dir, 'model-ai', model_path)

        self.model_path = model_path
        self.model = None
        self.encoders = {}
        # Các cột tính năng quan trọng
        self.feature_columns = [
            'medicine_name_id', 'region_id', 'storage_condition_id', 
            'temperature', 'flu_season', 'rain', 'is_holiday', 'is_weekend',
            'sales_lag_1', 'sales_lag_7', 'sales_lag_30'
        ]
        
        os.makedirs(os.path.dirname(self.model_path) if os.path.dirname(self.model_path) else '.', exist_ok=True)
        self.load_model()

    def _init_hybrid_model(self):
        """Khởi tạo mô hình Hybrid với tham số nhạy bén hơn"""
        # RF giúp ổn định xu hướng chung
        rf = RandomForestRegressor(
            n_estimators=150, max_depth=12, random_state=42, n_jobs=-1
        )
        # XGBoost tinh chỉnh để nhạy với biến thiên nhỏ (nhiệt độ, mùa)
        xgb = XGBRegressor(
            n_estimators=250, 
            learning_rate=0.08, # Tăng nhẹ để học nhanh hơn các biến đổi
            max_depth=6, 
            subsample=0.8,      # Tránh overfitting nhưng vẫn nhạy
            colsample_bytree=0.8,
            random_state=42
        )
        
        return VotingRegressor(estimators=[('rf', rf), ('xgb', xgb)])

    def preprocess_data(self, df, is_training=True):
        df = df.copy()

        cat_cols = ['medicine_name', 'region', 'storage_condition']

        # ===== DATE =====
        if 'sale_date' in df.columns:
            df['date'] = pd.to_datetime(df['sale_date'], errors='coerce')
            df.drop(columns=['sale_date'], inplace=True)
        elif 'date' in df.columns:
            df['date'] = pd.to_datetime(df['date'], errors='coerce')

        if 'date' in df.columns:
            df.sort_values(['medicine_name', 'region', 'date'], inplace=True)
            df['is_weekend'] = (df['date'].dt.weekday >= 5).astype(int)

        # ===== LAG (tối ưu groupby 1 lần) =====
        if is_training and 'quantity_sold' in df.columns:
            grouped = df.groupby(['medicine_name', 'region'])['quantity_sold']

            for lag in (1, 7, 30):
                col = f'sales_lag_{lag}'
                if col not in df.columns:
                    df[col] = grouped.shift(lag)

            # Fill NA nhanh hơn (không dùng lambda)
            lag_cols = df.filter(like='sales_lag').columns
            df[lag_cols] = df[lag_cols].fillna(df['quantity_sold'].mean())

        # ===== ENCODER (vectorized mapping) =====
        for col in cat_cols:
            id_col = f'{col}_id'

            if col not in df.columns:
                df[id_col] = 0
                continue

            if is_training:
                le = LabelEncoder()
                df[id_col] = le.fit_transform(df[col].astype(str))
                self.encoders[col] = le
            else:
                le = self.encoders.get(col)

                if le:
                    mapping = dict(zip(le.classes_, le.transform(le.classes_)))
                    df[id_col] = df[col].map(mapping)

                    # xử lý giá trị mới (unknown)
                    df[id_col] = df[id_col].fillna(np.mean(list(mapping.values())))
                else:
                    df[id_col] = 0

        return df

    def predict(self, medicine_name, region, features_dict):
        if self.model is None:
            return {"error": "Model chưa train"}

        input_df = pd.DataFrame([{
            'medicine_name': medicine_name,
            'region': region,
            **features_dict
        }])

        df_proc = self.preprocess_data(input_df, is_training=False)
        X = df_proc[self.feature_columns]

        # ===== Predict 1 lần =====
        base_pred = float(self.model.predict(X)[0])

        # ===== Heuristic fallback =====
        if base_pred <= 0:
            lag1 = features_dict.get('sales_lag_1', 50)
            lag7 = features_dict.get('sales_lag_7', 50)
            lag30 = features_dict.get('sales_lag_30', 50)

            base_pred = lag1 * 0.5 + lag7 * 0.3 + lag30 * 0.2

        # ===== Season adjust (deterministic, không random) =====
        if features_dict.get('flu_season', 0):
            base_pred *= 1.2

        if features_dict.get('is_weekend', 0):
            base_pred *= 1.02

        # ===== Confidence interval tối ưu =====
        preds = np.array([
            est.predict(X)[0] for est in self.model.estimators_
        ])

        std = preds.std() + base_pred * 0.05

        return {
            "prediction": float(max(1, base_pred)),
            "lower_bound": float(max(0, base_pred - 1.96 * std)),
            "upper_bound": float(base_pred + 1.96 * std)
        }
    
    def load_model(self):
        if os.path.exists(self.model_path):
            try:
                data = joblib.load(self.model_path)
                self.model = data['model']
                self.encoders = data['encoders']
            except: self.model = self._init_hybrid_model()
        else: self.model = self._init_hybrid_model()

    def save_model(self):
        print("🔥 ĐANG SAVE MODEL TẠI:", self.model_path)
        joblib.dump({'model': self.model, 'encoders': self.encoders}, self.model_path)
    
    def train(self, df):
        try:
            df_proc = self.preprocess_data(df, is_training=True)

            X = df_proc[self.feature_columns]
            y = df_proc['quantity_sold']

            # Split để tránh overfit
            X_train, X_val, y_train, y_val = train_test_split(
                X, y, test_size=0.2, random_state=42
            )

            self.model.fit(X_train, y_train)

            # Evaluate
            preds = self.model.predict(X_val)
            mae = mean_absolute_error(y_val, preds)
            r2 = r2_score(y_val, preds)

            print(f"MAE: {mae:.2f} | R2: {r2:.3f}")

            self.save_model()
            return True

        except Exception as e:
            print(f"Training error: {e}")
            return False