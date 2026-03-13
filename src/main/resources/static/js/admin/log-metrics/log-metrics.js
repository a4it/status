document.addEventListener('DOMContentLoaded', () => {
    if (!auth.requireAuth()) return;
    loadMetrics();
    setInterval(loadMetrics, 60000);
});

async function loadMetrics() {
    try {
        const hours = parseInt(document.getElementById('windowSelect').value, 10);
        const since = new Date(Date.now() - hours * 3600 * 1000).toISOString();
        const metrics = await API.get(`/log-metrics?since=${since}`);
        renderSummary(metrics);
        renderTable(metrics);
    } catch (e) {
        showError('Failed to load log metrics');
    }
}

function renderSummary(metrics) {
    const total   = metrics.reduce((sum, m) => sum + (m.count || 0), 0);
    const errors  = metrics.filter(m => m.level === 'ERROR' || m.level === 'CRITICAL').reduce((s, m) => s + m.count, 0);
    const warns   = metrics.filter(m => m.level === 'WARNING').reduce((s, m) => s + m.count, 0);
    const services = new Set(metrics.map(m => m.service)).size;

    document.getElementById('totalCount').textContent   = total.toLocaleString();
    document.getElementById('errorCount').textContent   = errors.toLocaleString();
    document.getElementById('warningCount').textContent = warns.toLocaleString();
    document.getElementById('serviceCount').textContent = services;
}

function renderTable(metrics) {
    const tbody = document.getElementById('metricsTable');
    if (!metrics.length) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-4">No metrics yet — logs are aggregated every minute</td></tr>';
        return;
    }

    const sorted = [...metrics].sort((a, b) => new Date(b.bucket) - new Date(a.bucket));
    tbody.innerHTML = sorted.map(m => `
        <tr>
            <td class="text-muted">${new Date(m.bucket).toLocaleString()}</td>
            <td><code>${escapeHtml(m.service)}</code></td>
            <td><span class="badge bg-${levelColor(m.level)}-lt text-${levelColor(m.level)}">${escapeHtml(m.level)}</span></td>
            <td><strong>${m.count.toLocaleString()}</strong></td>
        </tr>
    `).join('');
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
