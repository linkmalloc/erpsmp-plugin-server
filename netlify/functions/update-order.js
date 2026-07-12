const GIST_ID = process.env.GIST_ID;
const GITHUB_TOKEN = process.env.GITHUB_TOKEN;
const ADMIN_TOKEN = process.env.ADMIN_TOKEN;

const CORS_HEADERS = {
    'Content-Type': 'application/json',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type, X-Admin-Token',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS'
};

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

exports.handler = async (event) => {
    if (event.httpMethod === 'OPTIONS') {
        return { statusCode: 204, headers: CORS_HEADERS, body: '' };
    }
    if (event.httpMethod !== 'POST') {
        return { statusCode: 405, headers: CORS_HEADERS, body: JSON.stringify({ error: 'Method Not Allowed' }) };
    }

    const adminToken = event.headers['x-admin-token'];
    if (!ADMIN_TOKEN || adminToken !== ADMIN_TOKEN) {
        return { statusCode: 401, headers: CORS_HEADERS, body: JSON.stringify({ error: 'Unauthorized' }) };
    }

    let body;
    try {
        body = JSON.parse(event.body);
    } catch {
        return { statusCode: 400, headers: CORS_HEADERS, body: JSON.stringify({ error: 'Invalid JSON' }) };
    }

    let { orderId, status, adminNote } = body;
    if (status === 'approve') status = 'approved';
    if (status === 'reject') status = 'rejected';

    if (!orderId || !['approved', 'rejected'].includes(status)) {
        return { statusCode: 400, headers: CORS_HEADERS, body: JSON.stringify({ error: 'Invalid orderId or status' }) };
    }

    try {
        let orders = await readOrders();
        if (!Array.isArray(orders)) orders = [];
        const idx = orders.findIndex(o => o.orderId === orderId);
        if (idx === -1) {
            return { statusCode: 404, headers: CORS_HEADERS, body: JSON.stringify({ error: 'Order not found' }) };
        }

        orders[idx].status = status;
        orders[idx].adminNote = adminNote || null;
        if (status === 'approved') {
            orders[idx].approvedAt = new Date().toISOString();
        } else {
            orders[idx].rejectedAt = new Date().toISOString();
        }

        await writeOrders(orders);
        return {
            statusCode: 200,
            headers: CORS_HEADERS,
            body: JSON.stringify({ success: true, order: orders[idx] })
        };
    } catch (err) {
        console.error('update-order error:', err);
        return { statusCode: 500, headers: CORS_HEADERS, body: JSON.stringify({ error: 'Failed to update order' }) };
    }
};
