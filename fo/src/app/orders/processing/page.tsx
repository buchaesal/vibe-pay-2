'use client'

import { Suspense, useEffect, useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { createOrderApi, CreateOrderRequest } from '@/api/order'

function OrderProcessingContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [isProcessing, setIsProcessing] = useState(true)
  const [message, setMessage] = useState('결제 처리 중입니다...')

  useEffect(() => {
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

      // PG 인증 결과 파싱
      const resultCode = searchParams.get('resultCode')

      // 적립금 전액 결제인 경우
      if (orderData.pgType === 'POINT' || orderData.payments) {
        // 이미 payments가 구성된 경우 (적립금 전액)
        const request: CreateOrderRequest = {
          orderNo: orderData.orderNo,
          memberNo: orderData.memberNo,
          ordererName: orderData.ordererName,
          ordererPhone: orderData.ordererPhone,
          ordererEmail: orderData.ordererEmail,
          payments: orderData.payments,
          cartIdList: orderData.cartIdList
        }

        const response = await createOrderApi(request)
        sessionStorage.removeItem('orderData')
        router.push(`/orders/complete?orderNo=${response.orderNo}`)
        return
      }

      // 카드 결제인 경우 - 인증 결과 확인
      if (resultCode !== '0000') {
        const errorMsg = searchParams.get('resultMsg') ?? '결제 인증에 실패했습니다.'
        throw new Error(errorMsg)
      }

      // 인증 결과 데이터 구성
      const authResult: any = {}

      // 이니시스 인증 결과
      if (orderData.pgType === 'INICIS') {
        authResult.resultCode = searchParams.get('resultCode')
        authResult.resultMsg = searchParams.get('resultMsg')
        authResult.mid = searchParams.get('mid')
        authResult.orderNumber = searchParams.get('orderNumber')
        authResult.authToken = searchParams.get('authToken')
        authResult.idcName = searchParams.get('idcName')
        authResult.authUrl = searchParams.get('authUrl')
        authResult.netCancelUrl = searchParams.get('netCancelUrl')
        authResult.charset = searchParams.get('charset')
        authResult.merchantData = searchParams.get('merchantData')
      }

      // 토스 인증 결과
      else if (orderData.pgType === 'TOSS') {
        // TODO: 토스 인증 결과 파싱
        authResult.paymentKey = searchParams.get('paymentKey')
        authResult.orderId = searchParams.get('orderId')
        authResult.amount = searchParams.get('amount')
      }

      // 주문 생성 요청 구성
      const payments: Array<{
        pgType: string
        method: string
        amount: number
        authResult: any
      }> = []

      // 카드 결제 추가
      payments.push({
        pgType: orderData.pgType,
        method: 'CARD',
        amount: orderData.cardAmount,
        authResult
      })

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
        orderNo: orderData.orderNo,
        memberNo: orderData.memberNo,
        ordererName: orderData.ordererName,
        ordererPhone: orderData.ordererPhone,
        ordererEmail: orderData.ordererEmail,
        payments,
        cartIdList: orderData.cartIdList
      }

      setMessage('주문을 생성하고 있습니다...')

      const response = await createOrderApi(request)

      // sessionStorage 정리
      sessionStorage.removeItem('orderData')

      // 주문 완료 화면으로 이동
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
