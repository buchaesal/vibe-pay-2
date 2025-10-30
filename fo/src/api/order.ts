import axiosInstance from './axios'
import { ApiResponse } from '@/types/api'

/**
 * 회원 정보
 */
export interface MemberInfo {
  name: string
  phone: string
  email: string
  points: number
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
 * 주문서 조회 응답
 */
export interface OrderFormResponse {
  memberInfo: MemberInfo
  cartList: CartItem[]
  totalAmount: number
  availablePayments: string[]
}

/**
 * 주문번호 채번 응답
 */
export interface OrderSequenceResponse {
  orderNo: string
}

/**
 * 결제 인증 파라미터 응답
 */
export interface PaymentParamsResponse {
  mid: string
  timestamp: string
  mKey: string
  signature: string
  verification: string
}

/**
 * 주문서 조회 API
 */
export const getOrderFormApi = async (memberNo: string): Promise<OrderFormResponse> => {
  const response = await axiosInstance.get<ApiResponse<OrderFormResponse>>('/orders/form', {
    params: { memberNo },
  })
  return response.data.payload
}

/**
 * 주문번호 채번 API
 */
export const getOrderSequenceApi = async (): Promise<OrderSequenceResponse> => {
  const response = await axiosInstance.post<ApiResponse<OrderSequenceResponse>>('/orders/sequence', {})
  return response.data.payload
}

/**
 * 결제 인증 파라미터 조회 API (이니시스)
 */
export const getPaymentParamsApi = async (orderNo: string, price: number): Promise<PaymentParamsResponse> => {
  const response = await axiosInstance.get<ApiResponse<PaymentParamsResponse>>('/payment/params', {
    params: { orderNo, price },
  })
  return response.data.payload
}

/**
 * 주문 생성 요청
 */
export interface CreateOrderRequest {
  orderNo: string
  memberNo: string
  ordererName: string
  ordererPhone: string
  ordererEmail: string
  payments: Array<{
    pgType: string
    method: string
    amount: number
    authResult: any
  }>
  cartIdList: number[]
}

/**
 * 주문 생성 응답
 */
export interface CreateOrderResponse {
  orderNo: string
  paymentStatus: string
}

/**
 * 주문 생성 API
 */
export const createOrderApi = async (request: CreateOrderRequest): Promise<CreateOrderResponse> => {
  const response = await axiosInstance.post<ApiResponse<CreateOrderResponse>>('/orders', request)
  return response.data.payload
}
