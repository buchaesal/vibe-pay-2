import axiosInstance from './axios'
import { ApiResponse, CartItem } from '@/types/api'

/**
 * 장바구니 삭제 요청 타입
 */
export interface DeleteCartRequest {
  cartIdList: number[]
}

/**
 * 장바구니 수량 변경 요청 타입
 */
export interface UpdateCartQtyRequest {
  cartId: number
  qty: number
}

/**
 * 장바구니 목록 조회 API
 */
export const getCartListApi = async (memberNo: string): Promise<CartItem[]> => {
  const response = await axiosInstance.get<ApiResponse<CartItem[]>>('/cart', {
    params: { memberNo },
  })
  return response.data.payload
}

/**
 * 장바구니 수량 변경 API
 */
export const updateCartQtyApi = async (data: UpdateCartQtyRequest): Promise<void> => {
  await axiosInstance.put<ApiResponse<null>>('/cart/qty', data)
}

/**
 * 장바구니 삭제 API
 */
export const deleteCartApi = async (data: DeleteCartRequest): Promise<void> => {
  await axiosInstance.delete<ApiResponse<null>>('/cart', {
    data,
  })
}
