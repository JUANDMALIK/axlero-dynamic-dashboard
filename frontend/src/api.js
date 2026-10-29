async function readResponse(response) {
  if (response.ok) {
    return response.status === 204 ? null : response.json();
  }

  let message = `Request failed (${response.status})`;
  try {
    const body = await response.json();
    message = body.detail || body.message || body.error || message;
  } catch {
    // Keep the HTTP status message when the server does not return JSON.
  }
  throw new Error(message);
}

async function csrfToken() {
  const response = await fetch('/api/auth/csrf', { credentials: 'include' });
  const token = await readResponse(response);
  return token;
}

export async function api(path, options = {}) {
  const method = options.method || 'GET';
  const headers = new Headers(options.headers || {});
  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json');
  }

  if (method !== 'GET' && method !== 'HEAD') {
    const csrf = await csrfToken();
    headers.set(csrf.headerName, csrf.token);
  }

  const response = await fetch(path, {
    ...options,
    method,
    headers,
    credentials: 'include',
  });
  return readResponse(response);
}

export const dashboardApi = {
  currentUser: () => api('/api/auth/me'),
  login: (credentials) => api('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials),
  }),
  register: (details) => api('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(details),
  }),
  logout: () => api('/api/auth/logout', { method: 'POST' }),
  list: () => api('/api/dashboards'),
  create: (dashboard) => api('/api/dashboards', {
    method: 'POST',
    body: JSON.stringify(dashboard),
  }),
  update: (id, dashboard) => api(`/api/dashboards/${id}`, {
    method: 'PUT',
    body: JSON.stringify(dashboard),
  }),
  remove: (id) => api(`/api/dashboards/${id}`, { method: 'DELETE' }),
};
