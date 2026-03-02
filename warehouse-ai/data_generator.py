import pandas as pd
import numpy as np

def generate_data(days=365):
    np.random.seed(42)

    data = pd.DataFrame({
        "temperature": np.random.randint(24, 36, days),
        "flu_season": np.random.randint(0, 2, days),
        "rain": np.random.randint(0, 2, days),
    })

    data["sales"] = (
        data["temperature"] * 3
        + data["flu_season"] * 50
        + data["rain"] * 20
        + np.random.randint(0, 25, days)
    )

    return data
