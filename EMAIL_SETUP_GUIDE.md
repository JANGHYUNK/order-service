# 📧 이메일 인증 설정 가이드

## 🔒 보안 설정 구조

### 파일 분리
- **`application.properties`**: 공개 가능한 설정 (GitHub에 포함)
- **`application-secret.properties`**: 민감한 정보 (Git에서 제외)

## 📋 필수 설정 항목

### application-secret.properties에 추가해야 할 내용:

```properties
# ===============================
# EMAIL CREDENTIALS (민감정보)
# ===============================
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=noreply@orderservice.com
BASE_URL=http://localhost:8080

# 이메일 인증 설정
MAIL_VERIFICATION_EXPIRATION_HOURS=24
MAIL_CODE_EXPIRATION_MINUTES=10
```

## 🎯 이메일 제공업체별 설정

### 1. Gmail (권장)
```properties
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-gmail@gmail.com
MAIL_PASSWORD=xxxx xxxx xxxx xxxx  # 16자리 앱 비밀번호
MAIL_FROM=noreply@orderservice.com
```

**Gmail 앱 비밀번호 생성 방법:**
1. [Google 계정 관리](https://myaccount.google.com/) 접속
2. 보안 → 2단계 인증 설정 (필수)
3. 보안 → 앱 비밀번호 → 앱 선택: "메일"
4. 생성된 16자리 비밀번호 복사 (`xxxx xxxx xxxx xxxx` 형태)

### 2. 네이버 메일
```properties
MAIL_HOST=smtp.naver.com
MAIL_PORT=587 또는 465
MAIL_USERNAME=your-id@naver.com
MAIL_PASSWORD=your-naver-password
MAIL_FROM=your-id@naver.com
```

### 3. 다음 메일
```properties
MAIL_HOST=smtp.daum.net
MAIL_PORT=587
MAIL_USERNAME=your-id@daum.net
MAIL_PASSWORD=your-daum-password
MAIL_FROM=your-id@daum.net
```

### 4. Office 365
```properties
MAIL_HOST=smtp.office365.com
MAIL_PORT=587
MAIL_USERNAME=your-email@company.com
MAIL_PASSWORD=your-office365-password
MAIL_FROM=your-email@company.com
```

## 🔧 고급 설정 옵션

### 개발/운영 환경별 설정
```properties
# 개발환경
BASE_URL=http://localhost:8080
MAIL_FROM=dev-noreply@orderservice.com

# 운영환경
BASE_URL=https://your-domain.com
MAIL_FROM=noreply@your-domain.com
```

### 인증 시간 설정
```properties
# 이메일 인증번호 만료 시간 (분)
MAIL_CODE_EXPIRATION_MINUTES=10

# 일반 이메일 인증 링크 만료 시간 (시간)
MAIL_VERIFICATION_EXPIRATION_HOURS=24
```

## ⚠️ 보안 주의사항

### 1. Git 관리
```bash
# .gitignore에 추가 (이미 설정됨)
src/main/resources/application-secret.properties
```

### 2. 파일 권한 설정 (Linux/Mac)
```bash
chmod 600 src/main/resources/application-secret.properties
```

### 3. 운영 환경에서는
- 환경변수 사용 권장
- 또는 외부 설정 서버 (Spring Cloud Config)
- Docker Secrets 또는 Kubernetes Secrets

## 🧪 테스트 방법

### 1. 설정 확인
```bash
./gradlew bootRun
```
로그에서 이메일 설정 로딩 확인

### 2. 인증번호 발송 테스트
1. 회원가입 페이지에서 이메일 입력
2. "인증전송" 버튼 클릭
3. 이메일 수신 확인

### 3. 문제 해결
**이메일이 오지 않는 경우:**
- 스팸함 확인
- Gmail 앱 비밀번호 재생성
- 방화벽/네트워크 설정 확인
- 애플리케이션 로그 확인

## 📝 설정 체크리스트

- [ ] `application-secret.properties`에 MAIL_USERNAME 설정
- [ ] `application-secret.properties`에 MAIL_PASSWORD 설정 (앱 비밀번호)
- [ ] MAIL_FROM 이메일 주소 설정
- [ ] BASE_URL 환경에 맞게 설정
- [ ] Gmail 2단계 인증 활성화 (Gmail 사용 시)
- [ ] 앱 비밀번호 생성 (Gmail 사용 시)
- [ ] 테스트 이메일 발송 확인

## 🚨 문제 해결

### 자주 발생하는 오류
1. **Authentication failed**: 앱 비밀번호 확인
2. **Connection timeout**: 네트워크/방화벽 확인
3. **535 Authentication failed**: 이메일/비밀번호 재확인
4. **이메일 미수신**: 스팸함 확인

### 로그 확인
```bash
# 애플리케이션 실행 시 이메일 관련 로그 확인
./gradlew bootRun --info
```

## 📞 지원

이메일 설정 관련 문제가 있다면:
1. 로그 확인
2. 네트워크 연결 확인
3. 이메일 제공업체 설정 재확인
4. Gmail의 경우 앱 비밀번호 재생성