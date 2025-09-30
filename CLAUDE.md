# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.5.6 order service application using Java 21, designed for realtime order processing. The application provides a complete e-commerce platform with user authentication (local + OAuth2), role-based access control, product management, and order processing capabilities.

## Build System & Commands

**Build Tool**: Gradle with Wrapper
- **Build**: `./gradlew build`
- **Run**: `./gradlew bootRun`
- **Test**: `./gradlew test`
- **Clean**: `./gradlew clean`
- **Run single test**: `./gradlew test --tests ClassName.methodName`

## Technology Stack

**Core Framework**: Spring Boot 3.5.6 with Java 21

**Key Dependencies**:
- Spring Web (REST APIs)
- Spring Data JPA (MySQL integration with Hibernate)
- Spring Data Redis (Caching layer)
- Spring Kafka (Message streaming)
- Spring Security + OAuth2 Client (Google, Kakao, Naver)
- Spring Mail (Email verification)
- JWT (JJWT 0.12.3 for token-based authentication)
- Lombok (Code generation)
- MySQL Connector
- JUnit 5 (Testing)

## Architecture Overview

### Package Structure

**Base Package**: `com.example.order_service`

The application follows a standard layered architecture:

```
com.example.order_service/
├── config/          # Configuration classes (Security, Web, DataLoader)
├── controller/      # REST API endpoints and page controllers
├── dto/             # Data Transfer Objects for API requests/responses
├── entity/          # JPA entities (User, Product, Order, OrderItem, EmailVerification)
├── repository/      # Spring Data JPA repositories
├── security/        # Security components (JWT, OAuth2, UserDetails)
├── service/         # Business logic layer
└── util/            # Utility classes (JwtTokenProvider)
```

### Authentication & Authorization Architecture

**Multi-Provider Authentication System**:
1. **Local Authentication**: Username/password with JWT tokens
2. **OAuth2 Providers**: Google, Kakao, Naver integration
3. **Email Verification**: Required for local signup with time-limited codes

**Key Security Components**:
- `SecurityConfig`: Main security configuration with role-based access control
- `JwtAuthenticationFilter`: Validates JWT tokens on protected endpoints
- `JwtTokenProvider`: Generates and validates JWT access/refresh tokens
- `CustomUserDetailsService`: Loads user details for authentication
- `CustomOAuth2UserService`: Handles OAuth2 user information
- `OAuth2AuthenticationSuccessHandler`: Post-OAuth2 login flow (generates JWT, redirects)
- `OAuth2UserInfoFactory`: Abstract factory for different OAuth2 providers

**Role-Based Access Control (RBAC)**:
- `USER`: Standard user access (main page, mypage, orders)
- `SELLER`: Seller dashboard access
- `ADMIN`: Full admin dashboard access

### Entity Relationships

**Core Entities**:
- `User`: User accounts with role (USER/SELLER/ADMIN) and auth provider (LOCAL/GOOGLE/KAKAO/NAVER)
- `Product`: Product catalog with categories, pricing, stock management
- `Order`: Order records with status tracking
- `OrderItem`: Line items within orders
- `EmailVerification`: Email verification codes with expiration

**User Entity Enums**:
- `Role`: USER, SELLER, ADMIN
- `AuthProvider`: LOCAL, GOOGLE, KAKAO, NAVER

### API Endpoints Structure

**Public Endpoints** (`/api/public/**`):
- Product listing, categories, search (no authentication required)

**Auth Endpoints** (`/api/auth/**`):
- `/signup`: Local user registration with email verification
- `/login`: Local login (returns JWT tokens)
- `/me`: Get current user info (requires authentication)
- `/verify-email`: Email verification code validation
- `/send-verification-code`: Resend verification code

**Role-Protected Endpoints**:
- `/admin/**`: Admin dashboard APIs (ADMIN role)
- `/seller/**`: Seller dashboard APIs (SELLER role)
- `/user/**`: User-specific APIs (USER, SELLER, ADMIN roles)

**OAuth2 Endpoints**:
- `/oauth2/authorization/{provider}`: Initiate OAuth2 login (google/kakao/naver)
- `/login/oauth2/code/{provider}`: OAuth2 callback (handled by Spring Security)
- `/oauth2-success.html`: Frontend redirect with JWT token in query params

### Frontend Architecture

**Static HTML Pages** (in `src/main/resources/static/`):
- `index.html`: Main landing page with product catalog
- `login.html`: Login form (local + OAuth2 buttons)
- `signup.html`: Registration form with email verification
- `mypage.html`: User profile with tabs (orders, settings, likes)
- `dashboard.html`: Generic dashboard (SELLER/ADMIN roles)
- `admin-dashboard.html`: Admin-specific dashboard
- `seller-dashboard.html`: Seller-specific dashboard
- `oauth2-success.html`: OAuth2 callback handler (saves JWT, redirects by role)
- `oauth2-signup.html`: Additional info collection for OAuth2 new users

**Frontend Authentication Flow**:
1. User logs in → JWT tokens saved to localStorage
2. Authenticated requests include `Authorization: Bearer {token}` header
3. Role-based UI: USER sees "마이페이지" button, ADMIN/SELLER see "대시보드" button
4. Post-login redirects: USER → `/`, ADMIN/SELLER → `/dashboard.html`

### Configuration & Environment

**Required Environment Variables** (set in `application-secret.properties` or environment):
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`: MySQL connection
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`: Google OAuth2
- `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`: Kakao OAuth2
- `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET`: Naver OAuth2
- `JWT_SECRET`: JWT signing key
- `JWT_EXPIRATION`: JWT token expiration time (milliseconds)
- `OAUTH2_REDIRECT_URIS`: Allowed OAuth2 redirect URIs
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`: SMTP configuration
- `MAIL_FROM`: Email sender address
- `MAIL_VERIFICATION_EXPIRATION_HOURS`, `MAIL_CODE_EXPIRATION_MINUTES`: Verification settings

**Database Configuration**:
- Database: `order_service`
- Default: `jdbc:mysql://localhost:3306/order_service`
- Hibernate DDL: `update` (auto-creates/updates schema)
- SQL logging enabled in development
- MySQL dialect: `MySQLDialect`

### Key Design Patterns

1. **Factory Pattern**: `OAuth2UserInfoFactory` creates provider-specific user info objects
2. **Strategy Pattern**: Different OAuth2 providers implement `OAuth2UserInfo` interface
3. **Repository Pattern**: Spring Data JPA repositories for data access
4. **DTO Pattern**: Separate DTOs for API contracts vs. entities
5. **Builder Pattern**: Lombok `@Builder` on entities for clean object creation

### Data Loading

`DataLoader` component initializes sample data on application startup:
- Creates default admin, seller, and user accounts
- Seeds product catalog with sample products

## Development Notes

### Important Behavioral Aspects

1. **Package Naming**: Base package is `com.example.order_service` (underscore, not hyphen) due to Java naming constraints

2. **Session Management**:
   - Session policy: `ALWAYS` (maintains sessions for OAuth2)
   - JWT used for API authentication after login

3. **CORS Configuration**:
   - Allows all origins in development (`allowedOriginPatterns: *`)
   - Credentials enabled for cross-origin requests

4. **Security Filter Order**:
   - JWT filter runs before UsernamePasswordAuthenticationFilter
   - Public endpoints bypass authentication

5. **OAuth2 Flow**:
   - Success → Generate JWT → Redirect to `oauth2-success.html?token=...`
   - Frontend saves token → Fetches user info → Redirects by role
   - New OAuth2 users may need additional signup step

6. **Email Verification**:
   - Required for local signups
   - Codes expire after configured time
   - Verification links/codes sent via SMTP

## Common Development Tasks

### Adding a New OAuth2 Provider

1. Add provider configuration to `application.properties` (client ID, secret, scopes, endpoints)
2. Create provider-specific user info class extending `OAuth2UserInfo`
3. Update `OAuth2UserInfoFactory` to handle the new provider
4. Add `AuthProvider` enum value in `User` entity

### Adding a New API Endpoint

1. Create/update controller in `controller/` package
2. Define request/response DTOs in `dto/` package
3. Implement business logic in service layer
4. Configure security rules in `SecurityConfig` if needed
5. Add endpoint to appropriate path pattern (`/api/public/**`, `/api/auth/**`, etc.)

### Modifying User Roles or Permissions

1. Update `User.Role` enum for new roles
2. Modify `SecurityConfig.filterChain()` to add role-based matchers
3. Update frontend logic for role-based UI rendering
4. Adjust OAuth2/login redirects based on new role logic