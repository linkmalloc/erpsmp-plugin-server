const { Octokit } = require('@octokit/rest');

// Environment variables (set in Netlify dashboard):
// GITHUB_TOKEN - Personal Access Token with gist scope
// GIST_ID      - The Gist ID to use as the database
// ADMIN_TOKEN  - Secret token for admin API calls

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
        const orders = await readOrders();
        orders.unshift(order); // newest first
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
            body: JSON.stringify({ error: 'Failed to save order', details: err.message })
        };
    }
};
