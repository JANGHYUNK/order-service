# OAuth2 소셜 로그인 설정 가이드

## 개요
이 애플리케이션은 Google과 Kakao 소셜 로그인을 지원합니다. 소셜 로그인을 사용하려면 각 플랫폼에서 OAuth2 클라이언트를 설정해야 합니다.

## 🔧 수정된 내용

### 1. Security 설정 수정 (`SecurityConfig.java`)
- OAuth2 로그인 설정 추가
- 성공/실패 핸들러 연결
- JWT 필터와 OAuth2 통합

### 2. OAuth2 설정 개선 (`application.properties`)
- 환경 변수를 사용한 클라이언트 ID/Secret 설정
- redirect URI를 localhost:8080으로 고정 (baseUrl 문제 해결)
- 카카오 스코프 최적화 (profile_image 제거)
- 성공 후 redirect URI를 `/oauth2-success.html`로 변경

### 3. UI 개선 (`signup.html`, `login.html`)
- 소셜 로그인 버튼 크기 조정 (글씨 잘림 문제 해결)
- 텍스트와 아이콘 분리로 레이아웃 개선
- flexbox를 사용한 버튼 스타일 개선

### 4. OAuth2 사용자 처리 개선 (`CustomOAuth2UserService.java`, `KakaoOAuth2UserInfo.java`)
- 카카오에서 이메일을 가져오지 못할 경우 대체 이메일 생성 로직 추가
- 카카오 프로필 이미지 속성 처리 개선 (thumbnail_image 대체)
- 이메일 유효성 검증 로직 추가

## 🚀 OAuth2 설정 방법

### 1. Google OAuth2 설정

1. [Google Cloud Console](https://console.cloud.google.com/apis/credentials)에 접속
2. 프로젝트 생성 또는 선택
3. "사용자 인증 정보 만들기" → "OAuth 2.0 클라이언트 ID" 선택
4. 애플리케이션 유형: "웹 애플리케이션" 선택
5. 승인된 리디렉션 URI에 다음 추가:
   ```
   http://localhost:8080/login/oauth2/code/google
   ```
6. 클라이언트 ID와 클라이언트 보안 비밀 복사

### 2. Kakao OAuth2 설정

1. [Kakao Developers](https://developers.kakao.com/console/app)에 접속
2. 애플리케이션 생성
3. "제품 설정" → "카카오 로그인" 메뉴로 이동
4. 카카오 로그인 활성화 설정
5. Redirect URI에 다음 추가:
   ```
   http://localhost:8080/login/oauth2/code/kakao
   ```
6. 동의항목에서 다음 권한 설정:
   - 프로필 정보(닉네임/프로필 사진): 필수 동의
   - 카카오계정(이메일): 필수 동의
7. 앱 키의 "REST API 키"와 "보안" 탭의 "Client Secret" 복사

### 3. 환경 변수 설정

`.env.example` 파일을 `.env`로 복사하고 실제 값으로 변경:

```bash
cp .env.example .env
```

`.env` 파일 내용 수정:
```
GOOGLE_CLIENT_ID=실제_구글_클라이언트_ID
GOOGLE_CLIENT_SECRET=실제_구글_클라이언트_SECRET

KAKAO_CLIENT_ID=실제_카카오_앱키
KAKAO_CLIENT_SECRET=실제_카카오_클라이언트_SECRET
```

### 4. 환경 변수 로드

Spring Boot 애플리케이션 실행 시 환경 변수가 자동으로 로드됩니다.

또는 IntelliJ/Eclipse에서 실행 시 환경 변수를 설정할 수 있습니다.

## ✅ 테스트 방법

1. 애플리케이션 실행:
   ```bash
   ./gradlew bootRun
   ```

2. 브라우저에서 `http://localhost:8080/signup.html` 접속

3. "Google로 시작하기" 또는 "Kakao로 시작하기" 버튼 클릭

4. 각 플랫폼의 OAuth2 인증 페이지에서 로그인

5. 인증 성공 시 애플리케이션으로 리디렉트

## 🚨 문제 해결

### ✅ 해결된 문제들

#### 1. Google OAuth2 400 에러
**원인**: SecurityConfig에서 OAuth2 로그인이 비활성화되어 있었음
**해결**: `SecurityConfig.java`에서 `.oauth2Login()` 설정 추가

#### 2. 카카오 OAuth2 KOE101 에러
**원인**:
- 잘못된 스코프 설정 (`profile_image` 권한 문제)
- 카카오에서 이메일 정보 접근 실패
**해결**:
- 스코프를 `profile_nickname,account_email`로 변경
- 이메일을 가져올 수 없는 경우 대체 이메일 생성 로직 추가

#### 3. 소셜 로그인 버튼 글씨 잘림
**원인**: 고정된 폰트 크기와 패딩으로 인한 텍스트 오버플로우
**해결**: flexbox 레이아웃과 responsive 패딩 적용

### 🔍 일반적인 문제 해결

#### WhiteLabel Error 페이지 발생 시
1. OAuth2 클라이언트 ID/Secret이 올바른지 확인
2. Redirect URI가 정확히 설정되었는지 확인
3. 애플리케이션 로그에서 오류 메시지 확인
4. 브라우저 개발자 도구에서 네트워크 탭 확인

#### 환경 변수가 인식되지 않는 경우
1. `.env` 파일이 프로젝트 루트에 있는지 확인
2. 환경 변수명이 정확한지 확인
3. IDE 재시작 후 다시 시도
4. 시스템 환경 변수로 설정하거나 IDE 실행 설정에서 직접 설정

#### 카카오 OAuth2 추가 문제
1. **카카오 개발자 콘솔에서 확인해야 할 항목:**
   - 플랫폼 등록: Web 플랫폼 등록 및 사이트 도메인 추가
   - 카카오 로그인 활성화
   - 동의항목 설정: 닉네임, 이메일 필수 동의로 설정
   - Redirect URI: `http://localhost:8080/login/oauth2/code/kakao` 정확히 입력

## 📝 참고사항

- 개발 환경에서는 `localhost:8080`을 사용
- 프로덕션 배포 시 실제 도메인으로 Redirect URI 업데이트 필요
- HTTPS를 사용하는 것이 보안상 권장됨