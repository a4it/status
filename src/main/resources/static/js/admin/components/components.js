let componentModal;
let deleteModal;
let deleteCallback = null;
let platformsCache = [];
let selectedPlatformId = '';

document.addEventListener('DOMContentLoaded', () => {
    if (!auth.requireAuth()) return;

    componentModal = new bootstrap.Modal(document.getElementById('componentModal'));
    deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));

    document.getElementById('confirmDeleteBtn').addEventListener('click', () => {
        if (deleteCallback) {
            deleteCallback();
            deleteModal.hide();
        }
    });

    updateUserInfo();
    loadPlatforms();
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
        platformsCache = response.content || response;
        updatePlatformDropdowns();
        loadComponents();
    } catch (error) {
        console.error('Failed to load platforms:', error);
        showError('Failed to load platforms');
    }
}

function updatePlatformDropdowns() {
    const filterSelect = document.getElementById('platformFilter');
    const formSelect = document.getElementById('componentAppId');

    const optionsHtml = platformsCache.map(p =>
        `<option value="${p.id}">${escapeHtml(p.name)}</option>`
    ).join('');

    filterSelect.innerHTML = '<option value="">All Platforms</option>' + optionsHtml;
    formSelect.innerHTML = '<option value="">Select platform...</option>' + optionsHtml;
}

function filterByPlatform() {
    selectedPlatformId = document.getElementById('platformFilter').value;
    loadComponents();
}

async function loadComponents() {
    try {
        let url = '/components?size=100';
        if (selectedPlatformId) {
            url = `/components/app/${selectedPlatformId}`;
        }
        const response = await API.get(url);
        const components = response.content || response;
        displayComponents(components);
    } catch (error) {
        console.error('Failed to load components:', error);
        showError('Failed to load components');
    }
}

function displayComponents(components) {
    const tbody = document.getElementById('componentsTable');

    if (!components || components.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">No components found</td></tr>';
        return;
    }

    tbody.innerHTML = components.map(component => {
        const platform = platformsCache.find(p => p.id === component.appId) || {};
        return `
        <tr>
            <td>
                <div class="font-weight-medium">${escapeHtml(component.name)}</div>
                <div class="text-muted small">${escapeHtml(component.description || '')}</div>
            </td>
            <td>${escapeHtml(platform.name || 'Unknown')}</td>
            <td>${escapeHtml(component.groupName || '-')}</td>
            <td>
                <span class="badge bg-${getStatusColor(component.status)}">
                    ${formatStatus(component.status)}
                </span>
            </td>
            <td>
                ${getComponentHealthCheckStatus(component, platform)}
            </td>
            <td>${component.position}</td>
            <td>
                <button class="btn btn-sm btn-primary me-1" onclick="editComponent('${component.id}')" title="Edit">
                    <i class="ti ti-edit"></i>
                </button>
                <button class="btn btn-sm btn-danger" onclick="confirmDeleteComponent('${component.id}', '${escapeHtml(component.name)}')" title="Delete">
                    <i class="ti ti-trash"></i>
                </button>
            </td>
        </tr>
    `;
    }).join('');
}

function getComponentHealthCheckStatus(component, platform) {
    // If inheriting from app
    if (component.checkInheritFromApp) {
        if (!platform.checkEnabled || platform.checkType === 'NONE') {
            return '<span class="badge bg-secondary"><i class="ti ti-minus"></i> Inherited (Disabled)</span>';
        }
        return '<span class="badge bg-azure"><i class="ti ti-arrow-down"></i> Inherited</span>';
    }

    // Component's own check
    if (!component.checkEnabled || component.checkType === 'NONE') {
        return '<span class="badge bg-secondary"><i class="ti ti-minus"></i> Disabled</span>';
    }

    if (component.lastCheckSuccess === null) {
        return '<span class="badge bg-azure"><i class="ti ti-clock"></i> Pending</span>';
    }

    if (component.lastCheckSuccess) {
        const timeAgo = component.lastCheckAt ? formatTimeAgo(component.lastCheckAt) : '';
        return `<span class="badge bg-green" title="${escapeHtml(component.lastCheckMessage || '')}"><i class="ti ti-check"></i> OK ${timeAgo}</span>`;
    } else {
        const failures = component.consecutiveFailures || 0;
        return `<span class="badge bg-red" title="${escapeHtml(component.lastCheckMessage || '')}"><i class="ti ti-x"></i> Failed (${failures})</span>`;
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

function toggleComponentCheckInherit() {
    const inheritFromApp = document.getElementById('componentCheckInheritFromApp').checked;
    const healthCheckFields = document.getElementById('componentHealthCheckFields');
    healthCheckFields.style.display = inheritFromApp ? 'none' : '';
}

function toggleComponentCheckFields() {
    const checkType = document.getElementById('componentCheckType').value;
    const httpStatusRow = document.getElementById('componentHttpStatusRow');
    const checkUrlHelp = document.getElementById('componentCheckUrlHelp');

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

function openAddComponentModal() {
    document.getElementById('componentForm').reset();
    document.getElementById('componentId').value = '';
    document.getElementById('componentPosition').value = '0';

    if (selectedPlatformId) {
        document.getElementById('componentAppId').value = selectedPlatformId;
    }

    // Reset health check fields
    document.getElementById('componentCheckInheritFromApp').checked = true;
    document.getElementById('componentCheckEnabled').checked = false;
    document.getElementById('componentCheckType').value = 'NONE';
    document.getElementById('componentCheckUrl').value = '';
    document.getElementById('componentCheckInterval').value = '60';
    document.getElementById('componentCheckTimeout').value = '10';
    document.getElementById('componentCheckExpectedStatus').value = '200';
    document.getElementById('componentCheckFailureThreshold').value = '3';
    toggleComponentCheckInherit();
    toggleComponentCheckFields();

    // Hide API key section for new components
    document.getElementById('apiKeySection').style.display = 'none';

    document.querySelector('#componentModal .modal-title').textContent = 'Add Component';
    componentModal.show();
}

async function editComponent(id) {
    try {
        const component = await API.get(`/components/${id}`);

        document.getElementById('componentId').value = component.id;
        document.getElementById('componentAppId').value = component.appId || '';
        document.getElementById('componentName').value = component.name;
        document.getElementById('componentDescription').value = component.description || '';
        document.getElementById('componentGroupName').value = component.groupName || '';
        document.getElementById('componentPosition').value = component.position || 0;
        document.getElementById('componentStatus').value = component.status;

        // Health check fields
        document.getElementById('componentCheckInheritFromApp').checked = component.checkInheritFromApp !== false;
        document.getElementById('componentCheckEnabled').checked = component.checkEnabled || false;
        document.getElementById('componentCheckType').value = component.checkType || 'NONE';
        document.getElementById('componentCheckUrl').value = component.checkUrl || '';
        document.getElementById('componentCheckInterval').value = component.checkIntervalSeconds || 60;
        document.getElementById('componentCheckTimeout').value = component.checkTimeoutSeconds || 10;
        document.getElementById('componentCheckExpectedStatus').value = component.checkExpectedStatus || 200;
        document.getElementById('componentCheckFailureThreshold').value = component.checkFailureThreshold || 3;
        toggleComponentCheckInherit();
        toggleComponentCheckFields();

        // Expand health check section if not inheriting
        if (!component.checkInheritFromApp) {
            const healthCheckSection = document.getElementById('componentHealthCheckSection');
            if (healthCheckSection) {
                healthCheckSection.classList.add('show');
            }
        }

        // Show API key section and set the value
        document.getElementById('apiKeySection').style.display = '';
        document.getElementById('componentApiKey').value = component.apiKey || '';

        document.querySelector('#componentModal .modal-title').textContent = 'Edit Component';
        componentModal.show();
    } catch (error) {
        console.error('Failed to load component:', error);
        showError('Failed to load component details');
    }
}

async function saveComponent() {
    const id = document.getElementById('componentId').value;
    const component = {
        appId: document.getElementById('componentAppId').value,
        name: document.getElementById('componentName').value,
        description: document.getElementById('componentDescription').value,
        groupName: document.getElementById('componentGroupName').value,
        position: parseInt(document.getElementById('componentPosition').value) || 0,
        status: document.getElementById('componentStatus').value,
        // Health check configuration
        checkInheritFromApp: document.getElementById('componentCheckInheritFromApp').checked,
        checkEnabled: document.getElementById('componentCheckEnabled').checked,
        checkType: document.getElementById('componentCheckType').value,
        checkUrl: document.getElementById('componentCheckUrl').value,
        checkIntervalSeconds: parseInt(document.getElementById('componentCheckInterval').value) || 60,
        checkTimeoutSeconds: parseInt(document.getElementById('componentCheckTimeout').value) || 10,
        checkExpectedStatus: parseInt(document.getElementById('componentCheckExpectedStatus').value) || 200,
        checkFailureThreshold: parseInt(document.getElementById('componentCheckFailureThreshold').value) || 3
    };

    if (!component.name || !component.appId) {
        showError('Name and platform are required');
        return;
    }

    // Validate health check config if not inheriting and enabled
    if (!component.checkInheritFromApp && component.checkEnabled && component.checkType !== 'NONE' && !component.checkUrl) {
        showError('Check URL is required when health checking is enabled');
        return;
    }

    try {
        if (id) {
            await API.put(`/components/${id}`, component);
        } else {
            await API.post('/components', component);
        }
        componentModal.hide();
        loadComponents();
        showSuccess(id ? 'Component updated successfully' : 'Component created successfully');
    } catch (error) {
        console.error('Error saving component:', error);
        showError(error.message || 'Failed to save component');
    }
}

function confirmDeleteComponent(id, name) {
    document.getElementById('deleteMessage').textContent =
        `Do you really want to delete "${name}"? This action cannot be undone.`;
    deleteCallback = () => deleteComponent(id);
    deleteModal.show();
}

async function deleteComponent(id) {
    try {
        await API.delete(`/components/${id}`);
        loadComponents();
        showSuccess('Component deleted successfully');
    } catch (error) {
        console.error('Failed to delete component:', error);
        showError('Failed to delete component');
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

function copyApiKey() {
    const apiKeyInput = document.getElementById('componentApiKey');
    navigator.clipboard.writeText(apiKeyInput.value).then(() => {
        showSuccess('API key copied to clipboard');
    }).catch(() => {
        // Fallback for older browsers
        apiKeyInput.select();
        document.execCommand('copy');
        showSuccess('API key copied to clipboard');
    });
}

async function regenerateApiKey() {
    const id = document.getElementById('componentId').value;
    if (!id) {
        showError('Cannot regenerate API key for a new component');
        return;
    }

    if (!confirm('Are you sure you want to regenerate the API key? Any systems using the current key will stop working.')) {
        return;
    }

    try {
        const response = await API.post(`/events/regenerate-key/component/${id}`);
        document.getElementById('componentApiKey').value = response.apiKey || '';
        showSuccess('API key regenerated successfully');
    } catch (error) {
        console.error('Failed to regenerate API key:', error);
        showError('Failed to regenerate API key');
    }
}
