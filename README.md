# Vibe Pay 2.0

> 상품 주문 및 결제가 가능한 간단한 커머스 사이트

---

## 📋 프로젝트 개요

Vibe Pay는 상품 조회, 장바구니, 주문, 결제, 취소/환불 기능을 제공하는 풀스택 커머스 애플리케이션입니다.

**주요 기능:**
- 회원가입 및 로그인 (간소화된 인증 방식)
- 상품 목록 조회 및 장바구니 담기
- 주문서 작성 및 주문 생성
- 다중 결제수단 지원 (카드, 적립금)
- PG사 연동 (토스페이먼츠, 이니시스)
- 전체/부분 주문 취소 및 환불
- 주문 내역 조회

---

## 🛠️ 기술 스택

### Backend (API)
- **Java 21**
- **Spring Boot 3.4.11**
- **MyBatis 3.0.3** (SQL Mapper)
- **PostgreSQL** (Database)
- **WebFlux** (PG 연동용 WebClient)
- **Lombok**

### Frontend (FO)
- **Next.js 16.0.1** (App Router)
- **React 19.2.0**
- **TypeScript 5**
- **Tailwind CSS 4**
- **Zustand** (상태 관리)
- **Axios** (HTTP 클라이언트)
- **Zod** (스키마 검증)

### Database
- **PostgreSQL**
- 트랜잭션 기반 주문 처리
- 망취소 및 환불 로직 지원

---

## 📁 프로젝트 구조

```
vibe-pay-2/
├── api/                      # Spring Boot 백엔드
│   ├── src/main/java/vibe/api/
│   │   ├── common/          # 공통 모듈 (예외, 응답, Enum)
│   │   ├── config/          # 설정 (CORS, WebClient 등)
│   │   ├── controller/      # REST API 컨트롤러
│   │   ├── dto/             # Request/Response DTO
│   │   ├── entity/          # 엔티티 (도메인 모델)
│   │   ├── payment/         # 결제 전략 패턴 구현
│   │   │   ├── strategy/   # 결제수단별 전략 (Card, Point)
│   │   │   └── dto/        # 결제 관련 DTO
│   │   ├── pg/              # PG사 클라이언트 (Toss, Inicis)
│   │   ├── repository/      # MyBatis Mapper 인터페이스
│   │   └── service/         # 비즈니스 로직
│   └── src/main/resources/
│       ├── mapper/          # MyBatis XML Mapper
│       └── application.yml  # 설정 파일
│
├── fo/                       # Next.js 프론트엔드
│   ├── src/
│   │   ├── app/            # App Router 페이지
│   │   │   ├── api/       # API Routes (PG 콜백 등)
│   │   │   ├── auth/      # 로그인/회원가입
│   │   │   ├── cart/      # 장바구니
│   │   │   ├── orders/    # 주문 관련 페이지
│   │   │   └── page.tsx   # 메인 페이지 (상품 목록)
│   │   ├── components/     # React 컴포넌트
│   │   │   ├── layout/    # 레이아웃 컴포넌트
│   │   │   └── ui/        # 공통 UI 컴포넌트
│   │   ├── lib/           # 유틸리티 함수
│   │   ├── store/         # Zustand 상태 관리
│   │   └── types/         # TypeScript 타입 정의
│   └── package.json
│
├── prd/                      # 명세 문서
│   ├── API 명세.md
│   ├── 화면 명세.md
│   ├── 인터페이스 명세.md
│   └── 데이터 모델링.md
│
├── docs/                     # 개발 가이드
│   ├── api-guide.md         # Backend 개발 가이드
│   ├── fo-guide.md          # Frontend 개발 가이드
│   └── sql-guide.md         # Database/SQL 가이드
│
├── CLAUDE.md                 # 프로젝트 개발 가이드 (AI 작업용)
├── ITERATION_TASKS.md        # Iteration별 작업 내역
├── TEST_REPORT.md            # 테스트 보고서
└── README.md                 # 본 문서
```

---

## 🚀 실행 방법

### 1️⃣ 사전 준비

- **Java 21** 설치
- **Node.js 20+** 설치
- **PostgreSQL** 설치 및 데이터베이스 생성

### 2️⃣ 데이터베이스 설정

```sql
-- PostgreSQL 데이터베이스 생성
CREATE DATABASE vibe_pay;
```

스키마 및 초기 데이터는 `prd/데이터 모델링.md` 참조

### 3️⃣ Backend 실행

```bash
cd api

# application.yml 설정 (DB 접속 정보, PG 설정 등)
# src/main/resources/application.yml 파일 수정

# Maven 빌드 및 실행
./mvnw clean install
./mvnw spring-boot:run

# 기본 포트: 8080
```

### 4️⃣ Frontend 실행

```bash
cd fo

# 의존성 설치
npm install

# 개발 서버 실행
npm run dev

# 기본 포트: 3000
```

### 5️⃣ 접속

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api

---

## 🏗️ 아키텍처

### Backend 아키텍처

- **계층 구조**: Controller → Service → Repository (MyBatis Mapper)
- **전략 패턴**: 결제수단별 전략 (PaymentMethodStrategy)
- **PG 전략**: PG사별 전략 (PgStrategy - Toss, Inicis)
- **트랜잭션 관리**: Spring `@Transactional` + 별도 트랜잭션 분리 (REQUIRES_NEW)
- **예외 처리**: 통합 ExceptionHandler + 커스텀 ApiException
- **응답 형식**: 통일된 `Response<T>` 구조

### Frontend 아키텍처

- **App Router**: Next.js 16 App Router 사용
- **상태 관리**: Zustand (authStore)
- **HTTP 통신**: Axios + 인터셉터
- **스타일링**: Tailwind CSS 4
- **타입 안정성**: TypeScript + Zod 스키마 검증

### 결제 처리 흐름

```
1. 사용자가 결제수단 선택 (카드, 적립금)
2. PaymentService가 각 결제수단별 전략 실행
3. 카드 결제 시:
   - PG 인증 → PG 승인 → Payment INSERT
   - 에러 발생 시 망취소 처리
4. 적립금 결제 시:
   - Member 적립금 차감 → Payment INSERT
5. 모든 결제 성공 시 주문 완료
6. 일부 실패 시 성공한 결제 모두 롤백 (망취소/환불)
```

### 주문 취소 처리

- **전체 취소**: 주문의 모든 상품을 한 번에 취소
- **부분 취소**: 특정 상품의 일부 수량만 취소 (여러 번 가능)
- 모든 취소는 원주문(process_seq=1)을 parent로 참조
- 취소 금액에 따라 결제 환불 처리 (PG 취소 API 호출)

---

## 📝 주요 API 엔드포인트

### 인증/회원
- `POST /api/auth/login` - 로그인
- `POST /api/members` - 회원가입
- `GET /api/members/{memberNo}` - 회원 정보 조회

### 상품
- `GET /api/products` - 상품 목록 조회
- `GET /api/products/{productNo}` - 상품 상세 조회

### 장바구니
- `POST /api/cart/add` - 장바구니 담기
- `GET /api/cart/{memberNo}` - 장바구니 조회
- `DELETE /api/cart/{cartId}` - 장바구니 삭제

### 주문
- `POST /api/orders/order-no` - 주문번호 채번
- `POST /api/orders/form` - 주문서 데이터 조회
- `POST /api/orders` - 주문 생성
- `GET /api/orders/complete/{orderNo}` - 주문 완료 조회
- `GET /api/orders/history/{memberNo}` - 주문 내역 조회
- `GET /api/orders/{orderNo}` - 주문 상세 조회
- `POST /api/orders/cancel` - 주문 취소

### 결제
- `POST /api/payment/inicis/auth` - 이니시스 인증 요청
- `POST /api/payment/inicis/callback` - 이니시스 콜백 처리
- `POST /api/payment/toss/callback` - 토스 콜백 처리

---

## 📚 문서

- **[API 명세](prd/API%20명세.md)**: 모든 API 엔드포인트 상세 명세
- **[화면 명세](prd/화면%20명세.md)**: 프론트엔드 화면별 상세 명세
- **[인터페이스 명세](prd/인터페이스%20명세.md)**: PG사 연동 인터페이스 명세
- **[데이터 모델링](prd/데이터%20모델링.md)**: 데이터베이스 스키마 및 ERD
- **[Backend 가이드](docs/api-guide.md)**: Spring Boot 개발 가이드
- **[Frontend 가이드](docs/fo-guide.md)**: Next.js 개발 가이드
- **[Database 가이드](docs/sql-guide.md)**: PostgreSQL/MyBatis 가이드

---

## 🔑 주요 기능 설명

### 1. 전략 패턴 기반 결제 처리

결제수단(카드, 적립금)별로 다른 처리 로직을 전략 패턴으로 구현했습니다.

```java
// 결제수단 전략 인터페이스
public interface PaymentMethodStrategy {
    boolean supports(String method);
    ApprovalResult approve(OrderInfo orderInfo, PaymentInfo paymentInfo);
    void netCancel(ApprovalResult approvalResult);
    void refund(Payment payment, Integer cancelAmount, Integer remainAmount);
}

// 카드 결제 전략 - PG사 전략을 내부적으로 사용
@Component
public class CardPaymentStrategy implements PaymentMethodStrategy { ... }

// 적립금 결제 전략 - DB 트랜잭션으로 처리
@Component
public class PointPaymentStrategy implements PaymentMethodStrategy { ... }
```

### 2. 망취소 처리

PG 승인 후 시스템 에러 발생 시 망취소를 통해 일관성을 유지합니다.

```java
// ApprovalResult에 망취소 필요 여부와 컨텍스트 포함
public class ApprovalResult {
    private final Payment payment;
    private final boolean needsNetCancel;  // PG 승인 성공 여부
    private final Object netCancelContext;  // 망취소에 필요한 정보
    private final boolean success;
}
```

### 3. 별도 트랜잭션 분리

주문 완료 후 장바구니 삭제는 별도 트랜잭션으로 처리하여 실패해도 주문에 영향을 주지 않습니다.

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void deleteCartAfterOrder(List<Long> cartIdList) {
    // 독립적인 트랜잭션으로 실행
    // 실패해도 주문 결과에 영향 없음
}
```

### 4. 주문 취소 계층 구조

모든 취소 건은 원주문(process_seq=1)을 parent로 참조하여 명확한 계층 구조를 유지합니다.

```
ORDER_DETAIL 테이블 예시:
- process_seq=1, parent_process_seq=NULL (원주문)
- process_seq=2, parent_process_seq=1 (1차 취소)
- process_seq=3, parent_process_seq=1 (2차 취소)
```

---

## 🧪 테스트

테스트 시나리오 및 결과는 `TEST_REPORT.md` 참조

**주요 테스트 케이스:**
- 회원가입 및 로그인
- 상품 조회 및 장바구니 담기
- 주문서 작성 및 주문 생성
- 다중 결제수단 처리
- PG 콜백 처리
- 전체/부분 주문 취소
- 망취소 시나리오

---

## 📌 개발 규칙

### 네이밍 컨벤션
- **Java**: PascalCase (클래스), camelCase (변수/메서드)
- **TypeScript**: PascalCase (컴포넌트/타입), camelCase (변수/함수)
- **Database**: UPPER_SNAKE_CASE (테이블/컬럼)

### 코드 스타일
- **Backend**: Spring Boot Best Practice + 계층 구조 준수
- **Frontend**: React Best Practice + Tailwind CSS 활용
- **SQL**: 가독성 중심 (CTE, JOIN 명시)

자세한 내용은 `docs/` 폴더의 각 가이드 문서 참조

---

## 🔄 개발 프로세스

1. **명세 문서 확인**: `prd/` 폴더의 명세 문서 참조
2. **Iteration 단위 개발**: `ITERATION_TASKS.md` 기반 진행
3. **가이드 준수**: `docs/` 폴더의 개발 가이드 준수
4. **테스트 및 검증**: E2E 시나리오 테스트 수행

---

## 📄 라이선스

이 프로젝트는 학습 및 연습 목적으로 개발되었습니다.

---

## 👥 기여자

- **Claude** (AI Assistant) - 전체 시스템 설계 및 구현

---

## 📮 문의

프로젝트 관련 문의사항이 있으시면 이슈를 등록해주세요.
