// ─── State ────────────────────────────────────────────────────────────────────

let autoTailActive = false;
let autoTailInterval = null;

// ─── Init ─────────────────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => {
    if (!auth.requireAuth()) return;

    loadAppLog();
    loadLoggers();

    // When switching to syslog tab, load it if not yet loaded
    document.getElementById('tab-syslog').addEventListener('shown.bs.tab', () => {
        if (!document.getElementById('syslogOutput').dataset.loaded) {
            loadSyslog();
        }
    });
});

// ─── App Log ──────────────────────────────────────────────────────────────────

async function loadAppLog() {
    const lines = document.getElementById('appLogLines').value;
    const search = document.getElementById('appLogSearch').value.trim();
    const pre = document.getElementById('appLogOutput');
    pre.textContent = 'Loading…';

    try {
        const params = new URLSearchParams({ lines, search });
        const data = await API.get(`/log-viewer/app-log?${params}`);
        renderLogOutput(pre, data, 'appLogInfo', 'appLogTruncated');
    } catch (e) {
        pre.textContent = 'Error loading app log: ' + (e.message || JSON.stringify(e));
        showToast('Failed to load app log', 'danger');
    }
}

function handleAppLogSearchKeyup(event) {
    if (event.key === 'Enter') loadAppLog();
}

// ─── Syslog ───────────────────────────────────────────────────────────────────

async function loadSyslog() {
    const lines = document.getElementById('syslogLines').value;
    const search = document.getElementById('syslogSearch').value.trim();
    const pre = document.getElementById('syslogOutput');
    pre.textContent = 'Loading…';
    pre.dataset.loaded = 'true';

    try {
        const params = new URLSearchParams({ lines, search });
        const data = await API.get(`/log-viewer/syslog?${params}`);
        renderLogOutput(pre, data, 'syslogInfo', 'syslogTruncated');
    } catch (e) {
        pre.textContent = 'Error loading syslog: ' + (e.message || JSON.stringify(e));
        showToast('Failed to load syslog', 'danger');
    }
}

function handleSyslogSearchKeyup(event) {
    if (event.key === 'Enter') loadSyslog();
}

// ─── Render log output ────────────────────────────────────────────────────────

function renderLogOutput(pre, data, infoId, truncatedId) {
    const lines = data.lines || [];

    if (!lines.length) {
        pre.innerHTML = '<span class="text-muted">No log lines found.</span>';
    } else {
        pre.innerHTML = lines.map(line => colorLine(escapeHtml(line))).join('\n');
        // Scroll to bottom
        pre.scrollTop = pre.scrollHeight;
    }

    // Info badge
    const sizeKb = data.fileSizeBytes > 0 ? ' | ' + formatBytes(data.fileSizeBytes) : '';
    document.getElementById(infoId).textContent =
        (data.filePath || '') + sizeKb + ' | ' + lines.length + ' lines';

    // Truncated warning
    document.getElementById(truncatedId).style.display = data.truncated ? '' : 'none';
}

function colorLine(escapedLine) {
    if (/\bERROR\b/.test(escapedLine)) {
        return `<span class="text-danger">${escapedLine}</span>`;
    }
    if (/\bWARN\b/.test(escapedLine)) {
        return `<span class="text-warning">${escapedLine}</span>`;
    }
    if (/\bDEBUG\b/.test(escapedLine)) {
        return `<span class="text-muted">${escapedLine}</span>`;
    }
    if (/\bINFO\b/.test(escapedLine)) {
        return `<span class="text-info">${escapedLine}</span>`;
    }
    return escapedLine;
}

// ─── Log Levels ───────────────────────────────────────────────────────────────

async function loadLoggers() {
    try {
        const loggers = await API.get('/log-viewer/loggers');
        renderLoggersTable(loggers);
    } catch (e) {
        document.getElementById('loggersTable').innerHTML =
            '<tr><td colspan="4" class="text-center text-danger">Failed to load loggers</td></tr>';
    }
}

function renderLoggersTable(loggers) {
    const tbody = document.getElementById('loggersTable');
    if (!loggers || !loggers.length) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted">No loggers found</td></tr>';
        return;
    }

    const levels = ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR', 'OFF', 'DEFAULT'];

    tbody.innerHTML = loggers.map(logger => {
        const effectiveBadge = levelBadge(logger.effectiveLevel);
        const configuredBadge = logger.configuredLevel
            ? levelBadge(logger.configuredLevel)
            : '<span class="text-muted small">inherited</span>';

        const levelButtons = levels.map(lvl => {
            const isActive = logger.configuredLevel === lvl || (lvl === 'DEFAULT' && !logger.configuredLevel);
            const btnClass = isActive ? 'btn-primary' : 'btn-outline-secondary';
            return `<button class="btn btn-sm ${btnClass}" onclick="setLevel('${escapeAttr(logger.name)}', '${lvl}')" title="Set ${lvl}">${lvl}</button>`;
        }).join('');

        return `<tr>
            <td><code class="small">${escapeHtml(logger.name)}</code></td>
            <td>${effectiveBadge}</td>
            <td>${configuredBadge}</td>
            <td><div class="btn-group btn-group-sm flex-wrap gap-1">${levelButtons}</div></td>
        </tr>`;
    }).join('');
}

async function setLevel(name, level) {
    try {
        await API.put(`/log-viewer/loggers/${encodeURIComponent(name)}`, { level });
        showToast(`${name} set to ${level}`, 'success');
        loadLoggers();
    } catch (e) {
        showToast('Failed to set log level: ' + (e.message || JSON.stringify(e)), 'danger');
    }
}

async function resetAllLoggers() {
    const tbody = document.getElementById('loggersTable');
    const rows = tbody.querySelectorAll('code.small');
    const names = Array.from(rows).map(el => el.textContent);

    let failed = 0;
    for (const name of names) {
        try {
            await API.put(`/log-viewer/loggers/${encodeURIComponent(name)}`, { level: 'DEFAULT' });
        } catch (e) {
            failed++;
        }
    }

    if (failed === 0) {
        showToast('All loggers reset to inherited', 'success');
    } else {
        showToast(`Reset complete with ${failed} error(s)`, 'warning');
    }
    loadLoggers();
}

function levelBadge(level) {
    const map = {
        TRACE: 'secondary',
        DEBUG: 'secondary',
        INFO: 'info',
        WARN: 'warning',
        ERROR: 'danger',
        OFF: 'dark',
        INHERITED: 'light'
    };
    const color = map[level] || 'secondary';
    return `<span class="badge bg-${color}-lt text-${color}">${escapeHtml(level)}</span>`;
}

// ─── Auto Tail ────────────────────────────────────────────────────────────────

function toggleAutoTail() {
    autoTailActive = !autoTailActive;
    const btn = document.getElementById('autoTailBtn');

    if (autoTailActive) {
        btn.innerHTML = '<i class="ti ti-player-stop me-1"></i>Stop Refresh';
        btn.classList.replace('btn-outline-secondary', 'btn-danger');
        autoTailInterval = setInterval(() => {
            const activeTab = document.querySelector('#logViewerTabs .nav-link.active');
            if (!activeTab) return;
            const href = activeTab.getAttribute('href');
            if (href === '#tabAppLog') loadAppLog();
            else if (href === '#tabSyslog') loadSyslog();
        }, 5000);
    } else {
        btn.innerHTML = '<i class="ti ti-player-play me-1"></i>Auto Refresh';
        btn.classList.replace('btn-danger', 'btn-outline-secondary');
        clearInterval(autoTailInterval);
        autoTailInterval = null;
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function escapeHtml(text) {
    if (text == null) return '';
    const d = document.createElement('div');
    d.textContent = String(text);
    return d.innerHTML;
}

function escapeAttr(text) {
    if (text == null) return '';
    return String(text).replace(/'/g, "\\'");
}

function formatBytes(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

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
