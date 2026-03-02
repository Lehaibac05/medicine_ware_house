from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_absolute_error
import pandas as pd

class DemandPredictor:
    def __init__(self):
        self.model = RandomForestRegressor(n_estimators=100)

    def train(self, data):
        X = data[["temperature", "flu_season", "rain"]]
        y = data["sales"]

        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=0.2, random_state=42
        )

        self.model.fit(X_train, y_train)

        pred = self.model.predict(X_test)
        error = mean_absolute_error(y_test, pred)

        print("Model MAE:", error)

    def predict(self, temperature, flu, rain):

        input_data = pd.DataFrame(
            [[temperature, flu, rain]],
            columns=["temperature", "flu_season", "rain"]
        )

        return self.model.predict(input_data)[0]

