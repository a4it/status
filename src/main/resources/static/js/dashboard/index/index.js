/**
 * Dashboard Status Page JavaScript
 * Shows platforms with expandable components for authenticated users
 */

let platformsCache = [];

document.addEventListener('DOMContentLoaded', function() {
    // Require authentication
    if (!auth.requireAuth()) return;

    // Update user info in navbar
    updateUserInfo();

    // Load status data
    loadStatusSummary();
    loadPlatforms();
    loadRecentEvents();
});

function updateUserInfo() {
    const user = auth.getUser();
    if (user) {
        const avatarEl = document.getElementById('user-avatar');
        const nameEl = document.getElementById('user-name');
        const roleEl = document.getElementById('user-role');

        if (avatarEl) avatarEl.textContent = (user.username || 'U').charAt(0).toUpperCase();
        if (nameEl) nameEl.textContent = user.username || 'User';
        if (roleEl && user.roles && user.roles.length > 0) {
            roleEl.textContent = user.roles[0].replace('ROLE_', '');
        }
    }
}

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
                            <a href="/admin/issues" class="text-reset">${escapeHtml(incident.title)}</a>
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
            return;
        }

        platformsCache = apps;
        displayPlatforms(apps);
    } catch (error) {
        console.error('Failed to load platforms:', error);
        displayPlatformsError(error.message || 'Failed to load platforms');
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
        <a href="/admin/issues" class="list-group-item list-group-item-action">
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
