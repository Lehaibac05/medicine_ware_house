import requests
from functools import lru_cache
import os

WEATHER_API_KEY = "58dae377083df906dfaff20ba8bcb92a"

def map_region_to_city(region: str) -> str:
    region = region.lower()

    if region in ["bắc", "north"]:
        return "Hanoi"
    elif region in ["trung", "central"]:
        return "Da Nang"
    elif region in ["nam", "south"]:
        return "Ho Chi Minh"
    
    return "Hanoi"

@lru_cache(maxsize=10)
def get_weather_by_region(region: str):
    city = map_region_to_city(region)

    try:
        url = (
            f"https://api.openweathermap.org/data/2.5/weather"
            f"?q={city}&appid={WEATHER_API_KEY}&units=metric"
        )

        res = requests.get(url, timeout=5)

        print("WEATHER RAW:", res.text)  # debug

        data = res.json()

        if res.status_code != 200:
            print("API ERROR:", data)
            return None

        temp = data['main']['temp']
        weather_main = data['weather'][0]['main'].lower()

        result = {
            "temperature": temp,
            "rain": 1 if "rain" in weather_main else 0
        }

        print("WEATHER PARSED:", result)

        return result

    except Exception as e:
        print("Weather exception:", e)
        return None