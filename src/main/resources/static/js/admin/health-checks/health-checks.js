/**
 * Health Checks Admin Page JavaScript
 * Handles global settings management, health check status display, and manual triggers.
 */

let triggerResultModal;
let platforms = [];
let healthCheckData = [];
let refreshInterval;

document.addEventListener('DOMContentLoaded', () => {
    if (!auth.requireAuth()) return;

    // Initialize Bootstrap modal
    triggerResultModal = new bootstrap.Modal(document.getElementById('triggerResultModal'));

    // Update user info in navbar
    updateUserInfo();

    // Load initial data
    loadSettings();
    loadPlatforms();
    loadHealthCheckStatus();

    // Set up auto-refresh every 10 seconds
    refreshInterval = setInterval(loadHealthCheckStatus, 10000);
});

/**
 * Updates user info in the navbar.
 */
function updateUserInfo() {
    const user = auth.getUser();
    if (user) {
        const avatar = document.querySelector('.avatar');
        const nameDiv = document.querySelector('.navbar-nav .d-none.d-xl-block div');
        if (avatar) {
            avatar.textContent = (user.username || user.email || 'U').charAt(0).toUpperCase();
        }
        if (nameDiv) {
            nameDiv.textContent = user.username || user.email || 'User';
        }
    }
}

/**
 * Loads global health check settings.
 */
async function loadSettings() {
    try {
        const settings = await API.get('/health-checks/settings');
        document.getElementById('settingsEnabled').checked = settings.enabled;
        document.getElementById('settingsSchedulerInterval').value = settings.schedulerIntervalMs || 10000;
        document.getElementById('settingsThreadPoolSize').value = settings.threadPoolSize || 10;
        document.getElementById('settingsDefaultInterval').value = settings.defaultIntervalSeconds || 60;
        document.getElementById('settingsDefaultTimeout').value = settings.defaultTimeoutSeconds || 10;
    } catch (error) {
        console.error('Failed to load settings:', error);
        showError('Failed to load health check settings');
    }
}

/**
 * Saves global health check settings.
 */
async function saveSettings() {
    const settings = {
        enabled: document.getElementById('settingsEnabled').checked,
        schedulerIntervalMs: parseInt(document.getElementById('settingsSchedulerInterval').value) || 10000,
        threadPoolSize: parseInt(document.getElementById('settingsThreadPoolSize').value) || 10,
        defaultIntervalSeconds: parseInt(document.getElementById('settingsDefaultInterval').value) || 60,
        defaultTimeoutSeconds: parseInt(document.getElementById('settingsDefaultTimeout').value) || 10
    };

    try {
        await API.put('/health-checks/settings', settings);
        showSuccess('Settings saved successfully');
    } catch (error) {
        console.error('Failed to save settings:', error);
        showError('Failed to save settings: ' + (error.message || 'Unknown error'));
    }
}

/**
 * Loads available platforms for filtering.
 */
async function loadPlatforms() {
    try {
        const response = await API.get('/status-platforms?size=100');
        platforms = response.content || response || [];

        const select = document.getElementById('filterPlatform');
        select.innerHTML = '<option value="">All Platforms</option>';

        platforms.forEach(platform => {
            const option = document.createElement('option');
            option.value = platform.id;
            option.textContent = platform.name;
            select.appendChild(option);
        });
    } catch (error) {
        console.error('Failed to load platforms:', error);
    }
}

/**
 * Loads health check status for all entities.
 */
async function loadHealthCheckStatus() {
    try {
        const params = new URLSearchParams();

        const platformId = document.getElementById('filterPlatform').value;
        const status = document.getElementById('filterStatus').value;
        const enabled = document.getElementById('filterEnabled').value;

        if (platformId) params.append('platformId', platformId);
        if (status) params.append('status', status);
        if (enabled) params.append('checkEnabled', enabled);

        const url = '/health-checks/status' + (params.toString() ? '?' + params.toString() : '');
        healthCheckData = await API.get(url);

        displayHealthCheckStatus(healthCheckData);
    } catch (error) {
        console.error('Failed to load health check status:', error);
        const tbody = document.getElementById('statusTable');
        tbody.innerHTML = '<tr><td colspan="8" class="text-center text-danger">Failed to load data</td></tr>';
    }
}

/**
 * Displays health check status in the table.
 */
function displayHealthCheckStatus(data) {
    const tbody = document.getElementById('statusTable');

    if (!data || data.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted">No health check configurations found</td></tr>';
        return;
    }

    tbody.innerHTML = data.map(entity => {
        const lastCheckDisplay = getLastCheckDisplay(entity);
        const statusBadge = getStatusBadge(entity.status);
        const checkBadge = getCheckResultBadge(entity);

        return `
            <tr>
                <td>
                    <div class="font-weight-medium">${escapeHtml(entity.name)}</div>
                    ${entity.checkUrl ? `<div class="text-muted small">${escapeHtml(truncateUrl(entity.checkUrl))}</div>` : ''}
                </td>
                <td>
                    <span class="badge bg-${entity.entityType === 'APP' ? 'blue' : 'cyan'}">${entity.entityType}</span>
                </td>
                <td>${entity.platformName ? escapeHtml(entity.platformName) : '<span class="text-muted">-</span>'}</td>
                <td>
                    ${entity.checkEnabled ?
                        `<span class="badge bg-secondary">${formatCheckType(entity.checkType)}</span>` :
                        '<span class="badge bg-secondary">Disabled</span>'}
                </td>
                <td>${entity.checkEnabled && entity.checkIntervalSeconds ? entity.checkIntervalSeconds + 's' : '-'}</td>
                <td>${lastCheckDisplay}</td>
                <td>${statusBadge}</td>
                <td>
                    ${entity.checkEnabled && entity.checkType && entity.checkType !== 'NONE' ?
                        `<button class="btn btn-sm btn-outline-primary me-1" onclick="triggerCheck('${entity.entityType}', '${entity.entityId}')" title="Run Check">
                            <i class="ti ti-player-play"></i>
                        </button>` : ''}
                    <a href="/admin/${entity.entityType === 'APP' ? 'platforms' : 'components'}" class="btn btn-sm btn-outline-secondary" title="Edit">
                        <i class="ti ti-edit"></i>
                    </a>
                </td>
            </tr>
        `;
    }).join('');
}

/**
 * Gets the last check display with timing.
 */
function getLastCheckDisplay(entity) {
    if (!entity.lastCheckAt) {
        return '<span class="text-muted">Never</span>';
    }

    const checkTime = new Date(entity.lastCheckAt);
    const timeAgo = getTimeAgo(checkTime);
    const badge = getCheckResultBadge(entity);

    return `${badge} <small class="text-muted">${timeAgo}</small>`;
}

/**
 * Gets the check result badge.
 */
function getCheckResultBadge(entity) {
    if (entity.lastCheckSuccess === null || entity.lastCheckSuccess === undefined) {
        return '<span class="badge bg-secondary"><i class="ti ti-clock"></i> Pending</span>';
    }

    if (entity.lastCheckSuccess) {
        return '<span class="badge bg-green"><i class="ti ti-check"></i> OK</span>';
    } else {
        const failures = entity.consecutiveFailures || 0;
        return `<span class="badge bg-red"><i class="ti ti-x"></i> Failed (${failures})</span>`;
    }
}

/**
 * Gets the status badge.
 */
function getStatusBadge(status) {
    const colors = {
        'OPERATIONAL': 'green',
        'DEGRADED_PERFORMANCE': 'yellow',
        'PARTIAL_OUTAGE': 'orange',
        'MAJOR_OUTAGE': 'red',
        'UNDER_MAINTENANCE': 'blue'
    };
    const color = colors[status] || 'secondary';
    return `<span class="badge bg-${color}">${formatStatus(status)}</span>`;
}

/**
 * Formats status for display.
 */
function formatStatus(status) {
    if (!status) return 'Unknown';
    return status.split('_').map(word =>
        word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
    ).join(' ');
}

/**
 * Formats check type for display.
 */
function formatCheckType(type) {
    if (!type || type === 'NONE') return 'None';
    const types = {
        'PING': 'Ping',
        'HTTP_GET': 'HTTP GET',
        'SPRING_BOOT_HEALTH': 'Spring Health',
        'TCP_PORT': 'TCP Port'
    };
    return types[type] || type;
}

/**
 * Truncates a URL for display.
 */
function truncateUrl(url) {
    if (!url) return '';
    return url.length > 50 ? url.substring(0, 47) + '...' : url;
}

/**
 * Gets time ago string.
 */
function getTimeAgo(date) {
    const now = new Date();
    const diff = Math.floor((now - date) / 1000);

    if (diff < 60) return 'just now';
    if (diff < 3600) return Math.floor(diff / 60) + 'm ago';
    if (diff < 86400) return Math.floor(diff / 3600) + 'h ago';
    return Math.floor(diff / 86400) + 'd ago';
}

/**
 * Filter status table.
 */
function filterStatus() {
    loadHealthCheckStatus();
}

/**
 * Triggers all health checks.
 */
async function triggerAllChecks() {
    const btn = document.getElementById('triggerAllBtn');
    btn.disabled = true;
    btn.innerHTML = '<i class="ti ti-loader"></i> Running...';

    try {
        const result = await API.post('/health-checks/trigger/all');
        showTriggerResult(result.success, result.message, result.durationMs);

        // Refresh status after a short delay
        setTimeout(loadHealthCheckStatus, 1000);
    } catch (error) {
        console.error('Failed to trigger all checks:', error);
        showTriggerResult(false, 'Failed to trigger checks: ' + (error.message || 'Unknown error'));
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="ti ti-player-play"></i> Run All Checks Now';
    }
}

/**
 * Triggers a single health check.
 */
async function triggerCheck(entityType, entityId) {
    const endpoint = entityType === 'APP' ?
        `/health-checks/trigger/app/${entityId}` :
        `/health-checks/trigger/component/${entityId}`;

    try {
        const result = await API.post(endpoint);
        showTriggerResult(result.success, result.message, result.durationMs);

        // Refresh status after a short delay
        setTimeout(loadHealthCheckStatus, 500);
    } catch (error) {
        console.error('Failed to trigger check:', error);
        showTriggerResult(false, 'Failed to trigger check: ' + (error.message || 'Unknown error'));
    }
}

/**
 * Shows trigger result in modal.
 */
function showTriggerResult(success, message, durationMs) {
    const statusDiv = document.getElementById('triggerResultStatus');
    const icon = document.getElementById('triggerResultIcon');
    const title = document.getElementById('triggerResultTitle');
    const messageDiv = document.getElementById('triggerResultMessage');

    if (success) {
        statusDiv.className = 'modal-status bg-success';
        icon.className = 'ti ti-check text-success mb-2';
        title.textContent = 'Check Successful';
    } else {
        statusDiv.className = 'modal-status bg-danger';
        icon.className = 'ti ti-x text-danger mb-2';
        title.textContent = 'Check Failed';
    }

    let displayMessage = message || 'No details available';
    if (durationMs !== undefined && durationMs !== null) {
        displayMessage += ` (${durationMs}ms)`;
    }
    messageDiv.textContent = displayMessage;

    triggerResultModal.show();
}

/**
 * Escapes HTML special characters.
 */
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * Shows a success notification.
 */
function showSuccess(message) {
    showNotification(message, 'success');
}

/**
 * Shows an error notification.
 */
function showError(message) {
    showNotification(message, 'danger');
}

/**
 * Shows a notification toast.
 */
function showNotification(message, type) {
    // Create toast container if it doesn't exist
    let toastContainer = document.querySelector('.toast-container');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
        toastContainer.style.zIndex = '1100';
        document.body.appendChild(toastContainer);
    }

    const toastId = 'toast-' + Date.now();
    const toastHtml = `
        <div id="${toastId}" class="toast align-items-center text-bg-${type} border-0" role="alert">
            <div class="d-flex">
                <div class="toast-body">${escapeHtml(message)}</div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        </div>
    `;
    toastContainer.insertAdjacentHTML('beforeend', toastHtml);

    const toastElement = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastElement, { autohide: true, delay: 5000 });
    toast.show();

    toastElement.addEventListener('hidden.bs.toast', () => {
        toastElement.remove();
    });
}
