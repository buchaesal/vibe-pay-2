'use client'

import { useEffect, useState } from 'react'
import { useRouter, useParams } from 'next/navigation'
import { useAuthStore } from '@/store/authStore'
import axiosInstance from '@/api/axios'
import { ApiResponse, OrderDetailResponse, OrderCancelRequest } from '@/types/api'

export default function OrderDetailPage() {
  const router = useRouter()
  const params = useParams()
  const orderNo = params.orderNo as string
  const { isLoggedIn } = useAuthStore()

  const [orderData, setOrderData] = useState<OrderDetailResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [cancelling, setCancelling] = useState(false)
  const [selectedItem, setSelectedItem] = useState<{ orderSeq: number; maxQty: number } | null>(null)
  const [cancelQty, setCancelQty] = useState(1)

  useEffect(() => {
    if (!isLoggedIn) {
      alert('로그인이 필요합니다.')
      router.push('/')
      return
    }

    fetchOrderDetail()
  }, [isLoggedIn, orderNo, router])

  const fetchOrderDetail = async () => {
    try {
      setLoading(true)
      const response = await axiosInstance.get<ApiResponse<OrderDetailResponse>>(
        `/orders/${orderNo}`
      )
      setOrderData(response.data.payload)
    } catch (error: any) {
      console.error('주문 상세 조회 실패:', error)
      alert(error.message ?? '주문 상세를 불러오지 못했습니다.')
      router.push('/orders/history')
    } finally {
      setLoading(false)
    }
  }

  const handleCancelClick = (orderSeq: number, qty: number, cancelQty: number) => {
    const maxQty = qty - cancelQty
    if (maxQty <= 0) {
      alert('취소 가능한 수량이 없습니다.')
      return
    }
    setSelectedItem({ orderSeq, maxQty })
    setCancelQty(1)
  }

  const handleCancelConfirm = async () => {
    if (!selectedItem) return

    if (cancelQty <= 0 || cancelQty > selectedItem.maxQty) {
      alert(`취소 수량은 1개 이상 ${selectedItem.maxQty}개 이하로 입력해주세요.`)
      return
    }

    if (!confirm(`${cancelQty}개를 취소하시겠습니까?`)) {
      return
    }

    try {
      setCancelling(true)
      const request: OrderCancelRequest = {
        orderNo,
        orderSeq: selectedItem.orderSeq,
        cancelQty
      }
      await axiosInstance.post<ApiResponse<void>>('/orders/cancel', request)
      alert('주문이 취소되었습니다.')
      setSelectedItem(null)
      setCancelQty(1)
      await fetchOrderDetail()
    } catch (error: any) {
      console.error('주문 취소 실패:', error)
      alert(error.message ?? '주문 취소에 실패했습니다.')
    } finally {
      setCancelling(false)
    }
  }

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="text-center text-gray-600">로딩 중...</div>
      </div>
    )
  }

  if (!orderData) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="text-center text-gray-600">주문 정보를 찾을 수 없습니다.</div>
      </div>
    )
  }

  const totalAmount = orderData.items.reduce(
    (sum, item) => sum + item.price * item.qty,
    0
  )

  const totalCancelAmount = orderData.items.reduce(
    (sum, item) => sum + item.price * item.cancelQty,
    0
  )

  // 부분취소 발생 여부 체크 (한 번이라도 부분취소가 있으면 true)
  const hasPartialCancel = orderData.items.some(item => item.cancelQty > 0)

  // 전체취소 가능 여부 (부분취소 없고, 취소 가능한 상품이 있음)
  const canFullCancel = !hasPartialCancel && orderData.items.some(item => item.qty > item.cancelQty)

  // 전체취소 처리
  const handleFullCancel = async () => {
    if (!confirm('모든 상품을 취소하시겠습니까?')) {
      return
    }

    try {
      setCancelling(true)
      const request: OrderCancelRequest = {
        orderNo,
        isFullCancel: true
      }
      await axiosInstance.post<ApiResponse<void>>('/orders/cancel', request)
      alert('주문이 전체 취소되었습니다.')
      await fetchOrderDetail()
    } catch (error: any) {
      console.error('전체 취소 실패:', error)
      alert(error.message ?? '전체 취소에 실패했습니다.')
    } finally {
      setCancelling(false)
    }
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* 헤더 */}
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">주문 상세</h1>
        <p className="text-sm text-gray-500 mt-2">주문번호: {orderData.orderNo}</p>
      </div>

      {/* 주문자 정보 */}
      <div className="bg-white border border-gray-200 rounded-lg p-6 mb-6">
        <h2 className="text-xl font-semibold text-gray-900 mb-4">주문자 정보</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <p className="text-sm text-gray-600">이름</p>
            <p className="text-base font-medium text-gray-900">{orderData.orderer.name}</p>
          </div>
          <div>
            <p className="text-sm text-gray-600">전화번호</p>
            <p className="text-base font-medium text-gray-900">{orderData.orderer.phone}</p>
          </div>
          <div>
            <p className="text-sm text-gray-600">이메일</p>
            <p className="text-base font-medium text-gray-900">{orderData.orderer.email}</p>
          </div>
        </div>
        <div className="mt-4">
          <p className="text-sm text-gray-600">주문일시</p>
          <p className="text-base font-medium text-gray-900">{orderData.orderDate}</p>
        </div>
      </div>

      {/* 주문 상품 */}
      <div className="bg-white border border-gray-200 rounded-lg p-6 mb-6">
        <h2 className="text-xl font-semibold text-gray-900 mb-4">주문 상품</h2>
        <div className="space-y-4">
          {orderData.items.map((item) => {
            const availableQty = item.qty - item.cancelQty
            return (
              <div
                key={item.orderSeq}
                className="flex items-center gap-4 pb-4 border-b border-gray-200 last:border-b-0"
              >
                {/* 상품 이미지 */}
                <div className="w-20 h-20 bg-gray-100 rounded-md flex items-center justify-center flex-shrink-0">
                  <span className="text-gray-400 text-xs">이미지</span>
                </div>

                {/* 상품 정보 */}
                <div className="flex-1">
                  <h4 className="font-semibold text-gray-900">{item.productName}</h4>
                  <p className="text-sm text-gray-600 mt-1">
                    {item.price.toLocaleString()}원 × {item.qty}개
                  </p>
                  {item.cancelQty > 0 && (
                    <p className="text-sm text-red-600 mt-1">
                      취소: {item.cancelQty}개 ({(item.price * item.cancelQty).toLocaleString()}원)
                    </p>
                  )}
                </div>

                {/* 금액 및 취소 버튼 */}
                <div className="text-right">
                  <p className="font-bold text-lg text-gray-900 mb-2">
                    {(item.price * item.qty).toLocaleString()}원
                  </p>
                  {availableQty > 0 && (
                    <button
                      onClick={() => handleCancelClick(item.orderSeq, item.qty, item.cancelQty)}
                      className="px-4 py-2 text-sm text-white bg-red-600 rounded-md hover:bg-red-700 transition disabled:bg-gray-400"
                      disabled={cancelling}
                    >
                      취소하기
                    </button>
                  )}
                  {availableQty === 0 && (
                    <span className="inline-block px-4 py-2 text-sm text-gray-600 bg-gray-100 rounded-md">
                      취소완료
                    </span>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      </div>

      {/* 결제 정보 */}
      <div className="bg-white border border-gray-200 rounded-lg p-6 mb-6">
        <h2 className="text-xl font-semibold text-gray-900 mb-4">결제 정보</h2>
        <div className="space-y-3">
          {orderData.payments.map((payment, index) => {
            const methodLabel = payment.method === 'CARD' ? '카드' : payment.method === 'POINT' ? '포인트' : payment.method
            const pgLabel = payment.pgType === 'INICIS' ? '이니시스' : payment.pgType === 'TOSS' ? '토스페이먼츠' : payment.pgType === 'POINT' ? '적립금' : payment.pgType

            return (
              <div key={index} className="flex justify-between items-center">
                <div>
                  <p className="text-gray-700">{methodLabel}</p>
                  {payment.pgType && (
                    <p className="text-sm text-gray-500 mt-1">PG: {pgLabel}</p>
                  )}
                </div>
                <p className="font-semibold text-gray-900">
                  {payment.amount.toLocaleString()}원
                </p>
              </div>
            )
          })}
          <div className="pt-3 border-t border-gray-200">
            <div className="flex justify-between items-center mb-2">
              <p className="text-gray-700">총 주문금액</p>
              <p className="font-bold text-lg text-gray-900">
                {totalAmount.toLocaleString()}원
              </p>
            </div>
            {totalCancelAmount > 0 && (
              <div className="flex justify-between items-center text-red-600">
                <p>총 취소금액</p>
                <p className="font-bold">-{totalCancelAmount.toLocaleString()}원</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* 취소 모달 */}
      {selectedItem && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <h3 className="text-xl font-semibold text-gray-900 mb-4">주문 취소</h3>
            <p className="text-sm text-gray-600 mb-4">
              취소할 수량을 선택해주세요. (최대 {selectedItem.maxQty}개)
            </p>
            <div className="mb-6">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                취소 수량
              </label>
              <input
                type="number"
                min="1"
                max={selectedItem.maxQty}
                value={cancelQty}
                onChange={(e) => setCancelQty(parseInt(e.target.value) || 1)}
                className="w-full px-4 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
            <div className="flex gap-3">
              <button
                onClick={() => {
                  setSelectedItem(null)
                  setCancelQty(1)
                }}
                className="flex-1 px-4 py-2 text-gray-700 bg-gray-100 rounded-md hover:bg-gray-200 transition"
                disabled={cancelling}
              >
                취소
              </button>
              <button
                onClick={handleCancelConfirm}
                className="flex-1 px-4 py-2 text-white bg-red-600 rounded-md hover:bg-red-700 transition disabled:bg-gray-400"
                disabled={cancelling}
              >
                {cancelling ? '처리중...' : '확인'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 하단 버튼 */}
      <div className="flex gap-4">
        <button
          onClick={() => router.push('/orders/history')}
          className="flex-1 px-6 py-3 text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition"
        >
          목록으로
        </button>
        {canFullCancel && (
          <button
            onClick={handleFullCancel}
            disabled={cancelling}
            className="flex-1 px-6 py-3 text-white bg-red-600 rounded-md hover:bg-red-700 transition disabled:bg-gray-400 disabled:cursor-not-allowed"
          >
            {cancelling ? '처리중...' : '전체취소'}
          </button>
        )}
      </div>
    </div>
  )
}
