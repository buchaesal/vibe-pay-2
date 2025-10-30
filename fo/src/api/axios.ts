import axios from 'axios'

const axiosInstance = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 응답 인터셉터
axiosInstance.interceptors.response.use(
  (response) => {
    const { data } = response

    // Response<T> 형식 검증
    if (data && data.code === '0000') {
      return response
    }

    // 에러 처리
    throw new Error(data?.message ?? '알 수 없는 오류가 발생했습니다.')
  },
  (error) => {
    console.error('API 에러:', error)

    if (error.response) {
      const { data } = error.response
      throw new Error(data?.message ?? '서버 오류가 발생했습니다.')
    } else if (error.request) {
      throw new Error('서버와의 연결에 실패했습니다.')
    } else {
      throw new Error('요청 처리 중 오류가 발생했습니다.')
    }
  }
)

export default axiosInstance
