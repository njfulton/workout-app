/**
 * Returns the full HTML for the dashboard single-page app.
 * Inline CSS and JS — no external dependencies or build step.
 */
export function getDashboardHtml(): string {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Email Capture Dashboard</title>
  <style>
    :root {
      --bg: #0f1117;
      --surface: #1a1d27;
      --border: #2a2d3a;
      --text: #e1e4ed;
      --text-dim: #8b8fa3;
      --accent: #6c8cff;
      --accent-hover: #8ba4ff;
      --success: #4ade80;
      --error: #f87171;
      --warning: #fbbf24;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, sans-serif; background: var(--bg); color: var(--text); line-height: 1.5; }

    /* Login */
    .login-container { display: flex; align-items: center; justify-content: center; min-height: 100vh; }
    .login-box { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; padding: 2rem; width: 100%; max-width: 360px; }
    .login-box h1 { font-size: 1.25rem; margin-bottom: 1rem; }
    .login-box input { width: 100%; padding: 0.5rem 0.75rem; background: var(--bg); border: 1px solid var(--border); border-radius: 6px; color: var(--text); font-size: 0.875rem; margin-bottom: 0.75rem; }
    .login-box button { width: 100%; padding: 0.5rem; background: var(--accent); border: none; border-radius: 6px; color: #fff; font-size: 0.875rem; cursor: pointer; }
    .login-box button:hover { background: var(--accent-hover); }
    .login-error { color: var(--error); font-size: 0.8rem; margin-top: 0.5rem; display: none; }

    /* Layout */
    .app { display: none; }
    .header { display: flex; align-items: center; justify-content: space-between; padding: 1rem 1.5rem; border-bottom: 1px solid var(--border); }
    .header h1 { font-size: 1.125rem; font-weight: 600; }
    .header-actions { display: flex; gap: 0.5rem; align-items: center; }
    .header-actions button { padding: 0.375rem 0.75rem; background: var(--surface); border: 1px solid var(--border); border-radius: 6px; color: var(--text-dim); font-size: 0.8rem; cursor: pointer; }
    .header-actions button:hover { color: var(--text); border-color: var(--text-dim); }

    /* Tabs */
    .tabs { display: flex; gap: 0; border-bottom: 1px solid var(--border); padding: 0 1.5rem; }
    .tab { padding: 0.75rem 1rem; font-size: 0.875rem; color: var(--text-dim); cursor: pointer; border-bottom: 2px solid transparent; transition: all 0.15s; }
    .tab:hover { color: var(--text); }
    .tab.active { color: var(--accent); border-bottom-color: var(--accent); }

    /* Content */
    .content { padding: 1.5rem; max-width: 1200px; margin: 0 auto; }
    .panel { display: none; }
    .panel.active { display: block; }

    /* Stats cards */
    .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; margin-bottom: 1.5rem; }
    .stat-card { background: var(--surface); border: 1px solid var(--border); border-radius: 8px; padding: 1rem; }
    .stat-card .label { font-size: 0.75rem; color: var(--text-dim); text-transform: uppercase; letter-spacing: 0.05em; }
    .stat-card .value { font-size: 1.75rem; font-weight: 700; margin-top: 0.25rem; }

    /* Chart */
    .chart-container { background: var(--surface); border: 1px solid var(--border); border-radius: 8px; padding: 1rem; margin-bottom: 1.5rem; }
    .chart-container h3 { font-size: 0.875rem; margin-bottom: 0.75rem; color: var(--text-dim); }
    .bar-chart { display: flex; align-items: flex-end; gap: 4px; height: 120px; }
    .bar-wrapper { display: flex; flex-direction: column; align-items: center; flex: 1; min-width: 0; }
    .bar { width: 100%; background: var(--accent); border-radius: 3px 3px 0 0; min-height: 2px; transition: height 0.3s; }
    .bar-label { font-size: 0.6rem; color: var(--text-dim); margin-top: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 100%; }

    /* Lists */
    .list-section { background: var(--surface); border: 1px solid var(--border); border-radius: 8px; padding: 1rem; margin-bottom: 1rem; }
    .list-section h3 { font-size: 0.875rem; margin-bottom: 0.75rem; color: var(--text-dim); }
    .list-item { display: flex; justify-content: space-between; padding: 0.375rem 0; border-bottom: 1px solid var(--border); font-size: 0.8rem; }
    .list-item:last-child { border-bottom: none; }

    /* Table */
    .table-wrapper { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; font-size: 0.8rem; }
    th { text-align: left; padding: 0.5rem 0.75rem; border-bottom: 2px solid var(--border); color: var(--text-dim); font-weight: 500; text-transform: uppercase; font-size: 0.7rem; letter-spacing: 0.05em; white-space: nowrap; }
    td { padding: 0.5rem 0.75rem; border-bottom: 1px solid var(--border); vertical-align: top; }
    tr:hover td { background: rgba(108, 140, 255, 0.05); }

    /* Badges */
    .badge { display: inline-block; padding: 0.125rem 0.5rem; border-radius: 999px; font-size: 0.7rem; font-weight: 500; }
    .badge-success { background: rgba(74, 222, 128, 0.15); color: var(--success); }
    .badge-error { background: rgba(248, 113, 113, 0.15); color: var(--error); }
    .badge-notes { background: rgba(108, 140, 255, 0.15); color: var(--accent); }
    .badge-capture { background: rgba(251, 191, 36, 0.15); color: var(--warning); }
    .badge-tasks { background: rgba(74, 222, 128, 0.15); color: var(--success); }

    /* Filters */
    .filters { display: flex; gap: 0.5rem; margin-bottom: 1rem; flex-wrap: wrap; }
    .filters select { padding: 0.375rem 0.5rem; background: var(--surface); border: 1px solid var(--border); border-radius: 6px; color: var(--text); font-size: 0.8rem; }

    /* Pagination */
    .pagination { display: flex; gap: 0.5rem; justify-content: center; margin-top: 1rem; }
    .pagination button { padding: 0.375rem 0.75rem; background: var(--surface); border: 1px solid var(--border); border-radius: 6px; color: var(--text); font-size: 0.8rem; cursor: pointer; }
    .pagination button:disabled { opacity: 0.4; cursor: default; }
    .pagination button:hover:not(:disabled) { border-color: var(--accent); }

    /* Config */
    .config-section { background: var(--surface); border: 1px solid var(--border); border-radius: 8px; padding: 1rem; margin-bottom: 1rem; }
    .config-section h3 { font-size: 0.875rem; margin-bottom: 0.75rem; }
    .config-section textarea { width: 100%; min-height: 120px; padding: 0.5rem; background: var(--bg); border: 1px solid var(--border); border-radius: 6px; color: var(--text); font-family: 'SF Mono', 'Fira Code', monospace; font-size: 0.8rem; resize: vertical; }
    .config-actions { display: flex; gap: 0.5rem; margin-top: 0.5rem; }
    .btn { padding: 0.375rem 0.75rem; border-radius: 6px; font-size: 0.8rem; cursor: pointer; border: 1px solid var(--border); }
    .btn-primary { background: var(--accent); color: #fff; border-color: var(--accent); }
    .btn-primary:hover { background: var(--accent-hover); }
    .btn-sm { padding: 0.25rem 0.5rem; font-size: 0.7rem; }
    .btn-danger { color: var(--error); }

    /* Toast */
    .toast { position: fixed; bottom: 1rem; right: 1rem; padding: 0.75rem 1rem; background: var(--surface); border: 1px solid var(--border); border-radius: 8px; font-size: 0.8rem; opacity: 0; transition: opacity 0.3s; pointer-events: none; z-index: 100; }
    .toast.visible { opacity: 1; }
    .toast.success { border-color: var(--success); }
    .toast.error { border-color: var(--error); }

    /* Link styling */
    a { color: var(--accent); text-decoration: none; }
    a:hover { text-decoration: underline; }

    /* Responsive */
    @media (max-width: 640px) {
      .content { padding: 1rem; }
      .stats-grid { grid-template-columns: repeat(2, 1fr); }
    }
  </style>
</head>
<body>

<!-- Login Screen -->
<div class="login-container" id="loginScreen">
  <div class="login-box">
    <h1>Email Capture Dashboard</h1>
    <input type="password" id="tokenInput" placeholder="Enter dashboard token" onkeydown="if(event.key==='Enter')login()">
    <button onclick="login()">Sign In</button>
    <div class="login-error" id="loginError">Invalid token</div>
  </div>
</div>

<!-- App -->
<div class="app" id="app">
  <div class="header">
    <h1>Email Capture</h1>
    <div class="header-actions">
      <button onclick="refreshCurrentPanel()">Refresh</button>
      <button onclick="logout()">Logout</button>
    </div>
  </div>

  <div class="tabs">
    <div class="tab active" data-panel="analytics" onclick="switchTab(this)">Analytics</div>
    <div class="tab" data-panel="logs" onclick="switchTab(this)">Activity Log</div>
    <div class="tab" data-panel="config" onclick="switchTab(this)">Configuration</div>
  </div>

  <div class="content">
    <!-- Analytics Panel -->
    <div class="panel active" id="panel-analytics">
      <div class="stats-grid" id="statsCards"></div>
      <div class="chart-container">
        <h3>Emails Processed (Last 30 Days)</h3>
        <div class="bar-chart" id="dailyChart"></div>
      </div>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:1rem">
        <div class="list-section">
          <h3>Top Sources</h3>
          <div id="topSources"></div>
        </div>
        <div class="list-section">
          <h3>Top Tags</h3>
          <div id="topTags"></div>
        </div>
      </div>
    </div>

    <!-- Logs Panel -->
    <div class="panel" id="panel-logs">
      <div class="filters">
        <select id="filterStatus" onchange="loadLogs()">
          <option value="">All Statuses</option>
          <option value="success">Success</option>
          <option value="error">Error</option>
        </select>
        <select id="filterDest" onchange="loadLogs()">
          <option value="">All Databases</option>
          <option value="notes">Notes</option>
          <option value="capture">Capture</option>
          <option value="tasks">Tasks</option>
        </select>
      </div>
      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>Time</th>
              <th>Subject</th>
              <th>From</th>
              <th>Dest</th>
              <th>Tag</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody id="logsBody"></tbody>
        </table>
      </div>
      <div class="pagination" id="logsPagination"></div>
    </div>

    <!-- Config Panel -->
    <div class="panel" id="panel-config">
      <div id="configSections"></div>
    </div>
  </div>
</div>

<div class="toast" id="toast"></div>

<script>
const PAGE_SIZE = 25;
let logsPage = 0;

// ── Auth ────────────────────────────────────────────────────────────────

async function login() {
  const token = document.getElementById('tokenInput').value;
  const res = await fetch('/api/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token })
  });
  if (res.ok) {
    document.getElementById('loginScreen').style.display = 'none';
    document.getElementById('app').style.display = 'block';
    loadAnalytics();
  } else {
    const el = document.getElementById('loginError');
    el.style.display = 'block';
    setTimeout(() => el.style.display = 'none', 3000);
  }
}

async function logout() {
  await fetch('/api/logout', { method: 'POST' });
  location.reload();
}

// ── Init: check if already authenticated ────────────────────────────────

async function init() {
  const res = await fetch('/api/logs?limit=1');
  if (res.ok) {
    document.getElementById('loginScreen').style.display = 'none';
    document.getElementById('app').style.display = 'block';
    loadAnalytics();
  }
}
init();

// ── Tabs ────────────────────────────────────────────────────────────────

function switchTab(el) {
  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
  document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
  el.classList.add('active');
  const panel = document.getElementById('panel-' + el.dataset.panel);
  panel.classList.add('active');

  if (el.dataset.panel === 'analytics') loadAnalytics();
  else if (el.dataset.panel === 'logs') loadLogs();
  else if (el.dataset.panel === 'config') loadConfig();
}

function refreshCurrentPanel() {
  const active = document.querySelector('.tab.active');
  if (active) switchTab(active);
}

// ── Analytics ───────────────────────────────────────────────────────────

async function loadAnalytics() {
  try {
    const res = await fetch('/api/stats?days=30');
    const data = await res.json();

    const successCount = data.byStatus.find(s => s.status === 'success')?.count || 0;
    const errorCount = data.byStatus.find(s => s.status === 'error')?.count || 0;

    document.getElementById('statsCards').innerHTML =
      statCard('Total Processed', data.total) +
      statCard('Success', successCount, 'success') +
      statCard('Errors', errorCount, errorCount > 0 ? 'error' : '') +
      data.byDestination.map(d => statCard(d.destination, d.count, d.destination)).join('');

    // Daily chart
    const chart = document.getElementById('dailyChart');
    if (data.daily.length === 0) {
      chart.innerHTML = '<div style="color:var(--text-dim);font-size:0.8rem;padding:2rem;text-align:center">No data yet</div>';
    } else {
      const max = Math.max(...data.daily.map(d => d.count), 1);
      chart.innerHTML = data.daily.map(d => {
        const pct = (d.count / max) * 100;
        const label = d.date.slice(5); // MM-DD
        return '<div class="bar-wrapper"><div class="bar" style="height:' + pct + '%" title="' + d.date + ': ' + d.count + '"></div><div class="bar-label">' + label + '</div></div>';
      }).join('');
    }

    // Top sources
    document.getElementById('topSources').innerHTML = data.topSources.length === 0
      ? '<div style="color:var(--text-dim);font-size:0.8rem">No data yet</div>'
      : data.topSources.map(s => '<div class="list-item"><span>' + esc(s.sender_name) + '</span><span>' + s.count + '</span></div>').join('');

    // Top tags
    document.getElementById('topTags').innerHTML = data.topTags.length === 0
      ? '<div style="color:var(--text-dim);font-size:0.8rem">No data yet</div>'
      : data.topTags.map(t => '<div class="list-item"><span>' + esc(t.tag) + '</span><span>' + t.count + '</span></div>').join('');
  } catch (e) {
    toast('Failed to load analytics', 'error');
  }
}

function statCard(label, value, variant) {
  const color = variant === 'success' ? 'var(--success)' : variant === 'error' ? 'var(--error)' : variant === 'notes' ? 'var(--accent)' : variant === 'capture' ? 'var(--warning)' : variant === 'tasks' ? 'var(--success)' : 'var(--text)';
  return '<div class="stat-card"><div class="label">' + esc(label) + '</div><div class="value" style="color:' + color + '">' + value + '</div></div>';
}

// ── Logs ────────────────────────────────────────────────────────────────

async function loadLogs() {
  try {
    const status = document.getElementById('filterStatus').value;
    const dest = document.getElementById('filterDest').value;
    const params = new URLSearchParams({ limit: PAGE_SIZE, offset: logsPage * PAGE_SIZE });
    if (status) params.set('status', status);
    if (dest) params.set('destination', dest);

    const res = await fetch('/api/logs?' + params);
    const data = await res.json();

    const tbody = document.getElementById('logsBody');
    if (data.results.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:var(--text-dim);padding:2rem">No emails processed yet</td></tr>';
    } else {
      tbody.innerHTML = data.results.map(log => {
        const time = new Date(log.created_at + 'Z').toLocaleString();
        const notionLink = log.notion_page_id
          ? '<a href="https://notion.so/' + log.notion_page_id.replace(/-/g, '') + '" target="_blank">View</a>'
          : '-';
        const retryBtn = log.status === 'error'
          ? '<button class="btn btn-sm" onclick="retryEmail(' + log.id + ')">Retry</button>'
          : notionLink;
        return '<tr>' +
          '<td style="white-space:nowrap">' + esc(time) + '</td>' +
          '<td>' + esc(log.subject) + '</td>' +
          '<td>' + esc(log.sender_name) + '</td>' +
          '<td><span class="badge badge-' + log.destination + '">' + log.destination + '</span></td>' +
          '<td>' + (log.tag ? esc(log.tag) : '-') + '</td>' +
          '<td><span class="badge badge-' + log.status + '">' + log.status + '</span>' +
            (log.error_message ? '<br><small style="color:var(--error)">' + esc(log.error_message).slice(0, 80) + '</small>' : '') +
          '</td>' +
          '<td>' + retryBtn + '</td>' +
          '</tr>';
      }).join('');
    }

    // Pagination
    const totalPages = Math.ceil(data.total / PAGE_SIZE);
    const pag = document.getElementById('logsPagination');
    pag.innerHTML =
      '<button ' + (logsPage === 0 ? 'disabled' : '') + ' onclick="logsPage--;loadLogs()">Prev</button>' +
      '<span style="padding:0.375rem;font-size:0.8rem;color:var(--text-dim)">Page ' + (logsPage + 1) + ' of ' + Math.max(totalPages, 1) + '</span>' +
      '<button ' + (logsPage >= totalPages - 1 ? 'disabled' : '') + ' onclick="logsPage++;loadLogs()">Next</button>';
  } catch (e) {
    toast('Failed to load logs', 'error');
  }
}

async function retryEmail(id) {
  try {
    toast('Retrying...', 'success');
    const res = await fetch('/api/retry/' + id, { method: 'POST' });
    const data = await res.json();
    if (res.ok) {
      toast('Retry successful!', 'success');
    } else {
      toast('Retry failed: ' + (data.error || 'Unknown error'), 'error');
    }
    loadLogs();
  } catch (e) {
    toast('Retry failed', 'error');
  }
}

// ── Config ──────────────────────────────────────────────────────────────

const CONFIG_LABELS = {
  tag_mappings: { title: 'Tag Mappings', desc: 'Map short +tags to full Topic names. Keys are lowercase tag values, values are the Notion Topic name.' },
  notes_defaults: { title: 'Notes DB Defaults', desc: 'Default property values when routing to the Notes database.' },
  capture_defaults: { title: 'Capture DB Defaults', desc: 'Default property values when routing to the Capture database.' },
  tasks_defaults: { title: 'Tasks DB Defaults', desc: 'Default property values when routing to the Tasks database.' },
  special_tags: { title: 'Special Tags', desc: 'Tags that trigger property overrides instead of setting Topic. E.g., "newsletter" sets Format to Newsletter.' }
};

async function loadConfig() {
  try {
    const res = await fetch('/api/config');
    const config = await res.json();
    const container = document.getElementById('configSections');

    container.innerHTML = Object.entries(CONFIG_LABELS).map(([key, meta]) => {
      const value = config[key] !== undefined ? JSON.stringify(config[key], null, 2) : '{}';
      return '<div class="config-section">' +
        '<h3>' + esc(meta.title) + '</h3>' +
        '<p style="font-size:0.75rem;color:var(--text-dim);margin-bottom:0.5rem">' + esc(meta.desc) + '</p>' +
        '<textarea id="config-' + key + '">' + esc(value) + '</textarea>' +
        '<div class="config-actions">' +
        '<button class="btn btn-primary btn-sm" onclick="saveConfig(\\'' + key + '\\')">Save</button>' +
        '</div></div>';
    }).join('');
  } catch (e) {
    toast('Failed to load config', 'error');
  }
}

async function saveConfig(key) {
  try {
    const textarea = document.getElementById('config-' + key);
    const value = JSON.parse(textarea.value);
    const res = await fetch('/api/config/' + key, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(value)
    });
    if (res.ok) {
      toast('Config saved!', 'success');
    } else {
      toast('Failed to save config', 'error');
    }
  } catch (e) {
    toast('Invalid JSON', 'error');
  }
}

// ── Utilities ───────────────────────────────────────────────────────────

function esc(str) {
  if (!str) return '';
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

function toast(msg, type) {
  const el = document.getElementById('toast');
  el.textContent = msg;
  el.className = 'toast visible ' + (type || '');
  setTimeout(() => el.className = 'toast', 3000);
}
</script>
</body>
</html>`;
}
