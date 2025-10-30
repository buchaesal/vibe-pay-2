import axiosInstance from './axios'
import { ApiResponse, Product } from '@/types/api'

/**
 * 상품 등록 요청 타입
 */
export interface ProductRegisterRequest {
  productName: string
  price: number
}

/**
 * 장바구니 담기 요청 타입
 */
export interface AddToCartRequest {
  memberNo: string
  productNoList: string[]
}

/**
 * 상품 목록 조회 API
 */
export const getProductListApi = async (): Promise<Product[]> => {
  const response = await axiosInstance.get<ApiResponse<Product[]>>('/products')
  return response.data.payload
}

/**
 * 상품 등록 API
 */
export const registerProductApi = async (data: ProductRegisterRequest): Promise<void> => {
  await axiosInstance.post<ApiResponse<null>>('/products', data)
}

/**
 * 장바구니 담기 API
 */
export const addToCartApi = async (data: AddToCartRequest): Promise<void> => {
  await axiosInstance.post<ApiResponse<null>>('/cart', data)
}
