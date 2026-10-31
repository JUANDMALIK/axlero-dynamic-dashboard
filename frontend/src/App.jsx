
import { useEffect, useMemo, useState } from "react";
import {
  Activity,
  Bell,
  Check,
  ChevronDown,
  Database,
  LayoutDashboard,
  LogOut,
  LockKeyhole,
  Mail,
  Plus,
  Search,
  Settings,
  ShoppingCart,
  TrendingUp,
  Users,
  X,

  Moon,
  Sun,} from "lucide-react";

import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from "recharts";
import { api, logout } from "./services/api";
import "./App.css";

const DEFAULT_WIDGETS = [
  { id: "default-users", title: "Total Users", type: "stat", key: "totalUsers" },
  { id: "default-sales", title: "Total Sales", type: "stat", key: "totalSales" },
  { id: "default-orders", title: "Total Orders", type: "stat", key: "totalOrders" },
];

const AVAILABLE_WIDGETS = [
  { key: "totalUsers", title: "Total Users" },
  { key: "totalSales", title: "Total Sales" },
  { key: "totalOrders", title: "Total Orders" },
];

const NAV_ITEMS = [
  { label: "Overview", icon: LayoutDashboard },
  { label: "My Dashboards", icon: Activity },
  { label: "Data Sources", icon: Database },
  { label: "Settings", icon: Settings },
];

const MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun"];

const SAMPLE_REVENUE = [12, 18, 15, 24, 21, 30];

function toApiWidget(widget) {
  return {
    title: widget.title,
    type: widget.type || "stat",
    dataKey: widget.key,
  };
}

function fromApiWidget(widget) {
  return {
    id: widget.id,
    title: widget.title,
    type: widget.type,
    key: widget.dataKey,
  };
}

function formatNumber(value) {
  if (value === null || value === undefined || value === "") return "—";
  if (typeof value === "number") return value.toLocaleString("en-IN");
  return value;
}

function formatSales(value) {
  if (value === null || value === undefined) return "—";
  return Number(value).toLocaleString("en-IN");
}

export default function App() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loggedIn, setLoggedIn] = useState(
    Boolean(sessionStorage.getItem("axlero_jwt_token"))
  );

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [dashboardData, setDashboardData] = useState(null);
  const [widgets, setWidgets] = useState(DEFAULT_WIDGETS);
  const [activeNav, setActiveNav] = useState("Overview");
  const [showWidgetOptions, setShowWidgetOptions] = useState(false);
  const [search, setSearch] = useState("");
  const [showNotifications, setShowNotifications] = useState(false);
  const [showProfile, setShowProfile] = useState(false);
  const [notice, setNotice] = useState("");
  const [datasetAnalytics, setDatasetAnalytics] = useState(null);
  const [datasetMetadata, setDatasetMetadata] = useState(null);
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploadingDataset, setUploadingDataset] = useState(false);
  const [draggedWidgetId, setDraggedWidgetId] = useState(null);

  const [theme, setTheme] = useState(
    () => localStorage.getItem("axlero_theme") || "light"
  );

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem("axlero_theme", theme);
  }, [theme]);

  useEffect(() => {
    if (!loggedIn) {
      setDatasetAnalytics(null);
      setDatasetMetadata(null);
      return;
    }

    loadDataset();

    let cancelled = false;

    async function initializeDashboard() {
      setLoading(true);
      setError("");

      try {
        const [data, savedWidgets] = await Promise.all([
          api.get("/dashboard"),
          api.get("/api/dashboard/widgets"),
        ]);

        if (!Array.isArray(savedWidgets)) {
          throw new Error("Unexpected response from the widget API.");
        }

        let finalWidgets = savedWidgets;

        if (savedWidgets.length === 0) {
          finalWidgets = await api.put(
            "/api/dashboard/widgets",
            DEFAULT_WIDGETS.map(toApiWidget)
          );
        }

        if (!Array.isArray(finalWidgets)) {
          throw new Error("The backend did not return saved widgets.");
        }

        if (cancelled) return;

        setDashboardData(data);
        setWidgets(finalWidgets.map(fromApiWidget));
      } catch (err) {
        if (!cancelled) {
          setError(err.message || "Unable to load dashboard data.");
          console.error("Dashboard initialization failed:", err);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    initializeDashboard();

    return () => {
      cancelled = true;
    };
  }, [loggedIn]);

  async function loadDataset() {
    try {
      const analytics = await api.getDatasetAnalytics();
      setDatasetAnalytics(analytics);
      setDatasetMetadata(analytics);
    } catch (err) {
      setDatasetAnalytics(null);
      setDatasetMetadata(null);
      console.info("No uploaded dataset is available yet:", err.message);
    }
  }

  async function handleDatasetUpload() {
    if (!selectedFile) {
      setError("Please choose a CSV file first.");
      return;
    }

    if (!selectedFile.name.toLowerCase().endsWith(".csv")) {
      setError("Only CSV files are supported.");
      return;
    }

    if (selectedFile.size === 0) {
      setError("The selected CSV file is empty.");
      return;
    }

    setUploadingDataset(true);
    setError("");
    setNotice("");

    try {
      await api.uploadDataset(selectedFile);
      await loadDataset();
      setNotice(`${selectedFile.name} uploaded and analyzed successfully.`);
      setSelectedFile(null);
    } catch (err) {
      setError(err.message || "CSV upload failed.");
    } finally {
      setUploadingDataset(false);
    }
  }
  async function handleLogin(event) {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      await api.login(email, password);
      setLoggedIn(true);
    } catch (err) {
      setError(err.message || "Sign in failed. Check your email and password.");
    } finally {
      setLoading(false);
    }
  }

  async function refreshDashboard() {
    setLoading(true);
    setError("");
    setNotice("");

    try {
      const [data, savedWidgets] = await Promise.all([
        api.get("/dashboard"),
        api.get("/api/dashboard/widgets"),
      ]);

      if (!Array.isArray(savedWidgets)) {
        throw new Error("Unexpected response from the widget API.");
      }

      setDashboardData(data);
      setWidgets(savedWidgets.map(fromApiWidget));
      setNotice("Dashboard refreshed successfully.");
    } catch (err) {
      setError(err.message || "Could not refresh the dashboard.");
    } finally {
      setLoading(false);
    }
  }

  function handleLogout() {
    logout();
    setLoggedIn(false);
    setDashboardData(null);
    setWidgets(DEFAULT_WIDGETS);
    setPassword("");
    setError("");
    setNotice("");
  }

  async function saveWidgets(updatedWidgets) {
    const saved = await api.put(
      "/api/dashboard/widgets",
      updatedWidgets.map(toApiWidget)
    );

    if (!Array.isArray(saved)) {
      throw new Error("The backend did not return the saved widgets.");
    }

    setWidgets(saved.map(fromApiWidget));
  }

  async function addWidget(option) {
    if (widgets.some((widget) => widget.key === option.key)) {
      setNotice(`${option.title} is already on your dashboard.`);
      setShowWidgetOptions(false);
      return;
    }

    const updated = [
      ...widgets,
      {
        id: `new-${option.key}`,
        title: option.title,
        type: "stat",
        key: option.key,
      },
    ];

    setLoading(true);
    setError("");
    setNotice("");

    try {
      await saveWidgets(updated);
      setShowWidgetOptions(false);
      setNotice(`${option.title} added successfully.`);
    } catch (err) {
      setError(err.message || "Could not save this widget.");
    } finally {
      setLoading(false);
    }
  }

  async function handleWidgetDrop(targetId) {
    const sourceIndex = widgets.findIndex(
      (widget) => widget.id === draggedWidgetId
    );
    const targetIndex = widgets.findIndex(
      (widget) => widget.id === targetId
    );

    if (
      sourceIndex < 0 ||
      targetIndex < 0 ||
      sourceIndex === targetIndex
    ) {
      setDraggedWidgetId(null);
      return;
    }

    const updated = [...widgets];
    const [movedWidget] = updated.splice(sourceIndex, 1);
    updated.splice(targetIndex, 0, movedWidget);

    setDraggedWidgetId(null);
    setLoading(true);
    setError("");
    setNotice("");

    try {
      await saveWidgets(updated);
      setNotice("Widget order updated successfully.");
    } catch (err) {
      setError(err.message || "Could not update widget order.");
    } finally {
      setLoading(false);
    }
  }
  async function removeWidget(id) {
    const updated = widgets.filter((widget) => widget.id !== id);

    setLoading(true);
    setError("");
    setNotice("");

    try {
      await saveWidgets(updated);
      setNotice("Widget removed successfully.");
    } catch (err) {
      setError(err.message || "Could not remove this widget.");
    } finally {
      setLoading(false);
    }
  }

  const filteredWidgets = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return widgets;

    return widgets.filter((widget) =>
      widget.title.toLowerCase().includes(term)
    );
  }, [widgets, search]);

  const totalRevenue = dashboardData?.totalSales;
  const totalUsers = dashboardData?.totalUsers;
  const totalOrders = dashboardData?.totalOrders;
  const chartData = Array.isArray(datasetAnalytics?.chartData)
    ? datasetAnalytics.chartData
    : [];

  const numericSeries = Array.isArray(datasetAnalytics?.numericColumns)
    ? datasetAnalytics.numericColumns
    : [];

  function widgetValue(widget) {
    const value = dashboardData?.[widget.key];
    return widget.key === "totalSales"
      ? formatSales(value)
      : formatNumber(value);
  }

  if (!loggedIn) {
    return (
      <main className="login-page">
        <section className="login-card">
          <div className="brand login-brand">
            <span className="brand-logo">A</span>
            <span>axlero</span>
          </div>

          <p className="login-eyebrow">WELCOME BACK</p>
          <h1>Sign in to your workspace</h1>
          <p className="muted login-description">
            Access your dashboards, data, and insights in one place.
          </p>

          <form className="login-form" onSubmit={handleLogin}>
            <label htmlFor="email">Email address</label>
            <div className="input-wrap">
              <Mail size={18} />
              <input
                id="email"
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="you@example.com"
                autoComplete="email"
                required
              />
            </div>

            <label htmlFor="password">Password</label>
            <div className="input-wrap">
              <LockKeyhole size={18} />
              <input
                id="password"
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="Enter your password"
                autoComplete="current-password"
                required
              />
            </div>

            {error && <div className="alert-error">{error}</div>}

            <button className="btn btn-primary login-submit" disabled={loading}>
              {loading ? "Signing in..." : "Sign In"}
            </button>
          </form>

          <p className="login-footer">
            Secure access powered by your Axlero backend.
          </p>
        </section>
      </main>
    );
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <button
          className="brand sidebar-brand"
          onClick={() => setActiveNav("Overview")}
          aria-label="Axlero overview"
        >
          <span className="brand-logo">A</span>
          <span>axlero</span>
        </button>

        <p className="nav-section-label">WORKSPACE</p>

        <nav className="sidebar-nav" aria-label="Main navigation">
          {NAV_ITEMS.map(({ label, icon: Icon }) => (
            <button
              key={label}
              className={`nav-item ${activeNav === label ? "active" : ""}`}
              onClick={() => {
                setActiveNav(label);
                setShowWidgetOptions(false);
                setError("");
                setNotice("");
              }}
            >
              <Icon size={19} />
              <span>{label}</span>
            </button>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <div className="avatar">N</div>
          <div className="sidebar-user">
            <strong>Workspace User</strong>
            <span>Personal workspace</span>
          </div>
          <button
            className="icon-button sidebar-logout"
            onClick={handleLogout}
            aria-label="Sign out"
            title="Sign out"
          >
            <LogOut size={18} />
          </button>
        </div>
      </aside>

      <main className="workspace">
        <header className="topbar">
          <div className="breadcrumbs">
            <span>Workspace</span>
            <span className="breadcrumb-separator">/</span>
            <strong>{activeNav}</strong>
          </div>

          <div className="topbar-actions">
          <button
            type="button"
            className="icon-button theme-toggle"
            onClick={() =>
              setTheme((current) => current === "light" ? "dark" : "light")
            }
            aria-label={`Switch to ${theme === "light" ? "dark" : "light"} mode`}
            title={`Switch to ${theme === "light" ? "dark" : "light"} mode`}
          >
            {theme === "light" ? <Moon size={19} /> : <Sun size={19} />}
          </button>
            <label className="search-box">
              <Search size={18} />
              <input
                aria-label="Search widgets"
                value={search}
                onChange={(event) => {
                  setSearch(event.target.value);
                  setActiveNav("Overview");
                }}
                placeholder="Search widgets..."
              />
              {search && (
                <button
                  type="button"
                  className="search-clear"
                  onClick={() => setSearch("")}
                  aria-label="Clear search"
                >
                  <X size={15} />
                </button>
              )}
            </label>

            <div className="popup-anchor">
              <button
                className={`icon-button ${showNotifications ? "selected" : ""}`}
                onClick={() => {
                  setShowNotifications((value) => !value);
                  setShowProfile(false);
                }}
                aria-label="Notifications"
                title="Notifications"
              >
                <Bell size={19} />
                <span className="notification-dot" />
              </button>

              {showNotifications && (
                <div className="small-popup">
                  <strong>Notifications</strong>
                  <p>Your Axlero workspace is ready.</p>
                  <p>Dashboard data is available from your backend.</p>
                </div>
              )}
            </div>

            <div className="popup-anchor">
              <button
                className="profile-button"
                onClick={() => {
                  setShowProfile((value) => !value);
                  setShowNotifications(false);
                }}
                aria-label="Open profile menu"
              >
                N <ChevronDown size={14} />
              </button>

              {showProfile && (
                <div className="small-popup profile-popup">
                  <strong>Workspace User</strong>
                  <p>Axlero workspace</p>
                  <button onClick={handleLogout}>
                    <LogOut size={16} /> Sign out
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>

        {error && (
          <div className="alert-error page-alert">
            <span>{error}</span>
            <button onClick={() => setError("")} aria-label="Dismiss error">
              <X size={17} />
            </button>
          </div>
        )}

        {notice && (
          <div className="alert-success page-alert">
            <Check size={17} />
            <span>{notice}</span>
            <button onClick={() => setNotice("")} aria-label="Dismiss message">
              <X size={17} />
            </button>
          </div>
        )}

        {activeNav === "Overview" && (
          <>
           
<section className="welcome-banner">
  <div className="welcome-copy">
    <span className="welcome-tag">YOUR WORKSPACE</span>
    <h2>Welcome back</h2>
    <p>
      Here's your workspace at a glance. Keep track of your
      metrics and manage your dashboards in one place.
    </p>
  </div>
  <div className="welcome-date">
    <span> TODAY </span>
    <strong>
      {new Date().toLocaleDateString("en-IN", {
        day: "numeric",
        month: "short",
        year: "numeric",
      })}
    </strong>
  </div>
</section>

            <section className="page-heading">
              <div>
                <p className="eyebrow">YOUR WORKSPACE</p>
                <h1>Dashboard Overview</h1>
                <p className="muted">
                  Track your metrics and build insights from your data.
                </p>
              </div>

              <button
                className="btn btn-primary"
                onClick={() => setShowWidgetOptions((value) => !value)}
                disabled={loading}
              >
                <Plus size={18} />
                {showWidgetOptions ? "Close" : "Create Widget"}
              </button>
            </section>

            <div className="connection-line">
              <span className={`connection-indicator ${dashboardData ? "online" : ""}`} />
              <span>
                {dashboardData
                  ? "Backend connected — live dashboard response"
                  : "Connecting to your backend..."}
              </span>
              <button
                className="text-button"
                onClick={refreshDashboard}
                disabled={loading}
              >
                {loading ? "Loading..." : "Refresh data"}
              </button>
            </div>

            {showWidgetOptions && (
              <section className="widget-creator panel">
                <div className="section-heading">
                  <div>
                    <h2>Create a widget</h2>
                    <p className="muted">
                      Choose a statistic to add to your dashboard.
                    </p>
                  </div>
                  <button
                    className="icon-button"
                    onClick={() => setShowWidgetOptions(false)}
                    aria-label="Close widget chooser"
                  >
                    <X size={18} />
                  </button>
                </div>

                <div className="creator-options">
                  {AVAILABLE_WIDGETS.map((option) => {
                    const exists = widgets.some(
                      (widget) => widget.key === option.key
                    );

                    return (
                      <button
                        key={option.key}
                        className="creator-option"
                        disabled={exists || loading}
                        onClick={() => addWidget(option)}
                      >
                        <span className="creator-icon">
                          {option.key === "totalUsers" && <Users size={20} />}
                          {option.key === "totalSales" && <TrendingUp size={20} />}
                          {option.key === "totalOrders" && <ShoppingCart size={20} />}
                        </span>
                        <span className="creator-text">
                          <strong>{option.title}</strong>
                          <small>{exists ? "Already added" : "Add to dashboard"}</small>
                        </span>
                        {exists ? <Check size={17} /> : <Plus size={17} />}
                      </button>
                    );
                  })}
                </div>
              </section>
            )}

            <section className="metric-grid">
              <article className="metric-card">
                <div className="metric-top">
                  <span>Total Sales</span>
                  <span className="metric-icon purple"><TrendingUp size={19} /></span>
                </div>
                <strong className="metric-value">
                  {totalRevenue == null ? "—" : `₹${formatSales(totalRevenue)}`}
                </strong>
                <span className="metric-caption">Reported by dashboard API</span>
              </article>

              <article className="metric-card">
                <div className="metric-top">
                  <span>Total Users</span>
                  <span className="metric-icon green"><Users size={19} /></span>
                </div>
                <strong className="metric-value">{formatNumber(totalUsers)}</strong>
                <span className="metric-caption">Reported by dashboard API</span>
              </article>

              <article className="metric-card">
                <div className="metric-top">
                  <span>Total Orders</span>
                  <span className="metric-icon amber"><ShoppingCart size={19} /></span>
                </div>
                <strong className="metric-value">{formatNumber(totalOrders)}</strong>
                <span className="metric-caption">Reported by dashboard API</span>
              </article>

              <article className="metric-card">
                <div className="metric-top">
                  <span>Active Widgets</span>
                  <span className="metric-icon blue"><Activity size={19} /></span>
                </div>
                <strong className="metric-value">{widgets.length}</strong>
                <span className="metric-caption">Saved in your workspace</span>
              </article>
            </section>

            <section className="content-grid">
              <article className="panel revenue-panel">
  <div className="section-heading">
    <div>
      <h2>CSV Data Analytics</h2>
      <p className="muted">
        Visualizing numeric columns from your uploaded dataset
      </p>
    </div>
    <span className="sample-label">
      {chartData.length > 0 ? "Live dataset" : "No dataset"}
    </span>
  </div>

  {chartData.length > 0 && numericSeries.length > 0 ? (
    <>
      <div className="dataset-chart">
        <ResponsiveContainer width="100%" height={320}>
          <BarChart
            data={chartData}
            margin={{ top: 10, right: 20, left: 5, bottom: 55 }}
          >
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis
              dataKey="label"
              angle={-25}
              textAnchor="end"
              interval="preserveStartEnd"
              height={65}
            />
            <YAxis />
            <Tooltip />
            <Legend />
            {numericSeries.map((column, index) => (
              <Bar
                key={column.column}
                dataKey={column.column}
                name={column.column}
                fill={["#6366f1", "#0d9488", "#f59e0b", "#ec4899", "#2563eb"][index % 5]}
                radius={[5, 5, 0, 0]}
              />
            ))}
          </BarChart>
        </ResponsiveContainer>
      </div>
      <p className="chart-disclaimer">
        Showing {chartData.length.toLocaleString("en-IN")} records across{" "}
        {numericSeries.length} numeric columns.
      </p>
    </>
  ) : (
    <div className="empty-state dataset-empty-state">
      <Database size={30} />
      <strong>No CSV analytics available</strong>
      <p>Upload a CSV file in Data Sources to generate charts from your data.</p>
    </div>
  )}
</article>

              <article className="panel activity-panel">
                <div className="section-heading">
                  <div>
                    <h2>Workspace Activity</h2>
                    <p className="muted">Workspace overview</p>
                  </div>
                  <Activity size={19} className="muted-icon" />
                </div>

                <div className="activity-list">
                  <div className="activity-item">
                    <span className="activity-dot violet" />
                    <div>
                      <strong>Dashboard opened</strong>
                      <p>Overview workspace</p>
                      <small>Current session</small>
                    </div>
                  </div>

                  <div className="activity-item">
                    <span className="activity-dot green-dot" />
                    <div>
                      <strong>Backend connected</strong>
                      <p>
                        {dashboardData
                          ? "Dashboard API responded successfully"
                          : "Waiting for backend data"}
                      </p>
                      <small>{dashboardData ? "Connected" : "Pending"}</small>
                    </div>
                  </div>

                  <div className="activity-item">
                    <span className="activity-dot amber-dot" />
                    <div>
                      <strong>Saved widgets</strong>
                      <p>{widgets.length} widget configurations loaded</p>
                      <small>Current workspace</small>
                    </div>
                  </div>
                </div>
              </article>
            </section>

            <section className="panel widgets-panel">
              <div className="section-heading">
                <div>
                  <h2>My Widgets</h2>
                  <p className="muted">
                    Your saved dashboard statistics
                  </p>
                </div>
                <button
                  className="btn btn-secondary"
                  onClick={() => setShowWidgetOptions((value) => !value)}
                  disabled={loading}
                >
                  <Plus size={17} /> Add Widget
                </button>
              </div>

              {filteredWidgets.length === 0 ? (
                <div className="empty-state">
                  <LayoutDashboard size={30} />
                  <strong>{search ? "No matching widgets" : "No widgets yet"}</strong>
                  <p>
                    {search
                      ? "Try a different search term."
                      : "Create a widget to start customizing your dashboard."}
                  </p>
                  {!search && (
                    <button
                      className="btn btn-primary"
                      onClick={() => setShowWidgetOptions(true)}
                    >
                      <Plus size={17} /> Create your first widget
                    </button>
                  )}
                </div>
              ) : (
                <div className="saved-widget-grid">
                  {filteredWidgets.map((widget) => (
                    <article
                      className={`saved-widget-card ${draggedWidgetId === widget.id ? "is-dragging" : ""}`}
                      key={widget.id}
                      draggable={!loading && !search.trim()}
                      onDragStart={() => setDraggedWidgetId(widget.id)}
                      onDragOver={(event) => event.preventDefault()}
                      onDrop={(event) => {
                        event.preventDefault();
                        handleWidgetDrop(widget.id);
                      }}
                      onDragEnd={() => setDraggedWidgetId(null)}
                    >
                      <div className="saved-widget-top">
                        <span>{widget.title}</span>
                        <button
                          className="icon-button remove-button"
                          aria-label={`Remove ${widget.title}`}
                          title="Remove widget"
                          disabled={loading}
                          onClick={() => removeWidget(widget.id)}
                        >
                          <X size={17} />
                        </button>
                      </div>
                      <strong className="saved-widget-value">
                        {widgetValue(widget)}
                      </strong>
                      <span className="saved-widget-caption">
                        From the dashboard API
                      </span>
                    </article>
                  ))}
                </div>
              )}
            </section>
          </>
        )}

        {activeNav === "My Dashboards" && (
          <section className="subpage">
            <p className="eyebrow">WORKSPACE</p>
            <h1>My Dashboards</h1>
            <p className="muted">
              Your current dashboard and its saved widget configurations.
            </p>
            <article className="panel dashboard-list-card">
              <div className="list-icon"><LayoutDashboard size={22} /></div>
              <div className="list-copy">
                <strong>Overview Dashboard</strong>
                <p>{widgets.length} saved widgets</p>
              </div>
              <span className="status-pill"><Check size={14} /> Available</span>
              <button
                className="btn btn-secondary"
                onClick={() => setActiveNav("Overview")}
              >
                Open dashboard
              </button>
            </article>
          </section>
        )}

        {activeNav === "Data Sources" && (
          <section className="subpage">
            <p className="eyebrow">INTEGRATIONS</p>
            <h1>Data Sources</h1>
            <p className="muted">
              Monitor the backend connection that provides dashboard statistics.
            </p>
            <article className="panel data-source-card">
              <div className="list-icon"><Database size={22} /></div>
              <div className="list-copy">
                <strong>Axlero Spring Boot API</strong>
                <p>Local development server · localhost:8080</p>
                <small>Dashboard and widget endpoints</small>
              </div>
              <span className={`status-pill ${dashboardData ? "" : "status-pending"}`}>
                <span className="connection-indicator online" />
                {dashboardData ? "Connected" : "Check connection"}
              </span>
              <button className="btn btn-secondary" onClick={refreshDashboard} disabled={loading}>
                {loading ? "Checking..." : "Test connection"}
              </button>
            </article>
            <article className="panel dataset-upload-panel">
              <div className="section-heading">
                <div>
                  <h2>Upload CSV Dataset</h2>
                  <p className="muted">
                    Upload your sales or business data to generate live dashboard analytics.
                  </p>
                </div>
                <Database size={24} />
              </div>

              <div className="dataset-upload-controls">
                <input
                  type="file"
                  accept=".csv,text/csv"
                  onChange={(event) =>
                    setSelectedFile(event.target.files?.[0] || null)
                  }
                  disabled={uploadingDataset}
                />

                <button
                  className="btn btn-primary"
                  onClick={handleDatasetUpload}
                  disabled={!selectedFile || uploadingDataset}
                >
                  {uploadingDataset ? "Uploading..." : "Upload and Analyze"}
                </button>
              </div>

              {selectedFile && (
                <p className="muted">
                  Selected file: <strong>{selectedFile.name}</strong>
                </p>
              )}

              {datasetMetadata && (
                <div className="dataset-summary">
                  <h3>Latest Uploaded Dataset</h3>
                  <p><strong>File:</strong> {datasetMetadata.filename}</p>
                  <p><strong>Rows:</strong> {datasetMetadata.rowCount}</p>
                  <p><strong>Columns:</strong> {datasetMetadata.columnCount}</p>
                  <p>
                    <strong>Uploaded:</strong>{" "}
                    {datasetMetadata.uploadedAt
                      ? new Date(datasetMetadata.uploadedAt).toLocaleString()
                      : "Available"}
                  </p>
                </div>
              )}
            </article>

            <p className="info-note">
              CSV analytics are generated from your uploaded dataset.
              The activity feed may still contain illustrative frontend content.
            </p>
          </section>
        )}

        {activeNav === "Settings" && (
          <section className="subpage">
            <p className="eyebrow">PREFERENCES</p>
            <h1>Settings</h1>
            <p className="muted">Manage your current workspace session.</p>
            <article className="panel settings-card">
              <div className="settings-row">
                <div className="list-icon"><LockKeyhole size={21} /></div>
                <div className="list-copy">
                  <strong>Authentication</strong>
                  <p>Signed in using your backend-issued JWT session token.</p>
                </div>
                <span className="status-pill"><Check size={14} /> Active</span>
              </div>
              <div className="settings-row">
                <div className="list-icon"><Database size={21} /></div>
                <div className="list-copy">
                  <strong>Widget persistence</strong>
                  <p>Widget configurations are loaded and saved through the API.</p>
                </div>
                <button className="btn btn-secondary" onClick={refreshDashboard} disabled={loading}>
                  Verify now
                </button>
              </div>
              <div className="settings-row">
                <div className="list-icon"><LogOut size={21} /></div>
                <div className="list-copy">
                  <strong>Sign out</strong>
                  <p>End your current workspace session.</p>
                </div>
                <button className="btn btn-danger" onClick={handleLogout}>
                  <LogOut size={16} /> Sign out
                </button>
              </div>
            </article>
          </section>
        )}

        <footer className="workspace-footer">
          <span>© {new Date().getFullYear()} Axlero</span>
          <span>Workspace analytics</span>
        </footer>
      </main>
    </div>
  );
}





