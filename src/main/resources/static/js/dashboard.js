const me = API.user();
if (!API.token() || !me) location.href = 'login.html';
const $ = id => document.getElementById(id);
const isAdmin = me && me.role === 'ADMIN';
$('meName').textContent = me.fullName || me.username; $('meRole').textContent = me.role; $('meRole').className = 'role ' + me.role;

let cases = [];
const fileNames = {};
function toast(msg, bad) { const t = $('toast'); t.textContent = msg; t.className = 'toast on' + (bad ? ' bad' : ''); clearTimeout(toast.t); toast.t = setTimeout(() => t.className = 'toast', 3200); }
const fmtSize = b => b > 1048576 ? (b / 1048576).toFixed(1) + ' MB' : b > 1024 ? (b / 1024).toFixed(1) + ' KB' : b + ' B';
const srcTag = r => `<span class="src ${r.source}">${r.source} · ${r.timeMs} ms</span>`;
const fmtT = s => String(s || '').replace('T', ' ').replace(/\.\d+Z?$/, '').replace('Z', '');
const guard = fn => async (...a) => { try { return await fn(...a); } catch (e) { toast(e.message, true); } };

// ---------- navigation
function show(v) {
  document.querySelectorAll('.view').forEach(x => x.classList.toggle('on', x.id === 'v-' + v));
  document.querySelectorAll('#nav a').forEach(a => a.classList.toggle('on', a.dataset.v === v));
  ({ overview: loadOverview, cases: loadCases, evidence: loadEvidence, audit: loadAudit, bench: loadBench })[v]();
  history.replaceState(null, '', '#' + v);
}
document.querySelectorAll('#nav a').forEach(a => a.onclick = () => show(a.dataset.v));
$('logout').onclick = async () => { try { await API.post('/api/auth/logout'); } catch (e) {} sessionStorage.clear(); location.href = 'login.html'; };

// ---------- overview
const loadOverview = guard(async () => {
  const s = await API.get('/api/stats/overview');
  const card = (k, v, sub, glow) => `<div class="glass stat" style="--glow:${glow || 'rgba(39,225,193,.22)'}"><div class="k">${k}</div><div class="v">${v}</div><div class="s">${sub || '&nbsp;'}</div></div>`;
  $('cards').innerHTML =
    card('Cases', s.cases) + card('Evidence items', s.evidence, s.verified + ' verified') +
    card('Tampered', s.tampered, s.unverified + ' unverified', 'rgba(255,93,115,.25)') +
    card('Redis keys', s.redisKeys, 'v' + (s.redisVersion || '?') + ' · ' + (s.redisMemory || ''), 'rgba(255,107,107,.25)') +
    card('Active sessions', s.activeSessions, 'stored in Redis', 'rgba(77,141,255,.25)');
  $('ratio').textContent = s.hitRatio + '%'; $('hitN').textContent = s.cacheHits; $('missN').textContent = s.cacheMisses;
  const tot = Math.max(1, s.cacheHits + s.cacheMisses);
  requestAnimationFrame(() => { $('hitBar').style.width = (s.cacheHits / tot * 100) + '%'; $('missBar').style.width = (s.cacheMisses / tot * 100) + '%'; });
  const types = Object.entries(s.byType), mx = Math.max(1, ...types.map(t => t[1]));
  $('types').innerHTML = types.length ? types.map(([k, v]) => `<div class="bar"><div class="lbl"><span>${esc(k)}</span><b>${v}</b></div><div class="track"><div class="fill" style="width:${v / mx * 100}%;background:linear-gradient(90deg,var(--accent),var(--accent2))"></div></div></div>`).join('') : '<div class="empty">No evidence yet</div>';
});
$('demoRead').onclick = guard(async () => {
  const l = await API.get('/api/cases'); Flow3D.pulse(l.source);
  $('demoOut').textContent = 'GET /api/cases → ' + l.source + ' in ' + l.timeMs + ' ms'; setTimeout(loadOverview, 2200);
});

// ---------- cases
async function fetchCases() { const r = await API.get('/api/cases'); cases = r.data; return r; }
const loadCases = guard(async () => {
  const r = await fetchCases(); Flow3D.pulse(r.source);
  $('caseRows').innerHTML = cases.map(c => `<tr><td class="mono">${esc(c.caseNumber)}</td><td><b>${esc(c.title)}</b><div class="muted">${esc(c.description || '')}</div></td>
    <td><span class="badge b-${esc(c.status)}">${esc(c.status)}</span></td><td>${esc(c.createdBy)}</td><td>${srcTag(r)}</td>
    <td style="text-align:right"><button class="btn ghost sm" onclick="openCase(${c.id})">Evidence →</button>
    <button class="btn ghost sm" onclick="toggleCase(${c.id},'${c.status}')">${c.status === 'OPEN' ? 'Close' : 'Reopen'}</button></td></tr>`).join('') || '<tr><td colspan="6" class="empty">No cases</td></tr>';
});
$('caseForm').onsubmit = guard(async e => {
  e.preventDefault(); await API.post('/api/cases', { title: $('cTitle').value, description: $('cDesc').value });
  e.target.reset(); toast('Case created · cache invalidated'); loadCases();
});
window.toggleCase = guard(async (id, st) => { await API.put('/api/cases/' + id + '/status', { status: st === 'OPEN' ? 'CLOSED' : 'OPEN' }); loadCases(); });
window.openCase = id => { show('evidence'); setTimeout(() => { $('caseSel').value = id; loadEvidence(); }, 80); };

// ---------- evidence
async function fillCaseSelects() {
  if (!cases.length) await fetchCases();
  const opts = cases.map(c => `<option value="${c.id}">${esc(c.caseNumber)} — ${esc(c.title)}</option>`).join('');
  const keep = $('caseSel').value; $('caseSel').innerHTML = opts; if (keep) $('caseSel').value = keep;
  $('bCase').innerHTML = opts;
}
const loadEvidence = guard(async () => {
  await fillCaseSelects();
  const q = $('q').value.trim();
  const r = await API.get('/api/evidence?' + (q ? 'q=' + encodeURIComponent(q) : 'caseId=' + $('caseSel').value));
  Flow3D.pulse(r.source); $('listSrc').innerHTML = srcTag(r); r.data.forEach(e => fileNames[e.id] = e.originalFileName);
  $('evRows').innerHTML = r.data.map(e => `<tr><td>${e.id}</td><td><b>${esc(e.title)}</b><div class="muted">${esc(e.originalFileName)}</div></td><td>${esc(e.evidenceType)}</td><td>${fmtSize(e.sizeBytes)}</td>
    <td class="mono" title="${esc(e.sha256)}">${esc((e.sha256 || '').slice(0, 14))}…</td><td><span class="badge b-${esc(e.integrityStatus)}">${esc(e.integrityStatus)}</span></td>
    <td style="text-align:right;white-space:nowrap"><button class="btn ghost sm" onclick="viewEv(${e.id})">View</button>
    <button class="btn ghost sm" onclick="verifyEv(${e.id})">Verify</button>
    <button class="btn ghost sm" onclick="dlEv(${e.id})">Download</button>${isAdmin ? ` <button class="btn danger sm" onclick="delEv(${e.id})">Delete</button>` : ''}</td></tr>`).join('')
    || '<tr><td colspan="7" class="empty">No evidence found</td></tr>';
});
$('caseSel').onchange = () => { $('q').value = ''; loadEvidence(); };
$('q').oninput = (() => { let t; return () => { clearTimeout(t); t = setTimeout(loadEvidence, 350); }; })();
$('upForm').onsubmit = guard(async e => {
  e.preventDefault(); $('upBtn').disabled = true;
  try {
    const fd = new FormData(); fd.append('file', $('upFile').files[0]); fd.append('caseId', $('caseSel').value);
    fd.append('title', $('upTitle').value); fd.append('type', $('upType').value); fd.append('description', $('upDesc').value);
    const r = await API.req('/api/evidence', { method: 'POST', body: fd });
    toast('Uploaded · SHA-256 ' + r.sha256.slice(0, 12) + '…'); e.target.reset(); $('q').value = ''; loadEvidence();
  } finally { $('upBtn').disabled = false; }
});
window.viewEv = guard(async id => {
  const r = await API.get('/api/evidence/' + id); Flow3D.pulse(r.source); const e = r.data;
  const au = await API.get('/api/audit?evidenceId=' + id);
  $('modalBox').innerHTML = `<div class="row" style="justify-content:space-between"><h2>${esc(e.title)}</h2>${srcTag(r)}</div>
    <div class="muted">Served from <b>${r.source}</b> in ${r.timeMs} ms — open again to see the cache effect.</div>
    <div class="kv"><div>Case</div><div>${e.caseId}</div><div>Type</div><div>${esc(e.evidenceType)}</div><div>File</div><div>${esc(e.originalFileName)} (${fmtSize(e.sizeBytes)})</div>
    <div>Description</div><div>${esc(e.description || '—')}</div><div>Uploaded</div><div>${esc(e.uploadedBy)} · ${fmtT(e.uploadedAt)}</div>
    <div>SHA-256</div><div class="hash">${esc(e.sha256)}</div><div>Integrity</div><div><span class="badge b-${esc(e.integrityStatus)}">${esc(e.integrityStatus)}</span> ${e.lastVerifiedAt ? '<span class="muted">checked ' + fmtT(e.lastVerifiedAt) + '</span>' : ''}</div></div>
    <h3 style="margin:16px 0 6px">Custody history</h3><div class="tablewrap"><table><tbody>${au.map(a => `<tr><td class="muted">${fmtT(a.timestamp)}</td><td>${esc(a.username)}</td><td><b>${esc(a.action)}</b></td><td class="muted">${esc(a.details)}</td></tr>`).join('')}</tbody></table></div>
    <div style="text-align:right;margin-top:16px"><button class="btn ghost" onclick="closeModal()">Close</button></div>`;
  $('modal').classList.add('on');
});
window.closeModal = () => $('modal').classList.remove('on'); $('modal').onclick = e => { if (e.target === $('modal')) closeModal(); };
window.verifyEv = guard(async id => {
  const e = await API.post('/api/evidence/' + id + '/verify');
  toast(e.integrityStatus === 'VERIFIED' ? '✔ Integrity verified — hash matches' : '✖ TAMPERED — hash mismatch!', e.integrityStatus !== 'VERIFIED'); loadEvidence();
});
window.dlEv = guard(async id => {
  const blob = await API.get('/api/evidence/' + id + '/download'); const u = URL.createObjectURL(blob); const a = document.createElement('a');
  a.href = u; a.download = fileNames[id] || 'evidence'; a.click(); setTimeout(() => URL.revokeObjectURL(u), 3000);
});
window.delEv = guard(async id => { if (!confirm('Delete evidence #' + id + '? This is logged in the chain of custody.')) return; await API.del('/api/evidence/' + id); toast('Deleted'); loadEvidence(); });

// ---------- audit
const loadAudit = guard(async () => {
  const a = await API.get('/api/audit');
  $('auRows').innerHTML = a.map(x => `<tr><td>${x.id}</td><td class="mono">${fmtT(x.timestamp)}</td><td>${esc(x.username)}</td><td><b>${esc(x.action)}</b></td><td>${x.evidenceId || '—'}</td><td class="muted">${esc(x.details)}</td><td class="mono" title="prev ${esc(x.prevHash)}">${esc(x.hash.slice(0, 10))}…</td></tr>`).join('');
});
$('chainBtn').onclick = guard(async () => {
  const r = await API.get('/api/audit/verify-chain');
  $('chainRes').innerHTML = r.valid ? `<span class="badge b-VERIFIED">CHAIN VALID · ${r.entries} entries</span>` : `<span class="badge b-TAMPERED">BROKEN at entry #${r.brokenAtId}</span>`;
});

// ---------- benchmark
const loadBench = guard(async () => { await fillCaseSelects(); });
$('bRun').onclick = guard(async () => {
  $('bRun').disabled = true; $('bOut').innerHTML = '<div class="empty">Running…</div>';
  try {
    const r = await API.get('/api/stats/benchmark?caseId=' + $('bCase').value + '&n=' + $('bN').value);
    const mx = Math.max(r.dbTotalMs, r.redisTotalMs);
    $('bOut').innerHTML = `<div class="row" style="justify-content:space-between"><div><div class="muted">Redis vs database for ${r.requests} identical reads</div><div class="big">${r.speedup}× ${r.speedup >= 1 ? 'faster' : 'slower'}</div></div>
      <div class="muted" style="max-width:340px">Honest note: with a local in-memory database the gap is small; with MySQL on disk or a remote server, Redis pulls ahead much more.</div></div>
      <div class="bars"><div class="bar"><div class="lbl"><span>Database (JPA query)</span><b>${r.dbTotalMs} ms total · ${r.dbAvgMs} ms avg</b></div><div class="track"><div class="fill" id="f1" style="background:#4d8dff"></div></div></div>
      <div class="bar"><div class="lbl"><span>Redis cache</span><b>${r.redisTotalMs} ms total · ${r.redisAvgMs} ms avg</b></div><div class="track"><div class="fill" id="f2" style="background:#ff6b6b"></div></div></div></div>`;
    requestAnimationFrame(() => { $('f1').style.width = (r.dbTotalMs / mx * 100) + '%'; $('f2').style.width = (r.redisTotalMs / mx * 100) + '%'; });
  } finally { $('bRun').disabled = false; }
});

const start = location.hash.slice(1);
show(['overview', 'cases', 'evidence', 'audit', 'bench'].includes(start) ? start : 'overview');
