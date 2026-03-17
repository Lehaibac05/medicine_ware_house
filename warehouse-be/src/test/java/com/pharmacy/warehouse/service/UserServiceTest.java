package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.dto.CreateUserRequest;
import com.pharmacy.warehouse.dto.UpdateUserRequest;
import com.pharmacy.warehouse.dto.UserResponse;
import com.pharmacy.warehouse.exception.ResourceNotFoundException;
import com.pharmacy.warehouse.model.Role;
import com.pharmacy.warehouse.model.User;
import com.pharmacy.warehouse.repository.RoleRepository;
import com.pharmacy.warehouse.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role testRole;
    private CreateUserRequest testCreateUserRequest;
    private UpdateUserRequest testUpdateUserRequest;
    private LocalDateTime testTime;

    @BeforeEach
    public void setup() {
        testTime = LocalDateTime.of(2023, 1, 1, 12, 0, 0);

        // Setup test role
        testRole = new Role();
        testRole.setRoleId(1L);
        testRole.setRoleName("ADMIN");
        testRole.setDescription("Administrator role");

        // Setup test user
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("testuser");
        testUser.setPasswordHash("hashedPassword");
        testUser.setFullName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setStatus("ACTIVE");
        testUser.setLastLogin(testTime);
        testUser.setRole(testRole);

        // Setup test create user request
        testCreateUserRequest = new CreateUserRequest();
        testCreateUserRequest.setUsername("newuser");
        testCreateUserRequest.setPassword("password123");
        testCreateUserRequest.setFullName("New User");
        testCreateUserRequest.setEmail("newuser@example.com");
        testCreateUserRequest.setStatus("ACTIVE");
        testCreateUserRequest.setRoleId(1L);

        // Setup test update user request
        testUpdateUserRequest = new UpdateUserRequest();
        testUpdateUserRequest.setFullName("Updated User");
        testUpdateUserRequest.setEmail("updated@example.com");
        testUpdateUserRequest.setStatus("INACTIVE");
        testUpdateUserRequest.setRoleId(1L);
        testUpdateUserRequest.setPassword("newPassword123");
    }

    // ==========================================
    // 1. BLACK-BOX TESTING (EP, BVA)
    // ==========================================

    /**
     * Kiểm thử Hộp đen: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
     * Target Hàm: getUsers(int page, int size, String search, String sortBy, String sortDir)
     * Phân tích các parameters:
     * - page: negative, zero, positive values
     * - size: zero, negative, positive, large values
     * - search: null, empty, valid text
     * - sortBy: valid fields, invalid fields
     * - sortDir: asc, desc, invalid values
     */
    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra getUsers() với các trường hợp khác nhau")
    public void testGetUsers_EquivalencePartition_BoundaryValue() {
        List<User> users = List.of(testUser);
        Page<User> userPage = new PageImpl<>(users);

        // Mock for search case
        when(userRepository.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(
            anyString(), anyString(), any(Pageable.class))).thenReturn(userPage);
        
        // Mock for non-search case
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        // Case 1: Valid parameters with search
        var result1 = userService.getUsers(0, 10, "test", "username", "asc");
        assertEquals(1, result1.getContent().size());
        assertEquals("testuser", result1.getContent().get(0).getUsername());

        // Case 2: Negative page (should be normalized to 0)
        var result2 = userService.getUsers(-1, 10, null, "username", "asc");
        assertEquals(1, result2.getContent().size());

        // Case 3: Zero size (should be normalized to 10)
        var result3 = userService.getUsers(0, 0, null, "username", "asc");
        assertEquals(1, result3.getContent().size());

        // Case 4: Large size (should be normalized to 100)
        var result4 = userService.getUsers(0, 200, null, "username", "asc");
        assertEquals(1, result4.getContent().size());

        verify(userRepository, atLeastOnce()).findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(
            anyString(), anyString(), any(Pageable.class));
        verify(userRepository, atLeastOnce()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra getUsers() với các sortBy khác nhau")
    public void testGetUsers_DifferentSortBy() {
        List<User> users = List.of(testUser);
        Page<User> userPage = new PageImpl<>(users);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        // Test different sortBy fields
        userService.getUsers(0, 10, null, "email", "asc");
        userService.getUsers(0, 10, null, "fullName", "asc");
        userService.getUsers(0, 10, null, "status", "asc");
        userService.getUsers(0, 10, null, "invalid", "asc"); // Should default to "username"

        verify(userRepository, times(4)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra getUsers() với các sortDir khác nhau")
    public void testGetUsers_DifferentSortDir() {
        List<User> users = List.of(testUser);
        Page<User> userPage = new PageImpl<>(users);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        // Test different sort directions
        userService.getUsers(0, 10, null, "username", "asc");
        userService.getUsers(0, 10, null, "username", "desc");
        userService.getUsers(0, 10, null, "username", "invalid"); // Should default to ASC

        verify(userRepository, times(3)).findAll(any(Pageable.class));
    }

    // ==========================================
    // 2. WHITE-BOX TESTING (CFG, DFG)
    // ==========================================

    /**
     * Kiểm thử luồng điều khiển (CFG - Branch Coverage)
     * Dành cho hàm getUser(Long id):
     * - Branch 1: userRepository.findById() trả về Optional.isPresent() -> tiếp tục xử lý
     * - Branch 2: userRepository.findById() trả về Optional.isEmpty() -> ném ResourceNotFoundException
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh getUser() - Success path")
    public void testGetUser_SuccessPath_BranchCoverage() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserResponse result = userService.getUser(1L);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals("testuser", result.getUsername());
        assertEquals("Test User", result.getFullName());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("ACTIVE", result.getStatus());
        assertEquals(testTime, result.getLastLogin());
        assertEquals(1L, result.getRoleId());
        assertEquals("ADMIN", result.getRoleName());

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh getUser() - Exception path")
    public void testGetUser_NotFound_BranchCoverage() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUser(999L);
        });

        assertEquals("User not found with id: 999", exception.getMessage());
        verify(userRepository, times(1)).findById(999L);
    }

    /**
     * Phân tích luồng dữ liệu (DFG - Data Flow Graph)
     * Hàm: createUser(CreateUserRequest request)
     * Quy trình DFG:
     * 1. Define parameter `request` -> Use để set user properties
     * 2. Use passwordEncoder.encode() -> set password hash
     * 3. Use request.getRoleId() -> repository.findById() -> Define role
     * 4. Use user -> repository.save() -> Define saved user
     * 5. Use saved user -> toResponse() -> return result
     */
    @Test
    @DisplayName("White-Box | DFG: Phân tích luồng dữ liệu createUser() đầy đủ")
    public void testCreateUser_CompleteDataFlow() {
        when(passwordEncoder.encode("12345@")).thenReturn("hashedPassword123");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setUserId(1L);
            return saved;
        });

        UserResponse result = userService.createUser(testCreateUserRequest);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals("newuser", result.getUsername());
        assertEquals("New User", result.getFullName());
        assertEquals("newuser@example.com", result.getEmail());
        assertEquals("ACTIVE", result.getStatus());
        assertEquals(1L, result.getRoleId());
        assertEquals("ADMIN", result.getRoleName());

        verify(passwordEncoder, times(1)).encode("12345@");
        verify(roleRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("White-Box | DFG: Phân tích luồng dữ liệu createUser() - null roleId")
    public void testCreateUser_NullRoleId_DataFlow() {
        testCreateUserRequest.setRoleId(null);

        when(passwordEncoder.encode("12345@")).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setUserId(1L);
            return saved;
        });

        UserResponse result = userService.createUser(testCreateUserRequest);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertNull(result.getRoleId());
        assertNull(result.getRoleName());

        verify(passwordEncoder, times(1)).encode("12345@");
        verify(roleRepository, never()).findById(any());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh createUser() - Role not found")
    public void testCreateUser_RoleNotFound_BranchCoverage() {
        when(passwordEncoder.encode("12345@")).thenReturn("hashedPassword123");
        when(roleRepository.findById(999L)).thenReturn(Optional.empty());

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setRoleId(999L);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.createUser(request);
        });

        assertEquals("Role not found with id: 999", exception.getMessage());
        verify(passwordEncoder, times(1)).encode("12345@");
        verify(roleRepository, times(1)).findById(999L);
        verify(userRepository, never()).save(any());
    }

    /**
     * Kiểm thử luồng điều khiển cho updateUser()
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh updateUser() - Success path")
    public void testUpdateUser_Success_BranchCoverage() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
        when(passwordEncoder.encode("newPassword123")).thenReturn("hashedNewPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = userService.updateUser(1L, testUpdateUserRequest);

        assertNotNull(result);
        assertEquals("Updated User", result.getFullName());
        assertEquals("updated@example.com", result.getEmail());
        assertEquals("INACTIVE", result.getStatus());
        assertEquals(1L, result.getRoleId());

        verify(userRepository, times(1)).findById(1L);
        verify(roleRepository, times(1)).findById(1L);
        verify(passwordEncoder, times(1)).encode("newPassword123");
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh updateUser() - User not found")
    public void testUpdateUser_UserNotFound_BranchCoverage() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.updateUser(999L, testUpdateUserRequest);
        });

        assertEquals("User not found with id: 999", exception.getMessage());
        verify(userRepository, times(1)).findById(999L);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh updateUser() - Null fields")
    public void testUpdateUser_NullFields_BranchCoverage() {
        UpdateUserRequest request = new UpdateUserRequest();
        // All fields are null

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = userService.updateUser(1L, request);

        assertNotNull(result);
        // Original values should remain unchanged
        assertEquals("Test User", result.getFullName());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("ACTIVE", result.getStatus());

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh updateUser() - Empty password")
    public void testUpdateUser_EmptyPassword_BranchCoverage() {
        testUpdateUserRequest.setPassword(""); // Empty password

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateUser(1L, testUpdateUserRequest);

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh updateUser() - Role not found")
    public void testUpdateUser_RoleNotFound_BranchCoverage() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateUserRequest request = new UpdateUserRequest();
        request.setRoleId(999L);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.updateUser(1L, request);
        });

        assertEquals("Role not found with id: 999", exception.getMessage());
        verify(userRepository, times(1)).findById(1L);
        verify(roleRepository, times(1)).findById(999L);
        verify(userRepository, never()).save(any());
    }

    /**
     * Kiểm thử luồng điều khiển cho deleteUser()
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh deleteUser() - Success path")
    public void testDeleteUser_Success_BranchCoverage() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository, times(1)).existsById(1L);
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh deleteUser() - User not found")
    public void testDeleteUser_NotFound_BranchCoverage() {
        when(userRepository.existsById(999L)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.deleteUser(999L);
        });

        assertEquals("User not found with id: 999", exception.getMessage());
        verify(userRepository, times(1)).existsById(999L);
        verify(userRepository, never()).deleteById(any());
    }

    // ==========================================
    // 3. SIMPLE METHOD TESTING
    // ==========================================

    @Test
    @DisplayName("Simple Method | findByEmail: Test found and not found")
    public void testFindByEmail() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        User result1 = userService.findByEmail("test@example.com");
        assertNotNull(result1);
        assertEquals("testuser", result1.getUsername());

        User result2 = userService.findByEmail("notfound@example.com");
        assertNull(result2);

        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(userRepository, times(1)).findByEmail("notfound@example.com");
    }

    @Test
    @DisplayName("Simple Method | findByUsernameOrEmail: Test username found")
    public void testFindByUsernameOrEmail_UsernameFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        User result = userService.findByUsernameOrEmail("testuser");
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());

        verify(userRepository, times(1)).findByUsername("testuser");
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Simple Method | findByUsernameOrEmail: Test email found")
    public void testFindByUsernameOrEmail_EmailFound() {
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        User result = userService.findByUsernameOrEmail("test@example.com");
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());

        verify(userRepository, times(1)).findByUsername("test@example.com");
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Simple Method | findByUsernameOrEmail: Test not found")
    public void testFindByUsernameOrEmail_NotFound() {
        when(userRepository.findByUsername("notfound")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("notfound")).thenReturn(Optional.empty());

        User result = userService.findByUsernameOrEmail("notfound");
        assertNull(result);

        verify(userRepository, times(1)).findByUsername("notfound");
        verify(userRepository, times(1)).findByEmail("notfound");
    }

    @Test
    @DisplayName("Simple Method | save: Test save user")
    public void testSave() {
        when(userRepository.save(testUser)).thenReturn(testUser);

        User result = userService.save(testUser);

        assertNotNull(result);
        assertEquals(testUser, result);

        verify(userRepository, times(1)).save(testUser);
    }

    // ==========================================
    // 4. COMPLEX METHOD TESTING - CSV IMPORT
    // ==========================================

    @Test
    @DisplayName("Complex Method | importUsersFromCsv: Test successful import")
    public void testImportUsersFromCsv_Success() throws IOException {
        String csvContent = "email,username,full_name,status,role_id\n" +
                "test1@example.com,user1,User One,ACTIVE,1\n" +
                "test2@example.com,user2,User Two,INACTIVE,2";

        MultipartFile file = createMockMultipartFile(csvContent);

        when(passwordEncoder.encode("12345@")).thenReturn("hashedPassword");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setUserId(System.currentTimeMillis());
            return saved;
        });

        List<UserResponse> result = userService.importUsersFromCsv(file);

        assertEquals(2, result.size());
        assertEquals("user1", result.get(0).getUsername());
        assertEquals("User One", result.get(0).getFullName());
        assertEquals("user2", result.get(1).getUsername());
        assertEquals("User Two", result.get(1).getFullName());

        verify(passwordEncoder, times(2)).encode("12345@");
        verify(roleRepository, times(2)).findById(any(Long.class));
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    @DisplayName("Complex Method | importUsersFromCsv: Test empty file")
    public void testImportUsersFromCsv_EmptyFile() throws IOException {
        MultipartFile file = createMockMultipartFile("");

        List<UserResponse> result = userService.importUsersFromCsv(file);

        assertTrue(result.isEmpty());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Complex Method | importUsersFromCsv: Test missing required columns")
    public void testImportUsersFromCsv_MissingRequiredColumns() throws IOException {
        String csvContent = "email,username,full_name\n" + // Missing status and role_id
                "test@example.com,user1,User One";

        MultipartFile file = createMockMultipartFile(csvContent);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.importUsersFromCsv(file);
        });

        assertTrue(exception.getMessage().contains("CSV is missing required column"));
    }

    @Test
    @DisplayName("Complex Method | importUsersFromCsv: Test invalid role_id")
    public void testImportUsersFromCsv_InvalidRoleId() throws IOException {
        String csvContent = "email,username,full_name,status,role_id\n" +
                "test@example.com,user1,User One,ACTIVE,invalid";

        MultipartFile file = createMockMultipartFile(csvContent);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.importUsersFromCsv(file);
        });

        assertTrue(exception.getMessage().contains("Invalid role_id value"));
    }

    @Test
    @DisplayName("Complex Method | importUsersFromCsv: Test empty role_id")
    public void testImportUsersFromCsv_EmptyRoleId() throws IOException {
        String csvContent = "email,username,full_name,status,role_id\n" +
                "test@example.com,user1,User One,ACTIVE,";

        MultipartFile file = createMockMultipartFile(csvContent);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.importUsersFromCsv(file);
        });

        assertTrue(exception.getMessage().contains("role_id is required"));
    }

    @Test
    @DisplayName("Complex Method | importUsersFromCsv: Test IO exception")
    public void testImportUsersFromCsv_IOException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenThrow(new IOException("Test IO exception"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.importUsersFromCsv(file);
        });

        assertTrue(exception.getMessage().contains("Failed to import users from CSV"));
    }

    // ==========================================
    // 5. PRIVATE METHOD TESTING
    // ==========================================

    @Test
    @DisplayName("Private Method | recordLoginSuccess: Test successful login recording")
    public void testRecordLoginSuccess_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = userService.recordLoginSuccess("testuser", "refreshToken", testTime.plusHours(1));

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());

        // Verify user was updated - check that lastLogin was set (not null)
        assertNotNull(testUser.getLastLogin());
        assertEquals("refreshToken", testUser.getRefreshToken());
        assertEquals(testTime.plusHours(1), testUser.getRefreshTokenExpiry());

        verify(userRepository, times(1)).findByUsername("testuser");
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("Private Method | recordLoginSuccess: Test user not found")
    public void testRecordLoginSuccess_UserNotFound() {
        when(userRepository.findByUsername("notfound")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.recordLoginSuccess("notfound", "refreshToken", testTime.plusHours(1));
        });

        assertEquals("User not found with username: notfound", exception.getMessage());
        verify(userRepository, times(1)).findByUsername("notfound");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Private Method | toResponse: Test with role")
    public void testToResponse_WithRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        UserResponse result = userService.getUser(1L);
        // This test indirectly tests toResponse through getUser
        assertNotNull(result);
        assertEquals(1L, result.getRoleId());
        assertEquals("ADMIN", result.getRoleName());
    }

    @Test
    @DisplayName("Private Method | toResponse: Test without role")
    public void testToResponse_WithoutRole() {
        testUser.setRole(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserResponse result = userService.getUser(1L);

        assertNotNull(result);
        assertNull(result.getRoleId());
        assertNull(result.getRoleName());

        verify(userRepository, times(1)).findById(1L);
    }

    // ==========================================
    // 6. SUPPLEMENTARY COVERAGE TESTS
    // ==========================================

    @Test
    @DisplayName("Supplementary: Test getUsers repository exception")
    public void testGetUsersRepositoryException() {
        when(userRepository.findAll(any(Pageable.class))).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            userService.getUsers(0, 10, null, "username", "asc");
        });
    }

    @Test
    @DisplayName("Supplementary: Test getUser repository exception")
    public void testGetUserRepositoryException() {
        when(userRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            userService.getUser(1L);
        });
    }

    @Test
    @DisplayName("Supplementary: Test recordLoginSuccess repository exception")
    public void testRecordLoginSuccessRepositoryException() {
        when(userRepository.findByUsername("testuser")).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> {
            userService.recordLoginSuccess("testuser", "refreshToken", testTime.plusHours(1));
        });
    }

    @Test
    @DisplayName("Supplementary: Test createUser with null role")
    public void testCreateUser_NullRole() {
        when(passwordEncoder.encode("12345@")).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setUserId(1L);
            return saved;
        });

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setFullName("Test User");
        request.setEmail("test@example.com");
        request.setStatus("ACTIVE");
        // roleId is null

        UserResponse result = userService.createUser(request);

        assertNotNull(result);
        assertNull(result.getRoleId());
        assertNull(result.getRoleName());

        verify(roleRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Supplementary: Test updateUser with null roleId")
    public void testUpdateUser_NullRoleId() {
        testUpdateUserRequest.setRoleId(null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = userService.updateUser(1L, testUpdateUserRequest);

        assertNotNull(result);
        // Role should remain unchanged
        assertEquals(1L, result.getRoleId());
        assertEquals("ADMIN", result.getRoleName());

        verify(roleRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Supplementary: Test importUsersFromCsv with quoted fields")
    public void testImportUsersFromCsv_QuotedFields() throws IOException {
        String csvContent = "email,username,full_name,status,role_id\n" +
                "\"test@example.com\",\"user1\",\"User One\",ACTIVE,1";

        MultipartFile file = createMockMultipartFile(csvContent);

        when(passwordEncoder.encode("12345@")).thenReturn("hashedPassword");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setUserId(1L);
            return saved;
        });

        List<UserResponse> result = userService.importUsersFromCsv(file);

        assertEquals(1, result.size());
        assertEquals("test@example.com", result.get(0).getEmail());
        assertEquals("user1", result.get(0).getUsername());
        assertEquals("User One", result.get(0).getFullName());
    }

    @Test
    @DisplayName("Supplementary: Test importUsersFromCsv with extra spaces")
    public void testImportUsersFromCsv_ExtraSpaces() throws IOException {
        String csvContent = "  email  ,  username  ,  full_name  ,  status  ,  role_id  \n" +
                "  test@example.com  ,  user1  ,  User One  ,  ACTIVE  ,  1  ";

        MultipartFile file = createMockMultipartFile(csvContent);

        when(passwordEncoder.encode("12345@")).thenReturn("hashedPassword");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setUserId(1L);
            return saved;
        });

        List<UserResponse> result = userService.importUsersFromCsv(file);

        assertEquals(1, result.size());
        assertEquals("test@example.com", result.get(0).getEmail());
        assertEquals("user1", result.get(0).getUsername());
        assertEquals("User One", result.get(0).getFullName());
    }

    // Helper method to create mock MultipartFile
    private MultipartFile createMockMultipartFile(String content) {
        return new MultipartFile() {
            @Override
            public String getName() { return "file"; }

            @Override
            public String getOriginalFilename() { return "test.csv"; }

            @Override
            public String getContentType() { return "text/csv"; }

            @Override
            public boolean isEmpty() { return content.isEmpty(); }

            @Override
            public long getSize() { return content.getBytes().length; }

            @Override
            public byte[] getBytes() { return content.getBytes(); }

            @Override
            public java.io.InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(content.getBytes());
            }

            @Override
            public void transferTo(java.io.File dest) throws IOException {
                // Mock implementation
            }
        };
    }
}
