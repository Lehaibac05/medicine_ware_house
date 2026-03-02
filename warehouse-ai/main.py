from data_generator import generate_data
from demand_model import DemandPredictor
from inventory_rl import InventoryAgent

# 1. Sinh dữ liệu
data = generate_data()

# 2. Train model dự đoán nhu cầu
predictor = DemandPredictor()
predictor.train(data)

# 3. Dự đoán ngày mới
temperature = 32
flu = 1
rain = 0

predicted_sales = predictor.predict(temperature, flu, rain)

print("\nPredicted demand:", int(predicted_sales))

# 4. Train RL nhập kho
agent = InventoryAgent()
agent.train()

current_stock = 30

action = agent.best_action(current_stock)

print("Current stock:", current_stock)
print("Recommended order:", action)