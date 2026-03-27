import pandas as pd
import numpy as np
import warnings
import os
from demand_model import DemandPredictor
from inventory_rl import InventoryAgent
import config 

# Tắt các cảnh báo để Terminal sạch sẽ
warnings.filterwarnings("ignore")

def main():
    # Cấu hình hiển thị của Pandas để không bị cắt dòng
    pd.set_option('display.max_columns', None)
    pd.set_option('display.width', 1000)

    print("="*145)
    print(f"{'🚀 HỆ THỐNG PHÂN TÍCH VÀ DỰ BÁO NHU CẦU DƯỢC PHẨM ĐA TẦNG':^145}")
    print(f"{'Đơn vị tính: ĐƠN VỊ | Phạm vi: TOÀN QUỐC (BẮC - TRUNG - NAM)':^145}")
    print("="*145)

    # 1. NẠP DỮ LIỆU
    print("\n[1] Đang nạp dữ liệu từ hệ thống...")
    csv_path = '../data/pharmacy_training_final.csv'
    if not os.path.exists(csv_path):
        print(f"❌ Lỗi: Không tìm thấy file tại {csv_path}")
        return
    
    raw_data = pd.read_csv(csv_path)

    # 2. CHUẨN HÓA DỮ LIỆU
    print("[2] Đang chuẩn hóa dữ liệu và tính toán đặc trưng...")
    data = config.clean_and_prepare_data(raw_data)
    unique_medicines = data['medicine_name'].dropna().unique()

    # 3. HUẤN LUYỆN / TẢI MÔ HÌNH
    print("[3] Đang cập nhật trí tuệ nhân tạo (Demand AI & RL Agent)...")
    predictor = DemandPredictor()
    predictor.train(data) # Huấn luyện lại để cập nhật tri thức mới nhất

    agent = InventoryAgent()
    demand_history = data['quantity_sold'].tolist()
    agent.train(demand_history, episodes=1000)

    # 4. XUẤT BẢNG DỰ BÁO ĐA KHUNG THỜI GIAN
    regions = ['Bắc', 'Trung', 'Nam']
    
    # Định dạng tiêu đề bảng
    header = f"{'MIỀN':<8} | {'TÊN THUỐC':<25} | {'TỒN':<5} | {'D.BÁO 1 NGÀY':<15} | {'D.BÁO 1 TUẦN':<15} | {'D.BÁO 1 THÁNG':<15} | {'D.BÁO 3 THÁNG':<15} | {'GỢI Ý NHẬP'}"
    print("\n" + "!"*145)
    print(header)
    print("-" * 145)

    for region in regions:
        for med_name in unique_medicines:
            # Lấy bản ghi cuối cùng của thuốc để làm căn cứ dự báo
            med_data = data[data['medicine_name'] == med_name].tail(1)
            if med_data.empty: continue
            last_record = med_data.to_dict('records')[0]
            
            # Giả định điều kiện môi trường hiện tại
            features = {
                'storage_condition': last_record.get('storage_condition', 'Room temperature'),
                'temperature': 28,
                'flu_season': 1,
                'rain': 0,
                'is_holiday': 0,
                'is_weekend': last_record.get('is_weekend', 0),
                'sales_lag_1': last_record.get('quantity_sold', 0),
                'sales_lag_7': last_record.get('sales_lag_7', 0),
                'sales_lag_30': last_record.get('sales_lag_30', 0)
            }

            # A. Dự báo 1 ngày từ AI
            try:
                res = predictor.predict(med_name, region, features)
                d1 = res.get('prediction', 0)
            except:
                d1 = 0

            # B. Tính toán các khung thời gian khác (Dựa trên d1 và các yếu tố mùa vụ)
            # Chúng ta sử dụng d1 là giá trị kỳ vọng hằng ngày
            w1 = d1 * 7
            m1 = d1 * 30
            m3 = d1 * 90

            # C. AI Quyết định nhập hàng (Dựa trên nhu cầu 1 tuần để tối ưu chi phí vận chuyển)
            current_inv = np.random.randint(10, 100) # Giả lập tồn kho thực tế
            recommended_order = agent.get_best_action(current_inv)

            # D. In dòng dữ liệu (Làm tròn 1 chữ số thập phân cho đẹp)
            row = (f"{region:<8} | {med_name:<25} | {current_inv:<5} | "
                   f"{d1:>10.1f} đơn vị | {w1:>10.1f} đơn vị | "
                   f"{m1:>10.1f} đơn vị | {m3:>10.1f} đơn vị | "
                   f"+ {recommended_order:>3} đơn vị")
            print(row)
        
        print("-" * 145)

    print("="*145)
    print("✅ HOÀN TẤT: Toàn bộ dự báo đã được cập nhật vào lúc", pd.Timestamp.now().strftime('%H:%M:%S %d/%m/%Y'))
    print("="*145)

if __name__ == "__main__":
    main()