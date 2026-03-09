import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

// ── Custom metrics ──────────────────────────────────────────────────────────
const ordersCreated   = new Counter('orders_created');
const rateLimited     = new Counter('rate_limited');
const quotaExceeded   = new Counter('quota_exceeded');
const serverErrors    = new Counter('server_errors');
const successRate     = new Rate('success_rate');
const responseTime    = new Trend('response_time', true);

// ── Config ──────────────────────────────────────────────────────────────────
const GATEWAY_URL   = __ENV.GATEWAY_URL   || 'http://partner-orders-alb-279492906.ap-south-1.elb.amazonaws.com';
const PARTNER_TOKEN = __ENV.PARTNER_TOKEN || 'YOUR_PARTNER_JWT_TOKEN';

// ── Scenarios ───────────────────────────────────────────────────────────────
//
//  SCENARIO 1 — load_balance (0s-60s)
//    10 VUs, 0.1s sleep = ~100 req/s total
//    Rate limit is 10/s per partner → triggers 429s
//    Proves: load balancing across 2 order-service tasks
//
//  SCENARIO 2 — alarm_4xx (65s-125s)
//    20 VUs, no sleep, bad tokens
//    Generates 401s rapidly → triggers 4xx-high CloudWatch alarm
//
//  SCENARIO 3 — cpu_spike (130s-190s)
//    30 VUs, no sleep, valid tokens
//    Max throughput → triggers CPU alarm on order-service
//
export const options = {
    scenarios: {
        load_balance: {
            executor:  'constant-vus',
            vus:       10,
            duration:  '60s',
            startTime: '0s',
        },
        alarm_4xx: {
            executor:  'constant-vus',
            vus:       20,
            duration:  '60s',
            startTime: '65s',
        },
        cpu_spike: {
            executor:  'constant-vus',
            vus:       30,
            duration:  '60s',
            startTime: '130s',
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
        'Content-Type':    'application/json',
        'Authorization':   `Bearer ${PARTNER_TOKEN}`,
        'X-Correlation-Id': uuidv4(),
        'Idempotency-Key': uuidv4(),
    };
}

function badHeaders() {
    return {
        'Content-Type':    'application/json',
        'Authorization':   'Bearer INVALID_TOKEN_FOR_ALARM_DEMO',
        'X-Correlation-Id': uuidv4(),
        'Idempotency-Key': uuidv4(),
    };
}

// ── Main ─────────────────────────────────────────────────────────────────────
export default function () {

    // Scenario 2 — bad tokens to trigger 4xx alarm
    if (__VU > 10 && __VU <= 30) {
        const res = http.post(
            `${GATEWAY_URL}/orders`,
            makeOrder(),
            { headers: badHeaders() }
        );
        check(res, { '401 bad token': (r) => r.status === 401 });
        if (res.status >= 500) serverErrors.add(1);
        // No sleep — maximize 4xx count
        return;
    }

    // Scenario 1 + 3 — valid requests
    const res = http.post(
        `${GATEWAY_URL}/orders`,
        makeOrder(),
        { headers: validHeaders() }
    );

    responseTime.add(res.timings.duration);
    successRate.add(res.status === 201);

    if (res.status === 201) {
        ordersCreated.add(1);
        const body = JSON.parse(res.body);
        console.log(`✅ CREATED orderId=${body.orderId} vu=${__VU} time=${res.timings.duration}ms`);
    }

    if (res.status === 429) {
        const body = JSON.parse(res.body);
        if (body.errorCode === 'RATE_LIMIT_EXCEEDED') {
            rateLimited.add(1);
            console.log(`⚡ RATE LIMITED vu=${__VU}`);
        } else {
            quotaExceeded.add(1);
            console.log(`📊 QUOTA EXCEEDED vu=${__VU}`);
        }
    }

    if (res.status >= 500) {
        serverErrors.add(1);
        console.log(`❌ ERROR [${res.status}] ${res.body.substring(0, 150)}`);
    }

    check(res, { 'not 5xx': (r) => r.status < 500 });

    // 0.1s sleep = 10 req/s per VU — right at rate limit boundary
    if (__VU <= 10) sleep(0.1);
    // VUs > 30 (cpu_spike scenario) — no sleep, max throughput
}

// ── Summary ───────────────────────────────────────────────────────────────────
export function handleSummary(data) {
    const reqs    = data.metrics.http_reqs.values.count;
    const avg     = data.metrics.http_req_duration.values.avg.toFixed(0);
    const p95     = data.metrics.http_req_duration.values['p(95)'].toFixed(0);
    const created = data.metrics.orders_created  ? data.metrics.orders_created.values.count  : 0;
    const limited = data.metrics.rate_limited     ? data.metrics.rate_limited.values.count    : 0;
    const errors  = data.metrics.server_errors    ? data.metrics.server_errors.values.count   : 0;

    console.log('\n╔══════════════════════════════════════════╗');
    console.log('║       PARTNER ORDERS LOAD TEST RESULTS   ║');
    console.log('╠══════════════════════════════════════════╣');
    console.log(`║  Total Requests : ${String(reqs).padEnd(22)}║`);
    console.log(`║  Orders Created : ${String(created).padEnd(22)}║`);
    console.log(`║  Rate Limited   : ${String(limited).padEnd(22)}║`);
    console.log(`║  Server Errors  : ${String(errors).padEnd(22)}║`);
    console.log(`║  Avg Response   : ${String(avg + 'ms').padEnd(22)}║`);
    console.log(`║  p95 Response   : ${String(p95 + 'ms').padEnd(22)}║`);
    console.log('╠══════════════════════════════════════════╣');
    console.log('║  CHECK IN AWS CONSOLE:                   ║');
    console.log('║  1. CloudWatch Alarms → 4xx-high=ALARM   ║');
    console.log('║  2. CloudWatch Alarms → cpu-high=ALARM   ║');
    console.log('║  3. ECS order-service → 2 tasks running  ║');
    console.log('╠══════════════════════════════════════════╣');
    console.log('║  LOAD BALANCE PROOF — run in CloudWatch: ║');
    console.log('║  fields @timestamp, message              ║');
    console.log('║  | filter message like /Creating order/  ║');
    console.log('║  | stats count(*) by @logStream          ║');
    console.log('║  2 streams with counts = LOAD BALANCED   ║');
    console.log('╚══════════════════════════════════════════╝\n');

    return { 'load-test-summary.json': JSON.stringify(data, null, 2) };
}