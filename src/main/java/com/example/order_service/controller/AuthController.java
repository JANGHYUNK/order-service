package com.example.order_service.controller;

import com.example.order_service.dto.AuthResponse;
import com.example.order_service.dto.LoginRequest;
import com.example.order_service.dto.OAuth2SignupRequest;
import com.example.order_service.dto.SignUpRequest;
import com.example.order_service.service.AuthService;
import com.example.order_service.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@Valid @RequestBody SignUpRequest signUpRequest) {
        try {
            AuthResponse response = authService.signUp(signUpRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            AuthResponse response = authService.login(loginRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            AuthResponse response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<String> getCurrentUser() {
        return ResponseEntity.ok("현재 인증된 사용자 정보를 반환합니다.");
    }

    @PostMapping("/oauth2/complete")
    public ResponseEntity<AuthResponse> completeOAuth2Signup(@Valid @RequestBody OAuth2SignupRequest oauth2SignupRequest) {
        try {
            AuthResponse response = authService.completeOAuth2Signup(oauth2SignupRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {
        try {
            AuthResponse response = authService.verifyEmail(token);
            // Redirect to success page with tokens in URL (for frontend to handle)
            return ResponseEntity.status(302)
                    .header("Location", "/oauth2-success.html?verified=true&token=" + response.getAccessToken())
                    .build();
        } catch (RuntimeException e) {
            // Redirect to error page
            return ResponseEntity.status(302)
                    .header("Location", "/signup.html?error=" + e.getMessage())
                    .build();
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerificationEmail(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "이메일이 필요합니다."));
            }

            authService.resendVerificationEmail(email);
            return ResponseEntity.ok(Map.of("message", "인증 이메일이 재전송되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/send-verification-code")
    public ResponseEntity<?> sendVerificationCode(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "이메일이 필요합니다."));
            }

            // 이메일 중복 검사
            if (authService.isEmailExists(email)) {
                return ResponseEntity.badRequest().body(Map.of("message", "이미 사용 중인 이메일입니다."));
            }

            String verificationCode = emailService.sendVerificationCode(email);
            return ResponseEntity.ok(Map.of(
                    "message", "인증번호가 발송되었습니다.",
                    "email", email
                    // 실제 서비스에서는 verificationCode를 반환하지 않습니다.
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String code = request.get("code");

            if (email == null || email.isEmpty() || code == null || code.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "이메일과 인증번호가 필요합니다."));
            }

            boolean isValid = emailService.verifyCode(email, code);
            if (isValid) {
                return ResponseEntity.ok(Map.of("message", "이메일 인증이 완료되었습니다.", "verified", true));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "잘못된 인증번호이거나 만료된 인증번호입니다.", "verified", false));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage(), "verified", false));
        }
    }

    @GetMapping("/check-username")
    public ResponseEntity<?> checkUsername(@RequestParam("username") String username) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "아이디가 필요합니다."));
            }

            boolean exists = authService.isUsernameExists(username);
            return ResponseEntity.ok(Map.of(
                    "available", !exists,
                    "message", exists ? "이미 사용 중인 아이디입니다." : "사용 가능한 아이디입니다."
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}