# URL Shortener API

Spring Boot를 사용한 URL 단축 서비스 API입니다.

## 기능

- URL 단축 생성
- 커스텀 단축 코드 지원
- URL 만료 시간 설정
- 클릭 수 통계
- URL 리다이렉트
- URL 삭제

## 기술 스택

- Spring Boot 3.2.0
- Spring Data JPA
- H2 Database (개발용)
- Java 17
- Maven

## API 엔드포인트

### 1. URL 단축 생성
```
POST /api/urls
Content-Type: application/json

{
  "originalUrl": "https://example.com/very-long-url",
  "customShortCode": "optional-custom-code",
  "expiresAt": "2024-12-31T23:59:59"
}
```

### 2. URL 통계 조회
```
GET /api/urls/{shortCode}
```

### 3. URL 리다이렉트
```
GET /api/urls/redirect/{shortCode}
```

### 4. URL 삭제
```
DELETE /api/urls/{shortCode}
```

## 실행 방법

### 1. 프로젝트 빌드
```bash
mvn clean install
```

### 2. 애플리케이션 실행
```bash
mvn spring-boot:run
```

### 3. 접속
- API: http://localhost:8080
- H2 Console: http://localhost:8080/h2-console

## 사용 예시

### URL 단축 생성
```bash
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -d '{
    "originalUrl": "https://www.google.com/search?q=spring+boot+tutorial"
  }'
```

### 커스텀 단축 코드로 생성
```bash
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -d '{
    "originalUrl": "https://www.example.com",
    "customShortCode": "my-link"
  }'
```

### URL 통계 조회
```bash
curl http://localhost:8080/api/urls/abc12345
```

### URL 리다이렉트
브라우저에서 `http://localhost:8080/api/urls/redirect/abc12345` 접속

## 데이터베이스 스키마

```sql
CREATE TABLE urls (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_code VARCHAR(255) NOT NULL UNIQUE,
    original_url TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    click_count BIGINT NOT NULL DEFAULT 0
);
```

## 설정

`application.properties`에서 다음 설정을 변경할 수 있습니다:

- `server.port`: 서버 포트 (기본값: 8080)
- `app.base-url`: 기본 URL (기본값: http://localhost:8080)
- 데이터베이스 설정

## 개발 환경

- Java 17 이상
- Maven 3.6 이상
- Spring Boot 3.2.0
