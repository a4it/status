let allMetrics = [];
let sortCol = 'count';
let sortDir = 'desc';
let filterService = '';
let filterLevel = '';

document.addEventListener('DOMContentLoaded', () => {
    if (!auth.requireAuth()) return;

    // Restore persisted filter/sort state
    const user = auth.getUser();
    const stateKey = `log_metrics_state_${user ? user.username : 'default'}`;
    const saved = JSON.parse(localStorage.getItem(stateKey) || '{}');
    if (saved.sortCol) sortCol = saved.sortCol;
    if (saved.sortDir) sortDir = saved.sortDir;
    if (saved.filterService) filterService = saved.filterService;
    if (saved.filterLevel)   filterLevel   = saved.filterLevel;
    if (saved.window) {
        const sel = document.getElementById('windowSelect');
        if (sel) sel.value = saved.window;
    }

    loadMetrics();
    setInterval(loadMetrics, 60000);

    document.getElementById('serviceFilter').addEventListener('input', e => {
        filterService = e.target.value.trim().toLowerCase();
        saveState();
        renderTable();
    });

    document.getElementById('levelFilter').addEventListener('change', e => {
        filterLevel = e.target.value;
        saveState();
        renderTable();
    });

    function saveState() {
        localStorage.setItem(stateKey, JSON.stringify({
            sortCol, sortDir, filterService, filterLevel,
            window: document.getElementById('windowSelect').value
        }));
    }

    // Restore filter UI
    document.getElementById('serviceFilter').value = filterService;
    document.getElementById('levelFilter').value = filterLevel;

    // Column sorting
    document.querySelectorAll('th[data-col]').forEach(th => {
        th.style.cursor = 'pointer';
        th.addEventListener('click', () => {
            const col = th.dataset.col;
            if (sortCol === col) {
                sortDir = sortDir === 'asc' ? 'desc' : 'asc';
            } else {
                sortCol = col;
                sortDir = col === 'count' ? 'desc' : 'asc';
            }
            saveState();
            updateSortIcons();
            renderTable();
        });
    });

    updateSortIcons();
});

async function loadMetrics() {
    try {
        const hours = parseInt(document.getElementById('windowSelect').value, 10);
        const since = new Date(Date.now() - hours * 3600 * 1000).toISOString();
        allMetrics = await API.get(`/log-metrics?since=${encodeURIComponent(since)}`);
        if (!Array.isArray(allMetrics)) allMetrics = [];
        renderSummary(allMetrics);
        populateServiceFilter(allMetrics);
        renderTable();
    } catch (e) {
        showError('Failed to load log metrics');
    }
}

function renderSummary(metrics) {
    const total    = metrics.reduce((sum, m) => sum + (m.count || 0), 0);
    const errors   = metrics.filter(m => m.level === 'ERROR' || m.level === 'CRITICAL').reduce((s, m) => s + m.count, 0);
    const warns    = metrics.filter(m => m.level === 'WARNING' || m.level === 'WARN').reduce((s, m) => s + m.count, 0);
    const services = new Set(metrics.map(m => m.service)).size;

    document.getElementById('totalCount').textContent   = total.toLocaleString();
    document.getElementById('errorCount').textContent   = errors.toLocaleString();
    document.getElementById('warningCount').textContent = warns.toLocaleString();
    document.getElementById('serviceCount').textContent = services;
}

function populateServiceFilter(metrics) {
    const sel = document.getElementById('serviceFilter');
    // keep as text input — just a placeholder hint; populate datalist
    const dl = document.getElementById('serviceList');
    if (!dl) return;
    const services = [...new Set(metrics.map(m => m.service))].sort();
    dl.innerHTML = services.map(s => `<option value="${escapeHtml(s)}">`).join('');
}

function renderTable() {
    const tbody = document.getElementById('metricsTable');

    // Aggregate: group by service + level, sum counts
    const grouped = {};
    for (const m of allMetrics) {
        const levelMatch = !filterLevel || m.level === filterLevel;
        const serviceMatch = !filterService || (m.service || '').toLowerCase().includes(filterService);
        if (!levelMatch || !serviceMatch) continue;
        const key = `${m.service}|||${m.level}`;
        if (!grouped[key]) grouped[key] = { service: m.service, level: m.level, count: 0 };
        grouped[key].count += m.count || 0;
    }

    const rows = Object.values(grouped);

    if (!rows.length) {
        const msg = allMetrics.length === 0
            ? 'No metrics yet — logs are aggregated every minute'
            : 'No results match the current filters';
        tbody.innerHTML = `<tr><td colspan="3" class="text-center text-muted py-4">${msg}</td></tr>`;
        return;
    }

    // Sort
    rows.sort((a, b) => {
        let av = a[sortCol], bv = b[sortCol];
        if (typeof av === 'string') av = av.toLowerCase();
        if (typeof bv === 'string') bv = bv.toLowerCase();
        if (av < bv) return sortDir === 'asc' ? -1 : 1;
        if (av > bv) return sortDir === 'asc' ? 1 : -1;
        return 0;
    });

    const totalCount = rows.reduce((s, r) => s + r.count, 0);

    tbody.innerHTML = rows.map(r => {
        const pct = totalCount > 0 ? ((r.count / totalCount) * 100).toFixed(1) : '0.0';
        const color = levelColor(r.level);
        return `
        <tr>
            <td><code>${escapeHtml(r.service)}</code></td>
            <td><span class="badge bg-${color}-lt text-${color}">${escapeHtml(r.level)}</span></td>
            <td>
                <div class="d-flex align-items-center gap-2">
                    <strong>${r.count.toLocaleString()}</strong>
                    <div class="progress flex-grow-1" style="height:4px;">
                        <div class="progress-bar bg-${color}" style="width:${pct}%"></div>
                    </div>
                    <small class="text-muted">${pct}%</small>
                </div>
            </td>
        </tr>`;
    }).join('');
}

function updateSortIcons() {
    document.querySelectorAll('th[data-col]').forEach(th => {
        const icon = th.querySelector('.sort-icon');
        if (!icon) return;
        const col = th.dataset.col;
        if (col === sortCol) {
            icon.textContent = sortDir === 'asc' ? ' ↑' : ' ↓';
        } else {
            icon.textContent = '';
        }
    });
}

function levelColor(level) {
    const map = { DEBUG: 'secondary', INFO: 'info', WARNING: 'warning', WARN: 'warning', ERROR: 'danger', CRITICAL: 'danger' };
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
