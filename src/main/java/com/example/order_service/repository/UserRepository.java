package com.example.order_service.repository;

import com.example.order_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByNickname(String nickname);

    Optional<User> findByAuthProviderAndProviderId(User.AuthProvider authProvider, String providerId);

    List<User> findByRole(User.Role role);

    Long countByRole(User.Role role);
}