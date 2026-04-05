// ─── State ───────────────────────────────────────────────────────────────────

let allCases = [];
let selectedCaseId = null;

// ─── Utilities ───────────────────────────────────────────────────────────────

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function formatDuration(ms) {
    if (!ms || ms < 0) return '—';
    if (ms < 1000) return `${Math.round(ms)}ms`;
    const secs = ms / 1000;
    if (secs < 60) return `${secs.toFixed(1)}s`;
    const mins = Math.floor(secs / 60);
    const remSecs = Math.floor(secs % 60);
    if (mins < 60) return remSecs > 0 ? `${mins}m ${remSecs}s` : `${mins}m`;
    const hours = Math.floor(mins / 60);
    const remMins = mins % 60;
    if (hours < 24) return remMins > 0 ? `${hours}h ${remMins}m` : `${hours}h`;
    const days = Math.floor(hours / 24);
    return `${days}d ${hours % 24}h`;
}

function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `alert alert-${type} alert-dismissible fade show`;
    toast.style.cssText = 'position:fixed;top:76px;right:20px;z-index:9999;min-width:300px;max-width:480px;box-shadow:0 4px 12px rgba(0,0,0,.15);';
    toast.innerHTML = `${escapeHtml(message)}<button type="button" class="btn-close" onclick="this.parentElement.remove()"></button>`;
    document.body.appendChild(toast);
    setTimeout(() => { if (toast.parentElement) toast.remove(); }, 5000);
}

const LEVEL_COLORS = {
    CRITICAL: '#e53e3e',
    ERROR:    '#dd6b20',
    WARNING:  '#d69e2e',
    INFO:     '#3182ce',
    DEBUG:    '#718096'
};

const LEVEL_RANK = { CRITICAL: 5, ERROR: 4, WARNING: 3, INFO: 2, DEBUG: 1 };

function getLevelColor(level) {
    return LEVEL_COLORS[level] || '#718096';
}

function getWorstLevel(events) {
    let worst = 'INFO';
    let worstRank = 0;
    for (const e of events) {
        const rank = LEVEL_RANK[e.level] || 0;
        if (rank > worstRank) { worstRank = rank; worst = e.level; }
    }
    return worst;
}

function caseDurationMs(caseObj) {
    if (!caseObj.events || caseObj.events.length < 2) return 0;
    const times = caseObj.events.map(e => new Date(e.timestamp).getTime());
    return Math.max(...times) - Math.min(...times);
}

// ─── Page initialisation ─────────────────────────────────────────────────────

async function initPage() {
    if (!auth.requireAuth()) return;
    setDefaultDates();
    await loadScopeEntities();
    updateUserInfo();
}

function updateUserInfo() {
    const user = auth.getUser ? auth.getUser() : null;
    if (user) {
        const el = document.getElementById('userAvatar');
        if (el) el.textContent = (user.username || user.name || 'A')[0].toUpperCase();
    }
}

function setDefaultDates() {
    const now = new Date();
    const yesterday = new Date(now - 86400000);
    const toLocal = d => {
        const off = d.getTimezoneOffset() * 60000;
        return new Date(d - off).toISOString().slice(0, 16);
    };
    document.getElementById('tlFrom').value = toLocal(yesterday);
    document.getElementById('tlTo').value = toLocal(now);
}

function onScopeTypeChange() {
    const scopeType = document.querySelector('input[name="scopeType"]:checked').value;
    document.getElementById('tlScopeLabel').textContent =
        scopeType === 'platform' ? 'Platform' : 'Application';
    loadScopeEntities();
}

async function loadScopeEntities() {
    const scopeType = document.querySelector('input[name="scopeType"]:checked').value;
    const select = document.getElementById('tlScopeEntity');
    select.innerHTML = '<option value="">Loading…</option>';
    try {
        let items;
        if (scopeType === 'platform') {
            items = await api.get('/status-platforms/all');
        } else {
            const resp = await api.get('/status-apps?size=500&sort=name,asc');
            items = resp.content || [];
        }
        select.innerHTML = '<option value="">Select…</option>';
        items.forEach(item => {
            const opt = document.createElement('option');
            opt.value = item.id;
            opt.textContent = item.name;
            select.appendChild(opt);
        });
    } catch (e) {
        select.innerHTML = '<option value="">Error loading</option>';
        console.warn('Failed to load scope entities', e);
    }
}

// ─── Data loading ─────────────────────────────────────────────────────────────

async function loadData() {
    const scopeType = document.querySelector('input[name="scopeType"]:checked').value;
    const scopeId   = document.getElementById('tlScopeEntity').value;
    const from      = document.getElementById('tlFrom').value;
    const to        = document.getElementById('tlTo').value;
    const maxCases  = Math.min(1000, parseInt(document.getElementById('tlMaxCases').value) || 300);
    const minEvents = Math.max(1, parseInt(document.getElementById('tlMinEvents').value) || 2);

    if (!scopeId) {
        showToast('Please select a platform or application first.', 'warning');
        return;
    }

    const loadBtn = document.getElementById('tlLoadBtn');
    loadBtn.disabled = true;
    loadBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Loading…';

    try {
        const params = new URLSearchParams({ scope: scopeType, scopeId, maxCases, minEvents });
        if (from) params.append('from', new Date(from).toISOString());
        if (to)   params.append('to',   new Date(to).toISOString());

        const response = await api.get(`/logs/process-mining?${params}`);
        allCases = response.cases || [];
        selectedCaseId = null;

        const total = response.totalCases || 0;
        document.getElementById('tlCasesCount').textContent =
            `${total.toLocaleString()} trace${total !== 1 ? 's' : ''}`;

        renderCaseList();
        renderEmptyDetail();

        if (response.truncated) showToast('Results truncated — increase max cases for more.', 'warning');
        if (total === 0)        showToast('No traces found for this time window.', 'info');
    } catch (e) {
        showToast('Failed to load data: ' + (e.message || e), 'danger');
    } finally {
        loadBtn.disabled = false;
        loadBtn.innerHTML = '<i class="ti ti-player-play me-1"></i>Load';
    }
}

// ─── Cases list ──────────────────────────────────────────────────────────────

function renderCaseList() {
    const search = (document.getElementById('tlSearch').value || '').toLowerCase();
    const sort   = document.getElementById('tlSort').value;

    let cases = allCases.filter(c =>
        !search || c.caseId.toLowerCase().includes(search)
    );

    cases.sort((a, b) => {
        if (sort === 'events')   return b.events.length - a.events.length;
        if (sort === 'duration') return caseDurationMs(b) - caseDurationMs(a);
        if (sort === 'severity') return (LEVEL_RANK[getWorstLevel(b.events)] || 0) - (LEVEL_RANK[getWorstLevel(a.events)] || 0);
        return 0;
    });

    const container = document.getElementById('tlCaseList');
    if (cases.length === 0) {
        container.innerHTML = `<div class="text-center text-muted py-4 px-2" style="font-size:13px;">
            ${allCases.length === 0
                ? '<i class="ti ti-player-play d-block mb-2" style="font-size:24px;"></i>Load data to see traces.'
                : 'No traces match your search.'}
        </div>`;
        return;
    }

    container.innerHTML = cases.map(c => {
        const dur     = caseDurationMs(c);
        const worst   = getWorstLevel(c.events);
        const color   = getLevelColor(worst);
        const isSelected = c.caseId === selectedCaseId;
        const shortId = c.caseId.length > 22
            ? c.caseId.slice(0, 9) + '…' + c.caseId.slice(-7)
            : c.caseId;
        return `<div class="tl-case-item${isSelected ? ' selected' : ''}" data-case-id="${escapeHtml(c.caseId)}">
            <div class="d-flex justify-content-between align-items-center mb-1">
                <span class="tl-case-id" title="${escapeHtml(c.caseId)}">${escapeHtml(shortId)}</span>
                <span class="badge" style="background:${color};color:#fff;font-size:10px;padding:2px 6px;">${escapeHtml(worst)}</span>
            </div>
            <div class="tl-case-meta">
                <span><i class="ti ti-list-check" style="font-size:11px;"></i> ${c.events.length} events</span>
                <span><i class="ti ti-clock" style="font-size:11px;"></i> ${formatDuration(dur)}</span>
            </div>
        </div>`;
    }).join('');

    container.querySelectorAll('.tl-case-item').forEach(el => {
        el.addEventListener('click', () => selectCase(el.dataset.caseId));
    });
}

function selectCase(caseId) {
    selectedCaseId = caseId;
    const caseObj = allCases.find(c => c.caseId === caseId);
    if (!caseObj) return;

    document.querySelectorAll('.tl-case-item').forEach(el => {
        el.classList.toggle('selected', el.dataset.caseId === caseId);
    });

    renderCaseDetail(caseObj);
}

// ─── Case detail ─────────────────────────────────────────────────────────────

function renderEmptyDetail() {
    document.getElementById('tlDetail').innerHTML = `
        <div class="tl-empty-state">
            <i class="ti ti-timeline-event" style="font-size:52px;color:#dee2e6;"></i>
            <div class="mt-3 text-muted" style="font-size:14px;">Select a trace from the list to explore its path</div>
        </div>`;
}

function renderCaseDetail(caseObj) {
    document.getElementById('tlDetail').innerHTML =
        renderCaseStatsHtml(caseObj) +
        renderServicePathHtml(caseObj) +
        renderSwimlaneHtml(caseObj) +
        renderEventTableHtml(caseObj);
}

function renderCaseStatsHtml(caseObj) {
    const dur     = caseDurationMs(caseObj);
    const worst   = getWorstLevel(caseObj.events);
    const color   = getLevelColor(worst);
    const times   = caseObj.events.map(e => new Date(e.timestamp).getTime());
    const startTs = new Date(Math.min(...times));
    const services = new Set(caseObj.events.map(e => e.activity));

    return `<div class="tl-stats-row">
        <div class="tl-stat">
            <div class="tl-stat-label">Trace ID</div>
            <div class="tl-stat-value tl-mono" title="${escapeHtml(caseObj.caseId)}">${escapeHtml(
                caseObj.caseId.length > 20 ? caseObj.caseId.slice(0, 8) + '…' + caseObj.caseId.slice(-6) : caseObj.caseId
            )}</div>
        </div>
        <div class="tl-stat">
            <div class="tl-stat-label">Duration</div>
            <div class="tl-stat-value">${formatDuration(dur)}</div>
        </div>
        <div class="tl-stat">
            <div class="tl-stat-label">Events</div>
            <div class="tl-stat-value">${caseObj.events.length}</div>
        </div>
        <div class="tl-stat">
            <div class="tl-stat-label">Services</div>
            <div class="tl-stat-value">${services.size}</div>
        </div>
        <div class="tl-stat">
            <div class="tl-stat-label">Started</div>
            <div class="tl-stat-value">${startTs.toLocaleTimeString()}</div>
        </div>
        <div class="tl-stat">
            <div class="tl-stat-label">Severity</div>
            <div class="tl-stat-value">
                <span class="badge" style="background:${color};color:#fff;">${escapeHtml(worst)}</span>
            </div>
        </div>
    </div>`;
}

function renderServicePathHtml(caseObj) {
    const seen = new Set();
    const path = [];
    for (const e of caseObj.events) {
        if (!seen.has(e.activity)) {
            seen.add(e.activity);
            const svcEvents = caseObj.events.filter(ev => ev.activity === e.activity);
            path.push({ name: e.activity, worst: getWorstLevel(svcEvents) });
        }
    }

    const chips = path.map((svc, i) => {
        const color = getLevelColor(svc.worst);
        const arrow = i < path.length - 1
            ? '<span class="tl-path-arrow">→</span>'
            : '';
        return `<span class="tl-path-chip" style="border-color:${color};color:${color};">${escapeHtml(svc.name)}</span>${arrow}`;
    }).join('');

    return `<div class="tl-section">
        <div class="tl-section-title"><i class="ti ti-route me-1"></i>Service Path</div>
        <div class="tl-path-flow">${chips}</div>
    </div>`;
}

function renderSwimlaneHtml(caseObj) {
    const times   = caseObj.events.map(e => new Date(e.timestamp).getTime());
    const minTime = Math.min(...times);
    const maxTime = Math.max(...times);
    const span    = maxTime - minTime || 1;

    const services = [...new Set(caseObj.events.map(e => e.activity))];

    const rows = services.map(svc => {
        const svcEvents = caseObj.events
            .filter(e => e.activity === svc)
            .map(e => ({ ...e, xPct: ((new Date(e.timestamp).getTime() - minTime) / span) * 100 }));

        const dots = svcEvents.map(e =>
            `<div class="tl-dot" style="left:calc(${e.xPct.toFixed(2)}% - 7px);background:${getLevelColor(e.level)};"
                  title="${escapeHtml(e.level + ': ' + e.message)}"></div>`
        ).join('');

        return `<div class="tl-sw-row">
            <div class="tl-sw-label" title="${escapeHtml(svc)}">${escapeHtml(svc)}</div>
            <div class="tl-sw-track">
                <div class="tl-sw-line"></div>
                ${dots}
            </div>
        </div>`;
    }).join('');

    const mid = formatDuration(span / 2);
    const end = formatDuration(span);

    return `<div class="tl-section">
        <div class="tl-section-title"><i class="ti ti-chart-gantt me-1"></i>Swimlane Timeline</div>
        <div class="tl-swimlane">
            ${rows}
            <div class="tl-sw-axis">
                <span>0</span>
                <span>${escapeHtml(mid)}</span>
                <span>${escapeHtml(end)}</span>
            </div>
        </div>
    </div>`;
}

function renderEventTableHtml(caseObj) {
    const rows = caseObj.events.map((e, i) => {
        const color = getLevelColor(e.level);
        const dt    = i === 0 ? '—' : formatDuration(
            new Date(e.timestamp).getTime() - new Date(caseObj.events[i - 1].timestamp).getTime()
        );
        const ts = new Date(e.timestamp);
        return `<tr>
            <td class="text-nowrap" style="font-size:12px;color:#888;white-space:nowrap;">${escapeHtml(ts.toLocaleTimeString())}</td>
            <td class="text-nowrap" style="font-size:12px;color:#aaa;white-space:nowrap;">${escapeHtml(dt)}</td>
            <td><span class="badge bg-azure-lt text-azure" style="font-size:11px;">${escapeHtml(e.activity)}</span></td>
            <td><span class="badge" style="background:${color};color:#fff;font-size:11px;">${escapeHtml(e.level)}</span></td>
            <td style="font-size:13px;">${escapeHtml(e.message)}</td>
        </tr>`;
    }).join('');

    return `<div class="tl-section">
        <div class="tl-section-title"><i class="ti ti-list me-1"></i>Events</div>
        <div class="table-responsive">
            <table class="table table-sm table-vcenter mb-0">
                <thead>
                    <tr>
                        <th style="width:85px;">Time</th>
                        <th style="width:65px;">+Δ</th>
                        <th style="width:150px;">Service</th>
                        <th style="width:90px;">Level</th>
                        <th>Message</th>
                    </tr>
                </thead>
                <tbody>${rows}</tbody>
            </table>
        </div>
    </div>`;
}

// ─── Bootstrap ───────────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => initPage());
