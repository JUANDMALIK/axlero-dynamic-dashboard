
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "").replace(/\/$/, "");
const TOKEN_KEY = "axlero.auth.token";
const USERNAME_KEY = "axlero.auth.username";

export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

async function request(endpoint, options = {}) {
  const { skipAuth = false, ...fetchOptions } = options;
  const headers = new Headers(fetchOptions.headers);
  headers.set("Accept", "application/json");

  if (fetchOptions.body !== undefined && !(fetchOptions.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }

  const token = localStorage.getItem(TOKEN_KEY);
  if (!skipAuth && token) headers.set("Authorization", `Bearer ${token}`);

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${endpoint}`, { ...fetchOptions, headers });
  } catch (error) {
    if (error instanceof TypeError) {
      throw new ApiError("Unable to reach the API. Check that the backend is running.", 0);
    }
    throw error;
  }

  if (response.status === 204) return null;

  const responseText = await response.text();
  let payload = null;
  if (responseText) {
    try {
      payload = JSON.parse(responseText);
    } catch {
      payload = responseText;
    }
  }

  if (!response.ok) {
    const message = typeof payload === "string"
      ? payload
      : payload?.detail || payload?.message || `API request failed (${response.status}).`;
    throw new ApiError(message, response.status);
  }

  return payload;
}

async function download(endpoint) {
  const headers = new Headers({ Accept: "*/*" });
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) headers.set("Authorization", `Bearer ${token}`);

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${endpoint}`, { headers });
  } catch (error) {
    if (error instanceof TypeError) {
      throw new ApiError("Unable to reach the API. Check that the backend is running.", 0);
    }
    throw error;
  }

  if (!response.ok) {
    const responseText = await response.text();
    let payload = null;
    if (responseText) {
      try {
        payload = JSON.parse(responseText);
      } catch {
        payload = responseText;
      }
    }
    const message = typeof payload === "string"
      ? payload
      : payload?.detail || payload?.message || `API request failed (${response.status}).`;
    throw new ApiError(message, response.status);
  }

  return response.blob();
}

function authRequest(endpoint, body) {
  return request(endpoint, {
    method: "POST",
    body: JSON.stringify(body),
    skipAuth: true,
  });
}

export const api = {
  getSession() {
    const token = localStorage.getItem(TOKEN_KEY);
    if (!token) return null;
    return {
      token,
      username: localStorage.getItem(USERNAME_KEY) || "Workspace user",
    };
  },

  saveSession(authResponse) {
    if (!authResponse?.token || !authResponse?.username) {
      throw new ApiError("The authentication response did not include a token and username.");
    }
    localStorage.setItem(TOKEN_KEY, authResponse.token);
    localStorage.setItem(USERNAME_KEY, authResponse.username);
  },

  clearSession() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USERNAME_KEY);
  },

  login(credentials) {
    return authRequest("/api/auth/login", credentials);
  },

  register(details) {
    return authRequest("/api/auth/register", details);
  },

  getDashboard() {
    return request("/api/dashboard");
  },

  getWidgets() {
    return request("/api/dashboard/widgets");
  },

  getDataset() {
    return request("/api/dashboard/dataset").catch((error) => {
      if (error.status === 404) return null;
      throw error;
    });
  },

  uploadDataset(file) {
    const body = new FormData();
    body.append("file", file);
    return request("/api/dashboard/dataset", {
      method: "POST",
      body,
    });
  },

  deleteDataset() {
    return request("/api/dashboard/dataset", {
      method: "DELETE",
    });
  },

  getWidgetData(widgetId) {
    return request(`/api/dashboard/widgets/${encodeURIComponent(widgetId)}/data`);
  },

  createWidget(widget) {
    return request("/api/dashboard/widgets", {
      method: "POST",
      body: JSON.stringify(widget),
    });
  },

  deleteWidget(widgetId) {
    return request(`/api/dashboard/widgets/${encodeURIComponent(widgetId)}`, {
      method: "DELETE",
    });
  },

  reorderWidgets(widgetIds) {
    return request("/api/dashboard/widgets/order", {
      method: "PUT",
      body: JSON.stringify({ widgetIds }),
    });
  },

  exportWidgets(format) {
    if (format === "csv") {
      return download("/api/dashboard/export.csv");
    }
    if (format === "xlsx") {
      return download("/api/dashboard/export.xlsx");
    }
    throw new ApiError("Unsupported export format.");
  },
};
