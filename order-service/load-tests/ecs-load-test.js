import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

// ── Custom metrics ─────────────────────────────────────────────────────────
const ordersCreated  = new Counter('orders_created');
const rateLimited    = new Counter('rate_limited');
const successRate    = new Rate('success_rate');
const responseTime   = new Trend('response_time', true);

// ── Config — passed via -e env vars ───────────────────────────────────────
const GATEWAY_URL   = 'http://partner-orders-alb-279492906.ap-south-1.elb.amazonaws.com';
const PARTNER_TOKEN = __ENV.PARTNER_TOKEN || 'YOUR_PARTNER_JWT_TOKEN';
const PARTNER_ID    = __ENV.PARTNER_ID    || 'YOUR_PARTNER_ID';

// ── Test options ───────────────────────────────────────────────────────────
export const options = {
    scenarios: {
        load_balance_demo: {
            executor: 'constant-vus',
            vus:      10,
            duration: '60s',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<3000'],
        success_rate:      ['rate>0.95'],
    },
};

// ── Order payload — matches local.js exactly ──────────────────────────────
function makeOrder() {
    return JSON.stringify({
        orderType: 'DELIVERY',
        payload:   { item: 'test-item' },
    });
}

// ── Main test loop ─────────────────────────────────────────────────────────
export default function () {

    const headers = {
        'Content-Type':     'application/json',
        'Authorization':    `Bearer ${PARTNER_TOKEN}`,
        'X-Partner-Id':     PARTNER_ID,
        'X-Correlation-Id': uuidv4(),
        'Idempotency-Key':  uuidv4(),
    };

    const res = http.post(
        `${GATEWAY_URL}/orders`,
        makeOrder(),
        { headers, tags: { endpoint: 'create_order' } }
    );

    // Track metrics
    responseTime.add(res.timings.duration);
    successRate.add(res.status === 201);

    if (res.status === 201) ordersCreated.add(1);
    if (res.status === 429) rateLimited.add(1);

    const ok = check(res, {
        'status is 201': (r) => r.status === 201,
        'response time < 3s': (r) => r.timings.duration < 3000,
    });

    if (!ok) {
        console.log(`FAIL [${res.status}] ${res.body.substring(0, 200)}`);
    } else {
        const correlationId = res.headers['X-Correlation-Id'] || 'unknown';
        console.log(`OK [${res.status}] correlationId=${correlationId} time=${res.timings.duration}ms`);
    }

    sleep(0.5);
}

// ── Summary ────────────────────────────────────────────────────────────────
export function handleSummary(data) {
    const reqs     = data.metrics.http_reqs.values.count;
    const avgTime  = data.metrics.http_req_duration.values.avg.toFixed(0);
    const p95Time  = data.metrics.http_req_duration.values['p(95)'].toFixed(0);
    const rate     = (data.metrics.success_rate.values.rate * 100).toFixed(1);
    const created  = data.metrics.orders_created  ? data.metrics.orders_created.values.count  : 0;
    const limited  = data.metrics.rate_limited     ? data.metrics.rate_limited.values.count    : 0;

    console.log('\n════════════════════════════════════════');
    console.log('        LOAD BALANCE TEST SUMMARY        ');
    console.log('════════════════════════════════════════');
    console.log(`Total Requests  : ${reqs}`);
    console.log(`Orders Created  : ${created}`);
    console.log(`Rate Limited    : ${limited}`);
    console.log(`Success Rate    : ${rate}%`);
    console.log(`Avg Response    : ${avgTime}ms`);
    console.log(`p95 Response    : ${p95Time}ms`);
    console.log('════════════════════════════════════════');
    console.log('Check CloudWatch → /ecs/order-service');
    console.log('2 different container IDs = load balanced!');
    console.log('════════════════════════════════════════\n');

    return {
        'summary.json': JSON.stringify(data, null, 2),
    };
}