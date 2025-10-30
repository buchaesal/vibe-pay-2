'use client'

import { Suspense, useEffect, useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'

function OrderFailContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [errorMessage, setErrorMessage] = useState<string>('주문 처리 중 오류가 발생했습니다.')

  useEffect(() => {
    const message = searchParams.get('message')
    if (message) {
      setErrorMessage(decodeURIComponent(message))
    }
  }, [searchParams])

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex flex-col items-center justify-center min-h-[400px]">
        {/* 실패 아이콘 */}
        <div className="w-20 h-20 bg-red-100 rounded-full flex items-center justify-center mb-6">
          <svg
            className="w-12 h-12 text-red-600"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M6 18L18 6M6 6l12 12"
            />
          </svg>
        </div>

        {/* 메시지 */}
        <h1 className="text-2xl font-bold text-gray-900 mb-2">주문 처리 실패</h1>
        <p className="text-gray-600 mb-8 text-center max-w-md">{errorMessage}</p>

        {/* 버튼 */}
        <div className="flex gap-4">
          <button
            onClick={() => router.push('/')}
            className="px-6 py-3 text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition font-medium"
          >
            메인으로
          </button>
          <button
            onClick={() => router.push('/cart')}
            className="px-6 py-3 text-white bg-blue-600 rounded-md hover:bg-blue-700 transition font-medium"
          >
            장바구니로 돌아가기
          </button>
        </div>
      </div>
    </div>
  )
}

export default function OrderFailPage() {
  return (
    <Suspense fallback={
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="text-center text-gray-600">로딩 중...</div>
      </div>
    }>
      <OrderFailContent />
    </Suspense>
  )
}
