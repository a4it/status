let subscriberModal;
let deleteModal;
let deleteCallback = null;
let platforms = [];

document.addEventListener('DOMContentLoaded', () => {
    if (!auth.requireAuth()) return;

    // Initialize all dropdowns
    document.querySelectorAll('[data-bs-toggle="dropdown"]').forEach(el => {
        new bootstrap.Dropdown(el);
    });

    subscriberModal = new bootstrap.Modal(document.getElementById('subscriberModal'));
    deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));

    document.getElementById('confirmDeleteBtn').addEventListener('click', () => {
        if (deleteCallback) {
            deleteCallback();
            deleteModal.hide();
        }
    });

    updateUserInfo();
    loadPlatforms().then(() => loadSubscribers());
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
        platforms = response.content || response;

        // Populate filter dropdown
        const filterSelect = document.getElementById('platformFilter');
        filterSelect.innerHTML = '<option value="">All Platforms</option>' +
            platforms.map(p => `<option value="${p.id}">${escapeHtml(p.name)}</option>`).join('');

        // Populate modal dropdown
        const modalSelect = document.getElementById('subscriberAppId');
        modalSelect.innerHTML = '<option value="">Select a platform...</option>' +
            platforms.map(p => `<option value="${p.id}">${escapeHtml(p.name)}</option>`).join('');
    } catch (error) {
        console.error('Failed to load platforms:', error);
        showError('Failed to load platforms');
    }
}

async function loadSubscribers() {
    try {
        const appId = document.getElementById('platformFilter').value;
        let url = '/notification-subscribers';
        if (appId) {
            url = `/notification-subscribers/by-app/${appId}`;
        }

        const subscribers = await API.get(url);
        displaySubscribers(subscribers);
    } catch (error) {
        console.error('Failed to load subscribers:', error);
        showError('Failed to load subscribers');
    }
}

function displaySubscribers(subscribers) {
    const tbody = document.getElementById('subscribersTable');

    if (!subscribers || subscribers.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">No subscribers configured</td></tr>';
        return;
    }

    tbody.innerHTML = subscribers.map(subscriber => `
        <tr>
            <td>
                <div class="font-weight-medium">${escapeHtml(subscriber.email)}</div>
            </td>
            <td class="text-muted">${escapeHtml(subscriber.name || '-')}</td>
            <td>${escapeHtml(subscriber.appName || '-')}</td>
            <td>
                <span class="badge bg-${subscriber.isActive ? 'green' : 'secondary'}">
                    ${subscriber.isActive ? 'Active' : 'Inactive'}
                </span>
            </td>
            <td>
                <span class="badge bg-${subscriber.isVerified ? 'green' : 'yellow'}">
                    ${subscriber.isVerified ? 'Verified' : 'Pending'}
                </span>
            </td>
            <td class="text-muted">${formatDate(subscriber.createdDate)}</td>
            <td>
                <button class="btn btn-sm btn-primary me-1" onclick="editSubscriber('${subscriber.id}')">
                    <i class="ti ti-edit"></i>
                </button>
                <button class="btn btn-sm btn-danger" onclick="confirmDeleteSubscriber('${subscriber.id}', '${escapeHtml(subscriber.email)}')">
                    <i class="ti ti-trash"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

function formatDate(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function openAddSubscriberModal() {
    document.getElementById('subscriberForm').reset();
    document.getElementById('subscriberId').value = '';
    document.getElementById('subscriberIsActive').checked = true;

    // Pre-select platform if filtered
    const filteredPlatform = document.getElementById('platformFilter').value;
    if (filteredPlatform) {
        document.getElementById('subscriberAppId').value = filteredPlatform;
    }

    document.querySelector('#subscriberModal .modal-title').textContent = 'Add Subscriber';
    subscriberModal.show();
}

async function editSubscriber(id) {
    try {
        const subscriber = await API.get(`/notification-subscribers/${id}`);

        document.getElementById('subscriberId').value = subscriber.id;
        document.getElementById('subscriberAppId').value = subscriber.appId;
        document.getElementById('subscriberEmail').value = subscriber.email;
        document.getElementById('subscriberName').value = subscriber.name || '';
        document.getElementById('subscriberIsActive').checked = subscriber.isActive;

        document.querySelector('#subscriberModal .modal-title').textContent = 'Edit Subscriber';
        subscriberModal.show();
    } catch (error) {
        console.error('Failed to load subscriber:', error);
        showError('Failed to load subscriber details');
    }
}

async function saveSubscriber() {
    const id = document.getElementById('subscriberId').value;
    const subscriber = {
        appId: document.getElementById('subscriberAppId').value,
        email: document.getElementById('subscriberEmail').value,
        name: document.getElementById('subscriberName').value || null,
        isActive: document.getElementById('subscriberIsActive').checked
    };

    if (!subscriber.appId || !subscriber.email) {
        showError('Platform and email are required');
        return;
    }

    // Basic email validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(subscriber.email)) {
        showError('Please enter a valid email address');
        return;
    }

    try {
        if (id) {
            await API.put(`/notification-subscribers/${id}`, subscriber);
        } else {
            await API.post('/notification-subscribers', subscriber);
        }
        subscriberModal.hide();
        loadSubscribers();
        showSuccess(id ? 'Subscriber updated successfully' : 'Subscriber added successfully');
    } catch (error) {
        console.error('Error saving subscriber:', error);
        showError(error.message || 'Failed to save subscriber');
    }
}

function confirmDeleteSubscriber(id, email) {
    document.getElementById('deleteMessage').textContent =
        `Do you really want to delete "${email}"? They will no longer receive incident notifications.`;
    deleteCallback = () => deleteSubscriber(id);
    deleteModal.show();
}

async function deleteSubscriber(id) {
    try {
        await API.delete(`/notification-subscribers/${id}`);
        loadSubscribers();
        showSuccess('Subscriber deleted successfully');
    } catch (error) {
        console.error('Failed to delete subscriber:', error);
        showError('Failed to delete subscriber');
    }
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
