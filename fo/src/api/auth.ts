import axiosInstance from './axios'
import { ApiResponse, Member } from '@/types/api'

/**
 * 로그인 요청 타입
 */
export interface LoginRequest {
  loginId: string
  password: string
}

/**
 * 회원가입 요청 타입
 */
export interface RegisterRequest {
  loginId: string
  password: string
  name: string
  email: string
  phone: string
}

/**
 * 로그인 API
 */
export const loginApi = async (data: LoginRequest): Promise<Member> => {
  const response = await axiosInstance.post<ApiResponse<Member>>('/auth/login', data)
  return response.data.payload
}

/**
 * 회원가입 API
 */
export const registerApi = async (data: RegisterRequest): Promise<void> => {
  await axiosInstance.post<ApiResponse<null>>('/members', data)
}
