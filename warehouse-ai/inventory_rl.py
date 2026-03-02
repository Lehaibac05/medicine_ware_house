import numpy as np


class InventoryAgent:
    def __init__(self):
        self.states = 100
        self.actions = [0, 10, 20, 30, 40]

        self.Q = np.zeros((self.states, len(self.actions)))

        self.alpha = 0.1
        self.gamma = 0.9
        self.epsilon = 0.2

    def train(self, episodes=1000):
        for _ in range(episodes):
            stock = np.random.randint(20, 60)

            for _ in range(30):
                if np.random.rand() < self.epsilon:
                    action_id = np.random.randint(len(self.actions))
                else:
                    action_id = np.argmax(self.Q[stock])

                action = self.actions[action_id]

                demand = np.random.randint(10, 40)

                stock += action
                stock = min(stock, self.states - 1)

                sold = min(stock, demand)

                reward = sold * 10 - stock

                stock = max(0, stock - sold)
                next_stock = min(stock, self.states - 1)

                current_stock = min(stock, self.states - 1)

                self.Q[current_stock, action_id] += self.alpha * (

                    reward
                    + self.gamma * np.max(self.Q[next_stock])
                    - self.Q[stock, action_id]
                )

                stock = next_stock

    def best_action(self, stock):
        return self.actions[np.argmax(self.Q[stock])]
