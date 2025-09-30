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
        log.info("Processing OAuth2 user from provider: {}", oAuth2UserRequest.getClientRegistration().getRegistrationId());
        log.debug("OAuth2 user attributes: {}", oAuth2User.getAttributes());

        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                oAuth2UserRequest.getClientRegistration().getRegistrationId(),
                oAuth2User.getAttributes()
        );

        String email = oAuth2UserInfo.getEmail();
        log.info("OAuth2 user email: {}, name: {}, id: {}", email, oAuth2UserInfo.getName(), oAuth2UserInfo.getId());

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
            log.info("Existing user found: id={}, email={}, username={}", user.getId(), user.getEmail(), user.getUsername());

            if (!user.getAuthProvider().equals(User.AuthProvider.valueOf(
                    oAuth2UserRequest.getClientRegistration().getRegistrationId().toUpperCase()))) {
                throw new RuntimeException("Looks like you're signed up with " +
                        user.getAuthProvider() + " account. Please use your " + user.getAuthProvider() +
                        " account to login.");
            }
            user = updateExistingUser(user, oAuth2UserInfo);
        } else {
            // 신규 사용자: 즉시 활성화된 상태로 회원가입 완료
            user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo, email);
            log.info("New user registered: id={}, email={}, username={}", user.getId(), user.getEmail(), user.getUsername());
        }

        CustomUserDetailsService.UserPrincipal principal = CustomUserDetailsService.UserPrincipal.create(user, oAuth2User.getAttributes());
        log.info("Created UserPrincipal: name={}, email={}, username={}", principal.getName(), principal.getEmail(), principal.getUsername());

        return principal;
    }


    private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo, String email) {
        String name = oAuth2UserInfo.getName();

        // username 생성: 이메일의 @ 앞부분 사용
        String baseUsername = email.split("@")[0];
        String username = baseUsername;
        int counter = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }

        // nickname 생성: 이름 사용, 중복이면 숫자 추가
        String baseNickname = name != null && !name.isEmpty() ? name : username;
        String nickname = baseNickname;
        counter = 1;
        while (userRepository.existsByNickname(nickname)) {
            nickname = baseNickname + counter;
            counter++;
        }

        User user = User.builder()
                .username(username)
                .name(name)
                .nickname(nickname)
                .email(email)
                .profileImage(oAuth2UserInfo.getImageUrl())
                .authProvider(User.AuthProvider.valueOf(
                        oAuth2UserRequest.getClientRegistration().getRegistrationId().toUpperCase()))
                .providerId(oAuth2UserInfo.getId())
                .role(User.Role.USER)
                .isEnabled(true) // 즉시 활성화
                .emailVerified(true) // OAuth2 users are pre-verified
                .build();

        log.info("Registering new OAuth2 user: email={}, username={}, nickname={}, provider={}",
                email, username, nickname, oAuth2UserRequest.getClientRegistration().getRegistrationId());

        return userRepository.save(user);
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo) {
        existingUser.setName(oAuth2UserInfo.getName());
        existingUser.setProfileImage(oAuth2UserInfo.getImageUrl());

        return userRepository.save(existingUser);
    }
}