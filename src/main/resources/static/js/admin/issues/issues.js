let issueModal;
let issueDetailsModal;
let addUpdateModal;
let deleteModal;
let deleteCallback = null;
let currentFilter = 'ALL';
let currentIssueId = null;
let platformsCache = [];

document.addEventListener('DOMContentLoaded', () => {
    if (!auth.requireAuth()) return;

    issueModal = new bootstrap.Modal(document.getElementById('issueModal'));
    issueDetailsModal = new bootstrap.Modal(document.getElementById('issueDetailsModal'));
    addUpdateModal = new bootstrap.Modal(document.getElementById('addUpdateModal'));
    deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));

    document.getElementById('confirmDeleteBtn').addEventListener('click', () => {
        if (deleteCallback) {
            deleteCallback();
            deleteModal.hide();
        }
    });

    document.getElementById('editIssueBtn').addEventListener('click', () => {
        issueDetailsModal.hide();
        editIssue(currentIssueId);
    });

    document.getElementById('resolveIssueBtn').addEventListener('click', () => {
        resolveIssue(currentIssueId);
    });

    updateUserInfo();
    loadPlatforms();
    loadIssues();
    setInterval(loadIssues, 30000);
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
    const select = document.getElementById('issueAppId');
    select.innerHTML = '<option value="">Select platform...</option>' +
        platformsCache.map(p => `<option value="${p.id}">${escapeHtml(p.name)}</option>`).join('');
}

async function loadIssues() {
    try {
        let url = '/incidents?size=100';
        if (currentFilter !== 'ALL') {
            url += `&status=${currentFilter}`;
        }
        const response = await API.get(url);
        const issues = response.content || response;
        displayIssues(issues);
    } catch (error) {
        console.error('Failed to load issues:', error);
        showError('Failed to load issues');
    }
}

function displayIssues(issues) {
    const tbody = document.getElementById('issuesTable');

    if (!issues || issues.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">No issues found</td></tr>';
        return;
    }

    tbody.innerHTML = issues.map(issue => {
        const platform = platformsCache.find(p => p.id === issue.appId) || {};
        return `
        <tr>
            <td>
                <div class="font-weight-medium">${escapeHtml(issue.title)}</div>
                <div class="text-muted small">${escapeHtml(issue.description || '').substring(0, 50)}${issue.description && issue.description.length > 50 ? '...' : ''}</div>
            </td>
            <td>${escapeHtml(platform.name || 'Unknown')}</td>
            <td>
                <span class="badge bg-${getSeverityColor(issue.severity)}">
                    ${issue.severity}
                </span>
            </td>
            <td>
                <span class="badge bg-${getStatusColor(issue.status)}">
                    ${formatStatus(issue.status)}
                </span>
            </td>
            <td>${formatTime(issue.startedAt)}</td>
            <td>
                <button class="btn btn-sm btn-outline-primary me-1" onclick="viewIssue('${issue.id}')" title="View Details">
                    <i class="ti ti-eye"></i>
                </button>
                <button class="btn btn-sm btn-outline-info me-1" onclick="openAddUpdateModal('${issue.id}')" title="Add Update">
                    <i class="ti ti-message-plus"></i>
                </button>
                <button class="btn btn-sm btn-primary me-1" onclick="editIssue('${issue.id}')" title="Edit">
                    <i class="ti ti-edit"></i>
                </button>
                <button class="btn btn-sm btn-danger" onclick="confirmDeleteIssue('${issue.id}', '${escapeHtml(issue.title)}')" title="Delete">
                    <i class="ti ti-trash"></i>
                </button>
            </td>
        </tr>
    `;
    }).join('');
}

function filterIssues(status, btn) {
    currentFilter = status;
    document.querySelectorAll('.btn-group .btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    loadIssues();
}

function openAddIssueModal() {
    document.getElementById('issueForm').reset();
    document.getElementById('issueId').value = '';
    document.getElementById('issueIsPublic').checked = true;
    document.getElementById('issueStartedAt').value = new Date().toISOString().slice(0, 16);
    document.querySelector('#issueModal .modal-title').textContent = 'Report Issue';
    issueModal.show();
}

async function editIssue(id) {
    try {
        const issue = await API.get(`/incidents/${id}`);

        document.getElementById('issueId').value = issue.id;
        document.getElementById('issueTitle').value = issue.title;
        document.getElementById('issueAppId').value = issue.appId || '';
        document.getElementById('issueDescription').value = issue.description || '';
        document.getElementById('issueSeverity').value = issue.severity;
        document.getElementById('issueStatus').value = issue.status;
        document.getElementById('issueImpact').value = issue.impact || '';
        document.getElementById('issueIsPublic').checked = issue.isPublic !== false;

        if (issue.startedAt) {
            const date = new Date(issue.startedAt);
            document.getElementById('issueStartedAt').value = date.toISOString().slice(0, 16);
        }

        document.querySelector('#issueModal .modal-title').textContent = 'Edit Issue';
        issueModal.show();
    } catch (error) {
        console.error('Failed to load issue:', error);
        showError('Failed to load issue details');
    }
}

async function saveIssue() {
    const id = document.getElementById('issueId').value;
    const issue = {
        appId: document.getElementById('issueAppId').value,
        title: document.getElementById('issueTitle').value,
        description: document.getElementById('issueDescription').value,
        severity: document.getElementById('issueSeverity').value,
        status: document.getElementById('issueStatus').value,
        impact: document.getElementById('issueImpact').value,
        startedAt: new Date(document.getElementById('issueStartedAt').value).toISOString(),
        isPublic: document.getElementById('issueIsPublic').checked
    };

    if (!issue.title || !issue.appId || !issue.severity || !issue.status) {
        showError('Please fill in all required fields');
        return;
    }

    try {
        if (id) {
            await API.put(`/incidents/${id}`, issue);
        } else {
            await API.post('/incidents', issue);
        }
        issueModal.hide();
        loadIssues();
        showSuccess(id ? 'Issue updated successfully' : 'Issue created successfully');
    } catch (error) {
        console.error('Error saving issue:', error);
        showError(error.message || 'Failed to save issue');
    }
}

async function viewIssue(id) {
    currentIssueId = id;
    try {
        const issue = await API.get(`/incidents/${id}`);
        displayIssueDetails(issue);
        issueDetailsModal.show();
    } catch (error) {
        console.error('Failed to load issue details:', error);
        showError('Failed to load issue details');
    }
}

function displayIssueDetails(issue) {
    const platform = platformsCache.find(p => p.id === issue.appId) || {};
    const details = document.getElementById('issueDetails');

    const updatesHtml = issue.updates && issue.updates.length > 0
        ? `<div class="mt-3">
            <h4>Updates</h4>
            <div class="timeline">
                ${issue.updates.map(update => `
                    <div class="timeline-item">
                        <div class="timeline-dot bg-primary"></div>
                        <div class="timeline-content">
                            <div class="text-muted small">${formatTime(update.createdAt)}</div>
                            <div>${escapeHtml(update.message)}</div>
                        </div>
                    </div>
                `).join('')}
            </div>
           </div>`
        : '';

    details.innerHTML = `
        <div class="mb-3">
            <h4>${escapeHtml(issue.title)}</h4>
            <div class="mt-2">
                <span class="badge bg-${getSeverityColor(issue.severity)} me-2">${issue.severity}</span>
                <span class="badge bg-${getStatusColor(issue.status)}">${formatStatus(issue.status)}</span>
            </div>
        </div>
        <div class="row mb-3">
            <div class="col-md-6">
                <strong>Platform:</strong> ${escapeHtml(platform.name || 'Unknown')}
            </div>
            <div class="col-md-6">
                <strong>Started:</strong> ${new Date(issue.startedAt).toLocaleString()}
            </div>
        </div>
        ${issue.resolvedAt ? `
        <div class="mb-3">
            <strong>Resolved:</strong> ${new Date(issue.resolvedAt).toLocaleString()}
        </div>
        ` : ''}
        <div class="mb-3">
            <strong>Description:</strong><br>
            ${escapeHtml(issue.description || 'No description available')}
        </div>
        ${issue.impact ? `
        <div class="mb-3">
            <strong>Impact:</strong><br>
            ${escapeHtml(issue.impact)}
        </div>
        ` : ''}
        ${updatesHtml}
    `;

    const resolveBtn = document.getElementById('resolveIssueBtn');
    if (issue.status === 'RESOLVED') {
        resolveBtn.style.display = 'none';
    } else {
        resolveBtn.style.display = 'inline-block';
    }
}

function openAddUpdateModal(id) {
    document.getElementById('updateForm').reset();
    document.getElementById('updateIncidentId').value = id;
    addUpdateModal.show();
}

async function saveUpdate() {
    const incidentId = document.getElementById('updateIncidentId').value;
    const message = document.getElementById('updateMessage').value;
    const newStatus = document.getElementById('updateStatus').value;

    if (!message) {
        showError('Message is required');
        return;
    }

    if (!newStatus) {
        showError('Status is required');
        return;
    }

    try {
        await API.post(`/incidents/${incidentId}/updates`, { message, status: newStatus });

        addUpdateModal.hide();
        loadIssues();
        showSuccess('Update posted successfully');
    } catch (error) {
        console.error('Error posting update:', error);
        showError(error.message || 'Failed to post update');
    }
}

async function resolveIssue(id) {
    try {
        await API.request(`/incidents/${id}/resolve`, {
            method: 'PATCH',
            body: JSON.stringify('Issue has been resolved')
        });
        issueDetailsModal.hide();
        loadIssues();
        showSuccess('Issue marked as resolved');
    } catch (error) {
        console.error('Failed to resolve issue:', error);
        showError('Failed to resolve issue');
    }
}

function confirmDeleteIssue(id, title) {
    document.getElementById('deleteMessage').textContent =
        `Do you really want to delete "${title}"? This action cannot be undone.`;
    deleteCallback = () => deleteIssue(id);
    deleteModal.show();
}

async function deleteIssue(id) {
    try {
        await API.delete(`/incidents/${id}`);
        loadIssues();
        showSuccess('Issue deleted successfully');
    } catch (error) {
        console.error('Failed to delete issue:', error);
        showError('Failed to delete issue');
    }
}

function getSeverityColor(severity) {
    const colors = {
        'MINOR': 'blue',
        'MAJOR': 'orange',
        'CRITICAL': 'red'
    };
    return colors[severity] || 'secondary';
}

function getStatusColor(status) {
    const colors = {
        'INVESTIGATING': 'red',
        'IDENTIFIED': 'orange',
        'MONITORING': 'yellow',
        'RESOLVED': 'green'
    };
    return colors[status] || 'secondary';
}

function formatStatus(status) {
    const labels = {
        'INVESTIGATING': 'Investigating',
        'IDENTIFIED': 'Identified',
        'MONITORING': 'Monitoring',
        'RESOLVED': 'Resolved'
    };
    return labels[status] || status;
}

function formatTime(timestamp) {
    if (!timestamp) return 'Unknown';
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now - date;

    if (diff < 60000) return 'Just now';
    if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`;
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}h ago`;
    return date.toLocaleDateString();
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
