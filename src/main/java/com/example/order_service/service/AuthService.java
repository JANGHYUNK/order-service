package com.example.order_service.service;

import com.example.order_service.dto.AuthResponse;
import com.example.order_service.dto.LoginRequest;
import com.example.order_service.dto.OAuth2SignupRequest;
import com.example.order_service.dto.SignUpRequest;
import com.example.order_service.entity.EmailVerification;
import com.example.order_service.entity.User;
import com.example.order_service.repository.EmailVerificationRepository;
import com.example.order_service.repository.UserRepository;
import com.example.order_service.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    public AuthResponse signUp(SignUpRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }

        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new RuntimeException("이미 존재하는 아이디입니다.");
        }

        // 인증번호가 사전에 확인되었는지 검증
        if (!emailService.isCodeVerified(signUpRequest.getEmail(), signUpRequest.getVerificationCode())) {
            throw new RuntimeException("유효하지 않거나 확인되지 않은 인증번호입니다.");
        }

        User user = User.builder()
                .email(signUpRequest.getEmail())
                .username(signUpRequest.getUsername())
                .password(passwordEncoder.encode(signUpRequest.getPassword()))
                .name(signUpRequest.getName())
                .role(signUpRequest.getRole() != null ? signUpRequest.getRole() : User.Role.USER)
                .authProvider(User.AuthProvider.LOCAL)
                .isEnabled(true) // User is already email verified via code
                .emailVerified(true) // Email verified via 6-digit code
                .emailVerifiedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        // Mark verification as used
        EmailVerification verification = emailVerificationRepository
                .findByEmailAndVerificationCodeAndIsUsedFalse(savedUser.getEmail(), signUpRequest.getVerificationCode())
                .orElse(null);

        if (verification != null) {
            verification.setIsUsed(true);
            emailVerificationRepository.save(verification);
        }

        // Generate tokens for immediate login
        String accessToken = jwtTokenProvider.generateAccessToken(savedUser.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(AuthResponse.UserInfo.builder()
                        .id(savedUser.getId())
                        .email(savedUser.getEmail())
                        .name(savedUser.getName())
                        .profileImage(savedUser.getProfileImage())
                        .role(savedUser.getRole().name())
                        .build())
                .message("회원가입이 완료되었습니다. 환영합니다!")
                .build();
    }

    public AuthResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // Check if user account is enabled and email is verified
        if (!user.getIsEnabled() || !user.getEmailVerified()) {
            throw new RuntimeException("이메일 인증이 필요합니다. 이메일을 확인해주세요.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .profileImage(user.getProfileImage())
                        .role(user.getRole().name())
                        .build())
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("유효하지 않은 리프레시 토큰입니다.");
        }

        String email = jwtTokenProvider.getUserEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        String newAccessToken = jwtTokenProvider.generateAccessToken(email);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(email);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .profileImage(user.getProfileImage())
                        .role(user.getRole().name())
                        .build())
                .build();
    }

    public AuthResponse completeOAuth2Signup(OAuth2SignupRequest oauth2SignupRequest) {
        // OAuth2로 생성된 미완료 사용자를 찾음
        User user = userRepository.findByEmail(oauth2SignupRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("OAuth2 사용자를 찾을 수 없습니다."));

        // 이미 완료된 사용자인지 확인
        if (user.getIsEnabled()) {
            throw new RuntimeException("이미 회원가입이 완료된 사용자입니다.");
        }

        // OAuth2 사용자가 아닌 경우 체크
        if (user.getAuthProvider() == User.AuthProvider.LOCAL) {
            throw new RuntimeException("일반 회원가입 사용자입니다.");
        }

        // 사용자 정보 업데이트
        user.setName(oauth2SignupRequest.getName());
        user.setProfileImage(oauth2SignupRequest.getProfileImage());
        user.setIsEnabled(true); // 회원가입 완료

        log.info("Completing OAuth2 signup for user: email={}, provider={}",
                user.getEmail(), user.getAuthProvider());

        User savedUser = userRepository.save(user);

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(savedUser.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(AuthResponse.UserInfo.builder()
                        .id(savedUser.getId())
                        .email(savedUser.getEmail())
                        .name(savedUser.getName())
                        .profileImage(savedUser.getProfileImage())
                        .role(savedUser.getRole().name())
                        .build())
                .build();
    }

    public AuthResponse verifyEmail(String token) {
        EmailVerification verification = emailVerificationRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 인증 토큰입니다."));

        if (verification.isExpired()) {
            throw new RuntimeException("인증 토큰이 만료되었습니다. 새로운 인증 이메일을 요청해주세요.");
        }

        if (verification.getIsUsed()) {
            throw new RuntimeException("이미 사용된 인증 토큰입니다.");
        }

        User user = userRepository.findByEmail(verification.getEmail())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // Mark user as verified
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setIsEnabled(true);
        userRepository.save(user);

        // Mark token as used
        verification.setIsUsed(true);
        verification.setVerifiedAt(LocalDateTime.now());
        emailVerificationRepository.save(verification);

        // Generate tokens for immediate login
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        log.info("Email verified successfully for user: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .profileImage(user.getProfileImage())
                        .role(user.getRole().name())
                        .build())
                .message("이메일 인증이 완료되었습니다. 환영합니다!")
                .build();
    }

    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (user.getEmailVerified()) {
            throw new RuntimeException("이미 인증된 계정입니다.");
        }

        emailService.sendVerificationEmail(user);
        log.info("Verification email resent to: {}", email);
    }

    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean isUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }
}