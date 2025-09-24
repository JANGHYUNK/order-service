package com.example.order_service.security;

import com.example.order_service.entity.User;

import java.util.Map;

public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase(User.AuthProvider.GOOGLE.toString())) {
            return new GoogleOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase(User.AuthProvider.KAKAO.toString())) {
            return new KakaoOAuth2UserInfo(attributes);
        } else {
            throw new RuntimeException("Sorry! Login with " + registrationId + " is not supported yet.");
        }
    }
}