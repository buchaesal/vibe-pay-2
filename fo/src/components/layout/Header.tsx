'use client'

import { useState } from 'react'
import { useAuthStore } from '@/store/authStore'
import Link from 'next/link'
import LoginModal from '@/components/features/LoginModal'
import RegisterModal from '@/components/features/RegisterModal'
import axiosInstance from '@/api/axios'
import type { Member } from '@/types/auth'

interface ApiResponse<T> {
  timestamp: string
  code: string
  message: string
  payload: T
}

export default function Header() {
  const { isLoggedIn, member, logout, updateMember } = useAuthStore()
  const [showLoginModal, setShowLoginModal] = useState(false)
  const [showRegisterModal, setShowRegisterModal] = useState(false)
  const [isRefreshing, setIsRefreshing] = useState(false)

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

  const handleRefreshPoints = async () => {
    if (!member?.memberNo || isRefreshing) return

    setIsRefreshing(true)
    try {
      const response = await axiosInstance.get<ApiResponse<Member>>(
        `/members/${member.memberNo}`
      )
      if (response.data.code === '0000' && response.data.payload) {
        updateMember(response.data.payload)
      }
    } catch (error) {
      console.error('적립금 갱신 실패:', error)
    } finally {
      setIsRefreshing(false)
    }
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
            <nav className="flex items-center gap-1">
              {isLoggedIn ? (
                <>
                  <span className="px-3 py-2 text-sm font-medium text-gray-700">
                    {member?.name}님
                  </span>
                  <div className="flex items-center gap-1 px-3 py-2">
                    <span className="text-sm font-medium text-blue-600">
                      적립금: {member?.points?.toLocaleString() ?? 0}원
                    </span>
                    <button
                      onClick={handleRefreshPoints}
                      disabled={isRefreshing}
                      className="p-1 hover:bg-gray-100 rounded-full transition disabled:opacity-50"
                      title="적립금 새로고침"
                    >
                      <svg
                        className={`w-4 h-4 text-gray-500 ${isRefreshing ? 'animate-spin' : ''}`}
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
                        />
                      </svg>
                    </button>
                  </div>
                  <div className="w-px h-4 bg-gray-300 mx-1"></div>
                  <Link
                    href="/orders/history"
                    className="px-3 py-2 text-sm font-medium text-gray-700 hover:text-gray-900 hover:bg-gray-50 rounded-md transition"
                  >
                    주문내역
                  </Link>
                  <Link
                    href="/cart"
                    className="px-3 py-2 text-sm font-medium text-gray-700 hover:text-gray-900 hover:bg-gray-50 rounded-md transition"
                  >
                    장바구니
                  </Link>
                  <div className="w-px h-4 bg-gray-300 mx-1"></div>
                  <button
                    onClick={handleLogout}
                    className="px-4 py-2 text-sm font-medium text-white bg-gray-600 rounded-md hover:bg-gray-700 transition ml-1"
                  >
                    로그아웃
                  </button>
                </>
              ) : (
                <>
                  <button
                    onClick={() => setShowLoginModal(true)}
                    className="px-4 py-2 text-sm font-medium text-gray-700 hover:text-gray-900 hover:bg-gray-50 rounded-md transition"
                  >
                    로그인
                  </button>
                  <button
                    onClick={() => setShowRegisterModal(true)}
                    className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 transition"
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
