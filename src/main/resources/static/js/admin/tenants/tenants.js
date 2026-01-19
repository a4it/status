let tenantModal;
let deleteModal;
let deleteCallback = null;
let currentPage = 0;
let pageSize = 10;
let totalPages = 0;
let searchTimeout = null;

document.addEventListener('DOMContentLoaded', () => {
    if (!auth.requireAuth()) return;

    // Initialize all dropdowns
    document.querySelectorAll('[data-bs-toggle="dropdown"]').forEach(el => {
        new bootstrap.Dropdown(el);
    });

    tenantModal = new bootstrap.Modal(document.getElementById('tenantModal'));
    deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));

    document.getElementById('confirmDeleteBtn').addEventListener('click', () => {
        if (deleteCallback) {
            deleteCallback();
            deleteModal.hide();
        }
    });

    // Search input with debounce
    document.getElementById('searchInput').addEventListener('input', (e) => {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => {
            currentPage = 0;
            loadTenants();
        }, 300);
    });

    updateUserInfo();
    loadTenants();
});

function updateUserInfo() {
    const userDisplay = document.querySelector('.avatar + div > div');
    if (userDisplay) {
        userDisplay.textContent = auth.getUserDisplayName();
    }
}

async function loadTenants() {
    const search = document.getElementById('searchInput').value;
    try {
        let url = `/tenants?page=${currentPage}&size=${pageSize}`;
        if (search) {
            url += `&search=${encodeURIComponent(search)}`;
        }
        const response = await API.get(url);
        displayTenants(response.content || []);
        updatePagination(response);
    } catch (error) {
        console.error('Failed to load tenants:', error);
        showError('Failed to load tenants');
    }
}

function displayTenants(tenants) {
    const tbody = document.getElementById('tenantsTable');

    if (!tenants || tenants.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">No tenants found</td></tr>';
        return;
    }

    tbody.innerHTML = tenants.map(tenant => `
        <tr>
            <td>
                <div class="font-weight-medium">${escapeHtml(tenant.name)}</div>
            </td>
            <td>
                <span class="badge bg-${tenant.isActive ? 'green' : 'secondary'}">
                    ${tenant.isActive ? 'Active' : 'Inactive'}
                </span>
            </td>
            <td>
                <span class="badge bg-blue" id="org-count-${tenant.id}">Loading...</span>
            </td>
            <td class="text-muted">${formatDate(tenant.createdDate)}</td>
            <td class="text-muted">${formatDate(tenant.lastModifiedDate)}</td>
            <td>
                <button class="btn btn-sm btn-primary me-1" onclick="editTenant('${tenant.id}')" title="Edit">
                    <i class="ti ti-edit"></i>
                </button>
                <button class="btn btn-sm btn-danger" onclick="confirmDeleteTenant('${tenant.id}', '${escapeHtml(tenant.name)}')" title="Delete">
                    <i class="ti ti-trash"></i>
                </button>
            </td>
        </tr>
    `).join('');

    // Load organization counts for each tenant
    tenants.forEach(tenant => loadOrganizationCount(tenant.id));
}

async function loadOrganizationCount(tenantId) {
    try {
        const orgs = await API.get(`/organizations/tenant/${tenantId}`);
        const countEl = document.getElementById(`org-count-${tenantId}`);
        if (countEl) {
            const count = orgs.length || 0;
            countEl.textContent = `${count} org${count !== 1 ? 's' : ''}`;
        }
    } catch (error) {
        const countEl = document.getElementById(`org-count-${tenantId}`);
        if (countEl) {
            countEl.textContent = '0 orgs';
            countEl.classList.remove('bg-blue');
            countEl.classList.add('bg-secondary');
        }
    }
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

    // Previous button
    pagination.innerHTML += `
        <li class="page-item ${number === 0 ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="goToPage(${number - 1}); return false;">
                <i class="ti ti-chevron-left"></i>
            </a>
        </li>
    `;

    // Page numbers
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

    // Next button
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
    loadTenants();
}

function openAddTenantModal() {
    document.getElementById('tenantForm').reset();
    document.getElementById('tenantId').value = '';
    document.getElementById('tenantIsActive').checked = true;
    document.querySelector('#tenantModal .modal-title').textContent = 'Add Tenant';
    tenantModal.show();
}

async function editTenant(id) {
    try {
        const tenant = await API.get(`/tenants/${id}`);

        document.getElementById('tenantId').value = tenant.id;
        document.getElementById('tenantName').value = tenant.name;
        document.getElementById('tenantIsActive').checked = tenant.isActive;

        document.querySelector('#tenantModal .modal-title').textContent = 'Edit Tenant';
        tenantModal.show();
    } catch (error) {
        console.error('Failed to load tenant:', error);
        showError('Failed to load tenant details');
    }
}

async function saveTenant() {
    const id = document.getElementById('tenantId').value;
    const tenant = {
        name: document.getElementById('tenantName').value,
        isActive: document.getElementById('tenantIsActive').checked
    };

    if (!tenant.name) {
        showError('Tenant name is required');
        return;
    }

    try {
        if (id) {
            await API.put(`/tenants/${id}`, tenant);
        } else {
            await API.post('/tenants', tenant);
        }
        tenantModal.hide();
        loadTenants();
        showSuccess(id ? 'Tenant updated successfully' : 'Tenant created successfully');
    } catch (error) {
        console.error('Error saving tenant:', error);
        showError(error.message || 'Failed to save tenant');
    }
}

function confirmDeleteTenant(id, name) {
    document.getElementById('deleteMessage').textContent =
        `Do you really want to delete "${name}"? This will also delete all associated organizations. This action cannot be undone.`;
    deleteCallback = () => deleteTenant(id);
    deleteModal.show();
}

async function deleteTenant(id) {
    try {
        await API.delete(`/tenants/${id}`);
        loadTenants();
        showSuccess('Tenant deleted successfully');
    } catch (error) {
        console.error('Failed to delete tenant:', error);
        showError(error.message || 'Failed to delete tenant');
    }
}

function formatDate(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
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
