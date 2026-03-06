import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// ── Custom metrics ─────────────────────────────────────────────────────────
const requestsPerInstance = new Counter('requests_per_instance');
const successRate         = new Rate('success_rate');
const responseTime        = new Trend('response_time', true);

// ── Config — update these values ───────────────────────────────────────────
const GATEWAY_URL  = 'http://partner-orders-alb-279492906.ap-south-1.elb.amazonaws.com';
const PARTNER_TOKEN = __ENV.PARTNER_TOKEN || 'YOUR_PARTNER_JWT_TOKEN';

// ── Test options ───────────────────────────────────────────────────────────
export const options = {
    scenarios: {
        load_balance_demo: {
            executor:    'constant-vus',
            vus:         10,        // 10 virtual users
            duration:    '60s',     // run for 60 seconds
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<3000'],  // 95% requests under 3s
        success_rate:      ['rate>0.95'],   // 95% success rate
    },
};

// ── Order payload ──────────────────────────────────────────────────────────
function makeOrder() {
    return JSON.stringify({
        item:     'Widget',
        quantity: Math.floor(Math.random() * 5) + 1,
        address:  '123 Test Street, Mumbai'
    });
}

// ── Main test loop ─────────────────────────────────────────────────────────
export default function () {

    const headers = {
        'Content-Type':  'application/json',
        'Authorization': `Bearer ${PARTNER_TOKEN}`,
        'Idempotency-Key': `k6-${__VU}-${__ITER}`,
    };

    // POST /orders — routed to one of 2 order-service instances
    const res = http.post(
        `${GATEWAY_URL}/orders`,
        makeOrder(),
        { headers, tags: { endpoint: 'create_order' } }
    );

    // Track metrics
    responseTime.add(res.timings.duration);
    successRate.add(res.status === 201);

    const ok = check(res, {
        'status is 201': (r) => r.status === 201,
        'response time < 3s':   (r) => r.timings.duration < 3000,
    });

    if (res.status !== 201) {
        console.log(`FAIL status=${res.status} body=${res.body.substring(0, 200)}`);
    }
    if (!ok) {
        console.log(`FAIL [${res.status}] ${res.body}`);
    } else {
        // Log which instance handled it (from X-Correlation-Id header)
        const correlationId = res.headers['X-Correlation-Id'] || 'unknown';
        console.log(`OK [${res.status}] correlationId=${correlationId} time=${res.timings.duration}ms`);
    }

    sleep(0.5);  // 0.5s between requests per VU
}

// ── Summary ────────────────────────────────────────────────────────────────
export function handleSummary(data) {
    const reqs    = data.metrics.http_reqs.values.count;
    const avgTime = data.metrics.http_req_duration.values.avg.toFixed(0);
    const p95Time = data.metrics.http_req_duration.values['p(95)'].toFixed(0);
    const rate    = (data.metrics.success_rate.values.rate * 100).toFixed(1);

    console.log('\n════════════════════════════════════════');
    console.log('        LOAD BALANCE TEST SUMMARY        ');
    console.log('════════════════════════════════════════');
    console.log(`Total Requests  : ${reqs}`);
    console.log(`Success Rate    : ${rate}%`);
    console.log(`Avg Response    : ${avgTime}ms`);
    console.log(`p95 Response    : ${p95Time}ms`);
    console.log('════════════════════════════════════════');
    console.log('Check CloudWatch → /ecs/order-service');
    console.log('You will see logs from 2 different');
    console.log('container IDs — proving load balancing!');
    console.log('════════════════════════════════════════\n');

    return {
        'summary.json': JSON.stringify(data, null, 2),
    };
}