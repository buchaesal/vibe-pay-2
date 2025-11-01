import { create } from 'zustand'

type AlertType = 'success' | 'error' | 'warning' | 'info'
type ConfirmType = 'danger' | 'warning' | 'info'

interface AlertState {
  isOpen: boolean
  title?: string
  message: string
  type: AlertType
}

interface ConfirmState {
  isOpen: boolean
  title?: string
  message: string
  type: ConfirmType
  confirmText?: string
  cancelText?: string
  onConfirm: () => void
}

interface ModalStore {
  alert: AlertState
  confirm: ConfirmState
  showAlert: (message: string, type?: AlertType, title?: string) => void
  closeAlert: () => void
  showConfirm: (
    message: string,
    onConfirm: () => void,
    options?: {
      type?: ConfirmType
      title?: string
      confirmText?: string
      cancelText?: string
    }
  ) => void
  closeConfirm: () => void
}

export const useModalStore = create<ModalStore>((set) => ({
  alert: {
    isOpen: false,
    message: '',
    type: 'info',
  },
  confirm: {
    isOpen: false,
    message: '',
    type: 'info',
    onConfirm: () => {},
  },
  showAlert: (message, type = 'info', title) =>
    set({
      alert: {
        isOpen: true,
        message,
        type,
        title,
      },
    }),
  closeAlert: () =>
    set((state) => ({
      alert: {
        ...state.alert,
        isOpen: false,
      },
    })),
  showConfirm: (message, onConfirm, options = {}) =>
    set({
      confirm: {
        isOpen: true,
        message,
        type: options.type || 'info',
        title: options.title,
        confirmText: options.confirmText,
        cancelText: options.cancelText,
        onConfirm,
      },
    }),
  closeConfirm: () =>
    set((state) => ({
      confirm: {
        ...state.confirm,
        isOpen: false,
      },
    })),
}))