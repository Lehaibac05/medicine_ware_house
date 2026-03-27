package com.pharmacy.warehouse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service để gọi Python AI Model API
 * Kết nối với Flask server chạy mô hình dự báo nhu cầu
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AIModelService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.service.url:http://localhost:5000}")
    private String aiServiceUrl;

    @Value("${ai.service.timeout:30000}")
    private long timeout;

    /**
     * Kiểm tra sức khỏe của AI service
     */
    public boolean isAIServiceAvailable() {
        try {
            String url = aiServiceUrl + "/api/health";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            boolean isHealthy = response != null && "healthy".equals(response.get("status"));
            
            if (isHealthy) {
                log.info("✅ AI Service available at {}", aiServiceUrl);
            } else {
                log.warn("⚠️ AI Service at {} is not healthy", aiServiceUrl);
            }
            return isHealthy;
        } catch (Exception e) {
            log.error("❌ AI Service not available at {}: {}", aiServiceUrl, e.getMessage());
            return false;
        }
    }

    /**
     * Dự báo nhu cầu cho một thuốc
     *
     * @param request Chứa medicineId, medicineName, region, và các features khác
     * @return Kết quả dự báo (predictedQuantity, confidence, bounds, recommendedOrder)
     */
    public Map<String, Object> predictDemand(Map<String, Object> request) {
        try {
            String url = aiServiceUrl + "/api/predict";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            log.info("📊 Gọi AI Service predict cho thuốc: {}",
                    request.getOrDefault("medicineName", "Unknown"));
            
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            
            if (response != null) {
                log.info("✅ Dự báo thành công: {} đơn vị (confidence: {})",
                        response.get("predictedQuantity"),
                        response.get("confidenceLevel"));
            }
            
            return response != null ? response : getDefaultPrediction(request);
            
        } catch (Exception e) {
            log.error("❌ Lỗi gọi AI Service predict: {}", e.getMessage(), e);
            return getDefaultPrediction(request);
        }
    }

    /**
     * Dự báo hàng loạt cho nhiều thuốc
     *
     * @param predictions Danh sách các request dự báo
     * @return Danh sách kết quả dự báo
     */
    public Map<String, Object> predictBatch(List<Map<String, Object>> predictions) {
        try {
            String url = aiServiceUrl + "/api/predict/batch";
            
            Map<String, Object> request = new HashMap<>();
            request.put("predictions", predictions);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            log.info("📊 Gọi AI Service batch predict cho {} thuốc", predictions.size());
            
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            
            if (response != null) {
                log.info("✅ Batch dự báo thành công: {} kết quả",
                        response.get("total"));
            }
            
            return response != null ? response : new HashMap<>();
            
        } catch (Exception e) {
            log.error("❌ Lỗi gọi AI Service batch predict: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * Huấn luyện lại mô hình từ dữ liệu mới
     *
     * @param dataPath Đường dẫn tới file CSV dữ liệu huấn luyện
     * @return Kết quả huấn luyện (success/error)
     */
    public Map<String, Object> trainModel(String dataPath) {
        try {
            String url = aiServiceUrl + "/api/train";
            
            Map<String, Object> request = new HashMap<>();
            request.put("dataPath", dataPath);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            log.info("🔄 Gọi AI Service train model từ {}", dataPath);
            
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            
            if (response != null && "success".equals(response.get("status"))) {
                log.info("✅ Model training thành công. Rows: {}, Medicines: {}",
                        response.get("dataRows"),
                        response.get("uniqueMedicines"));
            }
            
            return response != null ? response : new HashMap<>();
            
        } catch (Exception e) {
            log.error("❌ Lỗi gọi AI Service train: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * Dự báo 30 ngày cho một thuốc
     *
     * @param request Thông tin thuốc và điều kiện
     * @return Danh sách dự báo 30 ngày
     */
    public List<Map<String, Object>> predict30Day(Map<String, Object> request) {
        List<Map<String, Object>> forecast30Day = new ArrayList<>();
        
        try {
            log.info("📅 Dự báo 30 ngày cho thuốc: {}", request.get("medicineName"));
            
            // Lấy ngày hiện tại
            LocalDate today = LocalDate.now();
            
            for (int i = 0; i < 30; i++) {
                LocalDate forecastDate = today.plusDays(i);
                
                // Tạo request cho ngày cụ thể
                Map<String, Object> dayRequest = new HashMap<>(request);
                dayRequest.put("forecastDate", forecastDate.toString());
                
                // Thêm các yếu tố theo ngày
                dayRequest.put("isWeekend", forecastDate.getDayOfWeek().getValue() >= 6);
                dayRequest.put("isHoliday", false); // Có thể mở rộng để check holidays
                
                // Gọi predict cho ngày này
                Map<String, Object> prediction = predictDemand(dayRequest);
                
                // Tạo entry cho ngày
                Map<String, Object> dayEntry = new HashMap<>();
                dayEntry.put("date", forecastDate.toString());
                dayEntry.put("predicted", Math.round(getDoubleValue(prediction, "predictedQuantity", 0.0)));
                dayEntry.put("lower", Math.round(getDoubleValue(prediction, "lowerBound", 0.0)));
                dayEntry.put("upper", Math.round(getDoubleValue(prediction, "upperBound", 0.0)));
                dayEntry.put("confidence", prediction.get("confidenceLevel"));
                dayEntry.put("isFallback", Boolean.TRUE.equals(prediction.get("isFallback")));
                
                forecast30Day.add(dayEntry);
            }
            
            log.info("✅ Dự báo 30 ngày hoàn tất cho {}", request.get("medicineName"));
            
        } catch (Exception e) {
            log.error("❌ Lỗi dự báo 30 ngày: {}", e.getMessage(), e);
            
            // Fallback: tạo dữ liệu giả
            LocalDate today = LocalDate.now();
            double basePrediction = getDoubleValue(request, "salesLag1", 50.0);
            
            for (int i = 0; i < 30; i++) {
                LocalDate forecastDate = today.plusDays(i);
                double variation = (Math.random() - 0.5) * 20; // ±10 variation
                double predicted = Math.max(0, basePrediction + variation);
                
                Map<String, Object> dayEntry = new HashMap<>();
                dayEntry.put("date", forecastDate.toString());
                dayEntry.put("predicted", Math.round(predicted));
                dayEntry.put("lower", Math.round(predicted * 0.8));
                dayEntry.put("upper", Math.round(predicted * 1.2));
                dayEntry.put("confidence", 0.7);
                dayEntry.put("isFallback", true);
                
                forecast30Day.add(dayEntry);
            }
        }
        
        return forecast30Day;
    }

    /**
     * Trả về dự báo mặc định khi AI service không khả dụng
     */
    private Map<String, Object> getDefaultPrediction(Map<String, Object> request) {
        // Tính toán dự báo đơn giản dựa trên dữ liệu đã có
        double salesLag1 = getDoubleValue(request, "salesLag1", 45.0);
        double salesLag7 = getDoubleValue(request, "salesLag7", 45.0);
        double salesLag30 = getDoubleValue(request, "salesLag30", 45.0);
        
        // Sử dụng trung bình di động
        double predictedQty = (salesLag1 * 0.5 + salesLag7 * 0.3 + salesLag30 * 0.2);
        
        Map<String, Object> response = new HashMap<>();
        response.put("medicineId", request.get("medicineId"));
        response.put("medicineName", request.get("medicineName"));
        response.put("predictedQuantity", Math.round(predictedQty * 100.0) / 100.0);
        response.put("confidenceLevel", 0.65); // Confidence thấp hơn vì là fallback
        response.put("period", "Daily");
        response.put("lowerBound", Math.round(predictedQty * 0.7 * 100.0) / 100.0);
        response.put("upperBound", Math.round(predictedQty * 1.3 * 100.0) / 100.0);
        response.put("recommendedOrder", Math.round(predictedQty * 1.5));
        response.put("warning", "⚠️ AI Service không khả dụng, sử dụng dự báo fallback");
        response.put("isFallback", true);
        
        log.warn("⚠️ Sử dụng dự báo fallback cho thuốc: {}", request.get("medicineName"));
        
        return response;
    }

    /**
     * Helper để lấy double value từ map
     */
    private double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        try {
            Object value = map.get(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof String) {
                return Double.parseDouble((String) value);
            }
            return defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
