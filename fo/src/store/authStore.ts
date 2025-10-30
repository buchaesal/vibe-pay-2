import { create } from 'zustand'
import { Member } from '@/types/api'

interface AuthState {
  member: Member | null
  isLoggedIn: boolean
  login: (member: Member) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  member: null,
  isLoggedIn: false,

  login: (member: Member) => {
    // localStorage에 memberNo 저장
    if (typeof window !== 'undefined') {
      localStorage.setItem('memberNo', member.memberNo)
    }

    set({ member, isLoggedIn: true })
  },

  logout: () => {
    // localStorage에서 memberNo 제거
    if (typeof window !== 'undefined') {
      localStorage.removeItem('memberNo')
    }

    set({ member: null, isLoggedIn: false })
  },
}))
