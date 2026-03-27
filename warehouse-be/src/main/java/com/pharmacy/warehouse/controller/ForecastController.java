package com.pharmacy.warehouse.controller;

import com.pharmacy.warehouse.model.AIModel;
import com.pharmacy.warehouse.model.Forecast;
import com.pharmacy.warehouse.model.Medicine;
import com.pharmacy.warehouse.repository.AIModelRepository;
import com.pharmacy.warehouse.repository.ForecastRepository;
import com.pharmacy.warehouse.repository.MedicineRepository;
import com.pharmacy.warehouse.service.AIModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
@RestController
@RequestMapping("/api/forecast")
@RequiredArgsConstructor
@Slf4j
public class ForecastController {

    private final ForecastRepository forecastRepository;
    private final AIModelRepository aiModelRepository;
    private final MedicineRepository medicineRepository;
    private final AIModelService aiModelService;

    @GetMapping
    public ResponseEntity<List<Forecast>> getAllForecasts() {
        List<Forecast> forecasts = forecastRepository.findAll();
        return ResponseEntity.ok(forecasts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Forecast> getForecastById(@PathVariable Long id) {
        Optional<Forecast> forecast = forecastRepository.findById(id);
        return forecast.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/predict")
    public ResponseEntity<Map<String, Object>> predictDemand(@RequestBody Map<String, Object> request) {
        try {
            log.info("📊 Yêu cầu dự báo: {}", request.get("medicineName"));
            
            // Kiểm tra AI service
            if (!aiModelService.isAIServiceAvailable()) {
                log.warn("⚠️ AI Service không khả dụng, sử dụng dự báo fallback");
            }
            
            // Gọi AI Model Service để dự báo thực tế
            Map<String, Object> prediction = aiModelService.predictDemand(request);
            
            log.info("✅ Dự báo hoàn tất: {} đơn vị", prediction.get("predictedQuantity"));
            return ResponseEntity.ok(prediction);
            
        } catch (Exception e) {
            log.error("❌ Lỗi dự báo: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/predict-batch")
    public ResponseEntity<Map<String, Object>> predictBatch(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> predictions = (List<Map<String, Object>>) request.get("predictions");
            
            if (predictions == null || predictions.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Empty predictions list"));
            }
            
            log.info("📊 Yêu cầu batch dự báo cho {} thuốc", predictions.size());
            
            // Gọi AI Model Service batch predict
            Map<String, Object> result = aiModelService.predictBatch(predictions);
            
            log.info("✅ Batch dự báo hoàn tất: {} kết quả", result.get("total"));
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Lỗi batch dự báo: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/train")
    public ResponseEntity<Map<String, Object>> trainModel(@RequestBody Map<String, Object> request) {
        try {
            String dataPath = (String) request.getOrDefault("dataPath", "../data/pharmacy_training_final.csv");
            
            log.info("🔄 Yêu cầu huấn luyện model từ: {}", dataPath);
            
            // Gọi AI Model Service train
            Map<String, Object> result = aiModelService.trainModel(dataPath);
            
            log.info("✅ Huấn luyện hoàn tất");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Lỗi huấn luyện model: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/30-day")
    public ResponseEntity<List<Map<String, Object>>> get30DayForecast(@RequestParam Long medicineId) {
        try {
            log.info("📅 Lấy dự báo 30 ngày cho thuốc ID: {}", medicineId);
            
            // Lấy thông tin thuốc
            Optional<Medicine> medicineOpt = medicineRepository.findById(medicineId);
            if (medicineOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Medicine medicine = medicineOpt.get();
            
            // Tạo request cho AI
            Map<String, Object> request = new HashMap<>();
            request.put("medicineId", medicineId);
            request.put("medicineName", medicine.getName());
            request.put("region", "Bắc"); // Default region
            request.put("temperature", 28.0);
            request.put("fluSeason", 0);
            request.put("rain", 0);
            request.put("currentInventory", 50); // Default
            request.put("salesLag1", 45.0); // Default values
            request.put("salesLag7", 48.0);
            request.put("salesLag30", 45.0);
            request.put("storageCondition", "Room temperature");
            
            // Gọi AI service
            List<Map<String, Object>> forecast30Day = aiModelService.predict30Day(request);
            
            log.info("✅ Dự báo 30 ngày hoàn tất: {} ngày", forecast30Day.size());
            return ResponseEntity.ok(forecast30Day);
            
        } catch (Exception e) {
            log.error("❌ Lỗi lấy dự báo 30 ngày: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<Forecast> createForecast(@RequestBody Map<String, Object> request) {
        try {
            Long medicineId = Long.valueOf(request.get("medicineId").toString());
            Integer predictedQuantity = Integer.valueOf(request.get("predictedQuantity").toString());
            String period = request.get("period").toString();
            Double confidenceLevel = Double.valueOf(request.get("confidenceLevel").toString());

            Optional<Medicine> medicineOpt = medicineRepository.findById(medicineId);
            if (medicineOpt.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // Get or create AI model
            AIModel aiModel = aiModelRepository.findById(1L)
                    .orElseGet(() -> {
                        AIModel newModel = new AIModel();
                        newModel.setModelName("Random Forest Demand Predictor");
                        newModel.setVersion("1.0");
                        newModel.setAccuracy(0.89);
                        return aiModelRepository.save(newModel);
                    });

            Forecast forecast = new Forecast();
            forecast.setMedicine(medicineOpt.get());
            forecast.setPredictedQuantity(predictedQuantity);
            forecast.setPeriod(period);
            forecast.setConfidenceLevel(confidenceLevel);
            forecast.setModel(aiModel);

            Forecast savedForecast = forecastRepository.save(forecast);
            return ResponseEntity.ok(savedForecast);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Forecast> updateForecast(@PathVariable Long id, @RequestBody Forecast forecastDetails) {
        Optional<Forecast> optionalForecast = forecastRepository.findById(id);
        if (optionalForecast.isPresent()) {
            Forecast forecast = optionalForecast.get();
            forecast.setPredictedQuantity(forecastDetails.getPredictedQuantity());
            forecast.setPeriod(forecastDetails.getPeriod());
            forecast.setConfidenceLevel(forecastDetails.getConfidenceLevel());
            Forecast updatedForecast = forecastRepository.save(forecast);
            return ResponseEntity.ok(updatedForecast);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteForecast(@PathVariable Long id) {
        if (forecastRepository.existsById(id)) {
            forecastRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/medicine/{medicineId}")
    public ResponseEntity<List<Forecast>> getForecastsByMedicine(@PathVariable Long medicineId) {
        Optional<Medicine> medicine = medicineRepository.findById(medicineId);
        if (medicine.isPresent()) {
            // Note: Forecast entity doesn't have direct medicine relationship
            // This would need to be implemented based on your data model
            List<Forecast> forecasts = forecastRepository.findAll();
            return ResponseEntity.ok(forecasts);
        }
        return ResponseEntity.notFound().build();
    }
}