package cn.heycloudream.ishua_backend.service.email;

public interface RegisterEmailSender {

    void sendVerificationCode(String email, String code);
}
