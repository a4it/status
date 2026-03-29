let selectedTenant = null;
let selectedOrganization = null;

document.addEventListener('DOMContentLoaded', function () {
    if (!auth.isAuthenticated()) {
        window.location.href = '/login';
        return;
    }

    loadTenants();
    document.getElementById('confirmBtn').addEventListener('click', confirmSelection);
});

async function loadTenants() {
    const container = document.getElementById('tenantsContainer');
    container.innerHTML = '<div class="d-flex align-items-center justify-content-center h-100 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading...</div>';

    try {
        const tenants = await API.get('/context/tenants');

        if (!tenants || tenants.length === 0) {
            container.innerHTML = '<div class="d-flex flex-column align-items-center justify-content-center h-100 text-muted gap-2"><i class="ti ti-inbox" style="font-size:1.5rem;opacity:0.4;"></i><span style="font-size:0.875rem;">No tenants found</span></div>';
            return;
        }

        renderTenants(tenants);

        if (tenants.length === 1) {
            selectTenant(tenants[0], true);
        }
    } catch (error) {
        container.innerHTML = `<div class="p-3"><div class="alert alert-danger mb-0">Failed to load tenants: ${escapeHtml(error.message || 'Unknown error')}</div></div>`;
    }
}

function renderTenants(tenants) {
    const container = document.getElementById('tenantsContainer');
    container.innerHTML = tenants.map((t, i) => `
        <div class="context-item d-flex align-items-center gap-3 list-group-item"
             data-tenant-id="${t.id}"
             onclick="selectTenant(${JSON.stringify(t).replace(/"/g, '&quot;')})">
            <div class="item-icon bg-primary-lt text-primary">${escapeHtml(t.name.charAt(0).toUpperCase())}</div>
            <div class="overflow-hidden">
                <div class="item-name text-truncate">${escapeHtml(t.name)}</div>
            </div>
            <i class="ti ti-chevron-right ms-auto text-muted" style="flex-shrink:0;font-size:0.875rem;"></i>
        </div>
    `).join('');
}

async function selectTenant(tenant, silent) {
    selectedTenant = tenant;
    selectedOrganization = null;

    // Update tenant selection visuals
    document.querySelectorAll('[data-tenant-id]').forEach(el => el.classList.remove('active'));
    const tenantEl = document.querySelector(`[data-tenant-id="${tenant.id}"]`);
    if (tenantEl) tenantEl.classList.add('active');

    // Update badge and crumb
    const badge = document.getElementById('selectedTenantBadge');
    badge.textContent = tenant.name;
    badge.classList.remove('d-none');

    updateCrumb(tenant.name, null);
    document.getElementById('confirmBtn').disabled = true;

    // Load organizations
    const orgsContainer = document.getElementById('orgsContainer');
    orgsContainer.innerHTML = '<div class="d-flex align-items-center justify-content-center h-100 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading...</div>';

    try {
        const orgs = await API.get(`/context/tenants/${tenant.id}/organizations`);

        if (!orgs || orgs.length === 0) {
            orgsContainer.innerHTML = '<div class="d-flex flex-column align-items-center justify-content-center h-100 text-muted gap-2"><i class="ti ti-inbox" style="font-size:1.5rem;opacity:0.4;"></i><span style="font-size:0.875rem;">No organizations in this tenant</span></div>';
            return;
        }

        renderOrganizations(orgs);

        if (orgs.length === 1 && silent) {
            selectOrganization(orgs[0]);
        }
    } catch (error) {
        orgsContainer.innerHTML = `<div class="p-3"><div class="alert alert-danger mb-0">Failed to load organizations: ${escapeHtml(error.message || 'Unknown error')}</div></div>`;
    }
}

function renderOrganizations(orgs) {
    const container = document.getElementById('orgsContainer');
    container.innerHTML = orgs.map(o => `
        <div class="context-item d-flex align-items-center gap-3 list-group-item"
             data-org-id="${o.id}"
             onclick="selectOrganization(${JSON.stringify(o).replace(/"/g, '&quot;')})">
            <div class="item-icon bg-green-lt text-green">${escapeHtml(o.name.charAt(0).toUpperCase())}</div>
            <div class="overflow-hidden">
                <div class="item-name text-truncate">${escapeHtml(o.name)}</div>
                ${o.description ? `<div class="item-meta text-truncate">${escapeHtml(o.description)}</div>` : ''}
            </div>
            <i class="ti ti-check ms-auto text-primary d-none" data-check="${o.id}" style="flex-shrink:0;font-size:1rem;"></i>
        </div>
    `).join('');
}

function selectOrganization(org) {
    selectedOrganization = org;

    document.querySelectorAll('[data-org-id]').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('[data-check]').forEach(el => el.classList.add('d-none'));

    const orgEl = document.querySelector(`[data-org-id="${org.id}"]`);
    if (orgEl) orgEl.classList.add('active');

    const check = document.querySelector(`[data-check="${org.id}"]`);
    if (check) check.classList.remove('d-none');

    updateCrumb(selectedTenant ? selectedTenant.name : null, org.name);
    document.getElementById('confirmBtn').disabled = false;
}

function updateCrumb(tenantName, orgName) {
    const tenantCrumb = document.getElementById('crumbTenant');
    const orgCrumb = document.getElementById('crumbOrg');
    const tenantNameEl = document.getElementById('crumbTenantName');
    const orgNameEl = document.getElementById('crumbOrgName');

    if (tenantName) {
        tenantNameEl.textContent = tenantName;
        tenantCrumb.classList.add('filled');
    } else {
        tenantNameEl.textContent = 'No tenant selected';
        tenantCrumb.classList.remove('filled');
    }

    if (orgName) {
        orgNameEl.textContent = orgName;
        orgCrumb.classList.add('filled');
    } else {
        orgNameEl.textContent = 'No organization selected';
        orgCrumb.classList.remove('filled');
    }
}

async function confirmSelection() {
    if (!selectedTenant || !selectedOrganization) return;

    const btn = document.getElementById('confirmBtn');
    const originalHTML = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Switching...';

    try {
        const result = await API.post('/context/switch', {
            tenantId: selectedTenant.id,
            organizationId: selectedOrganization.id
        });

        auth.updateToken(result.accessToken);
        notifications.show(`Switched to ${result.tenantName} / ${result.organizationName}`, 'success');

        setTimeout(() => {
            window.location.href = '/admin';
        }, 700);
    } catch (error) {
        notifications.show(error.message || 'Failed to switch context', 'error');
        btn.disabled = false;
        btn.innerHTML = originalHTML;
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
