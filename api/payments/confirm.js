// Vercel 서버리스 함수: 토스 결제 승인 (POST /api/payments/confirm)
// 시크릿 키는 Vercel 환경변수 TOSS_SECRET_KEY 에만 저장합니다. (VITE_ 접두사 금지: 브라우저에 노출됨)
// 승인 성공 후 재고 차감/주문 기록은 기존 Spring 백엔드의 /api/payments/cash 를 호출해 처리합니다.

const TOSS_CONFIRM_URL = 'https://api.tosspayments.com/v1/payments/confirm';
const BACKEND_API_URL = process.env.BACKEND_API_URL || 'https://malmal-coffee.duckdns.org';

export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  if (req.method === 'OPTIONS') return res.status(204).end();
  if (req.method !== 'POST') return res.status(405).json({ message: 'Method Not Allowed' });

  const secretKey = process.env.TOSS_SECRET_KEY;
  if (!secretKey) {
    return res.status(500).json({ message: 'TOSS_SECRET_KEY 환경변수가 설정되지 않았습니다.' });
  }

  const { paymentKey, orderId, amount, cartItems } = req.body || {};
  if (!paymentKey || !orderId || amount == null) {
    return res.status(400).json({ message: 'Invalid request parameters' });
  }

  // 1. 토스 결제 승인
  let tossRes;
  let tossBody;
  try {
    tossRes = await fetch(TOSS_CONFIRM_URL, {
      method: 'POST',
      headers: {
        Authorization: 'Basic ' + Buffer.from(secretKey + ':').toString('base64'),
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ paymentKey, orderId, amount: Number(amount) }),
    });
    tossBody = await tossRes.json().catch(() => ({}));
  } catch (e) {
    return res.status(502).json({ message: `토스 서버 요청 실패: ${e.message}` });
  }

  if (!tossRes.ok) {
    return res.status(tossRes.status).json({
      message: `Payment confirmation failed: ${tossBody.code || ''} ${tossBody.message || ''}`.trim(),
    });
  }

  // 2. 재고 차감 + 주문 기록 (실패해도 결제는 이미 승인됐으므로 성공으로 응답)
  if (Array.isArray(cartItems) && cartItems.length > 0) {
    try {
      await fetch(`${BACKEND_API_URL}/api/payments/cash`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ cartItems }),
        signal: AbortSignal.timeout(8000),
      });
    } catch (e) {
      console.error('재고 차감/주문 기록 실패:', e.message);
    }
  }

  return res.status(200).json(tossBody);
}
