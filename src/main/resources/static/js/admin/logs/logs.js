let currentPage = 0;
const pageSize = 50;
let totalPages = 0;
let totalElements = 0;
let tailInterval = null;
let tailActive = false;
let logDetailModal;
let deleteModal;

document.addEventListener('DOMContentLoaded', () => {
    if (!auth.requireAuth()) return;

    logDetailModal = new bootstrap.Modal(document.getElementById('logDetailModal'));

    loadServicesDropdown();
    loadFiltersFromUrl();
    loadLogs();
});

// ─── URL state ───────────────────────────────────────────────────────────────

function loadFiltersFromUrl() {
    const params = new URLSearchParams(window.location.search);
    if (params.get('level'))   document.getElementById('filterLevel').value   = params.get('level');
    if (params.get('service')) document.getElementById('filterService').value = params.get('service');
    if (params.get('start'))   document.getElementById('filterStartDate').value = params.get('start');
    if (params.get('end'))     document.getElementById('filterEndDate').value   = params.get('end');
    if (params.get('q'))       document.getElementById('filterSearch').value    = params.get('q');
    if (params.get('page'))    currentPage = parseInt(params.get('page'), 10);
}

function serializeFiltersToUrl() {
    const params = new URLSearchParams();
    const level   = document.getElementById('filterLevel').value;
    const service = document.getElementById('filterService').value;
    const start   = document.getElementById('filterStartDate').value;
    const end     = document.getElementById('filterEndDate').value;
    const q       = document.getElementById('filterSearch').value;

    if (level)   params.set('level', level);
    if (service) params.set('service', service);
    if (start)   params.set('start', start);
    if (end)     params.set('end', end);
    if (q)       params.set('q', q);
    if (currentPage > 0) params.set('page', currentPage);

    const newUrl = `${window.location.pathname}${params.toString() ? '?' + params.toString() : ''}`;
    window.history.replaceState(null, '', newUrl);
}

// ─── Services dropdown ───────────────────────────────────────────────────────

async function loadServicesDropdown() {
    try {
        const services = await API.get('/logs/services');
        const select = document.getElementById('filterService');
        const current = select.value;
        select.innerHTML = '<option value="">All Services</option>' +
            (services || []).map(s => `<option value="${escapeHtml(s)}">${escapeHtml(s)}</option>`).join('');
        if (current) select.value = current;
    } catch (e) {
        console.error('Could not load services', e);
    }
}

// ─── Load logs ───────────────────────────────────────────────────────────────

async function loadLogs() {
    try {
        const params = buildQueryParams();
        serializeFiltersToUrl();
        const response = await API.get(`/logs?${params}`);
        const logs = response.content || [];
        totalElements = response.totalElements || 0;
        totalPages = response.totalPages || 1;

        renderTable(logs);
        updatePagination();
    } catch (e) {
        console.error('Failed to load logs', e);
        showError('Failed to load logs');
    }
}

function buildQueryParams() {
    const params = new URLSearchParams();
    params.set('page', currentPage);
    params.set('size', pageSize);

    const level   = document.getElementById('filterLevel').value;
    const service = document.getElementById('filterService').value;
    const start   = document.getElementById('filterStartDate').value;
    const end     = document.getElementById('filterEndDate').value;
    const q       = document.getElementById('filterSearch').value.trim();

    if (level)   params.set('level', level);
    if (service) params.set('service', service);
    if (start)   params.set('startDate', new Date(start).toISOString());
    if (end)     params.set('endDate', new Date(end).toISOString());
    if (q)       params.set('search', q);

    return params.toString();
}

function applyFilters() {
    currentPage = 0;
    loadLogs();
}

function clearFilters() {
    document.getElementById('filterLevel').value     = '';
    document.getElementById('filterService').value   = '';
    document.getElementById('filterStartDate').value = '';
    document.getElementById('filterEndDate').value   = '';
    document.getElementById('filterSearch').value    = '';
    currentPage = 0;
    loadLogs();
}

function handleSearchKeyup(event) {
    if (event.key === 'Enter') applyFilters();
}

// ─── Tail mode ────────────────────────────────────────────────────────────────

function toggleTail() {
    tailActive = !tailActive;
    const btn    = document.getElementById('tailToggleBtn');
    const status = document.getElementById('tailStatus');

    if (tailActive) {
        btn.innerHTML = '<i class="ti ti-player-stop me-1"></i>Stop Tail';
        btn.classList.replace('btn-outline-secondary', 'btn-danger');
        status.style.display = '';
        tailInterval = setInterval(() => { currentPage = 0; loadLogs(); }, 3000);
    } else {
        btn.innerHTML = '<i class="ti ti-player-play me-1"></i>Live Tail';
        btn.classList.replace('btn-danger', 'btn-outline-secondary');
        status.style.display = 'none';
        clearInterval(tailInterval);
        tailInterval = null;
    }
}

// ─── Render ───────────────────────────────────────────────────────────────────

function renderTable(logs) {
    const tbody = document.getElementById('logsTable');

    if (!logs.length) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">No log entries found</td></tr>';
        return;
    }

    tbody.innerHTML = logs.map(log => `
        <tr>
            <td class="text-nowrap text-muted small">${formatTime(log.logTimestamp)}</td>
            <td><span class="badge bg-${levelColor(log.level)}-lt text-${levelColor(log.level)}">${escapeHtml(log.level)}</span></td>
            <td><span class="text-truncate d-inline-block" style="max-width:130px">${escapeHtml(log.service)}</span></td>
            <td><div class="text-truncate" style="max-width:400px" title="${escapeHtml(log.message)}">${escapeHtml(log.message)}</div></td>
            <td class="text-muted small">${log.traceId ? `<code class="small">${escapeHtml(log.traceId.substring(0,12))}…</code>` : '-'}</td>
            <td>
                <button class="btn btn-sm btn-outline-primary" onclick="viewLog('${log.id}')" title="Details">
                    <i class="ti ti-eye"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

// ─── Pagination ───────────────────────────────────────────────────────────────

function updatePagination() {
    const start = totalElements > 0 ? currentPage * pageSize + 1 : 0;
    const end   = Math.min((currentPage + 1) * pageSize, totalElements);
    document.getElementById('showingStart').textContent = start;
    document.getElementById('showingEnd').textContent   = end;
    document.getElementById('totalItems').textContent   = totalElements;

    const ul = document.getElementById('pagination');
    let html = `<li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
        <a class="page-link" href="#" onclick="goToPage(${currentPage - 1}); return false;"><i class="ti ti-chevron-left"></i></a>
    </li>`;

    const maxVisible = 5;
    let startPage = Math.max(0, currentPage - 2);
    let endPage   = Math.min(totalPages - 1, startPage + maxVisible - 1);
    if (endPage - startPage < maxVisible - 1) startPage = Math.max(0, endPage - maxVisible + 1);

    if (startPage > 0) html += `<li class="page-item"><a class="page-link" href="#" onclick="goToPage(0); return false;">1</a></li>${startPage > 1 ? '<li class="page-item disabled"><span class="page-link">…</span></li>' : ''}`;
    for (let i = startPage; i <= endPage; i++) {
        html += `<li class="page-item ${i === currentPage ? 'active' : ''}"><a class="page-link" href="#" onclick="goToPage(${i}); return false;">${i + 1}</a></li>`;
    }
    if (endPage < totalPages - 1) html += `${endPage < totalPages - 2 ? '<li class="page-item disabled"><span class="page-link">…</span></li>' : ''}<li class="page-item"><a class="page-link" href="#" onclick="goToPage(${totalPages - 1}); return false;">${totalPages}</a></li>`;

    html += `<li class="page-item ${currentPage >= totalPages - 1 ? 'disabled' : ''}">
        <a class="page-link" href="#" onclick="goToPage(${currentPage + 1}); return false;"><i class="ti ti-chevron-right"></i></a>
    </li>`;

    ul.innerHTML = html;
}

function goToPage(page) {
    if (page >= 0 && page < totalPages) {
        currentPage = page;
        loadLogs();
    }
}

// ─── Detail modal ─────────────────────────────────────────────────────────────

async function viewLog(id) {
    try {
        const log = await API.get(`/logs/${id}`);
        renderDetail(log);
        logDetailModal.show();
    } catch (e) {
        showError('Failed to load log entry');
    }
}

function renderDetail(log) {
    let metaHtml = '';
    if (log.metadata) {
        try {
            const parsed = JSON.parse(log.metadata);
            metaHtml = `<div class="mb-3">
                <strong>Metadata</strong>
                <pre class="mt-1 p-2 bg-light rounded" style="white-space:pre-wrap;word-break:break-all;">${escapeHtml(JSON.stringify(parsed, null, 2))}</pre>
            </div>`;
        } catch {
            metaHtml = `<div class="mb-3"><strong>Metadata</strong><pre class="mt-1 p-2 bg-light rounded">${escapeHtml(log.metadata)}</pre></div>`;
        }
    }

    document.getElementById('logDetailBody').innerHTML = `
        <div class="mb-3 d-flex justify-content-between align-items-center">
            <span class="badge bg-${levelColor(log.level)} fs-6">${escapeHtml(log.level)}</span>
            <span class="text-muted">${new Date(log.logTimestamp).toLocaleString()}</span>
        </div>
        <div class="mb-3"><strong>Service:</strong> <code>${escapeHtml(log.service)}</code></div>
        <div class="mb-3">
            <strong>Message</strong>
            <div class="mt-1 p-2 bg-light rounded">${escapeHtml(log.message)}</div>
        </div>
        ${log.traceId  ? `<div class="mb-2"><strong>Trace ID:</strong> <code>${escapeHtml(log.traceId)}</code></div>` : ''}
        ${log.requestId ? `<div class="mb-2"><strong>Request ID:</strong> <code>${escapeHtml(log.requestId)}</code></div>` : ''}
        ${metaHtml}
        <div class="text-muted small">Log ID: ${escapeHtml(log.id)}</div>
    `;
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function levelColor(level) {
    const map = { DEBUG: 'secondary', INFO: 'info', WARNING: 'warning', ERROR: 'danger', CRITICAL: 'danger' };
    return map[level] || 'secondary';
}

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
