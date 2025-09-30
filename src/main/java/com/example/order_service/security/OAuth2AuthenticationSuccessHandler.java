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

        CustomUserDetailsService.UserPrincipal userPrincipal = (CustomUserDetailsService.UserPrincipal) authentication.getPrincipal();
        log.info("OAuth2 authentication successful for user: {}, role: {}, provider: {}",
                userPrincipal.getEmail(),
                userPrincipal.getUser().getRole(),
                userPrincipal.getUser().getAuthProvider());

        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            log.warn("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        log.info("OAuth2 login successful. Redirecting to: {}", targetUrl);
        clearAuthenticationAttributes(request);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                       Authentication authentication) {

        CustomUserDetailsService.UserPrincipal userPrincipal = (CustomUserDetailsService.UserPrincipal) authentication.getPrincipal();

        // JWT 토큰 생성
        String token = tokenProvider.generateAccessToken(authentication);
        log.info("Generated JWT token for OAuth2 user: email={}, provider={}, role={}",
                userPrincipal.getUser().getEmail(),
                userPrincipal.getUser().getAuthProvider(),
                userPrincipal.getUser().getRole());

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", token)
                .build().toUriString();

        log.debug("Target URL with token: {}", targetUrl);
        return targetUrl;
    }

    protected boolean isAuthorizedRedirectUri(String uri) {
        URI clientRedirectUri = URI.create(uri);

        return URI.create(redirectUri)
                .getHost().equals(clientRedirectUri.getHost())
                && URI.create(redirectUri)
                .getPort() == clientRedirectUri.getPort();
    }
}