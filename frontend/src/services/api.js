
const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

const TOKEN_KEY = "axlero_jwt_token";

function getToken() {
  return sessionStorage.getItem(TOKEN_KEY);
}

function saveToken(token) {
  sessionStorage.setItem(TOKEN_KEY, token);
}

export function logout() {
  sessionStorage.removeItem(TOKEN_KEY);
}

async function request(endpoint, options = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...options.headers,
  };

  const token = getToken();

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  let response;

  try {
    response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers,
    });
  } catch {
    throw new Error(
      "Cannot connect to the backend. Check that Spring Boot is running."
    );
  }

  if (!response.ok) {
    throw new Error(`API request failed: ${response.status}`);
  }

  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get("content-type") || "";

  return contentType.includes("application/json")
    ? response.json()
    : response.text();
}

export const api = {
  async login(email, password) {
    const data = await request("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ username: email, password }),
    });

    if (!data?.token) {
      throw new Error("Login response did not contain a token.");
    }

    saveToken(data.token);
    return data;
  },

  async register(registrationData) {
    const data = await request("/api/auth/register", {
      method: "POST",
      body: JSON.stringify(registrationData),
    });

    if (data?.token) {
      saveToken(data.token);
    }

    return data;
  },

  logout,
  get: (endpoint) => request(endpoint),

  post: (endpoint, body) =>
    request(endpoint, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  put: (endpoint, body) =>
    request(endpoint, {
      method: "PUT",
      body: JSON.stringify(body),
    }),

  delete: (endpoint) =>
    request(endpoint, { method: "DELETE" }),
};
