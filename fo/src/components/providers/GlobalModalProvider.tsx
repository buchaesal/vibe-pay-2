'use client'

import { useModalStore } from '@/store/modalStore'
import AlertModal from '@/components/ui/AlertModal'
import ConfirmModal from '@/components/ui/ConfirmModal'

export default function GlobalModalProvider() {
  const { alert, confirm, closeAlert, closeConfirm } = useModalStore()

  return (
    <>
      <AlertModal
        isOpen={alert.isOpen}
        onClose={closeAlert}
        title={alert.title}
        message={alert.message}
        type={alert.type}
      />
      <ConfirmModal
        isOpen={confirm.isOpen}
        onClose={closeConfirm}
        onConfirm={confirm.onConfirm}
        title={confirm.title}
        message={confirm.message}
        type={confirm.type}
        confirmText={confirm.confirmText}
        cancelText={confirm.cancelText}
      />
    </>
  )
}