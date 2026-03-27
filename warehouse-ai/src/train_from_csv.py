import pandas as pd
import sys
from demand_model import DemandPredictor
import config

def train_from_csv(file_path):
    """
    Huấn luyện AI từ dữ liệu CSV, sử dụng luồng chuẩn hóa chung từ config.py
    """
    print(f"🚀 Đang nạp dữ liệu từ: {file_path}")
    
    # 1. Đọc dữ liệu thô
    try:
        raw_data = pd.read_csv(file_path)
    except FileNotFoundError:
        raise FileNotFoundError(f"Không tìm thấy file '{file_path}'. Vui lòng kiểm tra lại!")

    # 2. Chuẩn hóa dữ liệu qua config (áp dụng nguyên lý DRY)
    print("⚙️ Đang xử lý và chuẩn hóa dữ liệu...")
    data = config.standardize_data(raw_data)
    
    # 3. Khởi tạo và huấn luyện Model
    print("🧠 Đang huấn luyện mô hình AI...")
    predictor = DemandPredictor()
    metrics = predictor.train(data)
    
    print("\n✅ Huấn luyện hoàn tất!")
    
    # Dùng .get() để tránh lỗi nếu key trả về từ metrics bị khác tên
    mae = metrics.get('mae', metrics.get('test_mae', 0))
    r2 = metrics.get('r2', metrics.get('test_r2', 0))
    
    print(f"🔹 Sai số MAE     : {mae:.2f}")
    print(f"🔹 Độ chính xác R²: {r2:.2f}")
    
    return predictor, metrics

if __name__ == "__main__":
    # Lấy tên file từ command line hoặc dùng mặc định
    if len(sys.argv) > 1:
        csv_file = sys.argv[1]
    else:
        # Bạn có thể đổi tên file này khớp với file thực tế bạn đang có
        csv_file = "pharmacy_training_final.csv" 

    try:
        predictor, results = train_from_csv(csv_file)

        # --- CHẠY THỬ DỰ BÁO (TEST PREDICTION) ---
        print("\n" + "="*45)
        print("🧪 CHẠY THỬ DỰ BÁO CHO 1 SẢN PHẨM")
        print("="*45)
        
        # Giả lập dữ liệu nhập vào
        # Nhờ có config.standardize_data, nếu thiếu cột nào nó sẽ tự điền 0
        test_features = {
            'temperature': 30,
            'flu_season': 1,
            'rain': 0,
            'sales_lag_1': 50,
            'sales_lag_7': 45,
            'sales_lag_30': 40
        }

        # Gọi hàm predict
        # Sử dụng try-except để tự động thích ứng với cả model cũ (chưa có region) và mới (đã có region)
        try:
            # Nếu model của bạn đã được cập nhật để nhận thêm 'region'
            prediction = predictor.predict('Paracetamol 500mg', 'Bắc', test_features)
            print("Khu vực: Bắc")
        except TypeError:
            # Nếu model vẫn dùng cấu trúc cũ
            prediction = predictor.predict('Paracetamol 500mg', test_features)
        
        print(f"Thuốc: Paracetamol 500mg")
        
        # Lấy an toàn các giá trị từ dictionary kết quả
        pred_val = prediction.get('prediction', 0)
        lower = prediction.get('lower_bound', 0)
        upper = prediction.get('upper_bound', 0)
        
        print(f"Số lượng dự báo: {pred_val:.1f} đơn vị")
        if lower and upper:
            print(f"Khoảng tin cậy : [{lower:.1f} - {upper:.1f}]")

    except Exception as e:
        print(f"\n❌ Lỗi hệ thống: {e}")