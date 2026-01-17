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
        const response = await API.get('/status-platforms?size=100');
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
                <div class="d-flex align-items-center">
                    ${platform.logoUrl ? `<img src="${escapeHtml(platform.logoUrl)}" alt="" class="avatar avatar-sm me-2" style="object-fit: contain;">` : ''}
                    <div>
                        <div class="font-weight-medium">${escapeHtml(platform.name)}</div>
                        ${platform.websiteUrl ? `<small class="text-muted"><a href="${escapeHtml(platform.websiteUrl)}" target="_blank" class="text-reset"><i class="ti ti-external-link"></i></a></small>` : ''}
                    </div>
                </div>
            </td>
            <td><code>${escapeHtml(platform.slug)}</code></td>
            <td class="text-muted">${escapeHtml(truncate(platform.description, 50) || '-')}</td>
            <td>
                <span class="badge bg-${getStatusColor(platform.status)}">
                    ${formatStatus(platform.status)}
                </span>
            </td>
            <td>
                ${getAppsDisplay(platform)}
            </td>
            <td>
                <span class="badge bg-${platform.isPublic ? 'green' : 'secondary'}">
                    ${platform.isPublic ? 'Yes' : 'No'}
                </span>
            </td>
            <td>
                <button class="btn btn-sm btn-primary me-1" onclick="editPlatform('${platform.id}')" title="Edit">
                    <i class="ti ti-edit"></i>
                </button>
                <button class="btn btn-sm btn-danger" onclick="confirmDeletePlatform('${platform.id}', '${escapeHtml(platform.name)}')" title="Delete">
                    <i class="ti ti-trash"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

function getAppsDisplay(platform) {
    if (!platform.apps || platform.apps.length === 0) {
        return '<span class="text-muted">No apps</span>';
    }

    const appCount = platform.apps.length;
    const appNames = platform.apps.slice(0, 3).map(app => app.name).join(', ');
    const moreText = appCount > 3 ? ` +${appCount - 3} more` : '';

    return `<span class="badge bg-blue" title="${escapeHtml(platform.apps.map(a => a.name).join(', '))}">${appCount} app${appCount !== 1 ? 's' : ''}</span>`;
}

function truncate(text, maxLength) {
    if (!text) return '';
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
}

function openAddPlatformModal() {
    document.getElementById('platformForm').reset();
    document.getElementById('platformId').value = '';
    document.getElementById('platformIsPublic').checked = true;
    document.getElementById('platformPosition').value = '0';
    document.querySelector('#platformModal .modal-title').textContent = 'Add Platform';
    platformModal.show();
}

async function editPlatform(id) {
    try {
        const platform = await API.get(`/status-platforms/${id}`);

        document.getElementById('platformId').value = platform.id;
        document.getElementById('platformName').value = platform.name;
        document.getElementById('platformSlug').value = platform.slug;
        document.getElementById('platformDescription').value = platform.description || '';
        document.getElementById('platformLogoUrl').value = platform.logoUrl || '';
        document.getElementById('platformWebsiteUrl').value = platform.websiteUrl || '';
        document.getElementById('platformStatus').value = platform.status;
        document.getElementById('platformPosition').value = platform.position || 0;
        document.getElementById('platformIsPublic').checked = platform.isPublic;

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
        logoUrl: document.getElementById('platformLogoUrl').value || null,
        websiteUrl: document.getElementById('platformWebsiteUrl').value || null,
        status: document.getElementById('platformStatus').value,
        position: parseInt(document.getElementById('platformPosition').value) || 0,
        isPublic: document.getElementById('platformIsPublic').checked
    };

    if (!platform.name || !platform.slug) {
        showError('Name and slug are required');
        return;
    }

    try {
        if (id) {
            await API.put(`/status-platforms/${id}`, platform);
        } else {
            await API.post('/status-platforms', platform);
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
        await API.delete(`/status-platforms/${id}`);
        loadPlatforms();
        showSuccess('Platform deleted successfully');
    } catch (error) {
        console.error('Failed to delete platform:', error);
        showError(error.message || 'Failed to delete platform');
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
