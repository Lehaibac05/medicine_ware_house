package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.dto.ChangePasswordRequest;
import com.pharmacy.warehouse.dto.BulkUserImportResponse;
import com.pharmacy.warehouse.dto.BulkUserImportRowResult;
import com.pharmacy.warehouse.dto.CreateUserRequest;
import com.pharmacy.warehouse.dto.ForceChangePasswordRequest;
import com.pharmacy.warehouse.dto.UpdateUserRequest;
import com.pharmacy.warehouse.dto.UserResponse;
import com.pharmacy.warehouse.exception.ResourceNotFoundException;
import com.pharmacy.warehouse.model.Role;
import com.pharmacy.warehouse.model.User;
import com.pharmacy.warehouse.repository.RoleRepository;
import com.pharmacy.warehouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String DEFAULT_PASSWORD = "12345@";
    private static final Pattern SIMPLE_EMAIL_REGEX = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "INACTIVE", "LOCKED");
    private static final Map<String, List<String>> ROLE_NAME_CANDIDATES;

    static {
        Map<String, List<String>> roleMap = new LinkedHashMap<>();
        roleMap.put("ADMIN", List.of("ADMIN", "ROLE_ADMIN"));
        roleMap.put("WAREHOUSE_MANAGER", List.of("WAREHOUSE_MANAGER", "ROLE_WAREHOUSE_MANAGER"));
        roleMap.put("WAREHOUSE_STAFF", List.of("WAREHOUSE_STAFF", "ROLE_WAREHOUSE_STAFF"));
        roleMap.put("ACCOUNTANT", List.of("ACCOUNTANT", "ROLE_ACCOUNTANT"));
        roleMap.put("REQUESTER", List.of("REQUESTER", "ROLE_REQUESTER"));
        ROLE_NAME_CANDIDATES = Collections.unmodifiableMap(roleMap);
    }

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public Page<UserResponse> getUsers(int page,
                                       int size,
                                       String search,
                                       String sortBy,
                                       String sortDir) {

        int normalizedPage = Math.max(page, 0);
        int normalizedSize = size <= 0 ? 10 : Math.min(size, 100);

        String sortableField = switch (sortBy) {
            case "email" -> "email";
            case "fullName" -> "fullName";
            case "status" -> "status";
            default -> "username";
        };

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(
                normalizedPage,
                normalizedSize,
                Sort.by(direction, sortableField)
        );

        if (StringUtils.hasText(search)) {
            Page<User> userPage = userRepository
                    .findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                            search.trim(),
                            search.trim(),
                            pageable
                    );
            return userPage.map(this::toResponse);
        }

        return userRepository.findAll(pageable).map(this::toResponse);
    }

    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return toResponse(user);
    }

    public UserResponse createUser(CreateUserRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setStatus(request.getStatus());

        // Luôn luôn dùng mật khẩu mặc định khi tạo user mới
        user.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
        
        // Yêu cầu đổi mật khẩu trong lần đăng nhập đầu tiên
        user.setForceChangePassword(true);

        if (request.getRoleId() != null) {
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + request.getRoleId()));
            user.setRole(role);
        }

        User saved = userRepository.save(user);
        
        // Gửi email thông báo tài khoản mới
        try {
            emailService.sendAccountCreationEmail(
                saved.getEmail(),
                saved.getFullName(),
                saved.getUsername(),
                DEFAULT_PASSWORD
            );
        } catch (Exception e) {
            // Log lỗi nhưng không rollback việc tạo user
            System.err.println("Lỗi khi gửi email thông báo tạo tài khoản: " + e.getMessage());
        }
        
        return toResponse(saved);
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        if (StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRoleId() != null) {
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + request.getRoleId()));
            user.setRole(role);
        }

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public User findByUsernameOrEmail(String usernameOrEmail) {
        User byUsername = userRepository.findByUsername(usernameOrEmail).orElse(null);
        if (byUsername != null) {
            return byUsername;
        }
        return userRepository.findByEmail(usernameOrEmail).orElse(null);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public BulkUserImportResponse previewBulkUsersFromCsv(MultipartFile file) {
        return processBulkUserImport(file, true);
    }

    public BulkUserImportResponse executeBulkUsersFromCsv(MultipartFile file) {
        return processBulkUserImport(file, false);
    }

    /**
     * Legacy API compatibility. Prefer previewBulkUsersFromCsv + executeBulkUsersFromCsv.
     */
    public List<UserResponse> importUsersFromCsv(MultipartFile file) {
        BulkUserImportResponse response = executeBulkUsersFromCsv(file);
        if (response.getInvalidRows() > 0) {
            throw new RuntimeException("Import aborted due to validation errors. Please preview and fix the file first.");
        }

        List<UserResponse> created = new ArrayList<>();
        for (BulkUserImportRowResult row : response.getRows()) {
            if (row.getCreatedUserId() != null) {
                created.add(getUser(row.getCreatedUserId()));
            }
        }
        return created;
    }

    private BulkUserImportResponse processBulkUserImport(MultipartFile file, boolean dryRun) {
        List<BulkUserImportRowResult> rows = new ArrayList<>();
        Set<String> seenUsernames = new HashSet<>();
        Set<String> seenEmails = new HashSet<>();

        List<ValidatedImportUser> validUsers = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (!StringUtils.hasText(headerLine)) {
                return BulkUserImportResponse.builder()
                        .dryRun(dryRun)
                        .totalRows(0)
                        .validRows(0)
                        .invalidRows(0)
                        .createdRows(0)
                        .message("CSV file is empty")
                        .rows(rows)
                        .build();
            }

            Map<String, Integer> indexMap = buildHeaderIndex(headerLine);
            validateRequiredHeaders(indexMap);

            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (!StringUtils.hasText(line)) {
                    continue;
                }

                ParsedImportRow parsed = parseImportRow(indexMap, line, rowNumber);
                List<String> errors = new ArrayList<>();

                String username = normalizeText(parsed.username);
                String fullName = normalizeText(parsed.fullName);
                String email = normalizeText(parsed.email);
                String status = normalizeStatus(parsed.status);
                String roleInput = normalizeRoleInput(parsed.roleInput);

                if (!StringUtils.hasText(username)) {
                    errors.add("Username is required");
                }
                if (!StringUtils.hasText(fullName)) {
                    errors.add("Full name is required");
                }

                validateEmail(email, errors);
                validateStatus(status, errors);

                if (StringUtils.hasText(username)) {
                    String key = username.toLowerCase();
                    if (seenUsernames.contains(key)) {
                        errors.add("Duplicate username in file");
                    } else {
                        seenUsernames.add(key);
                    }

                    if (userRepository.existsByUsernameIgnoreCase(username)) {
                        errors.add("Username already exists in system");
                    }
                }

                if (StringUtils.hasText(email)) {
                    String key = email.toLowerCase();
                    if (seenEmails.contains(key)) {
                        errors.add("Duplicate email in file");
                    } else {
                        seenEmails.add(key);
                    }

                    if (userRepository.existsByEmailIgnoreCase(email)) {
                        errors.add("Email already exists in system");
                    }
                }

                Role resolvedRole = resolveRole(parsed.roleId, roleInput, errors);

                boolean valid = errors.isEmpty();
                rows.add(BulkUserImportRowResult.builder()
                        .rowNumber(rowNumber)
                        .username(username)
                        .fullName(fullName)
                        .email(email)
                        .status(status)
                        .roleInput(roleInput)
                        .valid(valid)
                        .errors(errors)
                        .build());

                if (valid) {
                    validUsers.add(new ValidatedImportUser(
                            rows.size() - 1,
                            username,
                            fullName,
                            email,
                            status,
                            resolvedRole
                    ));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to process CSV file", e);
        }

        int invalidRows = (int) rows.stream().filter(row -> !row.isValid()).count();
        int validRows = rows.size() - invalidRows;
        int createdRows = 0;
        String message;

        if (!dryRun && invalidRows == 0) {
            for (ValidatedImportUser item : validUsers) {
                CreateUserRequest request = new CreateUserRequest();
                request.setUsername(item.username());
                request.setFullName(item.fullName());
                request.setEmail(item.email());
                request.setStatus(item.status());
                request.setRoleId(item.role().getRoleId());

                UserResponse created = createUser(request);
                createdRows++;

                BulkUserImportRowResult row = rows.get(item.rowIndex());
                row.setCreatedUserId(created.getUserId());
            }
            message = "Import completed successfully";
        } else if (!dryRun) {
            message = "Import aborted: validation errors found. Please preview and fix the file.";
        } else {
            message = "Preview completed";
        }

        return BulkUserImportResponse.builder()
                .dryRun(dryRun)
                .totalRows(rows.size())
                .validRows(validRows)
                .invalidRows(invalidRows)
                .createdRows(createdRows)
                .message(message)
                .rows(rows)
                .build();
    }

    private Map<String, Integer> buildHeaderIndex(String headerLine) {
        String[] headers = parseCsvLine(headerLine);
        Map<String, Integer> indexMap = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String name = headers[i].trim().replace("\"", "").toLowerCase();
            indexMap.put(name, i);
        }
        return indexMap;
    }

    private void validateRequiredHeaders(Map<String, Integer> indexMap) {
        String[] requiredCols = {"email", "username", "full_name", "status"};
        for (String col : requiredCols) {
            if (!indexMap.containsKey(col)) {
                throw new RuntimeException("CSV is missing required column: " + col);
            }
        }

        boolean hasRoleByName = indexMap.containsKey("role") || indexMap.containsKey("role_name");
        boolean hasRoleById = indexMap.containsKey("role_id");
        if (!hasRoleByName && !hasRoleById) {
            throw new RuntimeException("CSV must contain role/role_name or role_id column");
        }
    }

    private ParsedImportRow parseImportRow(Map<String, Integer> indexMap, String line, int rowNumber) {
        String[] cols = parseCsvLine(line);

        String roleInput = getCol(cols, indexMap, "role");
        if (!StringUtils.hasText(roleInput)) {
            roleInput = getCol(cols, indexMap, "role_name");
        }

        String roleId = getCol(cols, indexMap, "role_id");

        return new ParsedImportRow(
                rowNumber,
                getCol(cols, indexMap, "username"),
                getCol(cols, indexMap, "full_name"),
                getCol(cols, indexMap, "email"),
                getCol(cols, indexMap, "status"),
                roleInput,
                roleId
        );
    }

    private void validateEmail(String email, List<String> errors) {
        if (!StringUtils.hasText(email)) {
            errors.add("Email is required");
            return;
        }
        if (!SIMPLE_EMAIL_REGEX.matcher(email).matches()) {
            errors.add("Email format is invalid");
        }
    }

    private void validateStatus(String status, List<String> errors) {
        if (!StringUtils.hasText(status)) {
            errors.add("Status is required");
            return;
        }
        if (!ALLOWED_STATUSES.contains(status)) {
            errors.add("Invalid status. Allowed values: ACTIVE, INACTIVE, LOCKED");
        }
    }

    private Role resolveRole(String roleIdRaw, String roleInput, List<String> errors) {
        if (StringUtils.hasText(roleIdRaw)) {
            try {
                long roleId = Long.parseLong(roleIdRaw.trim());
                return roleRepository.findById(roleId)
                        .orElseGet(() -> {
                            errors.add("Role not found for role_id=" + roleId);
                            return null;
                        });
            } catch (NumberFormatException ex) {
                errors.add("Invalid role_id value");
                return null;
            }
        }

        if (!StringUtils.hasText(roleInput)) {
            errors.add("Role is required");
            return null;
        }

        List<String> candidates = ROLE_NAME_CANDIDATES.get(roleInput);
        if (candidates == null) {
            errors.add("Invalid role value. Allowed values: " + String.join(", ", ROLE_NAME_CANDIDATES.keySet()));
            return null;
        }

        for (String candidate : candidates) {
            Role role = roleRepository.findByRoleName(candidate).orElse(null);
            if (role != null) {
                return role;
            }
        }

        errors.add("Mapped role not found in database for value: " + roleInput);
        return null;
    }

    private String getCol(String[] cols, Map<String, Integer> indexMap, String key) {
        Integer idx = indexMap.get(key);
        if (idx == null || idx < 0 || idx >= cols.length) {
            return null;
        }
        return cols[idx].trim().replace("\"", "");
    }

    private String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                inQuotes = !inQuotes;
                continue;
            }

            if (ch == ',' && !inQuotes) {
                tokens.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(ch);
        }
        tokens.add(current.toString());

        return tokens.toArray(new String[0]);
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.trim().toUpperCase();
    }

    private String normalizeRoleInput(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.trim().toUpperCase();
    }

    private record ParsedImportRow(
            int rowNumber,
            String username,
            String fullName,
            String email,
            String status,
            String roleInput,
            String roleId
    ) {
    }

    private record ValidatedImportUser(
            int rowIndex,
            String username,
            String fullName,
            String email,
            String status,
            Role role
    ) {
    }

    /**
     * Ghi nhận lần đăng nhập gần nhất, lưu refresh token và trả về thông tin user.
     */
    public UserResponse recordLoginSuccess(String username, String refreshToken, LocalDateTime refreshExpiry) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        user.setLastLogin(LocalDateTime.now());
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(refreshExpiry);
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    private UserResponse toResponse(User user) {
        Long roleId = user.getRole() != null ? user.getRole().getRoleId() : null;
        String roleName = user.getRole() != null ? user.getRole().getRoleName() : null;

        return new UserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getStatus(),
                user.getLastLogin(),
                roleId,
                roleName
        );
    }

    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        // Kiểm tra mật khẩu cũ
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu cũ không chính xác");
        }

        // Kiểm tra xác nhận mật khẩu mới
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu mới và xác nhận mật khẩu không khớp");
        }

        // Cập nhật mật khẩu mới
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public void forceChangePassword(String username, ForceChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        // Kiểm tra xác nhận mật khẩu mới
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu mới và xác nhận mật khẩu không khớp");
        }

        // Cập nhật mật khẩu mới và tắt yêu cầu đổi mật khẩu
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setForceChangePassword(false);
        userRepository.save(user);
    }

    public boolean shouldForceChangePassword(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        
        return Boolean.TRUE.equals(user.getForceChangePassword());
    }
}

