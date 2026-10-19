
import { useState } from "react";
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer
} from "recharts";
import {
  LayoutDashboard, PanelsTopLeft, Settings, Search,
  Bell, Plus, TrendingUp, Users, Database, Activity
} from "lucide-react";
import "./App.css";

const salesData = [
  { month: "Jan", sales: 12000 },
  { month: "Feb", sales: 18000 },
  { month: "Mar", sales: 15000 },
  { month: "Apr", sales: 24000 },
  { month: "May", sales: 21000 },
  { month: "Jun", sales: 30000 },
];

const initialWidgets = [
  { id: 1, title: "Revenue Overview", type: "Bar Chart" },
  { id: 2, title: "Customer Analytics", type: "Metric Card" },
];

export default function App() {
  const [widgets, setWidgets] = useState(initialWidgets);
  const [showForm, setShowForm] = useState(false);
  const [title, setTitle] = useState("");

  function addWidget(event) {
    event.preventDefault();
    if (!title.trim()) return;
    setWidgets([...widgets, {
      id: Date.now(),
      title: title.trim(),
      type: "Metric Card",
    }]);
    setTitle("");
    setShowForm(false);
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand"><span className="brand-mark">A</span> axlero</div>
        <p className="nav-label">WORKSPACE</p>
        <button className="nav-item active"><LayoutDashboard size={18}/> Overview</button>
        <button className="nav-item"><PanelsTopLeft size={18}/> My Dashboards</button>
        <button className="nav-item"><Database size={18}/> Data Sources</button>
        <button className="nav-item"><Settings size={18}/> Settings</button>
        <div className="sidebar-bottom">
          <div className="avatar">N</div>
          <div><strong>Workspace User</strong><small>Free workspace</small></div>
        </div>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <div className="breadcrumbs">Workspace <span>/</span> Overview</div>
          <div className="top-actions">
            <label className="search-box"><Search size={16}/><input placeholder="Search..." /></label>
            <button className="icon-button" aria-label="Notifications"><Bell size={18}/></button>
            <div className="avatar">N</div>
          </div>
        </header>

        <section className="page-heading">
          <div>
            <p className="eyebrow">YOUR WORKSPACE</p>
            <h1>Dashboard Overview</h1>
            <p className="subtitle">Track your metrics and build insights from your data.</p>
          </div>
          <button className="primary-button" onClick={() => setShowForm(!showForm)}>
            <Plus size={18}/> Create Widget
          </button>
        </section>

        {showForm && (
          <form className="widget-form" onSubmit={addWidget}>
            <label htmlFor="widget-title">Widget title</label>
            <input id="widget-title" value={title} onChange={e => setTitle(e.target.value)}
              placeholder="e.g. Monthly Revenue" required />
            <button className="primary-button" type="submit">Add Widget</button>
            <button className="secondary-button" type="button" onClick={() => setShowForm(false)}>Cancel</button>
          </form>
        )}

        <section className="stats-grid">
          <article className="stat-card">
            <div className="stat-top"><span>Total Revenue</span><TrendingUp size={19}/></div>
            <h2>₹1,24,500</h2><p className="positive">+12.8% <span>vs last month</span></p>
          </article>
          <article className="stat-card">
            <div className="stat-top"><span>Active Users</span><Users size={19}/></div>
            <h2>2,420</h2><p className="positive">+8.2% <span>vs last month</span></p>
          </article>
          <article className="stat-card">
            <div className="stat-top"><span>Data Sources</span><Database size={19}/></div>
            <h2>12</h2><p className="muted">Connected sources</p>
          </article>
          <article className="stat-card">
            <div className="stat-top"><span>Active Widgets</span><Activity size={19}/></div>
            <h2>{widgets.length}</h2><p className="muted">In this dashboard</p>
          </article>
        </section>

        <section className="content-grid">
          <article className="panel chart-panel">
            <div className="panel-heading"><div><h3>Revenue Analytics</h3><p>Monthly performance</p></div><span className="period-pill">Last 6 months</span></div>
            <div className="chart-wrap">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={salesData} margin={{ top: 12, right: 12, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e9edf4"/>
                  <XAxis dataKey="month" axisLine={false} tickLine={false} tick={{ fill: "#778198", fontSize: 12 }}/>
                  <YAxis axisLine={false} tickLine={false} tick={{ fill: "#778198", fontSize: 12 }} tickFormatter={v => `₹${v / 1000}k`}/>
                  <Tooltip formatter={v => [`₹${Number(v).toLocaleString("en-IN")}`, "Revenue"]}/>
                  <Bar dataKey="sales" fill="#6658e8" radius={[6, 6, 0, 0]} maxBarSize={42}/>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </article>

          <article className="panel activity-panel">
            <div className="panel-heading"><div><h3>Workspace Activity</h3><p>Recent updates</p></div></div>
            <div className="activity-item"><span className="activity-dot purple"/><div><strong>Dashboard created</strong><p>Revenue Overview</p><small>10 minutes ago</small></div></div>
            <div className="activity-item"><span className="activity-dot green"/><div><strong>Data source connected</strong><p>Sales records</p><small>1 hour ago</small></div></div>
            <div className="activity-item"><span className="activity-dot orange"/><div><strong>Widget updated</strong><p>Customer Analytics</p><small>Yesterday</small></div></div>
          </article>
        </section>

        <section className="panel widgets-panel">
          <div className="panel-heading"><div><h3>Your Widgets</h3><p>Manage your dashboard components</p></div></div>
          <div className="widgets-grid">
            {widgets.map(widget => (
              <article className="widget-card" key={widget.id}>
                <div className="widget-symbol"><PanelsTopLeft size={19}/></div>
                <div className="widget-info"><strong>{widget.title}</strong><p>{widget.type}</p></div>
                <button className="remove-button" onClick={() => setWidgets(widgets.filter(w => w.id !== widget.id))} aria-label={`Remove ${widget.title}`}>×</button>
              </article>
            ))}
          </div>
        </section>

        <footer className="footer">Axlero Dashboard Builder <span>Frontend preview · Sample data</span></footer>
      </main>
    </div>
  );
}
