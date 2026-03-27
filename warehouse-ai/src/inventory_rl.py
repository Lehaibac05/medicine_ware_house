import numpy as np
import pandas as pd
import joblib
import os

class InventoryAgent:
    def __init__(self, model_path='models/inventory_q_table.pkl'):
        # 1. Cấu hình Trạng thái và Hành động 
        self.max_stock = 200 
        self.states = self.max_stock + 1
        self.actions = [0, 10, 20, 50, 100]
        
        self.model_path = model_path
        self.q_table = None
        
        # 2. Tham số học máy
        self.alpha = 0.1    # Learning rate
        self.gamma = 0.9    # Discount factor
        self.epsilon = 0.2  # Exploration rate
        
        # 3. Thông số kinh tế 
        self.unit_profit = 10      # Hệ số lợi nhuận (tương ứng sold * 10 ở bản cũ)
        self.holding_cost = 1     # Hệ số chi phí tồn kho (tương ứng -stock ở bản cũ)

        # Tạo thư mục và tải model
        os.makedirs(os.path.dirname(self.model_path) if os.path.dirname(self.model_path) else '.', exist_ok=True)
        self.load_model()

    def load_model(self):
        """Tính năng Mới: Tải Q-table từ file"""
        if os.path.exists(self.model_path):
            try:
                self.q_table = joblib.load(self.model_path)
                print(f"✅ Đã tải Q-table từ {self.model_path}")
            except:
                self.q_table = np.zeros((self.states, len(self.actions)))
        else:
            self.q_table = np.zeros((self.states, len(self.actions)))

    def save_model(self):
        """Tính năng Mới: Lưu Q-table"""
        joblib.dump(self.q_table, self.model_path)
        print(f"💾 Đã lưu Q-table tại {self.model_path}")

    def train(self, demand_history, episodes=2000):
        """
        Tính năng: Huấn luyện dựa trên lịch sử nhu cầu (demand_history)
        """
        print(f"🧠 Đang huấn luyện Agent (Logic: Profit - Stock) trong {episodes} vòng...")
        
        if len(demand_history) == 0:
            demand_history = [np.random.randint(10, 40) for _ in range(100)]

        for episode in range(episodes):
            current_stock = np.random.randint(20, 60) 

            for _ in range(30): 
                # Chọn hành động
                if np.random.rand() < self.epsilon:
                    action_idx = np.random.randint(len(self.actions))
                else:
                    action_idx = np.argmax(self.q_table[current_stock])
                
                order_qty = self.actions[action_idx]

                # Quan sát nhu cầu từ dữ liệu lịch sử
                daily_demand = np.random.choice(demand_history)
                
                # Cập nhật kho trước khi bán
                stock_after_order = min(current_stock + order_qty, self.max_stock)
                
                # Bán hàng
                actual_sold = min(stock_after_order, daily_demand)
                
                # Trạng thái tiếp theo (Tồn kho còn lại)
                next_stock = stock_after_order - actual_sold
                next_stock = min(max(0, next_stock), self.max_stock)
                
                # --- LOGIC REWARD  ---
                # Reward = (Hàng bán được * 10) - (Hàng tồn kho hiện tại)
                reward = (actual_sold * self.unit_profit) - (stock_after_order * self.holding_cost)

                # Cập nhật Q-table
                best_next_action = np.max(self.q_table[next_stock])
                self.q_table[current_stock, action_idx] += self.alpha * (
                    reward + self.gamma * best_next_action - self.q_table[current_stock, action_idx]
                )

                current_stock = next_stock

        self.save_model()

    def get_best_action(self, current_stock):
        """Tính năng: Hàm xuất quyết định cho Backend"""
        safe_stock = min(max(0, int(current_stock)), self.max_stock)
        action_idx = np.argmax(self.q_table[safe_stock])
        return self.actions[action_idx]