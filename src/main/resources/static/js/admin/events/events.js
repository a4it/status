let eventDetailsModal;
let deleteModal;
let deleteCallback = null;
let platformsCache = [];
let componentsCache = [];
let currentPage = 0;
let pageSize = 20;
let totalPages = 0;
let totalElements = 0;

document.addEventListener('DOMContentLoaded', () => {
    if (!auth.requireAuth()) return;

    eventDetailsModal = new bootstrap.Modal(document.getElementById('eventDetailsModal'));
    deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));

    document.getElementById('confirmDeleteBtn').addEventListener('click', () => {
        if (deleteCallback) {
            deleteCallback();
            deleteModal.hide();
        }
    });

    updateUserInfo();
    loadPlatforms();
    loadEvents();
    setInterval(loadEvents, 30000);
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
        updatePlatformDropdown();
    } catch (error) {
        console.error('Failed to load platforms:', error);
    }
}

function updatePlatformDropdown() {
    const select = document.getElementById('filterPlatform');
    select.innerHTML = '<option value="">All Platforms</option>' +
        platformsCache.map(p => `<option value="${p.id}">${escapeHtml(p.name)}</option>`).join('');
}

async function loadComponents(appId) {
    if (!appId) {
        componentsCache = [];
        updateComponentDropdown();
        return;
    }
    try {
        const response = await API.get(`/components?appId=${appId}&size=100`);
        componentsCache = response.content || response;
        updateComponentDropdown();
    } catch (error) {
        console.error('Failed to load components:', error);
    }
}

function updateComponentDropdown() {
    const select = document.getElementById('filterComponent');
    select.innerHTML = '<option value="">All Components</option>' +
        componentsCache.map(c => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join('');
}

async function loadEvents() {
    try {
        const params = buildQueryParams();
        const response = await API.get(`/events?${params}`);

        const events = response.content || [];
        totalElements = response.totalElements || events.length;
        totalPages = response.totalPages || 1;

        displayEvents(events);
        updatePagination();
    } catch (error) {
        console.error('Failed to load events:', error);
        showError('Failed to load events');
    }
}

function buildQueryParams() {
    const params = new URLSearchParams();
    params.append('page', currentPage);
    params.append('size', pageSize);

    const appId = document.getElementById('filterPlatform').value;
    if (appId) {
        params.append('appId', appId);
        if (componentsCache.length === 0) {
            loadComponents(appId);
        }
    }

    const componentId = document.getElementById('filterComponent').value;
    if (componentId) params.append('componentId', componentId);

    const severity = document.getElementById('filterSeverity').value;
    if (severity) params.append('severity', severity);

    const startDate = document.getElementById('filterStartDate').value;
    if (startDate) params.append('startDate', new Date(startDate).toISOString());

    const endDate = document.getElementById('filterEndDate').value;
    if (endDate) params.append('endDate', new Date(endDate).toISOString());

    const search = document.getElementById('searchText').value;
    if (search) params.append('search', search);

    return params.toString();
}

function displayEvents(events) {
    const tbody = document.getElementById('eventsTable');

    if (!events || events.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">No events found</td></tr>';
        return;
    }

    tbody.innerHTML = events.map(event => `
        <tr>
            <td class="text-nowrap">${formatTime(event.eventTime)}</td>
            <td>
                <span class="badge bg-${getSeverityColor(event.severity)}">
                    ${event.severity}
                </span>
            </td>
            <td>${escapeHtml(event.appName || 'Unknown')}</td>
            <td>${escapeHtml(event.componentName || '-')}</td>
            <td>${escapeHtml(event.source || '-')}</td>
            <td>
                <div class="text-truncate" style="max-width: 300px;" title="${escapeHtml(event.message)}">
                    ${escapeHtml(event.message)}
                </div>
            </td>
            <td>
                <button class="btn btn-sm btn-outline-primary me-1" onclick="viewEvent('${event.id}')" title="View Details">
                    <i class="ti ti-eye"></i>
                </button>
                <button class="btn btn-sm btn-danger" onclick="confirmDeleteEvent('${event.id}')" title="Delete">
                    <i class="ti ti-trash"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

function updatePagination() {
    const start = currentPage * pageSize + 1;
    const end = Math.min((currentPage + 1) * pageSize, totalElements);

    document.getElementById('showingStart').textContent = totalElements > 0 ? start : 0;
    document.getElementById('showingEnd').textContent = end;
    document.getElementById('totalItems').textContent = totalElements;

    const pagination = document.getElementById('pagination');
    let html = '';

    // Previous button
    html += `<li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
        <a class="page-link" href="#" onclick="goToPage(${currentPage - 1}); return false;">
            <i class="ti ti-chevron-left"></i>
        </a>
    </li>`;

    // Page numbers
    const maxVisiblePages = 5;
    let startPage = Math.max(0, currentPage - Math.floor(maxVisiblePages / 2));
    let endPage = Math.min(totalPages - 1, startPage + maxVisiblePages - 1);

    if (endPage - startPage < maxVisiblePages - 1) {
        startPage = Math.max(0, endPage - maxVisiblePages + 1);
    }

    if (startPage > 0) {
        html += `<li class="page-item"><a class="page-link" href="#" onclick="goToPage(0); return false;">1</a></li>`;
        if (startPage > 1) {
            html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
        }
    }

    for (let i = startPage; i <= endPage; i++) {
        html += `<li class="page-item ${i === currentPage ? 'active' : ''}">
            <a class="page-link" href="#" onclick="goToPage(${i}); return false;">${i + 1}</a>
        </li>`;
    }

    if (endPage < totalPages - 1) {
        if (endPage < totalPages - 2) {
            html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
        }
        html += `<li class="page-item"><a class="page-link" href="#" onclick="goToPage(${totalPages - 1}); return false;">${totalPages}</a></li>`;
    }

    // Next button
    html += `<li class="page-item ${currentPage >= totalPages - 1 ? 'disabled' : ''}">
        <a class="page-link" href="#" onclick="goToPage(${currentPage + 1}); return false;">
            <i class="ti ti-chevron-right"></i>
        </a>
    </li>`;

    pagination.innerHTML = html;
}

function goToPage(page) {
    if (page >= 0 && page < totalPages) {
        currentPage = page;
        loadEvents();
    }
}

async function viewEvent(id) {
    try {
        const event = await API.get(`/events/${id}`);
        displayEventDetails(event);
        eventDetailsModal.show();
    } catch (error) {
        console.error('Failed to load event details:', error);
        showError('Failed to load event details');
    }
}

function displayEventDetails(event) {
    const details = document.getElementById('eventDetails');

    details.innerHTML = `
        <div class="mb-3">
            <div class="d-flex justify-content-between align-items-center">
                <span class="badge bg-${getSeverityColor(event.severity)} fs-6">${event.severity}</span>
                <span class="text-muted">${new Date(event.eventTime).toLocaleString()}</span>
            </div>
        </div>
        <div class="row mb-3">
            <div class="col-md-6">
                <strong>Platform:</strong> ${escapeHtml(event.appName || 'Unknown')}
            </div>
            <div class="col-md-6">
                <strong>Component:</strong> ${escapeHtml(event.componentName || 'N/A')}
            </div>
        </div>
        ${event.source ? `
        <div class="mb-3">
            <strong>Source:</strong> ${escapeHtml(event.source)}
        </div>
        ` : ''}
        <div class="mb-3">
            <strong>Message:</strong>
            <div class="mt-1 p-2 bg-light rounded">
                ${escapeHtml(event.message)}
            </div>
        </div>
        ${event.details ? `
        <div class="mb-3">
            <strong>Details:</strong>
            <pre class="mt-1 p-2 bg-light rounded" style="white-space: pre-wrap; word-break: break-all;">${escapeHtml(event.details)}</pre>
        </div>
        ` : ''}
        <div class="text-muted small">
            <strong>Logged at:</strong> ${new Date(event.createdDate).toLocaleString()}
        </div>
    `;
}

function confirmDeleteEvent(id) {
    deleteCallback = () => deleteEvent(id);
    deleteModal.show();
}

async function deleteEvent(id) {
    try {
        await API.delete(`/events/${id}`);
        loadEvents();
        showSuccess('Event deleted successfully');
    } catch (error) {
        console.error('Failed to delete event:', error);
        showError('Failed to delete event');
    }
}

function clearFilters() {
    document.getElementById('filterPlatform').value = '';
    document.getElementById('filterComponent').value = '';
    document.getElementById('filterSeverity').value = '';
    document.getElementById('filterStartDate').value = '';
    document.getElementById('filterEndDate').value = '';
    document.getElementById('searchText').value = '';
    componentsCache = [];
    updateComponentDropdown();
    currentPage = 0;
    loadEvents();
}

function handleSearchKeyup(event) {
    if (event.key === 'Enter') {
        currentPage = 0;
        loadEvents();
    }
}

function getSeverityColor(severity) {
    const colors = {
        'DEBUG': 'secondary',
        'INFO': 'info',
        'WARNING': 'warning',
        'ERROR': 'orange',
        'CRITICAL': 'danger'
    };
    return colors[severity] || 'secondary';
}

function formatTime(timestamp) {
    if (!timestamp) return 'Unknown';
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now - date;

    if (diff < 60000) return 'Just now';
    if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`;
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}h ago`;
    return date.toLocaleString();
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
