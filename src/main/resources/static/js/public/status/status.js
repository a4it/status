/**
 * Public Status Page JavaScript
 * Shows platforms with expandable components and uptime charts
 */

let platformsCache = [];
let uptimeDataCache = {};
let currentUptimeDetailData = null;

document.addEventListener('DOMContentLoaded', function() {
    loadStatusSummary();
    loadPlatforms();
    loadRecentEvents();
    initUptimeDetailModal();
});

async function loadStatusSummary() {
    try {
        const response = await api.get('/public/status/summary');
        if (response) {
            displayStatusSummary(response);
        } else {
            displayStatusError('No data available');
        }
    } catch (error) {
        console.error('Failed to load status summary:', error);
        displayStatusError(error.message || 'Failed to load status');
    }
}

function displayStatusSummary(summary) {
    const loadingEl = document.getElementById('overall-status-loading');
    const contentEl = document.getElementById('overall-status-content');
    const cardEl = document.getElementById('overall-status-card');
    const iconEl = document.getElementById('status-icon');
    const titleEl = document.getElementById('status-title');
    const descEl = document.getElementById('status-description');

    loadingEl.style.display = 'none';
    contentEl.style.display = 'block';

    const overallStatus = summary.overallStatus || 'OPERATIONAL';

    if (overallStatus === 'OPERATIONAL') {
        cardEl.classList.add('all-operational');
        iconEl.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" class="status-icon-lg" width="64" height="64" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round"><path stroke="none" d="M0 0h24v24H0z" fill="none"/><circle cx="12" cy="12" r="9"/><path d="M9 12l2 2l4 -4"/></svg>`;
        titleEl.textContent = 'All Systems Operational';
        descEl.textContent = 'All services are running smoothly.';
    } else if (overallStatus === 'DEGRADED') {
        cardEl.classList.add('has-issues');
        iconEl.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" class="status-icon-lg" width="64" height="64" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round"><path stroke="none" d="M0 0h24v24H0z" fill="none"/><path d="M12 9v2m0 4v.01"/><path d="M5 19h14a2 2 0 0 0 1.84 -2.75l-7.1 -12.25a2 2 0 0 0 -3.5 0l-7.1 12.25a2 2 0 0 0 1.75 2.75"/></svg>`;
        titleEl.textContent = 'Degraded Performance';
        descEl.textContent = 'Some services may be experiencing issues.';
    } else {
        cardEl.classList.add('major-issues');
        iconEl.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" class="status-icon-lg" width="64" height="64" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round"><path stroke="none" d="M0 0h24v24H0z" fill="none"/><circle cx="12" cy="12" r="9"/><path d="M12 8v4"/><path d="M12 16h.01"/></svg>`;
        titleEl.textContent = 'Major Outage';
        descEl.textContent = 'We are experiencing significant issues with our services.';
    }

    // Show active incidents if any
    if (summary.activeIncidents && summary.activeIncidents.length > 0) {
        displayActiveIncidents(summary.activeIncidents);
    }

    // Show scheduled maintenance if any
    if (summary.upcomingMaintenance && summary.upcomingMaintenance.length > 0) {
        displayScheduledMaintenance(summary.upcomingMaintenance);
    }
}

function displayStatusError(message) {
    const loadingEl = document.getElementById('overall-status-loading');
    const cardEl = document.getElementById('overall-status-card');
    if (cardEl) {
        cardEl.classList.add('border-warning');
    }
    loadingEl.innerHTML = `
        <div class="text-center">
            <svg xmlns="http://www.w3.org/2000/svg" class="icon icon-lg text-warning mb-2" width="48" height="48" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round">
                <path stroke="none" d="M0 0h24v24H0z" fill="none"/><path d="M12 9v2m0 4v.01"/><path d="M5 19h14a2 2 0 0 0 1.84 -2.75l-7.1 -12.25a2 2 0 0 0 -3.5 0l-7.1 12.25a2 2 0 0 0 1.75 2.75"/>
            </svg>
            <p class="text-muted mb-0">${message || 'Failed to load status. Please refresh the page.'}</p>
        </div>`;
}

function displayActiveIncidents(incidents) {
    const section = document.getElementById('active-incidents-section');
    const list = document.getElementById('active-incidents-list');

    section.style.display = 'block';
    list.innerHTML = incidents.map(incident => `
        <div class="card mb-2">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-start">
                    <div>
                        <h4 class="mb-1">
                            <a href="/incidents/${incident.id}" class="text-reset">${escapeHtml(incident.title)}</a>
                        </h4>
                        <p class="text-muted mb-0">${formatDate(incident.startedAt)}</p>
                    </div>
                    <span class="badge badge-${getImpactBadgeClass(incident.impact)}">${incident.impact}</span>
                </div>
            </div>
        </div>
    `).join('');
}

function displayScheduledMaintenance(maintenances) {
    const section = document.getElementById('scheduled-maintenance-section');
    const list = document.getElementById('scheduled-maintenance-list');

    section.style.display = 'block';
    list.innerHTML = maintenances.map(maintenance => `
        <div class="card mb-2">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-start">
                    <div>
                        <h4 class="mb-1">${escapeHtml(maintenance.title)}</h4>
                        <p class="text-muted mb-0">
                            Scheduled: ${formatDate(maintenance.scheduledStart)} - ${formatDate(maintenance.scheduledEnd)}
                        </p>
                    </div>
                    <span class="badge bg-blue">Scheduled</span>
                </div>
            </div>
        </div>
    `).join('');
}

async function loadPlatforms() {
    try {
        const apps = await api.get('/public/status/apps');

        if (!apps || apps.length === 0) {
            document.getElementById('platforms-loading').style.display = 'none';
            document.getElementById('platforms-empty').style.display = 'block';
            document.getElementById('uptime-loading').style.display = 'none';
            document.getElementById('uptime-empty').style.display = 'block';
            return;
        }

        platformsCache = apps;
        displayPlatforms(apps);
        loadUptimeData(apps);
    } catch (error) {
        console.error('Failed to load platforms:', error);
        displayPlatformsError(error.message || 'Failed to load platforms');
        displayUptimeError(error.message || 'Failed to load uptime data');
    }
}

function displayPlatformsError(message) {
    const loadingEl = document.getElementById('platforms-loading');
    loadingEl.innerHTML = `
        <div class="text-center py-3">
            <svg xmlns="http://www.w3.org/2000/svg" class="icon icon-lg text-warning mb-2" width="48" height="48" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round">
                <path stroke="none" d="M0 0h24v24H0z" fill="none"/><path d="M12 9v2m0 4v.01"/><path d="M5 19h14a2 2 0 0 0 1.84 -2.75l-7.1 -12.25a2 2 0 0 0 -3.5 0l-7.1 12.25a2 2 0 0 0 1.75 2.75"/>
            </svg>
            <p class="text-muted mb-0">${message}</p>
        </div>`;
}

function displayPlatforms(platforms) {
    const loadingEl = document.getElementById('platforms-loading');
    const listEl = document.getElementById('platforms-list');
    const emptyEl = document.getElementById('platforms-empty');

    loadingEl.style.display = 'none';

    if (platforms.length === 0) {
        emptyEl.style.display = 'block';
        return;
    }

    listEl.style.display = 'block';
    listEl.innerHTML = platforms.map(platform => `
        <div class="list-group-item list-group-item-action platform-item" data-platform-id="${platform.id}" onclick="togglePlatformComponents('${platform.id}')">
            <div class="d-flex justify-content-between align-items-center">
                <div class="d-flex align-items-center">
                    <svg xmlns="http://www.w3.org/2000/svg" class="icon me-2 platform-chevron" id="chevron-${platform.id}" width="24" height="24" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round"><path stroke="none" d="M0 0h24v24H0z" fill="none"/><polyline points="9 6 15 12 9 18"/></svg>
                    <div>
                        <strong>${escapeHtml(platform.name)}</strong>
                        ${platform.description ? `<p class="text-muted mb-0 small">${escapeHtml(platform.description)}</p>` : ''}
                    </div>
                </div>
                <div class="component-status">
                    <span class="component-status-dot ${getStatusClass(platform.status)}"></span>
                    <span class="text-muted">${formatStatus(platform.status)}</span>
                </div>
            </div>
        </div>
        <div class="collapse" id="components-${platform.id}">
            <div class="components-container bg-light" id="components-list-${platform.id}">
                <div class="text-center py-3">
                    <div class="spinner-border spinner-border-sm text-primary" role="status">
                        <span class="visually-hidden">Loading...</span>
                    </div>
                </div>
            </div>
        </div>
    `).join('');
}

async function togglePlatformComponents(platformId) {
    const collapseEl = document.getElementById(`components-${platformId}`);
    const chevronEl = document.getElementById(`chevron-${platformId}`);
    const componentsListEl = document.getElementById(`components-list-${platformId}`);

    // Check if already expanded
    const isExpanded = collapseEl.classList.contains('show');

    if (isExpanded) {
        // Collapse
        collapseEl.classList.remove('show');
        chevronEl.style.transform = 'rotate(0deg)';
    } else {
        // Expand and load components
        collapseEl.classList.add('show');
        chevronEl.style.transform = 'rotate(90deg)';

        // Check if components already loaded
        if (componentsListEl.dataset.loaded !== 'true') {
            try {
                const components = await api.get(`/public/status/apps/${platformId}/components`);
                displayPlatformComponents(platformId, components);
                componentsListEl.dataset.loaded = 'true';
            } catch (error) {
                console.error('Failed to load components:', error);
                componentsListEl.innerHTML = `<div class="text-center py-3 text-muted">Failed to load components</div>`;
            }
        }
    }
}

function displayPlatformComponents(platformId, components) {
    const listEl = document.getElementById(`components-list-${platformId}`);

    if (!components || components.length === 0) {
        listEl.innerHTML = `<div class="text-center py-3 text-muted">No components configured for this platform</div>`;
        return;
    }

    listEl.innerHTML = components.map(component => `
        <div class="list-group-item border-0 ps-5">
            <div class="d-flex justify-content-between align-items-center">
                <div>
                    <span>${escapeHtml(component.name)}</span>
                    ${component.description ? `<p class="text-muted mb-0 small">${escapeHtml(component.description)}</p>` : ''}
                </div>
                <div class="component-status">
                    <span class="component-status-dot ${getStatusClass(component.status)}"></span>
                    <span class="text-muted small">${formatStatus(component.status)}</span>
                </div>
            </div>
        </div>
    `).join('');
}

async function loadRecentEvents() {
    try {
        const apps = await api.get('/public/status/apps');

        if (!apps || apps.length === 0) {
            document.getElementById('events-loading').style.display = 'none';
            document.getElementById('events-empty').style.display = 'block';
            return;
        }

        // Load recent incidents for all apps
        let allIncidents = [];
        for (const app of apps) {
            try {
                const incidents = await api.get(`/public/status/apps/${app.id}/incidents?days=7`);
                if (incidents && incidents.length > 0) {
                    allIncidents = allIncidents.concat(incidents);
                }
            } catch (incidentError) {
                console.warn(`Failed to load incidents for app ${app.name}:`, incidentError);
            }
        }

        // Sort by date and take latest 5
        allIncidents.sort((a, b) => new Date(b.startedAt) - new Date(a.startedAt));
        allIncidents = allIncidents.slice(0, 5);

        displayRecentEvents(allIncidents);
    } catch (error) {
        console.error('Failed to load recent events:', error);
        displayEventsError(error.message || 'Failed to load events');
    }
}

function displayEventsError(message) {
    const loadingEl = document.getElementById('events-loading');
    loadingEl.innerHTML = `
        <div class="text-center py-3">
            <svg xmlns="http://www.w3.org/2000/svg" class="icon icon-lg text-warning mb-2" width="48" height="48" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round">
                <path stroke="none" d="M0 0h24v24H0z" fill="none"/><path d="M12 9v2m0 4v.01"/><path d="M5 19h14a2 2 0 0 0 1.84 -2.75l-7.1 -12.25a2 2 0 0 0 -3.5 0l-7.1 12.25a2 2 0 0 0 1.75 2.75"/>
            </svg>
            <p class="text-muted mb-0">${message}</p>
        </div>`;
}

function displayRecentEvents(incidents) {
    const loadingEl = document.getElementById('events-loading');
    const listEl = document.getElementById('events-list');
    const emptyEl = document.getElementById('events-empty');

    loadingEl.style.display = 'none';

    if (incidents.length === 0) {
        emptyEl.style.display = 'block';
        return;
    }

    listEl.style.display = 'block';
    listEl.innerHTML = incidents.map(incident => `
        <a href="/incidents/${incident.id}" class="list-group-item list-group-item-action">
            <div class="d-flex justify-content-between align-items-center">
                <div>
                    <strong>${escapeHtml(incident.title)}</strong>
                    <p class="text-muted mb-0 small">${formatDate(incident.startedAt)}</p>
                </div>
                <span class="badge ${incident.status === 'RESOLVED' ? 'bg-green' : 'bg-yellow'}">${incident.status}</span>
            </div>
        </a>
    `).join('');
}

// Utility functions
function getStatusClass(status) {
    const statusMap = {
        'OPERATIONAL': 'operational',
        'DEGRADED': 'degraded',
        'DEGRADED_PERFORMANCE': 'degraded',
        'PARTIAL_OUTAGE': 'partial-outage',
        'MAJOR_OUTAGE': 'major-outage',
        'MAINTENANCE': 'maintenance',
        'UNDER_MAINTENANCE': 'maintenance'
    };
    return statusMap[status] || 'operational';
}

function formatStatus(status) {
    const statusMap = {
        'OPERATIONAL': 'Operational',
        'DEGRADED': 'Degraded',
        'DEGRADED_PERFORMANCE': 'Degraded Performance',
        'PARTIAL_OUTAGE': 'Partial Outage',
        'MAJOR_OUTAGE': 'Major Outage',
        'MAINTENANCE': 'Under Maintenance',
        'UNDER_MAINTENANCE': 'Under Maintenance'
    };
    return statusMap[status] || status;
}

function getImpactBadgeClass(impact) {
    const impactMap = {
        'MINOR': 'minor',
        'MAJOR': 'major',
        'CRITICAL': 'critical'
    };
    return impactMap[impact] || 'secondary';
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleString();
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ========================================
// Uptime Chart Functions
// ========================================

async function loadUptimeData(apps) {
    try {
        const containerEl = document.getElementById('uptime-cards-container');
        const loadingEl = document.getElementById('uptime-loading');

        let uptimeHtml = '';

        for (const app of apps) {
            try {
                // Load platform uptime
                const appUptime = await api.get(`/public/status/apps/${app.id}/uptime-history?days=90`);
                uptimeDataCache[app.id] = { appUptime, componentUptimes: null };

                uptimeHtml += renderPlatformUptimeCard(app, appUptime);
            } catch (appError) {
                console.warn(`Failed to load uptime for app ${app.name}:`, appError);
            }
        }

        loadingEl.style.display = 'none';

        if (uptimeHtml) {
            containerEl.innerHTML = uptimeHtml;
            attachUptimeBarListeners();
            attachComponentExpandListeners();
        }
    } catch (error) {
        console.error('Failed to load uptime data:', error);
        displayUptimeError(error.message || 'Failed to load uptime data');
    }
}

function displayUptimeError(message) {
    const loadingEl = document.getElementById('uptime-loading');
    loadingEl.innerHTML = `
        <div class="text-center py-3">
            <svg xmlns="http://www.w3.org/2000/svg" class="icon icon-lg text-warning mb-2" width="48" height="48" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round">
                <path stroke="none" d="M0 0h24v24H0z" fill="none"/><path d="M12 9v2m0 4v.01"/><path d="M5 19h14a2 2 0 0 0 1.84 -2.75l-7.1 -12.25a2 2 0 0 0 -3.5 0l-7.1 12.25a2 2 0 0 0 1.75 2.75"/>
            </svg>
            <p class="text-muted mb-0">${message}</p>
        </div>`;
}

function renderPlatformUptimeCard(app, appUptime) {
    const percentageClass = getUptimePercentageClass(appUptime.overallUptimePercentage);
    const firstDate = appUptime.dailyHistory && appUptime.dailyHistory.length > 0
        ? formatDateShort(appUptime.dailyHistory[0].date) : '';
    const lastDate = appUptime.dailyHistory && appUptime.dailyHistory.length > 0
        ? formatDateShort(appUptime.dailyHistory[appUptime.dailyHistory.length - 1].date) : '';

    return `
        <div class="card mb-3 platform-uptime-card" data-platform-id="${app.id}">
            <div class="card-body">
                <div class="uptime-header">
                    <span class="uptime-title">${escapeHtml(app.name)}</span>
                    <span class="uptime-percentage ${percentageClass}">${formatUptime(appUptime.overallUptimePercentage)}%</span>
                </div>
                ${renderUptimeBars(appUptime.dailyHistory, app.id, 'app')}
                <div class="uptime-legend">
                    <span>${firstDate}</span>
                    <span>${lastDate}</span>
                </div>
                <div class="mt-3">
                    <button class="btn btn-link btn-sm p-0 component-expand-btn" data-platform-id="${app.id}">
                        <svg xmlns="http://www.w3.org/2000/svg" class="icon component-chevron" width="16" height="16" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round">
                            <path stroke="none" d="M0 0h24v24H0z" fill="none"/>
                            <polyline points="9 6 15 12 9 18"/>
                        </svg>
                        <span class="component-btn-text">Show component details</span>
                    </button>
                </div>
                <div class="component-uptime-container" id="components-uptime-${app.id}" style="display: none;">
                    <div class="text-center py-3">
                        <div class="spinner-border spinner-border-sm text-primary" role="status">
                            <span class="visually-hidden">Loading...</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;
}

function renderComponentUptimeCharts(componentUptimes) {
    if (!componentUptimes || componentUptimes.length === 0) {
        return '<div class="text-muted text-center py-3">No components configured for this platform</div>';
    }

    let html = '';
    for (const componentUptime of componentUptimes) {
        const compPercentageClass = getUptimePercentageClass(componentUptime.overallUptimePercentage);
        const firstDate = componentUptime.dailyHistory && componentUptime.dailyHistory.length > 0
            ? formatDateShort(componentUptime.dailyHistory[0].date) : '';
        const lastDate = componentUptime.dailyHistory && componentUptime.dailyHistory.length > 0
            ? formatDateShort(componentUptime.dailyHistory[componentUptime.dailyHistory.length - 1].date) : '';

        html += `
            <div class="component-uptime" data-component-id="${componentUptime.id}">
                <div class="uptime-header">
                    <span class="uptime-title">${escapeHtml(componentUptime.name)}</span>
                    <span class="uptime-percentage ${compPercentageClass}">${formatUptime(componentUptime.overallUptimePercentage)}%</span>
                </div>
                ${renderUptimeBars(componentUptime.dailyHistory, componentUptime.id, 'component')}
                <div class="uptime-legend">
                    <span>${firstDate}</span>
                    <span>${lastDate}</span>
                </div>
            </div>
        `;
    }
    return html;
}

function attachComponentExpandListeners() {
    document.querySelectorAll('.component-expand-btn').forEach(btn => {
        btn.addEventListener('click', async function() {
            const platformId = this.dataset.platformId;
            const containerEl = document.getElementById(`components-uptime-${platformId}`);
            const chevronEl = this.querySelector('.component-chevron');
            const textEl = this.querySelector('.component-btn-text');
            const isExpanded = containerEl.style.display !== 'none';

            if (isExpanded) {
                // Collapse
                containerEl.style.display = 'none';
                chevronEl.style.transform = 'rotate(0deg)';
                textEl.textContent = 'Show component details';
            } else {
                // Expand
                containerEl.style.display = 'block';
                chevronEl.style.transform = 'rotate(90deg)';
                textEl.textContent = 'Hide component details';

                // Load component data if not already loaded
                if (!uptimeDataCache[platformId]?.componentUptimes) {
                    try {
                        const componentUptimes = await api.get(`/public/status/apps/${platformId}/components/uptime-history?days=90`);
                        uptimeDataCache[platformId].componentUptimes = componentUptimes;
                        containerEl.innerHTML = renderComponentUptimeCharts(componentUptimes);
                        attachUptimeBarListeners();
                    } catch (error) {
                        console.error('Failed to load component uptime:', error);
                        containerEl.innerHTML = '<div class="text-danger text-center py-3">Failed to load component data</div>';
                    }
                }
            }
        });
    });
}

function renderUptimeBars(dailyHistory, entityId, entityType) {
    if (!dailyHistory || dailyHistory.length === 0) {
        return '<div class="text-muted text-center py-2">No uptime data available</div>';
    }

    const bars = dailyHistory.map((day, index) => {
        const dateStr = formatDateShort(day.date);
        const statusClass = getStatusClassFromUptime(day.status);
        const hasIncident = day.incidentCount > 0;
        const incidentClass = hasIncident ? 'has-incident' : '';
        const tooltip = `${dateStr}: ${formatUptime(day.uptimePercentage)}%${hasIncident ? ' - ' + day.incidentCount + ' incident(s)' : ''}`;

        return `<div class="uptime-bar ${statusClass} ${incidentClass}"
                     data-tooltip="${tooltip}"
                     data-index="${index}"
                     data-entity-id="${entityId}"
                     data-entity-type="${entityType}"
                     data-date="${day.date}"
                     data-status="${day.status}"
                     data-uptime="${day.uptimePercentage}"
                     data-incidents="${day.incidentCount}"
                     data-maintenance="${day.maintenanceCount}"></div>`;
    }).join('');

    return `<div class="uptime-bars">${bars}</div>`;
}

function attachUptimeBarListeners() {
    document.querySelectorAll('.uptime-bar').forEach(bar => {
        bar.addEventListener('click', function() {
            const date = this.dataset.date;
            const status = this.dataset.status;
            const uptime = this.dataset.uptime;
            const incidents = parseInt(this.dataset.incidents) || 0;
            const maintenance = parseInt(this.dataset.maintenance) || 0;

            showUptimeDetail({
                date,
                status,
                uptimePercentage: uptime,
                incidentCount: incidents,
                maintenanceCount: maintenance
            });
        });
    });
}

function initUptimeDetailModal() {
    const modal = document.getElementById('uptimeDetailModal');
    const closeBtn = document.getElementById('uptimeDetailClose');

    if (closeBtn) {
        closeBtn.addEventListener('click', hideUptimeDetail);
    }

    if (modal) {
        modal.addEventListener('click', function(e) {
            if (e.target === this) {
                hideUptimeDetail();
            }
        });
    }

    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            hideUptimeDetail();
        }
    });
}

function showUptimeDetail(data) {
    const modal = document.getElementById('uptimeDetailModal');
    const dateEl = document.getElementById('uptimeDetailDate');
    const bodyEl = document.getElementById('uptimeDetailBody');

    currentUptimeDetailData = data;

    const dateStr = formatDateLong(data.date);
    dateEl.textContent = dateStr;

    const statusLabels = {
        'OPERATIONAL': 'Operational',
        'DEGRADED': 'Degraded Performance',
        'PARTIAL_OUTAGE': 'Partial Outage',
        'MAJOR_OUTAGE': 'Major Outage',
        'OUTAGE': 'Service Outage',
        'MAINTENANCE': 'Under Maintenance'
    };

    const statusLabel = statusLabels[data.status] || data.status;
    const statusClass = getStatusClassFromUptime(data.status);

    bodyEl.innerHTML = `
        <div class="uptime-detail-stats">
            <div class="uptime-stat">
                <div class="uptime-stat-label">Status</div>
                <div class="uptime-stat-value ${statusClass}">${statusLabel}</div>
            </div>
            <div class="uptime-stat">
                <div class="uptime-stat-label">Uptime</div>
                <div class="uptime-stat-value ${statusClass}">${formatUptime(data.uptimePercentage)}%</div>
            </div>
        </div>
        ${data.incidentCount > 0 ? `
        <div class="alert alert-warning">
            <strong>${data.incidentCount}</strong> incident(s) occurred on this day.
        </div>
        ` : ''}
        ${data.maintenanceCount > 0 ? `
        <div class="alert alert-info">
            <strong>${data.maintenanceCount}</strong> maintenance window(s) on this day.
        </div>
        ` : ''}
        ${data.incidentCount === 0 && data.maintenanceCount === 0 && data.status === 'OPERATIONAL' ? `
        <div class="text-center text-muted py-3">
            <svg xmlns="http://www.w3.org/2000/svg" class="icon icon-lg mb-2" width="48" height="48" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round">
                <path stroke="none" d="M0 0h24v24H0z" fill="none"/><circle cx="12" cy="12" r="9"/><path d="M9 12l2 2l4 -4"/>
            </svg>
            <p class="mb-0">No incidents on this day. Everything was operational.</p>
        </div>
        ` : ''}
    `;

    modal.classList.add('show');
    document.body.style.overflow = 'hidden';
}

function hideUptimeDetail() {
    const modal = document.getElementById('uptimeDetailModal');
    modal.classList.remove('show');
    document.body.style.overflow = '';
    currentUptimeDetailData = null;
}

function getStatusClassFromUptime(status) {
    const statusMap = {
        'OPERATIONAL': 'operational',
        'DEGRADED': 'degraded',
        'DEGRADED_PERFORMANCE': 'degraded',
        'PARTIAL_OUTAGE': 'partial-outage',
        'MAJOR_OUTAGE': 'major-outage',
        'OUTAGE': 'outage',
        'MAINTENANCE': 'maintenance'
    };
    return statusMap[status] || 'operational';
}

function getUptimePercentageClass(percentage) {
    if (percentage >= 99.5) return 'operational';
    if (percentage >= 95) return 'degraded';
    return 'outage';
}

function formatUptime(percentage) {
    if (percentage === null || percentage === undefined) return '100.000';
    return parseFloat(percentage).toFixed(3);
}

function formatDateShort(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

function formatDateLong(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
        weekday: 'long',
        month: 'long',
        day: 'numeric',
        year: 'numeric'
    });
}
