'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { useAuthStore } from '@/store/authStore'
import axiosInstance from '@/api/axios'
import { ApiResponse } from '@/types/api'

interface OrderHistoryItem {
  orderSeq: number
  productNo: string
  productName: string
  price: number
  qty: number
  cancelQty: number
}

interface OrderHistoryResponse {
  orderNo: string
  orderDate: string
  totalAmount: number
  status: string
  items: OrderHistoryItem[]
}

export default function OrderHistoryPage() {
  const router = useRouter()
  const { isLoggedIn, member } = useAuthStore()
  const [orders, setOrders] = useState<OrderHistoryResponse[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!isLoggedIn || !member) {
      alert('로그인이 필요합니다.')
      router.push('/')
      return
    }

    fetchOrderHistory()
  }, [isLoggedIn, member, router])

  const fetchOrderHistory = async () => {
    if (!member) return

    try {
      setLoading(true)
      const response = await axiosInstance.get<ApiResponse<OrderHistoryResponse[]>>(
        '/orders/history',
        {
          params: { memberNo: member.memberNo }
        }
      )
      setOrders(response.data.payload)
    } catch (error: any) {
      console.error('주문 내역 조회 실패:', error)
      alert(error.message ?? '주문 내역을 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const handleOrderClick = (orderNo: string) => {
    router.push(`/orders/${orderNo}`)
  }

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="text-center text-gray-600">로딩 중...</div>
      </div>
    )
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* 헤더 */}
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">주문 내역</h1>
        <p className="text-sm text-gray-500 mt-2">주문하신 상품의 내역을 확인하실 수 있습니다.</p>
      </div>

      {/* 주문 목록 */}
      {orders.length === 0 ? (
        <div className="bg-white border border-gray-200 rounded-lg p-12 text-center">
          <p className="text-gray-600 mb-4">주문 내역이 없습니다.</p>
          <button
            onClick={() => router.push('/')}
            className="px-6 py-2 text-white bg-blue-600 rounded-md hover:bg-blue-700 transition"
          >
            쇼핑 계속하기
          </button>
        </div>
      ) : (
        <div className="space-y-4">
          {orders.map((order) => (
            <div
              key={order.orderNo}
              className="bg-white border border-gray-200 rounded-lg p-6 hover:shadow-md transition cursor-pointer"
              onClick={() => handleOrderClick(order.orderNo)}
            >
              {/* 주문 헤더 */}
              <div className="flex justify-between items-center mb-4 pb-4 border-b border-gray-200">
                <div>
                  <h3 className="text-lg font-semibold text-gray-900">
                    주문번호: {order.orderNo}
                  </h3>
                  <p className="text-sm text-gray-500 mt-1">{order.orderDate}</p>
                </div>
                <div className="text-right">
                  <span
                    className={`inline-block px-3 py-1 text-sm font-medium rounded-full ${
                      order.status === 'COMPLETE'
                        ? 'bg-green-100 text-green-800'
                        : 'bg-red-100 text-red-800'
                    }`}
                  >
                    {order.status === 'COMPLETE' ? '주문완료' : '취소완료'}
                  </span>
                  <p className="text-lg font-bold text-gray-900 mt-2">
                    {order.totalAmount.toLocaleString()}원
                  </p>
                </div>
              </div>

              {/* 상품 목록 */}
              <div className="space-y-3">
                {order.items.map((item) => (
                  <div
                    key={`${order.orderNo}-${item.orderSeq}`}
                    className="flex items-center gap-4"
                  >
                    {/* 상품 이미지 */}
                    <div className="w-16 h-16 bg-gray-100 rounded-md flex items-center justify-center flex-shrink-0">
                      <span className="text-gray-400 text-xs">이미지</span>
                    </div>

                    {/* 상품 정보 */}
                    <div className="flex-1">
                      <h4 className="font-medium text-gray-900">{item.productName}</h4>
                      <p className="text-sm text-gray-600 mt-1">
                        {item.price.toLocaleString()}원 × {item.qty}개
                        {item.cancelQty > 0 && (
                          <span className="text-red-600 ml-2">
                            (취소: {item.cancelQty}개)
                          </span>
                        )}
                      </p>
                    </div>

                    {/* 금액 */}
                    <div className="text-right">
                      <p className="font-semibold text-gray-900">
                        {(item.price * item.qty).toLocaleString()}원
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
