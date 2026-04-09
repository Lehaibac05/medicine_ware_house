import { apiFetch } from './api';
import { getMedicines } from './medicines';

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
  confidenceLevel?: number;
  confidence?: number;
  period: string;
  lowerBound: number;
  upperBound: number;
  recommendedOrder?: number;
  warning?: string;
  isFallback?: boolean;
}

export interface ForecastPoint {
  date: string;
  predicted?: number | null;
  lower?: number | null;
  upper?: number | null;
  confidence?: number | null;
  isFallback?: boolean;
  forecast?: number | null; // For forecast data (green line)
  isHistory?: boolean; // To distinguish history vs forecast
  dataSource?: string;
}

interface AnalysisChartItem {
  date: string;
  type?: 'history' | 'forecast' | string;
  quantity?: number;
  predicted?: number;
  forecast?: number;
  lowerBound?: number;
  lower?: number;
  upperBound?: number;
  upper?: number;
  confidence?: number;
  confidenceLevel?: number;
  isFallback?: boolean;
}

const AI_API_BASE_URL = import.meta.env.VITE_AI_API_URL ?? 'http://localhost:5000';

const aiFetch = async <T>(endpoint: string, options: RequestInit = {}): Promise<T> => {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };

  const response = await fetch(`${AI_API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    throw new Error(`AI API Error: ${response.statusText}`);
  }

  const contentType = response.headers.get('content-type');
  if (!contentType || !contentType.includes('application/json')) {
    return null as T;
  }

  return (await response.json()) as T;
};

export const forecastApi = {
  getAllForecasts: async (): Promise<Forecast[]> => {
    try {
      return await apiFetch<Forecast[]>('/api/forecast');
    } catch (error) {
      console.error('Error fetching forecasts:', error);
      return [];
    }
  },

  // Get 30-day forecast data from AI
  get30DayForecast: async (medicineId?: number, region?: string): Promise<ForecastPoint[]> => {
    try {
      let selectedMedicine;

      if (medicineId) {
        // Get specific medicine by ID
        const medicinePage = await getMedicines({
          page: 0,
          size: 100, // Get more to find the specific one
          sortBy: 'name',
          sortDir: 'asc',
        });
        selectedMedicine = medicinePage.content.find(m => m.medicineId === medicineId);
        if (!selectedMedicine) {
          throw new Error(`Medicine with ID ${medicineId} not found`);
        }
      } else {
        // Get first medicine as default
        const medicinePage = await getMedicines({
          page: 0,
          size: 1,
          sortBy: 'name',
          sortDir: 'asc',
        });
        selectedMedicine = medicinePage.content[0];
      }

      if (!selectedMedicine) {
        throw new Error('No medicines available for 30-day forecast');
      }
      const chartResponse = await aiFetch<{
        medicineName: string;
        region: string;
        splitDate: string;
        data: AnalysisChartItem[];
      }>('/api/analysis/chart', {
        method: 'POST',
        body: JSON.stringify({
          medicineName: selectedMedicine.name,
          region: region || 'Bắc',
          dataPath: 'data/pharmacy_training_final_scaled.csv', 
        }),
      });

      const chartData = chartResponse?.data ?? [];
      if (!chartData.length) {
        throw new Error('No chart data returned from AI analysis');
      }

      const parsedData = chartData.map((item) => {
        const isForecast = item.type === 'forecast';
        const rawDate = item.date ?? '';
        const normalizedDate = rawDate.split('T')[0];
        const quantity = Number(item.quantity ?? item.predicted ?? item.forecast ?? 0);
        return {
          date: normalizedDate,
          predicted: isForecast ? null : quantity,
          forecast: isForecast ? quantity : null,
          lower: isForecast ? Number(item.lowerBound ?? item.lower ?? 0) : null,
          upper: isForecast ? Number(item.upperBound ?? item.upper ?? 0) : null,
          confidence: Number(item.confidence ?? item.confidenceLevel ?? 0),
          isFallback: Boolean(item.isFallback),
          isHistory: item.type === 'history',
          dataSource: item.type === 'history' ? 'history' : 'forecast',
        };
      });

      const historyPoints = parsedData.filter((point) => point.isHistory);
      const forecastPoints = parsedData.filter((point) => !point.isHistory);
      const trimmedHistoryPoints = historyPoints.slice(-30);

      return [...trimmedHistoryPoints, ...forecastPoints].sort(
        (a, b) => new Date(a.date).getTime() - new Date(b.date).getTime(),
      );
    } catch (error) {
      console.warn('AI analysis chart unavailable, falling back to backend 30-day forecast', error);
      try {
        const medicinePage = await getMedicines({
          page: 0,
          size: 100,
          sortBy: 'name',
          sortDir: 'asc',
        });
        const fallbackMedicine = medicinePage.content.find(m => m.medicineId === medicineId) || medicinePage.content[0];
        const chartData = await apiFetch<ForecastPoint[]>(`/api/forecast/30-day?medicineId=${fallbackMedicine.medicineId}`);
        return chartData.map(item => ({
          ...item,
          date: item.date?.split('T')[0] ?? item.date,
          dataSource: item.dataSource ?? 'unknown',
          forecast: item.forecast ?? item.predicted,
        }));
      } catch (fallbackError) {
        console.error('Error in get30DayForecast fallback:', fallbackError);
        throw fallbackError;
      }
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
      body: JSON.stringify({
        medicineId: payload.medicineId,
        medicineName: payload.medicineName,
        region: payload.region,
        temperature: payload.temperature,
        fluSeason: payload.fluSeason,
        rain: payload.rain,
        isHoliday: 0, // Default value
        isWeekend: 0, // Default value
        salesLag1: payload.salesLag1,
        salesLag7: payload.salesLag7,
        salesLag30: payload.salesLag30,
        storageCondition: payload.storageCondition,
        currentInventory: payload.currentInventory,
      }),
    });
  },

  // Create new forecast
  createForecast: async (forecast: Omit<Forecast, 'forecastId'>): Promise<Forecast> => {
    return apiFetch<Forecast>('/api/forecast', {
      method: 'POST',
      body: JSON.stringify(forecast),
    });
  },

  // Update forecast
  updateForecast: async (id: number, forecast: Partial<Forecast>): Promise<Forecast> => {
    return apiFetch<Forecast>(`/api/forecast/${id}`, {
      method: 'PUT',
      body: JSON.stringify(forecast),
    });
  },

  // Delete forecast
  deleteForecast: async (id: number): Promise<void> => {
    return apiFetch<void>(`/api/forecast/${id}`, {
      method: 'DELETE',
    });
  },

  // Get forecasts by medicine
  getForecastsByMedicine: async (medicineId: number): Promise<Forecast[]> => {
    return apiFetch<Forecast[]>(`/api/forecast/medicine/${medicineId}`);
  },

  // Get forecast statistics
  getForecastStats: async (): Promise<{
    totalForecasts: number;
    highRiskCount: number;
    avgConfidence: number;
    needRestockCount: number;
  }> => {
    return apiFetch<{
      totalForecasts: number;
      highRiskCount: number;
      avgConfidence: number;
      needRestockCount: number;
    }>('/api/forecast/stats');
  },
};
