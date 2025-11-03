import { create } from 'zustand'
import { Member } from '@/types/api'

interface AuthState {
  member: Member | null
  isLoggedIn: boolean
  login: (member: Member) => void
  logout: () => void
  updateMember: (member: Member) => void
  initialize: () => void
}

// localStorage에서 초기 상태 복원
const getInitialState = () => {
  if (typeof window === 'undefined') {
    return { member: null, isLoggedIn: false }
  }

  try {
    const savedMember = localStorage.getItem('member')
    if (savedMember) {
      const member = JSON.parse(savedMember) as Member
      return { member, isLoggedIn: true }
    }
  } catch (error) {
    console.error('localStorage에서 회원 정보 복원 실패:', error)
  }

  return { member: null, isLoggedIn: false }
}

export const useAuthStore = create<AuthState>((set) => ({
  ...getInitialState(),

  login: (member: Member) => {
    // localStorage에 Member 전체 객체 저장
    if (typeof window !== 'undefined') {
      localStorage.setItem('member', JSON.stringify(member))
      localStorage.setItem('memberNo', member.memberNo)
    }

    set({ member, isLoggedIn: true })
  },

  logout: () => {
    // localStorage에서 회원 정보 제거
    if (typeof window !== 'undefined') {
      localStorage.removeItem('member')
      localStorage.removeItem('memberNo')
    }

    set({ member: null, isLoggedIn: false })
  },

  updateMember: (member: Member) => {
    // localStorage에 Member 전체 객체 업데이트
    if (typeof window !== 'undefined') {
      localStorage.setItem('member', JSON.stringify(member))
    }

    set({ member, isLoggedIn: true })
  },

  initialize: () => {
    set(getInitialState())
  },
}))
