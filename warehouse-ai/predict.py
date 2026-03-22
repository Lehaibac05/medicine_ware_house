from demand_model import DemandPredictor

def predict_demand():
    """
    Dùng model đã train để dự đoán nhu cầu
    """
    # Load model đã train
    predictor = DemandPredictor()

    # Dữ liệu ngày hôm nay
    features = {
        'temperature': 32,
        'flu_season': 1,  # 1 = yes, 0 = no
        'rain': 0,
        'sales_lag_1': 55,  # Bán được bao nhiêu ngày hôm qua
        'sales_lag_7': 48,  # Trung bình 7 ngày trước
        'sales_lag_30': 45, # Trung bình 30 ngày trước
        'day_of_week': 3,   # 0=Monday, 6=Sunday
        'is_weekend': 0,
        'manufacturer': 'DHG Pharma'
    }

    # Dự đoán
    prediction = predictor.predict('Paracetamol 500mg', features)

    print("=== DỰ ĐOÁN NHU CẦU ===")
    print(f"Thuốc: Paracetamol 500mg")
    print(f"Dự đoán bán: {prediction['prediction']:.0f} viên")
    print(f"Khoảng tin cậy: [{prediction['lower_bound']:.0f}, {prediction['upper_bound']:.0f}]")
    print(f"Độ tin cậy: {prediction['confidence']*100:.0f}%")

    return prediction

if __name__ == "__main__":
    predict_demand()
