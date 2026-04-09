/**
 * When the SPA runs on the dev server (not port 8080), call the Spring API on :8080
 * directly. Session cookies from GitHub OAuth are set on http://localhost:8080; browsers
 * often do not send them on XHRs to http://localhost:4200 even with a proxy.
 */
export function apiOrigin(): string {
  if (typeof window === 'undefined') {
    return '';
  }
  const { hostname, port } = window.location;
  const isLocalLoopback =
    hostname === 'localhost' ||
    hostname === '127.0.0.1' ||
    hostname === '[::1]' ||
    hostname === '::1';
  if (isLocalLoopback && port && port !== '8080') {
    if (hostname === '127.0.0.1') {
      return 'http://127.0.0.1:8080';
    }
    if (hostname === '[::1]' || hostname === '::1') {
      return 'http://[::1]:8080';
    }
    return 'http://localhost:8080';
  }
  return '';
}

export function apiUrl(path: string): string {
  const origin = apiOrigin();
  const normalized = path.startsWith('/') ? path : `/${path}`;
  return origin ? `${origin}${normalized}` : normalized;
}
