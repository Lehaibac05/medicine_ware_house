package com.pharmacy.warehouse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.pharmacy.warehouse.model.Role;
import com.pharmacy.warehouse.model.User;
import com.pharmacy.warehouse.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User activeUser;
    private User inactiveUser;
    private User userWithNullRole;
    private User userWithNullStatus;
    private Role adminRole;

    @BeforeEach
    public void setup() {
        adminRole = new Role();
        adminRole.setRoleId(1L);
        adminRole.setRoleName("ADMIN");
        adminRole.setDescription("Administrator role");

        activeUser = new User();
        activeUser.setUserId(1L);
        activeUser.setUsername("admin");
        activeUser.setPasswordHash("$2a$10$encodedPassword");
        activeUser.setFullName("Admin User");
        activeUser.setEmail("admin@example.com");
        activeUser.setStatus("ACTIVE");
        activeUser.setRole(adminRole);

        inactiveUser = new User();
        inactiveUser.setUserId(2L);
        inactiveUser.setUsername("inactive");
        inactiveUser.setPasswordHash("$2a$10$encodedPassword2");
        inactiveUser.setFullName("Inactive User");
        inactiveUser.setEmail("inactive@example.com");
        inactiveUser.setStatus("INACTIVE");
        inactiveUser.setRole(adminRole);

        userWithNullRole = new User();
        userWithNullRole.setUserId(3L);
        userWithNullRole.setUsername("norole");
        userWithNullRole.setPasswordHash("$2a$10$encodedPassword3");
        userWithNullRole.setFullName("No Role User");
        userWithNullRole.setEmail("norole@example.com");
        userWithNullRole.setStatus("ACTIVE");
        userWithNullRole.setRole(null);

        userWithNullStatus = new User();
        userWithNullStatus.setUserId(4L);
        userWithNullStatus.setUsername("nullstatus");
        userWithNullStatus.setPasswordHash("$2a$10$encodedPassword4");
        userWithNullStatus.setFullName("Null Status User");
        userWithNullStatus.setEmail("nullstatus@example.com");
        userWithNullStatus.setStatus(null);
        userWithNullStatus.setRole(adminRole);
    }

    // ==========================================
    // 1. BLACK-BOX TESTING (EP, BVA)
    // ==========================================

    /**
     * Kiểm thử Hộp đen: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
     * Target Hàm: loadUserByUsername(String username)
     * Phân tích username:
     * - EP1: Username tồn tại và user active -> trả về UserDetails
     * - EP2: Username tồn tại nhưng user inactive -> trả về UserDetails với enabled=false
     * - EP3: Username không tồn tại -> ném UsernameNotFoundException
     * - BVA: Username = null, empty, valid
     */
    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra loadUserByUsername với active user")
    public void testLoadUserByUsername_ActiveUser_EquivalencePartition() {
        when(userRepository.findByUsername("admin")).thenReturn(java.util.Optional.of(activeUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("admin");

        assertNotNull(userDetails);
        assertEquals("admin", userDetails.getUsername());
        assertEquals("$2a$10$encodedPassword", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertEquals(1, userDetails.getAuthorities().size());
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));

        verify(userRepository, times(1)).findByUsername("admin");
    }

    @Test
    @DisplayName("Black-Box | EP: Kiểm tra loadUserByUsername với inactive user")
    public void testLoadUserByUsername_InactiveUser_EquivalencePartition() {
        when(userRepository.findByUsername("inactive")).thenReturn(java.util.Optional.of(inactiveUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("inactive");

        assertNotNull(userDetails);
        assertEquals("inactive", userDetails.getUsername());
        assertEquals("$2a$10$encodedPassword2", userDetails.getPassword());
        assertFalse(userDetails.isEnabled()); // User is inactive
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertEquals(1, userDetails.getAuthorities().size());
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));

        verify(userRepository, times(1)).findByUsername("inactive");
    }

    @Test
    @DisplayName("Black-Box | BVA: Kiểm tra loadUserByUsername với username không tồn tại")
    public void testLoadUserByUsername_UserNotFound_BoundaryValue() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(java.util.Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername("nonexistent");
        });

        assertEquals("User not found: nonexistent", exception.getMessage());
        verify(userRepository, times(1)).findByUsername("nonexistent");
    }

    // ==========================================
    // 2. WHITE-BOX TESTING (CFG, DFG)
    // ==========================================

    /**
     * Kiểm thử luồng điều khiển (CFG - Branch Coverage)
     * Dành cho hàm loadUserByUsername():
     * - Branch 1: userRepository.findByUsername() trả về Optional.isPresent() -> tiếp tục xử lý
     * - Branch 2: userRepository.findByUsername() trả về Optional.isEmpty() -> ném UsernameNotFoundException
     * - Branch 3: user.getStatus() != null && equalsIgnoreCase("ACTIVE") -> enabled = true
     * - Branch 4: user.getStatus() == null || !equalsIgnoreCase("ACTIVE") -> enabled = false
     * - Branch 5: user.getRole() == null -> emptySet()
     * - Branch 6: user.getRole() != null -> singleton(authority)
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh success path - user found và active")
    public void testLoadUserByUsername_SuccessPath_BranchCoverage() {
        when(userRepository.findByUsername("admin")).thenReturn(java.util.Optional.of(activeUser));

        UserDetails result = customUserDetailsService.loadUserByUsername("admin");

        assertNotNull(result);
        assertTrue(result.isEnabled());
        assertEquals(1, result.getAuthorities().size());
        verify(userRepository, times(1)).findByUsername("admin");
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh exception path - user not found")
    public void testLoadUserByUsername_ExceptionPath_BranchCoverage() {
        when(userRepository.findByUsername("unknown")).thenReturn(java.util.Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername("unknown");
        });

        verify(userRepository, times(1)).findByUsername("unknown");
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh user với null role")
    public void testLoadUserByUsername_NullRole_BranchCoverage() {
        when(userRepository.findByUsername("norole")).thenReturn(java.util.Optional.of(userWithNullRole));

        UserDetails result = customUserDetailsService.loadUserByUsername("norole");

        assertNotNull(result);
        assertTrue(result.isEnabled());
        assertEquals(0, result.getAuthorities().size()); // Empty authorities set
        verify(userRepository, times(1)).findByUsername("norole");
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh user với null status")
    public void testLoadUserByUsername_NullStatus_BranchCoverage() {
        when(userRepository.findByUsername("nullstatus")).thenReturn(java.util.Optional.of(userWithNullStatus));

        UserDetails result = customUserDetailsService.loadUserByUsername("nullstatus");

        assertNotNull(result);
        assertFalse(result.isEnabled()); // Null status means not enabled
        assertEquals(1, result.getAuthorities().size());
        verify(userRepository, times(1)).findByUsername("nullstatus");
    }

    /**
     * Phân tích luồng dữ liệu (DFG - Data Flow Graph)
     * Hàm: loadUserByUsername(String username)
     * Quy trình DFG:
     * 1. Define parameter `username`
     * 2. Use `username` trong userRepository.findByUsername()
     * 3. Define `user` từ repository
     * 4. Use `user.getStatus()` để xác định enabled
     * 5. Use `user.getRole()` để tạo authorities
     * 6. Use `user.getUsername()`, `user.getPasswordHash()` để tạo UserDetails
     */
    @Test
    @DisplayName("White-Box | DFG: Phân tích luồng dữ liệu đầy đủ")
    public void testLoadUserByUsername_CompleteDataFlow() {
        when(userRepository.findByUsername("admin")).thenReturn(java.util.Optional.of(activeUser));

        UserDetails result = customUserDetailsService.loadUserByUsername("admin");

        // Verify data flow from input through all processing steps
        assertEquals("admin", result.getUsername()); // From user.getUsername()
        assertEquals("$2a$10$encodedPassword", result.getPassword()); // From user.getPasswordHash()
        assertTrue(result.isEnabled()); // From user.getStatus() logic
        assertEquals(1, result.getAuthorities().size()); // From user.getRole() logic
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));

        verify(userRepository, times(1)).findByUsername("admin");
    }

    // ==========================================
    // 3. SUPPLEMENTARY COVERAGE TESTS
    // ==========================================

    @Test
    @DisplayName("Supplementary: Test với username null")
    public void testLoadUserByUsername_NullUsername() {
        when(userRepository.findByUsername(null)).thenReturn(java.util.Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername(null);
        });

        verify(userRepository, times(1)).findByUsername(null);
    }

    @Test
    @DisplayName("Supplementary: Test với username empty string")
    public void testLoadUserByUsername_EmptyUsername() {
        when(userRepository.findByUsername("")).thenReturn(java.util.Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername("");
        });

        verify(userRepository, times(1)).findByUsername("");
    }

    @Test
    @DisplayName("Supplementary: Test với user có status khác ACTIVE")
    public void testLoadUserByUsername_NonActiveStatus() {
        User userWithDifferentStatus = new User();
        userWithDifferentStatus.setUserId(5L);
        userWithDifferentStatus.setUsername("different");
        userWithDifferentStatus.setPasswordHash("$2a$10$encodedPassword5");
        userWithDifferentStatus.setStatus("SUSPENDED");
        userWithDifferentStatus.setRole(adminRole);

        when(userRepository.findByUsername("different")).thenReturn(java.util.Optional.of(userWithDifferentStatus));

        UserDetails result = customUserDetailsService.loadUserByUsername("different");

        assertNotNull(result);
        assertFalse(result.isEnabled()); // Status is not "ACTIVE"
        assertEquals(1, result.getAuthorities().size());
        verify(userRepository, times(1)).findByUsername("different");
    }

    @Test
    @DisplayName("Supplementary: Test với user có status lowercase 'active'")
    public void testLoadUserUsername_LowercaseActiveStatus() {
        User userWithLowercaseStatus = new User();
        userWithLowercaseStatus.setUserId(6L);
        userWithLowercaseStatus.setUsername("lowercase");
        userWithLowercaseStatus.setPasswordHash("$2a$10$encodedPassword6");
        userWithLowercaseStatus.setStatus("active"); // lowercase
        userWithLowercaseStatus.setRole(adminRole);

        when(userRepository.findByUsername("lowercase")).thenReturn(java.util.Optional.of(userWithLowercaseStatus));

        UserDetails result = customUserDetailsService.loadUserByUsername("lowercase");

        assertNotNull(result);
        assertTrue(result.isEnabled()); // Should be enabled due to equalsIgnoreCase
        assertEquals(1, result.getAuthorities().size());
        verify(userRepository, times(1)).findByUsername("lowercase");
    }

    @Test
    @DisplayName("Supplementary: Test với user có role name khác")
    public void testLoadUserByUsername_DifferentRoleName() {
        Role userRole = new Role();
        userRole.setRoleId(2L);
        userRole.setRoleName("USER");
        userRole.setDescription("Regular user role");

        User regularUser = new User();
        regularUser.setUserId(7L);
        regularUser.setUsername("user");
        regularUser.setPasswordHash("$2a$10$encodedPassword7");
        regularUser.setStatus("ACTIVE");
        regularUser.setRole(userRole);

        when(userRepository.findByUsername("user")).thenReturn(java.util.Optional.of(regularUser));

        UserDetails result = customUserDetailsService.loadUserByUsername("user");

        assertNotNull(result);
        assertTrue(result.isEnabled());
        assertEquals(1, result.getAuthorities().size());
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
        verify(userRepository, times(1)).findByUsername("user");
    }

    @Test
    @DisplayName("Supplementary: Test khi repository throws exception")
    public void testLoadUserByUsername_RepositoryThrowsException() {
        when(userRepository.findByUsername(anyString())).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> {
            customUserDetailsService.loadUserByUsername("admin");
        });

        verify(userRepository, times(1)).findByUsername("admin");
    }

    @Test
    @DisplayName("Supplementary: Test verify UserDetails properties")
    public void testLoadUserByUsername_VerifyAllUserDetailsProperties() {
        when(userRepository.findByUsername("admin")).thenReturn(java.util.Optional.of(activeUser));

        UserDetails result = customUserDetailsService.loadUserByUsername("admin");

        assertNotNull(result);
        assertEquals("admin", result.getUsername());
        assertEquals("$2a$10$encodedPassword", result.getPassword());
        assertTrue(result.isEnabled());
        assertTrue(result.isAccountNonExpired()); // Always true
        assertTrue(result.isAccountNonLocked()); // Always true
        assertTrue(result.isCredentialsNonExpired()); // Always true
        assertEquals(1, result.getAuthorities().size());
        verify(userRepository, times(1)).findByUsername("admin");
    }

    @Test
    @DisplayName("Supplementary: Test với user có role name null")
    public void testLoadUserByUsername_RoleNameNull() {
        Role roleWithNullName = new Role();
        roleWithNullName.setRoleId(3L);
        roleWithNullName.setRoleName(null);
        roleWithNullName.setDescription("Role with null name");

        User userWithNullRoleName = new User();
        userWithNullRoleName.setUserId(8L);
        userWithNullRoleName.setUsername("nullrolename");
        userWithNullRoleName.setPasswordHash("$2a$10$encodedPassword8");
        userWithNullRoleName.setStatus("ACTIVE");
        userWithNullRoleName.setRole(roleWithNullName);

        when(userRepository.findByUsername("nullrolename")).thenReturn(java.util.Optional.of(userWithNullRoleName));

        UserDetails result = customUserDetailsService.loadUserByUsername("nullrolename");

        assertNotNull(result);
        assertTrue(result.isEnabled());
        assertEquals(1, result.getAuthorities().size());
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
        verify(userRepository, times(1)).findByUsername("nullrolename");
    }
}
