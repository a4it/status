/**
 * Public Incident Detail Page JavaScript
 */

document.addEventListener('DOMContentLoaded', function() {
    loadIncidentDetails();
});

async function loadIncidentDetails() {
    const incidentId = config.incidentId;

    if (!incidentId) {
        showError('Invalid incident ID');
        return;
    }

    try {
        const [incident, updates] = await Promise.all([
            api.get(`/public/status/incidents/${incidentId}`),
            api.get(`/public/status/incidents/${incidentId}/updates`)
        ]);

        displayIncidentDetails(incident, updates);
    } catch (error) {
        console.error('Failed to load incident:', error);
        showError('Failed to load incident details');
    }
}

function displayIncidentDetails(incident, updates) {
    const loadingEl = document.getElementById('incident-loading');
    const contentEl = document.getElementById('incident-content');
    const titleEl = document.getElementById('incident-title');

    loadingEl.style.display = 'none';
    contentEl.style.display = 'block';

    // Set title
    titleEl.textContent = incident.title;
    document.title = `${incident.title} - ${config.applicationName}`;

    // Set status badge
    const statusBadge = document.getElementById('incident-status-badge');
    statusBadge.textContent = incident.status;
    statusBadge.className = `badge ${getStatusBadgeClass(incident.status)}`;

    // Set impact badge
    const impactBadge = document.getElementById('incident-impact-badge');
    impactBadge.textContent = incident.impact;
    impactBadge.className = `badge ${getImpactBadgeClass(incident.impact)}`;

    // Set dates
    document.getElementById('incident-started').textContent = formatDate(incident.startedAt);

    if (incident.resolvedAt) {
        document.getElementById('incident-resolved-row').style.display = 'block';
        document.getElementById('incident-resolved').textContent = formatDate(incident.resolvedAt);
    }

    // Set affected components
    if (incident.affectedComponents && incident.affectedComponents.length > 0) {
        const componentsEl = document.getElementById('incident-affected-components');
        componentsEl.innerHTML = incident.affectedComponents.map(c =>
            `<span class="badge bg-secondary me-1">${escapeHtml(c.name)}</span>`
        ).join('');
    } else {
        document.getElementById('incident-affected-row').style.display = 'none';
    }

    // Display updates
    displayUpdates(updates);
}

function displayUpdates(updates) {
    const listEl = document.getElementById('incident-updates-list');

    if (!updates || updates.length === 0) {
        listEl.innerHTML = `
            <div class="list-group-item">
                <p class="text-muted mb-0">No updates available.</p>
            </div>
        `;
        return;
    }

    // Sort updates by date (newest first)
    updates.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

    listEl.innerHTML = updates.map(update => `
        <div class="list-group-item">
            <div class="d-flex justify-content-between align-items-start mb-2">
                <span class="badge ${getStatusBadgeClass(update.status)}">${update.status}</span>
                <small class="text-muted">${formatDate(update.createdAt)}</small>
            </div>
            <p class="mb-0">${escapeHtml(update.message)}</p>
        </div>
    `).join('');
}

function showError(message) {
    document.getElementById('incident-loading').style.display = 'none';
    const errorEl = document.getElementById('incident-error');
    errorEl.style.display = 'block';
    errorEl.innerHTML = `<p class="text-danger text-center mb-0">${escapeHtml(message)}</p>`;
}

function getStatusBadgeClass(status) {
    const classes = {
        'INVESTIGATING': 'bg-red',
        'IDENTIFIED': 'bg-orange',
        'MONITORING': 'bg-blue',
        'RESOLVED': 'bg-green'
    };
    return classes[status] || 'bg-secondary';
}

function getImpactBadgeClass(impact) {
    const classes = {
        'MINOR': 'bg-yellow',
        'MAJOR': 'bg-orange',
        'CRITICAL': 'bg-red'
    };
    return classes[impact] || 'bg-secondary';
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
