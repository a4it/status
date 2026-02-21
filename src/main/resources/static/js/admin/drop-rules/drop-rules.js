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
        const rules = await API.get('/drop-rules');
        renderTable(rules);
    } catch (e) {
        showError('Failed to load drop rules');
    }
}

function renderTable(rules) {
    const tbody = document.getElementById('rulesTable');
    if (!rules.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">No drop rules configured</td></tr>';
        return;
    }
    tbody.innerHTML = rules.map(r => `
        <tr>
            <td><strong>${escapeHtml(r.name)}</strong></td>
            <td>${r.level ? `<span class="badge bg-${levelColor(r.level)}">${r.level}</span>` : '<span class="text-muted">Any</span>'}</td>
            <td>${r.service ? escapeHtml(r.service) : '<span class="text-muted">Any</span>'}</td>
            <td>${r.messagePattern ? `<code>${escapeHtml(r.messagePattern)}</code>` : '<span class="text-muted">Any</span>'}</td>
            <td>
                <span class="badge ${r.isActive ? 'bg-green-lt text-green' : 'bg-secondary-lt text-secondary'}">
                    ${r.isActive ? 'Active' : 'Disabled'}
                </span>
            </td>
            <td class="text-muted small">${r.createdDate ? new Date(r.createdDate).toLocaleDateString() : '-'}</td>
            <td>
                <button class="btn btn-sm btn-outline-secondary me-1" onclick="toggleRule('${r.id}')" title="${r.isActive ? 'Disable' : 'Enable'}">
                    <i class="ti ti-${r.isActive ? 'toggle-right' : 'toggle-left'}"></i>
                </button>
                <button class="btn btn-sm btn-outline-primary me-1" onclick="openEditModal(${JSON.stringify(r).replace(/"/g, '&quot;')})" title="Edit">
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
    document.getElementById('ruleModalTitle').textContent = 'New Drop Rule';
    document.getElementById('ruleId').value = '';
    document.getElementById('ruleName').value = '';
    document.getElementById('ruleLevel').value = '';
    document.getElementById('ruleService').value = '';
    document.getElementById('rulePattern').value = '';
    document.getElementById('ruleActive').checked = true;
    ruleModal.show();
}

function openEditModal(rule) {
    editingId = rule.id;
    document.getElementById('ruleModalTitle').textContent = 'Edit Drop Rule';
    document.getElementById('ruleId').value = rule.id;
    document.getElementById('ruleName').value = rule.name || '';
    document.getElementById('ruleLevel').value = rule.level || '';
    document.getElementById('ruleService').value = rule.service || '';
    document.getElementById('rulePattern').value = rule.messagePattern || '';
    document.getElementById('ruleActive').checked = !!rule.isActive;
    ruleModal.show();
}

async function saveRule() {
    const name    = document.getElementById('ruleName').value.trim();
    const level   = document.getElementById('ruleLevel').value || null;
    const service = document.getElementById('ruleService').value.trim() || null;
    const pattern = document.getElementById('rulePattern').value.trim() || null;
    const active  = document.getElementById('ruleActive').checked;

    if (!name) { showError('Name is required'); return; }

    const payload = { name, level, service, messagePattern: pattern, active };

    try {
        if (editingId) {
            await API.put(`/drop-rules/${editingId}`, payload);
            showSuccess('Drop rule updated');
        } else {
            await API.post('/drop-rules', payload);
            showSuccess('Drop rule created');
        }
        ruleModal.hide();
        loadRules();
    } catch (e) {
        showError(e.message || 'Failed to save drop rule');
    }
}

async function toggleRule(id) {
    try {
        await API.post(`/drop-rules/${id}/toggle`, {});
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
        await API.delete(`/drop-rules/${id}`);
        showSuccess('Rule deleted');
        loadRules();
    } catch (e) {
        showError('Failed to delete rule');
    }
}

function levelColor(level) {
    const map = { DEBUG: 'secondary', INFO: 'info', WARNING: 'warning', ERROR: 'danger', CRITICAL: 'danger' };
    return map[level] || 'secondary';
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
