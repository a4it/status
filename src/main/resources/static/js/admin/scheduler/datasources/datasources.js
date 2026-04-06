// Scheduler Datasources page
let datasourceModal, deleteModal;
let currentDatasourceId = null;
let deleteCallback = null;

document.addEventListener('DOMContentLoaded', () => {
    if (!auth.requireAuth()) return;

    document.querySelectorAll('[data-bs-toggle="dropdown"]').forEach(el => {
        new bootstrap.Dropdown(el);
    });

    datasourceModal = new bootstrap.Modal(document.getElementById('datasourceModal'));
    deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));

    document.getElementById('confirmDeleteBtn').addEventListener('click', () => {
        if (deleteCallback) {
            deleteCallback();
            deleteModal.hide();
        }
    });

    updateUserInfo();
    loadDatasources();
});

function updateUserInfo() {
    const userDisplay = document.querySelector('.avatar + div > div');
    if (userDisplay) {
        userDisplay.textContent = auth.getUserDisplayName ? auth.getUserDisplayName() : 'Admin';
    }
}

// ─── Load & Render ────────────────────────────────────────────────────────────

async function loadDatasources() {
    try {
        const response = await API.get('/api/scheduler/datasources');
        const datasources = response.content || response;
        renderDatasourcesTable(datasources || []);
    } catch (error) {
        console.error('Failed to load datasources:', error);
        document.getElementById('datasourcesTable').innerHTML =
            '<tr><td colspan="5" class="text-center text-danger">Failed to load datasources</td></tr>';
    }
}

function renderDatasourcesTable(datasources) {
    const tbody = document.getElementById('datasourcesTable');
    if (!datasources || datasources.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">No datasources configured</td></tr>';
        return;
    }

    tbody.innerHTML = datasources.map(ds => `
        <tr>
            <td>
                <div class="font-weight-medium">${escapeHtml(ds.name)}</div>
                ${ds.description ? `<small class="text-muted">${escapeHtml(ds.description)}</small>` : ''}
            </td>
            <td><span class="badge bg-${getDbTypeBadgeColor(ds.dbType)}">${escapeHtml(ds.dbType)}</span></td>
            <td>
                <code class="text-muted small">
                    ${ds.jdbcUrl ? escapeHtml(ds.jdbcUrl) : escapeHtml(buildConnectionString(ds))}
                </code>
            </td>
            <td>
                <span class="badge bg-${ds.enabled ? 'success' : 'secondary'}">
                    ${ds.enabled ? 'Enabled' : 'Disabled'}
                </span>
            </td>
            <td>
                <button class="btn btn-sm btn-outline-primary me-1" onclick="testDatasource('${escapeHtml(ds.id)}')" title="Test Connection">
                    <i class="ti ti-plug"></i>
                </button>
                <button class="btn btn-sm btn-primary me-1" onclick="openEditModal('${escapeHtml(ds.id)}')" title="Edit">
                    <i class="ti ti-edit"></i>
                </button>
                <button class="btn btn-sm btn-danger" onclick="confirmDeleteDatasource('${escapeHtml(ds.id)}', '${escapeHtml(ds.name)}')" title="Delete">
                    <i class="ti ti-trash"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

function buildConnectionString(ds) {
    if (!ds.host) return '-';
    const port = ds.port ? `:${ds.port}` : '';
    const db = ds.database ? `/${ds.database}` : '';
    return `${ds.host}${port}${db}`;
}

function getDbTypeBadgeColor(type) {
    const map = { POSTGRESQL: 'blue', MYSQL: 'orange', MARIADB: 'cyan', H2: 'gray' };
    return map[type] || 'secondary';
}

// ─── Create / Edit Modal ──────────────────────────────────────────────────────

function openCreateModal() {
    currentDatasourceId = null;
    document.getElementById('datasourceId').value = '';
    document.getElementById('dsName').value = '';
    document.getElementById('dsDescription').value = '';
    document.getElementById('dbType').value = 'POSTGRESQL';
    document.getElementById('dsHost').value = '';
    document.getElementById('dsPort').value = '5432';
    document.getElementById('dsDatabase').value = '';
    document.getElementById('dsSchema').value = '';
    document.getElementById('dsUsername').value = '';
    document.getElementById('dsPassword').value = '';
    document.getElementById('dsEnabled').checked = true;
    document.getElementById('dsJdbcUrl').value = '';
    document.getElementById('dsPoolSize').value = '';
    document.getElementById('dsConnTimeout').value = '';
    document.getElementById('datasourceModalTitle').textContent = 'New Datasource';
    datasourceModal.show();
}

async function openEditModal(id) {
    try {
        const ds = await API.get(`/api/scheduler/datasources/${id}`);
        currentDatasourceId = id;

        document.getElementById('datasourceId').value = ds.id;
        document.getElementById('dsName').value = ds.name || '';
        document.getElementById('dsDescription').value = ds.description || '';
        document.getElementById('dbType').value = ds.dbType || 'POSTGRESQL';
        document.getElementById('dsHost').value = ds.host || '';
        document.getElementById('dsPort').value = ds.port || '';
        document.getElementById('dsDatabase').value = ds.database || '';
        document.getElementById('dsSchema').value = ds.schema || '';
        document.getElementById('dsUsername').value = ds.username || '';
        document.getElementById('dsPassword').value = ''; // never prefill password
        document.getElementById('dsEnabled').checked = ds.enabled !== false;
        document.getElementById('dsJdbcUrl').value = ds.jdbcUrl || '';
        document.getElementById('dsPoolSize').value = ds.poolSize || '';
        document.getElementById('dsConnTimeout').value = ds.connectionTimeoutMs || '';
        document.getElementById('datasourceModalTitle').textContent = 'Edit Datasource';
        datasourceModal.show();
    } catch (error) {
        console.error('Failed to load datasource:', error);
        showError('Failed to load datasource details');
    }
}

// ─── DB Type Port Defaults ────────────────────────────────────────────────────

function onDbTypeChange() {
    const type = document.getElementById('dbType').value;
    const portDefaults = { POSTGRESQL: 5432, MYSQL: 3306, MARIADB: 3306, H2: null };
    const defaultPort = portDefaults[type];
    const portField = document.getElementById('dsPort');
    if (defaultPort != null) {
        portField.value = defaultPort;
    } else {
        portField.value = '';
    }
}

// ─── Save ─────────────────────────────────────────────────────────────────────

async function saveDatasource() {
    const id = document.getElementById('datasourceId').value;
    const name = document.getElementById('dsName').value.trim();
    const host = document.getElementById('dsHost').value.trim();
    const username = document.getElementById('dsUsername').value.trim();

    if (!name) { showError('Name is required'); return; }
    if (!host && !document.getElementById('dsJdbcUrl').value.trim()) {
        showError('Host or JDBC URL override is required');
        return;
    }
    if (!username) { showError('Username is required'); return; }

    const ds = {
        name,
        description: document.getElementById('dsDescription').value.trim() || null,
        dbType: document.getElementById('dbType').value,
        host,
        port: parseInt(document.getElementById('dsPort').value) || null,
        database: document.getElementById('dsDatabase').value.trim() || null,
        schema: document.getElementById('dsSchema').value.trim() || null,
        username,
        enabled: document.getElementById('dsEnabled').checked,
        jdbcUrl: document.getElementById('dsJdbcUrl').value.trim() || null,
        poolSize: parseInt(document.getElementById('dsPoolSize').value) || null,
        connectionTimeoutMs: parseInt(document.getElementById('dsConnTimeout').value) || null
    };

    // Only include password if filled
    const pwd = document.getElementById('dsPassword').value;
    if (pwd) ds.password = pwd;

    try {
        if (id) {
            await API.put(`/api/scheduler/datasources/${id}`, ds);
            showSuccess('Datasource updated successfully');
        } else {
            await API.post('/api/scheduler/datasources', ds);
            showSuccess('Datasource created successfully');
        }
        datasourceModal.hide();
        loadDatasources();
    } catch (error) {
        console.error('Error saving datasource:', error);
        showError(error.message || 'Failed to save datasource');
    }
}

// ─── Test Connection ──────────────────────────────────────────────────────────

async function testDatasource(id) {
    showTestResult('info', 'Testing connection...');
    try {
        const result = await API.post(`/api/scheduler/datasources/${id}/test`, {});
        if (result.success) {
            showTestResult('success', `Connection successful! ${result.message || ''}`);
        } else {
            showTestResult('danger', `Connection failed: ${result.message || 'Unknown error'}`);
        }
    } catch (error) {
        showTestResult('danger', `Connection failed: ${error.message || 'Unknown error'}`);
    }
}

async function testDatasourceFromModal() {
    const id = document.getElementById('datasourceId').value;
    if (!id) {
        showError('Save the datasource first before testing the connection');
        return;
    }
    // Save first, then test
    await saveDatasource();
    await testDatasource(id);
}

function showTestResult(type, message) {
    const banner = document.getElementById('testResultBanner');
    const msg = document.getElementById('testResultMessage');
    banner.className = `alert alert-${type} mb-3`;
    msg.textContent = message;
    banner.style.display = '';
    if (type === 'success' || type === 'info') {
        setTimeout(() => { banner.style.display = 'none'; }, 5000);
    }
}

// ─── Delete ───────────────────────────────────────────────────────────────────

function confirmDeleteDatasource(id, name) {
    document.getElementById('deleteMessage').textContent =
        `Do you really want to delete "${name}"? Jobs using this datasource may fail. This action cannot be undone.`;
    deleteCallback = () => deleteDatasource(id);
    deleteModal.show();
}

async function deleteDatasource(id) {
    try {
        await API.delete(`/api/scheduler/datasources/${id}`);
        showSuccess('Datasource deleted successfully');
        loadDatasources();
    } catch (error) {
        console.error('Failed to delete datasource:', error);
        showError(error.message || 'Failed to delete datasource');
    }
}

// ─── Utilities ────────────────────────────────────────────────────────────────

function escapeHtml(str) {
    if (str == null) return '';
    const s = String(str);
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
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
    const bsToast = new bootstrap.Toast(toast, { autohide: true, delay: 3500 });
    bsToast.show();
    toast.addEventListener('hidden.bs.toast', () => toast.remove());
}

function createToastContainer() {
    const container = document.createElement('div');
    container.id = 'toast-container';
    container.className = 'toast-container position-fixed top-0 end-0 p-3';
    container.style.zIndex = '1200';
    document.body.appendChild(container);
    return container;
}
