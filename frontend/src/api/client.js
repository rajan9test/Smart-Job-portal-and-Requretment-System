// Thin wrapper around fetch for the Java REST API.
// - adds the Bearer token
// - turns error responses into ApiError (status, message, todo)
// - signals "session expired" so AuthContext can log the user out

const TOKEN_KEY = 'sjp.token';

export function getToken() {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setToken(token) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  } catch {
    // storage unavailable (private mode): the session just won't survive a reload
  }
}

export class ApiError extends Error {
  constructor(status, message, todo) {
    super(message);
    this.status = status;
    this.todo = todo; // e.g. "TODO(#8)" when the backend feature is an unfinished exercise
  }
}

const BACKEND_DOWN = 'Cannot reach the backend. Is the Java server running on http://localhost:8080?';

async function request(method, path, body) {
  const headers = {};
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  let response;
  try {
    response = await fetch(`/api${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, BACKEND_DOWN);
  }

  if (response.status === 204) return null;

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = null;
    }
  }

  if (!response.ok) {
    if (response.status === 401 && token) {
      window.dispatchEvent(new Event('auth:expired'));
    }
    // The Vite proxy answers 5xx with a non-JSON body when the backend is down.
    const message = data?.message ?? (response.status >= 500 ? BACKEND_DOWN : `Request failed (${response.status})`);
    throw new ApiError(response.status, message, data?.todo);
  }
  return data;
}

export const api = {
  get: (path) => request('GET', path),
  post: (path, body) => request('POST', path, body),
  put: (path, body) => request('PUT', path, body),
  del: (path) => request('DELETE', path),
};

/** Builds "?a=1&b=2", skipping empty values. */
export function queryString(params) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      search.set(key, String(value).trim());
    }
  });
  const qs = search.toString();
  return qs ? `?${qs}` : '';
}
