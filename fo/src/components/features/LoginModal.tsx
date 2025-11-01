'use client'

import { useState } from 'react'
import Modal from '@/components/ui/Modal'
import { loginApi } from '@/api/auth'
import { useAuthStore } from '@/store/authStore'
import { useModalStore } from '@/store/modalStore'

interface LoginModalProps {
  isOpen: boolean
  onClose: () => void
  onSwitchToRegister: () => void
}

export default function LoginModal({ isOpen, onClose, onSwitchToRegister }: LoginModalProps) {
  const [loginId, setLoginId] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const { login } = useAuthStore()
  const { showAlert } = useModalStore()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      const member = await loginApi({ loginId, password })
      login(member)
      showAlert('로그인 성공!', 'success')
      onClose()
      setLoginId('')
      setPassword('')
    } catch (err: any) {
      setError(err.message ?? '로그인에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="로그인">
      <form onSubmit={handleSubmit} className="space-y-4">
        {/* 에러 메시지 */}
        {error && (
          <div className="p-3 text-sm text-red-600 bg-red-50 rounded-md">
            {error}
          </div>
        )}

        {/* 아이디 */}
        <div>
          <label htmlFor="loginId" className="block text-sm font-medium text-gray-700 mb-1">
            아이디
          </label>
          <input
            type="text"
            id="loginId"
            value={loginId}
            onChange={(e) => setLoginId(e.target.value)}
            required
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="아이디를 입력하세요"
          />
        </div>

        {/* 비밀번호 */}
        <div>
          <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">
            비밀번호
          </label>
          <input
            type="password"
            id="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="비밀번호를 입력하세요"
          />
        </div>

        {/* 버튼 */}
        <div className="flex flex-col gap-2 pt-2">
          <button
            type="submit"
            disabled={loading}
            className="w-full px-4 py-2 text-white bg-blue-600 rounded-md hover:bg-blue-700 disabled:bg-gray-400 transition"
          >
            {loading ? '로그인 중...' : '로그인'}
          </button>

          <button
            type="button"
            onClick={onSwitchToRegister}
            className="w-full px-4 py-2 text-sm text-gray-600 hover:text-gray-900"
          >
            회원가입하기
          </button>
        </div>
      </form>
    </Modal>
  )
}
