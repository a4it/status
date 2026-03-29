let autoRefreshInterval = null;

document.addEventListener('DOMContentLoaded', () => {
    if (!auth.requireAuth()) return;

    loadStats();
    loadScheduleConfig();

    document.getElementById('autoRefreshToggle').addEventListener('change', function () {
        if (this.checked) {
            autoRefreshInterval = setInterval(loadStats, 5000);
        } else {
            clearInterval(autoRefreshInterval);
            autoRefreshInterval = null;
        }
    });
});

// ─── Stats ────────────────────────────────────────────────────────────────────

async function loadStats() {
    try {
        const stats = await API.get('/jvm/stats');
        renderStats(stats);
    } catch (e) {
        console.error('Failed to load JVM stats', e);
        showError('Failed to load JVM stats');
    }
}

function renderStats(stats) {
    // Heap memory
    const heapUsedMb = formatBytes(stats.heapUsed);
    const heapMaxMb  = formatBytes(stats.heapMax);
    document.getElementById('heapUsed').textContent = heapUsedMb;
    document.getElementById('heapMax').textContent  = heapMaxMb;
    document.getElementById('heapCommitted').textContent = 'Committed: ' + formatBytes(stats.heapCommitted);

    const heapPct = stats.heapMax > 0 ? Math.round((stats.heapUsed / stats.heapMax) * 100) : 0;
    const bar = document.getElementById('heapProgress');
    bar.style.width = heapPct + '%';
    bar.classList.remove('bg-blue', 'bg-yellow', 'bg-red');
    if (heapPct >= 85) {
        bar.classList.add('bg-red');
    } else if (heapPct >= 65) {
        bar.classList.add('bg-yellow');
    } else {
        bar.classList.add('bg-blue');
    }

    // Non-heap memory
    document.getElementById('nonHeapUsed').textContent      = formatBytes(stats.nonHeapUsed);
    document.getElementById('nonHeapCommitted').textContent = 'Committed: ' + formatBytes(stats.nonHeapCommitted);

    // Threads
    document.getElementById('threadCount').textContent = stats.threadCount;
    document.getElementById('daemonCount').textContent  = stats.daemonThreadCount;
    document.getElementById('peakCount').textContent    = stats.peakThreadCount;

    // Uptime
    document.getElementById('uptimeValue').textContent = formatUptime(stats.uptimeMs);

    // CPU
    const cpuEl = document.getElementById('cpuLoad');
    if (stats.processCpuLoad < 0) {
        cpuEl.textContent = 'N/A';
    } else {
        cpuEl.textContent = (stats.processCpuLoad * 100).toFixed(1) + '%';
    }

    // GC table
    renderGcTable(stats.gcCollectors || []);

    // Last GC run from schedule info
    if (stats.gcSchedule && stats.gcSchedule.lastRunAtMs) {
        document.getElementById('lastGcRun').textContent =
            'Last run: ' + new Date(stats.gcSchedule.lastRunAtMs).toLocaleString();
    }
}

function renderGcTable(collectors) {
    const tbody = document.getElementById('gcTable');
    if (!collectors.length) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted">No GC collectors found</td></tr>';
        return;
    }
    tbody.innerHTML = collectors.map(gc => {
        const avg = gc.collectionCount > 0
            ? (gc.collectionTimeMs / gc.collectionCount).toFixed(1)
            : '0';
        return `<tr>
            <td>${escapeHtml(gc.name)}</td>
            <td class="text-end">${gc.collectionCount}</td>
            <td class="text-end">${gc.collectionTimeMs.toLocaleString()}</td>
            <td class="text-end">${avg}</td>
        </tr>`;
    }).join('');
}

// ─── Schedule config ──────────────────────────────────────────────────────────

async function loadScheduleConfig() {
    try {
        const config = await API.get('/jvm/gc/schedule');
        document.getElementById('gcEnabled').checked = config.enabled;
        document.getElementById('cronExpression').value = config.cron || '0 0 * * * *';
        parseCronToFields();
    } catch (e) {
        console.error('Failed to load GC schedule config', e);
    }
}

function toggleGcEnabled() {
    saveSchedule();
}

function applyPreset() {
    const preset = document.getElementById('cronPreset').value;
    if (preset) {
        document.getElementById('cronExpression').value = preset;
        parseCronToFields();
    }
}

function rebuildCron() {
    const sec   = document.getElementById('cronSec').value   || '*';
    const min   = document.getElementById('cronMin').value   || '*';
    const hour  = document.getElementById('cronHour').value  || '*';
    const day   = document.getElementById('cronDay').value   || '*';
    const month = document.getElementById('cronMonth').value || '*';
    const wday  = document.getElementById('cronWday').value  || '*';
    document.getElementById('cronExpression').value = `${sec} ${min} ${hour} ${day} ${month} ${wday}`;

    // Reset preset selector since we're in custom mode
    document.getElementById('cronPreset').value = '';
}

function parseCronToFields() {
    const expr = document.getElementById('cronExpression').value.trim();
    if (!expr) return;
    const parts = expr.split(/\s+/);
    if (parts.length !== 6) return;

    document.getElementById('cronSec').value   = parts[0];
    document.getElementById('cronMin').value   = parts[1];
    document.getElementById('cronHour').value  = parts[2];
    document.getElementById('cronDay').value   = parts[3];
    document.getElementById('cronMonth').value = parts[4];
    document.getElementById('cronWday').value  = parts[5];

    // Try to match a preset
    const presetSelect = document.getElementById('cronPreset');
    let matched = false;
    for (const option of presetSelect.options) {
        if (option.value === expr) {
            presetSelect.value = option.value;
            matched = true;
            break;
        }
    }
    if (!matched) presetSelect.value = '';
}

async function saveSchedule() {
    const enabled = document.getElementById('gcEnabled').checked;
    const cron    = document.getElementById('cronExpression').value.trim();

    if (enabled && !cron) {
        showError('Please enter a cron expression before enabling the schedule.');
        return;
    }

    try {
        await API.put('/jvm/gc/schedule', { enabled, cron });
        showSuccess('GC schedule saved');
    } catch (e) {
        console.error('Failed to save schedule', e);
        showError(e.message || 'Failed to save schedule');
    }
}

async function triggerGcNow() {
    try {
        await API.post('/jvm/gc/run', {});
        showSuccess('GC triggered successfully');
        setTimeout(loadStats, 1500);
    } catch (e) {
        console.error('Failed to trigger GC', e);
        showError(e.message || 'Failed to trigger GC');
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function formatBytes(bytes) {
    if (bytes == null || bytes < 0) return 'N/A';
    if (bytes === 0) return '0 B';
    const gb = bytes / (1024 * 1024 * 1024);
    if (gb >= 1) return gb.toFixed(2) + ' GB';
    const mb = bytes / (1024 * 1024);
    if (mb >= 1) return mb.toFixed(1) + ' MB';
    const kb = bytes / 1024;
    return kb.toFixed(1) + ' KB';
}

function formatUptime(ms) {
    if (ms == null || ms < 0) return 'N/A';
    const totalSeconds = Math.floor(ms / 1000);
    const days    = Math.floor(totalSeconds / 86400);
    const hours   = Math.floor((totalSeconds % 86400) / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);

    const parts = [];
    if (days > 0)    parts.push(`${days}d`);
    if (hours > 0)   parts.push(`${hours}h`);
    parts.push(`${minutes}m`);
    return parts.join(' ');
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
