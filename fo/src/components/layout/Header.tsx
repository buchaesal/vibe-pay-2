'use client'

import { useState } from 'react'
import { useAuthStore } from '@/store/authStore'
import Link from 'next/link'
import LoginModal from '@/components/features/LoginModal'
import RegisterModal from '@/components/features/RegisterModal'

export default function Header() {
  const { isLoggedIn, member, logout } = useAuthStore()
  const [showLoginModal, setShowLoginModal] = useState(false)
  const [showRegisterModal, setShowRegisterModal] = useState(false)

  const handleLogout = () => {
    logout()
    window.location.href = '/'
  }

  const handleSwitchToRegister = () => {
    setShowLoginModal(false)
    setShowRegisterModal(true)
  }

  const handleSwitchToLogin = () => {
    setShowRegisterModal(false)
    setShowLoginModal(true)
  }

  return (
    <>
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            {/* 로고 */}
            <Link href="/" className="text-xl font-bold text-gray-900">
              VibePay
            </Link>

            {/* 네비게이션 */}
            <nav className="flex items-center gap-4">
              {isLoggedIn ? (
                <>
                  <span className="text-sm text-gray-600">
                    {member?.name}님 (적립금: {member?.points?.toLocaleString()}원)
                  </span>
                  <Link
                    href="/orders/history"
                    className="px-4 py-2 text-sm font-medium text-gray-700 hover:text-gray-900"
                  >
                    주문내역
                  </Link>
                  <Link
                    href="/cart"
                    className="px-4 py-2 text-sm font-medium text-gray-700 hover:text-gray-900"
                  >
                    장바구니
                  </Link>
                  <button
                    onClick={handleLogout}
                    className="px-4 py-2 text-sm font-medium text-white bg-gray-600 rounded-md hover:bg-gray-700"
                  >
                    로그아웃
                  </button>
                </>
              ) : (
                <>
                  <button
                    onClick={() => setShowLoginModal(true)}
                    className="px-4 py-2 text-sm font-medium text-gray-700 hover:text-gray-900"
                  >
                    로그인
                  </button>
                  <button
                    onClick={() => setShowRegisterModal(true)}
                    className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700"
                  >
                    회원가입
                  </button>
                </>
              )}
            </nav>
          </div>
        </div>
      </header>

      {/* 모달 */}
      <LoginModal
        isOpen={showLoginModal}
        onClose={() => setShowLoginModal(false)}
        onSwitchToRegister={handleSwitchToRegister}
      />
      <RegisterModal
        isOpen={showRegisterModal}
        onClose={() => setShowRegisterModal(false)}
        onSwitchToLogin={handleSwitchToLogin}
      />
    </>
  )
}
