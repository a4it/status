'use strict';

// ============================================================================
// State
// ============================================================================
const wizardState = {
    currentStep: 1,
    tenantId: null,
    organizationId: null,
    propertiesChanged: false,
    propertiesData: null  // cached from GET /api/setup/properties
};

// ============================================================================
// Boot
// ============================================================================
document.addEventListener('DOMContentLoaded', () => {
    bindButtons();
    loadStatus();
});

// ============================================================================
// Navigation
// ============================================================================
function showStep(n) {
    document.querySelectorAll('.step-panel').forEach(p => p.classList.add('d-none'));
    const panel = document.getElementById(`step-${n}`);
    if (panel) panel.classList.remove('d-none');

    // In Tabler's steps component: only the CURRENT step gets 'active'.
    // CSS rule .step-item.active~.step-item grays out everything after it automatically.
    // Steps before the current one render in the primary color (completed look) by default.
    for (let i = 1; i <= 6; i++) {
        const ind = document.getElementById(`step-indicator-${i}`);
        if (!ind) continue;
        ind.classList.remove('active');
        if (i === n) ind.classList.add('active');
    }

    wizardState.currentStep = n;
    window.scrollTo({ top: 0, behavior: 'smooth' });

    if (n === 5) loadProperties();
}

// ============================================================================
// Button bindings
// ============================================================================
function bindButtons() {
    // Step 1
    document.getElementById('step1-next').addEventListener('click', () => showStep(2));

    // Step 2
    document.getElementById('step2-back').addEventListener('click', () => showStep(1));
    document.getElementById('step2-next').addEventListener('click', handleStep2);

    // Step 3
    document.getElementById('step3-back').addEventListener('click', () => showStep(2));
    document.getElementById('step3-next').addEventListener('click', handleStep3);

    // Step 4
    document.getElementById('step4-back').addEventListener('click', () => showStep(3));
    document.getElementById('step4-next').addEventListener('click', handleStep4);

    // Step 5
    document.getElementById('step5-skip').addEventListener('click', () => showStep(6));
    document.getElementById('step5-save').addEventListener('click', handleStep5Save);
    document.getElementById('downloadPropsBtn').addEventListener('click', handleDownload);

    // Password toggle
    document.getElementById('togglePw').addEventListener('click', (e) => {
        e.preventDefault();
        const pw = document.getElementById('adminPassword');
        const icon = document.getElementById('togglePw').querySelector('i');
        if (pw.type === 'password') {
            pw.type = 'text';
            icon.className = 'ti ti-eye-off';
        } else {
            pw.type = 'password';
            icon.className = 'ti ti-eye';
        }
    });
}

// ============================================================================
// Step 1: load status
// ============================================================================
async function loadStatus() {
    try {
        const status = await apiGet('/api/setup/status');

        // DB status
        if (status.dbConnected) {
            setStatusBadge('db-status', true);
            setDetail('db-detail', 'Connected successfully');
        } else {
            setStatusBadge('db-status', false);
            setDetail('db-detail', status.dbError || 'Connection failed');
        }

        // Flyway status
        setStatusBadge('flyway-status', status.flywayVersion !== 'unknown');
        setDetail('flyway-detail', status.flywayVersion
            ? `Schema version ${status.flywayVersion}`
            : 'No migrations applied');

        // If setup was already completed (e.g. page refresh after step 4), jump to done
        if (status.setupCompleted) {
            showStep(6);
            return;
        }

        // Enable continue when DB is connected
        if (status.dbConnected) {
            document.getElementById('step1-next').disabled = false;
        }

        // Resumability: pre-load saved IDs
        if (status.tenantCreated && status.tenantId) {
            wizardState.tenantId = status.tenantId;
        }
        if (status.organizationCreated && status.organizationId) {
            wizardState.organizationId = status.organizationId;
        }
        if (status.tenantCreated || status.organizationCreated) {
            document.getElementById('step1-resume-notice').classList.remove('d-none');
        }

    } catch (err) {
        setStatusBadge('db-status', false);
        setDetail('db-detail', 'Could not reach server');
        setStatusBadge('flyway-status', false);
        setDetail('flyway-detail', 'Unknown');
    }
}

function setStatusBadge(id, ok) {
    const el = document.getElementById(id);
    el.innerHTML = ok
        ? '<span class="badge bg-success"><i class="ti ti-check me-1"></i>OK</span>'
        : '<span class="badge bg-danger"><i class="ti ti-x me-1"></i>Error</span>';
}

function setDetail(id, text) {
    const el = document.getElementById(id);
    if (el) el.textContent = text;
}

// ============================================================================
// Step 2: create tenant
// ============================================================================
async function handleStep2() {
    const name = document.getElementById('tenantName').value.trim();
    if (!name) { showStepError(2, 'Tenant name is required.'); return; }

    // If tenant already created (resume), skip creation
    if (wizardState.tenantId) {
        showStep(3);
        return;
    }

    setLoading('step2-next', true);
    clearStepError(2);
    try {
        const tenant = await apiPost('/api/setup/tenant', { name });
        wizardState.tenantId = tenant.id;
        showStep(3);
    } catch (err) {
        showStepError(2, err.message || 'Failed to create tenant. Please try again.');
    } finally {
        setLoading('step2-next', false);
    }
}

// ============================================================================
// Step 3: create organization
// ============================================================================
async function handleStep3() {
    const name = document.getElementById('orgName').value.trim();
    const email = document.getElementById('orgEmail').value.trim();
    const organizationType = document.getElementById('orgType').value;

    if (!name) { showStepError(3, 'Organization name is required.'); return; }
    if (!wizardState.tenantId) { showStepError(3, 'Tenant not found. Please go back and create the tenant first.'); return; }

    // If org already created (resume), skip creation
    if (wizardState.organizationId) {
        showStep(4);
        return;
    }

    setLoading('step3-next', true);
    clearStepError(3);
    try {
        const org = await apiPost('/api/setup/organization', {
            name,
            email: email || null,
            organizationType,
            tenantId: wizardState.tenantId
        });
        wizardState.organizationId = org.id;
        showStep(4);
    } catch (err) {
        showStepError(3, err.message || 'Failed to create organization. Please try again.');
    } finally {
        setLoading('step3-next', false);
    }
}

// ============================================================================
// Step 4: create superadmin + mark setup complete
// ============================================================================
async function handleStep4() {
    const username = document.getElementById('adminUsername').value.trim();
    const fullName = document.getElementById('adminFullName').value.trim();
    const email = document.getElementById('adminEmail').value.trim();
    const password = document.getElementById('adminPassword').value;
    const passwordConfirm = document.getElementById('adminPasswordConfirm').value;

    if (!username) { showStepError(4, 'Username is required.'); return; }
    if (!fullName) { showStepError(4, 'Full name is required.'); return; }
    if (!email) { showStepError(4, 'Email is required.'); return; }
    if (password.length < 8) { showStepError(4, 'Password must be at least 8 characters.'); return; }
    if (password !== passwordConfirm) { showStepError(4, 'Passwords do not match.'); return; }
    if (!wizardState.organizationId) { showStepError(4, 'Organization not found. Please go back and create the organization first.'); return; }

    setLoading('step4-next', true);
    clearStepError(4);
    try {
        const result = await apiPost('/api/setup/admin', {
            username,
            fullName,
            email,
            password,
            organizationId: wizardState.organizationId
        });
        if (!result.success) {
            showStepError(4, result.message || 'Failed to create admin account.');
            return;
        }
        showStep(5);
    } catch (err) {
        showStepError(4, err.message || 'Failed to create admin account. Please try again.');
    } finally {
        setLoading('step4-next', false);
    }
}

// ============================================================================
// Step 5: load and save properties
// ============================================================================
async function loadProperties() {
    document.getElementById('props-loading').classList.remove('d-none');
    document.getElementById('props-content').classList.add('d-none');
    document.getElementById('props-error').classList.add('d-none');

    try {
        const groups = await apiGet('/api/setup/properties');
        wizardState.propertiesData = groups;
        renderProperties(groups);
        document.getElementById('props-loading').classList.add('d-none');
        document.getElementById('props-content').classList.remove('d-none');
    } catch (err) {
        document.getElementById('props-loading').classList.add('d-none');
        document.getElementById('props-error').classList.remove('d-none');
    }
}

function renderProperties(groups) {
    const container = document.getElementById('props-content');
    container.innerHTML = '';

    const groupKeys = Object.keys(groups);
    groupKeys.forEach((groupName, idx) => {
        const entries = groups[groupName];
        const isFirst = idx === 0;
        const collapseId = `props-collapse-${idx}`;

        const section = document.createElement('div');
        section.className = 'accordion-item';
        section.innerHTML = `
            <h2 class="accordion-header">
                <button class="accordion-button ${isFirst ? '' : 'collapsed'}" type="button"
                        data-bs-toggle="collapse" data-bs-target="#${collapseId}">
                    ${groupName}
                    <span class="badge bg-secondary ms-2">${entries.length}</span>
                </button>
            </h2>
            <div id="${collapseId}" class="accordion-collapse collapse ${isFirst ? 'show' : ''}">
                <div class="accordion-body p-0">
                    <table class="table table-sm mb-0">
                        <tbody id="props-body-${idx}"></tbody>
                    </table>
                </div>
            </div>
        `;
        container.appendChild(section);

        const tbody = section.querySelector(`#props-body-${idx}`);
        entries.forEach(entry => {
            const tr = document.createElement('tr');
            const inputType = entry.sensitive ? 'password' : 'text';
            tr.innerHTML = `
                <td class="py-2 ps-3" style="min-width:280px; vertical-align:top;">
                    <code class="text-body">${escapeHtml(entry.key)}</code>
                    ${entry.description ? `<div class="text-muted small">${escapeHtml(entry.description)}</div>` : ''}
                </td>
                <td class="py-2 pe-3" style="width:100%;">
                    <div class="input-group input-group-sm ${entry.sensitive ? 'input-group-flat' : ''}">
                        <input type="${inputType}" class="form-control form-control-sm prop-input"
                               data-key="${escapeHtml(entry.key)}"
                               value="${escapeHtml(entry.value)}"
                               autocomplete="off">
                        ${entry.sensitive ? `
                        <span class="input-group-text">
                            <a href="#" class="link-secondary toggle-sensitive" title="Toggle visibility">
                                <i class="ti ti-eye"></i>
                            </a>
                        </span>` : ''}
                    </div>
                </td>
            `;
            tbody.appendChild(tr);
        });
    });

    // Bind toggle for sensitive fields
    container.querySelectorAll('.toggle-sensitive').forEach(link => {
        link.addEventListener('click', e => {
            e.preventDefault();
            const input = link.closest('.input-group').querySelector('input');
            const icon = link.querySelector('i');
            if (input.type === 'password') {
                input.type = 'text';
                icon.className = 'ti ti-eye-off';
            } else {
                input.type = 'password';
                icon.className = 'ti ti-eye';
            }
        });
    });
}

async function handleStep5Save() {
    const inputs = document.querySelectorAll('.prop-input');
    const properties = {};
    inputs.forEach(input => {
        properties[input.dataset.key] = input.value;
    });

    setLoading('step5-save', true);
    try {
        await apiPost('/api/setup/properties', { properties });
        wizardState.propertiesChanged = true;
        showStep(6);
        document.getElementById('restart-notice').classList.remove('d-none');
    } catch (err) {
        notifications.show('Failed to save configuration: ' + (err.message || 'Unknown error'), 'error');
    } finally {
        setLoading('step5-save', false);
    }
}

function handleDownload() {
    const groups = wizardState.propertiesData;
    if (!groups) {
        notifications.show('Configuration not loaded yet. Please wait.', 'warning');
        return;
    }

    // Build properties file content from current input values
    const inputs = document.querySelectorAll('.prop-input');
    const currentValues = {};
    inputs.forEach(input => {
        currentValues[input.dataset.key] = input.value;
    });

    let content = '# application.properties — downloaded from Setup Wizard\n\n';
    Object.entries(groups).forEach(([groupName, entries]) => {
        content += `# --- ${groupName} ---\n`;
        entries.forEach(entry => {
            if (entry.description) content += `# ${entry.description}\n`;
            const val = currentValues[entry.key] !== undefined ? currentValues[entry.key] : entry.value;
            content += `${entry.key}=${val}\n`;
        });
        content += '\n';
    });

    const blob = new Blob([content], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'application.properties';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
}

// ============================================================================
// API helpers (no JWT — setup is pre-auth)
// ============================================================================
async function apiGet(url) {
    const res = await fetch(url, {
        headers: { 'Accept': 'application/json' }
    });
    const body = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(body.message || `HTTP ${res.status}`);
    return body;
}

async function apiPost(url, data) {
    const res = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify(data)
    });
    const body = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(body.message || `HTTP ${res.status}`);
    return body;
}

// ============================================================================
// UI helpers
// ============================================================================
function showStepError(step, message) {
    const el = document.getElementById(`step${step}-error`);
    if (el) {
        el.textContent = message;
        el.classList.remove('d-none');
    }
}

function clearStepError(step) {
    const el = document.getElementById(`step${step}-error`);
    if (el) {
        el.textContent = '';
        el.classList.add('d-none');
    }
}

function setLoading(btnId, loading) {
    const btn = document.getElementById(btnId);
    if (!btn) return;
    btn.disabled = loading;
    if (loading) {
        btn.dataset.savedText = btn.innerHTML;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Please wait…';
    } else {
        btn.innerHTML = btn.dataset.savedText || btn.dataset.originalText || 'Continue';
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}
