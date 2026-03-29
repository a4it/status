let ruleModal;
let runResultModal;
let deleteModal;
let allPlatforms = [];
let pendingDeleteId = null;

document.addEventListener('DOMContentLoaded', () => {
    if (!auth.requireAuth()) return;

    ruleModal      = new bootstrap.Modal(document.getElementById('ruleModal'));
    runResultModal = new bootstrap.Modal(document.getElementById('runResultModal'));
    deleteModal    = new bootstrap.Modal(document.getElementById('deleteModal'));

    loadTenants();
    loadPlatforms();
    loadRules();
});

// ─── Load rules ───────────────────────────────────────────────────────────────

async function loadRules() {
    try {
        const rules = await API.get('/process-mining/retention');
        renderTable(rules || []);
        updateStats(rules || []);
    } catch (e) {
        console.error('Failed to load retention rules', e);
        showError('Failed to load retention rules');
    }
}

// ─── Render ───────────────────────────────────────────────────────────────────

function renderTable(rules) {
    const tbody = document.getElementById('rulesTable');

    if (!rules.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">No retention rules configured</td></tr>';
        return;
    }

    tbody.innerHTML = rules.map(rule => `
        <tr style="cursor:pointer;" onclick="openEditModal('${rule.id}')">
            <td>
                <span class="d-flex align-items-center">
                    <i class="ti ti-${rule.platformId ? 'server' : 'server-2'} me-2 text-muted"></i>
                    ${escapeHtml(rule.platformName || 'All Platforms')}
                </span>
            </td>
            <td>${rule.tenantName ? escapeHtml(rule.tenantName) : '<span class="text-muted">All Tenants</span>'}</td>
            <td>
                <span class="badge bg-blue-lt">${rule.retentionDays} days</span>
            </td>
            <td>
                ${rule.enabled
                    ? '<span class="badge bg-success-lt text-success">Active</span>'
                    : '<span class="badge bg-secondary-lt text-secondary">Disabled</span>'}
            </td>
            <td class="text-muted small">${rule.lastRunAt ? formatTime(rule.lastRunAt) : '<span class="text-muted">Never</span>'}</td>
            <td class="text-muted">${rule.lastRunDeletedCount != null ? rule.lastRunDeletedCount.toLocaleString() : '-'}</td>
            <td onclick="event.stopPropagation()">
                <button class="btn btn-sm btn-outline-primary me-1" onclick="openEditModal('${rule.id}')" title="Edit">
                    <i class="ti ti-edit"></i>
                </button>
                <button class="btn btn-sm btn-outline-danger" onclick="confirmDelete('${rule.id}')" title="Delete">
                    <i class="ti ti-trash"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

function updateStats(rules) {
    document.getElementById('statTotal').textContent = rules.length;
    document.getElementById('statActive').textContent = rules.filter(r => r.enabled).length;

    const lastRuns = rules
        .filter(r => r.lastRunAt)
        .map(r => new Date(r.lastRunAt).getTime());

    if (lastRuns.length) {
        const latest = new Date(Math.max(...lastRuns));
        document.getElementById('statLastRun').textContent = latest.toLocaleString();
    } else {
        document.getElementById('statLastRun').textContent = 'Never';
    }
}

// ─── Tenants dropdown ─────────────────────────────────────────────────────────

async function loadTenants() {
    try {
        const resp = await API.get('/tenants?size=200');
        const tenants = resp.content || resp || [];
        const select = document.getElementById('ruleTenantId');
        const current = select.value;
        select.innerHTML = '<option value="">All Tenants</option>' +
            tenants.map(t => `<option value="${t.id}">${escapeHtml(t.name)}</option>`).join('');
        if (current) select.value = current;
    } catch (e) {
        console.error('Could not load tenants', e);
    }
}

// ─── Platforms dropdown ───────────────────────────────────────────────────────

async function loadPlatforms() {
    try {
        const resp = await API.get('/status-platforms?size=500');
        allPlatforms = resp.content || resp || [];
        renderPlatformsSelect(allPlatforms);
    } catch (e) {
        console.error('Could not load platforms', e);
    }
}

function renderPlatformsSelect(platforms) {
    const select = document.getElementById('rulePlatformId');
    const current = select.value;
    select.innerHTML = '<option value="">All Platforms</option>' +
        platforms.map(p => `<option value="${p.id}">${escapeHtml(p.name)}</option>`).join('');
    if (current) select.value = current;
}

function onTenantChange() {
    const tenantId = document.getElementById('ruleTenantId').value;
    if (tenantId) {
        const filtered = allPlatforms.filter(p => p.tenantId === tenantId || !p.tenantId);
        renderPlatformsSelect(filtered);
    } else {
        renderPlatformsSelect(allPlatforms);
    }
}

// ─── Add / Edit modal ─────────────────────────────────────────────────────────

function openAddModal() {
    document.getElementById('ruleModalTitle').textContent = 'Add Retention Rule';
    document.getElementById('ruleId').value = '';
    document.getElementById('ruleTenantId').value = '';
    document.getElementById('rulePlatformId').value = '';
    document.getElementById('ruleRetentionDays').value = 30;
    document.getElementById('ruleEnabled').checked = true;
    renderPlatformsSelect(allPlatforms);
    ruleModal.show();
}

async function openEditModal(id) {
    try {
        const rule = await API.get(`/process-mining/retention/${id}`);
        document.getElementById('ruleModalTitle').textContent = 'Edit Retention Rule';
        document.getElementById('ruleId').value = rule.id;
        document.getElementById('ruleTenantId').value = rule.tenantId || '';
        onTenantChange();
        document.getElementById('rulePlatformId').value = rule.platformId || '';
        document.getElementById('ruleRetentionDays').value = rule.retentionDays;
        document.getElementById('ruleEnabled').checked = rule.enabled;
        ruleModal.show();
    } catch (e) {
        console.error('Failed to load rule', e);
        showError('Failed to load retention rule');
    }
}

async function saveRule() {
    const id = document.getElementById('ruleId').value;
    const tenantId   = document.getElementById('ruleTenantId').value   || null;
    const platformId = document.getElementById('rulePlatformId').value || null;
    const retentionDays = parseInt(document.getElementById('ruleRetentionDays').value, 10);
    const enabled = document.getElementById('ruleEnabled').checked;

    if (!retentionDays || retentionDays < 1 || retentionDays > 3650) {
        showError('Retention days must be between 1 and 3650');
        return;
    }

    const body = { tenantId, platformId, retentionDays, enabled };

    try {
        if (id) {
            await API.put(`/process-mining/retention/${id}`, body);
            showSuccess('Retention rule updated');
        } else {
            await API.post('/process-mining/retention', body);
            showSuccess('Retention rule created');
        }
        ruleModal.hide();
        loadRules();
    } catch (e) {
        showError(e.message || 'Failed to save retention rule');
    }
}

// ─── Delete ───────────────────────────────────────────────────────────────────

function confirmDelete(id) {
    pendingDeleteId = id;
    document.getElementById('confirmDeleteBtn').onclick = () => deleteRule(id);
    deleteModal.show();
}

async function deleteRule(id) {
    try {
        await API.delete(`/process-mining/retention/${id}`);
        deleteModal.hide();
        showSuccess('Retention rule deleted');
        loadRules();
    } catch (e) {
        showError(e.message || 'Failed to delete retention rule');
    }
}

// ─── Run Now ──────────────────────────────────────────────────────────────────

async function runRetentionNow() {
    try {
        const result = await API.post('/process-mining/retention/run', {});
        renderRunResult(result);
        runResultModal.show();
        loadRules();
    } catch (e) {
        showError(e.message || 'Failed to run retention');
    }
}

function renderRunResult(result) {
    const details = (result.details || []).map(d => `
        <tr>
            <td>${escapeHtml(d.platform)}</td>
            <td>${escapeHtml(d.tenant)}</td>
            <td>${d.retentionDays} days</td>
            <td>${d.deletedCount.toLocaleString()}</td>
        </tr>
    `).join('');

    document.getElementById('runResultBody').innerHTML = `
        <div class="row mb-3">
            <div class="col-6">
                <div class="card">
                    <div class="card-body text-center">
                        <div class="subheader">Rules Processed</div>
                        <div class="h2">${result.rulesProcessed}</div>
                    </div>
                </div>
            </div>
            <div class="col-6">
                <div class="card">
                    <div class="card-body text-center">
                        <div class="subheader">Total Deleted</div>
                        <div class="h2">${(result.totalDeleted || 0).toLocaleString()}</div>
                    </div>
                </div>
            </div>
        </div>
        ${details ? `
        <div class="table-responsive">
            <table class="table table-vcenter table-sm">
                <thead>
                    <tr>
                        <th>Platform</th>
                        <th>Tenant</th>
                        <th>Retention</th>
                        <th>Deleted</th>
                    </tr>
                </thead>
                <tbody>${details}</tbody>
            </table>
        </div>` : '<p class="text-muted">No enabled rules to process.</p>'}
        <div class="text-muted small mt-2">Run at: ${result.runAt}</div>
    `;
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function formatTime(ts) {
    if (!ts) return '-';
    const d = new Date(ts);
    const diff = Date.now() - d;
    if (diff < 60000)    return 'just now';
    if (diff < 3600000)  return `${Math.floor(diff / 60000)}m ago`;
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}h ago`;
    return d.toLocaleString();
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
