package com.example.order_service.controller;

import com.example.order_service.dto.UserInfoResponse;
import com.example.order_service.entity.User;
import com.example.order_service.repository.UserRepository;
import com.example.order_service.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserRepository userRepository;
    private final CustomUserDetailsService userDetailsService;

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> updates) {
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

            // 이름 업데이트
            if (updates.containsKey("name") && updates.get("name") != null) {
                String newName = updates.get("name").trim();
                if (!newName.isEmpty()) {
                    user.setName(newName);
                }
            }

            // 닉네임 업데이트
            if (updates.containsKey("nickname") && updates.get("nickname") != null) {
                String newNickname = updates.get("nickname").trim();
                if (!newNickname.isEmpty()) {
                    // 닉네임 중복 체크 (본인 제외)
                    boolean nicknameExists = userRepository.findAll().stream()
                            .anyMatch(u -> !u.getId().equals(user.getId()) &&
                                    newNickname.equals(u.getNickname()));
                    if (nicknameExists) {
                        return ResponseEntity.badRequest().body(Map.of("message", "이미 사용 중인 닉네임입니다."));
                    }
                    user.setNickname(newNickname);
                }
            }

            // 이메일 업데이트
            if (updates.containsKey("email")) {
                String newEmail = updates.get("email");
                if (newEmail != null && !newEmail.trim().isEmpty()) {
                    // 이메일 중복 체크 (본인 제외)
                    boolean emailExists = userRepository.findAll().stream()
                            .anyMatch(u -> !u.getId().equals(user.getId()) &&
                                    newEmail.equals(u.getEmail()));
                    if (emailExists) {
                        return ResponseEntity.badRequest().body(Map.of("message", "이미 사용 중인 이메일입니다."));
                    }
                    user.setEmail(newEmail);
                    user.setEmailVerified(false); // 이메일 변경 시 재인증 필요
                }
            }

            User updatedUser = userRepository.save(user);
            log.info("User profile updated: id={}, name={}, nickname={}", updatedUser.getId(), updatedUser.getName(), updatedUser.getNickname());

            UserInfoResponse response = UserInfoResponse.from(updatedUser);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Profile update failed", e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Profile update error", e);
            return ResponseEntity.status(500).body(Map.of("message", "서버 오류가 발생했습니다."));
        }
    }
}