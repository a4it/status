/**
 * Public Maintenance Page JavaScript
 */

document.addEventListener('DOMContentLoaded', function() {
    loadUpcomingMaintenance();
    loadPastMaintenance();
});

async function loadUpcomingMaintenance() {
    try {
        const apps = await api.get('/public/status/apps');

        let allMaintenance = [];
        for (const app of apps) {
            const maintenance = await api.get(`/public/status/apps/${app.id}/maintenance?type=upcoming`);
            allMaintenance = allMaintenance.concat(maintenance);
        }

        // Sort by scheduled start
        allMaintenance.sort((a, b) => new Date(a.scheduledStart) - new Date(b.scheduledStart));

        displayUpcomingMaintenance(allMaintenance);
    } catch (error) {
        console.error('Failed to load upcoming maintenance:', error);
        document.getElementById('upcoming-maintenance-loading').innerHTML = '<p class="text-danger text-center">Failed to load maintenance.</p>';
    }
}

function displayUpcomingMaintenance(maintenances) {
    const loadingEl = document.getElementById('upcoming-maintenance-loading');
    const listEl = document.getElementById('upcoming-maintenance-list');
    const emptyEl = document.getElementById('upcoming-maintenance-empty');

    loadingEl.style.display = 'none';

    if (maintenances.length === 0) {
        emptyEl.style.display = 'block';
        return;
    }

    listEl.style.display = 'block';
    listEl.innerHTML = maintenances.map(m => createMaintenanceCard(m)).join('');
}

async function loadPastMaintenance() {
    try {
        const apps = await api.get('/public/status/apps');

        let allMaintenance = [];
        for (const app of apps) {
            const maintenance = await api.get(`/public/status/apps/${app.id}/maintenance?type=completed`);
            allMaintenance = allMaintenance.concat(maintenance);
        }

        // Sort by scheduled start (newest first)
        allMaintenance.sort((a, b) => new Date(b.scheduledStart) - new Date(a.scheduledStart));

        displayPastMaintenance(allMaintenance);
    } catch (error) {
        console.error('Failed to load past maintenance:', error);
        document.getElementById('past-maintenance-loading').innerHTML = '<p class="text-danger text-center">Failed to load maintenance.</p>';
    }
}

function displayPastMaintenance(maintenances) {
    const loadingEl = document.getElementById('past-maintenance-loading');
    const listEl = document.getElementById('past-maintenance-list');
    const emptyEl = document.getElementById('past-maintenance-empty');

    loadingEl.style.display = 'none';

    if (maintenances.length === 0) {
        emptyEl.style.display = 'block';
        return;
    }

    listEl.style.display = 'block';
    listEl.innerHTML = maintenances.map(m => createMaintenanceCard(m, true)).join('');
}

function createMaintenanceCard(maintenance, isPast = false) {
    const statusBadge = getMaintenanceStatusBadge(maintenance.status);

    return `
        <div class="list-group-item">
            <div class="d-flex justify-content-between align-items-start">
                <div class="flex-grow-1">
                    <div class="d-flex align-items-center gap-2 mb-1">
                        <strong>${escapeHtml(maintenance.title)}</strong>
                        ${statusBadge}
                    </div>
                    ${maintenance.description ? `<p class="text-muted mb-2">${escapeHtml(maintenance.description)}</p>` : ''}
                    <p class="mb-0 small">
                        <strong>Scheduled:</strong> ${formatDate(maintenance.scheduledStart)} - ${formatDate(maintenance.scheduledEnd)}
                    </p>
                    ${maintenance.affectedComponents && maintenance.affectedComponents.length > 0 ? `
                        <p class="mb-0 small mt-1">
                            <strong>Affected:</strong> ${maintenance.affectedComponents.map(c => escapeHtml(c.name)).join(', ')}
                        </p>
                    ` : ''}
                </div>
            </div>
        </div>
    `;
}

function getMaintenanceStatusBadge(status) {
    const classes = {
        'SCHEDULED': 'bg-blue',
        'IN_PROGRESS': 'bg-yellow',
        'COMPLETED': 'bg-green',
        'CANCELLED': 'bg-secondary'
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
