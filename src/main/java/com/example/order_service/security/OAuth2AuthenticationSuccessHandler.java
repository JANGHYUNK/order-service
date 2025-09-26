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

        // 모든 OAuth2 사용자에 대해 JWT 토큰 생성 및 로그인 처리
        log.info("OAuth2 user login successful: email={}, provider={}",
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