/**
 * API 공통 응답 타입
 */
export interface ApiResponse<T> {
  timestamp: string
  code: string
  message: string
  payload: T
}

/**
 * 회원 정보
 */
export interface Member {
  memberNo: string
  name: string
  email: string
  phone: string
  points: number
}

/**
 * 상품 정보
 */
export interface Product {
  productNo: string
  productName: string
  price: number
}

/**
 * 장바구니 아이템
 */
export interface CartItem {
  cartId: number
  productNo: string
  productName: string
  qty: number
  price: number
}

/**
 * 주문 정보
 */
export interface Order {
  orderNo: string
  orderDate: string
  totalAmount: number
  status: string
  items: OrderItem[]
}

/**
 * 주문 상품
 */
export interface OrderItem {
  productName: string
  price: number
  qty: number
}

/**
 * 주문 상세 응답
 */
export interface OrderDetailResponse {
  orderNo: string
  orderDate: string
  orderer: OrdererInfo
  items: OrderDetailItem[]
  payments: PaymentInfo[]
}

/**
 * 주문자 정보
 */
export interface OrdererInfo {
  name: string
  phone: string
  email: string
}

/**
 * 주문 상세 상품
 */
export interface OrderDetailItem {
  orderSeq: number
  productNo: string
  productName: string
  price: number
  qty: number
  cancelQty: number
}

/**
 * 결제 정보
 */
export interface PaymentInfo {
  pgType?: string
  method: string
  amount: number
}

/**
 * 주문 취소 요청
 */
export interface OrderCancelRequest {
  orderNo: string
  orderSeq?: number
  cancelQty?: number
  isFullCancel?: boolean
}
