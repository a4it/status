// ─── Module state ────────────────────────────────────────────────────────────

let eventLog = [];
let viz = null;

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

// ─── Page initialisation ─────────────────────────────────────────────────────

async function initPage() {
    setDefaultDates();
    await loadScopeEntities();
    viz = new HighPerformanceTimelineVisualization();
    window.viz = viz;
}

function setDefaultDates() {
    const now = new Date();
    const yesterday = new Date(now - 86400000);
    const toLocal = d => {
        const off = d.getTimezoneOffset() * 60000;
        return new Date(d - off).toISOString().slice(0, 16);
    };
    document.getElementById('pmFrom').value = toLocal(yesterday);
    document.getElementById('pmTo').value = toLocal(now);
}

async function loadOrganizations() {
    // No-op: tenant/org selects removed
}

function onScopeTypeChange() {
    const scopeType = document.querySelector('input[name="scopeType"]:checked').value;
    document.getElementById('pmScopeLabel').textContent =
        scopeType === 'platform' ? 'Platform' : 'Application';
    loadScopeEntities();
}

async function loadScopeEntities() {
    const scopeType = document.querySelector('input[name="scopeType"]:checked').value;
    const select = document.getElementById('pmScopeEntity');
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

async function loadProcessMiningData() {
    const scopeType = document.querySelector('input[name="scopeType"]:checked').value;
    const scopeId = document.getElementById('pmScopeEntity').value;
    const from = document.getElementById('pmFrom').value;
    const to = document.getElementById('pmTo').value;
    const maxCases = Math.min(1000, parseInt(document.getElementById('pmMaxCases').value) || 300);
    const minEvents = Math.max(1, parseInt(document.getElementById('pmMinEvents').value) || 2);

    if (!scopeId) {
        showToast('Please select a platform or application first.', 'warning');
        return;
    }

    const loadingOverlay = document.getElementById('loadingOverlay');
    loadingOverlay.style.display = 'flex';

    if (viz) {
        viz.stopAnimation();
        viz.closeDetails();
    }

    try {
        const params = new URLSearchParams({ scope: scopeType, scopeId, maxCases, minEvents });
        if (from) params.append('from', new Date(from).toISOString());
        if (to) params.append('to', new Date(to).toISOString());

        const response = await api.get(`/logs/process-mining?${params}`);
        eventLog = response.cases || [];
        renderStatsBar(response);

        if (viz) {
            viz.reloadData();
        }
    } catch (e) {
        showToast('Failed to load process mining data: ' + (e.message || e), 'danger');
        loadingOverlay.style.display = 'none';
    }
}

function renderStatsBar(response) {
    const bar = document.getElementById('pmStatsBar');
    if (!bar) return;
    const total = response.totalCases || 0;
    bar.innerHTML = `
        <span class="badge bg-blue-lt fs-6">
            <i class="ti ti-git-branch me-1"></i>${total.toLocaleString()} trace${total !== 1 ? 's' : ''}
        </span>
        ${response.truncated
            ? `<span class="badge bg-yellow-lt fs-6">
                   <i class="ti ti-alert-triangle me-1"></i>Results truncated — increase max cases for more
               </span>`
            : ''}
        ${total === 0
            ? `<span class="badge bg-secondary-lt fs-6">No traces found for this time window</span>`
            : ''}
    `;
}

// ─── Visualization ───────────────────────────────────────────────────────────

class HighPerformanceTimelineVisualization {
    constructor() {
        this.canvas = document.getElementById('canvas');
        this.ctx = this.canvas.getContext('2d', { alpha: false, desynchronized: true });

        // Data structures
        this.nodes = new Map();
        this.edges = [];
        this.edgeIndex = new Map();
        this.clusters = new Map();
        this.casesPerNode = new Map();
        this.originalPositions = new Map();

        // Camera and interaction
        this.camera = { x: 0, y: 0, zoom: 1 };
        this.isDragging = false;
        this.dragStart = { x: 0, y: 0 };
        this.hoveredNode = null;
        this.selectedNode = null;
        this.hoveredCase = null;
        this.selectedCase = null;
        this.isDraggingNode = false;
        this.scrollHandler = null;
        this.editMode = false;

        // Animation
        this.animatedCases = [];
        this.animationRunning = false;
        this.animationSpeed = 1;
        this.maxAnimatedCases = 50;

        // Rendering
        this.nodeWidth = 160;
        this.nodeHeight = 40;
        this.visibleBounds = { left: 0, right: 0, top: 0, bottom: 0 };
        this.renderRequested = false;
        this.lastRenderTime = 0;

        // Performance
        this.enableClustering = true;
        this.enableLOD = true;
        this.clusterThreshold = 50;
        this.lodThresholds = { high: 1.0, medium: 0.5, low: 0.25 };

        // FPS tracking
        this.fps = 0;
        this.frameCount = 0;
        this.lastFpsUpdate = performance.now();

        // Bind methods used in inline event handlers
        this.showNodeDetails = this.showNodeDetails.bind(this);
        this.showCaseDetailsById = this.showCaseDetailsById.bind(this);
        this.highlightNode = this.highlightNode.bind(this);
        this.closeDetails = this.closeDetails.bind(this);
        this.filterCases = this.filterCases.bind(this);
        this.renderCasesList = this.renderCasesList.bind(this);
        this.setupVirtualScroll = this.setupVirtualScroll.bind(this);
        this.loadAllCases = this.loadAllCases.bind(this);
        this.toggleEditMode = this.toggleEditMode.bind(this);
        this.resetNodePositions = this.resetNodePositions.bind(this);

        this.init();
    }

    init() {
        this.setupCanvas();
        if (eventLog.length > 0) {
            this.processDataOptimized();
            this.layoutNodesOptimized();
        }
        this.setupEvents();
        this.updateStats();
        this.requestRender();
    }

    // ── Public API: reload with fresh data from eventLog ──────────────────────

    reloadData() {
        this.stopAnimation();
        this.closeDetails();

        this.nodes.clear();
        this.edges = [];
        this.edgeIndex.clear();
        this.casesPerNode.clear();
        this.originalPositions.clear();
        this.animatedCases = [];

        const loadingOverlay = document.getElementById('loadingOverlay');

        if (eventLog.length === 0) {
            this.updateStats();
            this.camera = { x: 0, y: 0, zoom: 1 };
            this.updateVisibleBounds();
            this.requestRender();
            loadingOverlay.style.display = 'none';
            return;
        }

        this.processDataOptimized();

        setTimeout(() => {
            this.layoutNodesOptimized();
            this.updateStats();
            this.camera = { x: 0, y: 0, zoom: 1 };
            this.updateVisibleBounds();
            this.requestRender();
            loadingOverlay.style.display = 'none';
        }, 80);
    }

    // ── Canvas setup ──────────────────────────────────────────────────────────

    setupCanvas() {
        const resize = () => {
            const rect = this.canvas.getBoundingClientRect();
            if (rect.width === 0 || rect.height === 0) return;
            const dpr = window.devicePixelRatio || 1;
            this.canvas.width = rect.width * dpr;
            this.canvas.height = rect.height * dpr;
            this.ctx.scale(dpr, dpr);
            this.canvas.style.width = rect.width + 'px';
            this.canvas.style.height = rect.height + 'px';
            this.centerX = rect.width / 2;
            this.centerY = rect.height / 2;
            this.updateVisibleBounds();
            this.requestRender();
        };
        window.addEventListener('resize', resize);
        resize();
    }

    updateVisibleBounds() {
        const margin = 100;
        this.visibleBounds = {
            left: (-this.centerX - margin - this.camera.x) / this.camera.zoom,
            right: (this.centerX + margin - this.camera.x) / this.camera.zoom,
            top: (-this.centerY - margin - this.camera.y) / this.camera.zoom,
            bottom: (this.centerY + margin - this.camera.y) / this.camera.zoom
        };
    }

    // ── Data processing ───────────────────────────────────────────────────────

    processDataOptimized() {
        this.nodes.clear();
        this.edges = [];
        this.edgeIndex.clear();
        this.casesPerNode.clear();

        this.nodes.set('START', { id: 'START', count: 0, x: 0, y: 0, icon: '🚀', isStart: true, isEnd: false, cluster: null, visible: true, special: true });
        this.nodes.set('END',   { id: 'END',   count: 0, x: 0, y: 0, icon: '🏁', isStart: false, isEnd: true, cluster: null, visible: true, special: true });
        this.casesPerNode.set('START', new Set());
        this.casesPerNode.set('END', new Set());

        const batchSize = 1000;
        let processed = 0;

        const processBatch = () => {
            const end = Math.min(processed + batchSize, eventLog.length);
            for (let i = processed; i < end; i++) {
                const caseData = eventLog[i];
                this.nodes.get('START').count++;
                this.nodes.get('END').count++;
                this.casesPerNode.get('START').add(caseData.caseId);
                this.casesPerNode.get('END').add(caseData.caseId);

                caseData.events.forEach((event, index) => {
                    if (!this.nodes.has(event.activity)) {
                        this.nodes.set(event.activity, { id: event.activity, count: 0, x: 0, y: 0, icon: event.icon || '📋', cluster: null, visible: true, special: false });
                    }
                    this.nodes.get(event.activity).count++;

                    if (!this.casesPerNode.has(event.activity)) this.casesPerNode.set(event.activity, new Set());
                    this.casesPerNode.get(event.activity).add(caseData.caseId);

                    const addEdge = (src, tgt, prevEvent, curEvent) => {
                        const key = `${src}->${tgt}`;
                        let edge = this.edgeIndex.get(key);
                        if (!edge) {
                            edge = { source: src, target: tgt, count: 0, durations: [], cases: new Set() };
                            this.edges.push(edge);
                            this.edgeIndex.set(key, edge);
                        }
                        edge.count++;
                        edge.cases.add(caseData.caseId);
                        if (prevEvent && curEvent && edge.durations.length < 100) {
                            edge.durations.push(new Date(curEvent.timestamp) - new Date(prevEvent.timestamp));
                        }
                    };

                    if (index === 0) addEdge('START', event.activity, null, null);
                    if (index > 0)  addEdge(caseData.events[index - 1].activity, event.activity, caseData.events[index - 1], event);
                    if (index === caseData.events.length - 1) addEdge(event.activity, 'END', null, null);
                });
            }
            processed = end;
            if (processed < eventLog.length) {
                setTimeout(processBatch, 0);
            } else {
                this.calculateEdgeStatistics();
                this.updateStats();
            }
        };
        processBatch();
    }

    calculateEdgeStatistics() {
        this.edges.forEach(edge => {
            if (edge.durations.length > 0) {
                edge.avgDuration = edge.durations.reduce((s, d) => s + d, 0) / edge.durations.length;
                edge.minDuration = Math.min(...edge.durations);
                edge.maxDuration = Math.max(...edge.durations);
                edge.durations = [];
            }
            if (edge.cases) {
                edge.caseCount = edge.cases.size;
                edge.cases = null;
            }
        });
    }

    layoutNodesOptimized() {
        const layers = this.calculateLayers();
        const vSpacing = 100;
        const hSpacing = 250;

        layers.forEach((layer, li) => {
            const startX = -(layer.length * hSpacing) / 2 + hSpacing / 2;
            layer.forEach((node, ni) => {
                node.x = startX + ni * hSpacing;
                node.y = -200 + li * vSpacing;
                this.originalPositions.set(node.id, { x: node.x, y: node.y });
            });
        });

        if (this.enableClustering) this.calculateClusters();
    }

    calculateLayers() {
        const layers = [];
        const visited = new Set();
        const nodeArray = Array.from(this.nodes.values());

        const startNode = this.nodes.get('START');
        if (startNode) { layers.push([startNode]); visited.add('START'); }

        const firstLayerNodes = nodeArray.filter(n => {
            if (n.id === 'START' || n.id === 'END') return false;
            return !this.edges.some(e => e.target === n.id && e.source !== 'START');
        });
        if (firstLayerNodes.length > 0) {
            layers.push(firstLayerNodes);
            firstLayerNodes.forEach(n => visited.add(n.id));
        }

        while (visited.size < this.nodes.size - 1) {
            const currentLayer = [];
            const lastLayer = layers[layers.length - 1] || [];

            this.edges.forEach(edge => {
                if (edge.target !== 'END' && lastLayer.some(n => n.id === edge.source) && !visited.has(edge.target)) {
                    const targetNode = this.nodes.get(edge.target);
                    if (targetNode && !currentLayer.includes(targetNode)) {
                        currentLayer.push(targetNode);
                        visited.add(edge.target);
                    }
                }
            });

            if (currentLayer.length === 0) {
                nodeArray.forEach(n => {
                    if (!visited.has(n.id) && n.id !== 'END') { currentLayer.push(n); visited.add(n.id); }
                });
            }
            if (currentLayer.length > 0) layers.push(currentLayer);
        }

        const endNode = this.nodes.get('END');
        if (endNode) layers.push([endNode]);

        return layers;
    }

    calculateClusters() {
        this.clusters.clear();
        const gridSize = 200;
        this.nodes.forEach(node => {
            const key = `${Math.floor(node.x / gridSize)},${Math.floor(node.y / gridSize)}`;
            if (!this.clusters.has(key)) this.clusters.set(key, { nodes: [], x: 0, y: 0, count: 0 });
            const cluster = this.clusters.get(key);
            cluster.nodes.push(node);
            cluster.count += node.count;
        });
        this.clusters.forEach(cluster => {
            cluster.x = cluster.nodes.reduce((s, n) => s + n.x, 0) / cluster.nodes.length;
            cluster.y = cluster.nodes.reduce((s, n) => s + n.y, 0) / cluster.nodes.length;
        });
    }

    // ── Events ────────────────────────────────────────────────────────────────

    setupEvents() {
        let moveThrottle = null;

        this.canvas.addEventListener('mousedown', (e) => {
            const worldPos = this.screenToWorld(e.offsetX, e.offsetY);

            let clickedCase = null;
            for (const ac of this.animatedCases) {
                if (ac.x !== undefined && ac.y !== undefined) {
                    const d = Math.sqrt((worldPos.x - ac.x) ** 2 + (worldPos.y - ac.y) ** 2);
                    if (d < 10) { clickedCase = ac; break; }
                }
            }
            if (clickedCase && !this.editMode) {
                this.selectedCase = clickedCase;
                this.showCaseDetails(clickedCase);
                return;
            }

            let clickedNode = null;
            for (const node of this.getVisibleNodes()) {
                if (this.isPointInNode(worldPos, node)) { clickedNode = node; break; }
            }
            if (clickedNode) {
                this.selectedNode = clickedNode;
                if ((this.editMode || e.shiftKey) && !clickedNode.special) {
                    this.isDraggingNode = true;
                    this.dragStart = { x: worldPos.x - clickedNode.x, y: worldPos.y - clickedNode.y };
                    this.canvas.style.cursor = 'grabbing';
                } else if (!this.editMode) {
                    this.showNodeDetails(clickedNode);
                }
            } else {
                this.isDragging = true;
                this.dragStart = { x: e.offsetX, y: e.offsetY };
                this.canvas.style.cursor = 'grabbing';
            }
        });

        this.canvas.addEventListener('mousemove', (e) => {
            if (moveThrottle) return;
            moveThrottle = setTimeout(() => {
                moveThrottle = null;
                const worldPos = this.screenToWorld(e.offsetX, e.offsetY);

                if (this.isDraggingNode && this.selectedNode) {
                    this.selectedNode.x = worldPos.x - this.dragStart.x;
                    this.selectedNode.y = worldPos.y - this.dragStart.y;
                    this.requestRender();
                } else if (this.isDragging) {
                    this.camera.x += e.offsetX - this.dragStart.x;
                    this.camera.y += e.offsetY - this.dragStart.y;
                    this.dragStart = { x: e.offsetX, y: e.offsetY };
                    this.updateVisibleBounds();
                    this.requestRender();
                } else {
                    let hoveredCase = null;
                    for (const ac of this.animatedCases) {
                        if (ac.x !== undefined && ac.y !== undefined) {
                            const d = Math.sqrt((worldPos.x - ac.x) ** 2 + (worldPos.y - ac.y) ** 2);
                            if (d < 10) { hoveredCase = ac; break; }
                        }
                    }
                    let hovered = null;
                    for (const node of this.getVisibleNodes()) {
                        if (this.isPointInNode(worldPos, node)) { hovered = node; break; }
                    }
                    if (hoveredCase !== this.hoveredCase || hovered !== this.hoveredNode) {
                        this.hoveredCase = hoveredCase;
                        this.hoveredNode = hovered;
                        if (hoveredCase || hovered) {
                            this.canvas.style.cursor = (hovered && (this.editMode || e.shiftKey) && !hovered.special) ? 'grab' : 'pointer';
                        } else {
                            this.canvas.style.cursor = 'move';
                        }
                        this.requestRender();
                        this.updateTooltip(e, hoveredCase ? null : hovered, hoveredCase);
                    }
                }
            }, 16);
        });

        this.canvas.addEventListener('mouseup', () => {
            if (this.isDraggingNode && this.editMode) this.selectedNode = null;
            this.isDragging = false;
            this.isDraggingNode = false;
            this.canvas.style.cursor = 'move';
        });

        this.canvas.addEventListener('mouseleave', () => {
            this.isDragging = false;
            this.isDraggingNode = false;
            this.hoveredNode = null;
            this.hoveredCase = null;
            this.canvas.style.cursor = 'move';
            document.getElementById('pm-tooltip').style.display = 'none';
        });

        this.canvas.addEventListener('wheel', (e) => {
            e.preventDefault();
            const factor = e.deltaY > 0 ? 0.9 : 1.1;
            const newZoom = this.camera.zoom * factor;
            if (newZoom >= 0.1 && newZoom <= 5) {
                this.camera.zoom = newZoom;
                this.updateVisibleBounds();
                this.requestRender();
            }
        }, { passive: false });

        document.getElementById('playBtn').addEventListener('click', () => this.toggleAnimation());
        document.getElementById('stopBtn').addEventListener('click', () => this.stopAnimation());

        document.getElementById('zoomIn').addEventListener('click', () => {
            this.camera.zoom = Math.min(5, this.camera.zoom * 1.2);
            this.updateVisibleBounds(); this.requestRender();
        });
        document.getElementById('zoomOut').addEventListener('click', () => {
            this.camera.zoom = Math.max(0.1, this.camera.zoom * 0.8);
            this.updateVisibleBounds(); this.requestRender();
        });
        document.getElementById('zoomReset').addEventListener('click', () => {
            this.camera = { x: 0, y: 0, zoom: 1 };
            this.updateVisibleBounds(); this.requestRender();
        });

        document.querySelectorAll('input[name="lineThickness"]').forEach(radio => {
            radio.addEventListener('change', () => this.requestRender());
        });

        document.getElementById('enableClustering').addEventListener('change', (e) => {
            this.enableClustering = e.target.checked;
            if (this.enableClustering) this.calculateClusters();
            this.requestRender();
        });
        document.getElementById('enableLOD').addEventListener('change', (e) => {
            this.enableLOD = e.target.checked;
            this.requestRender();
        });
        document.getElementById('sampleAnimation').addEventListener('change', () => {
            if (this.animationRunning) { this.stopAnimation(); this.toggleAnimation(); }
        });
        document.getElementById('speedSlider').addEventListener('input', (e) => {
            this.animationSpeed = parseFloat(e.target.value);
            document.getElementById('speedValue').textContent = this.animationSpeed + 'x';
        });
        document.getElementById('editBtn').addEventListener('click', () => this.toggleEditMode());

        document.addEventListener('keydown', (e) => {
            if (e.key === 'Shift' && this.hoveredNode && !this.editMode && !this.hoveredNode.special)
                this.canvas.style.cursor = 'grab';
            else if (e.key === 'Escape' && this.editMode)
                this.toggleEditMode();
        });
        document.addEventListener('keyup', (e) => {
            if (e.key === 'Shift' && this.hoveredNode && !this.isDraggingNode && !this.editMode && !this.hoveredNode.special)
                this.canvas.style.cursor = 'pointer';
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    isPointInNode(point, node) {
        return point.x >= node.x - this.nodeWidth / 2 && point.x <= node.x + this.nodeWidth / 2 &&
               point.y >= node.y - this.nodeHeight / 2 && point.y <= node.y + this.nodeHeight / 2;
    }

    updateTooltip(e, node, animCase) {
        const tooltip = document.getElementById('pm-tooltip');
        const canvasRect = this.canvas.getBoundingClientRect();
        const tx = e.clientX - canvasRect.left + 12;
        const ty = e.clientY - canvasRect.top + 12;

        if (animCase) {
            const cur = animCase.events[animCase.currentIndex];
            const nxt = animCase.currentIndex < animCase.events.length - 1 ? animCase.events[animCase.currentIndex + 1] : null;
            tooltip.style.display = 'block';
            tooltip.style.left = tx + 'px';
            tooltip.style.top = ty + 'px';
            tooltip.innerHTML = `<strong>${escapeHtml(animCase.caseId)}</strong><br>
                Current: ${escapeHtml(cur.activity)}<br>
                ${nxt ? `Next: ${escapeHtml(nxt.activity)}<br>` : 'Complete<br>'}
                <small style="opacity:.7">Click for details</small>`;
        } else if (node) {
            const outEdges = this.edges.filter(e2 => e2.source === node.id);
            const avgWait = outEdges.length > 0
                ? outEdges.reduce((s, e2) => s + (e2.avgDuration || 0), 0) / outEdges.length : 0;
            const caseCount = this.casesPerNode.get(node.id)?.size || 0;
            const action = this.editMode ? (node.special ? 'Cannot move start/end' : 'Drag to move')
                                         : (node.special ? 'Click for details' : 'Click for details • Shift+drag to move');
            tooltip.style.display = 'block';
            tooltip.style.left = tx + 'px';
            tooltip.style.top = ty + 'px';
            tooltip.innerHTML = `<strong>${escapeHtml(node.id)}</strong><br>
                Cases: ${caseCount.toLocaleString()}<br>
                ${avgWait > 0 ? `Avg wait to next: ${formatDuration(avgWait)}<br>` : ''}
                <small style="opacity:.7">${action}</small>`;
        } else {
            tooltip.style.display = 'none';
        }
    }

    getVisibleNodes() {
        const visible = [];
        this.nodes.forEach(node => {
            if (node.x >= this.visibleBounds.left && node.x <= this.visibleBounds.right &&
                node.y >= this.visibleBounds.top  && node.y <= this.visibleBounds.bottom)
                visible.push(node);
        });
        return visible;
    }

    getVisibleEdges() {
        return this.edges.filter(edge => {
            const src = this.nodes.get(edge.source);
            const tgt = this.nodes.get(edge.target);
            if (!src || !tgt) return false;
            const minX = Math.min(src.x, tgt.x), maxX = Math.max(src.x, tgt.x);
            const minY = Math.min(src.y, tgt.y), maxY = Math.max(src.y, tgt.y);
            return !(maxX < this.visibleBounds.left || minX > this.visibleBounds.right ||
                     maxY < this.visibleBounds.top  || minY > this.visibleBounds.bottom);
        });
    }

    screenToWorld(sx, sy) {
        return { x: (sx - this.centerX - this.camera.x) / this.camera.zoom, y: (sy - this.centerY - this.camera.y) / this.camera.zoom };
    }

    worldToScreen(wx, wy) {
        return { x: wx * this.camera.zoom + this.centerX + this.camera.x, y: wy * this.camera.zoom + this.centerY + this.camera.y };
    }

    requestRender() {
        if (!this.renderRequested) {
            this.renderRequested = true;
            requestAnimationFrame(() => this.render());
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    render() {
        const t0 = performance.now();
        this.renderRequested = false;
        const ctx = this.ctx;
        const width  = this.canvas.width  / (window.devicePixelRatio || 1);
        const height = this.canvas.height / (window.devicePixelRatio || 1);

        ctx.fillStyle = '#e8e8e8';
        ctx.fillRect(0, 0, width, height);

        if (eventLog.length === 0) {
            ctx.fillStyle = '#aaa';
            ctx.font = '14px sans-serif';
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.fillText('Select a scope and click Load to visualise process flows', width / 2, height / 2);
            return;
        }

        const lod = this.enableLOD ? this.getCurrentLOD() : 'high';

        if (lod !== 'low' && this.edges.length > 0) {
            const maxC = Math.max(...this.edges.map(e => e.caseCount || e.count)) || 1;
            const sorted = this.getVisibleEdges().slice().sort((a, b) =>
                ((a.caseCount || a.count) / maxC) - ((b.caseCount || b.count) / maxC));
            sorted.forEach(edge => this.drawEdge(edge, lod));
        }

        if (this.enableClustering && this.camera.zoom < 0.5) {
            this.drawClusters();
        } else {
            this.getVisibleNodes().forEach(node => this.drawNode(node, lod));
        }

        if (this.animatedCases.length > 0) this.drawAnimatedCases();

        this.updatePerformanceMetrics(t0);
        if (this.animationRunning) this.requestRender();
    }

    getCurrentLOD() {
        if (this.camera.zoom >= this.lodThresholds.high)   return 'high';
        if (this.camera.zoom >= this.lodThresholds.medium) return 'medium';
        return 'low';
    }

    drawEdge(edge, lod) {
        const ctx = this.ctx;
        const src = this.nodes.get(edge.source);
        const tgt = this.nodes.get(edge.target);
        if (!src || !tgt) return;

        // ── Compute style ────────────────────────────────────────────────────
        const thicknessMode = (document.querySelector('input[name="lineThickness"]:checked') || {}).value || 'timelines';
        let ratio;
        if (thicknessMode === 'none') {
            ratio = 0;
        } else if (thicknessMode === 'duration') {
            const maxDur = Math.max(...this.edges.map(e => e.avgDuration || 0)) || 1;
            ratio = (edge.avgDuration || 0) / maxDur;
        } else {
            const maxCases = Math.max(...this.edges.map(e => e.caseCount || e.count)) || 1;
            ratio = (edge.caseCount || edge.count) / maxCases;
        }
        const isConnected = this.selectedNode &&
            (edge.source === this.selectedNode.id || edge.target === this.selectedNode.id);

        let color;
        if (isConnected) {
            color = '#9c27b0';
            ctx.globalAlpha = 1;
        } else {
            // Teal palette: low-freq = muted blue-gray, high-freq = vivid teal
            const r = Math.round(148 - ratio * 134); // 148 → 14
            const g = Math.round(163 + ratio * 2);   // 163 → 165
            const b = Math.round(184 + ratio * 49);  // 184 → 233
            color = `rgb(${r},${g},${b})`;
            ctx.globalAlpha = 0.35 + ratio * 0.65;
        }
        ctx.strokeStyle = color;
        const minT = isConnected ? 2 : 1;
        const maxT = lod === 'high' ? 8 : 5;
        const thickness = thicknessMode === 'none' ? minT : minT + (maxT - minT) * ratio;
        ctx.lineWidth = thickness * this.camera.zoom;
        ctx.lineCap = 'round';

        // ── Self-loop: bezier arc above the node ─────────────────────────────
        if (src === tgt) {
            const pos = this.worldToScreen(src.x, src.y);
            const hw = (this.nodeWidth / 2) * this.camera.zoom;
            const hh = (this.nodeHeight / 2) * this.camera.zoom;
            const lift = 32 * this.camera.zoom;
            const sx = pos.x - hw * 0.35;
            const sy = pos.y - hh;
            const ex = pos.x + hw * 0.35;
            const ey = pos.y - hh;
            ctx.beginPath();
            ctx.moveTo(sx, sy);
            ctx.bezierCurveTo(sx, sy - lift, ex, ey - lift, ex, ey);
            ctx.stroke();
            ctx.globalAlpha = 1;
            if (lod === 'high') {
                // Filled arrowhead pointing downward at (ex, ey)
                const as = (4 + thickness * 0.9) * this.camera.zoom;
                const a = Math.PI / 2;
                ctx.fillStyle = color;
                ctx.beginPath();
                ctx.moveTo(ex, ey);
                ctx.lineTo(ex - as * Math.cos(a - Math.PI / 6), ey - as * Math.sin(a - Math.PI / 6));
                ctx.lineTo(ex - as * Math.cos(a + Math.PI / 6), ey - as * Math.sin(a + Math.PI / 6));
                ctx.closePath();
                ctx.fill();
            }
            return;
        }

        // ── Regular edge ─────────────────────────────────────────────────────
        const bez = this.computeEdgeBezier(src, tgt);
        const start = this.worldToScreen(bez.startX, bez.startY);
        const end   = this.worldToScreen(bez.endX, bez.endY);

        if (lod === 'medium') {
            const dist = Math.sqrt((end.x - start.x) ** 2 + (end.y - start.y) ** 2);
            if (dist < 10) return;
        }

        const cp1 = this.worldToScreen(bez.cp1x, bez.cp1y);
        const cp2 = this.worldToScreen(bez.cp2x, bez.cp2y);

        ctx.beginPath();
        ctx.moveTo(start.x, start.y);
        ctx.bezierCurveTo(cp1.x, cp1.y, cp2.x, cp2.y, end.x, end.y);
        ctx.stroke();

        if (lod === 'high') {
            // Arrowhead — same alpha as the stroke so it blends consistently
            const arrowAngle = Math.atan2(end.y - cp2.y, end.x - cp2.x);
            const as = (4 + thickness * 0.9) * this.camera.zoom;
            ctx.fillStyle = color;
            ctx.beginPath();
            ctx.moveTo(end.x, end.y);
            ctx.lineTo(end.x - as * Math.cos(arrowAngle - Math.PI / 6), end.y - as * Math.sin(arrowAngle - Math.PI / 6));
            ctx.lineTo(end.x - as * Math.cos(arrowAngle + Math.PI / 6), end.y - as * Math.sin(arrowAngle + Math.PI / 6));
            ctx.closePath();
            ctx.fill();
        }

        ctx.globalAlpha = 1;
    }

    drawNode(node, lod) {
        const ctx = this.ctx;
        ctx.globalAlpha = 1;
        const pos = this.worldToScreen(node.x, node.y);
        const w = this.nodeWidth * this.camera.zoom;
        const h = this.nodeHeight * this.camera.zoom;
        if (w < 5 || h < 5) return;

        if (lod === 'low') {
            ctx.fillStyle = node.special ? '#4a5568' : '#1f2937';
            ctx.fillRect(pos.x - w / 2, pos.y - h / 2, w, h);
            return;
        }

        let avgWaitTime = 0, waitCount = 0;
        if (!node.special) {
            this.edges.filter(e => e.source === node.id).forEach(e => {
                if (e.avgDuration) { avgWaitTime += e.avgDuration; waitCount++; }
            });
        }
        const hasLongWait = waitCount > 0 && (avgWaitTime / waitCount) > 3600000;

        if (lod === 'high') {
            if (this.editMode && !node.special) {
                ctx.shadowColor = 'rgba(33, 150, 243, 0.4)'; ctx.shadowBlur = 12 * this.camera.zoom;
            } else if (node.special) {
                ctx.shadowColor = 'rgba(99, 102, 241, 0.3)'; ctx.shadowBlur = 10 * this.camera.zoom;
            } else {
                ctx.shadowColor = hasLongWait ? 'rgba(255, 87, 34, 0.3)' : 'rgba(0, 0, 0, 0.2)';
                ctx.shadowBlur = hasLongWait ? 8 * this.camera.zoom : 4 * this.camera.zoom;
            }
            ctx.shadowOffsetX = 2 * this.camera.zoom;
            ctx.shadowOffsetY = 2 * this.camera.zoom;
        }

        const isConnectedToSelected = this.selectedNode && this.selectedNode !== node &&
            this.edges.some(e => (e.source === this.selectedNode.id && e.target === node.id) ||
                                 (e.target === this.selectedNode.id && e.source === node.id));

        if (node.special) {
            const grad = ctx.createLinearGradient(pos.x - w / 2, pos.y - h / 2, pos.x + w / 2, pos.y + h / 2);
            if (node.id === 'START') { grad.addColorStop(0, '#10b981'); grad.addColorStop(1, '#059669'); ctx.strokeStyle = '#047857'; }
            else                     { grad.addColorStop(0, '#ef4444'); grad.addColorStop(1, '#dc2626'); ctx.strokeStyle = '#b91c1c'; }
            ctx.fillStyle = grad;
            ctx.lineWidth = 2;
        } else if (this.selectedNode === node) {
            ctx.fillStyle = '#e3f2fd'; ctx.strokeStyle = '#2196f3'; ctx.lineWidth = 3;
        } else if (isConnectedToSelected) {
            ctx.fillStyle = '#f3e5f5'; ctx.strokeStyle = '#9c27b0'; ctx.lineWidth = 2;
        } else if (this.hoveredNode === node) {
            if (this.editMode && !node.special) { ctx.fillStyle = '#e3f2fd'; ctx.strokeStyle = '#2196f3'; ctx.lineWidth = 2; }
            else { ctx.fillStyle = '#f0f0f0'; ctx.strokeStyle = '#999'; ctx.lineWidth = 1; }
        } else if (this.editMode && !node.special) {
            ctx.fillStyle = '#f8f9fa'; ctx.strokeStyle = '#2196f3'; ctx.lineWidth = 1;
            ctx.setLineDash([5, 5]);
        } else {
            ctx.fillStyle = '#ffffff'; ctx.strokeStyle = hasLongWait ? '#ff5722' : '#d0d0d0';
            ctx.lineWidth = hasLongWait ? 2 : 1;
        }

        if (lod === 'high') {
            ctx.beginPath();
            ctx.roundRect(pos.x - w / 2, pos.y - h / 2, w, h, (node.special ? 10 : 6) * this.camera.zoom);
            ctx.fill(); ctx.stroke(); ctx.setLineDash([]);
        } else {
            ctx.fillRect(pos.x - w / 2, pos.y - h / 2, w, h);
            ctx.strokeRect(pos.x - w / 2, pos.y - h / 2, w, h);
            ctx.setLineDash([]);
        }

        ctx.shadowColor = 'transparent'; ctx.shadowBlur = 0; ctx.shadowOffsetX = 0; ctx.shadowOffsetY = 0;

        if (lod !== 'low' && w > 40) {
            if (lod === 'high') {
                if (!node.special) { ctx.fillStyle = '#f5f5f5'; ctx.fillRect(pos.x - w / 2, pos.y - h / 2, h, h); }
                ctx.font = `${(node.special ? 20 : 16) * this.camera.zoom}px sans-serif`;
                ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
                ctx.fillStyle = node.special ? '#ffffff' : '#666';
                ctx.fillText(node.icon, node.special ? pos.x : pos.x - w / 2 + h / 2, pos.y);
            }
            ctx.font = `${(node.special ? 14 : 12) * this.camera.zoom}px sans-serif`;
            ctx.textAlign = node.special ? 'center' : 'left';
            ctx.fillStyle = node.special ? '#ffffff' : '#333';

            if (node.special) {
                ctx.fillText(node.id, pos.x, pos.y - 8 * this.camera.zoom);
                ctx.font = `${10 * this.camera.zoom}px sans-serif`;
                ctx.fillText(node.count.toLocaleString() + ' cases', pos.x, pos.y + 8 * this.camera.zoom);
            } else {
                const textX = pos.x - w / 2 + (lod === 'high' ? h + 8 : 8) * this.camera.zoom;
                const maxTW = w - (lod === 'high' ? h : 0) - 16 * this.camera.zoom;
                let text = node.id;
                while (ctx.measureText(text + '…').width > maxTW && text.length > 0) text = text.slice(0, -1);
                if (text !== node.id) text += '…';
                ctx.fillText(text, textX, pos.y - 8 * this.camera.zoom);
                if (w > 80) {
                    ctx.font = `${10 * this.camera.zoom}px sans-serif`;
                    ctx.fillStyle = '#999';
                    ctx.fillText(node.count.toLocaleString(), textX, pos.y + 8 * this.camera.zoom);
                }
                if (hasLongWait && lod === 'high') {
                    ctx.font = `${12 * this.camera.zoom}px sans-serif`;
                    ctx.fillStyle = '#ff5722';
                    ctx.fillText('⏱️', pos.x + w / 2 - 15 * this.camera.zoom, pos.y - h / 2 - 5 * this.camera.zoom);
                }
            }
        }
    }

    drawClusters() {
        const ctx = this.ctx;
        this.clusters.forEach(cluster => {
            if (cluster.nodes.length === 1) {
                this.drawNode(cluster.nodes[0], 'medium');
            } else {
                const pos = this.worldToScreen(cluster.x, cluster.y);
                const radius = Math.min(50, 20 + Math.sqrt(cluster.nodes.length) * 5) * this.camera.zoom;
                ctx.beginPath();
                ctx.arc(pos.x, pos.y, radius, 0, Math.PI * 2);
                const maxAvg = Math.max(...Array.from(this.clusters.values()).map(c => c.count / c.nodes.length));
                const ratio = (cluster.count / cluster.nodes.length) / maxAvg;
                ctx.fillStyle = ratio > 0.7 ? '#2e7d32' : ratio > 0.4 ? '#f57c00' : '#757575';
                ctx.globalAlpha = 0.6; ctx.fill(); ctx.globalAlpha = 1;
                ctx.fillStyle = '#ffffff';
                ctx.font = `bold ${14 * this.camera.zoom}px sans-serif`;
                ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
                ctx.fillText(cluster.nodes.length.toString(), pos.x, pos.y);
                ctx.font = `${10 * this.camera.zoom}px sans-serif`;
                ctx.fillText(cluster.count.toLocaleString(), pos.x, pos.y + radius + 10);
            }
        });
    }

    drawAnimatedCases() {
        const ctx = this.ctx;
        this.animatedCases.forEach(ac => {
            if (ac.x === undefined || ac.y === undefined) return;
            if (ac.x < this.visibleBounds.left || ac.x > this.visibleBounds.right ||
                ac.y < this.visibleBounds.top  || ac.y > this.visibleBounds.bottom) return;

            const pos = this.worldToScreen(ac.x, ac.y);
            const isHovered = ac === this.hoveredCase;
            const isSelected = ac === this.selectedCase;
            const size = (isHovered || isSelected) ? 9 : 6;

            if (isSelected) {
                ctx.beginPath();
                ctx.arc(pos.x, pos.y, size * 2.5 * this.camera.zoom, 0, Math.PI * 2);
                ctx.fillStyle = '#2196f3'; ctx.globalAlpha = 0.3; ctx.fill();
            }
            ctx.beginPath();
            ctx.arc(pos.x, pos.y, size * this.camera.zoom, 0, Math.PI * 2);
            ctx.fillStyle = isSelected ? '#2196f3' : ac.color;
            ctx.globalAlpha = isHovered ? 1 : 0.8; ctx.fill();
            ctx.beginPath();
            ctx.arc(pos.x, pos.y, 2 * this.camera.zoom, 0, Math.PI * 2);
            ctx.fillStyle = '#ffffff'; ctx.globalAlpha = 1; ctx.fill();

            if (isSelected && this.camera.zoom > 0.5) {
                ctx.fillStyle = '#000'; ctx.font = `bold ${10 * this.camera.zoom}px sans-serif`;
                ctx.textAlign = 'center'; ctx.textBaseline = 'middle'; ctx.globalAlpha = 0.8;
                ctx.fillText(ac.caseId.slice(-6), pos.x, pos.y - 20 * this.camera.zoom);
            }
        });
        ctx.globalAlpha = 1;
    }

    // ── Edge geometry ─────────────────────────────────────────────────────────

    getEdgeEndpoints(src, tgt) {
        const dx = tgt.x - src.x, dy = tgt.y - src.y;
        const angle = Math.atan2(dy, dx);
        const s = this.getRectangleEdgePoint(src, angle);
        const t = this.getRectangleEdgePoint(tgt, angle + Math.PI);
        return { startX: s.x, startY: s.y, endX: t.x, endY: t.y, angle };
    }

    computeEdgeBezier(src, tgt) {
        const hw = this.nodeWidth / 2;
        const hh = this.nodeHeight / 2;
        const dx = tgt.x - src.x;
        const dy = tgt.y - src.y;

        // ── Self-loop: arc above the node (matches drawEdge self-loop rendering) ──
        if (src === tgt) {
            const lift = 32;
            const startX = src.x - hw * 0.35;
            const startY = src.y - hh;
            const endX   = src.x + hw * 0.35;
            const endY   = src.y - hh;
            return {
                startX, startY, endX, endY,
                cp1x: startX, cp1y: startY - lift,
                cp2x: endX,   cp2y: endY   - lift
            };
        }

        if (dy > 20) {
            // ── Forward (downward) edge ──────────────────────────────────────
            // Always exit from bottom-center, enter top-center.
            // Control points sit directly below/above so the curve flows
            // straight down then gently arcs toward the target x — clean & organic.
            const startX = src.x;
            const startY = src.y + hh;
            const endX   = tgt.x;
            const endY   = tgt.y - hh;
            const pull   = Math.max(40, Math.abs(dy) * 0.40);
            return {
                startX, startY, endX, endY,
                cp1x: startX, cp1y: startY + pull,
                cp2x: endX,   cp2y: endY   - pull
            };
        } else {
            // ── Backward / lateral edge: sweep right ─────────────────────────
            // Exit from the right side of src, arc outward, enter right side of tgt.
            // Width grows with the vertical distance so longer jumps arc wider,
            // naturally separating arcs without any random jitter.
            const arcW = 200 + Math.abs(dy) * 0.55;
            const startX = src.x + hw;
            const startY = src.y;
            const endX   = tgt.x + hw;
            const endY   = tgt.y;
            return {
                startX, startY, endX, endY,
                cp1x: startX + arcW, cp1y: startY,
                cp2x: endX   + arcW, cp2y: endY
            };
        }
    }

    getRectangleEdgePoint(node, angle) {
        const hw = this.nodeWidth / 2, hh = this.nodeHeight / 2;
        const cos = Math.cos(angle), sin = Math.sin(angle);
        let x, y;
        if (Math.abs(cos) * hh > Math.abs(sin) * hw) {
            x = cos > 0 ? hw : -hw; y = x * sin / cos;
        } else {
            y = sin > 0 ? hh : -hh; x = y * cos / sin;
        }
        return { x: node.x + x, y: node.y + y };
    }

    // ── Animation ─────────────────────────────────────────────────────────────

    toggleAnimation() {
        this.animationRunning = !this.animationRunning;
        document.getElementById('playBtn').innerHTML = this.animationRunning ? '⏸ Pause' : '&#9654; Play';
        if (this.animationRunning) {
            if (eventLog.length === 0) { this.animationRunning = false; document.getElementById('playBtn').innerHTML = '&#9654; Play'; return; }
            this.initAnimationOptimized();
            this.requestRender();
            this.animate();
        }
    }

    stopAnimation() {
        this.animationRunning = false;
        this.animatedCases = [];
        document.getElementById('playBtn').innerHTML = '&#9654; Play';
        this.requestRender();
    }

    initAnimationOptimized() {
        const sample = document.getElementById('sampleAnimation').checked;
        const maxCases = sample ? this.maxAnimatedCases : eventLog.length;
        const step = Math.max(1, Math.floor(eventLog.length / maxCases));
        const sampled = [];
        for (let i = 0; i < eventLog.length && sampled.length < maxCases; i += step) {
            if (eventLog[i].events && eventLog[i].events.length >= 1) sampled.push(eventLog[i]);
        }

        this.animatedCases = [];
        const waves = 3;
        const perWave = Math.ceil(sampled.length / waves);

        sampled.forEach((caseData, i) => {
            const durations = [1000];
            for (let j = 0; j < caseData.events.length - 1; j++) {
                durations.push(new Date(caseData.events[j + 1].timestamp) - new Date(caseData.events[j].timestamp));
            }
            durations.push(1000);

            const maxD = Math.max(...durations);
            const normD = durations.map(d => Math.max(500, Math.min(4000, (d / maxD) * 3000)));

            const augmented = [
                { activity: 'START', timestamp: caseData.events[0].timestamp },
                ...caseData.events,
                { activity: 'END', timestamp: caseData.events[caseData.events.length - 1].timestamp }
            ];

            this.animatedCases.push({
                ...caseData, events: augmented, currentIndex: 0, progress: 0,
                x: undefined, y: undefined,
                startDelay: Math.floor(i / perWave) * 1000 + (i % perWave) * 100,
                elapsed: 0, segmentElapsed: 0, segmentDurations: normD,
                color: `hsl(${(i * 37) % 360}, 65%, 50%)`
            });
        });
    }

    animate() {
        if (!this.animationRunning) return;
        const dt = 16;

        this.animatedCases.forEach(ac => {
            ac.elapsed += dt;
            if (ac.elapsed > ac.startDelay && ac.x === undefined) {
                const fn = this.nodes.get(ac.events[0].activity);
                if (fn) { ac.x = fn.x; ac.y = fn.y; }
            }
            if (ac.elapsed > ac.startDelay && ac.currentIndex < ac.events.length - 1) {
                ac.segmentElapsed += dt * this.animationSpeed;
                ac.progress = Math.min(1, ac.segmentElapsed / ac.segmentDurations[ac.currentIndex]);
                if (ac.progress >= 1) {
                    ac.progress = 0; ac.segmentElapsed = 0; ac.currentIndex++;
                    if (ac.currentIndex >= ac.events.length - 1) {
                        const animType = document.querySelector('input[name="animType"]:checked')?.value || 'continuous';
                        if (animType === 'continuous') { ac.currentIndex = 0; ac.progress = 0; ac.segmentElapsed = 0; }
                        return;
                    }
                }
                if (ac.currentIndex < ac.events.length - 1) {
                    const cn = this.nodes.get(ac.events[ac.currentIndex].activity);
                    const nn = this.nodes.get(ac.events[ac.currentIndex + 1].activity);
                    if (cn && nn) {
                        const bez = this.computeEdgeBezier(cn, nn);
                        const t = this.easeInOutQuad(ac.progress);
                        const mt = 1 - t;
                        ac.x = mt*mt*mt*bez.startX + 3*mt*mt*t*bez.cp1x + 3*mt*t*t*bez.cp2x + t*t*t*bez.endX;
                        ac.y = mt*mt*mt*bez.startY + 3*mt*mt*t*bez.cp1y + 3*mt*t*t*bez.cp2y + t*t*t*bez.endY;
                    }
                }
            }
        });

        this.requestRender();
        requestAnimationFrame(() => this.animate());
    }

    easeInOutQuad(t) { return t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t; }

    // ── Detail panels ─────────────────────────────────────────────────────────

    showNodeDetails(node) {
        const cases = this.casesPerNode.get(node.id) || new Set();
        const casesList = Array.from(cases);
        const inEdges  = this.edges.filter(e => e.target === node.id);
        const outEdges = this.edges.filter(e => e.source === node.id);
        let avgIn = 0, avgOut = 0;
        inEdges.forEach(e => { if (e.avgDuration) avgIn += e.avgDuration; });
        if (inEdges.length) avgIn /= inEdges.length;
        outEdges.forEach(e => { if (e.avgDuration) avgOut += e.avgDuration; });
        if (outEdges.length) avgOut /= outEdges.length;

        document.getElementById('detailsContent').innerHTML = `
            <div class="details-header">
                <h2>${node.icon} ${escapeHtml(node.id)}</h2>
                <button class="close-btn" onclick="viz.closeDetails()">×</button>
            </div>
            <div class="detail-section">
                <h3>Overview</h3>
                <div class="stats-grid">
                    <div class="stat-card"><div class="stat-card-value">${cases.size.toLocaleString()}</div><div class="stat-card-label">Total Cases</div></div>
                    <div class="stat-card"><div class="stat-card-value">${eventLog.length > 0 ? ((cases.size / eventLog.length) * 100).toFixed(1) : 0}%</div><div class="stat-card-label">Of All Cases</div></div>
                    <div class="stat-card"><div class="stat-card-value">${inEdges.length}</div><div class="stat-card-label">Incoming Paths</div></div>
                    <div class="stat-card"><div class="stat-card-value">${outEdges.length}</div><div class="stat-card-label">Outgoing Paths</div></div>
                </div>
            </div>
            ${!node.special ? `
            <div class="detail-section">
                <h3>Timing</h3>
                <div class="detail-row"><span class="detail-label">Avg time from previous:</span><span class="detail-value">${avgIn > 0 ? formatDuration(avgIn) : 'N/A'}</span></div>
                <div class="detail-row"><span class="detail-label">Avg time to next:</span><span class="detail-value">${avgOut > 0 ? formatDuration(avgOut) : 'N/A'}</span></div>
            </div>` : ''}
            <div class="detail-section">
                <h3>Connections</h3>
                <div style="margin-bottom:8px;"><strong style="font-size:12px;">From:</strong>
                    ${inEdges.map(e => `<div style="margin-left:10px;font-size:12px;color:#555;">${escapeHtml(e.source)} <span class="badge bg-secondary-lt">${e.count}</span></div>`).join('') || '<div style="margin-left:10px;font-size:12px;color:#aaa;">None</div>'}
                </div>
                <div><strong style="font-size:12px;">To:</strong>
                    ${outEdges.map(e => `<div style="margin-left:10px;font-size:12px;color:#555;">${escapeHtml(e.target)} <span class="badge bg-secondary-lt">${e.count}</span></div>`).join('') || '<div style="margin-left:10px;font-size:12px;color:#aaa;">None</div>'}
                </div>
            </div>
            <div class="detail-section">
                <h3>Cases (${cases.size.toLocaleString()} total)</h3>
                <input type="text" class="search-input" placeholder="Search trace IDs…"
                       onkeyup="viz.filterCases(this.value, '${escapeHtml(node.id)}')">
                ${cases.size > 50 ? `<div style="font-size:12px;color:#888;margin-bottom:6px;">
                    Showing first 50. <a href="#" onclick="event.preventDefault();viz.loadAllCases('${escapeHtml(node.id)}');">Load all</a>
                </div>` : ''}
                <div class="claims-list" id="casesList">
                    <div id="casesListContent">${this.renderCasesList(casesList, node.id)}</div>
                </div>
            </div>`;

        document.getElementById('rightSidebar').classList.add('active');
        this.setupVirtualScroll(casesList, node.id);
        setTimeout(() => { this.updateVisibleBounds(); this.requestRender(); }, 10);
    }

    renderCasesList(list, nodeId, limit = 50) {
        const items = list.slice(0, Math.min(limit, list.length));
        if (items.length === 0) return '<div style="text-align:center;color:#aaa;padding:16px;">No cases</div>';
        return items.map(id => `
            <div class="claim-item" onclick="viz.showCaseDetailsById('${escapeHtml(id)}', '${escapeHtml(nodeId)}')">
                <div style="font-weight:500;font-size:12px;">${escapeHtml(id)}</div>
                <div style="font-size:11px;color:#888;">Click for details</div>
            </div>`).join('');
    }

    setupVirtualScroll(allCases, nodeId) {
        const container = document.getElementById('casesList');
        if (!container || allCases.length <= 50) return;
        if (this.scrollHandler) container.removeEventListener('scroll', this.scrollHandler);

        let rendered = 50, isLoading = false;
        this.scrollHandler = () => {
            if (isLoading) return;
            if ((container.scrollTop + container.clientHeight) / container.scrollHeight > 0.8 && rendered < allCases.length) {
                isLoading = true;
                const content = document.getElementById('casesListContent');
                const loadDiv = document.createElement('div');
                loadDiv.id = 'loadingIndicator';
                loadDiv.style.cssText = 'text-align:center;padding:8px;color:#888;font-size:12px;';
                loadDiv.textContent = 'Loading…';
                content.appendChild(loadDiv);
                setTimeout(() => {
                    document.getElementById('loadingIndicator')?.remove();
                    const newCases = allCases.slice(rendered, rendered + 50);
                    newCases.forEach(id => {
                        const div = document.createElement('div');
                        div.className = 'claim-item';
                        div.onclick = () => this.showCaseDetailsById(id, nodeId);
                        div.innerHTML = `<div style="font-weight:500;font-size:12px;">${escapeHtml(id)}</div><div style="font-size:11px;color:#888;">Click for details</div>`;
                        content.appendChild(div);
                    });
                    rendered += newCases.length;
                    isLoading = false;
                    if (rendered >= allCases.length) {
                        const end = document.createElement('div');
                        end.style.cssText = 'text-align:center;padding:8px;color:#aaa;font-size:11px;';
                        end.textContent = `All ${allCases.length.toLocaleString()} cases loaded`;
                        content.appendChild(end);
                    }
                }, 80);
            }
        };
        container.addEventListener('scroll', this.scrollHandler);
    }

    showCaseDetails(animCase) {
        const caseData = eventLog.find(c => c.caseId === animCase.caseId);
        if (caseData) this.showCaseDetailsById(caseData.caseId);
    }

    showCaseDetailsById(caseId, fromNode = null) {
        const caseData = eventLog.find(c => c.caseId === caseId);
        if (!caseData) return;

        const t0 = new Date(caseData.events[0].timestamp);
        const tN = new Date(caseData.events[caseData.events.length - 1].timestamp);
        const animCase = this.animatedCases.find(ac => ac.caseId === caseId);
        const curIdx = animCase ? animCase.currentIndex : -1;

        const backBtn = fromNode ? `
            <button class="btn btn-sm btn-outline-secondary mb-3" onclick="viz.showNodeDetails(viz.nodes.get('${escapeHtml(fromNode)}'))">
                ← Back to ${escapeHtml(fromNode)}
            </button>` : '';

        document.getElementById('detailsContent').innerHTML = `
            <div class="details-header">
                <h2>📋 ${escapeHtml(caseId)}</h2>
                <button class="close-btn" onclick="viz.closeDetails()">×</button>
            </div>
            ${backBtn}
            <div class="detail-section">
                <h3>Case Overview</h3>
                <div class="detail-row"><span class="detail-label">Start:</span><span class="detail-value">${t0.toLocaleString()}</span></div>
                <div class="detail-row"><span class="detail-label">End:</span><span class="detail-value">${tN.toLocaleString()}</span></div>
                <div class="detail-row"><span class="detail-label">Duration:</span><span class="detail-value">${formatDuration(tN - t0)}</span></div>
                <div class="detail-row"><span class="detail-label">Events:</span><span class="detail-value">${caseData.events.length}</span></div>
            </div>
            <div class="detail-section">
                <h3>Event Timeline</h3>
                <div class="event-timeline">
                    ${caseData.events.map((ev, idx) => {
                        const dur = idx > 0 ? new Date(ev.timestamp) - new Date(caseData.events[idx - 1].timestamp) : 0;
                        return `<div class="timeline-event ${idx === curIdx ? 'current' : ''}" onclick="viz.highlightNode('${escapeHtml(ev.activity)}')">
                            <div style="flex:1;">
                                <div style="font-weight:500;">${ev.icon || ''} ${escapeHtml(ev.activity)}</div>
                                ${dur > 0 ? `<div style="font-size:11px;color:#888;">+${formatDuration(dur)}</div>` : ''}
                                ${ev.level ? `<div style="font-size:11px;color:#888;">${escapeHtml(ev.level)}</div>` : ''}
                            </div>
                            <div class="timeline-event-time">${new Date(ev.timestamp).toLocaleTimeString()}</div>
                        </div>`;
                    }).join('')}
                </div>
            </div>`;

        document.getElementById('rightSidebar').classList.add('active');
        this.selectedCase = animCase || null;
        this.requestRender();
    }

    highlightNode(nodeId) {
        const node = this.nodes.get(nodeId);
        if (node) {
            this.selectedNode = node;
            const sc = this.worldToScreen(node.x, node.y);
            this.camera.x += this.centerX - sc.x;
            this.camera.y += this.centerY - sc.y;
            this.updateVisibleBounds();
            this.requestRender();
        }
    }

    closeDetails() {
        document.getElementById('rightSidebar').classList.remove('active');
        this.selectedNode = null;
        this.selectedCase = null;
        const container = document.getElementById('casesList');
        if (container && this.scrollHandler) {
            container.removeEventListener('scroll', this.scrollHandler);
            this.scrollHandler = null;
        }
        this.requestRender();
    }

    loadAllCases(nodeId) {
        const cases = Array.from(this.casesPerNode.get(nodeId) || []);
        const content = document.getElementById('casesListContent');
        const container = document.getElementById('casesList');
        content.innerHTML = '<div style="text-align:center;padding:16px;color:#888;">Loading…</div>';
        setTimeout(() => {
            content.innerHTML = this.renderCasesList(cases, nodeId, cases.length);
            if (container) container.scrollTop = 0;
            if (this.scrollHandler) { container.removeEventListener('scroll', this.scrollHandler); this.scrollHandler = null; }
        }, 80);
    }

    filterCases(term, nodeId) {
        const cases = Array.from(this.casesPerNode.get(nodeId) || []);
        const filtered = term ? cases.filter(id => id.toLowerCase().includes(term.toLowerCase())) : cases;
        const content = document.getElementById('casesListContent');
        const container = document.getElementById('casesList');
        if (filtered.length === 0) {
            content.innerHTML = '<div style="text-align:center;color:#aaa;padding:16px;">No matching traces</div>';
        } else if (filtered.length > 1000) {
            content.innerHTML = `<div style="font-size:12px;color:#888;padding:8px;background:#f5f5f5;border-radius:4px;margin-bottom:8px;">Showing first 100 of ${filtered.length.toLocaleString()} matches</div>${this.renderCasesList(filtered.slice(0, 100), nodeId, 100)}`;
        } else {
            content.innerHTML = this.renderCasesList(filtered, nodeId, Math.min(50, filtered.length));
            if (!term && filtered.length > 50) this.setupVirtualScroll(filtered, nodeId);
        }
        if (container) container.scrollTop = 0;
    }

    // ── Stats & metrics ───────────────────────────────────────────────────────

    updateStats() {
        document.getElementById('nodeCount').textContent = this.nodes.size.toLocaleString();
        document.getElementById('edgeCount').textContent = this.edges.length.toLocaleString();
        document.getElementById('caseCount').textContent = eventLog.length.toLocaleString();

        if (eventLog.length > 0) {
            let total = 0, count = 0;
            eventLog.slice(0, 100).forEach(c => {
                if (c.events.length >= 2) {
                    total += new Date(c.events[c.events.length - 1].timestamp) - new Date(c.events[0].timestamp);
                    count++;
                }
            });
            if (count > 0) document.getElementById('avgDuration').textContent = `Avg: ${formatDuration(total / count)}`;
        } else {
            document.getElementById('avgDuration').textContent = '—';
        }
    }

    updatePerformanceMetrics(t0) {
        document.getElementById('renderTime').textContent = `${(performance.now() - t0).toFixed(1)}ms`;
        this.frameCount++;
        const now = performance.now();
        if (now - this.lastFpsUpdate > 1000) {
            this.fps = Math.round(this.frameCount * 1000 / (now - this.lastFpsUpdate));
            document.getElementById('fpsCounter').textContent = this.fps;
            this.frameCount = 0;
            this.lastFpsUpdate = now;
        }
    }

    // ── Edit mode ─────────────────────────────────────────────────────────────

    toggleEditMode() {
        this.editMode = !this.editMode;
        const btn = document.getElementById('editBtn');
        const helpText = document.getElementById('helpText');

        if (this.editMode) {
            btn.classList.add('btn-primary');
            btn.classList.remove('btn-outline-secondary');
            btn.innerHTML = '<i class="ti ti-check me-1"></i>Exit Edit Mode';
            this.closeDetails();
            if (helpText) helpText.innerHTML = '🔧 EDIT MODE — Drag any node to reposition &nbsp;•&nbsp; Drag background to pan &nbsp;•&nbsp; ESC to exit';

            const existing = document.getElementById('editModeIndicator');
            if (existing) existing.remove();
            const ind = document.createElement('div');
            ind.id = 'editModeIndicator';
            ind.style.cssText = 'position:absolute;top:12px;left:50%;transform:translateX(-50%);background:#2196f3;color:white;padding:8px 18px;border-radius:20px;font-size:13px;z-index:50;box-shadow:0 2px 10px rgba(0,0,0,.2);animation:pm-pulse 2s infinite;display:flex;align-items:center;gap:14px;';
            ind.innerHTML = `<span>🔧 Edit Mode: Drag nodes to reposition</span>
                <button onclick="viz.resetNodePositions()" style="background:rgba(255,255,255,.2);border:1px solid rgba(255,255,255,.4);color:white;padding:4px 10px;border-radius:4px;cursor:pointer;font-size:12px;">↺ Reset</button>`;
            document.querySelector('.visualization').appendChild(ind);
        } else {
            btn.classList.remove('btn-primary');
            btn.classList.add('btn-outline-secondary');
            btn.innerHTML = '<i class="ti ti-pencil me-1"></i>Edit schema';
            if (helpText) helpText.innerHTML = '💡 Click nodes/cases for details &nbsp;•&nbsp; Shift+Drag to move nodes &nbsp;•&nbsp; Scroll to zoom<br>📊 Click animated cases to track journeys &nbsp;•&nbsp; Purple = connected to selection<br>⚡ Use the filter bar above to load real log trace data';
            document.getElementById('editModeIndicator')?.remove();
        }

        if (this.hoveredNode) {
            this.canvas.style.cursor = (this.editMode && !this.hoveredNode.special) ? 'grab' : 'pointer';
        }
        this.requestRender();
    }

    resetNodePositions() {
        this.nodes.forEach((node, id) => {
            const orig = this.originalPositions.get(id);
            if (orig) { node.x = orig.x; node.y = orig.y; }
        });
        this.camera = { x: 0, y: 0, zoom: 1 };
        this.updateVisibleBounds();
        this.requestRender();
    }

    // ── Report capture ────────────────────────────────────────────────────────

    captureAllNodes() {
        // Save state
        const savedCamera = { ...this.camera };
        const savedCases = this.animatedCases;
        const savedRenderRequested = this.renderRequested;

        // Compute bounding box of all nodes
        let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;
        this.nodes.forEach(node => {
            minX = Math.min(minX, node.x - this.nodeWidth / 2);
            maxX = Math.max(maxX, node.x + this.nodeWidth / 2);
            minY = Math.min(minY, node.y - this.nodeHeight / 2);
            maxY = Math.max(maxY, node.y + this.nodeHeight / 2);
        });

        const padding = 60;
        const canvasW = this.canvas.width  / (window.devicePixelRatio || 1);
        const canvasH = this.canvas.height / (window.devicePixelRatio || 1);
        const contentW = maxX - minX + padding * 2;
        const contentH = maxY - minY + padding * 2;

        const zoom = Math.min(canvasW / contentW, canvasH / contentH, 2);
        const cx = (minX + maxX) / 2;
        const cy = (minY + maxY) / 2;

        // Apply fit-all camera, suppress animated dots
        this.camera = { x: -cx * zoom, y: -cy * zoom, zoom };
        this.animatedCases = [];
        this.updateVisibleBounds();
        this.renderRequested = false;
        this.render();

        const dataUrl = this.canvas.toDataURL('image/png');

        // Restore state
        this.camera = savedCamera;
        this.animatedCases = savedCases;
        this.updateVisibleBounds();
        this.renderRequested = savedRenderRequested;
        this.requestRender();

        return dataUrl;
    }
}

// ─── Report Generation ────────────────────────────────────────────────────────

function generateReport() {
    if (!viz || eventLog.length === 0) {
        showToast('Load process mining data first before generating a report.', 'warning');
        return;
    }

    const imageDataUrl = viz.captureAllNodes();
    const nodes = Array.from(viz.nodes.values()).filter(n => !n.special);
    const edges = viz.edges;
    const totalCases = eventLog.length;

    // ── Duration statistics ─────────────────────────────────────────────────
    const durations = [];
    let totalEvents = 0;
    eventLog.forEach(c => {
        (c.events || []).forEach(() => totalEvents++);
        if (c.events && c.events.length >= 2) {
            const d = new Date(c.events[c.events.length - 1].timestamp) - new Date(c.events[0].timestamp);
            if (d >= 0) durations.push({ caseId: c.caseId, duration: d, eventCount: c.events.length, firstTs: c.events[0].timestamp, lastTs: c.events[c.events.length - 1].timestamp });
        }
    });
    durations.sort((a, b) => a.duration - b.duration);
    const dv = durations.map(d => d.duration);
    const avgDuration  = dv.length > 0 ? formatDuration(dv.reduce((s, d) => s + d, 0) / dv.length) : '—';
    const minDuration  = dv.length > 0 ? formatDuration(dv[0]) : '—';
    const maxDuration  = dv.length > 0 ? formatDuration(dv[dv.length - 1]) : '—';
    const medianDuration = dv.length > 0 ? formatDuration(dv[Math.floor(dv.length / 2)]) : '—';
    const p95Duration  = dv.length > 0 ? formatDuration(dv[Math.floor(dv.length * 0.95)]) : '—';

    // ── Level analysis ──────────────────────────────────────────────────────
    const levelCounts = {}, levelCaseCounts = {};
    const levelOrder  = ['CRITICAL', 'ERROR', 'WARNING', 'INFO', 'DEBUG'];
    const levelColors = { CRITICAL: '#c62828', ERROR: '#e53935', WARNING: '#f57c00', INFO: '#1565c0', DEBUG: '#558b2f' };
    eventLog.forEach(c => {
        const seen = new Set();
        (c.events || []).forEach(ev => {
            const l = ev.level || 'UNKNOWN';
            levelCounts[l] = (levelCounts[l] || 0) + 1;
            seen.add(l);
        });
        seen.forEach(l => { levelCaseCounts[l] = (levelCaseCounts[l] || 0) + 1; });
    });
    const errorCasesUnique = eventLog.filter(c => (c.events || []).some(ev => ev.level === 'ERROR' || ev.level === 'CRITICAL')).length;
    const errorRate = totalCases > 0 ? ((errorCasesUnique / totalCases) * 100).toFixed(1) + '%' : '0%';

    // ── Duration buckets ────────────────────────────────────────────────────
    const buckets = [
        { label: '< 100ms',   min: 0,       max: 100 },
        { label: '100–999ms', min: 100,      max: 1000 },
        { label: '1–10s',     min: 1000,     max: 10000 },
        { label: '10–60s',    min: 10000,    max: 60000 },
        { label: '1–5m',      min: 60000,    max: 300000 },
        { label: '5–30m',     min: 300000,   max: 1800000 },
        { label: '30m–2h',    min: 1800000,  max: 7200000 },
        { label: '> 2h',      min: 7200000,  max: Infinity },
    ];
    buckets.forEach(b => {
        b.count = dv.filter(d => d >= b.min && d < b.max).length;
        b.pct = dv.length > 0 ? ((b.count / dv.length) * 100).toFixed(1) : '0.0';
    });
    const maxBucketCount = Math.max(...buckets.map(b => b.count), 1);
    buckets.forEach(b => { b.barW = Math.round((b.count / maxBucketCount) * 100); });

    // ── Case variants ───────────────────────────────────────────────────────
    const variantMap = new Map();
    eventLog.forEach(c => {
        const path = (c.events || []).map(e => e.activity).join(' → ');
        if (!variantMap.has(path)) variantMap.set(path, { path, count: 0, totalDur: 0, durCount: 0 });
        const v = variantMap.get(path);
        v.count++;
        if (c.events && c.events.length >= 2) {
            const d = new Date(c.events[c.events.length - 1].timestamp) - new Date(c.events[0].timestamp);
            if (d >= 0) { v.totalDur += d; v.durCount++; }
        }
    });
    const topVariants = [...variantMap.values()]
        .sort((a, b) => b.count - a.count).slice(0, 10)
        .map(v => ({
            ...v,
            avgDurMs: v.durCount > 0 ? v.totalDur / v.durCount : null,
            steps: v.path ? (v.path.match(/ → /g) || []).length + 1 : 0,
            pct: totalCases > 0 ? ((v.count / totalCases) * 100).toFixed(1) : '0.0',
        }));

    // ── Activity timing from outgoing edges ─────────────────────────────────
    const activityAvgMap = new Map();
    edges.forEach(e => {
        if (e.source !== 'START' && e.target !== 'END' && e.avgDuration) {
            if (!activityAvgMap.has(e.source)) activityAvgMap.set(e.source, { totalW: 0, totalC: 0 });
            const a = activityAvgMap.get(e.source);
            a.totalW += e.avgDuration * e.count;
            a.totalC += e.count;
        }
    });

    // ── Slowest cases ───────────────────────────────────────────────────────
    const slowestCases = [...durations].reverse().slice(0, 10);

    // ── Metadata ────────────────────────────────────────────────────────────
    const scopeType = document.querySelector('input[name="scopeType"]:checked').value;
    const scopeSelect = document.getElementById('pmScopeEntity');
    const scopeName = scopeSelect.options[scopeSelect.selectedIndex]?.text || '—';
    const fromVal = document.getElementById('pmFrom').value;
    const toVal = document.getElementById('pmTo').value;
    const topActivities = [...nodes].sort((a, b) => b.count - a.count).slice(0, 20);
    const topEdges = [...edges]
        .filter(e => e.source !== 'START' && e.target !== 'END')
        .sort((a, b) => b.count - a.count).slice(0, 20);

    const win = window.open('', '_blank');
    if (!win) {
        showToast('Could not open report window. Please allow popups for this site.', 'danger');
        return;
    }

    win.document.write(buildReportHtml({
        imageDataUrl, scopeType, scopeName, fromVal, toVal,
        totalCases, nodeCount: nodes.length, edgeCount: edges.length, totalEvents,
        avgDuration, minDuration, maxDuration, medianDuration, p95Duration,
        errorRate, errorCasesUnique, levelCounts, levelCaseCounts, levelOrder, levelColors,
        topActivities, topEdges, topVariants, slowestCases, buckets, activityAvgMap
    }));
    win.document.close();
}

function buildReportHtml({ imageDataUrl, scopeType, scopeName, fromVal, toVal,
                            totalCases, nodeCount, edgeCount, totalEvents,
                            avgDuration, minDuration, maxDuration, medianDuration, p95Duration,
                            errorRate, errorCasesUnique, levelCounts, levelCaseCounts, levelOrder, levelColors,
                            topActivities, topEdges, topVariants, slowestCases, buckets, activityAvgMap }) {
    const esc = s => String(s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    const fmtDate = s => { try { return s ? new Date(s).toLocaleString() : '—'; } catch { return s || '—'; } };
    const fmtMs = ms => {
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
    };
    const truncStr = (s, n) => s && s.length > n ? s.slice(0, n) + '…' : (s || '—');

    // ── Activity rows ────────────────────────────────────────────────────────
    const topAct0Count = topActivities.length > 0 ? topActivities[0].count : 1;
    const activityRows = topActivities.map(n => {
        const pct = totalCases > 0 ? ((n.count / totalCases) * 100).toFixed(1) : 0;
        const barW = Math.round((n.count / topAct0Count) * 100);
        const avgEntry = activityAvgMap.get(n.id);
        const avgTimeToNext = avgEntry && avgEntry.totalC > 0 ? fmtMs(avgEntry.totalW / avgEntry.totalC) : '—';
        return `<tr>
            <td>${esc(n.icon || '')} ${esc(n.id)}</td>
            <td style="text-align:right;">${n.count.toLocaleString()}</td>
            <td><div style="display:flex;align-items:center;gap:8px;">
                <div style="flex:1;background:#e8f0fe;border-radius:3px;height:8px;overflow:hidden;"><div style="width:${barW}%;height:100%;background:#1a73e8;border-radius:3px;"></div></div>
                <span style="min-width:38px;text-align:right;font-size:12px;color:#666;">${pct}%</span>
            </div></td>
            <td style="text-align:right;color:#555;">${avgTimeToNext}</td>
        </tr>`;
    }).join('');

    // ── Transition rows ──────────────────────────────────────────────────────
    const edgeRows = topEdges.map(e => {
        const pct = totalCases > 0 ? ((e.count / totalCases) * 100).toFixed(1) : 0;
        return `<tr>
            <td>${esc(e.source)}</td>
            <td style="text-align:center;color:#1a73e8;font-weight:600;">→</td>
            <td>${esc(e.target)}</td>
            <td style="text-align:right;">${e.count.toLocaleString()}</td>
            <td style="text-align:right;">${fmtMs(e.avgDuration)}</td>
            <td style="text-align:right;color:#2e7d32;">${fmtMs(e.minDuration)}</td>
            <td style="text-align:right;color:#c62828;">${fmtMs(e.maxDuration)}</td>
            <td style="text-align:right;">${pct}%</td>
        </tr>`;
    }).join('');

    // ── Duration bucket rows ─────────────────────────────────────────────────
    const bucketRows = buckets.map(b => `<tr>
        <td style="font-family:monospace;">${esc(b.label)}</td>
        <td style="text-align:right;">${b.count.toLocaleString()}</td>
        <td><div style="display:flex;align-items:center;gap:8px;">
            <div style="flex:1;background:#e8f0fe;border-radius:3px;height:10px;overflow:hidden;"><div style="width:${b.barW}%;height:100%;background:#1a73e8;border-radius:3px;"></div></div>
            <span style="min-width:42px;text-align:right;font-size:12px;color:#666;">${b.pct}%</span>
        </div></td>
    </tr>`).join('');

    // ── Level rows ───────────────────────────────────────────────────────────
    const allLevels = [...new Set([...levelOrder, ...Object.keys(levelCounts)])].filter(l => levelCounts[l]);
    const totalEventCount = Object.values(levelCounts).reduce((s, c) => s + c, 0);
    const levelRows = allLevels.map(lvl => {
        const evtCount = levelCounts[lvl] || 0;
        const caseCount = levelCaseCounts[lvl] || 0;
        const evtPct = totalEventCount > 0 ? ((evtCount / totalEventCount) * 100).toFixed(1) : '0.0';
        const casePct = totalCases > 0 ? ((caseCount / totalCases) * 100).toFixed(1) : '0.0';
        const color = levelColors[lvl] || '#555';
        const barW = totalEventCount > 0 ? Math.round((evtCount / totalEventCount) * 100) : 0;
        return `<tr>
            <td><span style="display:inline-block;padding:2px 8px;border-radius:3px;background:${color}22;color:${color};font-weight:600;font-size:12px;">${esc(lvl)}</span></td>
            <td style="text-align:right;">${evtCount.toLocaleString()}</td>
            <td><div style="display:flex;align-items:center;gap:8px;">
                <div style="flex:1;background:#eee;border-radius:3px;height:8px;overflow:hidden;"><div style="width:${barW}%;height:100%;background:${color};border-radius:3px;"></div></div>
                <span style="min-width:38px;text-align:right;font-size:12px;color:#666;">${evtPct}%</span>
            </div></td>
            <td style="text-align:right;">${caseCount.toLocaleString()}</td>
            <td style="text-align:right;color:#666;">${casePct}%</td>
        </tr>`;
    }).join('');

    // ── Variant rows ─────────────────────────────────────────────────────────
    const variantRows = topVariants.map((v, i) => `<tr>
        <td style="text-align:center;color:#999;font-size:12px;">${i + 1}</td>
        <td style="font-size:12px;" title="${esc(v.path)}">${esc(truncStr(v.path, 90))}</td>
        <td style="text-align:right;">${v.steps}</td>
        <td style="text-align:right;">${v.count.toLocaleString()}</td>
        <td style="text-align:right;">${v.pct}%</td>
        <td style="text-align:right;">${v.avgDurMs !== null ? fmtMs(v.avgDurMs) : '—'}</td>
    </tr>`).join('');

    // ── Slowest case rows ────────────────────────────────────────────────────
    const slowRows = slowestCases.map((c, i) => `<tr>
        <td style="text-align:center;color:#999;font-size:12px;">${i + 1}</td>
        <td style="font-family:monospace;font-size:11px;" title="${esc(c.caseId)}">${esc(truncStr(c.caseId, 32))}</td>
        <td style="text-align:right;">${c.eventCount}</td>
        <td style="font-size:12px;color:#555;">${fmtDate(c.firstTs)}</td>
        <td style="font-size:12px;color:#555;">${fmtDate(c.lastTs)}</td>
        <td style="text-align:right;font-weight:600;color:#c62828;">${fmtMs(c.duration)}</td>
    </tr>`).join('');

    const now = new Date().toLocaleString();
    const hasErrors = errorCasesUnique > 0;

    return `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>Process Mining Report — ${esc(scopeName)}</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size: 13px; color: #222; background: #f4f6fb; }
  .report-wrapper { max-width: 1200px; margin: 0 auto; padding: 32px 40px; background: #fff; min-height: 100vh; }
  .report-header { display: flex; justify-content: space-between; align-items: flex-start; padding-bottom: 18px; border-bottom: 3px solid #1a73e8; margin-bottom: 24px; }
  .report-title { font-size: 22px; font-weight: 700; color: #1a1a2e; }
  .report-subtitle { font-size: 13px; color: #555; margin-top: 5px; line-height: 1.6; }
  .report-meta { text-align: right; font-size: 12px; color: #777; line-height: 1.9; }
  .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 12px; }
  .stats-grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 28px; }
  .stat-box { background: #f0f4ff; border: 1px solid #d0deff; border-radius: 8px; padding: 14px 12px; text-align: center; }
  .stat-box.green { background: #f0fff4; border-color: #b2dfdb; }
  .stat-box.red   { background: #fff0f0; border-color: #ffcdd2; }
  .stat-box.amber { background: #fffde7; border-color: #ffe082; }
  .stat-box .value { font-size: 22px; font-weight: 700; color: #1a73e8; line-height: 1.2; }
  .stat-box.green .value { color: #2e7d32; }
  .stat-box.red   .value { color: #c62828; }
  .stat-box.amber .value { color: #e65100; }
  .stat-box .label { font-size: 11px; color: #666; margin-top: 4px; text-transform: uppercase; letter-spacing: .05em; }
  .section-title { font-size: 14px; font-weight: 700; color: #1a1a2e; margin-bottom: 12px; padding-bottom: 7px; border-bottom: 1px solid #e0e0e0; letter-spacing: .01em; }
  .section { margin-bottom: 28px; }
  .two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 28px; margin-bottom: 28px; }
  .two-col .section { margin-bottom: 0; }
  .process-map { border: 1px solid #dde3f0; border-radius: 8px; overflow: hidden; margin-bottom: 28px; background: #e8e8e8; }
  .process-map img { width: 100%; display: block; }
  table { width: 100%; border-collapse: collapse; font-size: 13px; }
  th { background: #eef2ff; color: #444; font-weight: 600; text-align: left; padding: 9px 12px; border-bottom: 2px solid #d0deff; font-size: 12px; text-transform: uppercase; letter-spacing: .04em; white-space: nowrap; }
  td { padding: 8px 12px; border-bottom: 1px solid #eee; color: #333; vertical-align: middle; }
  tr:last-child td { border-bottom: none; }
  tr:hover td { background: #f5f8ff; }
  .btn-print { display: inline-flex; align-items: center; gap: 7px; padding: 10px 22px; background: #1a73e8; color: white; border: none; border-radius: 6px; font-size: 14px; font-weight: 500; cursor: pointer; margin-bottom: 24px; text-decoration: none; }
  .btn-print:hover { background: #1557b0; }
  .footer { font-size: 11px; color: #aaa; text-align: center; padding-top: 16px; border-top: 1px solid #eee; margin-top: 8px; }
  @media print {
    body { background: #fff; }
    .btn-print { display: none !important; }
    .report-wrapper { padding: 16px 20px; max-width: none; min-height: auto; }
    .two-col { grid-template-columns: 1fr 1fr; }
    .process-map, .stats-grid, .stats-grid-3, .two-col, .section { page-break-inside: avoid; }
    tr { page-break-inside: avoid; }
  }
</style>
</head>
<body>
<div class="report-wrapper">

  <div class="report-header">
    <div>
      <div class="report-title">Process Mining Report</div>
      <div class="report-subtitle">
        <strong>${esc(scopeType === 'platform' ? 'Platform' : 'Application')}:</strong> ${esc(scopeName)}<br>
        <strong>Period:</strong> ${esc(fmtDate(fromVal))} &ndash; ${esc(fmtDate(toVal))}
      </div>
    </div>
    <div class="report-meta">Generated: ${esc(now)}<br>Status Monitor</div>
  </div>

  <button class="btn-print" onclick="window.print()">
    <svg width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M6 9V2h12v7M6 18H4a2 2 0 01-2-2v-5a2 2 0 012-2h16a2 2 0 012 2v5a2 2 0 01-2 2h-2M6 14h12v8H6z"/></svg>
    Print / Save as PDF
  </button>

  <div class="stats-grid">
    <div class="stat-box"><div class="value">${totalCases.toLocaleString()}</div><div class="label">Total Cases</div></div>
    <div class="stat-box"><div class="value">${nodeCount.toLocaleString()}</div><div class="label">Activities</div></div>
    <div class="stat-box"><div class="value">${edgeCount.toLocaleString()}</div><div class="label">Transitions</div></div>
    <div class="stat-box"><div class="value">${totalEvents.toLocaleString()}</div><div class="label">Total Events</div></div>
  </div>
  <div class="stats-grid">
    <div class="stat-box green"><div class="value">${esc(minDuration)}</div><div class="label">Min Duration</div></div>
    <div class="stat-box"><div class="value">${esc(medianDuration)}</div><div class="label">Median Duration</div></div>
    <div class="stat-box amber"><div class="value">${esc(avgDuration)}</div><div class="label">Avg Duration</div></div>
    <div class="stat-box red"><div class="value">${esc(maxDuration)}</div><div class="label">Max Duration</div></div>
  </div>
  <div class="stats-grid-3">
    <div class="stat-box"><div class="value">${esc(p95Duration)}</div><div class="label">p95 Duration</div></div>
    <div class="stat-box ${hasErrors ? 'red' : 'green'}"><div class="value">${esc(errorRate)}</div><div class="label">Cases with Errors</div></div>
    <div class="stat-box ${hasErrors ? 'red' : 'green'}"><div class="value">${errorCasesUnique.toLocaleString()}</div><div class="label">Error Case Count</div></div>
  </div>

  <div class="section-title">Process Map</div>
  <div class="process-map"><img src="${imageDataUrl}" alt="Process Map — ${esc(scopeName)}"></div>

  <div class="two-col">
    <div class="section">
      <div class="section-title">Duration Distribution</div>
      <table>
        <thead><tr><th>Bucket</th><th style="text-align:right;">Cases</th><th>Distribution</th></tr></thead>
        <tbody>${bucketRows || '<tr><td colspan="3" style="color:#aaa;text-align:center;padding:16px;">No duration data</td></tr>'}</tbody>
      </table>
    </div>
    <div class="section">
      <div class="section-title">Log Level Breakdown</div>
      <table>
        <thead><tr><th>Level</th><th style="text-align:right;">Events</th><th>Distribution</th><th style="text-align:right;">Cases</th><th style="text-align:right;">% Cases</th></tr></thead>
        <tbody>${levelRows || '<tr><td colspan="5" style="color:#aaa;text-align:center;padding:16px;">No level data</td></tr>'}</tbody>
      </table>
    </div>
  </div>

  <div class="section">
    <div class="section-title">Top Activities by Frequency</div>
    <table>
      <thead><tr><th>Activity</th><th style="text-align:right;">Event Count</th><th>% of Cases</th><th style="text-align:right;">Avg Time to Next</th></tr></thead>
      <tbody>${activityRows || '<tr><td colspan="4" style="color:#aaa;text-align:center;padding:16px;">No activities</td></tr>'}</tbody>
    </table>
  </div>

  <div class="section">
    <div class="section-title">Top Transitions</div>
    <table>
      <thead><tr><th>From</th><th></th><th>To</th><th style="text-align:right;">Count</th><th style="text-align:right;">Avg Duration</th><th style="text-align:right;">Min</th><th style="text-align:right;">Max</th><th style="text-align:right;">% Cases</th></tr></thead>
      <tbody>${edgeRows || '<tr><td colspan="8" style="color:#aaa;text-align:center;padding:16px;">No transitions</td></tr>'}</tbody>
    </table>
  </div>

  <div class="section">
    <div class="section-title">Case Variants — Top 10 Unique Paths</div>
    <table>
      <thead><tr><th style="text-align:center;">#</th><th>Path</th><th style="text-align:right;">Steps</th><th style="text-align:right;">Cases</th><th style="text-align:right;">% Total</th><th style="text-align:right;">Avg Duration</th></tr></thead>
      <tbody>${variantRows || '<tr><td colspan="6" style="color:#aaa;text-align:center;padding:16px;">No variant data</td></tr>'}</tbody>
    </table>
  </div>

  <div class="section">
    <div class="section-title">Slowest 10 Cases</div>
    <table>
      <thead><tr><th style="text-align:center;">#</th><th>Case ID</th><th style="text-align:right;">Events</th><th>Start Time</th><th>End Time</th><th style="text-align:right;">Duration</th></tr></thead>
      <tbody>${slowRows || '<tr><td colspan="6" style="color:#aaa;text-align:center;padding:16px;">No duration data available</td></tr>'}</tbody>
    </table>
  </div>

  <div class="footer">Generated by Status Monitor Process Mining &bull; ${esc(now)}</div>
</div>
</body>
</html>`;
}

// ─── Boot ─────────────────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', initPage);
