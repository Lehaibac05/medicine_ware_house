package com.pharmacy.warehouse.controller;

import com.pharmacy.warehouse.model.AIModel;
import com.pharmacy.warehouse.model.Forecast;
import com.pharmacy.warehouse.model.Medicine;
import com.pharmacy.warehouse.model.OrderItem;
import com.pharmacy.warehouse.repository.AIModelRepository;
import com.pharmacy.warehouse.repository.ForecastRepository;
import com.pharmacy.warehouse.repository.MedicineRepository;
import com.pharmacy.warehouse.repository.OrderItemRepository;
import com.pharmacy.warehouse.service.AIModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/forecast")
@RequiredArgsConstructor
@Slf4j
public class ForecastController {

    private final ForecastRepository forecastRepository;
    private final AIModelRepository aiModelRepository;
    private final MedicineRepository medicineRepository;
    private final OrderItemRepository orderItemRepository;
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
            log.info("Yêu cầu dự báo: {}", request.get("medicineName"));

            if (!aiModelService.isAIServiceAvailable()) {
                log.warn("AI Service không khả dụng, sử dụng dự báo fallback");
            }

            Map<String, Object> prediction = aiModelService.predictDemand(request);

            // Chuẩn hoá response cho frontend
            double predQty = 0.0;
            if (prediction.get("predictedQuantity") != null) {
                predQty = Double.parseDouble(prediction.get("predictedQuantity").toString());
            } else if (prediction.get("prediction") != null) {
                predQty = Double.parseDouble(prediction.get("prediction").toString());
            }

            double confidence = 0.65;
            if (prediction.get("confidenceLevel") != null) {
                confidence = Double.parseDouble(prediction.get("confidenceLevel").toString());
            } else if (prediction.get("confidence") != null) {
                confidence = Double.parseDouble(prediction.get("confidence").toString());
            }

            Map<String, Object> normalized = new HashMap<>();
            normalized.put("medicineId", request.get("medicineId"));
            normalized.put("medicineName", request.get("medicineName"));
            normalized.put("predictedQuantity", predQty);
            normalized.put("lowerBound", prediction.getOrDefault("lowerBound", prediction.get("lower")));
            normalized.put("upperBound", prediction.getOrDefault("upperBound", prediction.get("upper")));
            normalized.put("confidenceLevel", confidence);
            normalized.put("recommendedOrder", prediction.getOrDefault("recommendedOrder", ""));
            normalized.put("dataSource", request.getOrDefault("source", "api"));
            normalized.put("isFallback", prediction.getOrDefault("isFallback", false));

            log.info("✅ Dự báo hoàn tất: {} đơn vị (confidence {})", predQty, confidence);
            return ResponseEntity.ok(normalized);

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

            Optional<Medicine> medicineOpt = medicineRepository.findById(medicineId);
            if (medicineOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Medicine medicine = medicineOpt.get();

            // Kiểm tra dữ liệu từ DB
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startDate = now.minusDays(30);

            List<OrderItem> historyItems = orderItemRepository
                    .findByMedicine_MedicineIdAndIssuedAtBetween(medicineId, startDate, now);

            boolean hasDbData = !historyItems.isEmpty();
            log.info("   → Dữ liệu từ DB: {}, số dòng sales: {}", hasDbData, historyItems.size());

            double salesLag1 = 45.0;
            double salesLag7 = 48.0;
            double salesLag30 = 45.0;
            int currentInventory = 50;

            if (hasDbData) {
                Map<LocalDate, Integer> dailyTotal = historyItems.stream()
                        .collect(Collectors.groupingBy(
                                item -> item.getIssuedAt().toLocalDate(),
                                Collectors.summingInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                        ));

                LocalDate yesterday = LocalDate.now().minusDays(1);
                salesLag1 = dailyTotal.getOrDefault(yesterday, 0);

                salesLag7 = 0;
                for (int i = 1; i <= 7; i++) {
                    salesLag7 += dailyTotal.getOrDefault(LocalDate.now().minusDays(i), 0);
                }
                salesLag7 = Math.max(0.0, salesLag7 / 7.0);

                salesLag30 = 0;
                for (int i = 1; i <= 30; i++) {
                    salesLag30 += dailyTotal.getOrDefault(LocalDate.now().minusDays(i), 0);
                }
                salesLag30 = Math.max(0.0, salesLag30 / 30.0);

                // Nếu có dữ liệu tồn kho/nhập hàng trong DB, có thể lấy ra để cải thiện AI.
                // Thời điểm này tạm thời dùng giá trị giả định từ dòng gần nhất.
                currentInventory = historyItems.get(historyItems.size() - 1).getQuantity() != null
                        ? historyItems.get(historyItems.size() - 1).getQuantity() : 50;
            }

            Map<String, Object> request = new HashMap<>();
            request.put("medicineId", medicineId);
            request.put("medicineName", medicine.getName());
            request.put("region", "Bắc");
            request.put("temperature", 28.0);
            request.put("fluSeason", hasDbData ? 1 : 0);
            request.put("rain", 0);
            request.put("currentInventory", currentInventory);
            request.put("salesLag1", salesLag1);
            request.put("salesLag7", salesLag7);
            request.put("salesLag30", salesLag30);
            request.put("storageCondition", medicine.getStorageCondition() != null ? medicine.getStorageCondition() : "Room temperature");
            request.put("source", hasDbData ? "database" : "csv");

            List<Map<String, Object>> forecast30Day = aiModelService.predict30Day(request);
            forecast30Day.forEach(item -> item.put("dataSource", hasDbData ? "database" : "csv"));

            log.info("✅ Dự báo 30 ngày hoàn tất: {} ngày, source: {}", forecast30Day.size(), hasDbData ? "DB" : "CSV");
            return ResponseEntity.ok(forecast30Day);

        } catch (Exception e) {
            log.error("❌ Lỗi lấy dự báo 30 ngày: {}", e.getMessage(), e);
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