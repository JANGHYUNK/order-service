package com.example.order_service.security;

import java.util.Map;

public class KakaoOAuth2UserInfo extends OAuth2UserInfo {

    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getName() {
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
        if (properties == null) {
            return null;
        }
        return (String) properties.get("nickname");
    }

    @Override
    public String getEmail() {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        if (kakaoAccount == null) {
            return null;
        }

        Boolean emailValid = (Boolean) kakaoAccount.get("email_valid");
        Boolean isEmailVerified = (Boolean) kakaoAccount.get("is_email_verified");

        if (emailValid != null && emailValid && isEmailVerified != null && isEmailVerified) {
            return (String) kakaoAccount.get("email");
        }

        return null;
    }

    @Override
    public String getImageUrl() {
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
        if (properties == null) {
            return null;
        }

        String profileImage = (String) properties.get("profile_image");
        if (profileImage == null) {
            profileImage = (String) properties.get("thumbnail_image");
        }

        return profileImage;
    }
}