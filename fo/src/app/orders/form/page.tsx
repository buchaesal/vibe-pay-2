'use client'

import { useEffect, useRef, useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { useAuthStore } from '@/store/authStore'
import { getOrderFormApi, getOrderSequenceApi, getPaymentParamsApi, OrderFormResponse } from '@/api/order'

// PG 타입 선언
declare global {
  interface Window {
    INIStdPay: any
    TossPayments: any
  }
}

export default function OrderFormPage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const { isLoggedIn, member } = useAuthStore()
  const [orderFormData, setOrderFormData] = useState<OrderFormResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [orderNo, setOrderNo] = useState<string>('')
  const [selectedPaymentMethod, setSelectedPaymentMethod] = useState<string>('CARD')
  const [pointAmount, setPointAmount] = useState<number>(0)
  const [agreedToTerms, setAgreedToTerms] = useState<boolean>(false)
  const inicisFormRef = useRef<HTMLFormElement>(null)

  // 주문서 데이터 조회
  const fetchOrderForm = async () => {
    if (!member) return

    // URL에서 cartIdList 파라미터 읽기
    const cartIdList = searchParams.get('cartIdList')
    if (!cartIdList) {
      alert('주문할 상품을 선택해주세요.')
      router.push('/cart')
      return
    }

    try {
      setLoading(true)
      const data = await getOrderFormApi(member.memberNo, cartIdList)
      setOrderFormData(data)

      // 주문번호 채번
      const sequenceData = await getOrderSequenceApi()
      setOrderNo(sequenceData.orderNo)
    } catch (error: any) {
      console.error('주문서 조회 실패:', error)
      alert(error.message ?? '주문서를 불러오지 못했습니다.')
      router.push('/cart')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (!isLoggedIn || !member) {
      alert('로그인이 필요합니다.')
      router.push('/')
      return
    }

    fetchOrderForm()
  }, [isLoggedIn, member, router])

  // 결제하기
  const handlePayment = async () => {
    if (!orderFormData || orderFormData.cartList.length === 0) {
      alert('주문할 상품이 없습니다.')
      return
    }

    if (!member) {
      alert('로그인이 필요합니다.')
      return
    }

    const cardAmount = orderFormData.totalAmount - pointAmount

    // CASE 1: 적립금 전액 결제
    if (cardAmount === 0 && pointAmount > 0) {
      try {
        // sessionStorage에 주문 데이터 저장
        const orderData = {
          orderInfo: {
            orderNo,
            memberNo: member.memberNo,
            ordererName: orderFormData.memberInfo.name,
            ordererPhone: orderFormData.memberInfo.phone,
            ordererEmail: orderFormData.memberInfo.email,
            cartIdList: orderFormData.cartList.map((item) => item.cartId)
          },
          payments: [
            {
              pgType: 'POINT',
              method: 'POINT',
              amount: pointAmount,
              authResult: {}
            }
          ]
        }
        sessionStorage.setItem('orderData', JSON.stringify(orderData))

        // 결제 중 화면으로 이동
        router.push('/orders/processing')
      } catch (error) {
        console.error('적립금 결제 실패:', error)
        alert('결제 처리 중 오류가 발생했습니다.')
      }
      return
    }

    // CASE 2: 카드 결제 (또는 복합 결제)
    if (cardAmount > 0) {
      // 랜덤으로 이니시스 / 토스 선택 (50:50)
      const pgType = Math.random() < 0.5 ? 'INICIS' : 'TOSS'

      // 주문 데이터 sessionStorage에 저장
      const orderData = {
        orderInfo: {
          orderNo,
          memberNo: member.memberNo,
          ordererName: orderFormData.memberInfo.name,
          ordererPhone: orderFormData.memberInfo.phone,
          ordererEmail: orderFormData.memberInfo.email,
          cartIdList: orderFormData.cartList.map((item) => item.cartId)
        },
        cardAmount,
        pointAmount,
        pgType
      }
      sessionStorage.setItem('orderData', JSON.stringify(orderData))

      if (pgType === 'INICIS') {
        // 이니시스 인증
        await handleInicisPayment(cardAmount)
      } else {
        // 토스 인증
        await handleTossPayment(cardAmount)
      }
    } else {
      alert('결제 금액을 확인해주세요.')
    }
  }

  // 이니시스 JS 동적 로드
  const loadInicisScript = (): Promise<void> => {
    return new Promise((resolve, reject) => {
      // 이미 로드되어 있으면 바로 resolve
      if (window.INIStdPay) {
        console.log('이니시스 JS 이미 로드됨')
        resolve()
        return
      }

      // 스크립트가 이미 DOM에 있는지 확인
      const existingScript = document.querySelector('script[src="https://stdpay.inicis.com/stdjs/INIStdPay.js"]')
      if (existingScript) {
        // 있다면 로드 완료를 기다림
        existingScript.addEventListener('load', () => resolve())
        existingScript.addEventListener('error', () => reject(new Error('이니시스 JS 로드 실패')))
        return
      }

      // 새로 스크립트 생성 및 로드
      const script = document.createElement('script')
      script.src = 'https://stdpay.inicis.com/stdjs/INIStdPay.js'
      script.async = true
      script.onload = () => {
        console.log('이니시스 JS 로드 완료')
        resolve()
      }
      script.onerror = () => {
        reject(new Error('이니시스 JS 로드 실패'))
      }
      document.body.appendChild(script)
    })
  }

  // 이니시스 결제 처리 (Form Submit 방식)
  const handleInicisPayment = async (amount: number) => {
    try {
      // 이니시스 스크립트 로드
      await loadInicisScript()

      if (!window.INIStdPay) {
        throw new Error('결제 모듈을 불러올 수 없습니다.')
      }

      // 인증 파라미터 조회
      const params = await getPaymentParamsApi(orderNo, amount)

      // Form에 파라미터 설정
      const form = inicisFormRef.current
      if (!form) {
        throw new Error('결제 폼을 찾을 수 없습니다.')
      }

      // Form input 값 설정
      const setInputValue = (name: string, value: string | number) => {
        const input = form.querySelector(`input[name="${name}"]`) as HTMLInputElement
        if (input) {
          input.value = String(value)
        }
      }

      setInputValue('mid', params.mid)
      setInputValue('oid', orderNo)
      setInputValue('price', amount)
      setInputValue('timestamp', params.timestamp)
      setInputValue('signature', params.signature)
      setInputValue('verification', params.verification)
      setInputValue('mKey', params.mkey)
      setInputValue('goodname', orderFormData?.cartList[0]?.productName ?? '상품')
      setInputValue('buyername', member?.name ?? '')
      setInputValue('buyertel', member?.phone ?? '')
      setInputValue('buyeremail', member?.email ?? '')
      setInputValue('returnUrl', `${window.location.origin}/api/payment/inicis/callback`)
      setInputValue('closeUrl', '')

      // 이니시스 결제창 호출 (form 전달)
      window.INIStdPay.pay('inicisPayForm')

    } catch (error: any) {
      console.error('이니시스 결제 실패:', error)
      alert(error.message ?? '결제 처리 중 오류가 발생했습니다.')
    }
  }

  // 토스 결제 SDK 동적 로드
  const loadTossScript = (): Promise<void> => {
    return new Promise((resolve, reject) => {
      // 이미 로드되어 있으면 바로 resolve
      if (window.TossPayments) {
        console.log('토스 SDK 이미 로드됨')
        resolve()
        return
      }

      // 스크립트가 이미 DOM에 있는지 확인
      const existingScript = document.querySelector('script[src="https://js.tosspayments.com/v1/payment"]')
      if (existingScript) {
        existingScript.addEventListener('load', () => resolve())
        existingScript.addEventListener('error', () => reject(new Error('토스 SDK 로드 실패')))
        return
      }

      // 새로 스크립트 생성 및 로드
      const script = document.createElement('script')
      script.src = 'https://js.tosspayments.com/v1/payment'
      script.async = true
      script.onload = () => {
        console.log('토스 SDK 로드 완료')
        resolve()
      }
      script.onerror = () => {
        reject(new Error('토스 SDK 로드 실패'))
      }
      document.body.appendChild(script)
    })
  }

  // 토스 결제 처리
  const handleTossPayment = async (amount: number) => {
    try {
      // 토스 SDK 로드
      await loadTossScript()

      if (!window.TossPayments) {
        throw new Error('결제 모듈을 불러올 수 없습니다.')
      }

      // 토스페이먼츠 객체 생성
      const clientKey = 'test_ck_DpexMgkW36PL5OnYYn7drGbR5ozO'
      const tossPayments = window.TossPayments(clientKey)

      // 결제 요청
      await tossPayments.requestPayment('카드', {
        amount,
        orderId: orderNo,
        orderName: orderFormData?.cartList[0]?.productName ?? '상품',
        customerName: member?.name ?? '',
        customerEmail: member?.email ?? '',
        customerMobilePhone: member?.phone ?? '',
        successUrl: `${window.location.origin}/orders/processing`,
        failUrl: `${window.location.origin}/orders/fail`
      })

    } catch (error: any) {
      console.error('토스 결제 실패:', error)
      alert(error.message ?? '결제 처리 중 오류가 발생했습니다.')
    }
  }

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="text-center text-gray-600">로딩 중...</div>
      </div>
    )
  }

  if (!orderFormData) {
    return null
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* 헤더 */}
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">주문서</h1>
        <p className="text-sm text-gray-500 mt-2">주문번호: {orderNo}</p>
      </div>

      {/* 주문자 정보 */}
      <div className="bg-white border border-gray-200 rounded-lg p-6 mb-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">주문자 정보</h2>
        <div className="space-y-2 text-sm">
          <div className="flex">
            <span className="w-24 text-gray-600">이름</span>
            <span className="text-gray-900">{orderFormData.memberInfo.name}</span>
          </div>
          <div className="flex">
            <span className="w-24 text-gray-600">전화번호</span>
            <span className="text-gray-900">{orderFormData.memberInfo.phone}</span>
          </div>
          <div className="flex">
            <span className="w-24 text-gray-600">이메일</span>
            <span className="text-gray-900">{orderFormData.memberInfo.email}</span>
          </div>
          <div className="flex">
            <span className="w-24 text-gray-600">보유 적립금</span>
            <span className="text-blue-600 font-semibold">
              {orderFormData.memberInfo.points.toLocaleString()}원
            </span>
          </div>
        </div>
      </div>

      {/* 주문 상품 */}
      <div className="bg-white border border-gray-200 rounded-lg p-6 mb-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">주문 상품</h2>

        {orderFormData.cartList.length === 0 ? (
          <p className="text-center text-gray-600 py-8">주문할 상품이 없습니다.</p>
        ) : (
          <div className="space-y-4">
            {orderFormData.cartList.map((item) => (
              <div
                key={item.cartId}
                className="flex items-center gap-4 pb-4 border-b last:border-b-0 last:pb-0"
              >
                {/* 상품 이미지 */}
                <div className="w-20 h-20 bg-gray-100 rounded-md flex items-center justify-center flex-shrink-0">
                  <span className="text-gray-400 text-xs">이미지</span>
                </div>

                {/* 상품 정보 */}
                <div className="flex-1">
                  <h3 className="font-semibold text-gray-900">{item.productName}</h3>
                  <p className="text-sm text-gray-600 mt-1">
                    {item.price.toLocaleString()}원 × {item.qty}개
                  </p>
                </div>

                {/* 금액 */}
                <div className="text-right">
                  <p className="text-lg font-bold text-gray-900">
                    {(item.price * item.qty).toLocaleString()}원
                  </p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* 결제수단 선택 */}
      <div className="bg-white border border-gray-200 rounded-lg p-6 mb-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">결제수단</h2>

        {/* 적립금 사용 */}
        <div className="mb-4">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            적립금 사용 (보유: {orderFormData.memberInfo.points.toLocaleString()}원)
          </label>
          <div className="flex gap-2">
            <input
              type="number"
              value={pointAmount}
              onChange={(e) => {
                const value = Number(e.target.value)
                if (value < 0) {
                  setPointAmount(0)
                } else if (value > orderFormData.memberInfo.points) {
                  setPointAmount(orderFormData.memberInfo.points)
                } else if (value > orderFormData.totalAmount) {
                  setPointAmount(orderFormData.totalAmount)
                } else {
                  setPointAmount(value)
                }
              }}
              className="flex-1 px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="사용할 적립금 입력"
              min="0"
              max={Math.min(orderFormData.memberInfo.points, orderFormData.totalAmount)}
            />
            <button
              onClick={() => setPointAmount(Math.min(orderFormData.memberInfo.points, orderFormData.totalAmount))}
              className="px-4 py-2 text-sm font-medium text-white bg-gray-600 rounded-md hover:bg-gray-700 transition"
            >
              전액 사용
            </button>
          </div>
        </div>

        {/* 카드 결제 */}
        <div className="text-sm text-gray-600 mt-4">
          <div className="flex justify-between mb-2">
            <span>상품 금액</span>
            <span>{orderFormData.totalAmount.toLocaleString()}원</span>
          </div>
          <div className="flex justify-between mb-2">
            <span>사용 적립금</span>
            <span className="text-red-600">-{pointAmount.toLocaleString()}원</span>
          </div>
          <div className="flex justify-between font-semibold text-gray-900 pt-2 border-t">
            <span>카드 결제 금액</span>
            <span className="text-blue-600">
              {(orderFormData.totalAmount - pointAmount).toLocaleString()}원
            </span>
          </div>
        </div>
      </div>

      {/* 결제 정보 */}
      <div className="bg-white border border-gray-200 rounded-lg p-6 mb-6">
        <div className="space-y-3">
          <div className="flex justify-between text-gray-600">
            <span>상품 금액</span>
            <span className="font-medium">{orderFormData.totalAmount.toLocaleString()}원</span>
          </div>
          <div className="flex justify-between text-gray-600">
            <span>배송비</span>
            <span className="font-medium">0원</span>
          </div>
          <div className="border-t border-gray-200 pt-3 flex justify-between text-lg font-bold text-gray-900">
            <span>총 결제금액</span>
            <span className="text-blue-600">{orderFormData.totalAmount.toLocaleString()}원</span>
          </div>
        </div>
      </div>

      {/* 약관 동의 */}
      <div className="bg-white border border-gray-200 rounded-lg p-6 mb-6">
        <div className="space-y-3">
          <label className="flex items-center cursor-pointer">
            <input
              type="checkbox"
              checked={agreedToTerms}
              onChange={(e) => setAgreedToTerms(e.target.checked)}
              className="w-5 h-5 text-blue-600 border-gray-300 rounded focus:ring-2 focus:ring-blue-500 cursor-pointer"
            />
            <span className="ml-3 text-sm text-gray-900">
              <span className="font-semibold">[필수]</span> 주문 내용을 확인하였으며, 결제 진행에 동의합니다.
            </span>
          </label>
          <p className="text-xs text-gray-500 ml-8">
            주문할 상품의 정보, 결제 금액을 확인하였으며, 개인정보 제공 및 결제 대행 서비스 이용약관에 동의합니다.
          </p>
        </div>
      </div>

      {/* 결제 버튼 */}
      <div className="flex gap-3">
        <button
          onClick={() => router.push('/cart')}
          className="flex-1 py-3 text-lg font-semibold text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition"
        >
          장바구니로 돌아가기
        </button>
        <button
          onClick={handlePayment}
          disabled={orderFormData.cartList.length === 0 || !agreedToTerms}
          className="flex-1 py-3 text-lg font-semibold text-white bg-blue-600 rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition"
        >
          결제하기
        </button>
      </div>

      {/* 이니시스 결제 Form (숨김) */}
      <form ref={inicisFormRef} id="inicisPayForm" style={{ display: 'none' }}>
        <input type="hidden" name="version" value="1.0" />
        <input type="hidden" name="gopaymethod" value="Card" />
        <input type="hidden" name="mid" value="" />
        <input type="hidden" name="oid" value="" />
        <input type="hidden" name="price" value="" />
        <input type="hidden" name="timestamp" value="" />
        <input type="hidden" name="signature" value="" />
        <input type="hidden" name="verification" value="" />
        <input type="hidden" name="mKey" value="" />
        <input type="hidden" name="currency" value="WON" />
        <input type="hidden" name="goodname" value="" />
        <input type="hidden" name="buyername" value="" />
        <input type="hidden" name="buyertel" value="" />
        <input type="hidden" name="buyeremail" value="" />
        <input type="hidden" name="returnUrl" value="" />
        <input type="hidden" name="closeUrl" value="" />
        <input type="hidden" name="acceptmethod" value="HPP(1):below1000" />
        <input type="hidden" name="use_chkfake" value="" />
      </form>
    </div>
  )
}
