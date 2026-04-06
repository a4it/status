// Scheduler Jobs page
let currentPage = 0;
let currentJobId = null;
let currentStep = 1;
let jobModal, runHistoryModal, outputModal, deleteModal;
let deleteCallback = null;
let runHistoryJobId = null;
let runHistoryPage = 0;
let currentRunData = null;
let cronPreviewTimer = null;
let searchDebounceTimer = null;

document.addEventListener('DOMContentLoaded', () => {
    if (!auth.requireAuth()) return;

    document.querySelectorAll('[data-bs-toggle="dropdown"]').forEach(el => {
        new bootstrap.Dropdown(el);
    });

    jobModal = new bootstrap.Modal(document.getElementById('jobModal'));
    runHistoryModal = new bootstrap.Modal(document.getElementById('runHistoryModal'));
    outputModal = new bootstrap.Modal(document.getElementById('outputModal'));
    deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));

    document.getElementById('confirmDeleteBtn').addEventListener('click', () => {
        if (deleteCallback) {
            deleteCallback();
            deleteModal.hide();
        }
    });

    updateUserInfo();
    loadStats();
    loadJobs(0);
    loadTimezones();

    setInterval(loadStats, 30000);
    setInterval(() => loadJobs(currentPage), 60000);
});

function updateUserInfo() {
    const userDisplay = document.querySelector('.avatar + div > div');
    if (userDisplay) {
        userDisplay.textContent = auth.getUserDisplayName ? auth.getUserDisplayName() : 'Admin';
    }
}

// ─── Stats ───────────────────────────────────────────────────────────────────

async function loadStats() {
    try {
        const stats = await API.get('/api/scheduler/jobs/stats');
        document.getElementById('stat-total').textContent = stats.total ?? 0;
        document.getElementById('stat-running').textContent = stats.running ?? 0;
        document.getElementById('stat-succeeded').textContent = stats.succeededToday ?? 0;
        document.getElementById('stat-failed').textContent = stats.failedToday ?? 0;
    } catch (error) {
        console.error('Failed to load stats:', error);
    }
}

// ─── Jobs List ────────────────────────────────────────────────────────────────

async function loadJobs(page = 0) {
    currentPage = page;
    const status = document.getElementById('filterStatus').value;
    const type = document.getElementById('filterType').value;
    const search = document.getElementById('searchInput').value.trim();

    let url = `/api/scheduler/jobs?page=${page}&size=20`;
    if (status) url += `&status=${encodeURIComponent(status)}`;
    if (type) url += `&type=${encodeURIComponent(type)}`;
    if (search) url += `&search=${encodeURIComponent(search)}`;

    try {
        const response = await API.get(url);
        const jobs = response.content || response;
        const totalPages = response.totalPages || 1;
        const totalElements = response.totalElements || (jobs ? jobs.length : 0);
        renderJobsTable(jobs || []);
        renderPagination(page, totalPages, totalElements);
    } catch (error) {
        console.error('Failed to load jobs:', error);
        document.getElementById('jobsTable').innerHTML =
            '<tr><td colspan="7" class="text-center text-danger">Failed to load jobs</td></tr>';
    }
}

function renderJobsTable(jobs) {
    const tbody = document.getElementById('jobsTable');
    if (!jobs || jobs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">No jobs found</td></tr>';
        return;
    }

    tbody.innerHTML = jobs.map(job => `
        <tr style="cursor:pointer;" onclick="openRunHistory('${escapeHtml(job.id)}', '${escapeHtml(job.name)}')">
            <td>
                <div class="font-weight-medium">${escapeHtml(job.name)}</div>
                ${job.description ? `<small class="text-muted">${escapeHtml(truncate(job.description, 60))}</small>` : ''}
                ${job.tags ? `<div>${job.tags.map(t => `<span class="badge bg-secondary me-1">${escapeHtml(t)}</span>`).join('')}</div>` : ''}
            </td>
            <td><span class="badge bg-${getTypeBadgeColor(job.jobType)}">${escapeHtml(job.jobType)}</span></td>
            <td><code class="text-muted small">${escapeHtml(job.cronExpression || '-')}</code></td>
            <td class="text-muted small">${job.nextRunTime ? formatRelativeTime(job.nextRunTime) : '-'}</td>
            <td class="text-muted small">${job.lastRunTime ? formatRelativeTime(job.lastRunTime) : '-'}</td>
            <td><span class="badge bg-${getStatusBadgeColor(job.status)}">${escapeHtml(job.status)}</span></td>
            <td onclick="event.stopPropagation()">
                <button class="btn btn-sm btn-outline-primary me-1" onclick="triggerJob('${escapeHtml(job.id)}')" title="Trigger now">
                    <i class="ti ti-player-play"></i>
                </button>
                <button class="btn btn-sm btn-primary me-1" onclick="openEditModal('${escapeHtml(job.id)}')" title="Edit">
                    <i class="ti ti-edit"></i>
                </button>
                <button class="btn btn-sm btn-${job.status === 'PAUSED' ? 'success' : 'warning'} me-1"
                        onclick="pauseResumeJob('${escapeHtml(job.id)}', '${escapeHtml(job.status)}')"
                        title="${job.status === 'PAUSED' ? 'Resume' : 'Pause'}">
                    <i class="ti ti-${job.status === 'PAUSED' ? 'player-play' : 'player-pause'}"></i>
                </button>
                <button class="btn btn-sm btn-danger" onclick="confirmDeleteJob('${escapeHtml(job.id)}', '${escapeHtml(job.name)}')" title="Delete">
                    <i class="ti ti-trash"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

function renderPagination(page, totalPages, totalElements) {
    const container = document.getElementById('pagination');
    if (totalPages <= 1) {
        container.innerHTML = `<p class="m-0 text-muted">${totalElements} job${totalElements !== 1 ? 's' : ''}</p>`;
        return;
    }

    let html = `<p class="m-0 text-muted me-auto">${totalElements} jobs</p><ul class="pagination m-0 ms-auto">`;
    html += `<li class="page-item ${page === 0 ? 'disabled' : ''}">
        <a class="page-link" href="#" onclick="loadJobs(${page - 1}); return false;">prev</a></li>`;
    for (let i = 0; i < totalPages; i++) {
        if (i === page || i === 0 || i === totalPages - 1 || Math.abs(i - page) <= 1) {
            html += `<li class="page-item ${i === page ? 'active' : ''}">
                <a class="page-link" href="#" onclick="loadJobs(${i}); return false;">${i + 1}</a></li>`;
        } else if (Math.abs(i - page) === 2) {
            html += `<li class="page-item disabled"><span class="page-link">…</span></li>`;
        }
    }
    html += `<li class="page-item ${page >= totalPages - 1 ? 'disabled' : ''}">
        <a class="page-link" href="#" onclick="loadJobs(${page + 1}); return false;">next</a></li>`;
    html += '</ul>';
    container.innerHTML = html;
}

function clearFilters() {
    document.getElementById('filterStatus').value = '';
    document.getElementById('filterType').value = '';
    document.getElementById('searchInput').value = '';
    loadJobs(0);
}

function debounceSearch() {
    clearTimeout(searchDebounceTimer);
    searchDebounceTimer = setTimeout(() => loadJobs(0), 300);
}

// ─── Type / Status Badges ─────────────────────────────────────────────────────

function getTypeBadgeColor(type) {
    const map = { PROGRAM: 'orange', SQL: 'blue', REST: 'green', SOAP: 'purple' };
    return map[type] || 'secondary';
}

function getStatusBadgeColor(status) {
    const map = { ACTIVE: 'success', PAUSED: 'warning', DISABLED: 'secondary' };
    return map[status] || 'secondary';
}

function getRunStatusBadgeColor(status) {
    const map = { SUCCESS: 'success', FAILURE: 'danger', TIMEOUT: 'warning', RUNNING: 'azure', SKIPPED: 'secondary' };
    return map[status] || 'secondary';
}

// ─── Wizard Modal ─────────────────────────────────────────────────────────────

function openCreateModal() {
    currentJobId = null;
    currentStep = 1;
    document.getElementById('jobId').value = '';
    document.getElementById('jobName').value = '';
    document.getElementById('jobDescription').value = '';
    document.getElementById('jobTags').value = '';
    document.getElementById('cronExpression').value = '0 0 9 * * *';
    document.getElementById('cronPreview').style.display = 'none';
    document.getElementById('cronError').style.display = 'none';
    document.getElementById('step3Content').innerHTML = '';
    // Default to PROGRAM type
    const radios = document.querySelectorAll('input[name="jobType"]');
    radios.forEach(r => r.checked = r.value === 'PROGRAM');
    document.getElementById('jobModalTitle').textContent = 'New Job';
    showStep(1);
    jobModal.show();
}

async function openEditModal(jobId) {
    try {
        const job = await API.get(`/api/scheduler/jobs/${jobId}`);
        currentJobId = jobId;
        currentStep = 1;

        document.getElementById('jobId').value = job.id;
        document.getElementById('jobName').value = job.name || '';
        document.getElementById('jobDescription').value = job.description || '';
        document.getElementById('jobTags').value = job.tags ? job.tags.join(', ') : '';
        document.getElementById('cronExpression').value = job.cronExpression || '';

        const radios = document.querySelectorAll('input[name="jobType"]');
        radios.forEach(r => r.checked = r.value === job.jobType);

        // Store config for step 3
        document.getElementById('jobModal').dataset.jobConfig = JSON.stringify(job.config || {});

        document.getElementById('jobModalTitle').textContent = 'Edit Job';
        showStep(1);
        jobModal.show();
    } catch (error) {
        console.error('Failed to load job:', error);
        showError('Failed to load job details');
    }
}

function showStep(step) {
    currentStep = step;
    document.getElementById('step1').style.display = step === 1 ? '' : 'none';
    document.getElementById('step2').style.display = step === 2 ? '' : 'none';
    document.getElementById('step3').style.display = step === 3 ? '' : 'none';

    ['wizardStep1Nav', 'wizardStep2Nav', 'wizardStep3Nav'].forEach((id, i) => {
        const el = document.getElementById(id);
        el.classList.remove('active', 'completed');
        if (i + 1 < step) el.classList.add('completed');
        if (i + 1 === step) el.classList.add('active');
    });

    document.getElementById('btnBack').style.display = step > 1 ? '' : 'none';
    document.getElementById('btnNext').style.display = step < 3 ? '' : 'none';
    document.getElementById('btnSave').style.display = step === 3 ? '' : 'none';
}

async function nextStep() {
    if (currentStep === 1) {
        const name = document.getElementById('jobName').value.trim();
        if (!name) { showError('Job name is required'); return; }
        showStep(2);
    } else if (currentStep === 2) {
        const cron = document.getElementById('cronExpression').value.trim();
        if (!cron) { showError('Cron expression is required'); return; }
        await renderStep3();
        showStep(3);
    }
}

function prevStep() {
    if (currentStep > 1) showStep(currentStep - 1);
}

async function renderStep3() {
    const jobType = document.querySelector('input[name="jobType"]:checked')?.value || 'PROGRAM';
    const existingConfig = (() => {
        try {
            return JSON.parse(document.getElementById('jobModal').dataset.jobConfig || '{}');
        } catch { return {}; }
    })();

    let html = '';
    if (jobType === 'PROGRAM') {
        html = renderProgramForm(existingConfig);
    } else if (jobType === 'SQL') {
        html = await renderSqlFormAsync(existingConfig);
    } else if (jobType === 'REST') {
        html = renderRestForm(existingConfig);
    } else if (jobType === 'SOAP') {
        html = renderSoapForm(existingConfig);
    }
    document.getElementById('step3Content').innerHTML = html;
}

function renderProgramForm(config) {
    return `
        <div class="mb-3">
            <label class="form-label required">Command</label>
            <input type="text" class="form-control font-monospace" id="cfgCommand"
                value="${escapeHtml(config.command || '')}" placeholder="/usr/bin/python3 /opt/scripts/run.py">
        </div>
        <div class="mb-3">
            <label class="form-label">Arguments</label>
            <input type="text" class="form-control font-monospace" id="cfgArgs"
                value="${escapeHtml(config.args || '')}" placeholder="--env production --verbose">
        </div>
        <div class="row">
            <div class="col-md-6 mb-3">
                <label class="form-label">Working Directory</label>
                <input type="text" class="form-control font-monospace" id="cfgWorkDir"
                    value="${escapeHtml(config.workingDirectory || '')}" placeholder="/opt/scripts">
            </div>
            <div class="col-md-6 mb-3">
                <label class="form-label">Timeout (seconds)</label>
                <input type="number" class="form-control" id="cfgTimeout"
                    value="${config.timeoutSeconds || 300}" min="1">
            </div>
        </div>
        <div class="mb-3">
            <label class="form-check form-switch">
                <input type="checkbox" class="form-check-input" id="cfgShellWrap" ${config.shellWrap ? 'checked' : ''}>
                <span class="form-check-label">Wrap in shell (bash -c)</span>
            </label>
        </div>
        <div class="mb-3">
            <label class="form-label">Environment Variables</label>
            <textarea class="form-control font-monospace" id="cfgEnvVars" rows="3"
                placeholder="KEY=value&#10;ANOTHER_KEY=another_value">${escapeHtml(config.envVars ? Object.entries(config.envVars).map(([k,v]) => `${k}=${v}`).join('\n') : '')}</textarea>
            <small class="text-muted">One KEY=value per line</small>
        </div>
    `;
}

async function renderSqlFormAsync(config) {
    let datasourceOptions = '<option value="">-- Select Datasource --</option>';
    try {
        const datasources = await API.get('/api/scheduler/datasources');
        const list = datasources.content || datasources;
        if (list && list.length > 0) {
            datasourceOptions += list.map(ds =>
                `<option value="${escapeHtml(ds.id)}" ${config.datasourceId === ds.id ? 'selected' : ''}>${escapeHtml(ds.name)}</option>`
            ).join('');
        }
    } catch (e) {
        console.error('Failed to load datasources:', e);
    }

    return `
        <div class="mb-3">
            <label class="form-label required">Datasource</label>
            <select class="form-select" id="cfgDatasourceId">
                ${datasourceOptions}
            </select>
            <small class="text-muted">
                <a href="/admin/scheduler/datasources" target="_blank">Manage datasources</a>
            </small>
        </div>
        <div class="mb-3">
            <label class="form-label required">SQL Query</label>
            <textarea class="form-control font-monospace" id="cfgSql" rows="8"
                placeholder="SELECT * FROM my_table WHERE status = 'pending';">${escapeHtml(config.sql || '')}</textarea>
        </div>
        <div class="row">
            <div class="col-md-6 mb-3">
                <label class="form-label">Timeout (seconds)</label>
                <input type="number" class="form-control" id="cfgTimeout"
                    value="${config.timeoutSeconds || 60}" min="1">
            </div>
            <div class="col-md-6 mb-3">
                <label class="form-label">Max Rows</label>
                <input type="number" class="form-control" id="cfgMaxRows"
                    value="${config.maxRows || 1000}" min="1">
            </div>
        </div>
    `;
}

function renderRestForm(config) {
    const method = config.method || 'GET';
    const methods = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'];
    const headersJson = config.headers ? JSON.stringify(config.headers, null, 2) : '';

    return `
        <ul class="nav nav-tabs mb-3" id="restTabs">
            <li class="nav-item"><a class="nav-link active" href="#" onclick="showRestTab('basic'); return false;">Basic</a></li>
            <li class="nav-item"><a class="nav-link" href="#" onclick="showRestTab('headers'); return false;">Headers</a></li>
            <li class="nav-item"><a class="nav-link" href="#" onclick="showRestTab('body'); return false;">Body</a></li>
            <li class="nav-item"><a class="nav-link" href="#" onclick="showRestTab('auth'); return false;">Auth</a></li>
            <li class="nav-item"><a class="nav-link" href="#" onclick="showRestTab('advanced'); return false;">Advanced</a></li>
        </ul>
        <div id="restTabBasic">
            <div class="row">
                <div class="col-md-3 mb-3">
                    <label class="form-label required">Method</label>
                    <select class="form-select" id="cfgMethod">
                        ${methods.map(m => `<option value="${m}" ${m === method ? 'selected' : ''}>${m}</option>`).join('')}
                    </select>
                </div>
                <div class="col-md-9 mb-3">
                    <label class="form-label required">URL</label>
                    <input type="url" class="form-control font-monospace" id="cfgUrl"
                        value="${escapeHtml(config.url || '')}" placeholder="https://api.example.com/endpoint">
                </div>
            </div>
            <div class="row">
                <div class="col-md-6 mb-3">
                    <label class="form-label">Timeout (seconds)</label>
                    <input type="number" class="form-control" id="cfgTimeout"
                        value="${config.timeoutSeconds || 30}" min="1">
                </div>
                <div class="col-md-6 mb-3">
                    <label class="form-label">Expected Status Code</label>
                    <input type="number" class="form-control" id="cfgExpectedStatus"
                        value="${config.expectedStatus || 200}">
                </div>
            </div>
            <div class="mb-3">
                <label class="form-check form-switch">
                    <input type="checkbox" class="form-check-input" id="cfgFollowRedirects" ${config.followRedirects !== false ? 'checked' : ''}>
                    <span class="form-check-label">Follow redirects</span>
                </label>
            </div>
        </div>
        <div id="restTabHeaders" style="display:none;">
            <div class="mb-3">
                <label class="form-label">Request Headers (JSON)</label>
                <textarea class="form-control font-monospace" id="cfgHeaders" rows="6"
                    placeholder='{"Content-Type": "application/json", "Accept": "application/json"}'>${escapeHtml(headersJson)}</textarea>
            </div>
        </div>
        <div id="restTabBody" style="display:none;">
            <div class="mb-3">
                <label class="form-label">Content-Type</label>
                <select class="form-select" id="cfgContentType">
                    <option value="" ${!config.contentType ? 'selected' : ''}>None</option>
                    <option value="application/json" ${config.contentType === 'application/json' ? 'selected' : ''}>application/json</option>
                    <option value="application/x-www-form-urlencoded" ${config.contentType === 'application/x-www-form-urlencoded' ? 'selected' : ''}>application/x-www-form-urlencoded</option>
                    <option value="text/plain" ${config.contentType === 'text/plain' ? 'selected' : ''}>text/plain</option>
                    <option value="application/xml" ${config.contentType === 'application/xml' ? 'selected' : ''}>application/xml</option>
                </select>
            </div>
            <div class="mb-3">
                <label class="form-label">Request Body</label>
                <textarea class="form-control font-monospace" id="cfgBody" rows="8"
                    placeholder='{"key": "value"}'>${escapeHtml(config.body || '')}</textarea>
            </div>
        </div>
        <div id="restTabAuth" style="display:none;">
            <div class="mb-3">
                <label class="form-label">Auth Type</label>
                <select class="form-select" id="cfgAuthType" onchange="toggleRestAuthFields()">
                    <option value="NONE" ${!config.authType || config.authType === 'NONE' ? 'selected' : ''}>None</option>
                    <option value="BASIC" ${config.authType === 'BASIC' ? 'selected' : ''}>Basic Auth</option>
                    <option value="BEARER" ${config.authType === 'BEARER' ? 'selected' : ''}>Bearer Token</option>
                    <option value="API_KEY" ${config.authType === 'API_KEY' ? 'selected' : ''}>API Key</option>
                </select>
            </div>
            <div id="restAuthBasic" style="display:none;">
                <div class="row">
                    <div class="col-md-6 mb-3">
                        <label class="form-label">Username</label>
                        <input type="text" class="form-control" id="cfgAuthUser" value="${escapeHtml(config.authUsername || '')}">
                    </div>
                    <div class="col-md-6 mb-3">
                        <label class="form-label">Password</label>
                        <input type="password" class="form-control" id="cfgAuthPassword">
                    </div>
                </div>
            </div>
            <div id="restAuthBearer" style="display:none;">
                <div class="mb-3">
                    <label class="form-label">Bearer Token</label>
                    <input type="text" class="form-control font-monospace" id="cfgAuthToken" value="${escapeHtml(config.authToken || '')}">
                </div>
            </div>
            <div id="restAuthApiKey" style="display:none;">
                <div class="row">
                    <div class="col-md-4 mb-3">
                        <label class="form-label">Header Name</label>
                        <input type="text" class="form-control" id="cfgApiKeyHeader" value="${escapeHtml(config.apiKeyHeader || 'X-API-Key')}">
                    </div>
                    <div class="col-md-8 mb-3">
                        <label class="form-label">API Key Value</label>
                        <input type="text" class="form-control font-monospace" id="cfgApiKey" value="${escapeHtml(config.apiKey || '')}">
                    </div>
                </div>
            </div>
        </div>
        <div id="restTabAdvanced" style="display:none;">
            <div class="mb-3">
                <label class="form-check form-switch">
                    <input type="checkbox" class="form-check-input" id="cfgVerifySsl" ${config.verifySsl !== false ? 'checked' : ''}>
                    <span class="form-check-label">Verify SSL certificate</span>
                </label>
            </div>
            <div class="mb-3">
                <label class="form-label">Proxy URL</label>
                <input type="text" class="form-control font-monospace" id="cfgProxyUrl"
                    value="${escapeHtml(config.proxyUrl || '')}" placeholder="http://proxy.example.com:8080">
            </div>
            <div class="mb-3">
                <label class="form-label">Success Condition (JSONPath / regex)</label>
                <input type="text" class="form-control font-monospace" id="cfgSuccessCondition"
                    value="${escapeHtml(config.successCondition || '')}" placeholder="$.status == 'ok'">
            </div>
        </div>
    `;
}

function renderSoapForm(config) {
    return `
        <div class="mb-3">
            <label class="form-label required">Endpoint URL</label>
            <input type="url" class="form-control font-monospace" id="cfgUrl"
                value="${escapeHtml(config.url || '')}" placeholder="https://api.example.com/service?wsdl">
        </div>
        <div class="mb-3">
            <label class="form-label">SOAPAction Header</label>
            <input type="text" class="form-control font-monospace" id="cfgSoapAction"
                value="${escapeHtml(config.soapAction || '')}" placeholder="http://example.com/service/MyOperation">
        </div>
        <div class="mb-3">
            <label class="form-label required">SOAP Envelope (XML)</label>
            <textarea class="form-control font-monospace" id="cfgEnvelope" rows="10"
                placeholder="&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;&#10;&lt;soapenv:Envelope ...&gt;...&lt;/soapenv:Envelope&gt;">${escapeHtml(config.envelope || '')}</textarea>
        </div>
        <div class="row">
            <div class="col-md-4 mb-3">
                <label class="form-label">Auth Type</label>
                <select class="form-select" id="cfgSoapAuthType">
                    <option value="NONE" ${!config.authType || config.authType === 'NONE' ? 'selected' : ''}>None</option>
                    <option value="BASIC" ${config.authType === 'BASIC' ? 'selected' : ''}>Basic Auth</option>
                    <option value="WS_SECURITY" ${config.authType === 'WS_SECURITY' ? 'selected' : ''}>WS-Security</option>
                </select>
            </div>
            <div class="col-md-4 mb-3">
                <label class="form-label">Username</label>
                <input type="text" class="form-control" id="cfgSoapUser" value="${escapeHtml(config.authUsername || '')}">
            </div>
            <div class="col-md-4 mb-3">
                <label class="form-label">Password</label>
                <input type="password" class="form-control" id="cfgSoapPassword">
            </div>
        </div>
        <div class="mb-3">
            <label class="form-label">Timeout (seconds)</label>
            <input type="number" class="form-control" id="cfgTimeout"
                value="${config.timeoutSeconds || 60}" min="1">
        </div>
    `;
}

// REST tab switching
function showRestTab(tab) {
    ['basic', 'headers', 'body', 'auth', 'advanced'].forEach(t => {
        const el = document.getElementById(`restTab${t.charAt(0).toUpperCase() + t.slice(1)}`);
        if (el) el.style.display = t === tab ? '' : 'none';
    });
    document.querySelectorAll('#restTabs .nav-link').forEach((link, i) => {
        const tabs = ['basic', 'headers', 'body', 'auth', 'advanced'];
        link.classList.toggle('active', tabs[i] === tab);
    });
}

function toggleRestAuthFields() {
    const type = document.getElementById('cfgAuthType')?.value || 'NONE';
    const basic = document.getElementById('restAuthBasic');
    const bearer = document.getElementById('restAuthBearer');
    const apiKey = document.getElementById('restAuthApiKey');
    if (basic) basic.style.display = type === 'BASIC' ? '' : 'none';
    if (bearer) bearer.style.display = type === 'BEARER' ? '' : 'none';
    if (apiKey) apiKey.style.display = type === 'API_KEY' ? '' : 'none';
}

// ─── Cron Preview ─────────────────────────────────────────────────────────────

function setCronPreset(expr) {
    document.getElementById('cronExpression').value = expr;
    previewCron();
}

function debounceCronPreview() {
    clearTimeout(cronPreviewTimer);
    cronPreviewTimer = setTimeout(previewCron, 300);
}

async function previewCron() {
    const expr = document.getElementById('cronExpression').value.trim();
    const timezone = document.getElementById('cronTimezone').value || 'UTC';
    const previewDiv = document.getElementById('cronPreview');
    const errorDiv = document.getElementById('cronError');

    if (!expr) {
        previewDiv.style.display = 'none';
        errorDiv.style.display = 'none';
        return;
    }

    try {
        const result = await API.post('/api/scheduler/cron/preview', { expression: expr, timezone });
        errorDiv.style.display = 'none';
        if (result && result.nextRuns && result.nextRuns.length > 0) {
            document.getElementById('cronPreviewList').innerHTML =
                result.nextRuns.map(dt => `<li>${escapeHtml(dt)}</li>`).join('');
            previewDiv.style.display = '';
        } else {
            previewDiv.style.display = 'none';
        }
    } catch (error) {
        previewDiv.style.display = 'none';
        errorDiv.style.display = '';
        errorDiv.textContent = error.message || 'Invalid cron expression';
    }
}

async function loadTimezones() {
    try {
        const timezones = await API.get('/api/scheduler/cron/timezones');
        const select = document.getElementById('cronTimezone');
        const current = select.value || 'UTC';
        select.innerHTML = (timezones || []).map(tz =>
            `<option value="${escapeHtml(tz)}" ${tz === current ? 'selected' : ''}>${escapeHtml(tz)}</option>`
        ).join('');
        if (!select.value) select.value = 'UTC';
    } catch (e) {
        console.warn('Could not load timezones, using default list');
        // Keep the default UTC option
    }
}

// ─── Save Job ─────────────────────────────────────────────────────────────────

async function saveJob() {
    const id = document.getElementById('jobId').value;
    const jobType = document.querySelector('input[name="jobType"]:checked')?.value || 'PROGRAM';
    const config = collectConfig(jobType);

    const job = {
        name: document.getElementById('jobName').value.trim(),
        description: document.getElementById('jobDescription').value.trim() || null,
        jobType,
        cronExpression: document.getElementById('cronExpression').value.trim(),
        timezone: document.getElementById('cronTimezone').value || 'UTC',
        tags: document.getElementById('jobTags').value
            .split(',').map(t => t.trim()).filter(t => t.length > 0),
        config
    };

    if (!job.name || !job.cronExpression) {
        showError('Name and cron expression are required');
        return;
    }

    try {
        if (id) {
            await API.put(`/api/scheduler/jobs/${id}`, job);
            showSuccess('Job updated successfully');
        } else {
            await API.post('/api/scheduler/jobs', job);
            showSuccess('Job created successfully');
        }
        jobModal.hide();
        loadJobs(currentPage);
        loadStats();
    } catch (error) {
        console.error('Error saving job:', error);
        showError(error.message || 'Failed to save job');
    }
}

function collectConfig(jobType) {
    const cfg = {};
    if (jobType === 'PROGRAM') {
        cfg.command = document.getElementById('cfgCommand')?.value?.trim() || '';
        cfg.args = document.getElementById('cfgArgs')?.value?.trim() || '';
        cfg.workingDirectory = document.getElementById('cfgWorkDir')?.value?.trim() || null;
        cfg.timeoutSeconds = parseInt(document.getElementById('cfgTimeout')?.value) || 300;
        cfg.shellWrap = document.getElementById('cfgShellWrap')?.checked || false;
        const envText = document.getElementById('cfgEnvVars')?.value || '';
        cfg.envVars = {};
        envText.split('\n').forEach(line => {
            const idx = line.indexOf('=');
            if (idx > 0) {
                cfg.envVars[line.substring(0, idx).trim()] = line.substring(idx + 1).trim();
            }
        });
    } else if (jobType === 'SQL') {
        cfg.datasourceId = document.getElementById('cfgDatasourceId')?.value || null;
        cfg.sql = document.getElementById('cfgSql')?.value?.trim() || '';
        cfg.timeoutSeconds = parseInt(document.getElementById('cfgTimeout')?.value) || 60;
        cfg.maxRows = parseInt(document.getElementById('cfgMaxRows')?.value) || 1000;
    } else if (jobType === 'REST') {
        cfg.method = document.getElementById('cfgMethod')?.value || 'GET';
        cfg.url = document.getElementById('cfgUrl')?.value?.trim() || '';
        cfg.timeoutSeconds = parseInt(document.getElementById('cfgTimeout')?.value) || 30;
        cfg.expectedStatus = parseInt(document.getElementById('cfgExpectedStatus')?.value) || 200;
        cfg.followRedirects = document.getElementById('cfgFollowRedirects')?.checked !== false;
        cfg.verifySsl = document.getElementById('cfgVerifySsl')?.checked !== false;
        cfg.proxyUrl = document.getElementById('cfgProxyUrl')?.value?.trim() || null;
        cfg.successCondition = document.getElementById('cfgSuccessCondition')?.value?.trim() || null;
        cfg.contentType = document.getElementById('cfgContentType')?.value || null;
        cfg.body = document.getElementById('cfgBody')?.value?.trim() || null;
        cfg.authType = document.getElementById('cfgAuthType')?.value || 'NONE';
        cfg.authUsername = document.getElementById('cfgAuthUser')?.value?.trim() || null;
        cfg.authToken = document.getElementById('cfgAuthToken')?.value?.trim() || null;
        cfg.apiKeyHeader = document.getElementById('cfgApiKeyHeader')?.value?.trim() || null;
        cfg.apiKey = document.getElementById('cfgApiKey')?.value?.trim() || null;
        try {
            const headersStr = document.getElementById('cfgHeaders')?.value?.trim();
            cfg.headers = headersStr ? JSON.parse(headersStr) : null;
        } catch (e) { cfg.headers = null; }
    } else if (jobType === 'SOAP') {
        cfg.url = document.getElementById('cfgUrl')?.value?.trim() || '';
        cfg.soapAction = document.getElementById('cfgSoapAction')?.value?.trim() || null;
        cfg.envelope = document.getElementById('cfgEnvelope')?.value?.trim() || '';
        cfg.authType = document.getElementById('cfgSoapAuthType')?.value || 'NONE';
        cfg.authUsername = document.getElementById('cfgSoapUser')?.value?.trim() || null;
        cfg.timeoutSeconds = parseInt(document.getElementById('cfgTimeout')?.value) || 60;
    }
    return cfg;
}

// ─── Trigger / Pause / Resume ─────────────────────────────────────────────────

async function triggerJob(jobId) {
    if (!confirm('Trigger this job now?')) return;
    try {
        await API.post(`/api/scheduler/jobs/${jobId}/trigger`, {});
        showSuccess('Job triggered successfully');
        setTimeout(() => loadStats(), 1500);
    } catch (error) {
        console.error('Failed to trigger job:', error);
        showError(error.message || 'Failed to trigger job');
    }
}

async function pauseResumeJob(jobId, currentStatus) {
    const isPaused = currentStatus === 'PAUSED';
    try {
        await API.post(`/api/scheduler/jobs/${jobId}/${isPaused ? 'resume' : 'pause'}`, {});
        showSuccess(`Job ${isPaused ? 'resumed' : 'paused'} successfully`);
        loadJobs(currentPage);
    } catch (error) {
        console.error('Failed to pause/resume job:', error);
        showError(error.message || 'Failed to update job status');
    }
}

// ─── Delete ───────────────────────────────────────────────────────────────────

function confirmDeleteJob(jobId, name) {
    document.getElementById('deleteMessage').textContent =
        `Do you really want to delete "${name}"? This action cannot be undone.`;
    deleteCallback = () => deleteJob(jobId);
    deleteModal.show();
}

async function deleteJob(jobId) {
    try {
        await API.delete(`/api/scheduler/jobs/${jobId}`);
        showSuccess('Job deleted successfully');
        loadJobs(currentPage);
        loadStats();
    } catch (error) {
        console.error('Failed to delete job:', error);
        showError(error.message || 'Failed to delete job');
    }
}

// ─── Run History ──────────────────────────────────────────────────────────────

async function openRunHistory(jobId, jobName) {
    runHistoryJobId = jobId;
    runHistoryPage = 0;
    document.getElementById('runHistoryJobName').textContent = jobName;
    document.getElementById('runsTable').innerHTML =
        '<tr><td colspan="7" class="text-center text-muted">Loading...</td></tr>';
    runHistoryModal.show();
    await loadRuns(0);
}

async function loadRuns(page) {
    runHistoryPage = page;
    try {
        const response = await API.get(`/api/scheduler/runs/job/${runHistoryJobId}?page=${page}&size=20`);
        const runs = response.content || response;
        const totalPages = response.totalPages || 1;
        renderRunsTable(runs || []);
        renderRunsPagination(page, totalPages);
    } catch (error) {
        console.error('Failed to load runs:', error);
        document.getElementById('runsTable').innerHTML =
            '<tr><td colspan="7" class="text-center text-danger">Failed to load run history</td></tr>';
    }
}

function renderRunsTable(runs) {
    const tbody = document.getElementById('runsTable');
    if (!runs || runs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">No runs found</td></tr>';
        return;
    }

    tbody.innerHTML = runs.map((run, idx) => `
        <tr>
            <td class="text-muted">${escapeHtml(String(run.id || idx + 1))}</td>
            <td>
                <span class="badge bg-${run.triggerType === 'MANUAL' ? 'blue' : 'secondary'}">
                    ${escapeHtml(run.triggerType || 'SCHEDULED')}
                </span>
            </td>
            <td>
                <span class="badge bg-${getRunStatusBadgeColor(run.status)}">
                    ${run.status === 'RUNNING' ? '<span class="spinner-border spinner-border-sm me-1"></span>' : ''}
                    ${escapeHtml(run.status)}
                </span>
            </td>
            <td class="text-muted small">${run.startedAt ? formatRelativeTime(run.startedAt) : '-'}</td>
            <td class="text-muted small">${run.durationMs != null ? formatDuration(run.durationMs) : '-'}</td>
            <td class="text-muted">${run.exitCode != null ? escapeHtml(String(run.exitCode)) : '-'}</td>
            <td>
                <button class="btn btn-sm btn-outline-secondary" onclick="viewOutput('${escapeHtml(run.id || run.runId)}')" title="View Output">
                    <i class="ti ti-eye"></i> Output
                </button>
            </td>
        </tr>
    `).join('');
}

function renderRunsPagination(page, totalPages) {
    const container = document.getElementById('runsPagination');
    if (totalPages <= 1) { container.innerHTML = ''; return; }
    let html = '<ul class="pagination m-0">';
    html += `<li class="page-item ${page === 0 ? 'disabled' : ''}">
        <a class="page-link" href="#" onclick="loadRuns(${page - 1}); return false;">prev</a></li>`;
    for (let i = 0; i < totalPages; i++) {
        html += `<li class="page-item ${i === page ? 'active' : ''}">
            <a class="page-link" href="#" onclick="loadRuns(${i}); return false;">${i + 1}</a></li>`;
    }
    html += `<li class="page-item ${page >= totalPages - 1 ? 'disabled' : ''}">
        <a class="page-link" href="#" onclick="loadRuns(${page + 1}); return false;">next</a></li>`;
    html += '</ul>';
    container.innerHTML = html;
}

// ─── Output Viewer ────────────────────────────────────────────────────────────

async function viewOutput(runId) {
    try {
        const run = await API.get(`/api/scheduler/runs/${runId}`);
        currentRunData = run;
        showOutputTab('stdout');
        outputModal.show();
    } catch (error) {
        console.error('Failed to load run output:', error);
        showError(error.message || 'Failed to load run output');
    }
}

function showOutputTab(tab) {
    const run = currentRunData;
    if (!run) return;

    // Update active tab
    document.querySelectorAll('#outputTabs .nav-link').forEach(link => {
        link.classList.remove('active');
        if (link.getAttribute('href') === '#' && link.textContent.toLowerCase().replace(/\s/g, '') === tab) {
            link.classList.add('active');
        }
    });
    // Simpler: re-render tabs active state
    const tabs = document.querySelectorAll('#outputTabs .nav-link');
    const tabNames = ['stdout', 'stderr', 'response'];
    tabs.forEach((link, i) => link.classList.toggle('active', tabNames[i] === tab));

    let content = '';
    if (tab === 'stdout') content = run.stdout || '(no stdout)';
    else if (tab === 'stderr') content = run.stderr || '(no stderr)';
    else if (tab === 'response') content = run.responseBody || run.result || '(no response)';

    document.getElementById('outputPre').textContent = content;
}

function copyOutput() {
    const text = document.getElementById('outputPre').textContent;
    navigator.clipboard.writeText(text).then(() => showSuccess('Copied to clipboard')).catch(() => {
        showError('Failed to copy');
    });
}

// ─── Utilities ────────────────────────────────────────────────────────────────

function formatRelativeTime(isoString) {
    if (!isoString) return '-';
    const date = new Date(isoString);
    const now = new Date();
    const diffMs = date - now;
    const absDiff = Math.abs(diffMs);

    if (absDiff < 60000) return diffMs < 0 ? 'just now' : 'in a moment';
    if (absDiff < 3600000) {
        const mins = Math.round(absDiff / 60000);
        return diffMs < 0 ? `${mins} minute${mins !== 1 ? 's' : ''} ago` : `in ${mins} minute${mins !== 1 ? 's' : ''}`;
    }
    if (absDiff < 86400000) {
        const hours = Math.round(absDiff / 3600000);
        return diffMs < 0 ? `${hours} hour${hours !== 1 ? 's' : ''} ago` : `in ${hours} hour${hours !== 1 ? 's' : ''}`;
    }
    const days = Math.round(absDiff / 86400000);
    return diffMs < 0 ? `${days} day${days !== 1 ? 's' : ''} ago` : `in ${days} day${days !== 1 ? 's' : ''}`;
}

function formatDuration(ms) {
    if (ms == null) return '-';
    if (ms < 1000) return `${ms}ms`;
    if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`;
    const mins = Math.floor(ms / 60000);
    const secs = Math.round((ms % 60000) / 1000);
    return `${mins}m ${secs}s`;
}

function truncate(text, maxLength) {
    if (!text) return '';
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
}

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
