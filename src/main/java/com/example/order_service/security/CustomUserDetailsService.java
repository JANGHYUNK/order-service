package com.example.order_service.security;

import com.example.order_service.entity.User;
import com.example.order_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return UserPrincipal.create(user);
    }

    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        return UserPrincipal.create(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public static class UserPrincipal implements UserDetails, org.springframework.security.oauth2.core.user.OAuth2User {
        private Long id;
        private String email;
        private String username;
        private String password;
        private Collection<? extends GrantedAuthority> authorities;
        private Map<String, Object> attributes;
        private User user; // User 엔티티 추가

        public UserPrincipal(Long id, String email, String username, String password, Collection<? extends GrantedAuthority> authorities, User user) {
            this.id = id;
            this.email = email;
            this.username = username;
            this.password = password;
            this.authorities = authorities;
            this.user = user;
        }

        public static UserPrincipal create(User user) {
            Collection<GrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
            );

            return new UserPrincipal(
                    user.getId(),
                    user.getEmail(),
                    user.getUsername(),
                    user.getPassword(),
                    authorities,
                    user
            );
        }

        public static UserPrincipal create(User user, Map<String, Object> attributes) {
            UserPrincipal userPrincipal = UserPrincipal.create(user);
            userPrincipal.setAttributes(attributes);

            log.debug("UserPrincipal.create: id={}, email={}, username={}, getName()={}",
                    userPrincipal.getId(), userPrincipal.getEmail(), userPrincipal.getUsername(), userPrincipal.getName());

            return userPrincipal;
        }

        public void setAttributes(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        public Long getId() {
            return id;
        }

        public String getEmail() {
            return email;
        }

        public User getUser() {
            return user;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public String getName() {
            // OAuth2AuthorizedClient는 principalName이 비어있으면 안 됨
            // email이 있으면 email을 사용하고, 없으면 username을 사용
            String name = null;

            if (email != null && !email.isEmpty()) {
                name = email;
            } else if (username != null && !username.isEmpty()) {
                name = username;
            } else if (id != null) {
                name = String.valueOf(id);
            } else {
                // 모든 값이 null인 경우 기본값
                name = "unknown";
            }

            // 빈 문자열이 아닌지 확인
            if (name == null || name.isEmpty() || name.equals("null")) {
                return "unknown";
            }

            return name;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }
    }
}