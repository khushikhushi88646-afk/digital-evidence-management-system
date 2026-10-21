/* 3D request-flow visualiser: App -> Redis -> Database. Call Flow3D.pulse('REDIS'|'DATABASE') after each API read. */
const Flow3D = (function () {
  const canvas = document.getElementById('flow3d');
  if (!canvas || !window.THREE) return { pulse() {} };
  const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true });
  renderer.setPixelRatio(Math.min(devicePixelRatio, 2));
  const scene = new THREE.Scene();
  const cam = new THREE.PerspectiveCamera(45, 1, 0.1, 100); cam.position.set(0, 2.2, 11);
  cam.lookAt(0, 0, 0);

  function label(text, color) {
    const c = document.createElement('canvas'); c.width = 256; c.height = 64;
    const g = c.getContext('2d'); g.font = '600 30px system-ui,sans-serif'; g.fillStyle = color; g.textAlign = 'center'; g.fillText(text, 128, 42);
    const s = new THREE.Sprite(new THREE.SpriteMaterial({ map: new THREE.CanvasTexture(c), transparent: true }));
    s.scale.set(2.6, 0.65, 1); return s;
  }
  function node(x, y, color, geo, name) {
    const m = new THREE.Mesh(geo, new THREE.MeshBasicMaterial({ color, wireframe: true, transparent: true, opacity: 0.9 }));
    const inner = new THREE.Mesh(geo.clone().scale(0.55, 0.55, 0.55), new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0.35 }));
    const grp = new THREE.Group(); grp.add(m, inner); grp.position.set(x, y, 0);
    const l = label(name, '#' + color.toString(16).padStart(6, '0')); l.position.set(0, -1.6, 0); grp.add(l);
    scene.add(grp); return { grp, m, inner, base: 1, flash: 0 };
  }
  const A = node(-5, 0, 0x27e1c1, new THREE.OctahedronGeometry(1), 'App / Browser');
  const R = node(0, 1.3, 0xff6b6b, new THREE.IcosahedronGeometry(1.2, 0), 'Redis cache');
  const D = node(5, 0, 0x4d8dff, new THREE.CylinderGeometry(1, 1, 1.6, 20, 3), 'Database');
  const P = { A: A.grp.position, R: R.grp.position, D: D.grp.position };

  [[P.A, P.R, 0xff6b6b], [P.R, P.D, 0x4d8dff], [P.A, P.D, 0x27e1c1]].forEach(([a, b, c], i) => {
    const g = new THREE.BufferGeometry().setFromPoints([a, b]);
    scene.add(new THREE.Line(g, new THREE.LineBasicMaterial({ color: c, transparent: true, opacity: i === 2 ? 0.12 : 0.4 })));
  });
  const floor = new THREE.GridHelper(24, 24, 0x1c2b4a, 0x101a2e); floor.position.y = -2.2; scene.add(floor);

  const packets = [];
  function packet(path, color, delay) {
    const m = new THREE.Mesh(new THREE.SphereGeometry(0.17, 12, 12), new THREE.MeshBasicMaterial({ color }));
    m.visible = false; scene.add(m);
    packets.push({ m, path, t: -delay, seg: 0 });
  }
  function pulse(source) {
    if (source === 'REDIS') {
      packet([P.A, P.R], 0x34d399, 0); packet([P.R, P.A], 0x34d399, 0.55, 'hit');
      setTimeout(() => R.flash = 1, 520);
    } else {
      packet([P.A, P.R], 0xffb547, 0); packet([P.R, P.D], 0xffb547, 0.55); packet([P.D, P.R], 0x4d8dff, 1.25); packet([P.R, P.A], 0x4d8dff, 1.8);
      setTimeout(() => D.flash = 1, 1150); setTimeout(() => R.flash = 0.6, 1800);
    }
    A.flash = 1;
  }
  function resize() {
    const w = canvas.clientWidth, h = canvas.clientHeight; if (!w) return;
    renderer.setSize(w, h, false); cam.aspect = w / h; cam.position.z = w < 520 ? 16 : 11; cam.updateProjectionMatrix();
  }
  addEventListener('resize', resize); resize();
  const clk = new THREE.Clock();
  (function loop() {
    const t = clk.getElapsedTime(); resize();
    [A, R, D].forEach((n, i) => {
      n.m.rotation.y = t * (0.4 + i * 0.1); n.m.rotation.x = t * 0.25; n.inner.rotation.y = -t * 0.6;
      n.flash *= 0.94; const s = 1 + n.flash * 0.35; n.grp.scale.setScalar(s);
      n.grp.position.y = (i === 1 ? 1.3 : 0) + Math.sin(t * 1.2 + i) * 0.12;
    });
    for (let i = packets.length - 1; i >= 0; i--) {
      const p = packets[i]; p.t += 0.022 * 1.0;
      if (p.t < 0) continue;
      const k = Math.min(p.t / 0.5, 1); p.m.visible = true;
      p.m.position.lerpVectors(p.path[0], p.path[1], k); p.m.position.y += Math.sin(k * Math.PI) * 0.5;
      if (k >= 1) { scene.remove(p.m); packets.splice(i, 1); }
    }
    renderer.render(scene, cam); requestAnimationFrame(loop);
  })();
  return { pulse };
})();
