package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.dto.AlertStatsResponse;
import com.pharmacy.warehouse.dto.ChatAskResponse;
import com.pharmacy.warehouse.dto.CreateMedicineRequestRequest;
import com.pharmacy.warehouse.dto.ExpiringBatchResponse;
import com.pharmacy.warehouse.dto.InventoryResponse;
import com.pharmacy.warehouse.dto.MedicineRequestResponse;
import com.pharmacy.warehouse.model.User;
import com.pharmacy.warehouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final AlertService alertService;
    private final InventoryService inventoryService;
    private final SettingsService settingsService;
    private final MedicineRequestService medicineRequestService;
    private final UserRepository userRepository;
    private final GeminiAssistantService geminiAssistantService;

    private final Map<String, RestockDraft> restockDraftByUser = new ConcurrentHashMap<>();

    public ChatAskResponse ask(String message, Set<String> roles, String username) {
        String normalized = normalize(message);

        if (normalized.isBlank()) {
            return buildResponse(
                    "Bạn có thể hỏi mình về tồn kho thấp, thuốc sắp hết hạn, hoặc tổng quan cảnh báo.",
                    defaultSuggestions());
        }

        if (containsAny(normalized, "xin chao", "chao", "hello", "hi")) {
            return buildResponse(
                    "Xin chào. Mình có thể hỗ trợ tra cứu nhanh dữ liệu kho và cảnh báo theo thời gian thực.",
                    defaultSuggestions());
        }

        if (containsAny(normalized, "ton kho thap", "low stock", "thieu hang")) {
            return answerLowStock();
        }

        if (containsAny(normalized, "xac nhan de xuat nhap hang", "xac nhan nhap hang", "tao yeu cau nhap")) {
            return confirmRestockDraft(username, roles);
        }

        if (containsAny(normalized, "de xuat nhap hang", "goi y nhap hang", "de xuat mua")) {
            return buildRestockDraft(username);
        }

        if (containsAny(normalized, "scan canh bao", "quet canh bao", "xem xet canh bao", "xet canh bao")) {
            return executeAlertScan(roles);
        }

        if (containsAny(normalized, "sap het han", "het han", "expiry", "expired")) {
            return answerExpiry();
        }

        if (containsAny(normalized, "canh bao", "alert", "tong quan")) {
            return answerAlertsSummary();
        }

        Optional<String> llmAnswer = geminiAssistantService.ask(
            username,
            roles,
            message,
            buildLiveContext());
        if (llmAnswer.isPresent()) {
            return buildResponse(llmAnswer.get(), defaultSuggestions());
        }

        return buildResponse(
                "Mình chưa hiểu rõ câu hỏi. Bạn thử hỏi theo các mẫu gợi ý bên dưới nhé.",
                defaultSuggestions());
    }

        private String buildLiveContext() {
        try {
            AlertStatsResponse stats = alertService.getAlertStats();
            List<InventoryResponse> lowStockTop = inventoryService.getLowStockInventory().stream()
                .sorted((a, b) -> Long.compare(safeLong(a.getTotalStock()), safeLong(b.getTotalStock())))
                .limit(5)
                .toList();
            List<ExpiringBatchResponse> expiringTop = inventoryService.getExpiringBatches().stream()
                .limit(5)
                .toList();

            String lowStockText = lowStockTop.isEmpty()
                ? "Không có mặt hàng tồn kho thấp"
                : lowStockTop.stream()
                .map(item -> safe(item.getMedicineName()) + " @ " + safe(item.getWarehouseName()) + " = " + safeLong(item.getTotalStock()))
                .collect(Collectors.joining("; "));

            String expiringText = expiringTop.isEmpty()
                ? "Không có lô sắp hết hạn"
                : expiringTop.stream()
                .map(item -> safe(item.getMedicineName()) + " (lô " + safe(item.getLotNumber()) + ") - " + item.getExpiryDate())
                .collect(Collectors.joining("; "));

            return "Tổng cảnh báo mở=" + safeLong(stats.getTotalActiveAlerts())
                + ", Tồn kho thấp=" + safeLong(stats.getLowStockCount())
                + ", Sắp hết hạn=" + safeLong(stats.getExpiringSoonCount())
                + ", Đã hết hạn=" + safeLong(stats.getExpiredCount())
                + ", Cảnh báo hệ thống=" + safeLong(stats.getSystemWarningsCount())
                + "\nTop tồn kho thấp: " + lowStockText
                + "\nTop lô sắp hết hạn: " + expiringText;
        } catch (Exception ex) {
            return "Không lấy được context thời gian thực";
        }
        }

    private ChatAskResponse answerLowStock() {
        List<InventoryResponse> lowStock = inventoryService.getLowStockInventory();
        if (lowStock.isEmpty()) {
            return buildResponse(
                    "Hiện không có mặt hàng nào ở trạng thái tồn kho thấp.",
                    defaultSuggestions());
        }

        List<InventoryResponse> top = lowStock.stream()
                .sorted((a, b) -> Long.compare(safeLong(a.getTotalStock()), safeLong(b.getTotalStock())))
                .limit(5)
                .collect(Collectors.toList());

        String detail = top.stream()
                .map(item -> "- " + safe(item.getMedicineName()) + " (" + safe(item.getWarehouseName()) + "): " + safeLong(item.getTotalStock()) + " đơn vị")
                .collect(Collectors.joining("\n"));

        String answer = "Hiện có " + lowStock.size() + " dòng tồn kho thấp. Top ưu tiên xử lý:\n" + detail;
        return buildResponse(answer, defaultSuggestions());
    }

    private ChatAskResponse answerExpiry() {
        List<ExpiringBatchResponse> expiring = inventoryService.getExpiringBatches();
        AlertStatsResponse stats = alertService.getAlertStats();

        if (expiring.isEmpty() && safeLong(stats.getExpiredCount()) == 0) {
            return buildResponse(
                    "Hiện chưa có lô nào sắp hết hạn hoặc đã hết hạn theo ngưỡng cấu hình.",
                    defaultSuggestions());
        }

        List<ExpiringBatchResponse> top = expiring.stream().limit(5).collect(Collectors.toList());
        String detail = top.stream()
                .map(item -> "- " + safe(item.getMedicineName()) + " | Lô " + safe(item.getLotNumber()) + " | Hết hạn: " + item.getExpiryDate())
                .collect(Collectors.joining("\n"));

        StringBuilder answer = new StringBuilder();
        answer.append("Có ").append(expiring.size()).append(" lô sắp hết hạn.")
                .append("\nCảnh báo đã hết hạn đang mở: ").append(safeLong(stats.getExpiredCount()));

        if (!detail.isBlank()) {
            answer.append("\nTop lô sắp hết hạn:\n").append(detail);
        }

        return buildResponse(answer.toString(), defaultSuggestions());
    }

    private ChatAskResponse answerAlertsSummary() {
        AlertStatsResponse stats = alertService.getAlertStats();
        String answer = "Tổng cảnh báo đang mở: " + safeLong(stats.getTotalActiveAlerts())
                + "\n- Tồn kho thấp: " + safeLong(stats.getLowStockCount())
                + "\n- Sắp hết hạn: " + safeLong(stats.getExpiringSoonCount())
                + "\n- Đã hết hạn: " + safeLong(stats.getExpiredCount())
                + "\n- Hệ thống: " + safeLong(stats.getSystemWarningsCount());
        return buildResponse(answer, defaultSuggestions());
    }

    private ChatAskResponse executeAlertScan(Set<String> roles) {
        if (!hasAnyRole(roles, "ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER")) {
            return buildResponse(
                    "Bạn không có quyền quét cảnh báo từ chatbot. Chỉ ADMIN hoặc Quản lý kho được phép.",
                    defaultSuggestions());
        }

        int before = alertService.getActiveAlerts().size();
        alertService.checkAndGenerateAlerts();
        int after = alertService.getActiveAlerts().size();
        int generated = Math.max(after - before, 0);

        String answer = "Đã quét cảnh báo thành công."
                + "\n- Cảnh báo đang mở trước khi quét: " + before
                + "\n- Cảnh báo đang mở sau khi quét: " + after
                + "\n- Cảnh báo mới ước tính: " + generated;
        return buildResponse(answer, defaultSuggestions());
    }

    private ChatAskResponse buildResponse(String answer, List<String> suggestions) {
        return ChatAskResponse.builder()
                .answer(answer)
                .suggestions(suggestions)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private List<String> defaultSuggestions() {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("Tổng quan cảnh báo hôm nay");
        suggestions.add("Có bao nhiêu thuốc tồn kho thấp?");
        suggestions.add("Danh sách lô sắp hết hạn");
        suggestions.add("Scan cảnh báo");
        suggestions.add("Đề xuất nhập hàng");
        return suggestions;
    }

    private ChatAskResponse buildRestockDraft(String username) {
        List<InventoryResponse> lowStock = inventoryService.getLowStockInventory();
        if (lowStock.isEmpty()) {
            return buildResponse("Hiện không có mặt hàng tồn kho thấp để đề xuất nhập hàng.", defaultSuggestions());
        }

        Map<Long, List<InventoryResponse>> groupedByWarehouse = lowStock.stream()
                .filter(item -> item.getWarehouseId() != null)
                .collect(Collectors.groupingBy(
                        InventoryResponse::getWarehouseId,
                        LinkedHashMap::new,
                        Collectors.toList()));

        if (groupedByWarehouse.isEmpty()) {
            return buildResponse("Không xác định được kho để tạo đề xuất nhập hàng.", defaultSuggestions());
        }

        List<InventoryResponse> selectedWarehouseItems = groupedByWarehouse.values().stream()
                .max(Comparator.comparingInt(List::size))
                .orElse(List.of());

        if (selectedWarehouseItems.isEmpty()) {
            return buildResponse("Không đủ dữ liệu để tạo đề xuất nhập hàng.", defaultSuggestions());
        }

        InventoryResponse first = selectedWarehouseItems.get(0);
        int defaultReorderLevel = settingsService.getDefaultReorderLevel();
        int targetStock = Math.max(defaultReorderLevel * 2, defaultReorderLevel + 5);

        List<CreateMedicineRequestRequest.ItemRequest> items = selectedWarehouseItems.stream()
                .filter(item -> item.getMedicineId() != null)
                .limit(8)
                .map(item -> {
                    long stock = safeLong(item.getTotalStock());
                    int suggestQty = (int) Math.max(targetStock - stock, 1);
                    return new CreateMedicineRequestRequest.ItemRequest(
                            item.getMedicineId(),
                            suggestQty,
                            "Đề xuất tự động từ chatbot");
                })
                .toList();

        if (items.isEmpty()) {
            return buildResponse("Không tạo được danh sách mặt hàng cho đề xuất nhập.", defaultSuggestions());
        }

        RestockDraft draft = new RestockDraft(
                first.getWarehouseId(),
                safe(first.getWarehouseName()),
                items,
                LocalDateTime.now());
        restockDraftByUser.put(username, draft);

        String detail = selectedWarehouseItems.stream()
                .limit(items.size())
                .map(item -> {
                    long stock = safeLong(item.getTotalStock());
                    int suggestQty = (int) Math.max(targetStock - stock, 1);
                    return "- " + safe(item.getMedicineName()) + ": tồn " + stock + ", đề xuất nhập " + suggestQty;
                })
                .collect(Collectors.joining("\n"));

        String answer = "Đã tạo nháp đề xuất nhập hàng cho kho: " + draft.warehouseName + "."
                + "\nDanh sách đề xuất:\n" + detail
                + "\n\nNếu muốn tạo yêu cầu thật, hãy nhập: Xác nhận đề xuất nhập hàng";

        return buildResponse(answer, defaultSuggestions());
    }

    private ChatAskResponse confirmRestockDraft(String username, Set<String> roles) {
        if (!hasAnyRole(roles, "ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER")) {
            return buildResponse(
                    "Bạn không có quyền tạo yêu cầu nhập hàng từ chatbot. Chỉ ADMIN hoặc Quản lý kho được phép.",
                    defaultSuggestions());
        }

        RestockDraft draft = restockDraftByUser.get(username);
        if (draft == null) {
            return buildResponse(
                    "Bạn chưa có nháp đề xuất nhập hàng. Hãy nhập: Đề xuất nhập hàng",
                    defaultSuggestions());
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return buildResponse("Không xác định được tài khoản hiện tại để tạo yêu cầu nhập.", defaultSuggestions());
        }

        CreateMedicineRequestRequest request = new CreateMedicineRequestRequest();
        request.setWarehouseId(draft.warehouseId);
        request.setRequiredDate(LocalDate.now().plusDays(3));
        request.setNotes("Tạo từ chatbot - đề xuất nhập hàng tự động");
        request.setItems(draft.items);

        MedicineRequestResponse created = medicineRequestService.createRequest(request, user.getUserId());
        restockDraftByUser.remove(username);

        String answer = "Đã tạo yêu cầu nhập hàng thành công."
                + "\n- Mã yêu cầu: " + created.getRequestId()
                + "\n- Kho: " + draft.warehouseName
                + "\n- Số mặt hàng: " + draft.items.size();
        return buildResponse(answer, defaultSuggestions());
    }

    private String normalize(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }
        String lower = input.trim().toLowerCase(Locale.ROOT);
        return lower
                .replace("à", "a").replace("á", "a").replace("ạ", "a").replace("ả", "a").replace("ã", "a")
                .replace("â", "a").replace("ầ", "a").replace("ấ", "a").replace("ậ", "a").replace("ẩ", "a").replace("ẫ", "a")
                .replace("ă", "a").replace("ằ", "a").replace("ắ", "a").replace("ặ", "a").replace("ẳ", "a").replace("ẵ", "a")
                .replace("è", "e").replace("é", "e").replace("ẹ", "e").replace("ẻ", "e").replace("ẽ", "e")
                .replace("ê", "e").replace("ề", "e").replace("ế", "e").replace("ệ", "e").replace("ể", "e").replace("ễ", "e")
                .replace("ì", "i").replace("í", "i").replace("ị", "i").replace("ỉ", "i").replace("ĩ", "i")
                .replace("ò", "o").replace("ó", "o").replace("ọ", "o").replace("ỏ", "o").replace("õ", "o")
                .replace("ô", "o").replace("ồ", "o").replace("ố", "o").replace("ộ", "o").replace("ổ", "o").replace("ỗ", "o")
                .replace("ơ", "o").replace("ờ", "o").replace("ớ", "o").replace("ợ", "o").replace("ở", "o").replace("ỡ", "o")
                .replace("ù", "u").replace("ú", "u").replace("ụ", "u").replace("ủ", "u").replace("ũ", "u")
                .replace("ư", "u").replace("ừ", "u").replace("ứ", "u").replace("ự", "u").replace("ử", "u").replace("ữ", "u")
                .replace("ỳ", "y").replace("ý", "y").replace("ỵ", "y").replace("ỷ", "y").replace("ỹ", "y")
                .replace("đ", "d");
    }

    private boolean containsAny(String message, String... keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyRole(Set<String> roles, String... acceptedRoles) {
        for (String role : acceptedRoles) {
            if (roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private record RestockDraft(
            Long warehouseId,
            String warehouseName,
            List<CreateMedicineRequestRequest.ItemRequest> items,
            LocalDateTime createdAt) {
    }
}
