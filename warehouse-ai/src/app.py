from flask import Flask, request, jsonify
from flask_cors import CORS
import pandas as pd
import numpy as np
import logging
import os
from datetime import datetime, timedelta

from model.demand_model import DemandPredictor
from model.inventory_rl import InventoryAgent
from services.data_service import clean_and_prepare_data, extract_historical_demand
from services.weather_service import get_weather_by_region
from main import get_real_lag_data

app = Flask(__name__)
CORS(app)

# Cấu hình Logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))  # Đường dẫn đến thư mục src
DEFAULT_DATA_PATH = os.path.join(os.path.dirname(BASE_DIR), 'data', 'pharmacy_training_final.csv')
DEFAULT_DEMAND_MODEL_PATH = os.path.join(os.path.dirname(BASE_DIR), 'model-ai', 'demand_hybrid_model.pkl')
DEFAULT_INVENTORY_MODEL_PATH = os.path.join(os.path.dirname(BASE_DIR), 'model-ai', 'inventory_q_table.pkl')

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
    try:
        data = request.get_json() or {}

        # VALIDATE
        if 'medicineName' not in data or 'region' not in data:
            return jsonify({"error": "Missing medicineName or region"}), 400

        weather = get_weather_by_region(data['region'])

      # LẤY LAG THẬT
        lags = get_real_lag_data(data['medicineName'], data['region'])

        if lags:
            lag1 = lags['sales_lag_1']
            lag7 = lags['sales_lag_7']
            lag30 = lags['sales_lag_30']
        else:
            # fallback nếu DB lỗi
            lag1 = float(data.get('salesLag1', 50))
            lag7 = float(data.get('salesLag7', 50))
            lag30 = float(data.get('salesLag30', 50))

        features = {
            'temperature': weather['temperature'] if weather else 25,
            'rain': weather['rain'] if weather else 0,
            'flu_season': int(data.get('fluSeason', 0)),
            'is_holiday': int(data.get('isHoliday', 0)),
            'is_weekend': int(data.get('isWeekend', 0)),
            'sales_lag_1': lag1,
            'sales_lag_7': lag7,
            'sales_lag_30': lag30,
            'storage_condition': data.get('storageCondition', 'Room temperature')
        }

        logger.info(f"FINAL FEATURES → {features}")

        logger.info(f"Predict request: {data}")

        res = predictor.predict(data['medicineName'], data['region'], features)

        logger.info(f"Predict result: {res}")

        predicted_qty = res.get('prediction', 0)

        # FALLBACK nếu model trả 0
        if predicted_qty <= 0:
            predicted_qty = (
                features['sales_lag_1'] * 0.5 +
                features['sales_lag_7'] * 0.3 +
                features['sales_lag_30'] * 0.2
            )
            res['prediction'] = predicted_qty
            res['lower_bound'] = predicted_qty * 0.7
            res['upper_bound'] = predicted_qty * 1.3
            res['note'] = "fallback heuristic applied"

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
        logger.error(f"Predict error: {e}")
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
        df = clean_and_prepare_data(df)

        # Filter by medicine name and region if region column exists
        if 'region' in df.columns:
            hist_df = df[(df['medicine_name'] == med_name) & (df['region'] == region)]
        else:
            hist_df = df[df['medicine_name'] == med_name]
        
        logger.info(f"Found {len(hist_df)} records for {med_name} in {region}")

        # Ensure we have 'date' column (either 'date' or 'sale_date')
        if 'date' not in hist_df.columns and 'sale_date' in hist_df.columns:
            hist_df = hist_df.copy()
            hist_df['date'] = hist_df['sale_date']
        
        hist_df = hist_df.sort_values('date').tail(365) # Lấy 1 năm gần nhất

        chart_data = []
        for _, row in hist_df.iterrows():
            date_val = row['date']
            if not isinstance(date_val, str):
                date_val = pd.to_datetime(date_val).strftime('%Y-%m-%d')
            chart_data.append({
                "date": date_val,
                "quantity": float(row['quantity_sold']),
                "type": "history"
            })

        # 2. Xử lý dữ liệu Dự báo (30 ngày tiếp theo)
        if not chart_data:
            return jsonify({"error": "Không có dữ liệu cho thuốc/vùng này"}), 404

        last_date = pd.to_datetime(chart_data[-1]['date'])
        history_values = [float(row['quantity_sold']) for _, row in hist_df.iterrows()]
        history_values = history_values[-30:]

        storage_condition = hist_df['storage_condition'].iloc[-1] if 'storage_condition' in hist_df.columns else 'Room temperature'
        weather = get_weather_by_region(region)

        holiday_dates = {
            (1, 1), (1, 30), (4, 30), (5, 1), (9, 2),
            (2, 10), (3, 8), (11, 20)
        }

        for i in range(1, 31):
            future_date = last_date + timedelta(days=i)

            sales_lag_1 = history_values[-1] if len(history_values) >= 1 else float(chart_data[-1]['quantity'])
            sales_lag_7 = history_values[-7] if len(history_values) >= 7 else sales_lag_1
            sales_lag_30 = history_values[-30] if len(history_values) >= 30 else sales_lag_1

            future_temp = weather['temperature'] if weather and 'temperature' in weather else (28.0 if future_date.month in [5, 6, 7, 8] else 20.0)
            future_rain = weather['rain'] if weather and 'rain' in weather else int(future_date.month in [6, 7, 8])

            future_features = {
                'temperature': float(future_temp),
                'rain': int(future_rain),
                'flu_season': int(future_date.month in [1, 2, 11, 12]),
                'is_holiday': int((future_date.month, future_date.day) in holiday_dates),
                'is_weekend': int(future_date.weekday() >= 5),
                'sales_lag_1': float(sales_lag_1),
                'sales_lag_7': float(sales_lag_7),
                'sales_lag_30': float(sales_lag_30),
                'storage_condition': storage_condition
            }

            logger.info(f"Future features for {future_date.strftime('%Y-%m-%d')}: {future_features}")

            pred_res = predictor.predict(med_name, region, future_features)
            predicted_qty = float(pred_res.get('prediction', 0.0))

            history_values.append(predicted_qty)
            if len(history_values) > 30:
                history_values.pop(0)

            chart_data.append({
                "date": future_date.strftime('%Y-%m-%d'),
                "quantity": round(predicted_qty, 2),
                "lowerBound": round(pred_res.get('lower_bound', 0.0), 2),
                "upperBound": round(pred_res.get('upper_bound', 0.0), 2),
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
    try:
        data = request.get_json() or {}

        path = data.get('dataPath', DEFAULT_DATA_PATH)

        # FIX PATH CHUẨN
        if not os.path.isabs(path):
            path = os.path.abspath(os.path.join(os.path.dirname(BASE_DIR), path))

        logger.info(f"Training with data path: {path}")

        if not os.path.exists(path):
            return jsonify({"error": f"No such file: {path}"}), 404

        df = pd.read_csv(path)
        df = clean_and_prepare_data(df)
        logger.info(f"Loaded and cleaned {len(df)} records")

        # TRAIN
        predictor.train(df)
        agent.train(df['quantity_sold'].tolist(), episodes=500)

        # QUAN TRỌNG: reload model
        init_models()

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