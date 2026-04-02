import { apiFetch } from './api';

export interface Forecast {
  forecastId: number;
  medicineId: number;
  predictedQuantity: number;
  period: string;
  confidenceLevel: number;
  model: AIModel;
}

export interface AIModel {
  modelId: number;
  modelName: string;
  version: string;
  accuracy: number;
}

export interface ForecastPrediction {
  medicineId: number;
  predictedQuantity: number;
  confidenceLevel: number;
  period: string;
  lowerBound: number;
  upperBound: number;
  recommendedOrder?: number;
  warning?: string;
  isFallback?: boolean;
}

export interface ForecastPoint {
  date: string;
  predicted: number;
  lower: number;
  upper: number;
  confidence: number;
  isFallback?: boolean;
}

export interface ModelInfo {
  modelName: string;
  version: string;
  accuracy: number;
  lastUpdated: string;
  predictions: string[];
  trainingStatus?: string;
}

export type Forecast30DayParams = {
  medicineId?: number;
  medicineName?: string;
  region?: string;
  temperature?: number;
  fluSeason?: number | boolean;
  rain?: number | boolean;
  currentInventory?: number;
  salesLag1?: number;
  salesLag7?: number;
  salesLag30?: number;
  storageCondition?: string;
  days?: number;
};

// Mock 30-day data for demo
const getMock30DayData = (): ForecastPoint[] => {
  const data: ForecastPoint[] = [];
  const today = new Date();
  
  for (let i = 0; i < 30; i++) {
    const date = new Date(today);
    date.setDate(today.getDate() + i);
    
    // Generate realistic prediction with some variation
    const basePrediction = 45 + Math.sin(i / 5) * 10 + Math.random() * 5;
    const predicted = Math.max(0, Math.round(basePrediction));
    const lower = Math.round(predicted * 0.8);
    const upper = Math.round(predicted * 1.2);
    const confidence = 0.75 + Math.random() * 0.2; // 0.75-0.95
    
    data.push({
      date: date.toISOString().split('T')[0],
      predicted,
      lower,
      upper,
      confidence: Math.round(confidence * 100) / 100,
      isFallback: true,
    });
  }
  
  return data;
};

export const forecastApi = {
  // Get all forecasts from backend
  getAllForecasts: async (): Promise<Forecast[]> => {
    return apiFetch<Forecast[]>('/api/forecast');
  },

  // Get 30-day forecast data from AI
  get30DayForecast: async (params: Forecast30DayParams = {}): Promise<ForecastPoint[]> => {
    const buildQuery = (queryParams: Forecast30DayParams) => {
      const searchParams = new URLSearchParams();
      Object.entries(queryParams).forEach(([key, value]) => {
        if (value === undefined || value === null) {
          return;
        }
        if (typeof value === 'boolean') {
          searchParams.set(key, value ? '1' : '0');
          return;
        }
        searchParams.set(key, String(value));
      });
      const query = searchParams.toString();
      return query ? `/api/forecast/30-day?${query}` : "/api/forecast/30-day";
    };

    const fetchFromApi = async (queryParams: Forecast30DayParams) => {
      try {
        return await apiFetch<ForecastPoint[]>(buildQuery(queryParams));
      } catch (error) {
        console.error("30-day forecast API failed:", error);
        console.warn("Using mock data for 30-day forecast");
        return getMock30DayData();
      }
    };

    if (params.medicineId && !Number.isNaN(params.medicineId)) {
      return fetchFromApi(params);
    }

    try {
      const medicines = await apiFetch<{ medicineId: number; name: string }[]>("/api/medicines");

      if (medicines.length > 0) {
        const defaultMedicine = medicines[0];
        return fetchFromApi({
          ...params,
          medicineId: defaultMedicine.medicineId,
          medicineName: params.medicineName ?? defaultMedicine.name,
        });
      }

      return [];
    } catch (error) {
      console.error("Error in get30DayForecast:", error);
      return getMock30DayData();
    }
  },

  // Get forecast by ID (backend)
  getForecastById: async (id: number): Promise<Forecast> => {
    return apiFetch<Forecast>(`/api/forecast/${id}`);
  },

  // Predict demand for a medicine
  predictDemand: async (request: {
    medicineId: number;
    medicineName?: string;
    region?: string;
    temperature?: number;
    fluSeason?: boolean;
    rain?: boolean;
    currentInventory?: number;
    salesLag1?: number;
    salesLag7?: number;
    salesLag30?: number;
    storageCondition?: string;
  }): Promise<ForecastPrediction> => {
    const payload = {
      medicineId: request.medicineId,
      medicineName: request.medicineName ?? '',
      region: request.region ?? 'Bắc',
      temperature: request.temperature ?? 28,
      fluSeason: request.fluSeason ? 1 : 0,
      rain: request.rain ? 1 : 0,
      currentInventory: request.currentInventory ?? 50,
      salesLag1: request.salesLag1 ?? 0,
      salesLag7: request.salesLag7 ?? 0,
      salesLag30: request.salesLag30 ?? 0,
      storageCondition: request.storageCondition ?? 'Room temperature',
    };

    return apiFetch<ForecastPrediction>('/api/forecast/predict', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  getModelInfo: async (): Promise<ModelInfo> => {
    return apiFetch<ModelInfo>('/api/model/info');
  },

  // Create new forecast
  createForecast: async (forecast: Omit<Forecast, 'forecastId'>): Promise<Forecast> => {
    return apiFetch<Forecast>('/forecast', {
      method: 'POST',
      body: JSON.stringify(forecast),
    });
  },

  // Update forecast
  updateForecast: async (id: number, forecast: Partial<Forecast>): Promise<Forecast> => {
    return apiFetch<Forecast>(`/forecast/${id}`, {
      method: 'PUT',
      body: JSON.stringify(forecast),
    });
  },

  // Delete forecast
  deleteForecast: async (id: number): Promise<void> => {
    return apiFetch<void>(`/forecast/${id}`, {
      method: 'DELETE',
    });
  },

  // Get forecasts by medicine
  getForecastsByMedicine: async (medicineId: number): Promise<Forecast[]> => {
    return apiFetch<Forecast[]>(`/api/forecast/medicine/${medicineId}`);
  },
};
