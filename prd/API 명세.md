# API 명세_2

# API 명세

---

# 🧩 API 통합 명세서

> 로그인 로직을 단순화하여 회원 DB 기반으로 구현.
> 

> JWT나 세션 없이, 회원 정보를 조회해 일치 시 로그인 성공.
> 

> **모든 API 응답은 `Response<T>` 객체로 감싸서 반환하며, HTTP 200 OK를 기본으로 합니다.**
> 

> - 성공: `"code": "0000"`, `"message": "성공"`, `payload`에 데이터(없으면 `null`)
> 

> - 실패: 의미형 에러 코드 사용(예: `LOGIN_FAIL`, `DUPLICATE_ID`, `PRODUCT_NOT_FOUND`, `APPROVE_FAIL`, `CANCEL_FAIL`, `PG_SIGN_ERROR` 등), `payload: null`
> 

---

## 🔐 인증 / 회원

### 🔹 로그인 (`POST /api/auth/login`)

**설명:**

회원 DB에서 로그인 아이디와 비밀번호를 조회하여 일치할 경우 로그인 성공.

JWT 없이 memberNo 기반으로 인증 처리.

**Request**

```json
{
  "loginId": "user01",
  "password": "test1234"
}
```

**Response (성공)**

```json
{
  "timestamp": "2025-10-30T09:00:00.000",
  "code": "0000",
  "message": "성공",
  "payload": {
    "memberNo": "M001",
    "name": "홍길동",
    "email": "[hong@test.com](mailto:hong@test.com)",
    "points": 15000
  }
}
```

⚙️ **내부 처리 로직**

1. `member` 테이블에서 `login_id` 로 조회
2. 존재하지 않거나 비밀번호 불일치 시 Exception throw
3. 성공 시 회원 정보 반환 (토큰 없음)

⚠️ **예외 처리:**

- 회원 정보 없음 or 비밀번호 불일치 시
    
    ```json
    { "timestamp": "2025-10-30T09:01:00.000", "code": "LOGIN_FAIL", "message": "로그인 실패", "payload": null }
    ```
    

---

### 🔹 로그아웃 (클라이언트 처리)

**설명:**

로그아웃은 서버 세션이 아닌 클라이언트 책임으로 처리됨.

토큰이 없으므로, 클라이언트에서 로그인 상태를 해제.

**예시 (React)**

```tsx
function logout() {
  localStorage.removeItem("memberNo");
  sessionStorage.clear();
  window.location.href = "/login";
}
```

---

### 🔹 회원가입 (`POST /api/members`)

**Request**

```json
{
  "loginId": "newUser01",
  "password": "test1234",
  "name": "홍길동",
  "email": "[hong@test.com](mailto:hong@test.com)",
  "phone": "01012345678"
}
```

**Response (성공)**

```json
{
  "timestamp": "2025-10-30T09:05:00.000",
  "code": "0000",
  "message": "성공",
  "payload": null
}
```

⚠️ **예외 처리:**

- 중복 아이디, 필수 값 누락 시
    
    ```json
    { "timestamp": "2025-10-30T09:05:30.000", "code": "DUPLICATE_ID", "message": "아이디 중복 또는 필수 값 누락", "payload": null }
    ```
    

---

## 🛍 상품

### 🔹 상품 목록 조회 (`GET /api/products`)

**Request**

```
GET /api/products
```

**Response (성공)**

```json
{
  "timestamp": "2025-10-30T09:10:00.000",
  "code": "0000",
  "message": "성공",
  "payload": [
    { "productNo": "P001", "productName": "베이직 셔츠", "price": 29000 },
    { "productNo": "P002", "productName": "슬림핏 팬츠", "price": 45000 }
  ]
}
```

---

### 🔹 상품 등록 (`POST /api/products`)

**Request**

```json
{ "productName": "테스트 상품", "price": 15000 }
```

**Response (성공)**

```json
{
  "timestamp": "2025-10-30T09:12:00.000",
  "code": "0000",
  "message": "성공",
  "payload": null
}
```

⚠️ **예외 처리:**

- 필수값 누락, 중복 상품 등록 시
    
    ```json
    { "timestamp": "2025-10-30T09:12:30.000", "code": "DUPLICATE_PRODUCT", "message": "상품 등록 실패", "payload": null }
    ```
    

---

## 🛒 장바구니

### 🔹 조회 (`GET /api/cart?memberNo=M001`)

**Request**

```
GET /api/cart?memberNo=M001
```

**Response (성공)**

```json
{
  "timestamp": "2025-10-30T09:14:00.000",
  "code": "0000",
  "message": "성공",
  "payload": [
    { "cartId": 1, "productNo": "P001", "productName": "셔츠", "qty": 2, "price": 29000 }
  ]
}
```

---

### 🔹 담기 (`POST /api/cart`)

**설명:**

회원의 장바구니에 상품을 추가한다.

이미 동일한 상품이 존재하면 에러를 발생시키지 않고 **수량을 증가**시킨다.

**Request**

```json
{ "memberNo": "M001", "productNoList": ["P001", "P002"] }
```

**Response (성공)**

```json
{
  "timestamp": "2025-10-30T09:15:00.000",
  "code": "0000",
  "message": "성공",
  "payload": null
}
```

⚙️ **내부 처리 로직**

- 동일 상품 존재 시 `qty = qty + 1` 업데이트
- 없을 경우 새 row insert
- 예시 SQL:
    
    ```sql
    INSERT INTO cart (member_no, product_no, qty)
    VALUES (#{memberNo}, #{productNo}, 1)
    ON CONFLICT (member_no, product_no)
    DO UPDATE SET qty = cart.qty + 1;
    ```
    

⚠️ **예외 처리:**

- 존재하지 않는 상품번호 시
    
    ```json
    { "timestamp": "2025-10-30T09:15:30.000", "code": "PRODUCT_NOT_FOUND", "message": "상품 없음", "payload": null }
    ```
    

---

### 🔹 삭제 (`DELETE /api/cart`)

**Request**

```json
{ "cartIdList": [1, 2] }
```

**Response (성공)**

```json
{
  "timestamp": "2025-10-30T09:17:00.000",
  "code": "0000",
  "message": "성공",
  "payload": null
}
```

⚠️ **예외 처리:**

- 잘못된 cartId 전달 시
    
    ```json
    { "timestamp": "2025-10-30T09:17:30.000", "code": "INVALID_CART_ID", "message": "장바구니 삭제 실패", "payload": null }
    ```
    

---

## 🧾 주문

### 🔹 주문서 조회 (`GET /api/orders/form`)

**Request**

```
GET /api/orders/form?memberNo=M001
```

**Response (성공)**

```json
{
  "timestamp": "2025-10-30T09:18:00.000",
  "code": "0000",
  "message": "성공",
  "payload": {
    "memberInfo": {
      "name": "홍길동",
      "phone": "01012345678",
      "email": "[hong@test.com](mailto:hong@test.com)",
      "points": 15000
    },
    "cartList": [
      { "cartId": 1, "productNo": "P001", "productName": "셔츠", "qty": 2, "price": 29000 }
    ],
    "totalAmount": 58000,
    "availablePayments": ["CARD", "POINT"]
  }
}
```

---

### 🔹 주문 완료 조회 (`GET /api/orders/complete`)

**Request**

```
GET /api/orders/complete?orderNo=O202510300001
```

**Response (성공)**

```json
{
  "timestamp": "2025-10-30T09:19:00.000",
  "code": "0000",
  "message": "성공",
  "payload": {
    "orderNo": "O202510300001",
    "orderDate": "2025-10-30 13:45",
    "totalAmount": 45000,
    "paymentStatus": "SUCCESS",
    "items": [{ "productName": "셔츠", "price": 29000, "qty": 1 }]
  }
}
```

---

### 🔹 주문 내역 조회 (`GET /api/orders/history`)

**Request**

```
GET /api/orders/history?memberNo=M001
```

**Response (성공)**

```json
{
  "timestamp": "2025-10-30T09:20:00.000",
  "code": "0000",
  "message": "성공",
  "payload": [
    {
      "orderNo": "O202510300001",
      "orderDate": "2025-10-30",
      "totalAmount": 45000,
      "status": "COMPLETE",
      "items": [{ "productName": "셔츠", "price": 29000, "qty": 1 }]
    }
  ]
}
```

---

### 🔹 주문 상세 조회 (`GET /api/orders/{orderNo}`)

**Request**

```
GET /api/orders/O202510300001
```

**Response (성공)**

```json
{
  "timestamp": "2025-10-30T09:21:00.000",
  "code": "0000",
  "message": "성공",
  "payload": {
    "orderNo": "O202510300001",
    "orderDate": "2025-10-30 13:45",
    "orderer": { "name": "홍길동", "phone": "01012345678", "email": "[hong@test.com](mailto:hong@test.com)" },
    "items": [{ "productNo": "P001", "productName": "셔츠", "price": 29000, "qty": 1 }],
    "payments": [
      { "method": "CARD", "amount": 40000 },
      { "method": "POINT", "amount": 5000 }
    ]
  }
}
```

---

## ⚙️ 주문 프로세스 상세

### 🔸 주문번호 채번 (`POST /api/orders/sequence`)

**Request**

```json
{}
```

**Response (성공)**

```json
{
  "timestamp": "2025-10-30T09:22:00.000",
  "code": "0000",
  "message": "성공",
  "payload": { "orderNo": "O202510300001" }
}
```

⚙️ **내부 처리 로직**

- PostgreSQL 시퀀스(`seq_order_no`) 사용
- `YYYYMMDD` + 시퀀스 6자리 조합 → `O202510300001`

---

### 🔸 주문 생성 (`POST /api/orders`)

**설명:**

주문서에서 결제 수단 여러 개(예: 카드 + 적립금)로 복합 결제를 처리한다.

PG 인증 응답은 `authResult` Map 형태로 전달하며,

이니시스(`authToken`) 또는 토스(`paymentKey`) 등 PG별 구조 그대로 포함된다.

**Request**

```json
{
  "orderNo": "O202510300001",
  "memberNo": "M001",
  "ordererName": "홍길동",
  "ordererPhone": "01012345678",
  "payments": [
    {
      "pgType": "INICIS",
      "method": "CARD",
      "amount": 40000,
      "authResult": {
        "authToken": "AT123456789",
        "resultCode": "0000",
        "resultMsg": "SUCCESS"
      }
    },
    {
      "pgType": "TOSS",
      "method": "CARD",
      "amount": 5000,
      "authResult": {
        "paymentKey": "PK_ABC123456",
        "orderId": "O202510300001",
        "status": "DONE"
      }
    }
  ],
  "cartIdList": [1, 2]
}
```

**Response (성공)**

```json
{
  "timestamp": "2025-10-30T09:27:00.000",
  "code": "0000",
  "message": "성공",
  "payload": { "orderNo": "O202510300001", "paymentStatus": "SUCCESS" }
}
```

⚙️ **내부 처리 로직**

1. 주문 기본(ORDER_BASE), 주문 상품(ORDER_PRODUCT), 주문 상세(ORDER_DETAIL) 데이터 INSERT
2. 결제 서비스의 결제 메소드 호출
    1. 각 `payment` 반복 처리 (`forEach`)
    2. 반복문 안에서 전략 패턴으로 처리
        1. 결제 수단으로 POINT, CARD 중, 결제 수단 전략 선택
        2. 카드 전략 안에서 PG별로 PG 전략 선택
        3. 전략에는 결제 승인, 취소, 망취소가 있고 각 전략별 구현
        4. 토스는 망취소 API가 따로 없으므로, 같은 클래스 내 취소 메소드를 내부 호출 하는 걸로 구현
        5. 포인트는 주문 처리 실패 시, DB 롤백되므로 망취소 메소드 구현 필요 X, 빈 메소드로 두면 됨
    3. `authResult`는 `Map<String, Object>` 로 전달되어, PG별 처리 시 필요한 값을 꺼내어 사용됨
    4. 각 결제수단 승인 결과를 모두 취합하여 전체 결제 성공으로 판정된 시점에, 모든 PAYMENT 데이터를 INSERT하고, 하나라도 실패 시 이전 승인건을 망취소한다.
    5. 도중 에러 시, 예외 던짐
3. 장바구니에서 주문된 상품 삭제(트랜잭션 분리)

⚠️ **예외 처리:**

- 승인 실패, 포인트 부족 시
    
    ```json
    { "timestamp": "2025-10-30T09:27:30.000", "code": "APPROVE_FAIL", "message": "결제 승인 실패", "payload": null }
    ```
    

---

### 🔸 주문 취소 (`POST /api/orders/cancel`)

**설명:**

- 요청의 주문, 순번, 취소 수량으로 해당 주문 정보를 조회 해서 주문 상품의 판매가와 취소 수량을 기반으로 계산한 금액을 취소한다.
- 취소 금액 산정 공식(단가×수량) → 결제수단별 배분(카드 우선, 남으면 포인트) → 각 `PAYMENT.remain_refundable_amount` 업데이트

**Request**

```json
{ "orderNo": "O202510300001", "orderSeq": 1, "cancelQty": 1 }
```

**Response (성공)**

```json
{
  "timestamp": "2025-10-30T09:30:00.000",
  "code": "0000",
  "message": "성공",
  "payload": null
}
```

⚙️ **내부 처리 로직**

1. 클레임 번호 채번
2. 주문 데이터 조회
3. 주문 취소 데이터 처리 (채번한 클레임 번호 사용)
    1. 주문 상세(ORDER_DETAIL)의 해당 주문 순번(order_seq)에 대해
    - 취소 구분(주문내역구분 = '취소')으로 **새로운 row를 INSERT**
    
    (order_qty = 0, cancel_qty = 취소 수량, claim_no = 채번된 클레임 번호, parent_process_seq = 원 주문건 process_seq)
    
    - 동시에 **원 주문 row(ORDER_DETAIL.process_seq = parent_process_seq)의 cancel_qty를 누적 UPDATE**
    
    → 이로써 원 주문 행은 집계용, 새 행은 취소 이력용으로 역할 구분
    
4. 조회해 온 주문 데이터로 결제 취소 파라미터 구성  결제 취소 호출
    1. 결제하기 메소드처럼 결제 수단 리스트 반복 → 각 전략별 취소 메소드 호출
    2. 이니시스의 경우, 전체/부분 취소 API가 달라 분기 필요
    3. API 호출 시, 요청/응답 값을 PAYMENT_INTERFACE 이력 테이블에 INSERT
    
    (응답 성공/실패 여부와 무관하게 기록, 트랜잭션 분리)
    
    1. 취소 처리 후, 각 결제 수단별로 PAYMENT에 ROW INSERT
    2. 동시에 원결제 ROW의 remain_refundable_amount(환불가능금액) UPDATE

⚠️ **예외 처리:**

- 취소 금액 검증 실패 시
    
    ```json
    { "timestamp": "2025-10-30T09:30:30.000", "code": "CANCEL_FAIL", "message": "취소 불가", "payload": null }
    ```
    

---

## 💳 결제

### 🔹 인증 파라미터 조회 (`GET /api/payment/params`)

**Request**

```
GET /api/payment/params?orderNo=O202510300001&price=1000
```

**Response (성공)**

```json
{
  "timestamp": "2025-10-30T09:40:00.000",
  "code": "0000",
  "message": "성공",
  "payload": {
    "mid": "INIpayTest",
    "timestamp": "20251030134055",
    "mKey": "4a9b2b8d85...",
    "signature": "3b1e6a9e...",
    "verification": "05c6b90795..."
  }
}
```

⚙️ **파라미터 설명**

- **mid**: 스프링 설정 파일에 정의된 상점 ID
- **timestamp**: TimeInMillis (Long형)
- **signature**
    - 생성방법: NVP 방식으로 연결한 데이터를 SHA256으로 Hash한 값
    - NVP 방식: `oid=oidValue&price=priceValue&timestamp=timestampValue`
- **verification**
    - 생성방법: NVP 방식으로 연결한 데이터를 SHA256으로 Hash한 값
    - NVP 방식: `oid=oidValue&price=priceValue&signKey=signKeyValue&timestamp=timestampValue`
- **mKey**
    - signKey를 SHA256으로 Hash한 값