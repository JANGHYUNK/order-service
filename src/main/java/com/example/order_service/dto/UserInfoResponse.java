package com.example.order_service.dto;

import com.example.order_service.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoResponse {
    private Long id;
    private String email;
    private String username;
    private String name;
    private String nickname;
    private String role;
    private String profileImage;
    private boolean emailVerified;
    private String authProvider;

    public static UserInfoResponse from(User user) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .name(user.getName())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .profileImage(user.getProfileImage())
                .emailVerified(user.getEmailVerified())
                .authProvider(user.getAuthProvider().name())
                .build();
    }
}