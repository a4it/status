let keyModal;
let deleteModal;
let deleteCallback = null;

document.addEventListener('DOMContentLoaded', () => {
    if (!auth.requireAuth()) return;
    keyModal    = new bootstrap.Modal(document.getElementById('keyModal'));
    deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));

    document.getElementById('confirmDeleteBtn').addEventListener('click', () => {
        if (deleteCallback) { deleteCallback(); deleteModal.hide(); }
    });

    loadKeys();
});

async function loadKeys() {
    try {
        const keys = await API.get('/log-api-keys');
        renderTable(keys);
    } catch (e) {
        showError('Failed to load API keys');
    }
}

function renderTable(keys) {
    const tbody = document.getElementById('keysTable');
    if (!keys.length) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-4">No API keys yet — create one to start ingesting logs</td></tr>';
        return;
    }
    tbody.innerHTML = keys.map(k => `
        <tr>
            <td><strong>${escapeHtml(k.name)}</strong></td>
            <td>
                <span class="font-monospace text-muted small">${escapeHtml(k.keyPrefix)}••••••••••••••••••••••••••••••••••• <em class="text-secondary">(key shown only at creation)</em></span>
            </td>
            <td>
                <span class="badge ${k.isActive ? 'bg-green-lt text-green' : 'bg-secondary-lt text-secondary'}">
                    ${k.isActive ? 'Active' : 'Disabled'}
                </span>
            </td>
            <td class="text-muted small">${k.createdDate ? new Date(k.createdDate).toLocaleString() : '-'}</td>
            <td>
                <button class="btn btn-sm btn-outline-secondary me-1" onclick="toggleKey('${k.id}')" title="${k.isActive ? 'Disable' : 'Enable'}">
                    <i class="ti ti-${k.isActive ? 'toggle-right' : 'toggle-left'}"></i>
                </button>
                <button class="btn btn-sm btn-danger" onclick="confirmDelete('${k.id}')" title="Revoke">
                    <i class="ti ti-trash"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

function openCreateModal() {
    document.getElementById('keyName').value = '';
    keyModal.show();
}

async function createKey() {
    const name = document.getElementById('keyName').value.trim();
    if (!name) { showError('Name is required'); return; }

    try {
        const created = await API.post('/log-api-keys', { name });
        keyModal.hide();
        loadKeys();
        // MED-03: show the full key exactly once — it cannot be retrieved again
        if (created && created.rawKey) {
            showKeyOnce(created.rawKey);
        } else {
            showSuccess('API key created');
        }
    } catch (e) {
        showError(e.message || 'Failed to create API key');
    }
}

function showKeyOnce(rawKey) {
    const msg = `<strong>Save your API key — it will not be shown again.</strong><br>
        <code class="user-select-all">${escapeHtml(rawKey)}</code>`;
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.className = 'toast-container position-fixed top-0 end-0 p-3';
        container.style.zIndex = '1100';
        document.body.appendChild(container);
    }
    const toast = document.createElement('div');
    toast.className = 'toast align-items-center text-dark bg-warning border-0';
    toast.setAttribute('role', 'alert');
    toast.innerHTML = `<div class="d-flex"><div class="toast-body">${msg}</div><button type="button" class="btn-close me-2 m-auto" data-bs-dismiss="toast"></button></div>`;
    container.appendChild(toast);
    new bootstrap.Toast(toast, { autohide: false }).show();
}

async function toggleKey(id) {
    try {
        await API.post(`/log-api-keys/${id}/toggle`, {});
        loadKeys();
    } catch (e) {
        showError('Failed to toggle key');
    }
}

function confirmDelete(id) {
    deleteCallback = () => deleteKey(id);
    deleteModal.show();
}

async function deleteKey(id) {
    try {
        await API.delete(`/log-api-keys/${id}`);
        showSuccess('API key revoked');
        loadKeys();
    } catch (e) {
        showError('Failed to revoke API key');
    }
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
