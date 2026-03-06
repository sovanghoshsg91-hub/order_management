import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const GATEWAY_URL = 'http://localhost:8090';
const JWT_TOKEN   = 'eyJraWQiOiJHd1ZKRW1KNUhOY3RCbysyd1wvT1RscitpSEt2blRWVTYxSjRpa0h5UWhvVT0iLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiIzMTYzYmQzYS04MDAxLTcwMTctN2JhNy1lMGY5ZWFkZTk4YzgiLCJjb2duaXRvOmdyb3VwcyI6WyJQQVJUTkVSIl0sImlzcyI6Imh0dHBzOlwvXC9jb2duaXRvLWlkcC5hcC1zb3V0aC0xLmFtYXpvbmF3cy5jb21cL2FwLXNvdXRoLTFfWmZyUGZuQmJmIiwiY2xpZW50X2lkIjoiYjJiZXU5bjVkODFmM2ltMzVkaDNjY3BjNyIsIm9yaWdpbl9qdGkiOiI3ZTNiNDA0Yy0xNTJiLTQ4ZGQtOThiZS1mYmM5ZTg4NzdjNGUiLCJldmVudF9pZCI6ImVlMDM2MzY0LWRkMWQtNGY0NC05NTQyLTVkOGQ3NDA1YTJiNSIsInRva2VuX3VzZSI6ImFjY2VzcyIsInNjb3BlIjoiYXdzLmNvZ25pdG8uc2lnbmluLnVzZXIuYWRtaW4iLCJhdXRoX3RpbWUiOjE3NzI2MTI3MjQsImV4cCI6MTc3MjY5OTEyNCwiaWF0IjoxNzcyNjEyNzI0LCJqdGkiOiJjZDliNTNlYi02NzJlLTQwMjctYjIzYy1hNmI1YjY3OTM0ODciLCJ1c2VybmFtZSI6IjMxNjNiZDNhLTgwMDEtNzAxNy03YmE3LWUwZjllYWRlOThjOCJ9.Gz7vnv2gQCkEIp3D3ek4hgQJ_cYlRLhnG4OPYqbx6_P-v90pbzDZ321GZ9CZFjNaOzdUMh_GVWTY7gQwWcd_27v_sZ2dOal2xl8sz55M4bOr6xAQTKed0pm-FS8AKtCiK70bDDxkVP7MP-TTWxZ99t2XtLHk5obub8ePqb8SdCZDEH5GBtzill4mCNcHZeqfI_dos27GQ6-ypAB8Rvx6j877kQfDlyYbigfJ_yErzXiJZ06mpuKLpfOcty4uo7_2dDICfVTjtwO8bSHFKwmTSsYxxWrinCy-3Lkrr2-LFC_CC5ylcZjkJ_BEZ9Kjpp6rvssaAKzMkPFq7nNRSJKnPg';
const PARTNER_ID  = 'f0b01ecf-26ad-46a6-b53c-eaa43a077878';

// Custom counters — shows exactly how many 201 vs 429
const created    = new Counter('orders_created');
const rateLimited = new Counter('rate_limited');

export const options = {
  scenarios: {
    rate_limit_test: {
      executor: 'shared-iterations',
      vus: 10,
      iterations: 10,
      maxDuration: '10s',
    },
  },
};

export default function () {
  const res = http.post(
    `${GATEWAY_URL}/orders`,
    JSON.stringify({
      orderType: 'DELIVERY',
      payload: { item: 'test-item' },
    }),
    {
      headers: {
        'Content-Type':     'application/json',
        'Authorization':    `Bearer ${JWT_TOKEN}`,
        'X-Partner-Id':     PARTNER_ID,
        'X-Correlation-Id': uuidv4(),
        'Idempotency-Key':  uuidv4(),
      },
    }
  );

  console.log(`VU:${__VU} ITER:${__ITER} → ${res.status}`);

  // Track counts
  if (res.status === 201) created.add(1);
  if (res.status === 429) rateLimited.add(1);

  check(res, {
    '201 or 429': (r) => r.status === 201 || r.status === 429,
  });
}