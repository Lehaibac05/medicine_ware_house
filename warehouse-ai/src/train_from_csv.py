import pandas as pd
import sys
import os
from pathlib import Path
from demand_model import DemandPredictor
import config

def train_from_csv(file_path=None):
    """
    Huấn luyện mô hình AI từ file CSV và hiển thị đánh giá chi tiết.
    """
    # 1. XỬ LÝ ĐƯỜNG DẪN FILE
    if file_path is None:
        # Tự động tìm file trong thư mục data (cùng cấp hoặc cấp trên)
        base_path = Path(__file__).resolve().parent.parent
        file_path = base_path / 'data' / 'pharmacy_training_final.csv'
    else:
        file_path = Path(file_path)

    print(f"\n🚀 Đang nạp dữ liệu từ: {file_path}")
    
    # 2. ĐỌC DỮ LIỆU THÔ
    try:
        raw_data = pd.read_csv(file_path)
        print(f"📊 Đã tải thành công {len(raw_data)} dòng dữ liệu.")
    except Exception as e:
        print(f"❌ Lỗi không thể đọc file: {e}")
        return None, None

    # 3. CHUẨN HÓA DỮ LIỆU (Sử dụng hàm từ config.py)
    print("⚙️ Đang xử lý và chuẩn hóa dữ liệu theo cấu trúc hệ thống...")
    try:
        # Đảm bảo file config.py của bạn có hàm clean_and_prepare_data hoặc standardize_data
        if hasattr(config, 'clean_and_prepare_data'):
            data = config.clean_and_prepare_data(raw_data)
        else:
            data = config.standardize_data(raw_data)
    except Exception as e:
        print(f"❌ Lỗi khi chuẩn hóa dữ liệu: {e}")
        return None, None
    
    # 4. KHỞI TẠO VÀ HUẤN LUYỆN MODEL
    predictor = DemandPredictor()
    print("🧠 Đang huấn luyện mô hình Hybrid (Random Forest + XGBoost)...")
    metrics = predictor.train(data)
    
    # 5. HIỂN THỊ BÁO CÁO KẾT QUẢ
    print("\n" + "="*45)
    print(f"{'✅ HUẤN LUYỆN HOÀN TẤT':^45}")
    print("="*45)
    
    # Lấy các chỉ số an toàn từ dictionary metrics
    mae = metrics.get('mae', 0)
    rmse = metrics.get('rmse', 0)
    r2 = metrics.get('r2', 0)
    
    print(f"🔹 Sai số MAE (Trung bình)  : {mae:.2f}")
    print(f"🔹 Sai số RMSE (Độ lệch)    : {rmse:.2f}")
    print(f"🔹 Độ chính xác R² (0 -> 1): {r2:.4f}")
    print("="*45)
    
    return predictor, metrics

if __name__ == "__main__":
    # Nhận đường dẫn file từ dòng lệnh nếu có (ví dụ: python train_from_csv.py my_data.csv)
    csv_file = sys.argv[1] if len(sys.argv) > 1 else None

    try:
        predictor, results = train_from_csv(csv_file)

        if predictor:
            print("\n🧪 CHẠY THỬ DỰ BÁO KIỂM TRA (Dữ liệu giả lập):")
            
            # ĐÂY LÀ PHẦN QUAN TRỌNG: Truyền đầy đủ các tính năng để tránh lỗi Index
            test_features = {
                'storage_condition': 'Room temperature', # Điều kiện bảo quản
                'temperature': 30.5,                    # Nhiệt độ
                'flu_season': 1,                        # Đang mùa dịch
                'rain': 0,                              # Không mưa
                'is_holiday': 0,                        # Không phải ngày lễ
                'is_weekend': 1,                        # Là cuối tuần
                'sales_lag_1': 50,                      # Doanh số hôm qua
                'sales_lag_7': 320,                     # Doanh số 7 ngày trước
                'sales_lag_30': 1200                    # Doanh số tháng trước
            }
            
            # Thực hiện dự báo
            med_name = 'Paracetamol 500mg'
            region = 'Bắc'
            
            res = predictor.predict(med_name, region, test_features)
            
            print(f"📍 Đối tượng: {med_name} | Khu vực: {region}")
            print(f"📈 Kết quả dự báo: {res['prediction']:.1f} đơn vị")
            print(f"🛡️ Khoảng an toàn: [{res['lower_bound']:.1f} - {res['upper_bound']:.1f}]")
            print("\n✅ Hệ thống sẵn sàng hoạt động!")

    except Exception as e:
        print(f"\n❌ Lỗi thực thi hệ thống: {e}")
        import traceback
        traceback.print_exc()