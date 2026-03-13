let ruleModal;
let deleteModal;
let deleteCallback = null;
let editingId = null;

document.addEventListener('DOMContentLoaded', () => {
    if (!auth.requireAuth()) return;
    ruleModal   = new bootstrap.Modal(document.getElementById('ruleModal'));
    deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));

    document.getElementById('confirmDeleteBtn').addEventListener('click', () => {
        if (deleteCallback) { deleteCallback(); deleteModal.hide(); }
    });

    loadRules();
});

async function loadRules() {
    try {
        const rules = await API.get('/alert-rules');
        renderTable(rules);
    } catch (e) {
        showError('Failed to load alert rules');
    }
}

function renderTable(rules) {
    const tbody = document.getElementById('rulesTable');
    if (!rules.length) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center text-muted py-4">No alert rules configured</td></tr>';
        return;
    }
    tbody.innerHTML = rules.map(r => `
        <tr>
            <td><strong>${escapeHtml(r.name)}</strong></td>
            <td>${r.service ? `<code>${escapeHtml(r.service)}</code>` : '<span class="text-muted">All</span>'}</td>
            <td>${r.level ? `<span class="badge bg-${levelColor(r.level)}">${r.level}</span>` : '<span class="text-muted">All</span>'}</td>
            <td><strong>${r.thresholdCount}</strong></td>
            <td>${r.windowMinutes}m</td>
            <td>
                <span class="badge bg-azure-lt text-azure">
                    <i class="ti ti-${notifyIcon(r.notificationType)} me-1"></i>${r.notificationType}
                </span>
            </td>
            <td>
                <span class="badge ${r.isActive ? 'bg-green-lt text-green' : 'bg-secondary-lt text-secondary'}">
                    ${r.isActive ? 'Active' : 'Disabled'}
                </span>
            </td>
            <td class="text-muted small">${r.lastFiredAt ? new Date(r.lastFiredAt).toLocaleString() : 'Never'}</td>
            <td>
                <button class="btn btn-sm btn-outline-secondary me-1" onclick="toggleRule('${r.id}')" title="${r.isActive ? 'Disable' : 'Enable'}">
                    <i class="ti ti-${r.isActive ? 'toggle-right' : 'toggle-left'}"></i>
                </button>
                <button class="btn btn-sm btn-outline-primary me-1" onclick='openEditModal(${JSON.stringify(r)})' title="Edit">
                    <i class="ti ti-edit"></i>
                </button>
                <button class="btn btn-sm btn-danger" onclick="confirmDelete('${r.id}')" title="Delete">
                    <i class="ti ti-trash"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

function openCreateModal() {
    editingId = null;
    document.getElementById('ruleModalTitle').textContent = 'New Alert Rule';
    document.getElementById('ruleId').value = '';
    document.getElementById('ruleName').value = '';
    document.getElementById('ruleService').value = '';
    document.getElementById('ruleLevel').value = '';
    document.getElementById('ruleThreshold').value = '50';
    document.getElementById('ruleWindow').value = '5';
    document.getElementById('ruleCooldown').value = '15';
    document.getElementById('ruleNotifyType').value = 'EMAIL';
    document.getElementById('ruleTarget').value = '';
    document.getElementById('ruleActive').checked = true;
    updateTargetLabel();
    ruleModal.show();
}

function openEditModal(rule) {
    editingId = rule.id;
    document.getElementById('ruleModalTitle').textContent = 'Edit Alert Rule';
    document.getElementById('ruleId').value = rule.id;
    document.getElementById('ruleName').value = rule.name || '';
    document.getElementById('ruleService').value = rule.service || '';
    document.getElementById('ruleLevel').value = rule.level || '';
    document.getElementById('ruleThreshold').value = rule.thresholdCount || 50;
    document.getElementById('ruleWindow').value = rule.windowMinutes || 5;
    document.getElementById('ruleCooldown').value = rule.cooldownMinutes || 15;
    document.getElementById('ruleNotifyType').value = rule.notificationType || 'EMAIL';
    document.getElementById('ruleTarget').value = rule.notificationTarget || '';
    document.getElementById('ruleActive').checked = !!rule.isActive;
    updateTargetLabel();
    ruleModal.show();
}

function updateTargetLabel() {
    const type = document.getElementById('ruleNotifyType').value;
    const labels = { EMAIL: 'Email address', SLACK: 'Slack webhook URL', WEBHOOK: 'HTTP webhook URL' };
    const placeholders = { EMAIL: 'alerts@example.com', SLACK: 'https://hooks.slack.com/...', WEBHOOK: 'https://your-endpoint.com/...' };
    document.getElementById('targetLabel').textContent = labels[type] || 'Target';
    document.getElementById('ruleTarget').placeholder = placeholders[type] || '';
}

async function saveRule() {
    const name       = document.getElementById('ruleName').value.trim();
    const service    = document.getElementById('ruleService').value.trim() || null;
    const level      = document.getElementById('ruleLevel').value || null;
    const threshold  = parseInt(document.getElementById('ruleThreshold').value, 10);
    const window_    = parseInt(document.getElementById('ruleWindow').value, 10);
    const cooldown   = parseInt(document.getElementById('ruleCooldown').value, 10);
    const notifyType = document.getElementById('ruleNotifyType').value;
    const target     = document.getElementById('ruleTarget').value.trim() || null;
    const active     = document.getElementById('ruleActive').checked;

    if (!name) { showError('Name is required'); return; }
    if (!threshold || threshold < 1) { showError('Threshold must be at least 1'); return; }
    if (!window_ || window_ < 1) { showError('Window must be at least 1 minute'); return; }

    const payload = {
        name, service, level,
        thresholdCount: threshold,
        windowMinutes: window_,
        cooldownMinutes: cooldown,
        notificationType: notifyType,
        notificationTarget: target,
        active
    };

    try {
        if (editingId) {
            await API.put(`/alert-rules/${editingId}`, payload);
            showSuccess('Alert rule updated');
        } else {
            await API.post('/alert-rules', payload);
            showSuccess('Alert rule created');
        }
        ruleModal.hide();
        loadRules();
    } catch (e) {
        showError(e.message || 'Failed to save alert rule');
    }
}

async function toggleRule(id) {
    try {
        await API.post(`/alert-rules/${id}/toggle`, {});
        loadRules();
    } catch (e) {
        showError('Failed to toggle rule');
    }
}

function confirmDelete(id) {
    deleteCallback = () => deleteRule(id);
    deleteModal.show();
}

async function deleteRule(id) {
    try {
        await API.delete(`/alert-rules/${id}`);
        showSuccess('Alert rule deleted');
        loadRules();
    } catch (e) {
        showError('Failed to delete rule');
    }
}

function levelColor(level) {
    const map = { DEBUG: 'secondary', INFO: 'info', WARNING: 'warning', ERROR: 'danger', CRITICAL: 'danger' };
    return map[level] || 'secondary';
}

function notifyIcon(type) {
    const map = { EMAIL: 'mail', SLACK: 'brand-slack', WEBHOOK: 'webhook' };
    return map[type] || 'bell';
}

function escapeHtml(text) {
    if (text == null) return '';
    const d = document.createElement('div');
    d.textContent = String(text);
    return d.innerHTML;
}

function showSuccess(msg) { showToast(msg, 'success'); }
function showError(msg)   { showToast(msg, 'danger'); }

function showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.className = 'toast-container position-fixed top-0 end-0 p-3';
        container.style.zIndex = '1100';
        document.body.appendChild(container);
    }
    const toast = document.createElement('div');
    toast.className = `toast align-items-center text-white bg-${type} border-0`;
    toast.setAttribute('role', 'alert');
    toast.innerHTML = `<div class="d-flex"><div class="toast-body">${escapeHtml(message)}</div><button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div>`;
    container.appendChild(toast);
    const t = new bootstrap.Toast(toast, { autohide: true, delay: 3000 });
    t.show();
    toast.addEventListener('hidden.bs.toast', () => toast.remove());
}
