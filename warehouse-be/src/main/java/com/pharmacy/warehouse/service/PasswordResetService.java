package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.model.User;
import com.pharmacy.warehouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Tạo OTP và lưu vào user, giả lập việc gửi email.
     */
    public void requestReset(String email, String ip, String userAgent) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return;
        }

        String otp = generateOtp();
        user.setResetOtp(otp);
        user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(10));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        log.info("Password reset requested for email {} from ip {}, agent {}. OTP: {}",
                email, ip, userAgent, otp);

        String subject = "Mã OTP đặt lại mật khẩu";
        String body = "Xin chào,\n\n"
                + "Mã OTP để đặt lại mật khẩu của bạn là: " + otp + "\n"
                + "OTP có hiệu lực trong 10 phút.\n\n"
                + "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.";
        emailService.sendSimpleMessage(email, subject, body);
    }

    public String verifyOtp(String email, String otp) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(otp)) {
            throw new IllegalArgumentException("Email và OTP là bắt buộc");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null ||
                user.getResetOtp() == null ||
                user.getResetOtpExpiry() == null ||
                !otp.equals(user.getResetOtp()) ||
                user.getResetOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Mã OTP không hợp lệ hoặc đã hết hạn");
        }

        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        return resetToken;
    }

    public void resetPassword(String email, String resetToken, String newPassword) {
        if (!StringUtils.hasText(email) ||
                !StringUtils.hasText(resetToken) ||
                !StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("Email, reset token và mật khẩu mới là bắt buộc");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null ||
                user.getResetToken() == null ||
                user.getResetTokenExpiry() == null ||
                !resetToken.equals(user.getResetToken()) ||
                user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Reset token không hợp lệ hoặc đã hết hạn");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetOtp(null);
        user.setResetOtpExpiry(null);
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    private String generateOtp() {
        Random random = new Random();
        int value = 100000 + random.nextInt(900000);
        return String.valueOf(value);
    }
}

