// import { apiFetch } from './api';

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
}

const mock30DayData = [
  { date: '2026-03-21', predicted: 65, lower: 52, upper: 78, confidence: 0.89 },
  { date: '2026-03-22', predicted: 70, lower: 56, upper: 84, confidence: 0.87 },
  { date: '2026-03-23', predicted: 68, lower: 54, upper: 82, confidence: 0.88 },
  { date: '2026-03-24', predicted: 75, lower: 60, upper: 90, confidence: 0.85 },
  { date: '2026-03-25', predicted: 80, lower: 64, upper: 96, confidence: 0.83 },
  { date: '2026-03-26', predicted: 72, lower: 57, upper: 87, confidence: 0.86 },
  { date: '2026-03-27', predicted: 85, lower: 68, upper: 102, confidence: 0.82 },
  { date: '2026-03-28', predicted: 78, lower: 62, upper: 94, confidence: 0.84 },
  { date: '2026-03-29', predicted: 82, lower: 65, upper: 99, confidence: 0.81 },
  { date: '2026-03-30', predicted: 76, lower: 61, upper: 91, confidence: 0.86 },
];

export const forecastApi = {
  // Get all forecasts
  getAllForecasts: async (): Promise<Forecast[]> => {
    // return apiFetch<Forecast[]>('/forecast');
    return [
      {
        forecastId: 1,
        medicineId: 1,
        predictedQuantity: 65,
        period: '2026-Q1',
        confidenceLevel: 0.89,
        model: { modelId: 1, modelName: 'Random Forest', version: '1.0', accuracy: 0.89 }
      },
      {
        forecastId: 2,
        medicineId: 2,
        predictedQuantity: 48,
        period: '2026-Q1',
        confidenceLevel: 0.91,
        model: { modelId: 1, modelName: 'Random Forest', version: '1.0', accuracy: 0.89 }
      },
      {
        forecastId: 3,
        medicineId: 3,
        predictedQuantity: 55,
        period: '2026-Q1',
        confidenceLevel: 0.87,
        model: { modelId: 1, modelName: 'Random Forest', version: '1.0', accuracy: 0.89 }
      },
    ];
  },

  // Get 30-day forecast data
  get30DayForecast: async (): Promise<any[]> => {
    return mock30DayData;
  },

  // Get forecast by ID
  getForecastById: async (_id: number): Promise<Forecast> => {
    // return apiFetch<Forecast>(`/forecast/${id}`);
    throw new Error('Not implemented');
  },

  // Predict demand for a medicine
  predictDemand: async (request: {
    medicineId: number;
    temperature?: number;
    fluSeason?: boolean;
    rain?: boolean;
    // Add other features as needed
  }): Promise<ForecastPrediction> => {
    // return apiFetch<ForecastPrediction>('/forecast/predict', {
    //   method: 'POST',
    //   body: JSON.stringify(request),
    // });
    return {
      medicineId: request.medicineId,
      predictedQuantity: 45.5,
      confidenceLevel: 0.89,
      period: '2026-Q1',
      lowerBound: 35.0,
      upperBound: 56.0,
    };
  },

  // Create new forecast
  createForecast: async (_forecast: Omit<Forecast, 'forecastId'>): Promise<Forecast> => {
    // return apiFetch<Forecast>('/forecast', {
    //   method: 'POST',
    //   body: JSON.stringify(forecast),
    // });
    throw new Error('Not implemented');
  },

  // Update forecast
  updateForecast: async (_id: number, _forecast: Partial<Forecast>): Promise<Forecast> => {
    // return apiFetch<Forecast>(`/forecast/${id}`, {
    //   method: 'PUT',
    //   body: JSON.stringify(forecast),
    // });
    throw new Error('Not implemented');
  },

  // Delete forecast
  deleteForecast: async (_id: number): Promise<void> => {
    // return apiFetch<void>(`/forecast/${id}`, {
    //   method: 'DELETE',
    // });
    throw new Error('Not implemented');
  },

  // Get forecasts by medicine
  getForecastsByMedicine: async (_medicineId: number): Promise<Forecast[]> => {
    // return apiFetch<Forecast[]>(`/forecast/medicine/${medicineId}`);
    return [];
  },
};