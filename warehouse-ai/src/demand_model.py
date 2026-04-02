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
            base_dir = os.path.dirname(os.path.abspath(__file__))  # Đường dẫn đến thư mục src
            model_path = os.path.join(base_dir, 'models', 'demand_hybrid_model.pkl')
        elif not os.path.isabs(model_path):
            base_dir = os.path.dirname(os.path.abspath(__file__))
            model_path = os.path.join(base_dir, model_path)

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
        df_proc = df.copy()
        cat_cols = ['medicine_name', 'region', 'storage_condition']
        
        # Đổi tên cột sale_date -> date để nhất quán
        if 'sale_date' in df_proc.columns:
            df_proc['date'] = pd.to_datetime(df_proc['sale_date'])
            df_proc = df_proc.drop('sale_date', axis=1)
        elif 'date' in df_proc.columns:
            df_proc['date'] = pd.to_datetime(df_proc['date'])
        
        # Sắp xếp theo date để tính lag
        if 'date' in df_proc.columns:
            df_proc = df_proc.sort_values(['medicine_name', 'region', 'date'])
        
        # Tạo các cột lag nếu chưa có
        lag_cols = ['sales_lag_1', 'sales_lag_7', 'sales_lag_30']
        if is_training:
            for lag in [1, 7, 30]:
                lag_col = f'sales_lag_{lag}'
                if lag_col not in df_proc.columns:
                    # Tính lag theo nhóm medicine_name và region
                    df_proc[lag_col] = df_proc.groupby(['medicine_name', 'region'])['quantity_sold'].shift(lag)
                    # Điền giá trị NaN bằng mean của nhóm
                    df_proc[lag_col] = df_proc.groupby(['medicine_name', 'region'])[lag_col].transform(
                        lambda x: x.fillna(x.mean())
                    )
                    # Nếu vẫn còn NaN (cho các nhóm chỉ có 1 record), dùng giá trị trung bình toàn bộ
                    df_proc[lag_col] = df_proc[lag_col].fillna(df_proc['quantity_sold'].mean())
        
        for col in cat_cols:
            if col not in df_proc.columns: continue
            if is_training:
                le = LabelEncoder()
                df_proc[f'{col}_id'] = le.fit_transform(df_proc[col])
                self.encoders[col] = le
            else:
                le = self.encoders.get(col)
                if le:
                    # Xử lý các nhãn mới chưa từng thấy khi dự báo
                    mapping = dict(zip(le.classes_, le.transform(le.classes_)))
                    df_proc[f'{col}_id'] = df_proc[col].apply(lambda x: mapping.get(x, 0))
                else:
                    df_proc[f'{col}_id'] = 0
        return df_proc

    def predict(self, medicine_name, region, features_dict):
        """Hàm dự báo có tính đến sự biến thiên thực tế"""
        if self.model is None:
            return {"error": "Mô hình chưa được huấn luyện"}

        input_data = pd.DataFrame([{
            'medicine_name': medicine_name,
            'region': region,
            **features_dict
        }])
        
        df_proc = self.preprocess_data(input_data, is_training=False)
        X = df_proc[self.feature_columns]
        
        # Dự báo cơ sở từ mô hình
        base_prediction = self.model.predict(X)[0]
        
        # --- Tăng cường độ nhạy bén (Heuristic Adjustment) ---
        # Nếu là mùa dịch (flu_season=1), tăng nhu cầu thêm 15-20% 
        if features_dict.get('flu_season', 0) == 1:
            base_prediction *= np.random.uniform(1.15, 1.25)
            
        # Nếu là cuối tuần, thường nhu cầu biến động nhẹ
        if features_dict.get('is_weekend', 0) == 1:
            base_prediction *= np.random.uniform(0.95, 1.05)

        # Tính khoảng tin cậy dựa trên các estimator con
        preds = [est.predict(X)[0] for est in self.model.named_estimators_.values()]
        std_dev = np.std(preds) + (base_prediction * 0.05) # Thêm chút nhiễu thực tế
        
        return {
            'prediction': float(max(0, base_prediction)),
            'lower_bound': float(max(0, base_prediction - 1.96 * std_dev)),
            'upper_bound': float(base_prediction + 1.96 * std_dev)
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
        joblib.dump({'model': self.model, 'encoders': self.encoders}, self.model_path)
    
    def train(self, df):
        """Huấn luyện mô hình với dữ liệu mới"""
        try:
            # Tiền xử lý dữ liệu
            df_proc = self.preprocess_data(df, is_training=True)
            
            # Chuẩn bị features và target
            X = df_proc[self.feature_columns]
            y = df_proc['quantity_sold']
            
            # Huấn luyện mô hình
            self.model.fit(X, y)
            
            # Lưu mô hình sau khi huấn luyện
            self.save_model()
            
            print(f"✅ Mô hình demand đã được huấn luyện với {len(df)} bản ghi")
            return True
        except Exception as e:
            print(f"❌ Lỗi khi huấn luyện mô hình demand: {e}")
            return False