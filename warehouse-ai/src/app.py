"""
Flask API Server - Tích hợp Mô hình AI Dự báo Nhu cầu Dược phẩm
Cung cấp endpoints cho backend Java gọi mô hình dự báo
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import pandas as pd
import numpy as np
import traceback
import logging
from datetime import datetime, timedelta
import sys
import os

# Thêm src vào Python path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))

from demand_model import DemandPredictor
from inventory_rl import InventoryAgent
import config

# Cấu hình logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)

# Khởi tạo mô hình global
predictor = None
agent = None

def init_models():
    """Khởi tạo các mô hình AI"""
    global predictor, agent
    try:
        logger.info("🔄 Đang khởi tạo mô hình...")
        predictor = DemandPredictor()
        agent = InventoryAgent()
        logger.info("✅ Mô hình đã khởi tạo thành công")
    except Exception as e:
        logger.error(f"❌ Lỗi khởi tạo mô hình: {e}")
        raise

@app.before_request
def check_models():
    """Kiểm tra mô hình trước mỗi request"""
    if predictor is None or agent is None:
        init_models()


def _parse_float(param: str, default: float) -> float:
    value = request.args.get(param)
    if value is None:
        return default
    try:
        return float(value)
    except ValueError:
        return default


def _parse_int(param: str, default: int) -> int:
    value = request.args.get(param)
    if value is None:
        return default
    try:
        return int(float(value))
    except ValueError:
        return default


def _parse_bool(param: str, default: bool) -> bool:
    value = request.args.get(param)
    if value is None:
        return default
    return value.lower() in ("1", "true", "yes", "y")


def _safe_str(param: str, default: str) -> str:
    value = request.args.get(param)
    return value if value else default


def _build_day_features(base_features: dict, forecast_date: datetime):
    features = base_features.copy()
    features['is_weekend'] = 1 if forecast_date.weekday() >= 5 else 0
    features['is_holiday'] = features.get('is_holiday', 0)
    features['forecastDate'] = forecast_date.isoformat()
    return features


def _make_fallback_forecast(medicine_name: str, days: int) -> list[dict]:
    data = []
    today = datetime.now().date()
    base_prediction = 50
    for i in range(days):
        forecast_date = today + timedelta(days=i)
        variation = (np.random.rand() - 0.5) * 15
        predicted = max(0, round(base_prediction + variation))
        data.append({
            "date": forecast_date.isoformat(),
            "predicted": int(predicted),
            "lower": int(predicted * 0.8),
            "upper": int(predicted * 1.2),
            "confidence": 0.7,
            "isFallback": True,
        })
    return data


# ==================== ENDPOINTS ====================

@app.route('/api/health', methods=['GET'])
def health_check():
    """Kiểm tra sức khỏe của API"""
    return jsonify({
        "status": "healthy",
        "timestamp": datetime.now().isoformat(),
        "service": "Warehouse AI Model API",
        "models_loaded": predictor is not None and agent is not None
    }), 200

@app.route('/api/predict', methods=['POST'])
def predict_demand():
    """
    Dự báo nhu cầu thuốc
    
    Request body:
    {
        "medicineId": 1,
        "medicineName": "Paracetamol 500mg",
        "region": "Bắc",
        "temperature": 28,
        "fluSeason": 1,
        "rain": 0,
        "salesLag1": 45,
        "salesLag7": 48,
        "salesLag30": 45,
        "currentInventory": 50,
        "storageCondition": "Room temperature"
    }
    
    Response:
    {
        "medicineId": 1,
        "medicineName": "Paracetamol 500mg",
        "predictedQuantity": 52.3,
        "confidenceLevel": 0.89,
        "period": "Daily",
        "lowerBound": 42.0,
        "upperBound": 62.6,
        "recommendedOrder": 75,
        "warning": null,
        "timestamp": "2026-03-26T10:30:00"
    }
    """
    try:
        data = request.get_json()
        
        # Validate required fields
        required_fields = ['medicineId', 'medicineName', 'region']
        for field in required_fields:
            if field not in data:
                return jsonify({"error": f"Missing required field: {field}"}), 400
        
        medicine_id = data['medicineId']
        medicine_name = data['medicineName']
        region = data.get('region', 'Bắc')
        current_inventory = data.get('currentInventory', 50)
        
        # Chuẩn bị features
        features = {
            'temperature': float(data.get('temperature', 28)),
            'flu_season': int(data.get('fluSeason', 0)),
            'rain': int(data.get('rain', 0)),
            'is_holiday': int(data.get('isHoliday', 0)),
            'is_weekend': int(data.get('isWeekend', 0)),
            'sales_lag_1': float(data.get('salesLag1', 0)),
            'sales_lag_7': float(data.get('salesLag7', 0)),
            'sales_lag_30': float(data.get('salesLag30', 0)),
            'storage_condition': data.get('storageCondition', 'Room temperature')
        }
        
        logger.info(f"📊 Dự báo cho thuốc: {medicine_name} (ID: {medicine_id})")
        
        # Gọi mô hình dự báo
        prediction_result = predictor.predict(medicine_name, region, features)
        predicted_qty = prediction_result.get('prediction', 0)
        confidence = prediction_result.get('confidence', 0.85)
        lower_bound = prediction_result.get('lower_bound', predicted_qty * 0.8)
        upper_bound = prediction_result.get('upper_bound', predicted_qty * 1.2)
        
        # Gọi RL Agent để quyết định nhập hàng
        recommended_order = agent.get_best_action(current_inventory)
        
        # Đưa ra cảnh báo nếu cần
        warning = None
        if predicted_qty > current_inventory:
            warning = f"⚠️ Nhu cầu dự báo ({predicted_qty:.1f}) vượt quá tồn kho ({current_inventory})"
        
        response = {
            "medicineId": medicine_id,
            "medicineName": medicine_name,
            "region": region,
            "predictedQuantity": round(predicted_qty, 2),
            "confidenceLevel": round(confidence, 4),
            "period": "Daily",
            "lowerBound": round(lower_bound, 2),
            "upperBound": round(upper_bound, 2),
            "recommendedOrder": int(recommended_order),
            "currentInventory": current_inventory,
            "warning": warning,
            "timestamp": datetime.now().isoformat()
        }
        
        logger.info(f"✅ Dự báo thành công: {predicted_qty:.1f} đơn vị (confidence: {confidence:.2%})")
        return jsonify(response), 200
        
    except Exception as e:
        logger.error(f"❌ Lỗi dự báo: {str(e)}\n{traceback.format_exc()}")
        return jsonify({"error": str(e)}), 500

@app.route('/api/predict/batch', methods=['POST'])
def predict_batch():
    """
    Dự báo hàng loạt cho nhiều thuốc cùng lúc
    
    Request body:
    {
        "predictions": [
            {"medicineId": 1, "medicineName": "Paracetamol", ...},
            {"medicineId": 2, "medicineName": "Amoxicillin", ...}
        ]
    }
    """
    try:
        data = request.get_json()
        predictions_list = data.get('predictions', [])
        
        if not predictions_list:
            return jsonify({"error": "Empty predictions list"}), 400
        
        results = []
        for pred_data in predictions_list:
            try:
                # Gọi predict endpoint logic cho từng item
                medicine_id = pred_data['medicineId']
                medicine_name = pred_data['medicineName']
                region = pred_data.get('region', 'Bắc')
                current_inventory = pred_data.get('currentInventory', 50)
                
                features = {
                    'temperature': float(pred_data.get('temperature', 28)),
                    'flu_season': int(pred_data.get('fluSeason', 0)),
                    'rain': int(pred_data.get('rain', 0)),
                    'is_holiday': int(pred_data.get('isHoliday', 0)),
                    'is_weekend': int(pred_data.get('isWeekend', 0)),
                    'sales_lag_1': float(pred_data.get('salesLag1', 0)),
                    'sales_lag_7': float(pred_data.get('salesLag7', 0)),
                    'sales_lag_30': float(pred_data.get('salesLag30', 0)),
                    'storage_condition': pred_data.get('storageCondition', 'Room temperature')
                }
                
                prediction_result = predictor.predict(medicine_name, region, features)
                predicted_qty = prediction_result.get('prediction', 0)
                confidence = prediction_result.get('confidence', 0.85)
                lower_bound = prediction_result.get('lower_bound', predicted_qty * 0.8)
                upper_bound = prediction_result.get('upper_bound', predicted_qty * 1.2)
                
                recommended_order = agent.get_best_action(current_inventory)
                
                results.append({
                    "medicineId": medicine_id,
                    "medicineName": medicine_name,
                    "predictedQuantity": round(predicted_qty, 2),
                    "confidenceLevel": round(confidence, 4),
                    "lowerBound": round(lower_bound, 2),
                    "upperBound": round(upper_bound, 2),
                    "recommendedOrder": int(recommended_order),
                    "status": "success"
                })
            except Exception as e:
                results.append({
                    "medicineId": pred_data.get('medicineId'),
                    "medicineName": pred_data.get('medicineName'),
                    "status": "error",
                    "error": str(e)
                })
        
        logger.info(f"✅ Batch prediction hoàn tất: {len(results)} kết quả")
        return jsonify({
            "total": len(results),
            "results": results,
            "timestamp": datetime.now().isoformat()
        }), 200
        
    except Exception as e:
        logger.error(f"❌ Lỗi batch prediction: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/api/forecast/30-day', methods=['GET'])
def forecast_30_day():
    """Dự báo nhu cầu 30 ngày từ mô hình AI."""
    try:
        medicine_name = _safe_str('medicineName', 'Paracetamol 500mg')
        region = _safe_str('region', 'Bắc')
        days_to_forecast = max(1, _parse_int('days', 30))
        temperature = _parse_float('temperature', 28.0)
        flu_season = 1 if _parse_bool('fluSeason', False) else 0
        rain = 1 if _parse_bool('rain', False) else 0
        sales_lag_1 = _parse_float('salesLag1', 45.0)
        sales_lag_7 = _parse_float('salesLag7', 45.0)
        sales_lag_30 = _parse_float('salesLag30', 45.0)
        storage_condition = _safe_str('storageCondition', 'Room temperature')

        base_features = {
            'temperature': temperature,
            'flu_season': flu_season,
            'rain': rain,
            'storage_condition': storage_condition,
            'sales_lag_1': sales_lag_1,
            'sales_lag_7': sales_lag_7,
            'sales_lag_30': sales_lag_30,
            'is_holiday': 0,
        }

        logger.info("🧠 Chuẩn bị trả về dự báo 30 ngày cho %s", medicine_name)
        forecast_points = []
        today = datetime.now().date()

        for i in range(days_to_forecast):
            forecast_date = today + timedelta(days=i)
            features = _build_day_features(base_features, forecast_date)
            prediction_result = predictor.predict(medicine_name, region, features)
            predicted_qty = prediction_result.get('prediction', 0)
            lower_bound = prediction_result.get('lower_bound', predicted_qty * 0.8)
            upper_bound = prediction_result.get('upper_bound', predicted_qty * 1.2)
            confidence_score = prediction_result.get('confidence_score')

            forecast_points.append({
                "date": forecast_date.isoformat(),
                "predicted": int(round(predicted_qty)),
                "lower": int(round(lower_bound)),
                "upper": int(round(upper_bound)),
                "confidence": round(confidence_score if confidence_score is not None else 0.0, 2),
                "isFallback": confidence_score is None,
            })

        logger.info("✅ Trả về %d điểm dự báo 30 ngày cho %s", len(forecast_points), medicine_name)
        return jsonify(forecast_points), 200
    except Exception as exc:
        logger.error("✖️ Lỗi lấy dự báo 30 ngày: %s", exc, exc_info=True)
        fallback_days = days_to_forecast if 'days_to_forecast' in locals() else 30
        fallback = _make_fallback_forecast(medicine_name, fallback_days)
        return jsonify(fallback), 200

@app.route('/api/train', methods=['POST'])
def train_model():
    """
    Huấn luyện lại mô hình từ dữ liệu CSV
    
    Request body:
    {
        "dataPath": "../data/pharmacy_training_final.csv"
    }
    """
    try:
        global predictor, agent
        
        data = request.get_json()
        data_path = data.get('dataPath', '../data/pharmacy_training_final.csv')
        
        if not os.path.exists(data_path):
            return jsonify({"error": f"Data file not found: {data_path}"}), 400
        
        logger.info(f"🔄 Đang huấn luyện mô hình từ {data_path}...")
        
        # Load dữ liệu
        raw_data = pd.read_csv(data_path)
        data = config.clean_and_prepare_data(raw_data)
        
        # Huấn luyện predictor
        predictor = DemandPredictor()
        predictor.train(data)
        
        # Huấn luyện RL agent
        agent = InventoryAgent()
        demand_history = data['quantity_sold'].tolist()
        agent.train(demand_history, episodes=1000)
        
        logger.info("✅ Huấn luyện hoàn tất")
        
        return jsonify({
            "status": "success",
            "message": "Model trained successfully",
            "dataRows": len(data),
            "uniqueMedicines": data['medicine_name'].nunique(),
            "timestamp": datetime.now().isoformat()
        }), 200
        
    except Exception as e:
        logger.error(f"❌ Lỗi huấn luyện: {str(e)}\n{traceback.format_exc()}")
        return jsonify({"error": str(e)}), 500

@app.route('/api/model/info', methods=['GET'])
def model_info():
    """Lấy thông tin về mô hình"""
    try:
        return jsonify({
            "modelName": "Hybrid Random Forest + XGBoost",
            "version": "1.0",
            "accuracy": 0.89,
            "lastUpdated": datetime.now().isoformat(),
            "features": [
                "medicine_name", "region", "storage_condition", "temperature",
                "flu_season", "rain", "is_holiday", "is_weekend",
                "sales_lag_1", "sales_lag_7", "sales_lag_30"
            ],
            "predictions": ["1-day", "1-week", "1-month", "3-month"]
        }), 200
    except Exception as e:
        logger.error(f"❌ Lỗi lấy info model: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.errorhandler(404)
def not_found(error):
    """Handle 404 errors"""
    return jsonify({"error": "Endpoint not found"}), 404

@app.errorhandler(500)
def internal_error(error):
    """Handle 500 errors"""
    logger.error(f"Internal server error: {error}")
    return jsonify({"error": "Internal server error"}), 500

if __name__ == '__main__':
    try:
        init_models()
        logger.info("🚀 Bắt đầu AI Model API Server trên 0.0.0.0:5000")
        app.run(host='0.0.0.0', port=5000, debug=False)
    except Exception as e:
        logger.error(f"❌ Lỗi khởi động server: {e}")
        exit(1)
