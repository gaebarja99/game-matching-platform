# Shop Application

Spring Boot 기반 Maven 프로젝트입니다.

# Project Version

7

## 프로젝트 구조

- `config` - 설정 클래스
- `constant` - 애플리케이션 상수
- `controller` - REST 컨트롤러
- `dao` - Data Access Objects
- `dto` - Data Transfer Objects
- `entity` - 도메인 엔티티
- `exception` - 커스텀 예외 클래스
- `repository` - Spring Data JPA 리포지토리
- `service` - 비즈니스 로직 서비스

## 실행 방법

```bash
./mvnw spring-boot:run
```

Windows:
```cmd
mvnw.cmd spring-boot:run
```

## 빌드 방법

```bash
./mvnw clean package
```

라이엇 api로 받은 롤 전적 json 파일을 받을 dto 추가.

롤 관련 api들이 추가된 dto를 사용하도록 변경.

롤 매치 엔티티 수정.

롤, 발로란트 샘플 데이터 추가.

샘플 데이터 로더 추가 및 샘플 프로파일 자동 실행 설정 추가.