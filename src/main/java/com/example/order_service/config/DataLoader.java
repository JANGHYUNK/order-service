package com.example.order_service.config;

import com.example.order_service.entity.Product;
import com.example.order_service.entity.User;
import com.example.order_service.repository.ProductRepository;
import com.example.order_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        updateExistingUsersWithNickname();
        createDefaultAdminUser();
        createSampleProducts();
    }

    private void updateExistingUsersWithNickname() {
        // 닉네임이 없는 기존 사용자들에게 닉네임 추가
        userRepository.findAll().forEach(user -> {
            if (user.getNickname() == null || user.getNickname().isEmpty()) {
                // 이름을 닉네임으로 사용, 중복이면 숫자 추가
                String baseNickname = user.getName() != null ? user.getName() : user.getUsername();
                String nickname = baseNickname;
                int counter = 1;

                while (userRepository.existsByNickname(nickname)) {
                    nickname = baseNickname + counter;
                    counter++;
                }

                user.setNickname(nickname);
                userRepository.save(user);
                log.info("Updated user {} with nickname: {}", user.getUsername(), nickname);
            }
        });
    }

    private void createDefaultAdminUser() {
        String adminUsername = "admin";
        String adminEmail = "admin@example.com";

        if (!userRepository.existsByUsername(adminUsername) &&
            !userRepository.existsByEmail(adminEmail)) {

            User adminUser = User.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .password(passwordEncoder.encode("admin"))
                    .name("관리자")
                    .nickname("관리자")
                    .role(User.Role.ADMIN)
                    .authProvider(User.AuthProvider.LOCAL)
                    .isEnabled(true)
                    .emailVerified(true)
                    .emailVerifiedAt(java.time.LocalDateTime.now())
                    .build();

            userRepository.save(adminUser);
            log.info("Default admin user created: username='admin', password='admin'");
        } else {
            log.info("Admin user already exists, skipping creation");
        }
    }

    private void createSampleProducts() {
        if (productRepository.count() > 0) {
            log.info("Sample products already exist, skipping creation");
            return;
        }

        // Get the admin user as the seller for sample products
        User adminUser = userRepository.findByUsername("admin").orElse(null);
        if (adminUser == null) {
            log.warn("Admin user not found, cannot create sample products");
            return;
        }

        Product[] sampleProducts = {
            Product.builder()
                .name("스마트폰 케이스")
                .description("고품질 실리콘 소재의 스마트폰 보호 케이스입니다. 충격 흡수 기능이 뛰어나며 다양한 색상으로 제공됩니다.")
                .price(new BigDecimal("15000"))
                .stockQuantity(100)
                .category("전자제품")
                .imageUrl("https://images.unsplash.com/photo-1512499617640-c2f1d0e3e4d9?w=300&h=300&fit=crop")
                .seller(adminUser)
                .status(Product.ProductStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build(),

            Product.builder()
                .name("무선 이어폰")
                .description("최신 블루투스 5.0 기술을 적용한 완전 무선 이어폰입니다. 노이즈 캔슬링 기능과 30시간 재생이 가능합니다.")
                .price(new BigDecimal("89000"))
                .stockQuantity(50)
                .category("전자제품")
                .imageUrl("https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=300&h=300&fit=crop")
                .seller(adminUser)
                .status(Product.ProductStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build(),

            Product.builder()
                .name("백팩")
                .description("일상과 여행에 완벽한 멀티포켓 백팩입니다. 방수 기능과 USB 충전 포트가 내장되어 있습니다.")
                .price(new BigDecimal("45000"))
                .stockQuantity(75)
                .category("패션")
                .imageUrl("https://images.unsplash.com/photo-1548036328-c9fa89d128fa?w=300&h=300&fit=crop")
                .seller(adminUser)
                .status(Product.ProductStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build(),

            Product.builder()
                .name("운동화")
                .description("편안한 착용감과 뛰어난 쿠셔닝을 제공하는 러닝화입니다. 모든 활동에 적합합니다.")
                .price(new BigDecimal("120000"))
                .stockQuantity(30)
                .category("패션")
                .imageUrl("https://images.unsplash.com/photo-1549298916-b41d501d3772?w=300&h=300&fit=crop")
                .seller(adminUser)
                .status(Product.ProductStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build(),

            Product.builder()
                .name("커피 원두")
                .description("에티오피아 원산지의 프리미엄 아라비카 원두입니다. 중배전으로 깔끔하고 부드러운 맛이 특징입니다.")
                .price(new BigDecimal("25000"))
                .stockQuantity(200)
                .category("식품")
                .imageUrl("https://images.unsplash.com/photo-1559056199-641a0ac8b55e?w=300&h=300&fit=crop")
                .seller(adminUser)
                .status(Product.ProductStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build(),

            Product.builder()
                .name("텀블러")
                .description("스테인리스 스틸 이중벽 구조로 보온/보냉 효과가 뛰어난 텀블러입니다. 친환경적이고 세련된 디자인입니다.")
                .price(new BigDecimal("18000"))
                .stockQuantity(80)
                .category("생활용품")
                .imageUrl("https://images.unsplash.com/photo-1464207687429-7505649dae38?w=300&h=300&fit=crop")
                .seller(adminUser)
                .status(Product.ProductStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build(),

            Product.builder()
                .name("LED 데스크램프")
                .description("눈의 피로를 줄여주는 무단계 밝기 조절 LED 램프입니다. USB 충전 가능하고 휴대성이 뛰어납니다.")
                .price(new BigDecimal("35000"))
                .stockQuantity(60)
                .category("생활용품")
                .imageUrl("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&h=300&fit=crop")
                .seller(adminUser)
                .status(Product.ProductStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build(),

            Product.builder()
                .name("스킨케어 세트")
                .description("민감한 피부를 위한 천연 성분 스킨케어 3종 세트입니다. 토너, 에센스, 크림이 포함되어 있습니다.")
                .price(new BigDecimal("65000"))
                .stockQuantity(40)
                .category("뷰티")
                .imageUrl("https://images.unsplash.com/photo-1556228578-8c89e6adf883?w=300&h=300&fit=crop")
                .seller(adminUser)
                .status(Product.ProductStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build()
        };

        for (Product product : sampleProducts) {
            productRepository.save(product);
        }

        log.info("Created {} sample products", sampleProducts.length);
    }
}