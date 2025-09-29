package com.example.order_service.service;

import com.example.order_service.entity.EmailVerification;
import com.example.order_service.entity.User;
import com.example.order_service.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailVerificationRepository emailVerificationRepository;

    @Value("${app.mail.verification.expiration-hours:24}")
    private int verificationExpirationHours;

    @Value("${app.mail.verification.code-expiration-minutes:10}")
    private int codeExpirationMinutes;

    @Value("${app.mail.from:noreply@orderservice.com}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public void sendVerificationEmail(User user) {
        try {
            // 기존 미사용 토큰 삭제
            emailVerificationRepository.deleteByEmail(user.getEmail());

            // 새 인증 토큰 생성
            String token = UUID.randomUUID().toString();
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(verificationExpirationHours);

            EmailVerification verification = EmailVerification.builder()
                    .email(user.getEmail())
                    .token(token)
                    .expiresAt(expiresAt)
                    .build();

            emailVerificationRepository.save(verification);

            // 이메일 전송
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("이메일 인증을 완료해주세요 - Order Service");
            message.setText(buildVerificationEmailContent(user.getName(), token));

            mailSender.send(message);

            log.info("Verification email sent to: {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", user.getEmail(), e);
            throw new RuntimeException("이메일 전송에 실패했습니다.", e);
        }
    }

    public void cleanupExpiredTokens() {
        try {
            emailVerificationRepository.deleteExpiredTokens(LocalDateTime.now());
            log.debug("Expired email verification tokens cleaned up");
        } catch (Exception e) {
            log.error("Failed to cleanup expired tokens", e);
        }
    }

    public String sendVerificationCode(String email) {
        try {
            // 기존 미사용 인증번호 삭제
            emailVerificationRepository.deleteByEmail(email);

            // 6자리 인증번호 생성
            String verificationCode = generateSixDigitCode();
            String token = UUID.randomUUID().toString(); // 토큰도 함께 생성
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(codeExpirationMinutes);

            EmailVerification verification = EmailVerification.builder()
                    .email(email)
                    .token(token)
                    .verificationCode(verificationCode)
                    .expiresAt(expiresAt)
                    .build();

            emailVerificationRepository.save(verification);

            // 인증번호 이메일 전송
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("이메일 인증번호 - Order Service");
            message.setText(buildVerificationCodeEmailContent(verificationCode));

            mailSender.send(message);

            log.info("Verification code sent to: {}", email);
            return verificationCode; // 테스트용으로 반환 (실제 서비스에서는 반환하지 않음)

        } catch (Exception e) {
            log.error("Failed to send verification code to: {}", email, e);
            throw new RuntimeException("인증번호 전송에 실패했습니다.", e);
        }
    }

    public boolean verifyCode(String email, String code) {
        Optional<EmailVerification> verificationOpt =
                emailVerificationRepository.findByEmailAndVerificationCodeAndIsUsedFalse(email, code);

        if (verificationOpt.isEmpty()) {
            log.warn("Verification code not found or already used: email={}, code={}", email, code);
            return false;
        }

        EmailVerification verification = verificationOpt.get();

        if (verification.isExpired()) {
            log.warn("Verification code expired: email={}, code={}", email, code);
            return false;
        }

        // 인증번호 확인만 하고 사용됨으로 표시하지 않음 (회원가입 시에 표시)
        verification.setVerifiedAt(LocalDateTime.now());
        emailVerificationRepository.save(verification);

        log.info("Email verification code verified for: {}", email);
        return true;
    }

    public boolean isCodeVerified(String email, String code) {
        Optional<EmailVerification> verificationOpt =
                emailVerificationRepository.findByEmailAndVerificationCodeAndIsUsedFalse(email, code);

        if (verificationOpt.isEmpty()) {
            return false;
        }

        EmailVerification verification = verificationOpt.get();
        return verification.getVerifiedAt() != null && !verification.isExpired();
    }

    private String generateSixDigitCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    private String buildVerificationCodeEmailContent(String verificationCode) {
        return String.format(
                "안녕하세요!\n\n" +
                        "Order Service 이메일 인증번호입니다.\n\n" +
                        "인증번호: %s\n\n" +
                        "이 인증번호는 %d분 후에 만료됩니다.\n\n" +
                        "만약 본인이 요청하지 않으셨다면 이 이메일을 무시해주세요.\n\n" +
                        "감사합니다.\n" +
                        "Order Service 팀",
                verificationCode, codeExpirationMinutes
        );
    }

    private String buildVerificationEmailContent(String userName, String token) {
        String verificationUrl = baseUrl + "/api/auth/verify-email?token=" + token;

        return String.format(
                "안녕하세요 %s님,\n\n" +
                        "Order Service에 가입해주셔서 감사합니다!\n\n" +
                        "아래 링크를 클릭하여 이메일 인증을 완료해주세요:\n" +
                        "%s\n\n" +
                        "이 링크는 %d시간 후에 만료됩니다.\n\n" +
                        "만약 본인이 회원가입을 하지 않으셨다면 이 이메일을 무시해주세요.\n\n" +
                        "감사합니다.\n" +
                        "Order Service 팀",
                userName, verificationUrl, verificationExpirationHours
        );
    }
}