package com.example.order_service.controller;

import com.example.order_service.entity.User;
import com.example.order_service.repository.UserRepository;
import com.example.order_service.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/oauth2")
public class OAuth2SignupController {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/signup/complete")
    public ResponseEntity<?> completeOAuth2Signup(@RequestBody Map<String, String> signupData) {
        try {
            String email = signupData.get("email");
            String name = signupData.get("name");
            String provider = signupData.get("provider");
            String providerId = signupData.get("providerId");
            String profileImage = signupData.get("profileImage");

            // 임시 사용자 정보로 실제 사용자 생성
            User user = User.builder()
                    .name(name)
                    .email(email)
                    .profileImage(profileImage)
                    .authProvider(User.AuthProvider.valueOf(provider.toUpperCase()))
                    .providerId(providerId)
                    .role(User.Role.USER)
                    .isEnabled(true) // 회원가입 완료
                    .build();

            User savedUser = userRepository.save(user);

            // JWT 토큰 생성
            String token = jwtTokenProvider.generateAccessToken(savedUser.getEmail());

            return ResponseEntity.ok().body(Map.of(
                "message", "회원가입이 완료되었습니다.",
                "token", token,
                "user", Map.of(
                    "id", savedUser.getId(),
                    "name", savedUser.getName(),
                    "email", savedUser.getEmail(),
                    "profileImage", savedUser.getProfileImage() != null ? savedUser.getProfileImage() : ""
                )
            ));

        } catch (Exception e) {
            log.error("OAuth2 signup completion failed", e);
            return ResponseEntity.badRequest().body(Map.of("error", "회원가입 처리 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/check-user")
    public ResponseEntity<?> checkUserExists(@RequestParam String email) {
        Optional<User> user = userRepository.findByEmail(email);
        return ResponseEntity.ok().body(Map.of("exists", user.isPresent()));
    }
}