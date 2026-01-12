let platformModal;
let deleteModal;
let deleteCallback = null;

document.addEventListener('DOMContentLoaded', () => {
    if (!auth.requireAuth()) return;

    // Initialize all dropdowns
    document.querySelectorAll('[data-bs-toggle="dropdown"]').forEach(el => {
        new bootstrap.Dropdown(el);
    });

    platformModal = new bootstrap.Modal(document.getElementById('platformModal'));
    deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));

    document.getElementById('confirmDeleteBtn').addEventListener('click', () => {
        if (deleteCallback) {
            deleteCallback();
            deleteModal.hide();
        }
    });

    updateUserInfo();
    loadPlatforms();
    setInterval(loadPlatforms, 60000);
});

function updateUserInfo() {
    const userDisplay = document.querySelector('.avatar + div > div');
    if (userDisplay) {
        userDisplay.textContent = auth.getUserDisplayName();
    }
}

async function loadPlatforms() {
    try {
        const response = await API.get('/status-apps?size=100');
        const platforms = response.content || response;
        displayPlatforms(platforms);
    } catch (error) {
        console.error('Failed to load platforms:', error);
        showError('Failed to load platforms');
    }
}

function displayPlatforms(platforms) {
    const tbody = document.getElementById('platformsTable');

    if (!platforms || platforms.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">No platforms configured</td></tr>';
        return;
    }

    tbody.innerHTML = platforms.map(platform => `
        <tr>
            <td>
                <div class="font-weight-medium">${escapeHtml(platform.name)}</div>
            </td>
            <td><code>${escapeHtml(platform.slug)}</code></td>
            <td class="text-muted">${escapeHtml(platform.description || '-')}</td>
            <td>
                <span class="badge bg-${getStatusColor(platform.status)}">
                    ${formatStatus(platform.status)}
                </span>
            </td>
            <td>
                ${getHealthCheckStatus(platform)}
            </td>
            <td>
                <span class="badge bg-${platform.isPublic ? 'green' : 'secondary'}">
                    ${platform.isPublic ? 'Yes' : 'No'}
                </span>
            </td>
            <td>
                <button class="btn btn-sm btn-primary me-1" onclick="editPlatform('${platform.id}')">
                    <i class="ti ti-edit"></i>
                </button>
                <button class="btn btn-sm btn-danger" onclick="confirmDeletePlatform('${platform.id}', '${escapeHtml(platform.name)}')">
                    <i class="ti ti-trash"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

function getHealthCheckStatus(platform) {
    if (!platform.checkEnabled || platform.checkType === 'NONE') {
        return '<span class="badge bg-secondary"><i class="ti ti-minus"></i> Disabled</span>';
    }

    if (platform.lastCheckSuccess === null) {
        return '<span class="badge bg-azure"><i class="ti ti-clock"></i> Pending</span>';
    }

    if (platform.lastCheckSuccess) {
        const timeAgo = platform.lastCheckAt ? formatTimeAgo(platform.lastCheckAt) : '';
        return `<span class="badge bg-green" title="${escapeHtml(platform.lastCheckMessage || '')}"><i class="ti ti-check"></i> OK ${timeAgo}</span>`;
    } else {
        const failures = platform.consecutiveFailures || 0;
        return `<span class="badge bg-red" title="${escapeHtml(platform.lastCheckMessage || '')}"><i class="ti ti-x"></i> Failed (${failures})</span>`;
    }
}

function formatTimeAgo(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now - date;
    const diffSecs = Math.floor(diffMs / 1000);

    if (diffSecs < 60) return `${diffSecs}s ago`;
    if (diffSecs < 3600) return `${Math.floor(diffSecs / 60)}m ago`;
    if (diffSecs < 86400) return `${Math.floor(diffSecs / 3600)}h ago`;
    return `${Math.floor(diffSecs / 86400)}d ago`;
}

function openAddPlatformModal() {
    document.getElementById('platformForm').reset();
    document.getElementById('platformId').value = '';
    document.getElementById('platformIsPublic').checked = true;
    // Reset health check fields
    document.getElementById('platformCheckEnabled').checked = false;
    document.getElementById('platformCheckType').value = 'NONE';
    document.getElementById('platformCheckUrl').value = '';
    document.getElementById('platformCheckInterval').value = '60';
    document.getElementById('platformCheckTimeout').value = '10';
    document.getElementById('platformCheckExpectedStatus').value = '200';
    document.getElementById('platformCheckFailureThreshold').value = '3';
    toggleCheckFields();
    document.querySelector('#platformModal .modal-title').textContent = 'Add Platform';
    platformModal.show();
}

function toggleCheckFields() {
    const checkType = document.getElementById('platformCheckType').value;
    const httpStatusRow = document.getElementById('httpStatusRow');
    const checkUrlHelp = document.getElementById('checkUrlHelp');

    // Show/hide HTTP status field based on check type
    if (checkType === 'HTTP_GET') {
        httpStatusRow.style.display = '';
        checkUrlHelp.textContent = 'Enter full URL (e.g., https://example.com/api/health)';
    } else if (checkType === 'SPRING_BOOT_HEALTH') {
        httpStatusRow.style.display = 'none';
        checkUrlHelp.textContent = 'Enter base URL (e.g., https://example.com). /actuator/health will be appended automatically.';
    } else if (checkType === 'PING') {
        httpStatusRow.style.display = 'none';
        checkUrlHelp.textContent = 'Enter hostname (e.g., example.com)';
    } else if (checkType === 'TCP_PORT') {
        httpStatusRow.style.display = 'none';
        checkUrlHelp.textContent = 'Enter host:port (e.g., example.com:5432)';
    } else {
        httpStatusRow.style.display = 'none';
        checkUrlHelp.textContent = 'Enter URL for HTTP/Health checks, hostname for Ping, host:port for TCP';
    }
}

async function editPlatform(id) {
    try {
        const platform = await API.get(`/status-apps/${id}`);

        document.getElementById('platformId').value = platform.id;
        document.getElementById('platformName').value = platform.name;
        document.getElementById('platformSlug').value = platform.slug;
        document.getElementById('platformDescription').value = platform.description || '';
        document.getElementById('platformStatus').value = platform.status;
        document.getElementById('platformIsPublic').checked = platform.isPublic;

        // Health check fields
        document.getElementById('platformCheckEnabled').checked = platform.checkEnabled || false;
        document.getElementById('platformCheckType').value = platform.checkType || 'NONE';
        document.getElementById('platformCheckUrl').value = platform.checkUrl || '';
        document.getElementById('platformCheckInterval').value = platform.checkIntervalSeconds || 60;
        document.getElementById('platformCheckTimeout').value = platform.checkTimeoutSeconds || 10;
        document.getElementById('platformCheckExpectedStatus').value = platform.checkExpectedStatus || 200;
        document.getElementById('platformCheckFailureThreshold').value = platform.checkFailureThreshold || 3;
        toggleCheckFields();

        // Expand health check section if enabled
        if (platform.checkEnabled) {
            const healthCheckSection = document.getElementById('healthCheckSection');
            if (healthCheckSection) {
                healthCheckSection.classList.add('show');
            }
        }

        document.querySelector('#platformModal .modal-title').textContent = 'Edit Platform';
        platformModal.show();
    } catch (error) {
        console.error('Failed to load platform:', error);
        showError('Failed to load platform details');
    }
}

async function savePlatform() {
    const id = document.getElementById('platformId').value;
    const platform = {
        name: document.getElementById('platformName').value,
        slug: document.getElementById('platformSlug').value,
        description: document.getElementById('platformDescription').value,
        status: document.getElementById('platformStatus').value,
        isPublic: document.getElementById('platformIsPublic').checked,
        // Health check configuration
        checkEnabled: document.getElementById('platformCheckEnabled').checked,
        checkType: document.getElementById('platformCheckType').value,
        checkUrl: document.getElementById('platformCheckUrl').value,
        checkIntervalSeconds: parseInt(document.getElementById('platformCheckInterval').value) || 60,
        checkTimeoutSeconds: parseInt(document.getElementById('platformCheckTimeout').value) || 10,
        checkExpectedStatus: parseInt(document.getElementById('platformCheckExpectedStatus').value) || 200,
        checkFailureThreshold: parseInt(document.getElementById('platformCheckFailureThreshold').value) || 3
    };

    if (!platform.name || !platform.slug) {
        showError('Name and slug are required');
        return;
    }

    // Validate health check config if enabled
    if (platform.checkEnabled && platform.checkType !== 'NONE' && !platform.checkUrl) {
        showError('Check URL is required when health checking is enabled');
        return;
    }

    try {
        if (id) {
            await API.put(`/status-apps/${id}`, platform);
        } else {
            await API.post('/status-apps', platform);
        }
        platformModal.hide();
        loadPlatforms();
        showSuccess(id ? 'Platform updated successfully' : 'Platform created successfully');
    } catch (error) {
        console.error('Error saving platform:', error);
        showError(error.message || 'Failed to save platform');
    }
}

function confirmDeletePlatform(id, name) {
    document.getElementById('deleteMessage').textContent =
        `Do you really want to delete "${name}"? This action cannot be undone.`;
    deleteCallback = () => deletePlatform(id);
    deleteModal.show();
}

async function deletePlatform(id) {
    try {
        await API.delete(`/status-apps/${id}`);
        loadPlatforms();
        showSuccess('Platform deleted successfully');
    } catch (error) {
        console.error('Failed to delete platform:', error);
        showError('Failed to delete platform');
    }
}

function getStatusColor(status) {
    const colors = {
        'OPERATIONAL': 'green',
        'DEGRADED_PERFORMANCE': 'yellow',
        'PARTIAL_OUTAGE': 'orange',
        'MAJOR_OUTAGE': 'red',
        'UNDER_MAINTENANCE': 'blue'
    };
    return colors[status] || 'secondary';
}

function formatStatus(status) {
    const labels = {
        'OPERATIONAL': 'Operational',
        'DEGRADED_PERFORMANCE': 'Degraded',
        'PARTIAL_OUTAGE': 'Partial Outage',
        'MAJOR_OUTAGE': 'Major Outage',
        'UNDER_MAINTENANCE': 'Maintenance'
    };
    return labels[status] || status;
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showSuccess(message) {
    showToast(message, 'success');
}

function showError(message) {
    showToast(message, 'danger');
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container') || createToastContainer();
    const toast = document.createElement('div');
    toast.className = `toast align-items-center text-white bg-${type} border-0`;
    toast.setAttribute('role', 'alert');
    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">${escapeHtml(message)}</div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>
    `;
    container.appendChild(toast);
    const bsToast = new bootstrap.Toast(toast, { autohide: true, delay: 3000 });
    bsToast.show();
    toast.addEventListener('hidden.bs.toast', () => toast.remove());
}

function createToastContainer() {
    const container = document.createElement('div');
    container.id = 'toast-container';
    container.className = 'toast-container position-fixed top-0 end-0 p-3';
    container.style.zIndex = '1100';
    document.body.appendChild(container);
    return container;
}
