import { useEffect, useState } from "react";
import {
  Activity,
  ArrowDown,
  ArrowUp,
  BarChart3,
  ChartNoAxesCombined,
  Download,
  FileSpreadsheet,
  GripVertical,
  LayoutDashboard,
  LineChart as LineChartIcon,
  LogOut,
  PieChart as PieChartIcon,
  Plus,
  RefreshCw,
  ShieldCheck,
  Trash2,
  Upload,
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { api } from "./services/api.js";
import "./App.css";

const chartTypes = [
  { value: "BAR", label: "Bar chart", Icon: BarChart3 },
  { value: "LINE", label: "Line chart", Icon: LineChartIcon },
  { value: "PIE", label: "Pie chart", Icon: PieChartIcon },
];

const metricTypes = [
  { value: "COUNT", label: "Count" },
  { value: "SUM", label: "Sum" },
  { value: "AVERAGE", label: "Average" },
];

function AuthScreen({ onAuthenticated }) {
  const [mode, setMode] = useState("login");
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const isRegistering = mode === "register";

  async function submit(event) {
    event.preventDefault();
    setError("");
    setSubmitting(true);

    try {
      const result = isRegistering
        ? await api.register({ username, email, password })
        : await api.login({ username, password });
      api.saveSession(result);
      onAuthenticated({ token: result.token, username: result.username });
    } catch (requestError) {
      if (requestError.status === 401 && !isRegistering) {
        setError("We couldn't sign you in. Check your username and password, or create an account if you haven't registered yet.");
      } else if (requestError.status === 401) {
        setError("The server denied this registration request (401). Refresh the page and try again. If it continues, contact support with this status.");
      } else {
        setError(requestError.message || "The request failed. Check your connection and try again.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  function changeMode(nextMode) {
    setMode(nextMode);
    setError("");
  }

  return (
    <main className="auth-layout">
      <section className="auth-card">
        <div className="brand">
          <span className="brand-mark">A</span>
          axlero
        </div>
        <p className="eyebrow">DASHBOARD WORKSPACE</p>
        <h1>{isRegistering ? "Create your account" : "Welcome back"}</h1>
        <p className="subtitle">
          {isRegistering
            ? "Register to access your dashboard."
            : "Sign in to continue to your dashboard."}
        </p>

        <form className="auth-form" onSubmit={submit}>
          <label htmlFor="username">Username</label>
          <input
            autoComplete="username"
            id="username"
            onChange={(event) => setUsername(event.target.value)}
            required
            value={username}
          />
          {isRegistering && (
            <>
              <label htmlFor="email">Email</label>
              <input
                autoComplete="email"
                id="email"
                onChange={(event) => setEmail(event.target.value)}
                required
                type="email"
                value={email}
              />
            </>
          )}
          <label htmlFor="password">Password</label>
          <input
            autoComplete={isRegistering ? "new-password" : "current-password"}
            id="password"
            minLength={isRegistering ? 8 : undefined}
            onChange={(event) => setPassword(event.target.value)}
            required
            type="password"
            value={password}
          />
          {error && (
            <p aria-live="assertive" className="form-error auth-error" role="alert">
              {error}
            </p>
          )}
          <button className="primary-button auth-submit" disabled={submitting} type="submit">
            {submitting ? "Please wait…" : isRegistering ? "Create account" : "Sign in"}
          </button>
        </form>

        <p className="auth-switch">
          {isRegistering ? "Already have an account?" : "New to Axlero?"}{" "}
          <button
            onClick={() => changeMode(isRegistering ? "login" : "register")}
            type="button"
          >
            {isRegistering ? "Sign in" : "Create an account"}
          </button>
        </p>
      </section>
    </main>
  );
}

function ChartPreview({ widget }) {
  const data = Array.isArray(widget.data) ? widget.data : [];
  const chartProps = {
    data,
    margin: { top: 12, right: 12, left: 0, bottom: 0 },
  };

  if (data.length === 0) {
    return (
      <div className="chart-preview">
        <div className="chart-empty" role="status">
          <ChartNoAxesCombined size={19} />
          <span>{widget.dataError || "No chart data is available for this widget."}</span>
        </div>
      </div>
    );
  }

  return (
    <div className="chart-preview" aria-label={`${widget.chartType.toLowerCase()} chart`}>
      <ResponsiveContainer width="100%" height="100%">
        {widget.chartType === "LINE" ? (
          <LineChart {...chartProps}>
            <CartesianGrid stroke="#edf0f5" strokeDasharray="3 3" vertical={false} />
            <XAxis dataKey="name" />
            <YAxis />
            <Tooltip />
            <Line dataKey="value" stroke="#6658e8" type="monotone" />
          </LineChart>
        ) : widget.chartType === "PIE" ? (
          <PieChart>
            <Tooltip />
            <Pie data={data} dataKey="value" nameKey="name" />
          </PieChart>
        ) : (
          <BarChart {...chartProps}>
            <CartesianGrid stroke="#edf0f5" strokeDasharray="3 3" vertical={false} />
            <XAxis dataKey="name" />
            <YAxis />
            <Tooltip />
            <Bar dataKey="value" fill="#6658e8" radius={[5, 5, 0, 0]} />
          </BarChart>
        )}
      </ResponsiveContainer>
    </div>
  );
}

function DashboardLoading() {
  return (
    <section aria-live="polite" className="state-panel">
      <span className="loading-spinner" />
      <h2>Loading your dashboard</h2>
      <p>Connecting securely to your workspace…</p>
    </section>
  );
}

function DashboardError({ error, onRetry, onSignOut }) {
  return (
    <section className="state-panel state-panel-error" role="alert">
      <div className="state-icon"><Activity size={20} /></div>
      <h2>Could not load your dashboard</h2>
      <p>{error}</p>
      <div className="state-actions">
        <button className="primary-button" onClick={onRetry} type="button">
          <RefreshCw size={16} /> Try again
        </button>
        <button className="secondary-button" onClick={onSignOut} type="button">
          Sign out
        </button>
      </div>
    </section>
  );
}

function WidgetCard({ widget, index, total, onMove, onRemove, onDragStart, onDragOver, onDrop }) {
  const chart = chartTypes.find((type) => type.value === widget.chartType);
  const metric = metricTypes.find((type) => type.value === widget.metricType);

  return (
    <article
      className="widget-card"
      draggable
      onDragOver={onDragOver}
      onDragStart={onDragStart}
      onDrop={onDrop}
    >
      <header className="widget-header">
        <div className="widget-title">
          <GripVertical aria-hidden="true" className="drag-handle" size={18} />
          <div>
            <h3>{widget.title}</h3>
            <p>{metric?.label} · {chart?.label}</p>
          </div>
        </div>
        <div className="widget-actions">
          <button
            aria-label={`Move ${widget.title} up`}
            disabled={index === 0}
            onClick={() => onMove(index, index - 1)}
            title="Move up"
            type="button"
          >
            <ArrowUp size={16} />
          </button>
          <button
            aria-label={`Move ${widget.title} down`}
            disabled={index === total - 1}
            onClick={() => onMove(index, index + 1)}
            title="Move down"
            type="button"
          >
            <ArrowDown size={16} />
          </button>
          <button
            aria-label={`Remove ${widget.title}`}
            onClick={() => onRemove(widget.id)}
            title="Remove widget"
            type="button"
          >
            <Trash2 size={16} />
          </button>
        </div>
      </header>
      <ChartPreview widget={widget} />
      <footer className="widget-fields">
        <span>Value field: <strong>{widget.valueField || "Not set"}</strong></span>
        <span>Group by: <strong>{widget.groupByField || "None"}</strong></span>
      </footer>
    </article>
  );
}

function DashboardWorkspace({
  dashboard,
  dataset,
  onSignOut,
  setDataset,
  username,
  widgets,
  setWidgets,
}) {
  const [showBuilder, setShowBuilder] = useState(false);
  const [title, setTitle] = useState("");
  const [metricType, setMetricType] = useState("COUNT");
  const [chartType, setChartType] = useState("BAR");
  const [valueField, setValueField] = useState("");
  const [groupByField, setGroupByField] = useState("");
  const [formError, setFormError] = useState("");
  const [widgetError, setWidgetError] = useState("");
  const [savingWidget, setSavingWidget] = useState(false);
  const [uploadFile, setUploadFile] = useState(null);
  const [uploadingDataset, setUploadingDataset] = useState(false);
  const [datasetError, setDatasetError] = useState("");
  const [exporting, setExporting] = useState("");

  async function addWidget(event) {
    event.preventDefault();
    const cleanTitle = title.trim();
    const cleanValueField = valueField.trim();

    if (!dataset) {
      setFormError("Upload a dataset before creating a chart.");
      return;
    }
    if (!cleanTitle || !cleanValueField) {
      setFormError("Enter a widget title and select a value column.");
      return;
    }

    setSavingWidget(true);
    setFormError("");
    setWidgetError("");
    try {
      const savedWidget = await api.createWidget({
        title: cleanTitle,
        chartType,
        metricType,
        valueField: cleanValueField,
        groupByField: groupByField.trim(),
      });
      let chartData = [];
      let dataError = "";
      try {
        chartData = await api.getWidgetData(savedWidget.id);
      } catch (error) {
        if (error.status === 401) throw error;
        dataError = error.message;
      }
      setWidgets((current) => [...current, { ...savedWidget, data: chartData, dataError }]);
      setTitle("");
      setValueField("");
      setGroupByField("");
      setShowBuilder(false);
    } catch (error) {
      if (error.status === 401) {
        onSignOut();
      } else {
        setFormError(error.message);
      }
    } finally {
      setSavingWidget(false);
    }
  }

  async function uploadDatasetFile(event) {
    event.preventDefault();
    const uploadForm = event.currentTarget;
    if (!uploadFile) {
      setDatasetError("Choose a CSV or XLSX file to upload.");
      return;
    }
    if (dataset && !window.confirm("Uploading a new file replaces the current dataset for this dashboard. Continue?")) {
      return;
    }

    setUploadingDataset(true);
    setDatasetError("");
    setWidgetError("");
    try {
      const uploaded = await api.uploadDataset(uploadFile);
      const updatedWidgets = await Promise.all(widgets.map(async (widget) => {
        try {
          const data = await api.getWidgetData(widget.id);
          return { ...widget, data, dataError: "" };
        } catch (error) {
          if (error.status === 401) throw error;
          return { ...widget, data: [], dataError: error.message };
        }
      }));
      setDataset(uploaded);
      setWidgets(updatedWidgets);
      setUploadFile(null);
      uploadForm.reset();
      if (valueField && !uploaded.columns.includes(valueField)) setValueField("");
      if (groupByField && !uploaded.columns.includes(groupByField)) setGroupByField("");
    } catch (error) {
      if (error.status === 401) {
        onSignOut();
      } else {
        setDatasetError(error.message);
      }
    } finally {
      setUploadingDataset(false);
    }
  }

  async function removeDataset() {
    if (!window.confirm("Remove this dataset? Saved widgets will remain, but their charts will no longer have data.")) {
      return;
    }
    setDatasetError("");
    try {
      await api.deleteDataset();
      setDataset(null);
      setWidgets((current) => current.map((widget) => ({
        ...widget,
        data: [],
        dataError: "Upload a dataset to display this chart.",
      })));
    } catch (error) {
      if (error.status === 401) onSignOut();
      else setDatasetError(error.message);
    }
  }

  async function moveWidget(from, to) {
    if (to < 0 || to >= widgets.length) return;
    const next = [...widgets];
    const [moved] = next.splice(from, 1);
    next.splice(to, 0, moved);
    setWidgetError("");
    try {
      const savedOrder = await api.reorderWidgets(next.map((widget) => widget.id));
      const currentById = new Map(widgets.map((widget) => [widget.id, widget]));
      setWidgets(savedOrder.map((widget) => ({ ...currentById.get(widget.id), ...widget })));
    } catch (error) {
      if (error.status === 401) {
        onSignOut();
      } else {
        setWidgetError(error.message);
      }
    }
  }

  function dropWidget(targetId, event) {
    event.preventDefault();
    const draggedId = Number(event.dataTransfer.getData("text/plain"));
    if (!Number.isInteger(draggedId) || draggedId === targetId) return;
    const from = widgets.findIndex((widget) => widget.id === draggedId);
    const to = widgets.findIndex((widget) => widget.id === targetId);
    if (from >= 0 && to >= 0) moveWidget(from, to);
  }

  async function removeWidget(widgetId) {
    setWidgetError("");
    try {
      await api.deleteWidget(widgetId);
      setWidgets((current) => current.filter((widget) => widget.id !== widgetId));
    } catch (error) {
      if (error.status === 401) {
        onSignOut();
      } else {
        setWidgetError(error.message);
      }
    }
  }

  async function exportWidgets(format) {
    setExporting(format);
    setWidgetError("");
    try {
      const blob = await api.exportWidgets(format);
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = format === "csv" ? "dashboard-results.csv" : "dashboard-results.xlsx";
      document.body.append(link);
      link.click();
      link.remove();
      setTimeout(() => URL.revokeObjectURL(url), 0);
    } catch (error) {
      if (error.status === 401) {
        onSignOut();
      } else {
        setWidgetError(error.message);
      }
    } finally {
      setExporting("");
    }
  }

  const dashboardName = dashboard?.name || "My Dashboard";

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand"><span className="brand-mark">A</span> axlero</div>
        <p className="nav-label">WORKSPACE</p>
        <div className="nav-item active"><LayoutDashboard size={18} /> Overview</div>
        <div className="sidebar-note">
          <ShieldCheck size={17} />
          <span>Signed in securely as <strong>{username}</strong></span>
        </div>
        <div className="sidebar-bottom">
          <div className="avatar">{username.charAt(0).toUpperCase()}</div>
          <div className="profile-name"><strong>{username}</strong><small>Workspace account</small></div>
        </div>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <div className="breadcrumbs">Workspace <span>/</span> Overview</div>
          <button className="signout-button" onClick={onSignOut} type="button">
            <LogOut size={16} /> Sign out
          </button>
        </header>

        <section className="page-heading">
          <div>
            <p className="eyebrow">YOUR WORKSPACE</p>
            <h1>{dashboardName}</h1>
            <p className="subtitle">Configure dashboard widgets and arrange your workspace.</p>
          </div>
          <button
            className="primary-button"
            disabled={!dataset}
            onClick={() => {
              setFormError("");
              setShowBuilder((visible) => !visible);
            }}
            title={dataset ? "Create a chart from your dataset" : "Upload a dataset first"}
            type="button"
          >
            <Plus size={18} /> Create widget
          </button>
        </section>

        <section className="dashboard-notice" aria-live="polite">
          <ChartNoAxesCombined size={18} />
          <p>
            Upload a CSV or Excel workbook, then choose its columns and metrics. Your dataset and
            widget settings are saved privately to this dashboard.
          </p>
        </section>

        <section className="dataset-panel">
          <div className="form-heading">
            <div>
              <h2>Dataset</h2>
              <p>CSV or Excel (.xlsx), up to 10 MB and 50,000 rows.</p>
            </div>
          </div>
          <form className="dataset-upload" onSubmit={uploadDatasetFile}>
            <label className="dataset-file">
              <FileSpreadsheet size={18} />
              <span>{uploadFile?.name || "Choose a .csv or .xlsx file"}</span>
              <input
                accept=".csv,.xlsx,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                onChange={(event) => setUploadFile(event.target.files?.[0] || null)}
                type="file"
              />
            </label>
            <button className="primary-button" disabled={uploadingDataset || !uploadFile} type="submit">
              <Upload size={16} /> {uploadingDataset ? "Uploading…" : dataset ? "Replace dataset" : "Upload dataset"}
            </button>
            {dataset && (
              <button className="secondary-button" onClick={removeDataset} type="button">
                Remove dataset
              </button>
            )}
          </form>
          {dataset && (
            <p className="dataset-summary" role="status">
              <strong>{dataset.name}</strong> · {dataset.rowCount.toLocaleString()} rows · {dataset.columns.length} columns
            </p>
          )}
          {datasetError && <p className="form-error" role="alert">{datasetError}</p>}
        </section>

        {showBuilder && (
          <form className="widget-form" onSubmit={addWidget}>
            <div className="form-heading">
              <div>
                <h2>Configure a widget</h2>
                <p>These settings are saved to your dashboard.</p>
              </div>
            </div>
            <div className="form-grid">
              <label>
                Widget title
                <input
                  maxLength={150}
                  onChange={(event) => setTitle(event.target.value)}
                  placeholder="e.g. Orders by month"
                  required
                  value={title}
                />
              </label>
              <label>
                Metric
                <select onChange={(event) => setMetricType(event.target.value)} value={metricType}>
                  {metricTypes.map((metric) => (
                    <option key={metric.value} value={metric.value}>{metric.label}</option>
                  ))}
                </select>
              </label>
              <label>
                Chart type
                <select onChange={(event) => setChartType(event.target.value)} value={chartType}>
                  {chartTypes.map(({ value, label }) => (
                    <option key={value} value={value}>{label}</option>
                  ))}
                </select>
              </label>
              <label>
                Value column
                <select
                  onChange={(event) => setValueField(event.target.value)}
                  required
                  value={valueField}
                >
                  <option value="">Choose a column</option>
                  {dataset?.columns.map((column) => <option key={column} value={column}>{column}</option>)}
                </select>
              </label>
              <label>
                Group by field <span className="optional-label">Optional</span>
                <select
                  onChange={(event) => setGroupByField(event.target.value)}
                  value={groupByField}
                >
                  <option value="">No grouping</option>
                  {dataset?.columns.map((column) => <option key={column} value={column}>{column}</option>)}
                </select>
              </label>
            </div>
            {formError && <p className="form-error" role="alert">{formError}</p>}
            <div className="form-actions">
              <button className="primary-button" disabled={savingWidget} type="submit">
                {savingWidget ? "Saving…" : "Add widget"}
              </button>
              <button
                className="secondary-button"
                onClick={() => setShowBuilder(false)}
                type="button"
              >
                Cancel
              </button>
            </div>
          </form>
        )}

        <section className="widgets-panel">
          <div className="section-heading">
            <div>
              <h2>Your widgets</h2>
              <p>{widgets.length} configured {widgets.length === 1 ? "widget" : "widgets"}</p>
            </div>
            <div className="section-actions">
              {widgets.length > 1 && <span className="drag-hint">Drag cards to reorder</span>}
              <button
                className="secondary-button export-button"
                disabled={exporting !== "" || !dataset || widgets.length === 0}
                onClick={() => exportWidgets("csv")}
                type="button"
              >
                <Download size={15} /> {exporting === "csv" ? "Exporting…" : "Export CSV"}
              </button>
              <button
                className="secondary-button export-button"
                disabled={exporting !== "" || !dataset || widgets.length === 0}
                onClick={() => exportWidgets("xlsx")}
                type="button"
              >
                <Download size={15} /> {exporting === "xlsx" ? "Exporting…" : "Export Excel"}
              </button>
            </div>
          </div>

          <p className="export-note">Exports contain the calculated results shown in your charts.</p>
          {widgetError && <p className="form-error" role="alert">{widgetError}</p>}

          {widgets.length === 0 ? (
            <div className="empty-state">
              <div className="state-icon"><LayoutDashboard size={22} /></div>
              <h3>No widgets yet</h3>
              <p>Upload a dataset, then create a chart to explore its metrics.</p>
              <button
                className="secondary-button"
                disabled={!dataset}
                onClick={() => setShowBuilder(true)}
                type="button"
              >
                <Plus size={16} /> Create your first widget
              </button>
            </div>
          ) : (
            <div className="widgets-grid">
              {widgets.map((widget, index) => (
                <WidgetCard
                  index={index}
                  key={widget.id}
                  onDragOver={(event) => event.preventDefault()}
                  onDragStart={(event) => {
                    event.dataTransfer.effectAllowed = "move";
                    event.dataTransfer.setData("text/plain", String(widget.id));
                  }}
                  onDrop={(event) => dropWidget(widget.id, event)}
                  onMove={moveWidget}
                  onRemove={removeWidget}
                  total={widgets.length}
                  widget={widget}
                />
              ))}
            </div>
          )}
        </section>

        <footer className="footer">
          Axlero Dashboard Builder
          <span>CSV and Excel dataset charts</span>
        </footer>
      </main>
    </div>
  );
}

export default function App() {
  const [session, setSession] = useState(() => api.getSession());
  const [dashboardState, setDashboardState] = useState(() => (
    session?.token ? { status: "loading" } : { status: "idle" }
  ));
  const [dataset, setDataset] = useState(null);
  const [widgets, setWidgets] = useState([]);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    if (!session?.token) return undefined;
    let active = true;

    api.getDashboard().then(async (dashboard) => {
      const [savedDataset, savedWidgets] = await Promise.all([
        api.getDataset(),
        api.getWidgets(),
      ]);
      const widgetsWithData = await Promise.all(savedWidgets.map(async (widget) => {
        if (!savedDataset) {
          return { ...widget, data: [], dataError: "Upload a dataset to display this chart." };
        }
        try {
          return { ...widget, data: await api.getWidgetData(widget.id), dataError: "" };
        } catch (error) {
          if (error.status === 401) throw error;
          return { ...widget, data: [], dataError: error.message };
        }
      }));
      if (active) {
        setDataset(savedDataset);
        setWidgets(widgetsWithData);
        setDashboardState({ status: "loaded", dashboard });
      }
    }).catch((error) => {
      if (!active) return;
      if (error.status === 401) {
        api.clearSession();
        setSession(null);
        setDataset(null);
        setWidgets([]);
        return;
      }
      setDashboardState({ status: "error", error: error.message });
    });

    return () => {
      active = false;
    };
  }, [session, reloadKey]);

  function signOut() {
    api.clearSession();
    setSession(null);
    setDataset(null);
    setWidgets([]);
    setDashboardState({ status: "idle" });
  }

  function retry() {
    setDashboardState({ status: "loading" });
    setReloadKey((current) => current + 1);
  }

  if (!session?.token) {
    return (
      <AuthScreen
        onAuthenticated={(nextSession) => {
          setWidgets([]);
          setDataset(null);
          setDashboardState({ status: "loading" });
          setSession(nextSession);
        }}
      />
    );
  }

  if (dashboardState.status === "loading") return <DashboardLoading />;
  if (dashboardState.status === "error") {
    return (
      <DashboardError error={dashboardState.error} onRetry={retry} onSignOut={signOut} />
    );
  }
  if (dashboardState.status !== "loaded") return null;

  return (
    <DashboardWorkspace
      dashboard={dashboardState.dashboard}
      dataset={dataset}
      onSignOut={signOut}
      setDataset={setDataset}
      setWidgets={setWidgets}
      username={session.username}
      widgets={widgets}
    />
  );
}
