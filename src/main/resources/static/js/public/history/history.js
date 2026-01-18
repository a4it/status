/**
 * Public History Page JavaScript
 */

document.addEventListener('DOMContentLoaded', function() {
    loadHistory();

    document.getElementById('days-filter').addEventListener('change', function() {
        loadHistory();
    });
});

async function loadHistory() {
    const days = document.getElementById('days-filter').value;

    // Update header texts
    document.getElementById('platforms-days-header').textContent = `Last ${days} Days`;
    document.getElementById('components-days-header').textContent = `Last ${days} Days`;

    // Load platforms and components in parallel
    loadPlatforms(parseInt(days));
    loadComponents(parseInt(days));
}

async function loadPlatforms(days) {
    document.getElementById('platforms-loading').style.display = 'block';
    document.getElementById('platforms-content').style.display = 'none';
    document.getElementById('platforms-empty').style.display = 'none';

    try {
        const apps = await api.get('/public/status/apps');

        if (!apps || apps.length === 0) {
            document.getElementById('platforms-loading').style.display = 'none';
            document.getElementById('platforms-empty').style.display = 'block';
            return;
        }

        let platformsWithHistory = [];
        for (const app of apps) {
            try {
                const history = await api.get(`/public/status/apps/${app.id}/uptime-history?days=${days}`);
                platformsWithHistory.push({
                    ...app,
                    history: history
                });
            } catch (e) {
                platformsWithHistory.push({
                    ...app,
                    history: null
                });
            }
        }

        displayPlatforms(platformsWithHistory, days);
    } catch (error) {
        console.error('Failed to load platforms:', error);
        document.getElementById('platforms-loading').innerHTML = '<p class="text-danger text-center">Failed to load platforms.</p>';
    }
}

function displayPlatforms(platforms, days) {
    const loadingEl = document.getElementById('platforms-loading');
    const contentEl = document.getElementById('platforms-content');
    const emptyEl = document.getElementById('platforms-empty');
    const tableBody = document.getElementById('platforms-table-body');

    loadingEl.style.display = 'none';

    if (platforms.length === 0) {
        emptyEl.style.display = 'block';
        return;
    }

    contentEl.style.display = 'block';
    tableBody.innerHTML = platforms.map(platform => createPlatformHistoryRow(platform, days)).join('');
}

function createPlatformHistoryRow(platform, days) {
    const uptime = platform.history?.overallUptimePercentage ?? 100;
    const uptimeClass = uptime >= 99.9 ? 'text-green' : uptime >= 99 ? 'text-yellow' : 'text-red';

    // Generate uptime bar from dailyHistory
    const uptimeBar = generateUptimeBar(platform.history?.dailyHistory || [], days);

    return `
        <tr>
            <td>
                <div class="d-flex align-items-center">
                    <span class="component-status-dot ${getStatusClass(platform.status)} me-2"></span>
                    <div>
                        <div class="font-weight-medium">${escapeHtml(platform.name)}</div>
                        ${platform.description ? `<div class="text-muted small">${escapeHtml(platform.description)}</div>` : ''}
                    </div>
                </div>
            </td>
            <td>
                <span class="${uptimeClass} font-weight-medium">${uptime.toFixed(2)}%</span>
            </td>
            <td style="width: 40%;">
                <div class="uptime-bar" title="Uptime history">
                    ${uptimeBar}
                </div>
            </td>
        </tr>
    `;
}

async function loadComponents(days) {
    document.getElementById('history-loading').style.display = 'block';
    document.getElementById('history-content').style.display = 'none';
    document.getElementById('history-empty').style.display = 'none';

    try {
        const apps = await api.get('/public/status/apps');

        let allComponents = [];
        for (const app of apps) {
            const components = await api.get(`/public/status/apps/${app.id}/components`);
            for (const component of components) {
                try {
                    const history = await api.get(`/public/status/components/${component.id}/history?days=${days}`);
                    allComponents.push({
                        ...component,
                        appName: app.name,
                        history: history
                    });
                } catch (e) {
                    allComponents.push({
                        ...component,
                        appName: app.name,
                        history: null
                    });
                }
            }
        }

        displayComponents(allComponents, days);
    } catch (error) {
        console.error('Failed to load history:', error);
        document.getElementById('history-loading').innerHTML = '<p class="text-danger text-center">Failed to load history.</p>';
    }
}

function displayComponents(components, days) {
    const loadingEl = document.getElementById('history-loading');
    const contentEl = document.getElementById('history-content');
    const emptyEl = document.getElementById('history-empty');
    const tableBody = document.getElementById('history-table-body');

    loadingEl.style.display = 'none';

    if (components.length === 0) {
        emptyEl.style.display = 'block';
        return;
    }

    contentEl.style.display = 'block';
    tableBody.innerHTML = components.map(component => createComponentHistoryRow(component, days)).join('');
}

function createComponentHistoryRow(component, days) {
    const uptime = component.history?.uptimePercentage ?? 100;
    const uptimeClass = uptime >= 99.9 ? 'text-green' : uptime >= 99 ? 'text-yellow' : 'text-red';

    // Generate uptime bar
    const uptimeBar = generateUptimeBar(component.history?.dailyStatus || [], days);

    return `
        <tr>
            <td>
                <div class="d-flex align-items-center">
                    <span class="component-status-dot ${getStatusClass(component.status)} me-2"></span>
                    <div>
                        <div class="font-weight-medium">${escapeHtml(component.name)}</div>
                        <div class="text-muted small">${escapeHtml(component.appName)}</div>
                    </div>
                </div>
            </td>
            <td>
                <span class="${uptimeClass} font-weight-medium">${uptime.toFixed(2)}%</span>
            </td>
            <td style="width: 40%;">
                <div class="uptime-bar" title="Uptime history">
                    ${uptimeBar}
                </div>
            </td>
        </tr>
    `;
}

function generateUptimeBar(dailyHistory, days) {
    const bars = [];
    const today = new Date();

    for (let i = days - 1; i >= 0; i--) {
        const date = new Date(today);
        date.setDate(date.getDate() - i);
        const dateStr = date.toISOString().split('T')[0];

        const dayData = dailyHistory.find(d => d.date === dateStr);
        const status = dayData?.status || 'no-data';
        const statusClass = getStatusClass(status);

        bars.push(`<div class="uptime-bar-day ${statusClass}" title="${date.toLocaleDateString()}: ${formatStatus(status)}"></div>`);
    }

    return bars.join('');
}

function getStatusClass(status) {
    const statusMap = {
        'OPERATIONAL': 'operational',
        'DEGRADED': 'degraded',
        'DEGRADED_PERFORMANCE': 'degraded',
        'PARTIAL_OUTAGE': 'partial-outage',
        'MAJOR_OUTAGE': 'major-outage',
        'OUTAGE': 'major-outage',
        'MAINTENANCE': 'maintenance',
        'UNDER_MAINTENANCE': 'maintenance',
        'no-data': 'no-data'
    };
    return statusMap[status] || 'no-data';
}

function formatStatus(status) {
    const statusMap = {
        'OPERATIONAL': 'Operational',
        'DEGRADED': 'Degraded',
        'DEGRADED_PERFORMANCE': 'Degraded Performance',
        'PARTIAL_OUTAGE': 'Partial Outage',
        'MAJOR_OUTAGE': 'Major Outage',
        'OUTAGE': 'Outage',
        'MAINTENANCE': 'Under Maintenance',
        'UNDER_MAINTENANCE': 'Under Maintenance',
        'no-data': 'No Data'
    };
    return statusMap[status] || status;
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
