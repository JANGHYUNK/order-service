package com.example.order_service.security;

import com.example.order_service.entity.User;
import com.example.order_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (Exception ex) {
            OAuth2Error oauth2Error = new OAuth2Error("invalid_user_info_response", ex.getMessage(), null);
            throw new OAuth2AuthenticationException(oauth2Error, ex);
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                oAuth2UserRequest.getClientRegistration().getRegistrationId(),
                oAuth2User.getAttributes()
        );

        String email = oAuth2UserInfo.getEmail();
        if (!StringUtils.hasText(email)) {
            if ("kakao".equals(oAuth2UserRequest.getClientRegistration().getRegistrationId())) {
                email = oAuth2UserInfo.getId() + "@kakao.local";
                log.warn("Email not provided by Kakao, using generated email: {}", email);
            } else {
                throw new RuntimeException("Email not found from OAuth2 provider");
            }
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (!user.getAuthProvider().equals(User.AuthProvider.valueOf(
                    oAuth2UserRequest.getClientRegistration().getRegistrationId().toUpperCase()))) {
                throw new RuntimeException("Looks like you're signed up with " +
                        user.getAuthProvider() + " account. Please use your " + user.getAuthProvider() +
                        " account to login.");
            }
            user = updateExistingUser(user, oAuth2UserInfo);
        } else {
            // 신규 사용자: 회원가입이 필요한 상태로 DB에 저장
            user = createPendingUser(oAuth2UserRequest, oAuth2UserInfo, email);
        }

        return CustomUserDetailsService.UserPrincipal.create(user, oAuth2User.getAttributes());
    }

    private User createPendingUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo, String email) {
        // 회원가입이 필요한 사용자를 DB에 저장 (isEnabled=false로 설정)
        User user = User.builder()
                .name(oAuth2UserInfo.getName())
                .email(email)
                .profileImage(oAuth2UserInfo.getImageUrl())
                .authProvider(User.AuthProvider.valueOf(
                        oAuth2UserRequest.getClientRegistration().getRegistrationId().toUpperCase()))
                .providerId(oAuth2UserInfo.getId())
                .role(User.Role.USER)
                .isEnabled(false) // 회원가입 미완료 상태
                .build();

        log.info("Creating pending user for OAuth2 signup: email={}, provider={}",
                email, oAuth2UserRequest.getClientRegistration().getRegistrationId());

        return userRepository.save(user);
    }

    private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo, String email) {
        User user = User.builder()
                .name(oAuth2UserInfo.getName())
                .email(email)
                .profileImage(oAuth2UserInfo.getImageUrl())
                .authProvider(User.AuthProvider.valueOf(
                        oAuth2UserRequest.getClientRegistration().getRegistrationId().toUpperCase()))
                .providerId(oAuth2UserInfo.getId())
                .role(User.Role.USER)
                .isEnabled(true)
                .build();

        return userRepository.save(user);
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo) {
        existingUser.setName(oAuth2UserInfo.getName());
        existingUser.setProfileImage(oAuth2UserInfo.getImageUrl());

        return userRepository.save(existingUser);
    }
}