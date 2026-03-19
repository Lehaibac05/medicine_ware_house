package com.pharmacy.warehouse.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    @DisplayName("Gửi email thành công")
    public void testSendSimpleMessage_Success() {
        // Gọi hàm
        emailService.sendSimpleMessage("test@example.com", "Subject", "Body");

        // Verify mailSender.send được gọi 1 lần
        Mockito.verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Gửi email thất bại (Exception)")
    public void testSendSimpleMessage_Exception() {
        // Giả lập ngoại lệ khi gọi send
        doThrow(new RuntimeException("Mail server is down")).when(mailSender).send(any(SimpleMailMessage.class));

        // Gọi hàm sẽ catch exception bên trong và báo lỗi log
        emailService.sendSimpleMessage("test@example.com", "Subject", "Body");

        // Vẫn verify mailSender.send được gọi 1 lần
        Mockito.verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
