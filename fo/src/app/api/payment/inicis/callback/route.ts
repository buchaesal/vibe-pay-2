import { NextRequest, NextResponse } from 'next/server'

/**
 * 이니시스 결제 인증 콜백 Route Handler
 *
 * - 이니시스 인증 성공 시 POST로 호출됨
 * - Form Data로 인증 결과를 수신
 * - QueryString으로 변환하여 결제 중 화면으로 redirect
 */
export async function POST(request: NextRequest) {
  try {
    // Form Data 파싱
    const formData = await request.formData()

    // 인증 결과 데이터 추출
    const resultCode = formData.get('resultCode')
    const resultMsg = formData.get('resultMsg')
    const mid = formData.get('mid')
    const orderNumber = formData.get('orderNumber')
    const authToken = formData.get('authToken')
    const idcName = formData.get('idc_name')
    const authUrl = formData.get('authUrl')
    const netCancelUrl = formData.get('netCancelUrl')
    const charset = formData.get('charset')
    const merchantData = formData.get('merchantData')

    console.log('이니시스 인증 결과 수신:', {
      resultCode,
      resultMsg,
      orderNumber,
      authToken
    })

    // 인증 실패
    if (resultCode !== '0000') {
      console.error('이니시스 인증 실패:', resultCode, resultMsg)
      const errorParams = new URLSearchParams({
        error: 'true',
        message: resultMsg?.toString() ?? '인증에 실패했습니다.'
      })
      return NextResponse.redirect(new URL(`/orders/fail?${errorParams.toString()}`, request.url))
    }

    // 인증 성공 - QueryString 생성
    const params = new URLSearchParams({
      resultCode: resultCode.toString(),
      resultMsg: resultMsg?.toString() ?? '',
      mid: mid?.toString() ?? '',
      orderNumber: orderNumber?.toString() ?? '',
      authToken: authToken?.toString() ?? '',
      idcName: idcName?.toString() ?? '',
      authUrl: authUrl?.toString() ?? '',
      netCancelUrl: netCancelUrl?.toString() ?? '',
      charset: charset?.toString() ?? 'UTF-8',
      merchantData: merchantData?.toString() ?? ''
    })

    // 결제 중 화면으로 redirect
    const redirectUrl = new URL(`/orders/processing?${params.toString()}`, request.url)
    return NextResponse.redirect(redirectUrl)

  } catch (error) {
    console.error('이니시스 콜백 처리 실패:', error)
    return NextResponse.redirect(new URL('/orders/fail?error=true&message=결제 처리 중 오류가 발생했습니다.', request.url))
  }
}
