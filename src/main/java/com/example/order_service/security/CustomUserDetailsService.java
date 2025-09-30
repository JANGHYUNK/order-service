package com.example.order_service.security;

import com.example.order_service.entity.User;
import com.example.order_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
            return String.valueOf(id);
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }
    }
}