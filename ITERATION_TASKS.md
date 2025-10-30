# ITERATION_TASKS_v4.md — 화면명세 기반 실무형 체크리스트 (React + Spring Boot + MyBatis)

> **구성 기준:** 화면 명세 + API 명세 + 인터페이스 명세 + 데이터 모델링  
> **리포지토리 구조:**  
> - Frontend → `/fo`  
> - Backend → `/api`  
> **전제:** 빈 프로젝트 생성 완료 상태 (Vite + Spring Initializr 기본 설정만 있음), DB스키마, 시퀀스, 인덱스 생성 완료 상태

---

## ⚙️ Iteration 0: 개발환경 세팅

### (FE) 기본 환경
- [x] `/fo` 프로젝트에 `axios`, `react-router-dom`, `zustand`(또는 recoil/pinia 유사 스토어) 설치
- [x] `.env` 파일 생성 → `VITE_API_BASE_URL=http://localhost:8080/api`
- [x] Axios 인스턴스 생성 (`/fo/src/api/axios.ts`)
- [x] 응답 인터셉터에서 `res.data.code === "0000"` 검사 후 에러 처리
- [x] Router 설정 (`/fo/src/router/index.tsx`)
- [x] 공통 Layout 구성 (`Header`, `Footer`, `Outlet` 포함)
- [x] UI 라이브러리 설치 (선택: MUI, AntD, Tailwind 등)

### (BE) 기본 환경
- [x] `/api` 프로젝트 PostgreSQL 연결 확인 (`application.yml`)
- [x] `spring.datasource`, `mybatis.mapper-locations` 설정
- [x] `MyBatisConfig` 및 `Response<T>` 클래스 생성
- [x] 전역 예외 처리기(`GlobalExceptionHandler`) 추가 (Response 포맷 유지)
- [x] DB 연결 테스트 (`select 1` 확인)
- [x] 로깅 설정 (`logback-spring.xml`)

---

## 🧩 Iteration 1: 로그인 & 회원가입

### (FE)
- [x] `LoginModal.tsx` 생성 (아이디, 비밀번호 입력)
- [x] 로그인 API 연결 (`POST /api/auth/login`)
- [x] 성공 시 localStorage.memberNo 저장, 실패 시 alert
- [x] `RegisterModal.tsx` 생성 (회원가입 폼)
- [x] `/api/members` 호출 후 성공 시 로그인 모달 전환
- [x] Header 상태(로그인/비로그인) 렌더링 처리
- [x] `/fo/src/store/authStore.ts` 상태관리 작성

### (BE)
- [x] `AuthController` 작성 (`/api/auth/login`)
- [x] `MemberController` → `/api/members` 회원가입 추가
- [x] `MemberMapper.xml`에 `selectMemberByLoginId`, `insertMember` 작성
- [x] `MemberService`에서 비밀번호 검증 로직 구현 (BCrypt 등)
- [x] `MemberDto`, `MemberEntity` 작성
- [x] 예외코드 `LOGIN_FAIL`, `DUPLICATE_ID` 매핑

---

## 🛍 Iteration 2: 메인 (상품 목록 & 등록)

### (FE)
- [x] `ProductList.tsx` 페이지 생성
- [x] `/api/products` GET 호출 → 상품 리스트 렌더링
- [x] `ProductModal.tsx` 작성 → `/api/products` POST 등록
- [x] 등록 성공 시 리스트 재조회
- [x] 각 상품에 "장바구니 담기" 버튼 추가 → `/api/cart` POST
- [x] "등록" 버튼 클릭 시 `ProductModal` 오픈 로직 추가

### (BE)
- [x] `ProductController` → `/api/products` GET/POST 구현
- [x] `ProductMapper.xml` → `selectProductList`, `insertProduct`
- [x] `ProductService` → Validation 및 중복명 검증
- [x] 에러코드 `DUPLICATE_PRODUCT`, `PRODUCT_NOT_FOUND` 정의

---

## 🛒 Iteration 3: 장바구니 화면

### (FE)
- [x] `CartPage.tsx` 페이지 생성
- [x] `/api/cart?memberNo=` GET → 목록 렌더링
- [x] 수량/삭제 버튼 구현 → `/api/cart` DELETE 호출
- [x] 총 금액 계산 로직 추가
- [x] "주문하기" 클릭 시 `/orders/form` 이동

### (BE)
- [x] Cart Entity 및 DTO 작성
- [x] `CartMapper.xml` → `selectCartList`, `insertCart`, `deleteCart`
- [x] `CartService` → ON CONFLICT 수량 증가 로직 처리
- [x] `CartController` → `/api/cart` CRUD 구현

---

## 💳 Iteration 4: 주문서 (Order Form)

### (FE)
- [x] `OrderForm.tsx` 페이지 생성
- [x] `/api/orders/form?memberNo=` GET → 회원정보 + 장바구니 리스트 표시
- [x] `/api/orders/sequence` 호출 → 주문번호 채번
- [x] 결제수단 선택 (카드 / 포인트)
- [x] "결제하기" 버튼 클릭 → `/api/payment/params` 호출 후 PG 인증 진입

### (BE)
- [x] `OrderController` → `/api/orders/form`, `/api/orders/sequence` 작성
- [x] `OrderMapper.xml` → `selectOrderFormData`, `selectNextOrderNo`
- [x] `OrderService` → 회원 + 장바구니 조회 통합
- [x] `OrderDto` → `OrderFormResponse`, `OrderSequenceResponse` 작성

---

## 🧾 Iteration 5: 결제 중 (PG 인증 및 주문 생성)

### (FE)
- [x] `OrderForm.tsx` 수정 (적립금 입력, PG 인증 로직)
- [x] `Route Handler` 생성 (/api/payment/inicis/callback)
- [x] `PaymentProcessing.tsx` 생성
- [x] PG 응답 파라미터 파싱 후 `/api/orders` POST 호출
- [x] 성공 시 `/orders/complete?orderNo=` 이동
- [x] 실패 시 `/orders/fail` 이동
- [x] `OrderComplete.tsx` 주문 완료 화면 구현
- [x] `OrderFail.tsx` 주문 실패 화면 구현

### (BE)
- [x] Payment, PaymentInterface Entity 작성
- [x] OrderProduct, OrderDetail Entity 작성
- [x] CreateOrderRequest, CreateOrderResponse DTO 작성
- [x] PaymentParamsResponse DTO 작성
- [x] PG 설정 (application.yml, PaymentProperties)
- [x] InicisClient 구현 (인증파라미터, 승인, 취소, 부분취소, 망취소)
- [x] application.yml에서 auth-url 제거 (Frontend에서 처리)
- [x] TossClient 구현 (승인, 취소, 망취소)
- [x] 결제 전략 패턴 구현 (InicisStrategy, TossStrategy, PointStrategy)
- [x] PaymentTrxMapper, PaymentInterfaceTrxMapper, OrderTrxMapper 작성
- [x] `PaymentMapper.xml` → `insertPayment`, `insertPaymentInterface`
- [x] `OrderMapper.xml` → ORDER_BASE, ORDER_PRODUCT, ORDER_DETAIL INSERT
- [x] `MemberTrxMapper` → updateMemberPoints 추가
- [x] `PaymentService` → 전략 패턴 기반 결제 처리 구현 (승인, 망취소)
- [x] `OrderService` → 주문 생성 로직 구현
- [x] `PaymentController` → `/api/payment/params`, `/api/orders` 구현
- [x] 결제 승인 실패 시 code="APPROVE_FAIL"
- [x] PG 요청/응답 JSON `PAYMENT_INTERFACE`에 저장

---

## 🧾 Iteration 6: 주문 완료 / 실패 화면

### (FE)
- [x] `OrderComplete.tsx` 생성 → API 연동 완료
- [x] 주문번호, 결제금액, 수단 표시
- [x] "주문 내역 보기" 버튼 → `/orders/history`
- [x] `OrderFail.tsx` 생성 → 실패 사유 표시 및 메인 복귀 버튼

### (BE)
- [x] `/api/orders/complete?orderNo=` → 주문/결제내역 조회
- [x] `OrderMapper.xml` → `selectOrderCompleteData`
- [x] `OrderCompleteDto` 작성

---

## 📜 Iteration 7: 주문 내역 화면

### (FE)
- [x] `OrderHistory.tsx` 페이지 생성
- [x] `/api/orders/history?memberNo=` GET 호출
- [x] 각 주문 클릭 시 `/orders/{orderNo}` 이동
- [x] 주문 상태별 표시 (COMPLETE / CANCEL)

### (BE)
- [x] `OrderController` → `/api/orders/history` 추가
- [x] `OrderMapper.xml` → `selectOrderHistoryList`
- [x] `OrderHistoryDto` 작성

---

## 📦 Iteration 8: 주문 상세 & 취소

### (FE)
- [x] `OrderDetail.tsx` 페이지 생성
- [x] `/api/orders/{orderNo}` GET 호출 → 상품, 결제, 취소이력 표시
- [x] 부분취소 UI → 수량 선택 후 `/api/orders/cancel` 호출
- [x] 취소 성공 시 재조회

### (BE)
- [x] `OrderController` → `/api/orders/{orderNo}`, `/api/orders/cancel`
- [x] `OrderMapper.xml` → `selectOrderDetail`, `insertOrderCancel`
- [x] `OrderService` → 클레임번호 채번, 원주문/취소주문 동시 처리
- [x] `PaymentService` → PG 취소 API 호출
- [x] 에러코드 `CANCEL_FAIL` 정의

---

## 🪙 Iteration 9: PG 연동 (Inicis / Toss)

> ✅ **이미 Iteration 5에서 완전히 구현 완료됨**

### (BE)
- [x] Inicis 연동 클래스 생성 (`InicisClient.java`)
  - [x] 인증 파라미터 서명 생성 (SHA256/SHA512 기반)
  - [x] 승인/취소 API 구현 (approve, refund, partialRefund, netCancel)
- [x] Toss 연동 클래스 생성 (`TossClient.java`)
  - [x] 결제 승인, 부분취소 API 구현 (approve, refund, netCancel)
- [x] PG 응답 결과 로깅 (성공/실패 구분) - `PAYMENT_INTERFACE` 테이블에 JSON 저장
- [x] 결제 처리 로직 - `OrderService.createOrder`와 `PaymentService.processPayments`에 통합 구현
- [x] 취소 처리 로직 - `OrderService.cancelOrder`와 `PaymentService.processRefund`에 통합 구현
- [x] 전략 패턴 구현 - `InicisStrategy`, `TossStrategy`, `PointStrategy`로 PG 추상화

---

## 🧪 Iteration 10: 통합 테스트 & 검증

### (FE)
- [x] Next.js 빌드 성공 (11개 페이지 정상 빌드)
- [x] useSearchParams Suspense boundary 처리 완료
- [x] TypeScript 컴파일 에러 없음
- [x] Member 타입에 phone 필드 추가
- [x] 모든 페이지 라우팅 검증 완료

### (BE)
- [x] Controller Response<T> 형식 일관성 검증 완료
- [x] 모든 CUD 작업에 @Transactional 적용 확인
- [x] ErrorCode 체계 정리 (장바구니, 주문, 결제 구분)
- [x] CART_NOT_FOUND, ORDER_NOT_FOUND 에러 코드 추가
- [x] 결제 에러 코드 4xxx → 5xxx로 재분류

---

✅ **완료 기준 요약**
- [x] FE 화면 전부 구현 및 API 연결 완료
- [x] BE 모든 API 응답 `Response<T>` 형식 일치
- [x] PG 연동 정상 처리 (InicisClient, TossClient, 전략 패턴)
- [x] 주문/취소/결제 로그 정상 누적 (PAYMENT_INTERFACE 테이블)
- [x] Frontend 빌드 성공 (정적 페이지 11개 생성)
- [x] Backend 코드 검증 완료 (트랜잭션, 에러 코드, API 응답)
