const API = {
  token: () => sessionStorage.getItem('token'),
  user: () => { try { return JSON.parse(sessionStorage.getItem('user') || 'null'); } catch (e) { return null; } },
  async req(path, opts = {}) {
    const headers = Object.assign({}, opts.headers || {});
    if (API.token()) headers['Authorization'] = 'Bearer ' + API.token();
    if (opts.json) { headers['Content-Type'] = 'application/json'; opts.body = JSON.stringify(opts.json); }
    const res = await fetch(path, Object.assign({}, opts, { headers }));
    if (res.status === 401 && !path.includes('/auth/login')) { sessionStorage.clear(); location.href = 'login.html'; throw new Error('Session expired'); }
    if (res.status === 403) throw new Error('Not allowed for your role');
    const ct = res.headers.get('content-type') || '';
    const data = ct.includes('json') ? await res.json() : await res.blob();
    if (!res.ok) throw new Error((data && data.error) || ('Request failed (' + res.status + ')'));
    return data;
  },
  get: p => API.req(p),
  post: (p, json) => API.req(p, { method: 'POST', json }),
  put: (p, json) => API.req(p, { method: 'PUT', json }),
  del: p => API.req(p, { method: 'DELETE' }),
};
const esc = s => String(s == null ? '' : s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
