# Shop Application

Spring Boot 기반 Maven 프로젝트입니다.

# Project Version

8

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

발로란트 라이프 타임 (간략한 매치 정보) 관련 dto변환 및 엔티티 생성

발로란트 라이프 타임 샘플 데이터 추가 및 발로란트 매치 샘플 데이터 변경

발로란트 분석을 위한 매치 스탯 dto 추가

발로란트 api 명세 및 해당 api들의 dto, entity와 저장 로직 생성.