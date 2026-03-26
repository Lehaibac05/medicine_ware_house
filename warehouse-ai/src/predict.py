from demand_model import DemandPredictor
import config
import pandas as pd

def predict_demand():
    """
    Sử dụng model đã huấn luyện để dự đoán nhu cầu với luồng xử lý từ config.py
    """
    print("🔍 Đang khởi tạo mô hình dự báo...")
    
    # 1. Load model đã train
    try:
        predictor = DemandPredictor()
    except Exception as e:
        print(f"❌ Không thể tải mô hình: {e}")
        return

    # 2. Chuẩn bị dữ liệu đầu vào (Ví dụ cho 1 sản phẩm)
    # Bạn có thể thay đổi các giá trị này tùy theo thực tế
    medicine_name = 'Paracetamol 500mg'
    region = 'Bắc'  # Thêm khu vực nếu model mới của bạn có hỗ trợ
    
    raw_features = {
        'temperature': 32,
        'flu_season': 1,
        'rain': 0,
        'sales_lag_1': 55,  # Doanh số ngày hôm qua
        'sales_lag_7': 48,  # Trung bình 7 ngày trước
        'sales_lag_30': 45, # Trung bình 30 ngày trước
        # Các cột thời gian khác sẽ được config.standardize_data tự động bổ sung
    }

    # 3. Chuẩn hóa dữ liệu qua config (Đảm bảo input giống y hệt lúc train)
    # Chuyển dictionary thành DataFrame để xử lý
    df_input = pd.DataFrame([raw_features])
    processed_features = config.standardize_data(df_input)
    
    # Chuyển ngược lại thành dict để đưa vào hàm predict của model
    # (Lấy row đầu tiên)
    final_features_dict = processed_features.iloc[0].to_dict()

    print(f"\n=== KẾT QUẢ DỰ ĐOÁN ===")
    print(f"Thuốc    : {medicine_name}")
    print(f"Khu vực  : {region}")
    
    # 4. Gọi hàm dự báo
    try:
        # Thử gọi với cấu trúc mới (có region)
        prediction = predictor.predict(medicine_name, region, final_features_dict)
    except TypeError:
        # Fallback nếu model của bạn vẫn dùng cấu trúc cũ (không có region)
        prediction = predictor.predict(medicine_name, final_features_dict)

    # 5. Hiển thị kết quả
    pred_val = prediction.get('prediction', 0)
    lower = prediction.get('lower_bound', 0)
    upper = prediction.get('upper_bound', 0)
    conf = prediction.get('confidence', 0.95) * 100

    print(f"Dự đoán  : {pred_val:.1f} đơn vị")
    print(f"Khoảng tin cậy ({conf:.0f}%): [{lower:.1f} - {upper:.1f}]")
    
    # Đưa ra lời khuyên nhanh
    if pred_val > raw_features['sales_lag_1'] * 1.2:
        print("⚠️ CẢNH BÁO: Nhu cầu có xu hướng tăng cao, cân nhắc nhập thêm hàng!")
    elif pred_val < raw_features['sales_lag_1'] * 0.8:
        print("📉 THÔNG BÁO: Nhu cầu giảm, tránh nhập hàng quá nhiều.")

if __name__ == "__main__":
    predict_demand()