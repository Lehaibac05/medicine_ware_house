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
  forecast?: number; // For forecast data (green line)
  isHistory?: boolean; // To distinguish history vs forecast
}

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
    try {
      return await apiFetch<Forecast[]>('/api/inventory/forecast');
    } catch (error) {
      console.error('Error fetching forecasts:', error);
      return [];
    }
  },

  // Get 30-day forecast data from AI
  get30DayForecast: async (): Promise<ForecastPoint[]> => {
    try {
      // Get first medicine as default for 30-day forecast
      let medicines: { medicineId: number; name: string }[] = [];
      try {
        medicines = await apiFetch<{ medicineId: number; name: string }[]>('/api/medicines');
      } catch (error) {
        console.warn('Medicines API not available, using fallback data', error);
        medicines = [{ medicineId: 1, name: 'Acetylcysteine syrup' }];
      }

      if (medicines && medicines.length > 0) {
        const defaultMedicine = medicines[0];
        try {
          // Call the correct backend endpoint for chart data
          const chartData = await apiFetch<{
            data: Array<{
              date: string;
              quantity: number;
              type: string;
              lowerBound?: number;
              upperBound?: number;
            }>;
            medicineName: string;
            region: string;
            splitDate: string;
          }>(`/api/analysis/chart`, {
            method: 'POST',
            body: JSON.stringify({
              medicineName: defaultMedicine.name,
              region: 'Bắc',
              dataPath: 'data/pharmacy_training_final.csv'
            }),
          });

          // Transform chart data to ForecastPoint format
          console.log('Raw chart data:', chartData);
          const transformedData = chartData.data.map(item => ({
            date: item.date,
            predicted: item.type === 'history' ? item.quantity : 0, // History data in blue line
            forecast: item.type === 'forecast' ? item.quantity : 0, // Forecast data in green line
            lower: item.lowerBound || item.quantity * 0.8,
            upper: item.upperBound || item.quantity * 1.2,
            confidence: 0.85, // Default confidence
            isFallback: false,
            isHistory: item.type === 'history',
          }));
          console.log('Transformed data:', transformedData);
          console.log('History points:', transformedData.filter(d => d.isHistory).length);
          console.log('Forecast points:', transformedData.filter(d => !d.isHistory).length);
          return transformedData;
        } catch (error) {
          console.error('Chart API failed:', error);
          // Return mock data for demo purposes
          console.warn('Using mock data for 30-day forecast');
          return getMock30DayData();
        }
      }
      // Return empty array if no medicines available
      return [];
    } catch (error) {
      console.error('Error in get30DayForecast:', error);
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

    return apiFetch<ForecastPrediction>('/api/predict', {
      method: 'POST',
      body: JSON.stringify({
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
