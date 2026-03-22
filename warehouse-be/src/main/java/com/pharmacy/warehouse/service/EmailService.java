package com.pharmacy.warehouse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            message.setFrom("noreply@pharmacy-warehouse.com");
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    public void sendAccountCreationEmail(String toEmail, String fullName, String username, String temporaryPassword) {
        try {
            log.info("Bắt đầu gửi email đến: {}", toEmail);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Chào mừng bạn đến với Hệ thống Quản lý Kho Thuốc - Thông tin tài khoản");

            Context context = new Context();
            context.setVariable("fullName", fullName);
            context.setVariable("username", username);
            context.setVariable("temporaryPassword", temporaryPassword);
            context.setVariable("loginUrl", "http://localhost:5173/login"); // Frontend URL

            String htmlContent = templateEngine.process("account-creation-email", context);
            helper.setText(htmlContent, true);

            helper.setFrom("noreply@pharmacy-warehouse.com");

            mailSender.send(message);
            log.info("Email thông báo tạo tài khoản đã được gửi thành công đến: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Lỗi khi gửi email thông báo tạo tài khoản đến {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Không thể gửi email thông báo tạo tài khoản", e);
        }
    }
}

