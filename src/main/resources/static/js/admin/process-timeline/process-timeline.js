// ─── State ───────────────────────────────────────────────────────────────────

let allCases = [];
let selectedCaseId = null;
let compareMode = false;
let selectedForCompare = []; // ordered array of caseId strings, max 4
let bpmnViewer = null;
let activeDetailTab = 'timeline';
let currentCaseObj = null;

// ─── Utilities ───────────────────────────────────────────────────────────────

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function escapeXml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&apos;');
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
        selectedForCompare = [];
        if (compareMode) toggleCompareMode();

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
        const dur        = caseDurationMs(c);
        const worst      = getWorstLevel(c.events);
        const color      = getLevelColor(worst);
        const isSelected = c.caseId === selectedCaseId;
        const inCompare  = selectedForCompare.includes(c.caseId);
        const cmpIdx     = selectedForCompare.indexOf(c.caseId);
        const shortId    = c.caseId.length > 22
            ? c.caseId.slice(0, 9) + '…' + c.caseId.slice(-7)
            : c.caseId;
        const itemClass  = [
            'tl-case-item',
            isSelected && !compareMode ? 'selected' : '',
            inCompare ? 'compare-selected' : ''
        ].filter(Boolean).join(' ');
        return `<div class="${itemClass}" data-case-id="${escapeHtml(c.caseId)}">
            <div class="d-flex justify-content-between align-items-center mb-1 gap-1">
                <div class="d-flex align-items-center gap-1" style="min-width:0;overflow:hidden;">
                    <input type="checkbox" class="tl-compare-check" ${inCompare ? 'checked' : ''}
                           onclick="event.stopPropagation();toggleCompareSelect('${escapeHtml(c.caseId)}')">
                    <span class="tl-compare-badge${inCompare ? ' visible' : ''}">${cmpIdx + 1}</span>
                    <span class="tl-case-id" title="${escapeHtml(c.caseId)}">${escapeHtml(shortId)}</span>
                </div>
                <span class="badge flex-shrink-0" style="background:${color};color:#fff;font-size:10px;padding:2px 6px;">${escapeHtml(worst)}</span>
            </div>
            <div class="tl-case-meta">
                <span><i class="ti ti-list-check" style="font-size:11px;"></i> ${c.events.length} events</span>
                <span><i class="ti ti-clock" style="font-size:11px;"></i> ${formatDuration(dur)}</span>
            </div>
        </div>`;
    }).join('');

    container.querySelectorAll('.tl-case-item').forEach(el => {
        el.addEventListener('click', () => {
            if (compareMode) toggleCompareSelect(el.dataset.caseId);
            else selectCase(el.dataset.caseId);
        });
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
    if (bpmnViewer) { bpmnViewer.destroy(); bpmnViewer = null; }
    currentCaseObj = caseObj;
    activeDetailTab = 'timeline';

    const timelineContent =
        renderCaseStatsHtml(caseObj) +
        renderServicePathHtml(caseObj) +
        renderSwimlaneHtml(caseObj) +
        renderEventTableHtml(caseObj);

    document.getElementById('tlDetail').innerHTML = `
        <div class="tl-detail-tabs">
            <button class="tl-detail-tab-btn active" id="tlTabTimeline"
                    onclick="switchDetailTab('timeline')">
                <i class="ti ti-chart-gantt me-1"></i>Timeline
            </button>
            <button class="tl-detail-tab-btn" id="tlTabBpmn"
                    onclick="switchDetailTab('bpmn')">
                <i class="ti ti-git-branch me-1"></i>BPMN Diagram
            </button>
        </div>
        <div id="tlTimelinePanel">${timelineContent}</div>
        <div id="tlBpmnPanel" style="display:none;">
            <div id="tlBpmnContainer">
                <div id="tlBpmnCanvas"></div>
                <div class="tl-bpmn-loading" id="tlBpmnLoading" style="display:none;">
                    <span class="spinner-border spinner-border-sm"></span> Rendering…
                </div>
            </div>
        </div>`;
}

function switchDetailTab(tab) {
    activeDetailTab = tab;
    document.getElementById('tlTabTimeline').classList.toggle('active', tab === 'timeline');
    document.getElementById('tlTabBpmn').classList.toggle('active', tab === 'bpmn');
    document.getElementById('tlTimelinePanel').style.display = tab === 'timeline' ? '' : 'none';
    document.getElementById('tlBpmnPanel').style.display     = tab === 'bpmn'     ? '' : 'none';
    if (tab === 'bpmn' && currentCaseObj) renderBpmnDiagram(currentCaseObj);
}

function generateBpmnXml(caseObj) {
    // ── Layout constants ──────────────────────────────────────────────────────
    const POOL_HDR_W = 30;
    const LANE_HDR_W = 120;
    const LANE_H     = 110;
    const TASK_W     = 140;
    const TASK_H     = 50;
    const SE_R       = 18;   // start/end event radius
    const SE_D       = SE_R * 2;
    const COL_W      = 170;
    const L_PAD      = 20;
    const R_PAD      = 40;
    const CONTENT_X  = POOL_HDR_W + LANE_HDR_W; // 150

    const events    = caseObj.events;
    const N         = events.length;

    // ── Derive lanes (unique services, in order of first appearance) ──────────
    const laneNames = [...new Set(events.map(e => e.activity))];
    const laneIdx   = Object.fromEntries(laneNames.map((n, i) => [n, i]));
    const numLanes  = laneNames.length;

    // ── Coordinate helpers ────────────────────────────────────────────────────
    const taskX    = i  => CONTENT_X + L_PAD + SE_D + 10 + i * COL_W;
    const taskTopY = li => li * LANE_H + Math.round((LANE_H - TASK_H) / 2);
    const taskCtrY = li => li * LANE_H + Math.round(LANE_H / 2);

    const firstLane = laneIdx[events[0].activity];
    const lastLane  = laneIdx[events[N - 1].activity];

    const startX    = CONTENT_X + L_PAD;
    const startTopY = li => li * LANE_H + Math.round((LANE_H - SE_D) / 2);
    const endX      = taskX(N - 1) + TASK_W + 10;

    const POOL_W = endX + SE_D + R_PAD;
    const POOL_H = numLanes * LANE_H;

    // ── Level → bioc colors ───────────────────────────────────────────────────
    const BIOC = {
        CRITICAL: { fill: '#fde8e8', stroke: '#dc3545' },
        ERROR:    { fill: '#fef0e6', stroke: '#fd7e14' },
        WARNING:  { fill: '#fff8e1', stroke: '#ffc107' },
        INFO:     { fill: '#e8f4fd', stroke: '#0d6efd' },
        DEBUG:    { fill: '#f0f0f0', stroke: '#6c757d' },
    };
    const bioc = level => {
        const c = BIOC[level] || BIOC.DEBUG;
        return `bioc:fill="${c.fill}" bioc:stroke="${c.stroke}"`;
    };

    // ── Task display name ─────────────────────────────────────────────────────
    const taskName = e => {
        const msg = e.message || '';
        const short = msg.length > 28 ? msg.slice(0, 28) + '…' : msg;
        return escapeXml(e.level + ': ' + short);
    };

    // ── Build semantic XML ────────────────────────────────────────────────────
    // Lane flowNodeRef lists
    const laneRefs = laneNames.map(() => []);
    laneRefs[firstLane].push('startEvt');
    events.forEach((e, i) => laneRefs[laneIdx[e.activity]].push('t_' + i));
    laneRefs[lastLane].push('endEvt');

    const lanesXml = laneNames.map((name, li) =>
        `<lane id="lane_${li}" name="${escapeXml(name)}">` +
        laneRefs[li].map(ref => `<flowNodeRef>${ref}</flowNodeRef>`).join('') +
        `</lane>`
    ).join('');

    const tasksXml = events.map((e, i) =>
        `<task id="t_${i}" name="${taskName(e)}" ${bioc(e.level)}/>`
    ).join('');

    const flowsXml =
        `<sequenceFlow id="sf_s" sourceRef="startEvt" targetRef="t_0"/>` +
        events.slice(0, N - 1).map((_, i) =>
            `<sequenceFlow id="sf_${i}" sourceRef="t_${i}" targetRef="t_${i + 1}"/>`
        ).join('') +
        `<sequenceFlow id="sf_e" sourceRef="t_${N - 1}" targetRef="endEvt"/>`;

    // ── Build DI shapes ───────────────────────────────────────────────────────
    const poolShape = `<bpmndi:BPMNShape id="pool1_di" bpmnElement="pool1" isHorizontal="true">
      <dc:Bounds x="0" y="0" width="${POOL_W}" height="${POOL_H}"/>
    </bpmndi:BPMNShape>`;

    const laneShapes = laneNames.map((_, li) =>
        `<bpmndi:BPMNShape id="lane_${li}_di" bpmnElement="lane_${li}" isHorizontal="true">
      <dc:Bounds x="${POOL_HDR_W}" y="${li * LANE_H}" width="${POOL_W - POOL_HDR_W}" height="${LANE_H}"/>
    </bpmndi:BPMNShape>`
    ).join('');

    const startShape = `<bpmndi:BPMNShape id="startEvt_di" bpmnElement="startEvt">
      <dc:Bounds x="${startX}" y="${startTopY(firstLane)}" width="${SE_D}" height="${SE_D}"/>
    </bpmndi:BPMNShape>`;

    const taskShapes = events.map((e, i) => {
        const li = laneIdx[e.activity];
        return `<bpmndi:BPMNShape id="t_${i}_di" bpmnElement="t_${i}">
      <dc:Bounds x="${taskX(i)}" y="${taskTopY(li)}" width="${TASK_W}" height="${TASK_H}"/>
    </bpmndi:BPMNShape>`;
    }).join('');

    const endShape = `<bpmndi:BPMNShape id="endEvt_di" bpmnElement="endEvt">
      <dc:Bounds x="${endX}" y="${startTopY(lastLane)}" width="${SE_D}" height="${SE_D}"/>
    </bpmndi:BPMNShape>`;

    // ── Build DI edges ────────────────────────────────────────────────────────
    const wp = pts => pts.map(p => `<di:waypoint x="${Math.round(p.x)}" y="${Math.round(p.y)}"/>`).join('');

    const crossLaneWp = (x1, y1, x2, y2) => {
        if (y1 === y2) return [{ x: x1, y: y1 }, { x: x2, y: y2 }];
        const midX = Math.round((x1 + x2) / 2);
        return [{ x: x1, y: y1 }, { x: midX, y: y1 }, { x: midX, y: y2 }, { x: x2, y: y2 }];
    };

    const startEdge = `<bpmndi:BPMNEdge id="sf_s_di" bpmnElement="sf_s">
      ${wp([{ x: startX + SE_D, y: taskCtrY(firstLane) }, { x: taskX(0), y: taskCtrY(firstLane) }])}
    </bpmndi:BPMNEdge>`;

    const taskEdges = events.slice(0, N - 1).map((e, i) => {
        const srcLi = laneIdx[events[i].activity];
        const tgtLi = laneIdx[events[i + 1].activity];
        const x1 = taskX(i) + TASK_W, y1 = taskCtrY(srcLi);
        const x2 = taskX(i + 1),      y2 = taskCtrY(tgtLi);
        return `<bpmndi:BPMNEdge id="sf_${i}_di" bpmnElement="sf_${i}">
      ${wp(crossLaneWp(x1, y1, x2, y2))}
    </bpmndi:BPMNEdge>`;
    }).join('');

    const endEdge = `<bpmndi:BPMNEdge id="sf_e_di" bpmnElement="sf_e">
      ${wp([{ x: taskX(N - 1) + TASK_W, y: taskCtrY(lastLane) }, { x: endX, y: taskCtrY(lastLane) }])}
    </bpmndi:BPMNEdge>`;

    // ── Assemble ──────────────────────────────────────────────────────────────
    const shortId = caseObj.caseId.length > 20
        ? caseObj.caseId.slice(0, 8) + '…' + caseObj.caseId.slice(-6)
        : caseObj.caseId;

    return `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
             xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
             xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
             xmlns:bioc="http://bpmn.io/schema/bpmn/biocolor/1.0"
             targetNamespace="http://status-monitor/bpmn">
  <collaboration id="collab1">
    <participant id="pool1" name="Trace: ${escapeXml(shortId)}" processRef="proc1"/>
  </collaboration>
  <process id="proc1" isExecutable="false">
    <laneSet id="ls1">${lanesXml}</laneSet>
    <startEvent id="startEvt"/>
    ${tasksXml}
    <endEvent id="endEvt"/>
    ${flowsXml}
  </process>
  <bpmndi:BPMNDiagram id="dia1">
    <bpmndi:BPMNPlane id="plane1" bpmnElement="collab1">
      ${poolShape}
      ${laneShapes}
      ${startShape}
      ${taskShapes}
      ${endShape}
      ${startEdge}
      ${taskEdges}
      ${endEdge}
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</definitions>`;
}

async function renderBpmnDiagram(caseObj) {
    const container = document.getElementById('tlBpmnContainer');
    const loading   = document.getElementById('tlBpmnLoading');
    if (!container) return;

    if (typeof window.BpmnJS === 'undefined') {
        container.innerHTML = '<div class="tl-bpmn-error">bpmn-viewer.production.min.js did not load — check the script tag.</div>';
        return;
    }

    loading.style.display = 'flex';
    if (bpmnViewer) { bpmnViewer.destroy(); bpmnViewer = null; }
    bpmnViewer = new window.BpmnJS({ container: '#tlBpmnCanvas' });

    try {
        const xml = generateBpmnXml(caseObj);
        await bpmnViewer.importXML(xml);
        bpmnViewer.get('canvas').zoom('fit-viewport', 'auto');
    } catch (err) {
        console.error('BPMN render error:', err);
        container.innerHTML = `<div class="tl-bpmn-error">Failed to render BPMN: ${escapeHtml(err.message || String(err))}</div>`;
    } finally {
        loading.style.display = 'none';
    }
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

// ─── Compare mode ────────────────────────────────────────────────────────────

function toggleCompareMode() {
    compareMode = !compareMode;
    selectedForCompare = [];

    const btn    = document.getElementById('tlCompareBtn');
    const banner = document.getElementById('tlCompareBanner');
    const list   = document.getElementById('tlCaseList');

    if (compareMode) {
        btn.classList.remove('btn-outline-secondary');
        btn.classList.add('btn-primary');
        btn.innerHTML = '<i class="ti ti-x me-1"></i>Exit Compare';
        banner.classList.remove('d-none');
        list.classList.add('compare-mode-active');
        renderEmptyDetail();
    } else {
        btn.classList.remove('btn-primary');
        btn.classList.add('btn-outline-secondary');
        btn.innerHTML = '<i class="ti ti-layout-columns me-1"></i>Compare';
        banner.classList.add('d-none');
        list.classList.remove('compare-mode-active');
        renderEmptyDetail();
    }
    updateCompareBanner();
    renderCaseList();
}

function clearCompareSelection() {
    selectedForCompare = [];
    updateCompareBanner();
    renderCaseList();
    renderEmptyDetail();
}

function updateCompareBanner() {
    const n = selectedForCompare.length;
    document.getElementById('tlCompareCount').textContent =
        n === 0 ? '0 selected' : `${n} trace${n !== 1 ? 's' : ''} selected`;
    document.getElementById('tlRunCompareBtn').disabled = n < 2;
}

function toggleCompareSelect(caseId) {
    const idx = selectedForCompare.indexOf(caseId);
    if (idx >= 0) {
        selectedForCompare.splice(idx, 1);
    } else {
        if (selectedForCompare.length >= 4) {
            showToast('Maximum 4 traces can be compared at once.', 'warning');
            return;
        }
        selectedForCompare.push(caseId);
    }
    updateCompareBanner();
    renderCaseList();
}

function renderCompareView() {
    const cases = selectedForCompare
        .map(id => allCases.find(c => c.caseId === id))
        .filter(Boolean);
    if (cases.length < 2) return;

    document.getElementById('tlDetail').innerHTML =
        renderCompareHeaderHtml(cases) +
        renderCompareStatsTableHtml(cases) +
        renderCompareServicePathsHtml(cases) +
        renderCompareStackedSwimlaneHtml(cases);
}

function renderCompareHeaderHtml(cases) {
    return `<div class="d-flex align-items-center justify-content-between px-3 py-2 mb-0"
                 style="border-bottom:1px solid #e9ecef;background:#fff;flex-shrink:0;">
        <span style="font-size:13px;font-weight:600;color:#333;">
            <i class="ti ti-layout-columns me-1"></i>Comparing ${cases.length} traces
        </span>
        <button class="btn btn-sm btn-outline-secondary" onclick="toggleCompareMode()">
            <i class="ti ti-x me-1"></i>Exit Compare
        </button>
    </div>`;
}

function renderCompareStatsTableHtml(cases) {
    const derived = cases.map(c => {
        const times    = c.events.map(e => new Date(e.timestamp).getTime());
        const dur      = caseDurationMs(c);
        const worst    = getWorstLevel(c.events);
        const services = new Set(c.events.map(e => e.activity));
        const startTs  = new Date(Math.min(...times));
        const shortId  = c.caseId.length > 16
            ? c.caseId.slice(0, 7) + '…' + c.caseId.slice(-5)
            : c.caseId;
        return { dur, worst, services, startTs, shortId, worstRank: LEVEL_RANK[worst] || 0 };
    });

    const minDur = Math.min(...derived.map(d => d.dur));
    const maxDur = Math.max(...derived.map(d => d.dur));
    const maxSev = Math.max(...derived.map(d => d.worstRank));
    const minSev = Math.min(...derived.map(d => d.worstRank));

    const headCols = cases.map((c, i) => {
        const d = derived[i];
        return `<th class="text-center" style="min-width:120px;">
            <span class="tl-compare-badge visible me-1" style="display:inline-block;">${i + 1}</span>
            <code style="font-size:11px;">${escapeHtml(d.shortId)}</code><br>
            <a href="#" onclick="drillDownSingle('${escapeHtml(c.caseId)}');return false;"
               style="font-size:10px;color:#888;">view single ↗</a>
        </th>`;
    }).join('');

    const durRow = derived.map(d => {
        const cls = derived.length > 1 && d.dur === maxDur ? 'tl-cmp-highlight-worst'
                  : derived.length > 1 && d.dur === minDur ? 'tl-cmp-highlight-best' : '';
        return `<td class="text-center ${cls}">${formatDuration(d.dur)}</td>`;
    }).join('');

    const evtRow = cases.map(c =>
        `<td class="text-center">${c.events.length}</td>`
    ).join('');

    const svcRow = derived.map(d =>
        `<td class="text-center">${d.services.size}</td>`
    ).join('');

    const sevRow = derived.map(d => {
        const color = getLevelColor(d.worst);
        const cls = derived.length > 1 && d.worstRank === maxSev ? 'tl-cmp-highlight-worst'
                  : derived.length > 1 && d.worstRank === minSev ? 'tl-cmp-highlight-best' : '';
        return `<td class="text-center ${cls}">
            <span class="badge" style="background:${color};color:#fff;font-size:11px;">${escapeHtml(d.worst)}</span>
        </td>`;
    }).join('');

    const startRow = derived.map(d =>
        `<td class="text-center" style="font-size:12px;">${escapeHtml(d.startTs.toLocaleTimeString())}</td>`
    ).join('');

    return `<div class="tl-section">
        <div class="tl-section-title"><i class="ti ti-table me-1"></i>Stats Comparison</div>
        <div class="table-responsive">
            <table class="table table-sm table-vcenter mb-0">
                <thead>
                    <tr>
                        <th style="width:100px;color:#888;font-size:11px;">Metric</th>
                        ${headCols}
                    </tr>
                </thead>
                <tbody>
                    <tr><td class="text-muted" style="font-size:12px;">Duration</td>${durRow}</tr>
                    <tr><td class="text-muted" style="font-size:12px;">Events</td>${evtRow}</tr>
                    <tr><td class="text-muted" style="font-size:12px;">Services</td>${svcRow}</tr>
                    <tr><td class="text-muted" style="font-size:12px;">Severity</td>${sevRow}</tr>
                    <tr><td class="text-muted" style="font-size:12px;">Started</td>${startRow}</tr>
                </tbody>
            </table>
        </div>
    </div>`;
}

function renderCompareServicePathsHtml(cases) {
    const rows = cases.map((c, i) => {
        const seen = new Set();
        const path = [];
        for (const e of c.events) {
            if (!seen.has(e.activity)) {
                seen.add(e.activity);
                const svcEvents = c.events.filter(ev => ev.activity === e.activity);
                path.push({ name: e.activity, worst: getWorstLevel(svcEvents) });
            }
        }
        const chips = path.map((svc, j) => {
            const color = getLevelColor(svc.worst);
            const arrow = j < path.length - 1 ? '<span class="tl-path-arrow">→</span>' : '';
            return `<span class="tl-path-chip" style="border-color:${color};color:${color};">${escapeHtml(svc.name)}</span>${arrow}`;
        }).join('');

        return `<div class="d-flex align-items-start gap-2 mb-2">
            <span class="tl-compare-badge visible flex-shrink-0 mt-1" style="display:inline-block;">${i + 1}</span>
            <div class="tl-path-flow">${chips}</div>
        </div>`;
    }).join('');

    return `<div class="tl-section">
        <div class="tl-section-title"><i class="ti ti-route me-1"></i>Service Paths</div>
        ${rows}
    </div>`;
}

function renderCompareStackedSwimlaneHtml(cases) {
    const blocks = cases.map((c, i) => {
        const times   = c.events.map(e => new Date(e.timestamp).getTime());
        const minTime = Math.min(...times);
        const maxTime = Math.max(...times);
        const span    = maxTime - minTime || 1;
        const services = [...new Set(c.events.map(e => e.activity))];
        const shortId  = c.caseId.length > 14
            ? c.caseId.slice(0, 6) + '…' + c.caseId.slice(-4) : c.caseId;

        const rows = services.map(svc => {
            const dots = c.events
                .filter(e => e.activity === svc)
                .map(e => {
                    const xPct = ((new Date(e.timestamp).getTime() - minTime) / span) * 100;
                    return `<div class="tl-dot"
                        style="left:calc(${xPct.toFixed(2)}% - 7px);background:${getLevelColor(e.level)};"
                        title="${escapeHtml(e.level + ': ' + e.message)}"></div>`;
                }).join('');
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

        return `<div class="tl-cmp-swimlane-group">
            <div class="tl-cmp-swimlane-group-label">
                <span class="tl-compare-badge visible" style="display:inline-block;">${i + 1}</span>
                <code style="font-size:11px;">${escapeHtml(shortId)}</code>
                <span class="text-muted" style="font-weight:400;">${formatDuration(span)}</span>
                <a href="#" class="ms-auto" style="font-size:10px;color:#888;"
                   onclick="drillDownSingle('${escapeHtml(c.caseId)}');return false;">view single ↗</a>
            </div>
            <div class="tl-swimlane">
                ${rows}
                <div class="tl-sw-axis">
                    <span>0</span>
                    <span>${escapeHtml(mid)}</span>
                    <span>${escapeHtml(end)}</span>
                </div>
            </div>
        </div>`;
    }).join('');

    return `<div class="tl-section">
        <div class="tl-section-title"><i class="ti ti-chart-gantt me-1"></i>Swimlane Comparison</div>
        ${blocks}
    </div>`;
}

function drillDownSingle(caseId) {
    compareMode = false;
    selectedForCompare = [];
    const btn = document.getElementById('tlCompareBtn');
    btn.classList.remove('btn-primary');
    btn.classList.add('btn-outline-secondary');
    btn.innerHTML = '<i class="ti ti-layout-columns me-1"></i>Compare';
    document.getElementById('tlCompareBanner').classList.add('d-none');
    document.getElementById('tlCaseList').classList.remove('compare-mode-active');
    updateCompareBanner();
    selectCase(caseId);
    renderCaseList();
}

// ─── Bootstrap ───────────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => initPage());
