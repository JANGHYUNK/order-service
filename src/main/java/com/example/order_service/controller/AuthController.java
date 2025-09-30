package com.example.order_service.controller;

import com.example.order_service.dto.AuthResponse;
import com.example.order_service.dto.LoginRequest;
import com.example.order_service.dto.OAuth2SignupRequest;
import com.example.order_service.dto.SignUpRequest;
import com.example.order_service.dto.UserInfoResponse;
import com.example.order_service.entity.User;
import com.example.order_service.security.CustomUserDetailsService;
import com.example.order_service.service.AuthService;
import com.example.order_service.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;
    private final CustomUserDetailsService userDetailsService;

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
    public ResponseEntity<?> getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getName() == null) {
                return ResponseEntity.status(401).body(Map.of("message", "인증되지 않은 사용자입니다."));
            }

            String identifier = auth.getName();
            User user;

            // 이메일인지 사용자명인지 확인하여 사용자 조회
            if (identifier.contains("@")) {
                user = userDetailsService.findByEmail(identifier)
                        .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            } else {
                user = userDetailsService.findByUsername(identifier)
                        .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            }

            UserInfoResponse userInfo = UserInfoResponse.from(user);
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "사용자 정보를 조회할 수 없습니다."));
        }
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
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "서버 오류가 발생했습니다."));
        }
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<?> checkNickname(@RequestParam("nickname") String nickname) {
        try {
            if (nickname == null || nickname.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "닉네임이 필요합니다."));
            }

            boolean exists = authService.isNicknameExists(nickname);
            return ResponseEntity.ok(Map.of(
                    "available", !exists,
                    "message", exists ? "이미 사용 중인 닉네임입니다." : "사용 가능한 닉네임입니다."
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}