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

exports.handler = async (event) => {
    // CORS preflight
    if (event.httpMethod === 'OPTIONS') {
        return {
            statusCode: 204,
            headers: {
                'Access-Control-Allow-Origin': '*',
                'Access-Control-Allow-Headers': 'Content-Type, X-Admin-Token',
                'Access-Control-Allow-Methods': 'GET, OPTIONS'
            },
            body: ''
        };
    }

    if (event.httpMethod !== 'GET') {
        return { statusCode: 405, body: JSON.stringify({ error: 'Method Not Allowed' }) };
    }

    const adminToken = event.headers['x-admin-token'];
    if (!ADMIN_TOKEN || adminToken !== ADMIN_TOKEN) {
        return { statusCode: 401, body: JSON.stringify({ error: 'Unauthorized' }) };
    }

    try {
        const orders = await readOrders();
        return {
            statusCode: 200,
            headers: {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*',
                'Access-Control-Allow-Headers': 'Content-Type, X-Admin-Token'
            },
            body: JSON.stringify(orders)
        };
    } catch (err) {
        console.error('get-orders error:', err);
        return {
            statusCode: 500,
            body: JSON.stringify({ error: 'Failed to fetch orders' })
        };
    }
};
