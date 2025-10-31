'use client'

import { Suspense, useEffect, useRef, useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { createOrderApi, CreateOrderRequest } from '@/api/order'

function OrderProcessingContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [isProcessing, setIsProcessing] = useState(true)
  const [message, setMessage] = useState('결제 처리 중입니다...')
  const hasProcessed = useRef(false)

  useEffect(() => {
    // 이미 처리되었으면 중복 실행 방지
    if (hasProcessed.current) {
      return
    }
    hasProcessed.current = true
    processOrder()
  }, [])

  const processOrder = async () => {
    try {
      // sessionStorage에서 주문 데이터 가져오기
      const orderDataStr = sessionStorage.getItem('orderData')
      if (!orderDataStr) {
        throw new Error('주문 정보를 찾을 수 없습니다.')
      }

      const orderData = JSON.parse(orderDataStr)

      // 적립금 전액 결제인 경우
      if (orderData.payments) {
        // 이미 payments가 구성된 경우 (적립금 전액)
        const request: CreateOrderRequest = {
          orderInfo: orderData.orderInfo,
          payments: orderData.payments
        }

        const response = await createOrderApi(request)
        sessionStorage.removeItem('orderData')
        router.push(`/orders/complete?orderNo=${response.orderNo}`)
        return
      }

      // 카드 결제인 경우 - PG 타입에 따라 분기
      if (orderData.pgType === 'TOSS') {
        // 토스 결제 - query params에서 응답 추출
        const paymentKey = searchParams.get('paymentKey')
        const orderId = searchParams.get('orderId')
        const amount = searchParams.get('amount')

        if (!paymentKey || !orderId || !amount) {
          throw new Error('결제 인증 정보가 올바르지 않습니다.')
        }

        // 토스 인증 결과 구성
        const authResult = {
          paymentKey,
          orderId,
          amount: parseInt(amount)
        }

        // 주문 생성 요청 구성
        const payments = [
          {
            pgType: 'TOSS',
            method: 'CARD',
            amount: orderData.cardAmount,
            authResult
          }
        ]

        // 적립금 사용 추가
        if (orderData.pointAmount > 0) {
          payments.push({
            pgType: 'POINT',
            method: 'POINT',
            amount: orderData.pointAmount,
            authResult: {}
          })
        }

        const request: CreateOrderRequest = {
          orderInfo: orderData.orderInfo,
          payments
        }

        setMessage('주문을 생성하고 있습니다...')
        const response = await createOrderApi(request)
        sessionStorage.removeItem('orderData')
        router.push(`/orders/complete?orderNo=${response.orderNo}`)
        return
      }

      // 이니시스 결제 - 인증 결과 확인
      const resultCode = searchParams.get('resultCode')
      if (resultCode !== '0000') {
        const errorMsg = searchParams.get('resultMsg') ?? '결제 인증에 실패했습니다.'
        throw new Error(errorMsg)
      }

      // 이니시스 인증 결과 구성
      const authResult = {
        resultCode: searchParams.get('resultCode'),
        resultMsg: searchParams.get('resultMsg'),
        mid: searchParams.get('mid'),
        orderNumber: searchParams.get('orderNumber'),
        authToken: searchParams.get('authToken'),
        idcName: searchParams.get('idcName'),
        authUrl: searchParams.get('authUrl'),
        netCancelUrl: searchParams.get('netCancelUrl'),
        charset: searchParams.get('charset'),
        merchantData: searchParams.get('merchantData')
      }

      // 결제 정보 구성
      const payments = [
        {
          pgType: 'INICIS',
          method: 'CARD',
          amount: orderData.cardAmount,
          authResult
        }
      ]

      // 적립금 사용 추가
      if (orderData.pointAmount > 0) {
        payments.push({
          pgType: 'POINT',
          method: 'POINT',
          amount: orderData.pointAmount,
          authResult: {}
        })
      }

      // 주문 생성 API 호출
      const request: CreateOrderRequest = {
        orderInfo: orderData.orderInfo,
        payments
      }

      setMessage('주문을 생성하고 있습니다...')
      const response = await createOrderApi(request)
      sessionStorage.removeItem('orderData')
      router.push(`/orders/complete?orderNo=${response.orderNo}`)

    } catch (error: any) {
      console.error('주문 처리 실패:', error)
      sessionStorage.removeItem('orderData')

      const errorMessage = error.message ?? '결제 처리 중 오류가 발생했습니다.'
      router.push(`/orders/fail?message=${encodeURIComponent(errorMessage)}`)
    }
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex flex-col items-center justify-center min-h-[400px]">
        {/* 로딩 스피너 */}
        <div className="w-16 h-16 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin mb-6"></div>

        {/* 메시지 */}
        <h2 className="text-xl font-semibold text-gray-900 mb-2">{message}</h2>
        <p className="text-sm text-gray-600">잠시만 기다려주세요.</p>
      </div>
    </div>
  )
}

export default function OrderProcessingPage() {
  return (
    <Suspense fallback={
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex flex-col items-center justify-center min-h-[400px]">
          <div className="w-16 h-16 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin mb-6"></div>
          <h2 className="text-xl font-semibold text-gray-900 mb-2">로딩 중...</h2>
        </div>
      </div>
    }>
      <OrderProcessingContent />
    </Suspense>
  )
}
