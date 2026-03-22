# Hướng dẫn cấu hình Email cho tính năng gửi thông báo tài khoản

## 1. Cấu hình Email trong application.properties

Sao chép file `application.properties.example` thành `application.properties` và cấu hình các thông số sau:

```properties
# ===============================
# MAIL CONFIGURATION
# ===============================
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000
```

## 2. Cấu hình Gmail (nếu dùng Gmail)

### Bước 1: Bật 2-Factor Authentication
1. Đăng nhập vào tài khoản Gmail
2. Vào Settings > Security
3. Bật "2-Step Verification"

### Bước 2: Tạo App Password
1. Vào Google Account settings
2. Chọn "Security"
3. Kéo xuống "App passwords"
4. Tạo app password mới với:
   - Select app: Mail
   - Select device: Other (Custom name) -> "Pharmacy Warehouse"
5. Copy password được tạo (16 ký tự)

### Bước 3: Cập nhật cấu hình
- `spring.mail.username`: email@gmail.com của bạn
- `spring.mail.password`: app password vừa tạo

## 3. Các API Endpoint đã được tạo

### 1. Tạo user (có gửi email)
```
POST /users
Content-Type: application/json
Authorization: Bearer {admin_token}

{
  "username": "nguyenvana",
  "fullName": "Nguyễn Văn A",
  "email": "nguyenvana@example.com",
  "status": "ACTIVE",
  "roleId": 2
}
```

### 2. Kiểm tra yêu cầu đổi mật khẩu
```
GET /users/check-force-change-password
Authorization: Bearer {user_token}

Response:
{
  "forceChangePassword": true
}
```

### 3. Đổi mật khẩu bắt buộc (lần đầu đăng nhập)
```
POST /users/force-change-password
Content-Type: application/json
Authorization: Bearer {user_token}

{
  "newPassword": "newpassword123",
  "confirmPassword": "newpassword123"
}
```

### 4. Đổi mật khẩu thông thường
```
POST /users/change-password
Content-Type: application/json
Authorization: Bearer {user_token}

{
  "currentPassword": "oldpassword",
  "newPassword": "newpassword123",
  "confirmPassword": "newpassword123"
}
```

## 4. Quy trình hoạt động

1. **Admin tạo tài khoản mới**:
   - Tự động tạo mật khẩu mặc định: `12345@`
   - Tự động đặt `forceChangePassword = true`
   - Gửi email thông báo kèm thông tin đăng nhập

2. **User nhận email**:
   - Email chứa thông tin tài khoản và mật khẩu tạm thời
   - Link đăng nhập đến frontend

3. **User đăng nhập lần đầu**:
   - Frontend gọi `/users/check-force-change-password`
   - Nếu trả về `true`, hiển thị form đổi mật khẩu bắt buộc

4. **User đổi mật khẩu**:
   - Gọi `/users/force-change-password`
   - Hệ thống cập nhật mật khẩu mới và đặt `forceChangePassword = false`

## 5. Template Email

Email được gửi với template HTML đẹp mắt chứa:
- Thông tin chào mừng
- Tên đăng nhập và mật khẩu tạm thời
- Link đăng nhập
- Hướng dẫn chi tiết
- Lưu ý về bảo mật

Template nằm tại: `src/main/resources/templates/account-creation-email.html`

## 6. Lưu ý quan trọng

- Mật khẩu mặc định: `12345@`
- User bắt buộc phải đổi mật khẩu trong lần đăng nhập đầu tiên
- Email được gửi bất đồng bộ, không ảnh hưởng đến việc tạo user
- Nếu gửi email lỗi, user vẫn được tạo thành công
- Log lỗi được ghi lại để debug

## 7. Testing

Để test chức năng email:
1. Cấu hình email trong application.properties
2. Tạo một user mới qua API
3. Kiểm tra email nhận được
4. Đăng nhập với tài khoản đó và test đổi mật khẩu

## 8. Customization

- Thay đổi mật khẩu mặc định: Sửa `DEFAULT_PASSWORD` trong `UserService`
- Custom template email: Sửa file `account-creation-email.html`
- Thay đổi frontend URL: Sửa `loginUrl` trong `EmailService.sendAccountCreationEmail()`
