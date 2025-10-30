'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { useAuthStore } from '@/store/authStore'
import { getCartListApi, deleteCartApi } from '@/api/cart'
import { CartItem } from '@/types/api'

export default function CartPage() {
  const router = useRouter()
  const { isLoggedIn, member } = useAuthStore()
  const [cartItems, setCartItems] = useState<CartItem[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedCartIds, setSelectedCartIds] = useState<number[]>([])

  // 장바구니 목록 조회
  const fetchCartList = async () => {
    if (!member) return

    try {
      setLoading(true)
      const data = await getCartListApi(member.memberNo)
      setCartItems(data)
    } catch (error: any) {
      console.error('장바구니 목록 조회 실패:', error)
      alert(error.message ?? '장바구니 목록을 불러오지 못했습니다.')
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

    fetchCartList()
  }, [isLoggedIn, member, router])

  // 체크박스 토글
  const handleToggleCartItem = (cartId: number) => {
    setSelectedCartIds(prev =>
      prev.includes(cartId)
        ? prev.filter(id => id !== cartId)
        : [...prev, cartId]
    )
  }

  // 전체 선택/해제
  const handleToggleAll = () => {
    if (selectedCartIds.length === cartItems.length) {
      setSelectedCartIds([])
    } else {
      setSelectedCartIds(cartItems.map(item => item.cartId))
    }
  }

  // 선택 삭제
  const handleDeleteSelected = async () => {
    if (selectedCartIds.length === 0) {
      alert('삭제할 상품을 선택해주세요.')
      return
    }

    if (!confirm(`선택한 ${selectedCartIds.length}개의 상품을 삭제하시겠습니까?`)) {
      return
    }

    try {
      await deleteCartApi({ cartIdList: selectedCartIds })
      alert('선택한 상품이 삭제되었습니다.')
      setSelectedCartIds([])
      fetchCartList()
    } catch (error: any) {
      alert(error.message ?? '상품 삭제에 실패했습니다.')
    }
  }

  // 총 금액 계산
  const totalAmount = cartItems
    .filter(item => selectedCartIds.includes(item.cartId))
    .reduce((sum, item) => sum + item.price * item.qty, 0)

  // 주문하기
  const handleOrder = () => {
    if (selectedCartIds.length === 0) {
      alert('주문할 상품을 선택해주세요.')
      return
    }

    router.push('/orders/form')
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
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-900">장바구니</h1>
        <button
          onClick={() => router.push('/')}
          className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
        >
          쇼핑 계속하기
        </button>
      </div>

      {/* 장바구니 목록 */}
      {cartItems.length === 0 ? (
        <div className="bg-white border border-gray-200 rounded-lg p-12 text-center">
          <p className="text-gray-600 mb-4">장바구니가 비어 있습니다.</p>
          <button
            onClick={() => router.push('/')}
            className="px-6 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700"
          >
            상품 보러가기
          </button>
        </div>
      ) : (
        <>
          {/* 전체 선택 & 삭제 */}
          <div className="bg-white border border-gray-200 rounded-t-lg px-6 py-4 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={selectedCartIds.length === cartItems.length}
                onChange={handleToggleAll}
                className="w-5 h-5 text-blue-600 rounded focus:ring-2 focus:ring-blue-500"
              />
              <span className="text-sm font-medium text-gray-700">
                전체 선택 ({selectedCartIds.length}/{cartItems.length})
              </span>
            </div>
            <button
              onClick={handleDeleteSelected}
              disabled={selectedCartIds.length === 0}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              선택 삭제
            </button>
          </div>

          {/* 상품 목록 */}
          <div className="bg-white border-x border-b border-gray-200 rounded-b-lg divide-y divide-gray-200">
            {cartItems.map(item => (
              <div
                key={item.cartId}
                className="px-6 py-4 flex items-center gap-4 hover:bg-gray-50 transition"
              >
                {/* 체크박스 */}
                <input
                  type="checkbox"
                  checked={selectedCartIds.includes(item.cartId)}
                  onChange={() => handleToggleCartItem(item.cartId)}
                  className="w-5 h-5 text-blue-600 rounded focus:ring-2 focus:ring-blue-500"
                />

                {/* 상품 이미지 */}
                <div className="w-24 h-24 bg-gray-100 rounded-md flex items-center justify-center flex-shrink-0">
                  <span className="text-gray-400 text-xs">이미지</span>
                </div>

                {/* 상품 정보 */}
                <div className="flex-1">
                  <h3 className="text-lg font-semibold text-gray-900 mb-1">
                    {item.productName}
                  </h3>
                  <p className="text-sm text-gray-600">
                    수량: {item.qty}개
                  </p>
                </div>

                {/* 가격 */}
                <div className="text-right">
                  <p className="text-xl font-bold text-blue-600">
                    {(item.price * item.qty).toLocaleString()}원
                  </p>
                  <p className="text-sm text-gray-500 mt-1">
                    개당 {item.price.toLocaleString()}원
                  </p>
                </div>
              </div>
            ))}
          </div>

          {/* 결제 정보 */}
          <div className="mt-8 bg-white border border-gray-200 rounded-lg p-6">
            <div className="space-y-3">
              <div className="flex justify-between text-gray-600">
                <span>선택 상품 금액</span>
                <span className="font-medium">{totalAmount.toLocaleString()}원</span>
              </div>
              <div className="flex justify-between text-gray-600">
                <span>배송비</span>
                <span className="font-medium">0원</span>
              </div>
              <div className="border-t border-gray-200 pt-3 flex justify-between text-lg font-bold text-gray-900">
                <span>총 결제금액</span>
                <span className="text-blue-600">{totalAmount.toLocaleString()}원</span>
              </div>
            </div>

            <button
              onClick={handleOrder}
              disabled={selectedCartIds.length === 0}
              className="w-full mt-6 py-3 text-lg font-semibold text-white bg-blue-600 rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition"
            >
              {selectedCartIds.length > 0
                ? `주문하기 (${selectedCartIds.length}개)`
                : '상품을 선택해주세요'}
            </button>
          </div>
        </>
      )}
    </div>
  )
}
