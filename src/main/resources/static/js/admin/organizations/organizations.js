let organizationModal;
let deleteModal;
let deleteCallback = null;
let currentPage = 0;
let pageSize = 10;
let totalPages = 0;
let searchTimeout = null;
let tenants = [];

document.addEventListener('DOMContentLoaded', () => {
    if (!auth.requireAuth()) return;

    // Initialize all dropdowns
    document.querySelectorAll('[data-bs-toggle="dropdown"]').forEach(el => {
        new bootstrap.Dropdown(el);
    });

    organizationModal = new bootstrap.Modal(document.getElementById('organizationModal'));
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
            loadOrganizations();
        }, 300);
    });

    // Tenant filter change
    document.getElementById('tenantFilter').addEventListener('change', () => {
        currentPage = 0;
        loadOrganizations();
    });

    updateUserInfo();
    loadTenants().then(() => {
        loadOrganizations();
    });
});

function updateUserInfo() {
    const userDisplay = document.querySelector('.avatar + div > div');
    if (userDisplay) {
        userDisplay.textContent = auth.getUserDisplayName();
    }
}

async function loadTenants() {
    try {
        const response = await API.get('/tenants?size=100');
        tenants = response.content || [];

        // Populate tenant filter dropdown
        const filterSelect = document.getElementById('tenantFilter');
        filterSelect.innerHTML = '<option value="">All Tenants</option>' +
            tenants.map(t => `<option value="${t.id}">${escapeHtml(t.name)}</option>`).join('');

        // Populate tenant select in modal
        const modalSelect = document.getElementById('organizationTenant');
        modalSelect.innerHTML = '<option value="">Select a tenant...</option>' +
            tenants.map(t => `<option value="${t.id}">${escapeHtml(t.name)}</option>`).join('');
    } catch (error) {
        console.error('Failed to load tenants:', error);
        showError('Failed to load tenants');
    }
}

async function loadOrganizations() {
    const search = document.getElementById('searchInput').value;
    const tenantId = document.getElementById('tenantFilter').value;

    try {
        let url = `/organizations?page=${currentPage}&size=${pageSize}`;
        if (search) {
            url += `&search=${encodeURIComponent(search)}`;
        }
        if (tenantId) {
            url += `&tenantId=${tenantId}`;
        }
        const response = await API.get(url);
        displayOrganizations(response.content || []);
        updatePagination(response);
    } catch (error) {
        console.error('Failed to load organizations:', error);
        showError('Failed to load organizations');
    }
}

function displayOrganizations(organizations) {
    const tbody = document.getElementById('organizationsTable');

    if (!organizations || organizations.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">No organizations found</td></tr>';
        return;
    }

    tbody.innerHTML = organizations.map(org => `
        <tr>
            <td>
                <div class="d-flex align-items-center">
                    ${org.logoUrl ? `<img src="${escapeHtml(org.logoUrl)}" alt="" class="avatar avatar-sm me-2" style="object-fit: contain;">` : ''}
                    <div>
                        <div class="font-weight-medium">${escapeHtml(org.name)}</div>
                        ${org.description ? `<small class="text-muted">${escapeHtml(truncate(org.description, 40))}</small>` : ''}
                    </div>
                </div>
            </td>
            <td>
                ${org.tenant ? `<span class="badge bg-azure">${escapeHtml(org.tenant.name)}</span>` : '<span class="text-muted">-</span>'}
            </td>
            <td>
                <span class="badge bg-purple">${formatOrganizationType(org.organizationType)}</span>
            </td>
            <td>
                <div>
                    ${org.email ? `<small class="d-block"><i class="ti ti-mail me-1"></i>${escapeHtml(org.email)}</small>` : ''}
                    ${org.phone ? `<small class="d-block"><i class="ti ti-phone me-1"></i>${escapeHtml(org.phone)}</small>` : ''}
                    ${!org.email && !org.phone ? '<span class="text-muted">-</span>' : ''}
                </div>
            </td>
            <td>
                <span class="badge bg-${getStatusColor(org.status)}">
                    ${escapeHtml(org.status)}
                </span>
            </td>
            <td class="text-muted">${formatDate(org.createdDate)}</td>
            <td>
                <button class="btn btn-sm btn-primary me-1" onclick="editOrganization('${org.id}')" title="Edit">
                    <i class="ti ti-edit"></i>
                </button>
                <button class="btn btn-sm btn-danger" onclick="confirmDeleteOrganization('${org.id}', '${escapeHtml(org.name)}')" title="Delete">
                    <i class="ti ti-trash"></i>
                </button>
            </td>
        </tr>
    `).join('');
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
    loadOrganizations();
}

function openAddOrganizationModal() {
    document.getElementById('organizationForm').reset();
    document.getElementById('organizationId').value = '';
    document.getElementById('organizationStatus').value = 'ACTIVE';
    document.getElementById('organizationThrottlingEnabled').checked = true;
    document.getElementById('organizationSubscriptionExempt').checked = false;
    document.querySelector('#organizationModal .modal-title').textContent = 'Add Organization';
    organizationModal.show();
}

async function editOrganization(id) {
    try {
        const org = await API.get(`/organizations/${id}`);

        document.getElementById('organizationId').value = org.id;
        document.getElementById('organizationName').value = org.name || '';
        document.getElementById('organizationTenant').value = org.tenant ? org.tenant.id : '';
        document.getElementById('organizationDescription').value = org.description || '';
        document.getElementById('organizationOrgType').value = org.organizationType || '';
        document.getElementById('organizationStatus').value = org.status || 'ACTIVE';
        document.getElementById('organizationEmail').value = org.email || '';
        document.getElementById('organizationPhone').value = org.phone || '';
        document.getElementById('organizationWebsite').value = org.website || '';
        document.getElementById('organizationLogoUrl').value = org.logoUrl || '';
        document.getElementById('organizationAddress').value = org.address || '';
        document.getElementById('organizationPostalCode').value = org.postalCode || '';
        document.getElementById('organizationCommunity').value = org.community || '';
        document.getElementById('organizationCountry').value = org.country || '';
        document.getElementById('organizationVatNumber').value = org.vatNumber || '';
        document.getElementById('organizationType').value = org.type || '';
        document.getElementById('organizationSubscriptionExempt').checked = org.subscriptionExempt || false;
        document.getElementById('organizationThrottlingEnabled').checked = org.throttlingEnabled !== false;

        document.querySelector('#organizationModal .modal-title').textContent = 'Edit Organization';
        organizationModal.show();
    } catch (error) {
        console.error('Failed to load organization:', error);
        showError('Failed to load organization details');
    }
}

async function saveOrganization() {
    const id = document.getElementById('organizationId').value;
    const organization = {
        name: document.getElementById('organizationName').value,
        tenantId: document.getElementById('organizationTenant').value || null,
        description: document.getElementById('organizationDescription').value || null,
        organizationType: document.getElementById('organizationOrgType').value,
        status: document.getElementById('organizationStatus').value,
        email: document.getElementById('organizationEmail').value || null,
        phone: document.getElementById('organizationPhone').value || null,
        website: document.getElementById('organizationWebsite').value || null,
        logoUrl: document.getElementById('organizationLogoUrl').value || null,
        address: document.getElementById('organizationAddress').value || null,
        postalCode: document.getElementById('organizationPostalCode').value || null,
        community: document.getElementById('organizationCommunity').value || null,
        country: document.getElementById('organizationCountry').value || null,
        vatNumber: document.getElementById('organizationVatNumber').value || null,
        type: document.getElementById('organizationType').value || null,
        subscriptionExempt: document.getElementById('organizationSubscriptionExempt').checked,
        throttlingEnabled: document.getElementById('organizationThrottlingEnabled').checked
    };

    if (!organization.name) {
        showError('Organization name is required');
        return;
    }
    if (!organization.organizationType) {
        showError('Organization type is required');
        return;
    }

    try {
        if (id) {
            await API.put(`/organizations/${id}`, organization);
        } else {
            await API.post('/organizations', organization);
        }
        organizationModal.hide();
        loadOrganizations();
        showSuccess(id ? 'Organization updated successfully' : 'Organization created successfully');
    } catch (error) {
        console.error('Error saving organization:', error);
        showError(error.message || 'Failed to save organization');
    }
}

function confirmDeleteOrganization(id, name) {
    document.getElementById('deleteMessage').textContent =
        `Do you really want to delete "${name}"? This action cannot be undone.`;
    deleteCallback = () => deleteOrganization(id);
    deleteModal.show();
}

async function deleteOrganization(id) {
    try {
        await API.delete(`/organizations/${id}`);
        loadOrganizations();
        showSuccess('Organization deleted successfully');
    } catch (error) {
        console.error('Failed to delete organization:', error);
        showError(error.message || 'Failed to delete organization');
    }
}

function formatOrganizationType(type) {
    const types = {
        'ENTERPRISE': 'Enterprise',
        'SMALL_BUSINESS': 'Small Business',
        'STARTUP': 'Startup',
        'NON_PROFIT': 'Non-Profit',
        'GOVERNMENT': 'Government',
        'EDUCATION': 'Education',
        'INDIVIDUAL': 'Individual'
    };
    return types[type] || type || '-';
}

function getStatusColor(status) {
    const colors = {
        'ACTIVE': 'green',
        'INACTIVE': 'secondary',
        'SUSPENDED': 'red',
        'PENDING': 'yellow'
    };
    return colors[status] || 'secondary';
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

function truncate(text, maxLength) {
    if (!text) return '';
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
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
