'use client'

import { useEffect, useState } from 'react'
import { useAuthStore } from '@/store/authStore'
import { useModalStore } from '@/store/modalStore'
import { getProductListApi, addToCartApi } from '@/api/product'
import { Product } from '@/types/api'
import ProductModal from '@/components/features/ProductModal'

export default function HomePage() {
  const { isLoggedIn, member } = useAuthStore()
  const { showAlert } = useModalStore()
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedProducts, setSelectedProducts] = useState<string[]>([])
  const [showProductModal, setShowProductModal] = useState(false)

  // 상품 목록 조회
  const fetchProducts = async () => {
    try {
      setLoading(true)
      const data = await getProductListApi()
      setProducts(data)
    } catch (error: any) {
      console.error('상품 목록 조회 실패:', error)
      showAlert(error.message ?? '상품 목록을 불러오지 못했습니다.', 'error')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchProducts()
  }, [])

  // 체크박스 토글
  const handleToggleProduct = (productNo: string) => {
    setSelectedProducts(prev =>
      prev.includes(productNo)
        ? prev.filter(no => no !== productNo)
        : [...prev, productNo]
    )
  }

  // 장바구니 담기
  const handleAddToCart = async () => {
    if (!isLoggedIn || !member) {
      showAlert('로그인이 필요합니다.', 'warning')
      return
    }

    if (selectedProducts.length === 0) {
      showAlert('상품을 선택해주세요.', 'warning')
      return
    }

    try {
      await addToCartApi({
        memberNo: member.memberNo,
        productNoList: selectedProducts,
      })
      showAlert('장바구니에 담았습니다!', 'success')
      setSelectedProducts([])
    } catch (error: any) {
      showAlert(error.message ?? '장바구니 담기에 실패했습니다.', 'error')
    }
  }

  // 상품 등록 성공 후 리스트 재조회
  const handleProductRegistered = () => {
    setShowProductModal(false)
    fetchProducts()
  }

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="text-center text-gray-600">로딩 중...</div>
      </div>
    )
  }

  return (
    <>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* 헤더 */}
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-3xl font-bold text-gray-900">상품 목록</h1>
          <div className="flex gap-2">
            {selectedProducts.length > 0 && (
              <button
                onClick={handleAddToCart}
                className="px-4 py-2 text-sm font-medium text-white bg-green-600 rounded-md hover:bg-green-700"
              >
                장바구니 담기 ({selectedProducts.length})
              </button>
            )}
            <button
              onClick={() => setShowProductModal(true)}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700"
            >
              상품 등록
            </button>
          </div>
        </div>

        {/* 상품 목록 */}
        {products.length === 0 ? (
          <div className="text-center text-gray-600 py-12">
            등록된 상품이 없습니다.
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {products.map(product => (
              <div
                key={product.productNo}
                className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition"
              >
                {/* 체크박스 */}
                <div className="flex items-start justify-between mb-3">
                  <input
                    type="checkbox"
                    checked={selectedProducts.includes(product.productNo)}
                    onChange={() => handleToggleProduct(product.productNo)}
                    className="w-5 h-5 text-blue-600 rounded focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                {/* 상품 이미지 (임시) */}
                <div className="w-full h-48 bg-gray-100 rounded-md mb-3 flex items-center justify-center">
                  <span className="text-gray-400 text-sm">이미지 없음</span>
                </div>

                {/* 상품 정보 */}
                <h3 className="text-lg font-semibold text-gray-900 mb-2 truncate">
                  {product.productName}
                </h3>
                <p className="text-xl font-bold text-blue-600">
                  {product.price.toLocaleString()}원
                </p>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* 상품 등록 모달 */}
      <ProductModal
        isOpen={showProductModal}
        onClose={() => setShowProductModal(false)}
        onSuccess={handleProductRegistered}
      />
    </>
  )
}
