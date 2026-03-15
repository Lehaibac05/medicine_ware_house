package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.model.User;
import com.pharmacy.warehouse.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User testUser;

    @BeforeEach
    public void setup() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("oldpassword");
        testUser.setStatus("ACTIVE");
    }

    // ==========================================
    // 1. BLACK-BOX TESTING (EP, BVA)
    // ==========================================

    /**
     * Kiểm thử Hộp đen: Phân tích giá trị biên (BVA) & Phân vùng tương đương (EP)
     * Target Hàm: requestReset(String email, String ip, String userAgent)
     * Phân tích email:
     * - EP1: Email tồn tại trong hệ thống -> tạo OTP và gửi email
     * - EP2: Email không tồn tại -> không làm gì cả (silent fail)
     * - BVA: Email = null, empty, valid, invalid format
     */
    @Test
    @DisplayName("Black-Box | EP & BVA: Kiểm tra requestReset() với email tồn tại")
    public void testRequestReset_EmailExists_EquivalencePartition() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(emailService).sendSimpleMessage(anyString(), anyString(), anyString());

        passwordResetService.requestReset("test@example.com", "192.168.1.1", "Mozilla/5.0");

        // Verify OTP was generated and saved
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        
        assertNotNull(savedUser.getResetOtp());
        assertEquals(6, savedUser.getResetOtp().length());
        assertTrue(savedUser.getResetOtpExpiry().isAfter(LocalDateTime.now()));
        assertNull(savedUser.getResetToken());
        assertNull(savedUser.getResetTokenExpiry());

        // Verify email was sent
        verify(emailService, times(1)).sendSimpleMessage(
                eq("test@example.com"),
                eq("Mã OTP đặt lại mật khẩu"),
                contains("Mã OTP để đặt lại mật khẩu của bạn là: ")
        );

        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Black-Box | EP: Kiểm tra requestReset() với email không tồn tại")
    public void testRequestReset_EmailNotExists_EquivalencePartition() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Should not throw exception, should handle silently
        assertDoesNotThrow(() -> {
            passwordResetService.requestReset("nonexistent@example.com", "192.168.1.1", "Mozilla/5.0");
        });

        // Verify no save or email was sent
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
        verify(userRepository, times(1)).findByEmail("nonexistent@example.com");
    }

    @Test
    @DisplayName("Black-Box | BVA: Kiểm tra requestReset() với null email")
    public void testRequestReset_NullEmail_BoundaryValue() {
        when(userRepository.findByEmail(null)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> {
            passwordResetService.requestReset(null, "192.168.1.1", "Mozilla/5.0");
        });

        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
        verify(userRepository, times(1)).findByEmail(null);
    }

    @Test
    @DisplayName("Black-Box | BVA: Kiểm tra requestReset() với empty email")
    public void testRequestReset_EmptyEmail_BoundaryValue() {
        when(userRepository.findByEmail("")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> {
            passwordResetService.requestReset("", "192.168.1.1", "Mozilla/5.0");
        });

        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
        verify(userRepository, times(1)).findByEmail("");
    }

    // ==========================================
    // 2. WHITE-BOX TESTING (CFG, DFG)
    // ==========================================

    /**
     * Kiểm thử luồng điều khiển (CFG - Branch Coverage)
     * Dành cho hàm verifyOtp(String email, String otp):
     * - Branch 1: !StringUtils.hasText(email) || !StringUtils.hasText(otp) -> throw IllegalArgumentException
     * - Branch 2: userRepository.findByEmail() empty -> throw IllegalArgumentException
     * - Branch 3: user.getResetOtp() == null -> throw IllegalArgumentException
     * - Branch 4: user.getResetOtpExpiry() == null -> throw IllegalArgumentException
     * - Branch 5: !otp.equals(user.getResetOtp()) -> throw IllegalArgumentException
     * - Branch 6: user.getResetOtpExpiry().isBefore(LocalDateTime.now()) -> throw IllegalArgumentException
     * - Branch 7: Success path -> generate and return reset token
     */
    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh verifyOtp() - Success path")
    public void testVerifyOtp_SuccessPath_BranchCoverage() {
        // Setup user with valid OTP
        testUser.setResetOtp("123456");
        testUser.setResetOtpExpiry(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String resetToken = passwordResetService.verifyOtp("test@example.com", "123456");

        assertNotNull(resetToken);
        assertTrue(resetToken.matches("[a-f0-9\\-]{36}")); // UUID format
        
        // Verify reset token was set
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        
        assertEquals(resetToken, savedUser.getResetToken());
        assertTrue(savedUser.getResetTokenExpiry().isAfter(LocalDateTime.now()));
        // OTP should still be valid until password reset
        assertEquals("123456", savedUser.getResetOtp());

        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh verifyOtp() - Empty email and OTP")
    public void testVerifyOtp_EmptyEmailAndOtp_BranchCoverage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            passwordResetService.verifyOtp("", "");
        });

        assertEquals("Email và OTP là bắt buộc", exception.getMessage());
        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh verifyOtp() - Null email")
    public void testVerifyOtp_NullEmail_BranchCoverage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            passwordResetService.verifyOtp(null, "123456");
        });

        assertEquals("Email và OTP là bắt buộc", exception.getMessage());
        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh verifyOtp() - Null OTP")
    public void testVerifyOtp_NullOtp_BranchCoverage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            passwordResetService.verifyOtp("test@example.com", null);
        });

        assertEquals("Email và OTP là bắt buộc", exception.getMessage());
        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh verifyOtp() - User not found")
    public void testVerifyOtp_UserNotFound_BranchCoverage() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            passwordResetService.verifyOtp("nonexistent@example.com", "123456");
        });

        assertEquals("Mã OTP không hợp lệ hoặc đã hết hạn", exception.getMessage());
        verify(userRepository, times(1)).findByEmail("nonexistent@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh verifyOtp() - Null OTP in user")
    public void testVerifyOtp_NullOtpInUser_BranchCoverage() {
        testUser.setResetOtp(null);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            passwordResetService.verifyOtp("test@example.com", "123456");
        });

        assertEquals("Mã OTP không hợp lệ hoặc đã hết hạn", exception.getMessage());
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh verifyOtp() - Null expiry in user")
    public void testVerifyOtp_NullExpiryInUser_BranchCoverage() {
        testUser.setResetOtp("123456");
        testUser.setResetOtpExpiry(null);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            passwordResetService.verifyOtp("test@example.com", "123456");
        });

        assertEquals("Mã OTP không hợp lệ hoặc đã hết hạn", exception.getMessage());
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh verifyOtp() - OTP mismatch")
    public void testVerifyOtp_OtpMismatch_BranchCoverage() {
        testUser.setResetOtp("654321");
        testUser.setResetOtpExpiry(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            passwordResetService.verifyOtp("test@example.com", "123456");
        });

        assertEquals("Mã OTP không hợp lệ hoặc đã hết hạn", exception.getMessage());
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh verifyOtp() - OTP expired")
    public void testVerifyOtp_OtpExpired_BranchCoverage() {
        testUser.setResetOtp("123456");
        testUser.setResetOtpExpiry(LocalDateTime.now().minusMinutes(1)); // Expired

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            passwordResetService.verifyOtp("test@example.com", "123456");
        });

        assertEquals("Mã OTP không hợp lệ hoặc đã hết hạn", exception.getMessage());
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(userRepository, never()).save(any());
    }

    /**
     * Phân tích luồng dữ liệu (DFG - Data Flow Graph)
     * Hàm: resetPassword(String email, String resetToken, String newPassword)
     * Quy trình DFG:
     * 1. Define parameters -> Use trong validation checks
     * 2. Use email -> userRepository.findByEmail() -> Define user
     * 3. Use user.getResetToken() -> validation
     * 4. Use newPassword -> passwordEncoder.encode() -> Use trong user.setPasswordHash()
     * 5. Use user -> userRepository.save()
     */
    @Test
    @DisplayName("White-Box | DFG: Phân tích luồng dữ liệu resetPassword() đầy đủ")
    public void testResetPassword_CompleteDataFlow() {
        // Setup user with valid reset token
        testUser.setResetToken(UUID.randomUUID().toString());
        testUser.setResetTokenExpiry(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newpassword123")).thenReturn("encoded_newpassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        passwordResetService.resetPassword("test@example.com", testUser.getResetToken(), "newpassword123");

        // Verify password was encoded and set
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        
        assertEquals("encoded_newpassword", savedUser.getPasswordHash());
        // Verify all reset fields were cleared
        assertNull(savedUser.getResetOtp());
        assertNull(savedUser.getResetOtpExpiry());
        assertNull(savedUser.getResetToken());
        assertNull(savedUser.getResetTokenExpiry());

        verify(passwordEncoder, times(1)).encode("newpassword123");
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh resetPassword() - Empty parameters")
    public void testResetPassword_EmptyParameters_BranchCoverage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            passwordResetService.resetPassword("", "", "");
        });

        assertEquals("Email, reset token và mật khẩu mới là bắt buộc", exception.getMessage());
        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh resetPassword() - Null parameters")
    public void testResetPassword_NullParameters_BranchCoverage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            passwordResetService.resetPassword(null, null, null);
        });

        assertEquals("Email, reset token và mật khẩu mới là bắt buộc", exception.getMessage());
        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("White-Box | CFG: Phủ nhánh resetPassword() - Invalid reset token")
    public void testResetPassword_InvalidResetToken_BranchCoverage() {
        testUser.setResetToken("valid-token");
        testUser.setResetTokenExpiry(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            passwordResetService.resetPassword("test@example.com", "invalid-token", "newpassword123");
        });

        assertEquals("Reset token không hợp lệ hoặc đã hết hạn", exception.getMessage());
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    // ==========================================
    // 3. PRIVATE METHOD TESTING
    // ==========================================

    /**
     * Test private method generateOtp() thông qua public requestReset()
     * Verify OTP format and range
     */
    @Test
    @DisplayName("Private Method | generateOtp: Test OTP generation")
    public void testGenerateOtp_ViaRequestReset() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(emailService).sendSimpleMessage(anyString(), anyString(), anyString());

        // Call multiple times to test randomness
        passwordResetService.requestReset("test@example.com", "192.168.1.1", "Mozilla/5.0");
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        User savedUser1 = userCaptor.getValue();
        String otp1 = savedUser1.getResetOtp();

        // Reset user and call again
        testUser.setResetOtp(null);
        testUser.setResetOtpExpiry(null);
        
        passwordResetService.requestReset("test@example.com", "192.168.1.1", "Mozilla/5.0");
        verify(userRepository, times(2)).save(userCaptor.capture());
        User savedUser2 = userCaptor.getValue();
        String otp2 = savedUser2.getResetOtp();

        // Verify OTP format and range
        assertEquals(6, otp1.length());
        assertEquals(6, otp2.length());
        assertTrue(otp1.matches("\\d{6}"));
        assertTrue(otp2.matches("\\d{6}"));
        
        // Verify OTPs are different (randomness)
        assertNotEquals(otp1, otp2);
        
        // Verify OTP range (100000-999999)
        int otpNum1 = Integer.parseInt(otp1);
        int otpNum2 = Integer.parseInt(otp2);
        assertTrue(otpNum1 >= 100000 && otpNum1 <= 999999);
        assertTrue(otpNum2 >= 100000 && otpNum2 <= 999999);
    }

    // ==========================================
    // 4. SUPPLEMENTARY COVERAGE TESTS
    // ==========================================

    @Test
    @DisplayName("Supplementary: Test requestReset() with repository exception")
    public void testRequestReset_RepositoryException() {
        when(userRepository.findByEmail("test@example.com"))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> {
            passwordResetService.requestReset("test@example.com", "192.168.1.1", "Mozilla/5.0");
        });

        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Supplementary: Test verifyOtp() with repository exception")
    public void testVerifyOtp_RepositoryException() {
        when(userRepository.findByEmail("test@example.com"))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> {
            passwordResetService.verifyOtp("test@example.com", "123456");
        });

        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Supplementary: Test resetPassword() with repository exception")
    public void testResetPassword_RepositoryException() {
        when(userRepository.findByEmail("test@example.com"))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> {
            passwordResetService.resetPassword("test@example.com", "token", "newpassword");
        });

        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("Supplementary: Test resetPassword() with password encoder exception")
    public void testResetPassword_PasswordEncoderException() {
        testUser.setResetToken(UUID.randomUUID().toString());
        testUser.setResetTokenExpiry(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newpassword")).thenThrow(new RuntimeException("Encoding error"));

        assertThrows(RuntimeException.class, () -> {
            passwordResetService.resetPassword("test@example.com", testUser.getResetToken(), "newpassword");
        });

        verify(passwordEncoder, times(1)).encode("newpassword");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Supplementary: Test requestReset() clears existing reset token")
    public void testRequestReset_ClearsExistingResetToken() {
        // Setup user with existing reset token
        testUser.setResetToken("existing-token");
        testUser.setResetTokenExpiry(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(emailService).sendSimpleMessage(anyString(), anyString(), anyString());

        passwordResetService.requestReset("test@example.com", "192.168.1.1", "Mozilla/5.0");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        // Verify existing reset token was cleared
        assertNull(savedUser.getResetToken());
        assertNull(savedUser.getResetTokenExpiry());
        // Verify new OTP was set
        assertNotNull(savedUser.getResetOtp());
        assertNotNull(savedUser.getResetOtpExpiry());
    }

    @Test
    @DisplayName("Supplementary: Test verifyOtp() with whitespace email and OTP")
    public void testVerifyOtp_WhitespaceParameters() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            passwordResetService.verifyOtp("   ", "   ");
        });

        assertEquals("Email và OTP là bắt buộc", exception.getMessage());
        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Supplementary: Test resetPassword() with whitespace parameters")
    public void testResetPassword_WhitespaceParameters() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            passwordResetService.resetPassword("   ", "   ", "   ");
        });

        assertEquals("Email, reset token và mật khẩu mới là bắt buộc", exception.getMessage());
        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("Supplementary: Test resetPassword() with expired reset token")
    public void testResetPassword_ExpiredResetToken() {
        testUser.setResetToken(UUID.randomUUID().toString());
        testUser.setResetTokenExpiry(LocalDateTime.now().minusMinutes(1)); // Expired

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            passwordResetService.resetPassword("test@example.com", testUser.getResetToken(), "newpassword");
        });

        assertEquals("Reset token không hợp lệ hoặc đã hết hạn", exception.getMessage());
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("Supplementary: Test resetPassword() with null reset token in user")
    public void testResetPassword_NullResetTokenInUser() {
        testUser.setResetToken(null);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            passwordResetService.resetPassword("test@example.com", "some-token", "newpassword");
        });

        assertEquals("Reset token không hợp lệ hoặc đã hết hạn", exception.getMessage());
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("Supplementary: Test resetPassword() with null reset token expiry in user")
    public void testResetPassword_NullResetTokenExpiryInUser() {
        testUser.setResetToken(UUID.randomUUID().toString());
        testUser.setResetTokenExpiry(null);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            passwordResetService.resetPassword("test@example.com", testUser.getResetToken(), "newpassword");
        });

        assertEquals("Reset token không hợp lệ hoặc đã hết hạn", exception.getMessage());
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("Supplementary: Test resetPassword() with email service exception")
    public void testRequestReset_EmailServiceException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("Email send failed"))
                .when(emailService).sendSimpleMessage(anyString(), anyString(), anyString());

        // EmailService actually throws exception, so PasswordResetService will also throw
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            passwordResetService.requestReset("test@example.com", "192.168.1.1", "Mozilla/5.0");
        });

        assertEquals("Email send failed", exception.getMessage());
        
        // Verify OTP was still generated and saved despite email failure
        verify(userRepository, times(1)).save(any());
        verify(emailService, times(1)).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Supplementary: Test resetPassword() - User not found (missing branch)")
    public void testResetPassword_UserNotFound_MissingBranch() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            passwordResetService.resetPassword("nonexistent@example.com", "valid-token", "newpassword123");
        });

        assertEquals("Reset token không hợp lệ hoặc đã hết hạn", exception.getMessage());
        verify(userRepository, times(1)).findByEmail("nonexistent@example.com");
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("Supplementary: Test resetPassword() - Partial null parameters (missing branches)")
    public void testResetPassword_PartialNullParameters_MissingBranches() {
        // Test with null email only
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () -> {
            passwordResetService.resetPassword(null, "valid-token", "newpassword123");
        });
        assertEquals("Email, reset token và mật khẩu mới là bắt buộc", exception1.getMessage());

        // Test with null reset token only
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () -> {
            passwordResetService.resetPassword("test@example.com", null, "newpassword123");
        });
        assertEquals("Email, reset token và mật khẩu mới là bắt buộc", exception2.getMessage());

        // Test with null password only
        IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class, () -> {
            passwordResetService.resetPassword("test@example.com", "valid-token", null);
        });
        assertEquals("Email, reset token và mật khẩu mới là bắt buộc", exception3.getMessage());

        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }
}
