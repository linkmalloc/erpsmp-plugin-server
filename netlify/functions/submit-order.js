// Environment variables (set in Netlify dashboard):
// GITHUB_TOKEN - Personal Access Token with gist scope
// GIST_ID      - The Gist ID to use as the database
// ADMIN_TOKEN  - Secret token for admin API calls

const GIST_ID = process.env.GIST_ID;
const GITHUB_TOKEN = process.env.GITHUB_TOKEN;
const ADMIN_TOKEN = process.env.ADMIN_TOKEN;

async function readOrders() {
    const res = await fetch(`https://api.github.com/gists/${GIST_ID}`, {
        headers: {
            Authorization: `Bearer ${GITHUB_TOKEN}`,
            Accept: 'application/vnd.github+json'
        }
    });
    if (!res.ok) throw new Error(`GitHub API error: ${res.status}`);
    const data = await res.json();
    const content = data.files['orders.json']?.content || '[]';
    return JSON.parse(content);
}

async function writeOrders(orders) {
    const res = await fetch(`https://api.github.com/gists/${GIST_ID}`, {
        method: 'PATCH',
        headers: {
            Authorization: `Bearer ${GITHUB_TOKEN}`,
            Accept: 'application/vnd.github+json',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            files: {
                'orders.json': { content: JSON.stringify(orders, null, 2) }
            }
        })
    });
    if (!res.ok) throw new Error(`GitHub write error: ${res.status}`);
}

function generateOrderId() {
    const ts = Date.now().toString(36).toUpperCase();
    const rand = Math.random().toString(36).substring(2, 6).toUpperCase();
    return `ERP-${ts}-${rand}`;
}

exports.handler = async (event) => {
    if (event.httpMethod !== 'POST') {
        return { statusCode: 405, body: JSON.stringify({ error: 'Method Not Allowed' }) };
    }

    let body;
    try {
        body = JSON.parse(event.body);
    } catch {
        return { statusCode: 400, body: JSON.stringify({ error: 'Invalid JSON' }) };
    }

    const { username, platform, items, total, currency, method, refNo, screenshotBase64 } = body;
    if (!username || !items || !items.length || !method) {
        return { statusCode: 400, body: JSON.stringify({ error: 'Missing required fields' }) };
    }

    const orderId = generateOrderId();
    const order = {
        orderId,
        username,
        platform,
        items,
        total,
        currency,
        method,
        refNo: refNo || null,
        screenshotBase64: screenshotBase64 || null,
        status: 'pending',
        submittedAt: new Date().toISOString(),
        approvedAt: null,
        rejectedAt: null,
        adminNote: null
    };

    try {
        let orders = await readOrders();
        if (!Array.isArray(orders)) {
            orders = [];
        }
        orders.unshift(order);
        await writeOrders(orders);
        return {
            statusCode: 200,
            headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' },
            body: JSON.stringify({ success: true, orderId })
        };
    } catch (err) {
        console.error('submit-order error:', err);
        return {
            statusCode: 500,
            headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' },
            body: JSON.stringify({ error: 'Failed to save order', details: err.message })
        };
    }
};
