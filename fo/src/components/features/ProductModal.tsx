'use client'

import { useState } from 'react'
import Modal from '@/components/ui/Modal'
import { registerProductApi } from '@/api/product'

interface ProductModalProps {
  isOpen: boolean
  onClose: () => void
  onSuccess: () => void
}

export default function ProductModal({ isOpen, onClose, onSuccess }: ProductModalProps) {
  const [productName, setProductName] = useState('')
  const [price, setPrice] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    // 가격 검증
    const priceNumber = parseInt(price)
    if (isNaN(priceNumber) || priceNumber < 0) {
      setError('올바른 가격을 입력해주세요.')
      setLoading(false)
      return
    }

    try {
      await registerProductApi({
        productName,
        price: priceNumber,
      })
      alert('상품이 등록되었습니다!')
      setProductName('')
      setPrice('')
      onSuccess()
    } catch (err: any) {
      setError(err.message ?? '상품 등록에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="상품 등록">
      <form onSubmit={handleSubmit} className="space-y-4">
        {/* 에러 메시지 */}
        {error && (
          <div className="p-3 text-sm text-red-600 bg-red-50 rounded-md">
            {error}
          </div>
        )}

        {/* 상품명 */}
        <div>
          <label htmlFor="productName" className="block text-sm font-medium text-gray-700 mb-1">
            상품명 *
          </label>
          <input
            type="text"
            id="productName"
            value={productName}
            onChange={(e) => setProductName(e.target.value)}
            required
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="상품명을 입력하세요"
          />
        </div>

        {/* 가격 */}
        <div>
          <label htmlFor="price" className="block text-sm font-medium text-gray-700 mb-1">
            판매가 *
          </label>
          <input
            type="number"
            id="price"
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            required
            min="0"
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="15000"
          />
        </div>

        {/* 버튼 */}
        <div className="flex gap-2 pt-2">
          <button
            type="button"
            onClick={onClose}
            className="flex-1 px-4 py-2 text-gray-700 bg-gray-100 rounded-md hover:bg-gray-200"
          >
            취소
          </button>
          <button
            type="submit"
            disabled={loading}
            className="flex-1 px-4 py-2 text-white bg-blue-600 rounded-md hover:bg-blue-700 disabled:bg-gray-400"
          >
            {loading ? '등록 중...' : '등록'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
