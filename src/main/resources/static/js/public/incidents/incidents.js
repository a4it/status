/**
 * Public Incidents Page JavaScript
 */

document.addEventListener('DOMContentLoaded', function() {
    loadActiveIncidents();
    loadPastIncidents();

    document.getElementById('days-filter').addEventListener('change', function() {
        loadPastIncidents();
    });
});

async function loadActiveIncidents() {
    try {
        const apps = await api.get('/public/status/apps');

        let allActiveIncidents = [];
        for (const app of apps) {
            const incidents = await api.get(`/public/status/apps/${app.id}/incidents/current`);
            allActiveIncidents = allActiveIncidents.concat(incidents);
        }

        displayActiveIncidents(allActiveIncidents);
    } catch (error) {
        console.error('Failed to load active incidents:', error);
        document.getElementById('active-incidents-loading').innerHTML = '<p class="text-danger text-center">Failed to load incidents.</p>';
    }
}

function displayActiveIncidents(incidents) {
    const loadingEl = document.getElementById('active-incidents-loading');
    const listEl = document.getElementById('active-incidents-list');
    const emptyEl = document.getElementById('active-incidents-empty');

    loadingEl.style.display = 'none';

    if (incidents.length === 0) {
        emptyEl.style.display = 'block';
        return;
    }

    listEl.style.display = 'block';
    listEl.innerHTML = incidents.map(incident => createIncidentCard(incident)).join('');
}

async function loadPastIncidents() {
    const days = document.getElementById('days-filter').value;

    document.getElementById('past-incidents-loading').style.display = 'block';
    document.getElementById('past-incidents-list').style.display = 'none';
    document.getElementById('past-incidents-empty').style.display = 'none';

    try {
        const apps = await api.get('/public/status/apps');

        let allIncidents = [];
        for (const app of apps) {
            const incidents = await api.get(`/public/status/apps/${app.id}/incidents?days=${days}`);
            allIncidents = allIncidents.concat(incidents);
        }

        // Filter out active incidents and sort by date
        const pastIncidents = allIncidents
            .filter(i => i.status === 'RESOLVED')
            .sort((a, b) => new Date(b.startedAt) - new Date(a.startedAt));

        displayPastIncidents(pastIncidents);
    } catch (error) {
        console.error('Failed to load past incidents:', error);
        document.getElementById('past-incidents-loading').innerHTML = '<p class="text-danger text-center">Failed to load incidents.</p>';
    }
}

function displayPastIncidents(incidents) {
    const loadingEl = document.getElementById('past-incidents-loading');
    const listEl = document.getElementById('past-incidents-list');
    const emptyEl = document.getElementById('past-incidents-empty');

    loadingEl.style.display = 'none';

    if (incidents.length === 0) {
        emptyEl.style.display = 'block';
        return;
    }

    listEl.style.display = 'block';
    listEl.innerHTML = incidents.map(incident => createIncidentCard(incident)).join('');
}

function createIncidentCard(incident) {
    const impactBadge = getImpactBadge(incident.impact);
    const statusBadge = getStatusBadge(incident.status);

    return `
        <a href="/incidents/${incident.id}" class="list-group-item list-group-item-action">
            <div class="d-flex justify-content-between align-items-start">
                <div class="flex-grow-1">
                    <div class="d-flex align-items-center gap-2 mb-1">
                        <strong>${escapeHtml(incident.title)}</strong>
                        ${impactBadge}
                        ${statusBadge}
                    </div>
                    <p class="text-muted mb-0 small">
                        Started: ${formatDate(incident.startedAt)}
                        ${incident.resolvedAt ? ` | Resolved: ${formatDate(incident.resolvedAt)}` : ''}
                    </p>
                </div>
            </div>
        </a>
    `;
}

function getImpactBadge(impact) {
    const classes = {
        'MINOR': 'bg-yellow',
        'MAJOR': 'bg-orange',
        'CRITICAL': 'bg-red'
    };
    return `<span class="badge ${classes[impact] || 'bg-secondary'}">${impact}</span>`;
}

function getStatusBadge(status) {
    const classes = {
        'INVESTIGATING': 'bg-red',
        'IDENTIFIED': 'bg-orange',
        'MONITORING': 'bg-blue',
        'RESOLVED': 'bg-green'
    };
    return `<span class="badge ${classes[status] || 'bg-secondary'}">${status}</span>`;
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
