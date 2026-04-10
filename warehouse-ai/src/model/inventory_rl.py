import numpy as np
import pandas as pd
import joblib
import os
import logging

# Cấu hình logging để đồng bộ với hệ thống app.py
logger = logging.getLogger(__name__)

class InventoryAgent:
    def __init__(self, model_path=None):
        if model_path is None:
            # Đường dẫn đến thư mục warehouse-ai (lên 3 cấp từ src/model/)
            base_dir = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
            model_path = os.path.join(base_dir, 'model-ai', 'inventory_q_table.pkl')
        elif not os.path.isabs(model_path):
            base_dir = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
            model_path = os.path.join(base_dir, 'model-ai', model_path)

        # 1. Cấu hình Trạng thái và Hành động
        self.max_stock = 200
        self.states = self.max_stock + 1  # 0-200 = 201 states
        # Các mức nhập hàng linh hoạt
        self.actions = [0, 10, 20, 50, 100, 150, 200]
        
        self.model_path = model_path
        self.q_table = None
        
        # 2. Tham số học máy (Q-Learning)
        self.alpha = 0.1    # Tốc độ học (Learning rate)
        self.gamma = 0.9    # Hệ số chiết khấu tương lai
        self.epsilon = 0.1  # Tỷ lệ khám phá (Exploration)
        
        # 3. Thông số kinh tế để tính điểm thưởng (Reward)
        self.unit_profit = 15      # Lợi nhuận trên mỗi đơn vị bán được
        self.holding_cost = 1     # Chi phí lưu kho mỗi đơn vị
        self.out_of_stock_penalty = 25 # Phạt nặng nếu cháy hàng

        # Tạo thư mục chứa model nếu chưa có
        os.makedirs(os.path.dirname(self.model_path) if os.path.dirname(self.model_path) else '.', exist_ok=True)
        self.load_model()

    def load_model(self):
        """Tải tri thức đã học từ file pkl"""
        if os.path.exists(self.model_path):
            try:
                self.q_table = joblib.load(self.model_path)
                # Kiểm tra kích thước q-table
                expected_shape = (self.states, len(self.actions))
                if self.q_table.shape != expected_shape:
                    logger.warning(f"Q-table shape {self.q_table.shape} != expected {expected_shape}. Đang khởi tạo lại.")
                    self.q_table = np.zeros(expected_shape)
                else:
                    logger.info(f"Đã nạp thành công bộ não Agent từ {self.model_path}")
            except Exception as e:
                logger.error(f"Lỗi nạp Q-table: {e}. Đang khởi tạo bảng mới.")
                self.q_table = np.zeros((self.states, len(self.actions)))
        else:
            self.q_table = np.zeros((self.states, len(self.actions)))

    def save_model(self):
        """Lưu lại kết quả huấn luyện"""
        try:
            joblib.dump(self.q_table, self.model_path)
            logger.info(f"Đã lưu Q-table tại {self.model_path}")
        except Exception as e:
            logger.error(f"Không thể lưu file model: {e}")

    def train(self, demand_history, episodes=2000):
        """
        Huấn luyện Agent dựa trên dữ liệu lịch sử bán hàng
        """
        logger.info(f"Bắt đầu huấn luyện RL Agent ({episodes} episodes)...")
        
        if not demand_history or len(demand_history) == 0:
            demand_history = [np.random.randint(10, 50) for _ in range(100)]

        for episode in range(episodes):
            current_stock = np.random.randint(0, self.max_stock + 1)  # 0 đến 200

            for _ in range(30): # Giả lập 30 ngày giao dịch
                # Quyết định hành động (Epsilon-greedy)
                if np.random.rand() < self.epsilon:
                    action_idx = np.random.randint(len(self.actions))
                else:
                    # Đảm bảo current_stock trong bounds [0, max_stock]
                    safe_stock = min(max(0, current_stock), self.max_stock)
                    action_idx = np.argmax(self.q_table[safe_stock])
                
                order_qty = self.actions[action_idx]

                # Lấy nhu cầu ngẫu nhiên từ thực tế lịch sử
                daily_demand = int(np.random.choice(demand_history))
                
                # Cập nhật kho sau nhập hàng
                stock_after_order = min(current_stock + order_qty, self.max_stock)
                
                # Thực tế bán hàng
                actual_sold = min(stock_after_order, daily_demand)
                
                # Trạng thái tiếp theo (Kho cuối ngày)
                next_stock = max(0, stock_after_order - actual_sold)
                
                # --- TÍNH TOÁN PHẦN THƯỞNG (REWARD) ---
                reward = (actual_sold * self.unit_profit) - (stock_after_order * self.holding_cost)
                
                # Phạt nặng nếu để hết hàng khi khách đang cần
                if stock_after_order < daily_demand:
                    reward -= self.out_of_stock_penalty
                
                # Cập nhật giá trị Q (Công thức Bellman) - Đảm bảo bounds
                safe_current_stock = min(max(0, current_stock), self.max_stock)
                safe_next_stock = int(min(max(0, next_stock), self.max_stock))
                
                # Kiểm tra thêm bounds cho action_idx
                safe_action_idx = min(max(0, action_idx), len(self.actions) - 1)
                
                best_next_action = np.max(self.q_table[safe_next_stock])
                self.q_table[safe_current_stock, safe_action_idx] += self.alpha * (
                    reward + self.gamma * best_next_action - self.q_table[safe_current_stock, safe_action_idx]
                )

                current_stock = next_stock

        self.save_model()

    def get_best_action(self, current_stock, predicted_demand=None):
        """
        HÀM QUAN TRÒNG: Tính toán chính xác lượng nhập hàng dựa trên dự báo và tồn kho
        """
        # Đảm bảo index nằm trong giới hạn mảng
        safe_stock = int(min(max(0, current_stock), self.max_stock))
        
        # 1. Lấy đề xuất gốc dựa trên kinh nghiệm (Q-table) - chỉ dùng làm reference
        action_idx = np.argmax(self.q_table[safe_stock])
        base_recommendation = self.actions[action_idx]
        
        # 2. TÍNH TOÁN CHÍNH XÁC dựa trên dự báo AI
        if predicted_demand is not None:
            # Tính toán lượng thiếu hụt
            shortage = predicted_demand - safe_stock
            
            if shortage > 0:
                # Cân nhập thêm: lượng thiếu hụt + 10% buffer
                recommended_order = max(0, shortage * 1.1)
                logger.info(f"Nhu cầu cao: dự báo {predicted_demand:.1f}, tồn kho {safe_stock}, nhập {recommended_order:.1f}")
            elif predicted_demand < (safe_stock * 0.3):
                # Tồn kho quá dư, giảm nhập
                recommended_order = 0
                logger.info(f"Tồn kho dư: dự báo {predicted_demand:.1f}, tồn kho {safe_stock}, không nhập")
            else:
                # Tồn kho dư dư, giảm nhập
                recommended_order = max(0, (predicted_demand - safe_stock) * 0.8)
                logger.info(f"Tồn kho dư dư: dự báo {predicted_demand:.1f}, tồn kho {safe_stock}, nhập {recommended_order:.1f}")
        else:
            # Không có dự báo, dùng Q-table
            recommended_order = base_recommendation

        # 3. Giới hạn trong mức an toàn (0-200)
        recommended_order = min(max(0, recommended_order), self.max_stock)
        
        # 4. Làm tròn lên 5 đơn vị để tiện việc xuất kho
        recommended_order = int(round(recommended_order / 5) * 5)

        return int(recommended_order)