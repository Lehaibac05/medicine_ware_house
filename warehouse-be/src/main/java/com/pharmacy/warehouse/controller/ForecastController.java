package com.pharmacy.warehouse.controller;

import com.pharmacy.warehouse.model.AIModel;
import com.pharmacy.warehouse.model.Forecast;
import com.pharmacy.warehouse.model.Medicine;
import com.pharmacy.warehouse.repository.AIModelRepository;
import com.pharmacy.warehouse.repository.ForecastRepository;
import com.pharmacy.warehouse.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/forecast")
@RequiredArgsConstructor
public class ForecastController {

    private final ForecastRepository forecastRepository;
    private final AIModelRepository aiModelRepository;
    private final MedicineRepository medicineRepository;

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
            // This would call the Python AI service
            // For now, return mock prediction
            Map<String, Object> prediction = Map.of(
                "medicineId", request.get("medicineId"),
                "predictedQuantity", 45.5,
                "confidenceLevel", 0.89,
                "period", "2026-Q1",
                "lowerBound", 35.0,
                "upperBound", 56.0
            );

            return ResponseEntity.ok(prediction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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