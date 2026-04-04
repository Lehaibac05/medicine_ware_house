package com.pharmacy.warehouse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.*;

/**
 * AI Model Service - FINAL VERSION (FIXED + OPTIMIZED)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AIModelService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.service.url:http://localhost:5000}")
    private String aiServiceUrl;

    // =========================
    // HEALTH CHECK
    // =========================
    public boolean isAIServiceAvailable() {
        try {
            String url = aiServiceUrl + "/api/health";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return response != null && "healthy".equals(response.get("status"));
        } catch (Exception e) {
            log.warn("AI Service unavailable: {}", e.getMessage());
            return false;
        }
    }

    // =========================
    // PREDICT SINGLE
    // =========================
    public Map<String, Object> predictDemand(Map<String, Object> request) {
        try {
            String url = aiServiceUrl + "/api/predict";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            if (response == null)
                return getDefaultPrediction(request);

            // 🔥 FIX KEY MAPPING
            double predicted = getDouble(response, "prediction", "predictedQuantity");
            double lower = getDouble(response, "lower_bound", "lowerBound");
            double upper = getDouble(response, "upper_bound", "upperBound");

            // 🔥 FALLBACK nếu model trả 0
            if (predicted <= 0) {
                predicted = fallbackPredict(request);
                lower = predicted * 0.7;
                upper = predicted * 1.3;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("predictedQuantity", predicted);
            result.put("lowerBound", lower);
            result.put("upperBound", upper);
            result.put("confidenceLevel", 0.8);
            result.put("isFallback", false);

            return result;

        } catch (Exception e) {
            log.error("Predict error: {}", e.getMessage());
            return getDefaultPrediction(request);
        }
    }

    // =========================
    // PREDICT 30 DAYS
    // =========================
    public List<Map<String, Object>> predict30Day(Map<String, Object> request) {
        List<Map<String, Object>> result = new ArrayList<>();

        LocalDate today = LocalDate.now();

        for (int i = 0; i < 30; i++) {
            LocalDate date = today.plusDays(i);

            Map<String, Object> dayRequest = new HashMap<>(request);
            dayRequest.put("forecastDate", date.toString());
            dayRequest.put("isWeekend", date.getDayOfWeek().getValue() >= 6);
            dayRequest.put("isHoliday", false);

            Map<String, Object> prediction = predictDemand(dayRequest);

            double predicted = getDouble(prediction, "predictedQuantity");
            double lower = getDouble(prediction, "lowerBound");
            double upper = getDouble(prediction, "upperBound");

            Map<String, Object> day = new HashMap<>();
            day.put("date", date.toString());
            day.put("predicted", Math.round(predicted));
            day.put("lower", Math.round(lower));
            day.put("upper", Math.round(upper));
            day.put("confidence", prediction.get("confidenceLevel"));
            day.put("isFallback", prediction.get("isFallback"));

            result.add(day);
        }

        return result;
    }

    // =========================
    // PREDICT BATCH
    // =========================
    public Map<String, Object> predictBatch(List<Map<String, Object>> requests) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (Map<String, Object> req : requests) {
            Map<String, Object> prediction = predictDemand(req);

            Map<String, Object> item = new HashMap<>();
            item.put("medicineId", req.get("medicineId"));
            item.put("medicineName", req.get("medicineName"));
            item.put("predictedQuantity", prediction.get("predictedQuantity"));
            item.put("lowerBound", prediction.get("lowerBound"));
            item.put("upperBound", prediction.get("upperBound"));
            item.put("confidenceLevel", prediction.get("confidenceLevel"));
            item.put("isFallback", prediction.get("isFallback"));

            results.add(item);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("total", results.size());
        response.put("results", results);

        return response;
    }

    // =========================
    // TRAIN MODEL
    // =========================
    public Map<String, Object> trainModel(String dataPath) {
        try {
            String url = aiServiceUrl + "/api/train";

            Map<String, Object> req = new HashMap<>();
            req.put("dataPath", dataPath);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(req, headers);

            return restTemplate.postForObject(url, entity, Map.class);

        } catch (Exception e) {
            log.error("Train error: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    // =========================
    // HELPER METHODS
    // =========================

    private double getDouble(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            if (value instanceof String) {
                try {
                    return Double.parseDouble((String) value);
                } catch (Exception ignored) {
                }
            }
        }
        return 0.0;
    }

    private double fallbackPredict(Map<String, Object> request) {
        double l1 = getDouble(request, "salesLag1");
        double l7 = getDouble(request, "salesLag7");
        double l30 = getDouble(request, "salesLag30");

        return l1 * 0.5 + l7 * 0.3 + l30 * 0.2;
    }

    private Map<String, Object> getDefaultPrediction(Map<String, Object> request) {
        double pred = fallbackPredict(request);

        return Map.of(
                "predictedQuantity", pred,
                "lowerBound", pred * 0.7,
                "upperBound", pred * 1.3,
                "confidenceLevel", 0.6,
                "isFallback", true);
    }
}