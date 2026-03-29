let userModal;
let deleteModal;
let deleteCallback = null;
let currentPage = 0;
let pageSize = 10;
let totalPages = 0;
let searchTimeout = null;
let organizations = [];

document.addEventListener('DOMContentLoaded', () => {
    if (!auth.requireAuth()) return;

    document.querySelectorAll('[data-bs-toggle="dropdown"]').forEach(el => {
        new bootstrap.Dropdown(el);
    });

    userModal = new bootstrap.Modal(document.getElementById('userModal'));
    deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));

    document.getElementById('confirmDeleteBtn').addEventListener('click', () => {
        if (deleteCallback) {
            deleteCallback();
            deleteModal.hide();
        }
    });

    document.getElementById('searchInput').addEventListener('input', () => {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => {
            currentPage = 0;
            loadUsers();
        }, 300);
    });

    document.getElementById('organizationFilter').addEventListener('change', () => {
        currentPage = 0;
        loadUsers();
    });

    document.getElementById('roleFilter').addEventListener('change', () => {
        currentPage = 0;
        loadUsers();
    });

    updateUserInfo();
    loadOrganizations().then(() => {
        loadUsers();
    });
});

function updateUserInfo() {
    const userDisplay = document.querySelector('.avatar + div > div');
    if (userDisplay) {
        userDisplay.textContent = auth.getUserDisplayName();
    }
}

async function loadOrganizations() {
    try {
        const response = await API.get('/organizations?size=200');
        organizations = response.content || [];

        const filterSelect = document.getElementById('organizationFilter');
        filterSelect.innerHTML = '<option value="">All Organizations</option>' +
            organizations.map(o => `<option value="${o.id}">${escapeHtml(o.name)}</option>`).join('');

        const modalSelect = document.getElementById('userOrganization');
        modalSelect.innerHTML = '<option value="">No organization</option>' +
            organizations.map(o => `<option value="${o.id}">${escapeHtml(o.name)}</option>`).join('');
    } catch (error) {
        console.error('Failed to load organizations:', error);
        showError('Failed to load organizations');
    }
}

async function loadUsers() {
    const search = document.getElementById('searchInput').value;
    const organizationId = document.getElementById('organizationFilter').value;
    const role = document.getElementById('roleFilter').value;

    try {
        let url = `/users?page=${currentPage}&size=${pageSize}`;
        if (search) url += `&search=${encodeURIComponent(search)}`;
        if (organizationId) url += `&organizationId=${organizationId}`;
        if (role) url += `&role=${encodeURIComponent(role)}`;

        const response = await API.get(url);
        displayUsers(response.content || []);
        updatePagination(response);
    } catch (error) {
        console.error('Failed to load users:', error);
        showError('Failed to load users');
    }
}

function displayUsers(users) {
    const tbody = document.getElementById('usersTable');

    if (!users || users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted">No users found</td></tr>';
        return;
    }

    tbody.innerHTML = users.map(user => {
        const initials = getInitials(user.fullName || user.username);
        const avatarColor = getAvatarColor(user.username);
        return `
        <tr>
            <td>
                <div class="d-flex align-items-center">
                    <span class="avatar avatar-sm me-2" style="background-color: ${avatarColor};">${escapeHtml(initials)}</span>
                    <div>
                        <div class="font-weight-medium">${escapeHtml(user.username)}</div>
                        ${user.fullName ? `<small class="text-muted">${escapeHtml(user.fullName)}</small>` : ''}
                    </div>
                </div>
            </td>
            <td class="text-muted">${escapeHtml(user.email || '-')}</td>
            <td>
                ${user.organization ? `<span class="badge bg-azure">${escapeHtml(user.organization.name)}</span>` : '<span class="text-muted">-</span>'}
            </td>
            <td>
                <span class="badge bg-${getRoleColor(user.role)}">${escapeHtml(formatRole(user.role))}</span>
            </td>
            <td>
                <span class="badge bg-${getStatusColor(user.status)}">${escapeHtml(user.status || '-')}</span>
            </td>
            <td>
                ${user.enabled
                    ? '<span class="badge bg-green"><i class="ti ti-check me-1"></i>Enabled</span>'
                    : '<span class="badge bg-secondary"><i class="ti ti-x me-1"></i>Disabled</span>'}
            </td>
            <td class="text-muted">${formatDate(user.createdDate)}</td>
            <td>
                <button class="btn btn-sm btn-primary me-1" onclick="editUser('${user.id}')" title="Edit">
                    <i class="ti ti-edit"></i>
                </button>
                ${user.enabled
                    ? `<button class="btn btn-sm btn-warning me-1" onclick="toggleUser('${user.id}', false)" title="Disable"><i class="ti ti-user-off"></i></button>`
                    : `<button class="btn btn-sm btn-success me-1" onclick="toggleUser('${user.id}', true)" title="Enable"><i class="ti ti-user-check"></i></button>`}
                <button class="btn btn-sm btn-danger" onclick="confirmDeleteUser('${user.id}', '${escapeHtml(user.username)}')" title="Delete">
                    <i class="ti ti-trash"></i>
                </button>
            </td>
        </tr>
        `;
    }).join('');
}

function updatePagination(response) {
    totalPages = response.totalPages || 0;
    const totalElements = response.totalElements || 0;
    const number = response.number || 0;
    const size = response.size || pageSize;

    const from = totalElements > 0 ? (number * size) + 1 : 0;
    const to = Math.min((number + 1) * size, totalElements);

    document.getElementById('showingFrom').textContent = from;
    document.getElementById('showingTo').textContent = to;
    document.getElementById('totalEntries').textContent = totalElements;

    const pagination = document.getElementById('pagination');
    pagination.innerHTML = '';

    if (totalPages <= 1) return;

    pagination.innerHTML += `
        <li class="page-item ${number === 0 ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="goToPage(${number - 1}); return false;">
                <i class="ti ti-chevron-left"></i>
            </a>
        </li>
    `;

    const startPage = Math.max(0, number - 2);
    const endPage = Math.min(totalPages - 1, number + 2);

    if (startPage > 0) {
        pagination.innerHTML += `<li class="page-item"><a class="page-link" href="#" onclick="goToPage(0); return false;">1</a></li>`;
        if (startPage > 1) {
            pagination.innerHTML += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
        }
    }

    for (let i = startPage; i <= endPage; i++) {
        pagination.innerHTML += `
            <li class="page-item ${i === number ? 'active' : ''}">
                <a class="page-link" href="#" onclick="goToPage(${i}); return false;">${i + 1}</a>
            </li>
        `;
    }

    if (endPage < totalPages - 1) {
        if (endPage < totalPages - 2) {
            pagination.innerHTML += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
        }
        pagination.innerHTML += `<li class="page-item"><a class="page-link" href="#" onclick="goToPage(${totalPages - 1}); return false;">${totalPages}</a></li>`;
    }

    pagination.innerHTML += `
        <li class="page-item ${number >= totalPages - 1 ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="goToPage(${number + 1}); return false;">
                <i class="ti ti-chevron-right"></i>
            </a>
        </li>
    `;
}

function goToPage(page) {
    if (page < 0 || page >= totalPages) return;
    currentPage = page;
    loadUsers();
}

function openAddUserModal() {
    document.getElementById('userForm').reset();
    document.getElementById('userId').value = '';
    document.getElementById('userRole').value = 'USER';
    document.getElementById('userStatus').value = 'ACTIVE';
    document.getElementById('userEnabled').checked = true;

    document.getElementById('passwordLabel').innerHTML = 'Password <span class="text-danger">*</span>';
    document.getElementById('userPassword').required = true;
    document.getElementById('passwordHint').style.display = 'none';

    document.querySelector('#userModal .modal-title').textContent = 'Add User';
    userModal.show();
}

async function editUser(id) {
    try {
        const user = await API.get(`/users/${id}`);

        document.getElementById('userId').value = user.id;
        document.getElementById('userUsername').value = user.username || '';
        document.getElementById('userFullName').value = user.fullName || '';
        document.getElementById('userEmail').value = user.email || '';
        document.getElementById('userPassword').value = '';
        document.getElementById('userRole').value = user.role || 'USER';
        document.getElementById('userStatus').value = user.status || 'ACTIVE';
        document.getElementById('userEnabled').checked = user.enabled !== false;
        document.getElementById('userOrganization').value = user.organization ? user.organization.id : '';

        document.getElementById('passwordLabel').textContent = 'Password';
        document.getElementById('userPassword').required = false;
        document.getElementById('passwordHint').style.display = 'block';

        document.querySelector('#userModal .modal-title').textContent = 'Edit User';
        userModal.show();
    } catch (error) {
        console.error('Failed to load user:', error);
        showError('Failed to load user details');
    }
}

async function saveUser() {
    const id = document.getElementById('userId').value;
    const password = document.getElementById('userPassword').value;

    const user = {
        username: document.getElementById('userUsername').value,
        fullName: document.getElementById('userFullName').value,
        email: document.getElementById('userEmail').value,
        role: document.getElementById('userRole').value,
        status: document.getElementById('userStatus').value,
        enabled: document.getElementById('userEnabled').checked,
        organizationId: auth.isSuperadmin()
            ? (document.getElementById('userOrganization').value || null)
            : (auth.getUser()?.organizationId || null)
    };

    if (password) {
        user.password = password;
    }

    if (!user.username) { showError('Username is required'); return; }
    if (!user.fullName) { showError('Full name is required'); return; }
    if (!user.email) { showError('Email is required'); return; }
    if (!id && !password) { showError('Password is required for new users'); return; }
    if (password && password.length < 8) { showError('Password must be at least 8 characters'); return; }

    try {
        if (id) {
            await API.put(`/users/${id}`, user);
        } else {
            await API.post('/users', user);
        }
        userModal.hide();
        loadUsers();
        showSuccess(id ? 'User updated successfully' : 'User created successfully');
    } catch (error) {
        console.error('Error saving user:', error);
        showError(error.message || 'Failed to save user');
    }
}

async function toggleUser(id, enable) {
    try {
        const endpoint = enable ? `/users/${id}/enable` : `/users/${id}/disable`;
        await API.patch(endpoint);
        loadUsers();
        showSuccess(enable ? 'User enabled successfully' : 'User disabled successfully');
    } catch (error) {
        console.error('Failed to toggle user:', error);
        showError(error.message || 'Failed to update user');
    }
}

function confirmDeleteUser(id, username) {
    document.getElementById('deleteMessage').textContent =
        `Do you really want to delete "${username}"? This action cannot be undone.`;
    deleteCallback = () => deleteUser(id);
    deleteModal.show();
}

async function deleteUser(id) {
    try {
        await API.delete(`/users/${id}`);
        loadUsers();
        showSuccess('User deleted successfully');
    } catch (error) {
        console.error('Failed to delete user:', error);
        showError(error.message || 'Failed to delete user');
    }
}

function getInitials(name) {
    if (!name) return '?';
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
    return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
}

function getAvatarColor(username) {
    const colors = ['#206bc4', '#ae3ec9', '#2fb344', '#f76707', '#e63946', '#0ca678', '#4263eb'];
    let hash = 0;
    for (let i = 0; i < (username || '').length; i++) {
        hash = username.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
}

function formatRole(role) {
    const roles = { 'ADMIN': 'Admin', 'MANAGER': 'Manager', 'USER': 'User', 'VIEWER': 'Viewer' };
    return roles[role] || role || '-';
}

function getRoleColor(role) {
    const colors = { 'ADMIN': 'red', 'MANAGER': 'orange', 'USER': 'blue', 'VIEWER': 'secondary' };
    return colors[role] || 'secondary';
}

function getStatusColor(status) {
    const colors = { 'ACTIVE': 'green', 'INACTIVE': 'secondary', 'SUSPENDED': 'red', 'PENDING': 'yellow' };
    return colors[status] || 'secondary';
}

function formatDate(dateString) {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showSuccess(message) { showToast(message, 'success'); }
function showError(message) { showToast(message, 'danger'); }

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
