from flask import Flask, request, jsonify
from flask_cors import CORS
import pandas as pd
import numpy as np
import logging
import os
from datetime import datetime, timedelta

from demand_model import DemandPredictor
from inventory_rl import InventoryAgent

app = Flask(__name__)
CORS(app)

# Cấu hình Logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))  # Đường dẫn đến thư mục src
DEFAULT_DATA_PATH = os.path.join(os.path.dirname(BASE_DIR), 'data', 'pharmacy_training_final.csv')
DEFAULT_DEMAND_MODEL_PATH = os.path.join(BASE_DIR, 'models', 'demand_hybrid_model.pkl')
DEFAULT_INVENTORY_MODEL_PATH = os.path.join(BASE_DIR, 'models', 'inventory_q_table.pkl')

predictor = None
agent = None

def init_models():
    global predictor, agent
    predictor = DemandPredictor(model_path=DEFAULT_DEMAND_MODEL_PATH)
    agent = InventoryAgent(model_path=DEFAULT_INVENTORY_MODEL_PATH)

@app.before_request
def ensure_models():
    if predictor is None: init_models()

# ----------------- ENDPOINTS CHÍNH -----------------

@app.route('/api/predict', methods=['POST'])
def predict_demand():
    """Dự báo đơn lẻ và đưa ra khuyến nghị nhập hàng"""
    try:
        data = request.get_json()
        features = {
            'temperature': float(data.get('temperature', 25)),
            'flu_season': int(data.get('fluSeason', 0)),
            'rain': int(data.get('rain', 0)),
            'is_holiday': int(data.get('isHoliday', 0)),
            'is_weekend': int(data.get('isWeekend', 0)),
            'sales_lag_1': float(data.get('salesLag1', 50)),
            'sales_lag_7': float(data.get('salesLag7', 50)),
            'sales_lag_30': float(data.get('salesLag30', 50)),
            'storage_condition': data.get('storageCondition', 'Room temperature')
        }
        
        res = predictor.predict(data['medicineName'], data['region'], features)
        predicted_qty = res['prediction']
        
        # Cập nhật: Truyền predicted_demand vào Agent để tối ưu
        recommended = agent.get_best_action(
            data.get('currentInventory', 50),
            predicted_demand=predicted_qty
        )
        
        return jsonify({
            **res,
            "recommendedOrder": int(recommended),
            "timestamp": datetime.now().isoformat()
        }), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/analysis/chart', methods=['POST'])
def get_combined_chart():
    """
    Endpoint quan trọng: Nối 12 tháng lịch sử (CSV) với 30 ngày dự báo (AI)
    Giao diện sẽ dùng splitDate để phân cách
    """
    try:
        data = request.get_json()
        med_name = data.get('medicineName')
        region = data.get('region', 'Bắc')
        csv_path = data.get('dataPath', DEFAULT_DATA_PATH)
        if not os.path.isabs(csv_path):
            # Xử lý đường dẫn tương đối từ client
            if csv_path.startswith('data/'):
                csv_path = os.path.join(os.path.dirname(BASE_DIR), csv_path)  # Lên 1 cấp từ src để vào data
            else:
                csv_path = os.path.join(os.path.dirname(BASE_DIR), 'data', csv_path)  # Lên 1 cấp từ src rồi vào data

        if not os.path.exists(csv_path):
            return jsonify({"error": "Không tìm thấy dữ liệu lịch sử"}), 404

        # 1. Xử lý dữ liệu Quá khứ
        df = pd.read_csv(csv_path)
        # Đảm bảo cột date tồn tại
        if 'sale_date' in df.columns:
            df['date'] = pd.to_datetime(df['sale_date'])
        elif 'date' in df.columns:
            df['date'] = pd.to_datetime(df['date'])
        else:
            return jsonify({"error": "Không tìm thấy cột ngày tháng trong dữ liệu"}), 400
        hist_df = df[(df['medicine_name'] == med_name) & (df['region'] == region)]
        logger.info(f"Found {len(hist_df)} records for {med_name} in {region}")
        hist_df = hist_df.sort_values('date').tail(365) # Lấy 1 năm gần nhất

        chart_data = []
        for _, row in hist_df.iterrows():
            chart_data.append({
                "date": row['date'].strftime('%Y-%m-%d'),
                "quantity": float(row['quantity_sold']),
                "type": "history"
            })

        # 2. Xử lý dữ liệu Dự báo (30 ngày tiếp theo)
        if not chart_data:
            return jsonify({"error": "Không có dữ liệu cho thuốc/vùng này"}), 404

        last_date = pd.to_datetime(chart_data[-1]['date'])
        last_qty = chart_data[-1]['quantity']
        
        for i in range(1, 31):
            future_date = last_date + timedelta(days=i)
            # Giả lập features cho tương lai dựa trên ngày tháng
            future_features = {
                'temperature': 28.0 if future_date.month in [5,6,7,8] else 20.0,
                'flu_season': 1 if future_date.month in [1, 2, 11, 12] else 0,
                'rain': 0, 'is_holiday': 0,
                'is_weekend': 1 if future_date.weekday() >= 5 else 0,
                'sales_lag_1': last_qty,
                'sales_lag_7': last_qty * 0.98,
                'sales_lag_30': last_qty * 1.02,
                'storage_condition': 'Room temperature'
            }
            
            pred_res = predictor.predict(med_name, region, future_features)
            last_qty = pred_res['prediction'] # Dùng kết quả này làm lag cho ngày mai
            
            chart_data.append({
                "date": future_date.strftime('%Y-%m-%d'),
                "quantity": round(last_qty, 2),
                "lowerBound": round(pred_res['lower_bound'], 2),
                "upperBound": round(pred_res['upper_bound'], 2),
                "type": "forecast"
            })

        return jsonify({
            "medicineName": med_name,
            "region": region,
            "splitDate": last_date.strftime('%Y-%m-%d'),
            "data": chart_data
        }), 200

    except Exception as e:
        logger.error(f"Error chart: {e}")
        return jsonify({"error": str(e)}), 500

@app.route('/api/train', methods=['POST'])
def train_route():
    """Huấn luyện lại mô hình"""
    try:
        data = request.get_json()
        path = data.get('dataPath', DEFAULT_DATA_PATH)
        
        # Nếu path là relative path, chuyển thành absolute path
        if not os.path.isabs(path):
            # Xử lý cả trường hợp path có bắt đầu bằng "data/" hoặc không
            if path.startswith('data/'):
                path = os.path.join(os.path.dirname(BASE_DIR), path)  # Lên 1 cấp từ src để vào data
            else:
                path = os.path.join(os.path.dirname(BASE_DIR), 'data', path)  # Lên 1 cấp từ src rồi vào data

        logger.info(f"Checking data path: {path}")
        
        if not os.path.exists(path):
            return jsonify({"error": f"No such file or directory: '{path}'"}), 404

        # Load dữ liệu
        df = pd.read_csv(path)
        logger.info(f"Loaded {len(df)} records from {path}")
        
        # Huấn luyện demand model
        logger.info("Training demand model...")
        demand_success = predictor.train(df)
        logger.info(f"Demand model training result: {demand_success}")
        
        # Huấn luyện inventory model
        logger.info("Training inventory model...")
        agent.train(df['quantity_sold'].tolist(), episodes=500)
        logger.info("Inventory model training completed")
        
        return jsonify({
            "status": "success", 
            "message": "Models trained successfully",
            "records_processed": len(df)
        }), 200
    except Exception as e:
        logger.error(f"Training error: {e}")
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)