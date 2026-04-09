package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.dto.UserActivityLogResponse;
import com.pharmacy.warehouse.model.User;
import com.pharmacy.warehouse.model.UserActivityLog;
import com.pharmacy.warehouse.repository.UserActivityLogRepository;
import com.pharmacy.warehouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityLogService {

    private final UserActivityLogRepository userActivityLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void log(String username, String action, String targetObject, String ipAddress) {
        try {
            User user = null;
            if (StringUtils.hasText(username) && !"system".equalsIgnoreCase(username)) {
                user = userRepository.findByUsername(username).orElse(null);
            }

            UserActivityLog item = new UserActivityLog();
            item.setAction(action);
            item.setTargetObject(targetObject);
            item.setTimestamp(LocalDateTime.now());
            item.setIpAddress(normalizeIp(ipAddress));
            item.setUser(user);

            userActivityLogRepository.save(item);
        } catch (Exception ex) {
            log.warn("Failed to write user activity log. action={}, target={}, error={}", action, targetObject, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<UserActivityLogResponse> getLogs(int page, int size, String action) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = size <= 0 ? 20 : Math.min(size, 100);

        Pageable pageable = PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<UserActivityLog> logs = StringUtils.hasText(action)
                ? userActivityLogRepository.findByActionContainingIgnoreCase(action.trim(), pageable)
                : userActivityLogRepository.findAll(pageable);

        return logs.map(this::toResponse);
    }

    private UserActivityLogResponse toResponse(UserActivityLog item) {
        User user = item.getUser();
        return UserActivityLogResponse.builder()
                .logId(item.getLogId())
                .action(item.getAction())
                .targetObject(item.getTargetObject())
                .timestamp(item.getTimestamp())
                .ipAddress(item.getIpAddress())
                .userId(user != null ? user.getUserId() : null)
                .username(user != null ? user.getUsername() : null)
                .fullName(user != null ? user.getFullName() : null)
                .build();
    }

    private String normalizeIp(String ipAddress) {
        if (!StringUtils.hasText(ipAddress)) {
            return "unknown";
        }
        return ipAddress.trim();
    }
}
