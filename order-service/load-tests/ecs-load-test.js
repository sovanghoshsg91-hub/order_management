import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

// ── Custom metrics ──────────────────────────────────────────────────────────
const ordersCreated = new Counter('orders_created');
const rateLimited   = new Counter('rate_limited');
const serverErrors  = new Counter('server_errors');
const successRate   = new Rate('success_rate');
const responseTime  = new Trend('response_time', true);

// ── Config ──────────────────────────────────────────────────────────────────
const GATEWAY_URL   = __ENV.GATEWAY_URL   || 'http://partner-orders-alb-279492906.ap-south-1.elb.amazonaws.com';
const PARTNER_TOKEN = __ENV.PARTNER_TOKEN || 'YOUR_PARTNER_JWT_TOKEN';

// ── Scenarios ───────────────────────────────────────────────────────────────
//
//  SCENARIO 1 — rate_limit_demo (0s-30s)
//    15 VUs, no sleep = ~150 req/s
//    Rate limit = 10/s per partner → 429s guaranteed every second
//    Shows: RATE_LIMIT_EXCEEDED in k6 output + orders still succeeding
//
//  SCENARIO 2 — alarm_4xx (35s-95s)
//    20 VUs, no sleep, bad tokens
//    Generates 401s rapidly → triggers 4xx-high CloudWatch alarm
//    Shows: CloudWatch alarm flipping to ALARM state live
//
export const options = {
    scenarios: {
        rate_limit_demo: {
            executor:  'constant-vus',
            vus:       15,
            duration:  '30s',
            startTime: '0s',
        },
        alarm_4xx: {
            executor:  'constant-vus',
            vus:       20,
            duration:  '60s',
            startTime: '35s',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<5000'],
    },
};

// ── Helpers ──────────────────────────────────────────────────────────────────
function makeOrder() {
    return JSON.stringify({
        orderType: 'DELIVERY',
        payload:   { item: 'load-test-item', quantity: 1 },
    });
}

function validHeaders() {
    return {
        'Content-Type':     'application/json',
        'Authorization':    `Bearer ${PARTNER_TOKEN}`,
        'X-Correlation-Id': uuidv4(),
        'Idempotency-Key':  uuidv4(),
    };
}

function badHeaders() {
    return {
        'Content-Type':     'application/json',
        'Authorization':    'Bearer INVALID_TOKEN_FOR_ALARM_DEMO',
        'X-Correlation-Id': uuidv4(),
        'Idempotency-Key':  uuidv4(),
    };
}

// ── Main ─────────────────────────────────────────────────────────────────────
export default function () {

    // Scenario 2 — bad tokens → 4xx alarm
    if (__VU > 15) {
        const res = http.post(
            `${GATEWAY_URL}/orders`,
            makeOrder(),
            { headers: badHeaders() }
        );
        check(res, { '401 bad token': (r) => r.status === 401 });
        // No sleep — maximize 4xx rate
        return;
    }

    // Scenario 1 — valid requests, no sleep → burst > 10/s → triggers 429
    const res = http.post(
        `${GATEWAY_URL}/orders`,
        makeOrder(),
        { headers: validHeaders() }
    );

    responseTime.add(res.timings.duration);
    successRate.add(res.status === 201);

    if (res.status === 201) {
        ordersCreated.add(1);
        try {
            const body = JSON.parse(res.body);
            console.log(`✅ CREATED orderId=${body.orderId} vu=${__VU} time=${res.timings.duration}ms`);
        } catch (e) {
            console.log(`✅ CREATED vu=${__VU} time=${res.timings.duration}ms`);
        }
    }

    if (res.status === 429) {
        rateLimited.add(1);
        try {
            const body = JSON.parse(res.body);
            console.log(`⚡ 429 ${body.errorCode} vu=${__VU} — ${body.message}`);
        } catch (e) {
            console.log(`⚡ 429 RATE_LIMIT_EXCEEDED vu=${__VU}`);
        }
    }

    if (res.status >= 500) {
        serverErrors.add(1);
        console.log(`❌ ERROR [${res.status}] vu=${__VU} body=${res.body.substring(0, 150)}`);
    }

    check(res, {
        '201 or 429 (expected)': (r) => r.status === 201 || r.status === 429,
        'not 5xx':               (r) => r.status < 500,
    });

    // Small sleep after 429 to avoid hammering — realistic client behaviour
    if (res.status === 429) sleep(0.1);
}

// ── Summary ───────────────────────────────────────────────────────────────────
export function handleSummary(data) {
    const reqs    = data.metrics.http_reqs.values.count;
    const avg     = data.metrics.http_req_duration.values.avg.toFixed(0);
    const p95     = data.metrics.http_req_duration.values['p(95)'].toFixed(0);
    const created = data.metrics.orders_created ? data.metrics.orders_created.values.count : 0;
    const limited = data.metrics.rate_limited   ? data.metrics.rate_limited.values.count   : 0;
    const errors  = data.metrics.server_errors  ? data.metrics.server_errors.values.count  : 0;
    const limitPct = reqs > 0 ? ((limited / reqs) * 100).toFixed(1) : '0';

    console.log('\n╔══════════════════════════════════════════╗');
    console.log('║      PARTNER ORDERS LOAD TEST RESULTS    ║');
    console.log('╠══════════════════════════════════════════╣');
    console.log(`║  Total Requests  : ${String(reqs).padEnd(21)}║`);
    console.log(`║  Orders Created  : ${String(created).padEnd(21)}║`);
    console.log(`║  Rate Limited    : ${String(limited + ' (' + limitPct + '%)').padEnd(21)}║`);
    console.log(`║  Server Errors   : ${String(errors).padEnd(21)}║`);
    console.log(`║  Avg Response    : ${String(avg + 'ms').padEnd(21)}║`);
    console.log(`║  p95 Response    : ${String(p95 + 'ms').padEnd(21)}║`);
    console.log('╠══════════════════════════════════════════╣');
    console.log('║  DEMO CHECKLIST:                         ║');
    console.log('║  ✅ 429 RATE_LIMIT_EXCEEDED in output    ║');
    console.log('║  ✅ CloudWatch → 4xx-high = ALARM 🔴     ║');
    console.log('║  ✅ ECS → order-service → 2 tasks        ║');
    console.log('╠══════════════════════════════════════════╣');
    console.log('║  LOAD BALANCE PROOF (CloudWatch Logs):   ║');
    console.log('║  fields @timestamp, message              ║');
    console.log('║  | filter message like /Creating order/  ║');
    console.log('║  | stats count(*) by @logStream          ║');
    console.log('║  → 2 streams with counts = balanced ✅   ║');
    console.log('╚══════════════════════════════════════════╝\n');

    return { 'load-test-summary.json': JSON.stringify(data, null, 2) };
}