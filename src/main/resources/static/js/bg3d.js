/* Shared 3D background: wireframe "evidence vault" + orbit rings + floating hash particles. */
(function () {
  const canvas = document.getElementById('bg3d');
  if (!canvas || !window.THREE) return;
  const compact = canvas.dataset.mode === 'compact';
  const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true });
  renderer.setPixelRatio(Math.min(devicePixelRatio, 2));
  const scene = new THREE.Scene();
  const camera = new THREE.PerspectiveCamera(55, 1, 0.1, 200);
  camera.position.set(0, 0, compact ? 16 : 11);

  const group = new THREE.Group();
  scene.add(group);

  // outer icosahedron wireframe
  const ico = new THREE.Mesh(new THREE.IcosahedronGeometry(3.2, 1),
    new THREE.MeshBasicMaterial({ color: 0x27e1c1, wireframe: true, transparent: true, opacity: 0.22 }));
  group.add(ico);
  // inner glowing core cube (the "evidence")
  const core = new THREE.Mesh(new THREE.BoxGeometry(1.5, 1.5, 1.5),
    new THREE.MeshBasicMaterial({ color: 0x4d8dff, wireframe: true, transparent: true, opacity: 0.9 }));
  group.add(core);
  const core2 = new THREE.Mesh(new THREE.BoxGeometry(0.8, 0.8, 0.8),
    new THREE.MeshBasicMaterial({ color: 0x27e1c1, transparent: true, opacity: 0.55 }));
  group.add(core2);
  // rings
  const rings = [];
  [4.3, 5.1].forEach((r, i) => {
    const ring = new THREE.Mesh(new THREE.TorusGeometry(r, 0.012, 8, 160),
      new THREE.MeshBasicMaterial({ color: i ? 0x4d8dff : 0x27e1c1, transparent: true, opacity: 0.55 }));
    ring.rotation.x = Math.PI / 2.4 + i * 0.7; ring.rotation.y = i * 0.8;
    group.add(ring); rings.push(ring);
    const sat = new THREE.Mesh(new THREE.SphereGeometry(0.1, 12, 12), new THREE.MeshBasicMaterial({ color: 0xffffff }));
    ring.add(sat); sat.userData = { r, speed: 0.4 + i * 0.25, a: i * 2 };
  });
  // particles
  const N = compact ? 500 : 900, pos = new Float32Array(N * 3);
  for (let i = 0; i < N; i++) {
    const r = 8 + Math.random() * 22, t = Math.random() * 6.283, p = Math.acos(2 * Math.random() - 1);
    pos[i * 3] = r * Math.sin(p) * Math.cos(t); pos[i * 3 + 1] = r * Math.sin(p) * Math.sin(t); pos[i * 3 + 2] = r * Math.cos(p);
  }
  const pg = new THREE.BufferGeometry(); pg.setAttribute('position', new THREE.BufferAttribute(pos, 3));
  const pts = new THREE.Points(pg, new THREE.PointsMaterial({ color: 0x8fb6ff, size: 0.06, transparent: true, opacity: 0.7 }));
  scene.add(pts);

  if (compact) { group.position.set(7, -1, -4); group.scale.setScalar(0.9); }
  else { group.position.x = -3.4; }

  let mx = 0, my = 0;
  addEventListener('pointermove', e => { mx = (e.clientX / innerWidth - 0.5); my = (e.clientY / innerHeight - 0.5); });
  function resize() {
    const w = innerWidth, h = innerHeight; renderer.setSize(w, h, false);
    camera.aspect = w / h;
    if (w < 900 && !compact) group.position.x = 0; else if (!compact) group.position.x = -3.4;
    camera.updateProjectionMatrix();
  }
  addEventListener('resize', resize); resize();

  const clock = new THREE.Clock();
  (function loop() {
    const t = clock.getElapsedTime();
    ico.rotation.y = t * 0.18; ico.rotation.x = t * 0.08;
    core.rotation.x = t * 0.5; core.rotation.y = t * 0.7;
    core2.rotation.x = -t * 0.9; core2.rotation.z = t * 0.6;
    const s = 1 + Math.sin(t * 2) * 0.06; core2.scale.setScalar(s);
    rings.forEach((r, i) => { r.rotation.z = t * (0.15 + i * 0.1);
      const sat = r.children[0], d = sat.userData; d.a += 0.01 * d.speed * 4;
      sat.position.set(Math.cos(d.a) * d.r, Math.sin(d.a) * d.r, 0); });
    pts.rotation.y = t * 0.02;
    group.rotation.y += (mx * 0.8 - group.rotation.y) * 0.03;
    group.rotation.x += (my * 0.5 - group.rotation.x) * 0.03;
    camera.position.x += (mx * 1.5 - camera.position.x) * 0.02;
    renderer.render(scene, camera);
    requestAnimationFrame(loop);
  })();
})();
