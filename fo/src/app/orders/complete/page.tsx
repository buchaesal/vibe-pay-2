'use client'

import { Suspense, useEffect, useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import axiosInstance from '@/api/axios'
import { ApiResponse } from '@/types/api'
import { useModalStore } from '@/store/modalStore'

interface OrderCompleteItem {
  productName: string
  price: number
  qty: number
}

interface PaymentInfo {
  method: string
  amount: number
}

interface OrderCompleteResponse {
  orderNo: string
  orderDate: string
  totalAmount: number
  paymentStatus: string
  items: OrderCompleteItem[]
  payments: PaymentInfo[]
}

function OrderCompleteContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const { showAlert } = useModalStore()
  const [orderData, setOrderData] = useState<OrderCompleteResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  useEffect(() => {
    if (!mounted) return

    const orderNoParam = searchParams.get('orderNo')
    if (!orderNoParam) {
      showAlert('주문 정보를 찾을 수 없습니다.', 'error', () => router.push('/'))
      return
    }

    fetchOrderComplete(orderNoParam)
  }, [mounted, searchParams, router])

  const fetchOrderComplete = async (orderNo: string) => {
    try {
      setLoading(true)
      const response = await axiosInstance.get<ApiResponse<OrderCompleteResponse>>(
        '/orders/complete',
        {
          params: { orderNo }
        }
      )
      setOrderData(response.data.payload)
    } catch (error: any) {
      console.error('주문 완료 조회 실패:', error)
      showAlert(error.message ?? '주문 정보를 불러오지 못했습니다.', 'error', () => router.push('/'))
    } finally {
      setLoading(false)
    }
  }

  // 서버/클라이언트 hydration 일치를 위해 마운트 전에는 로딩 표시
  if (!mounted || loading) {
    return (
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="text-center text-gray-600">로딩 중...</div>
      </div>
    )
  }

  if (!orderData) {
    return null
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex flex-col items-center justify-center min-h-[400px]">
        {/* 성공 아이콘 */}
        <div className="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center mb-6">
          <svg
            className="w-12 h-12 text-green-600"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M5 13l4 4L19 7"
            />
          </svg>
        </div>

        {/* 메시지 */}
        <h1 className="text-2xl font-bold text-gray-900 mb-2">주문이 완료되었습니다</h1>
        <p className="text-gray-600 mb-1">주문번호: {orderData.orderNo}</p>
        <p className="text-sm text-gray-500 mb-1">주문일시: {orderData.orderDate}</p>
        <p className="text-xl font-bold text-blue-600 mb-8">
          결제금액: {orderData.totalAmount.toLocaleString()}원
        </p>

        {/* 주문 상품 */}
        <div className="w-full max-w-md bg-white border border-gray-200 rounded-lg p-6 mb-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">주문 상품</h2>
          <div className="space-y-3">
            {orderData.items.map((item, index) => (
              <div key={index} className="flex justify-between text-sm">
                <span className="text-gray-700">
                  {item.productName} × {item.qty}
                </span>
                <span className="font-medium text-gray-900">
                  {(item.price * item.qty).toLocaleString()}원
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* 결제 정보 */}
        <div className="w-full max-w-md bg-white border border-gray-200 rounded-lg p-6 mb-8">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">결제 정보</h2>
          <div className="space-y-3">
            {orderData.payments.map((payment, index) => (
              <div key={index} className="flex justify-between text-sm">
                <span className="text-gray-700">
                  {payment.method === 'CARD' ? '카드 결제' : '적립금 사용'}
                </span>
                <span className="font-medium text-gray-900">
                  {payment.amount.toLocaleString()}원
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* 버튼 */}
        <div className="flex gap-4">
          <button
            onClick={() => router.push('/')}
            className="px-6 py-3 text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition font-medium"
          >
            메인으로
          </button>
          <button
            onClick={() => router.push('/orders/history')}
            className="px-6 py-3 text-white bg-blue-600 rounded-md hover:bg-blue-700 transition font-medium"
          >
            주문 내역 보기
          </button>
        </div>
      </div>
    </div>
  )
}

export default function OrderCompletePage() {
  return (
    <Suspense fallback={
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="text-center text-gray-600">로딩 중...</div>
      </div>
    }>
      <OrderCompleteContent />
    </Suspense>
  )
}
