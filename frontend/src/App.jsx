import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Activity, ArrowDown, ArrowUp, BarChart3, Bell, Check, ChevronDown,
  CircleHelp, Clock3, GripVertical, LayoutDashboard, LineChart, LogOut,
  MoreHorizontal, Plus, Search, Settings2, Sparkles, X,
} from 'lucide-react';
import {
  Area, AreaChart, Bar, BarChart, CartesianGrid, Cell, Line, LineChart as RechartsLineChart,
  Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts';
import { dashboardApi } from './api.js';

const metrics = {
  revenue: { label: 'Revenue', field: 'revenue', color: '#5267e8', prefix: '$', value: '$84,254', change: '+12.8%' },
  orders: { label: 'Orders', field: 'orders', color: '#18a978', prefix: '', value: '2,845', change: '+8.2%' },
  customers: { label: 'Customers', field: 'customers', color: '#eab346', prefix: '', value: '1,482', change: '+4.6%' },
  conversion: { label: 'Conversion rate', field: 'conversion', color: '#e57962', prefix: '', value: '3.62%', change: '+0.8%' },
};
const chartTypes = ['area', 'bar', 'line', 'pie'];
const chartLabels = { area: 'Area', bar: 'Bar', line: 'Line', pie: 'Donut' };
const chartColors = ['#5267e8', '#18a978', '#eab346', '#e57962', '#9871df', '#54abc4'];

const initialWidgets = [
  { title: 'Revenue over time', chartType: 'area', metricType: 'sum', groupByField: 'month', valueField: 'revenue', position: 0 },
  { title: 'Orders by month', chartType: 'bar', metricType: 'sum', groupByField: 'month', valueField: 'orders', position: 1 },
  { title: 'Customer growth', chartType: 'line', metricType: 'sum', groupByField: 'month', valueField: 'customers', position: 2 },
  { title: 'Revenue mix', chartType: 'pie', metricType: 'sum', groupByField: 'category', valueField: 'revenue', position: 3 },
];

const periods = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const demoSeries = {
  revenue: [38, 45, 42, 58, 54, 67, 62, 73, 68, 82, 76, 94],
  orders: [28, 35, 31, 48, 41, 53, 50, 62, 57, 70, 66, 82],
  customers: [22, 24, 29, 31, 38, 42, 45, 49, 55, 61, 67, 75],
  conversion: [2.1, 2.4, 2.2, 2.7, 2.5, 2.9, 3.0, 2.8, 3.2, 3.4, 3.3, 3.6],
};

function chartData(widget) {
  const values = demoSeries[widget.valueField] || demoSeries.revenue;
  if (widget.groupByField === 'category') {
    return [
      { name: 'Direct', value: values[9] },
      { name: 'Organic', value: values[7] },
      { name: 'Referral', value: values[5] },
      { name: 'Social', value: values[3] },
    ];
  }
  return periods.map((month, index) => ({ name: month, value: values[index] }));
}

function IconButton({ label, onClick, children, className = '' }) {
  return <button aria-label={label} className={`icon-button ${className}`} onClick={onClick} type="button">{children}</button>;
}

function LoginScreen({ onAuthenticated }) {
  const [isRegistering, setIsRegistering] = useState(false);
  const [form, setForm] = useState({ name: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError('');
    try {
      const user = isRegistering
        ? await dashboardApi.register(form)
        : await dashboardApi.login({ email: form.email, password: form.password });
      if (isRegistering) {
        await dashboardApi.login({ email: form.email, password: form.password });
      }
      onAuthenticated(user);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-card">
        <div className="brand-mark"><BarChart3 size={21} strokeWidth={2.5} /></div>
        <p className="eyebrow">AXLERO ANALYTICS</p>
        <h1>{isRegistering ? 'Create your account' : 'Welcome back'}</h1>
        <p className="auth-subtitle">Your workspace for clearer, faster decisions.</p>
        <form onSubmit={submit}>
          {isRegistering && <label>Your name<input autoComplete="name" onChange={(event) => setForm({ ...form, name: event.target.value })} required value={form.name} /></label>}
          <label>Email address<input autoComplete="email" onChange={(event) => setForm({ ...form, email: event.target.value })} required type="email" value={form.email} /></label>
          <label>Password<input autoComplete={isRegistering ? 'new-password' : 'current-password'} minLength={8} onChange={(event) => setForm({ ...form, password: event.target.value })} required type="password" value={form.password} /></label>
          {error && <div className="form-error" role="alert">{error}</div>}
          <button className="primary-button auth-submit" disabled={busy} type="submit">{busy ? 'Please wait…' : isRegistering ? 'Create account' : 'Sign in'}<ArrowUp size={16} /></button>
        </form>
        <button className="text-button auth-switch" onClick={() => { setError(''); setIsRegistering(!isRegistering); }} type="button">
          {isRegistering ? 'Already have an account? Sign in' : 'New to Axlero? Create an account'}
        </button>
        <p className="auth-footnote"><Check size={14} /> Secure session protected by your workspace</p>
      </section>
    </main>
  );
}

function ChartView({ widget }) {
  const data = chartData(widget);
  const common = { data, margin: { top: 8, right: 8, left: -20, bottom: 0 } };
  if (widget.chartType === 'pie') {
    return (
      <div className="donut-layout">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie data={data} dataKey="value" nameKey="name" innerRadius="62%" outerRadius="86%" paddingAngle={4} stroke="none">
              {data.map((entry, index) => <Cell fill={chartColors[index % chartColors.length]} key={entry.name} />)}
            </Pie>
            <Tooltip />
          </PieChart>
        </ResponsiveContainer>
        <div className="donut-legend">{data.map((entry, index) => <span key={entry.name}><i style={{ background: chartColors[index] }} />{entry.name}<b>{entry.value}%</b></span>)}</div>
      </div>
    );
  }
  return (
    <ResponsiveContainer height="100%" width="100%">
      {widget.chartType === 'bar' ? (
        <BarChart {...common}>
          <CartesianGrid stroke="#eef0f4" strokeDasharray="3 3" vertical={false} />
          <XAxis axisLine={false} dataKey="name" tickLine={false} tick={{ fill: '#9aa1ad', fontSize: 11 }} />
          <YAxis axisLine={false} tickLine={false} tick={{ fill: '#9aa1ad', fontSize: 11 }} />
          <Tooltip />
          <Bar dataKey="value" fill={metrics[widget.valueField]?.color || '#5267e8'} radius={[4, 4, 0, 0]} />
        </BarChart>
      ) : widget.chartType === 'line' ? (
        <RechartsLineChart {...common}>
          <CartesianGrid stroke="#eef0f4" strokeDasharray="3 3" vertical={false} />
          <XAxis axisLine={false} dataKey="name" tickLine={false} tick={{ fill: '#9aa1ad', fontSize: 11 }} />
          <YAxis axisLine={false} tickLine={false} tick={{ fill: '#9aa1ad', fontSize: 11 }} />
          <Tooltip />
          <Line dataKey="value" dot={false} stroke={metrics[widget.valueField]?.color || '#5267e8'} strokeWidth={2.5} type="monotone" />
        </RechartsLineChart>
      ) : (
        <AreaChart {...common}>
          <defs><linearGradient id={`fill-${widget.position}`} x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stopColor={metrics[widget.valueField]?.color || '#5267e8'} stopOpacity={0.22} /><stop offset="95%" stopColor={metrics[widget.valueField]?.color || '#5267e8'} stopOpacity={0.01} /></linearGradient></defs>
          <CartesianGrid stroke="#eef0f4" strokeDasharray="3 3" vertical={false} />
          <XAxis axisLine={false} dataKey="name" tickLine={false} tick={{ fill: '#9aa1ad', fontSize: 11 }} />
          <YAxis axisLine={false} tickLine={false} tick={{ fill: '#9aa1ad', fontSize: 11 }} />
          <Tooltip />
          <Area dataKey="value" fill={`url(#fill-${widget.position})`} stroke={metrics[widget.valueField]?.color || '#5267e8'} strokeWidth={2.5} type="monotone" />
        </AreaChart>
      )}
    </ResponsiveContainer>
  );
}

function Dashboard({ user, onLogout }) {
  const [dashboards, setDashboards] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [widgets, setWidgets] = useState([]);
  const [metric, setMetric] = useState('revenue');
  const [chartType, setChartType] = useState('area');
  const [draggedIndex, setDraggedIndex] = useState(null);
  const [saving, setSaving] = useState(false);
  const [dirty, setDirty] = useState(false);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  const [dashboardName, setDashboardName] = useState('Executive overview');

  const selectedDashboard = dashboards.find((dashboard) => dashboard.id === selectedId);
  const totalMetric = useMemo(() => metrics.revenue, []);

  const loadDashboards = useCallback(async () => {
    const found = await dashboardApi.list();
    if (!found.length) {
      const created = await dashboardApi.create({ name: 'Executive overview', widgets: initialWidgets });
      setDashboards([created]);
      setSelectedId(created.id);
      setDashboardName(created.name);
      setWidgets(created.widgets);
      return;
    }
    setDashboards(found);
    setSelectedId(found[0].id);
    setDashboardName(found[0].name);
    setWidgets(found[0].widgets);
  }, []);

  useEffect(() => {
    loadDashboards().catch((requestError) => setError(requestError.message));
  }, [loadDashboards]);

  function chooseDashboard(dashboard) {
    setSelectedId(dashboard.id);
    setDashboardName(dashboard.name);
    setWidgets(dashboard.widgets);
    setDirty(false);
    setNotice('');
  }

  function updateWidget(index, changes) {
    setWidgets((current) => current.map((widget, itemIndex) => itemIndex === index ? { ...widget, ...changes } : widget));
    setDirty(true);
    setNotice('');
  }

  function addWidget() {
    const selectedMetric = metrics[metric];
    setWidgets((current) => [...current, {
      title: `${selectedMetric.label} by month`,
      chartType,
      metricType: 'sum',
      groupByField: 'month',
      valueField: selectedMetric.field,
      position: current.length,
    }]);
    setDirty(true);
    setNotice('');
  }

  function reorderWidgets(fromIndex, toIndex) {
    if (fromIndex === null || fromIndex === toIndex) return;
    setWidgets((current) => {
      const next = [...current];
      const [moved] = next.splice(fromIndex, 1);
      next.splice(toIndex, 0, moved);
      return next.map((widget, index) => ({ ...widget, position: index }));
    });
    setDirty(true);
  }

  async function saveDashboard() {
    if (!selectedId || saving) return;
    setSaving(true);
    setError('');
    try {
      const updated = await dashboardApi.update(selectedId, { name: dashboardName, widgets });
      setWidgets(updated.widgets);
      setDashboards((current) => current.map((dashboard) => dashboard.id === updated.id ? updated : dashboard));
      setDirty(false);
      setNotice('All changes saved');
      window.setTimeout(() => setNotice(''), 2600);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSaving(false);
    }
  }

  async function createDashboard() {
    const dashboard = await dashboardApi.create({ name: `Untitled dashboard ${dashboards.length + 1}`, widgets: [] });
    setDashboards((current) => [dashboard, ...current]);
    chooseDashboard(dashboard);
  }

  async function removeWidget(index) {
    setWidgets((current) => current.filter((_, itemIndex) => index !== itemIndex).map((widget, position) => ({ ...widget, position })));
    setDirty(true);
  }

  async function logout() {
    try {
      await dashboardApi.logout();
      onLogout();
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#"><span className="brand-mark"><BarChart3 size={19} strokeWidth={2.5} /></span><span>axlero<span className="brand-dot">.</span></span></a>
        <div className="workspace-switch"><span className="workspace-avatar">A</span><span><b>Acme Studio</b><small>Free workspace</small></span><ChevronDown size={15} /></div>
        <div className="side-label">WORKSPACE</div>
        <button className="nav-item active"><LayoutDashboard size={17} /> Dashboards <span className="nav-count">{dashboards.length}</span></button>
        <button className="nav-item"><BarChart3 size={17} /> Reports</button>
        <button className="nav-item"><Settings2 size={17} /> Data sources</button>
        <div className="side-label side-label-spaced">YOUR DASHBOARDS <IconButton label="Create dashboard" onClick={() => createDashboard().catch((e) => setError(e.message))}><Plus size={15} /></IconButton></div>
        <div className="dashboard-nav">{dashboards.map((dashboard) => <button className={`dashboard-link ${selectedId === dashboard.id ? 'selected' : ''}`} key={dashboard.id} onClick={() => chooseDashboard(dashboard)}><span className="dashboard-bullet" />{dashboard.name}</button>)}</div>
        <div className="sidebar-bottom">
          <div className="help-card"><div><Sparkles size={16} /><b>Need a hand?</b></div><p>Get more from your data with a little help from our team.</p><button type="button">Visit help center <ArrowUp size={13} /></button></div>
          <button className="profile-row" onClick={logout}><span className="user-avatar">{user.name?.slice(0, 1).toUpperCase() || 'U'}</span><span><b>{user.name}</b><small>{user.email}</small></span><LogOut size={16} /></button>
        </div>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <div className="breadcrumbs"><span>Workspace</span><span>/</span><b>Dashboards</b></div>
          <div className="topbar-actions"><div className="search-box"><Search size={15} /><span>Search anything...</span><kbd>⌘ K</kbd></div><IconButton label="Notifications"><Bell size={17} /><i className="notification-dot" /></IconButton><span className="top-divider" /><span className="avatar-small">{user.name?.slice(0, 1).toUpperCase() || 'U'}</span></div>
        </header>
        <div className="page-content">
          <div className="page-heading">
            <div><div className="eyebrow heading-eyebrow"><span className="live-dot" /> LIVE DASHBOARD <span className="heading-separator">·</span> UPDATED JUST NOW</div><h1>{dashboardName}</h1><p>A clear view of what’s happening across your business.</p></div>
            <div className="heading-actions"><button className="outline-button" type="button"><Clock3 size={15} /> Last 30 days <ChevronDown size={14} /></button><button className="outline-button" type="button"><CircleHelp size={15} /> Share</button><button className="primary-button save-button" disabled={!dirty || saving} onClick={saveDashboard} type="button">{saving ? 'Saving…' : dirty ? 'Save changes' : 'Saved'}{dirty ? <ArrowUp size={15} /> : <Check size={15} />}</button></div>
          </div>
          {error && <div className="notice error-notice" role="alert">{error}<IconButton label="Dismiss error" onClick={() => setError('')}><X size={15} /></IconButton></div>}
          {notice && <div className="notice success-notice"><Check size={15} />{notice}</div>}
          <section className="metric-grid" aria-label="Key metrics">
            {Object.entries(metrics).map(([key, item], index) => (
              <article className="metric-card" key={key}>
                <div className="metric-card-top"><span>{item.label}</span><span className="metric-icon" style={{ color: item.color, background: `${item.color}12` }}>{index === 0 ? <Activity size={17} /> : index === 1 ? <BarChart3 size={17} /> : index === 2 ? <ArrowUp size={17} /> : <LineChart size={17} />}</span></div>
                <strong>{item.value}</strong><div className="metric-foot"><span className="positive"><ArrowUp size={12} /> {item.change}</span><span>vs. previous period</span></div>
              </article>
            ))}
          </section>
          <section className="builder-toolbar">
            <div className="builder-title"><span className="builder-icon"><Sparkles size={16} /></span><div><b>Build your dashboard</b><small>Choose a metric and chart, then add it to your canvas.</small></div></div>
            <div className="builder-controls">
              <label className="select-control"><span>Metric</span><select onChange={(event) => setMetric(event.target.value)} value={metric}>{Object.entries(metrics).map(([key, item]) => <option key={key} value={key}>{item.label}</option>)}</select></label>
              <label className="select-control"><span>Chart type</span><select onChange={(event) => setChartType(event.target.value)} value={chartType}>{chartTypes.map((type) => <option key={type} value={type}>{chartLabels[type]}</option>)}</select></label>
              <button className="add-widget-button" onClick={addWidget} type="button"><Plus size={16} /> Add widget</button>
            </div>
          </section>
          <div className="section-title"><div><h2>Performance overview</h2><span>{widgets.length} widgets <span className="muted-dot">·</span> Drag cards to rearrange</span></div><button className="text-button" type="button"><MoreHorizontal size={18} /> Customize</button></div>
          {widgets.length === 0 ? <div className="empty-state"><div className="empty-icon"><BarChart3 size={22} /></div><h3>Your canvas is ready</h3><p>Add a metric above to start building this dashboard.</p><button className="primary-button" onClick={addWidget} type="button"><Plus size={15} /> Add your first widget</button></div> : (
            <section className="chart-grid">
              {widgets.map((widget, index) => {
                const selected = metrics[widget.valueField] || totalMetric;
                return (
                  <article className={`chart-card ${draggedIndex === index ? 'dragging' : ''}`} draggable onDragEnd={() => setDraggedIndex(null)} onDragOver={(event) => event.preventDefault()} onDragStart={() => setDraggedIndex(index)} onDrop={() => reorderWidgets(draggedIndex, index)} key={`${widget.id || 'new'}-${index}`}>
                    <div className="chart-card-head">
                      <div className="chart-title-wrap"><GripVertical className="drag-handle" size={17} /><div><h3>{widget.title}</h3><span>{widget.groupByField === 'category' ? 'By category' : 'Monthly breakdown'}</span></div></div>
                      <div className="chart-actions"><span className="chart-menu-label">{chartLabels[widget.chartType]}</span><IconButton label={`Remove ${widget.title}`} onClick={() => removeWidget(index)}><X size={15} /></IconButton></div>
                    </div>
                    <div className="chart-filters">
                      <label><span className="legend-dot" style={{ background: selected.color }} />{selected.label}<select aria-label={`Metric for ${widget.title}`} onChange={(event) => updateWidget(index, { valueField: event.target.value, title: `${metrics[event.target.value].label} by month` })} value={widget.valueField}>{Object.entries(metrics).map(([key, item]) => <option key={key} value={key}>{item.label}</option>)}</select><ChevronDown size={12} /></label>
                      <select aria-label={`Chart type for ${widget.title}`} className="chart-type-select" onChange={(event) => updateWidget(index, { chartType: event.target.value })} value={widget.chartType}>{chartTypes.map((type) => <option key={type} value={type}>{chartLabels[type]}</option>)}</select>
                    </div>
                    <div className="chart-area"><ChartView widget={widget} /></div>
                    <div className="chart-card-foot"><span><i className="demo-dot" /> Demo data · Monthly</span><span>View details <ArrowUp size={12} /></span></div>
                  </article>
                );
              })}
            </section>
          )}
          <div className="data-note"><Sparkles size={14} /><span>Charts use sample metrics for preview. Connect a business data source to display live values.</span><button type="button">Connect a source <ArrowUp size={12} /></button></div>
        </div>
      </main>
    </div>
  );
}

export default function App() {
  const [user, setUser] = useState(null);
  const [checkingSession, setCheckingSession] = useState(true);
  const [sessionError, setSessionError] = useState('');

  useEffect(() => {
    dashboardApi.currentUser()
      .then(setUser)
      .catch((error) => {
        if (!error.message.includes('(403)') && !error.message.includes('(401)')) {
          setSessionError(error.message);
        }
      })
      .finally(() => setCheckingSession(false));
  }, []);

  if (checkingSession) return <div className="loading-screen"><span className="loading-mark"><BarChart3 size={20} /></span><span>Loading your workspace…</span></div>;
  if (!user) return <><LoginScreen onAuthenticated={setUser} />{sessionError && <div className="connection-error" role="alert">Could not connect to the dashboard API: {sessionError}</div>}</>;
  return <Dashboard key={user.id} onLogout={() => setUser(null)} user={user} />;
}
