const { Octokit } = require('@octokit/rest');

const octokit = new Octokit({ auth: process.env.GITHUB_TOKEN });
const GIST_ID = process.env.GIST_ID;
const ADMIN_TOKEN = process.env.ADMIN_TOKEN;

async function readOrders() {
    try {
        const { data } = await octokit.gists.get({ gist_id: GIST_ID });
        const content = data.files['orders.json']?.content || '[]';
        return JSON.parse(content);
    } catch (err) {
        console.error('readOrders error:', err);
        return [];
    }
}

async function writeOrders(orders) {
    await octokit.gists.update({
        gist_id: GIST_ID,
        files: {
            'orders.json': {
                content: JSON.stringify(orders, null, 2)
            }
        }
    });
}

exports.handler = async (event) => {
    // CORS preflight
    if (event.httpMethod === 'OPTIONS') {
        return {
            statusCode: 204,
            headers: {
                'Access-Control-Allow-Origin': '*',
                'Access-Control-Allow-Headers': 'Content-Type, X-Admin-Token',
                'Access-Control-Allow-Methods': 'POST, OPTIONS'
            },
            body: ''
        };
    }

    if (event.httpMethod !== 'POST') {
        return { statusCode: 405, body: JSON.stringify({ error: 'Method Not Allowed' }) };
    }

    const adminToken = event.headers['x-admin-token'];
    if (!ADMIN_TOKEN || adminToken !== ADMIN_TOKEN) {
        return { statusCode: 401, body: JSON.stringify({ error: 'Unauthorized' }) };
    }

    let body;
    try {
        body = JSON.parse(event.body);
    } catch {
        return { statusCode: 400, body: JSON.stringify({ error: 'Invalid JSON' }) };
    }

    const { orderId, status, adminNote } = body;
    if (!orderId || !['approved', 'rejected'].includes(status)) {
        return { statusCode: 400, body: JSON.stringify({ error: 'Invalid orderId or status' }) };
    }

    try {
        const orders = await readOrders();
        const idx = orders.findIndex(o => o.orderId === orderId);
        if (idx === -1) {
            return { statusCode: 404, body: JSON.stringify({ error: 'Order not found' }) };
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
            headers: {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*',
                'Access-Control-Allow-Headers': 'Content-Type, X-Admin-Token'
            },
            body: JSON.stringify({ success: true, order: orders[idx] })
        };
    } catch (err) {
        console.error('update-order error:', err);
        return {
            statusCode: 500,
            body: JSON.stringify({ error: 'Failed to update order' })
        };
    }
};
