package com.example.order_service.security;

import com.example.order_service.util.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;

    @Value("${app.oauth2.authorizedRedirectUris:http://localhost:8080/oauth2-success.html}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {

        log.info("OAuth2 authentication successful for user: {}",
                ((CustomUserDetailsService.UserPrincipal) authentication.getPrincipal()).getEmail());

        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            log.warn("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        log.info("Redirecting to: {}", targetUrl);
        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                       Authentication authentication) {

        CustomUserDetailsService.UserPrincipal userPrincipal = (CustomUserDetailsService.UserPrincipal) authentication.getPrincipal();

        // 신규 사용자 체크 (isEnabled가 false이면 회원가입이 필요)
        if (!userPrincipal.getUser().getIsEnabled()) {
            log.info("New user needs signup completion: email={}, provider={}",
                    userPrincipal.getUser().getEmail(), userPrincipal.getUser().getAuthProvider());

            // 회원가입 페이지로 리다이렉트 (사용자 정보를 URL 파라미터로 전달)
            return UriComponentsBuilder.fromUriString("http://localhost:8080/oauth2-signup.html")
                    .queryParam("email", userPrincipal.getUser().getEmail())
                    .queryParam("name", userPrincipal.getUser().getName())
                    .queryParam("provider", userPrincipal.getUser().getAuthProvider().name())
                    .queryParam("providerId", userPrincipal.getUser().getProviderId())
                    .queryParam("profileImage", userPrincipal.getUser().getProfileImage())
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();
        }

        // 기존 사용자는 JWT 토큰과 함께 로그인 처리
        log.info("Existing user login successful: email={}, provider={}",
                userPrincipal.getUser().getEmail(), userPrincipal.getUser().getAuthProvider());

        String token = tokenProvider.generateAccessToken(authentication);

        return UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", token)
                .build().toUriString();
    }

    protected boolean isAuthorizedRedirectUri(String uri) {
        URI clientRedirectUri = URI.create(uri);

        return URI.create(redirectUri)
                .getHost().equals(clientRedirectUri.getHost())
                && URI.create(redirectUri)
                .getPort() == clientRedirectUri.getPort();
    }
}