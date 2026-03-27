import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestRegressor, VotingRegressor
from xgboost import XGBRegressor
from sklearn.metrics import mean_absolute_error, r2_score
from sklearn.preprocessing import LabelEncoder
import joblib
import os

class DemandPredictor:
    def __init__(self, model_path='models/demand_hybrid_model.pkl'):
        self.model_path = model_path
        self.model = None
        self.encoders = {}  # Lưu trữ LabelEncoders cho các cột phân loại
        
        # Danh sách các cột tính năng cố định để đảm bảo tính nhất quán
        self.feature_columns = [
            'medicine_name_id', 'region_id', 'storage_condition_id', 
            'temperature', 'flu_season', 'rain', 'is_holiday', 'is_weekend',
            'sales_lag_1', 'sales_lag_7', 'sales_lag_30'
        ]
        
        # Tạo thư mục models nếu chưa có
        os.makedirs(os.path.dirname(self.model_path) if os.path.dirname(self.model_path) else '.', exist_ok=True)
        self.load_model()

    def _init_hybrid_model(self):
        """Khởi tạo cấu trúc mô hình Hybrid (RF + XGB)"""
        rf = RandomForestRegressor(
            n_estimators=200, max_depth=15, random_state=42, n_jobs=-1
        )
        xgb = XGBRegressor(
            n_estimators=200, learning_rate=0.05, max_depth=8, random_state=42
        )
        
        # Kết hợp 2 mô hình theo cơ chế trung bình trọng số (Voting)
        return VotingRegressor(estimators=[
            ('rf', rf),
            ('xgb', xgb)
        ])

    def load_model(self):
        """Load model và encoders từ file"""
        if os.path.exists(self.model_path):
            try:
                data = joblib.load(self.model_path)
                self.model = data['model']
                self.encoders = data['encoders']
                print(f"✅ Đã tải mô hình Hybrid từ {self.model_path}")
            except Exception as e:
                print(f"⚠️ Lỗi tải mô hình: {e}. Sẽ khởi tạo mô hình mới.")
                self.model = self._init_hybrid_model()
        else:
            self.model = self._init_hybrid_model()

    def save_model(self):
        """Lưu cả mô hình và bộ mã hóa vào một file duy nhất"""
        joblib.dump({
            'model': self.model,
            'encoders': self.encoders
        }, self.model_path)
        print(f"💾 Đã lưu mô hình tại {self.model_path}")

    def preprocess_data(self, df, is_training=True):
        """Xử lý dữ liệu thô sang dạng số (Encoding)"""
        df_proc = df.copy()
        cat_cols = ['medicine_name', 'region', 'storage_condition']
        
        for col in cat_cols:
            if col not in df_proc.columns:
                continue
                
            if is_training:
                le = LabelEncoder()
                df_proc[f'{col}_id'] = le.fit_transform(df_proc[col])
                self.encoders[col] = le
            else:
                # Nếu là dự đoán, dùng encoder đã học được từ lúc train
                le = self.encoders.get(col)
                if le:
                    # Xử lý trường hợp có nhãn mới chưa từng thấy (ví dụ thuốc mới)
                    mapping = dict(zip(le.classes_, le.transform(le.classes_)))
                    df_proc[f'{col}_id'] = df_proc[col].apply(lambda x: mapping.get(x, -1))
                else:
                    df_proc[f'{col}_id'] = 0

        # Đảm bảo có đủ các cột lag, nếu thiếu thì điền 0
        for lag in ['sales_lag_1', 'sales_lag_7', 'sales_lag_30']:
            if lag not in df_proc.columns:
                df_proc[lag] = 0
                
        return df_proc

    def train(self, df):
        """Huấn luyện mô hình với dữ liệu từ DB hoặc CSV"""
        print("Đang xử lý dữ liệu và huấn luyện mô hình Hybrid...")
        
        df_ready = self.preprocess_data(df, is_training=True)
        
        X = df_ready[self.feature_columns]
        y = df_ready['quantity_sold']
        
        self.model.fit(X, y)
        
        # Đánh giá nhanh
        y_pred = self.model.predict(X)
        metrics = {
            'mae': mean_absolute_error(y, y_pred),
            'r2': r2_score(y, y_pred)
        }
        
        self.save_model()
        return metrics

    def predict(self, medicine_name, region, features_dict):
        """Dự đoán cho một trường hợp cụ thể"""
        if self.model is None:
            return {"error": "Mô hình chưa được huấn luyện"}

        # Tạo DataFrame từ input để đồng nhất quy trình preprocess
        input_data = pd.DataFrame([{
            'medicine_name': medicine_name,
            'region': region,
            **features_dict
        }])
        
        df_proc = self.preprocess_data(input_data, is_training=False)
        X = df_proc[self.feature_columns]
        
        prediction = self.model.predict(X)[0]
        
        # Tính toán khoảng tin cậy dựa trên sai số của các estimator (RF & XGB)
        # Trong VotingRegressor, chúng ta lấy độ lệch chuẩn giữa các model thành phần
        preds = []
        for name, est in self.model.named_estimators_.items():
            preds.append(est.predict(X)[0])
        
        std_dev = np.std(preds)
        
        return {
            'prediction': float(max(0, prediction)),
            'lower_bound': float(max(0, prediction - 1.96 * std_dev)),
            'upper_bound': float(prediction + 1.96 * std_dev),
            'confidence_score': float(self.model.score(X, [prediction])) # Chỉ số tham khảo
        }