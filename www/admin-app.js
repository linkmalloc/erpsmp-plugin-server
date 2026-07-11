// =============================================
// ErpSMP Admin Dashboard — admin-app.js
// =============================================

const PRODUCT_RANK_MAP = {
    erpie: 'erp+',
    erpiepro: 'erp++',
    erpiepromaxx: 'erp+++'
};

const KEY_CMD_MAP = {
    echokey: { cmd: 'echokeys', label: 'Echo Key' },
    crimsonkey: { cmd: 'crimsonkeys', label: 'Crimson Key' },
    endkey: { cmd: 'keys', keyType: 'end', label: 'End Key' },
    amethystkey: { cmd: 'keys', keyType: 'amethyst', label: 'Amethyst Key' },
    basickey: { cmd: 'keys', keyType: 'basic', label: 'Basic Key' }
};

let adminToken = sessionStorage.getItem('erp_admin_token') || '';
let allOrders = [];
let currentTab = 'pending';
let actionTargetOrder = null;
let actionType = null; // 'approve' | 'reject'

// DOM refs
const adminLoginOverlay = document.getElementById('adminLoginOverlay');
const adminDashboard = document.getElementById('adminDashboard');
const adminTokenInput = document.getElementById('adminTokenInput');
const btnAdminLogin = document.getElementById('btnAdminLogin');
const adminLoginError = document.getElementById('adminLoginError');

const adminLoading = document.getElementById('adminLoading');
const adminEmptyState = document.getElementById('adminEmptyState');
const adminOrdersTable = document.getElementById('adminOrdersTable');
const adminTableBody = document.getElementById('adminTableBody');

const pendingCount = document.getElementById('pendingCount');
const statPending = document.getElementById('statPending');
const statApproved = document.getElementById('statApproved');
const statRejected = document.getElementById('statRejected');
const statTotal = document.getElementById('statTotal');

const adminTabTitle = document.getElementById('adminTabTitle');
const adminTabSub = document.getElementById('adminTabSub');
const adminSearchInput = document.getElementById('adminSearchInput');

const screenshotModalOverlay = document.getElementById('screenshotModalOverlay');
const screenshotModalImg = document.getElementById('screenshotModalImg');
const screenshotModalClose = document.getElementById('screenshotModalClose');

const actionModalOverlay = document.getElementById('actionModalOverlay');
const actionModalTitle = document.getElementById('actionModalTitle');
const actionModalDesc = document.getElementById('actionModalDesc');
const actionCommandBox = document.getElementById('actionCommandBox');
const actionCommandsList = document.getElementById('actionCommandsList');
const btnCopyCommands = document.getElementById('btnCopyCommands');
const actionNoteInput = document.getElementById('actionNoteInput');
const btnActionCancel = document.getElementById('btnActionCancel');
const btnActionConfirm = document.getElementById('btnActionConfirm');

const TAB_META = {
    pending: { title: 'Pending Orders', sub: 'Orders awaiting review and payment verification.' },
    approved: { title: 'Approved Orders', sub: 'These orders have been verified and items credited.' },
    rejected: { title: 'Rejected Orders', sub: 'Orders that were denied due to invalid or suspicious payment proof.' },
    all: { title: 'All Orders', sub: 'Complete order history.' }
};

// ---- Helpers ----
function formatDate(iso) {
    if (!iso) return '—';
    const d = new Date(iso);
    return d.toLocaleString('en-PH', { dateStyle: 'medium', timeStyle: 'short' });
}

function formatCurrency(amount, currency) {
    if (currency === 'PHP') return `₱${Number(amount).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
    return `$${Number(amount).toFixed(2)}`;
}

function getAvatarUrl(username, platform) {
    if (platform === 'bedrock') return 'https://mc-heads.net/avatar/steve/32';
    const clean = username.startsWith('.') ? username.substring(1) : username;
    return `https://mc-heads.net/avatar/${clean}/32`;
}

function buildServerCommands(order) {
    const cmds = [];
    const player = order.username;
    for (const item of order.items) {
        const rank = PRODUCT_RANK_MAP[item.id];
        if (rank) {
            cmds.push(`/setrank ${player} ${rank}`);
        } else {
            const keyDef = KEY_CMD_MAP[item.id];
            if (keyDef) {
                if (keyDef.keyType) {
                    cmds.push(`/keys ${keyDef.keyType} add ${item.quantity} ${player}`);
                } else {
                    cmds.push(`/${keyDef.cmd} ${player} add ${item.quantity}`);
                }
            }
        }
    }
    return cmds;
}

// ---- Auth ----
function showLogin() {
    adminLoginOverlay.style.display = 'flex';
    adminDashboard.style.display = 'none';
}

function showDashboard(orders) {
    adminLoginOverlay.style.display = 'none';
    adminDashboard.style.display = 'flex';
    adminLoading.style.display = 'none';
    allOrders = orders;
    updateStats();
    renderTable();
}

btnAdminLogin.addEventListener('click', async () => {
    const token = adminTokenInput.value.trim();
    if (!token) {
        showError('Please enter your admin token.');
        return;
    }
    adminToken = token;
    const originalHtml = btnAdminLogin.innerHTML;
    btnAdminLogin.disabled = true;
    btnAdminLogin.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Logging in...';
    try {
        const orders = await fetchOrders();
        sessionStorage.setItem('erp_admin_token', token);
        adminLoginError.style.display = 'none';
        showDashboard(orders);
    } catch (err) {
        if (err.status === 401) {
            showError('Invalid admin token. Please try again.');
        } else {
            showError(`Connection error (HTTP ${err.status || '?'}): ${err.message}`);
        }
    } finally {
        btnAdminLogin.disabled = false;
        btnAdminLogin.innerHTML = originalHtml;
    }
});

adminTokenInput.addEventListener('keydown', e => {
    if (e.key === 'Enter') btnAdminLogin.click();
});

function showError(msg) {
    adminLoginError.textContent = msg;
    adminLoginError.style.display = 'block';
}

document.getElementById('btnLogout').addEventListener('click', () => {
    sessionStorage.removeItem('erp_admin_token');
    adminToken = '';
    showLogin();
});

// ---- Fetch Orders ----
async function fetchOrders() {
    const resp = await fetch('/.netlify/functions/get-orders', {
        headers: { 'X-Admin-Token': adminToken }
    });
    if (!resp.ok) {
        let errMsg = `HTTP ${resp.status}`;
        try { const d = await resp.json(); errMsg = d.error || errMsg; } catch {}
        const err = new Error(errMsg);
        err.status = resp.status;
        throw err;
    }
    return await resp.json();
}

async function loadOrders() {
    adminLoading.style.display = 'flex';
    adminOrdersTable.style.display = 'none';
    adminEmptyState.style.display = 'none';

    try {
        allOrders = await fetchOrders();
        adminLoading.style.display = 'none';
        updateStats();
        renderTable();
    } catch (err) {
        adminLoading.style.display = 'none';
        adminEmptyState.style.display = 'flex';
        adminEmptyState.innerHTML = `<i class="fa-solid fa-triangle-exclamation"></i><p>Failed to load orders: <strong>${err.message}</strong>.<br>Check GITHUB_TOKEN, GIST_ID env vars in Netlify.</p>`;
    }
}

document.getElementById('btnRefresh').addEventListener('click', loadOrders);

// ---- Stats ----
function updateStats() {
    const pending = allOrders.filter(o => o.status === 'pending').length;
    const approved = allOrders.filter(o => o.status === 'approved').length;
    const rejected = allOrders.filter(o => o.status === 'rejected').length;
    statPending.textContent = pending;
    statApproved.textContent = approved;
    statRejected.textContent = rejected;
    statTotal.textContent = allOrders.length;
    pendingCount.textContent = pending;
    pendingCount.style.display = pending > 0 ? 'inline-block' : 'none';
}

// ---- Tab Navigation ----
document.querySelectorAll('.admin-nav-link').forEach(link => {
    link.addEventListener('click', e => {
        e.preventDefault();
        document.querySelectorAll('.admin-nav-link').forEach(l => l.classList.remove('active'));
        link.classList.add('active');
        currentTab = link.getAttribute('data-tab');
        const meta = TAB_META[currentTab];
        adminTabTitle.textContent = meta.title;
        adminTabSub.textContent = meta.sub;
        renderTable();
    });
});

// ---- Search ----
adminSearchInput.addEventListener('input', () => renderTable());

// ---- Render Table ----
function renderTable() {
    adminLoading.style.display = 'none';
    const searchQuery = adminSearchInput.value.toLowerCase();

    let filtered = allOrders.filter(o => {
        if (currentTab !== 'all' && o.status !== currentTab) return false;
        if (searchQuery) {
            return (
                o.username?.toLowerCase().includes(searchQuery) ||
                o.orderId?.toLowerCase().includes(searchQuery) ||
                o.refNo?.toLowerCase().includes(searchQuery)
            );
        }
        return true;
    });

    if (filtered.length === 0) {
        adminOrdersTable.style.display = 'none';
        adminEmptyState.style.display = 'flex';
        adminEmptyState.innerHTML = '<i class="fa-solid fa-inbox"></i><p>No orders found.</p>';
        return;
    }

    adminEmptyState.style.display = 'none';
    adminOrdersTable.style.display = 'table';
    adminTableBody.innerHTML = '';

    for (const order of filtered) {
        const tr = document.createElement('tr');
        tr.innerHTML = buildRowHTML(order);
        adminTableBody.appendChild(tr);
    }

    // Bind thumbnail click
    adminTableBody.querySelectorAll('.screenshot-thumb').forEach(img => {
        img.addEventListener('click', () => {
            screenshotModalImg.src = img.getAttribute('data-full');
            screenshotModalOverlay.style.display = 'flex';
        });
    });

    // Bind approve/reject buttons
    adminTableBody.querySelectorAll('.btn-approve').forEach(btn => {
        btn.addEventListener('click', () => openActionModal(btn.getAttribute('data-id'), 'approve'));
    });
    adminTableBody.querySelectorAll('.btn-reject').forEach(btn => {
        btn.addEventListener('click', () => openActionModal(btn.getAttribute('data-id'), 'reject'));
    });
}

function buildRowHTML(order) {
    const avatarUrl = getAvatarUrl(order.username, order.platform);
    const itemsHtml = (order.items || []).map(i =>
        `<span class="item-tag">${i.name || i.id}${i.quantity > 1 ? ` x${i.quantity}` : ''}</span>`
    ).join('');
    const methodClass = order.method || 'gcash';
    const screenshotHtml = order.screenshotBase64
        ? `<img src="${order.screenshotBase64}" class="screenshot-thumb" data-full="${order.screenshotBase64}" alt="Screenshot" loading="lazy">`
        : `<span class="no-screenshot">—</span>`;
    const isActionable = order.status === 'pending';

    return `
        <td><span class="order-id-cell">${order.orderId}</span></td>
        <td>
            <div class="player-cell">
                <img src="${avatarUrl}" class="player-avatar" alt="${order.username}" onerror="this.src='https://mc-heads.net/avatar/steve/32'">
                <div>
                    <div class="player-name">${order.username}</div>
                    <div class="player-platform">${order.platform}</div>
                </div>
            </div>
        </td>
        <td><div class="items-cell">${itemsHtml}</div></td>
        <td><span class="total-cell">${formatCurrency(order.total, order.currency)}</span></td>
        <td><span class="method-badge ${methodClass}">${methodClass.toUpperCase()}</span></td>
        <td><span class="ref-cell" title="${order.refNo || ''}">${order.refNo || '—'}</span></td>
        <td>${screenshotHtml}</td>
        <td><span class="date-cell">${formatDate(order.submittedAt)}</span></td>
        <td>
            <span class="status-badge ${order.status}">
                ${order.status === 'pending' ? '<i class="fa-solid fa-clock"></i>' : ''}
                ${order.status === 'approved' ? '<i class="fa-solid fa-check"></i>' : ''}
                ${order.status === 'rejected' ? '<i class="fa-solid fa-xmark"></i>' : ''}
                ${order.status}
            </span>
        </td>
        <td>
            <div class="action-btns">
                <button class="btn-approve" data-id="${order.orderId}" ${!isActionable ? 'disabled' : ''}>
                    <i class="fa-solid fa-check"></i> Approve
                </button>
                <button class="btn-reject" data-id="${order.orderId}" ${!isActionable ? 'disabled' : ''}>
                    <i class="fa-solid fa-xmark"></i> Reject
                </button>
            </div>
        </td>
    `;
}

// ---- Screenshot Modal ----
screenshotModalClose.addEventListener('click', () => {
    screenshotModalOverlay.style.display = 'none';
    screenshotModalImg.src = '';
});
screenshotModalOverlay.addEventListener('click', e => {
    if (e.target === screenshotModalOverlay) {
        screenshotModalOverlay.style.display = 'none';
        screenshotModalImg.src = '';
    }
});

// ---- Action Modal ----
function openActionModal(orderId, type) {
    actionTargetOrder = allOrders.find(o => o.orderId === orderId);
    if (!actionTargetOrder) return;
    actionType = type;

    actionNoteInput.value = '';

    if (type === 'approve') {
        actionModalTitle.textContent = 'Approve Order';
        actionModalTitle.style.color = '#22c55e';
        actionModalDesc.textContent = `Approve order ${orderId} for player ${actionTargetOrder.username}?`;
        btnActionConfirm.textContent = 'Approve';
        btnActionConfirm.className = 'btn-action-confirm';

        // Build server commands
        const cmds = buildServerCommands(actionTargetOrder);
        if (cmds.length > 0) {
            actionCommandBox.style.display = 'block';
            actionCommandsList.innerHTML = cmds.map(c => `<div class="action-command-line">${c}</div>`).join('');
        } else {
            actionCommandBox.style.display = 'none';
        }
    } else {
        actionModalTitle.textContent = 'Reject Order';
        actionModalTitle.style.color = '#ef4444';
        actionModalDesc.textContent = `Reject order ${orderId} for player ${actionTargetOrder.username}?`;
        btnActionConfirm.textContent = 'Reject';
        btnActionConfirm.className = 'btn-action-confirm danger';
        actionCommandBox.style.display = 'none';
    }

    actionModalOverlay.style.display = 'flex';
}

btnActionCancel.addEventListener('click', () => {
    actionModalOverlay.style.display = 'none';
    actionTargetOrder = null;
});

btnActionConfirm.addEventListener('click', async () => {
    if (!actionTargetOrder) return;
    const note = actionNoteInput.value.trim();
    btnActionConfirm.disabled = true;
    btnActionConfirm.textContent = 'Processing...';

    try {
        const resp = await fetch('/.netlify/functions/update-order', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Admin-Token': adminToken
            },
            body: JSON.stringify({
                orderId: actionTargetOrder.orderId,
                status: actionType,
                adminNote: note || null
            })
        });
        const data = await resp.json();
        if (!resp.ok || !data.success) throw new Error(data.error || 'Unknown error');

        // Update local state
        const idx = allOrders.findIndex(o => o.orderId === actionTargetOrder.orderId);
        if (idx !== -1) allOrders[idx] = data.order;

        actionModalOverlay.style.display = 'none';
        actionTargetOrder = null;
        updateStats();
        renderTable();
    } catch (err) {
        alert('Failed to update order: ' + err.message);
    } finally {
        btnActionConfirm.disabled = false;
        btnActionConfirm.textContent = actionType === 'approve' ? 'Approve' : 'Reject';
    }
});

// Copy commands button
btnCopyCommands.addEventListener('click', () => {
    const lines = Array.from(actionCommandsList.querySelectorAll('.action-command-line')).map(el => el.textContent.trim());
    navigator.clipboard.writeText(lines.join('\n')).then(() => {
        const orig = btnCopyCommands.innerHTML;
        btnCopyCommands.innerHTML = '<i class="fa-solid fa-check"></i> Copied!';
        setTimeout(() => { btnCopyCommands.innerHTML = orig; }, 2000);
    });
});

// ---- Init ----
if (adminToken) {
    fetchOrders()
        .then(orders => showDashboard(orders))
        .catch(() => showLogin());
} else {
    showLogin();
}
