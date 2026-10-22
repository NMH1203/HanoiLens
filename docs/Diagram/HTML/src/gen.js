// Generates diagram-design flowchart HTML from a layout spec and checks connector rules.
// usage: node gen.js spec.js out.html
const fs = require('fs');
const spec = require(require('path').resolve(process.argv[2]));
const C = { paper: '#f5f5f5', paper2: '#ececec', ink: '#2d3142', muted: '#4f5d75', soft: '#5a6580', accent: '#bf4520', rule: 'rgba(45,49,66,0.12)' };
const SIZE = { step: [160, 48], dec: [168, 72], term: [120, 40], ref: [168, 48], dot: [8, 8] };
const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

const nodes = {};
for (const n of spec.nodes) {
  const [w, h] = n.k === 'ent' ? [n.w || 208, 28 + 14 * n.f.length + 10] : n.w ? [n.w, n.h || SIZE[n.k][1]] : SIZE[n.k];
  nodes[n.id] = { ...n, w, h, x0: n.x - w / 2, y0: n.y - h / 2, x1: n.x + w / 2, y1: n.y + h / 2 };
}
function anchor(ref) {
  const m = ref.match(/^(\w+)\.([lrtb])([+-]\d+)?$/);
  if (!m) throw new Error('bad anchor ' + ref);
  const n = nodes[m[1]]; if (!n) throw new Error('no node ' + m[1]);
  const o = +(m[3] || 0);
  if (n.k === 'dot') return { n, p: [n.x, n.y], side: m[2] };
  const p = { l: [n.x0, n.y + o], r: [n.x1, n.y + o], t: [n.x + o, n.y0], b: [n.x + o, n.y1] }[m[2]];
  return { n, p, side: m[2] };
}
const warns = [];
const edges = spec.edges.map((e, i) => {
  const a = anchor(e.f), b = anchor(e.t);
  let pts = [a.p, ...(e.via || []), b.p];
  if (!e.via && a.p[0] !== b.p[0] && a.p[1] !== b.p[1]) {
    const horiz = a.side === 'l' || a.side === 'r';
    pts = [a.p, horiz ? [b.p[0], a.p[1]] : [a.p[0], b.p[1]], b.p];
  }
  for (let j = 1; j < pts.length; j++) if (pts[j][0] !== pts[j - 1][0] && pts[j][1] !== pts[j - 1][1]) warns.push(`edge ${e.f}->${e.t}: diagonal segment ${j}`);
  return { ...e, pts, a, b, i };
});

function pathD(pts, r = 8) {
  let d = `M ${pts[0][0]} ${pts[0][1]}`;
  for (let i = 1; i < pts.length - 1; i++) {
    const [px, py] = pts[i - 1], [cx, cy] = pts[i], [nx, ny] = pts[i + 1];
    const l1 = Math.hypot(cx - px, cy - py), l2 = Math.hypot(nx - cx, ny - cy);
    const rr = Math.min(r, l1 / 2, l2 / 2);
    const ax = cx - ((cx - px) / l1) * rr, ay = cy - ((cy - py) / l1) * rr;
    const bx = cx + ((nx - cx) / l2) * rr, by = cy + ((ny - cy) / l2) * rr;
    d += ` L ${ax} ${ay} Q ${cx} ${cy} ${bx} ${by}`;
  }
  const last = pts[pts.length - 1];
  return d + ` L ${last[0]} ${last[1]}`;
}

// labels: on segment e.ls (default: first segment long enough), beside the line
const labels = [];
for (const e of edges) {
  if (!e.label) continue;
  const text = e.label.toUpperCase();
  if (text.length > 14) warns.push(`label too long: ${text}`);
  const segs = e.pts.slice(1).map((p, j) => [e.pts[j], p]);
  const s = segs[e.ls ?? 0];
  const lw = text.length * 6.2 + 10, lh = 12;
  let cx, cy;
  if (s[0][1] === s[1][1]) { // horizontal: above the line
    cx = (s[0][0] + s[1][0]) / 2 + (e.lx || 0); cy = s[0][1] - 8 - lh / 2;
  } else { // vertical: right of the line (or left with e.side='l')
    cx = s[0][0] + (e.side === 'l' ? -(8 + lw / 2) : 8 + lw / 2); cy = (s[0][1] + s[1][1]) / 2 + (e.ly || 0);
  }
  labels.push({ e, text, x0: cx - lw / 2, y0: cy - lh / 2, w: lw, h: lh, cx, cy });
}

// cardinality labels (ER): c1 near the start, c2 near the end, beside the line
for (const e of edges) for (const [text, end] of [[e.c1, 0], [e.c2, 1]]) {
  if (!text) continue;
  const p = end ? e.pts[e.pts.length - 1] : e.pts[0], q = end ? e.pts[e.pts.length - 2] : e.pts[1];
  const lw = text.length * 6 + 6, lh = 12;
  let cx, cy;
  if (p[1] === q[1]) { cx = p[0] + Math.sign(q[0] - p[0]) * (12 + lw / 2); cy = p[1] - 8 - lh / 2; }
  else { cx = p[0] + 8 + lw / 2; cy = p[1] + Math.sign(q[1] - p[1]) * (12 + lh / 2); }
  labels.push({ e, text, card: true, x0: cx - lw / 2, y0: cy - lh / 2, w: lw, h: lh, cx, cy });
}

// checks: labels vs nodes / other edges, segments through non-endpoint nodes, overlapping collinear segments
const inter = (a, b) => a.x0 < b.x0 + b.w && b.x0 < a.x0 + a.w && a.y0 < b.y0 + b.h && b.y0 < a.y0 + a.h;
const box = (n, pad = 0) => ({ x0: n.x0 - pad, y0: n.y0 - pad, w: n.w + 2 * pad, h: n.h + 2 * pad });
const segBox = (p, q, pad = 0) => ({ x0: Math.min(p[0], q[0]) - pad, y0: Math.min(p[1], q[1]) - pad, w: Math.abs(p[0] - q[0]) + 2 * pad, h: Math.abs(p[1] - q[1]) + 2 * pad });
for (const L of labels) {
  for (const n of Object.values(nodes)) if (inter(L, box(n, 2))) warns.push(`label ${L.text} (${L.e.f}) overlaps node ${n.id}`);
  for (const e of edges) e.pts.slice(1).forEach((q, j) => { const p = e.pts[j]; if (inter(L, segBox(p, q, 3)) && e !== L.e) warns.push(`label ${L.text} (${L.e.f}) on edge ${e.f}->${e.t}`); });
  for (const M of labels) if (M !== L && inter(L, M)) warns.push(`labels overlap ${L.text}/${M.text}`);
}
for (const e of edges) e.pts.slice(1).forEach((q, j) => {
  const p = e.pts[j];
  for (const n of Object.values(nodes)) {
    if (n === e.a.n || n === e.b.n) continue;
    if (inter(segBox(p, q), box(n, -1))) warns.push(`edge ${e.f}->${e.t} passes through ${n.id}`);
  }
  for (const f of edges) if (f.i > e.i) f.pts.slice(1).forEach((q2, k) => {
    const p2 = f.pts[k];
    const sameH = p[1] === q[1] && p2[1] === q2[1] && Math.abs(p[1] - p2[1]) < 12;
    const sameV = p[0] === q[0] && p2[0] === q2[0] && Math.abs(p[0] - p2[0]) < 12;
    if (sameH && Math.min(Math.max(p[0], q[0]), Math.max(p2[0], q2[0])) - Math.max(Math.min(p[0], q[0]), Math.min(p2[0], q2[0])) > 0) warns.push(`parallel overlap ${e.f}->${e.t} / ${f.f}->${f.t}`);
    if (sameV && Math.min(Math.max(p[1], q[1]), Math.max(p2[1], q2[1])) - Math.max(Math.min(p[1], q[1]), Math.min(p2[1], q2[1])) > 0) warns.push(`parallel overlap ${e.f}->${e.t} / ${f.f}->${f.t}`);
  });
});
for (const n of Object.values(nodes)) if (n.x0 < 24 || n.x1 > spec.width - 24 || n.y1 > spec.height - 88) warns.push(`node ${n.id} too close to edge`);

// render
const font = { sans: "'Geist', sans-serif", mono: "'Geist Mono', monospace" };
let svg = '';
for (const z of spec.zones || []) {
  const ns = z.nodes.map((id) => nodes[id]);
  const x0 = Math.min(...ns.map((n) => n.x0)) - 20, y0 = Math.min(...ns.map((n) => n.y0)) - 32;
  const x1 = Math.max(...ns.map((n) => n.x1)) + 20, y1 = Math.max(...ns.map((n) => n.y1)) + 20;
  z.box = { x0, y0, w: x1 - x0, h: y1 - y0 };
  svg += `<rect x="${x0}" y="${y0}" width="${x1 - x0}" height="${y1 - y0}" rx="8" fill="${C.paper2}" fill-opacity="0.55" stroke="${C.rule}" stroke-width="1"/>\n`;
  svg += `<text x="${x0 + 12}" y="${y0 + 18}" fill="${C.soft}" font-size="8" font-family="${font.mono}" letter-spacing="0.18em">${esc(z.label.toUpperCase())}</text>\n`;
}
for (const [i, a] of (spec.zones || []).entries()) for (const b of (spec.zones || []).slice(i + 1)) if (inter(a.box, b.box)) warns.push(`zones overlap ${a.label}/${b.label}`);
for (const e of edges) {
  const col = e.accent ? C.accent : C.muted;
  svg += `<path d="${pathD(e.pts)}" fill="none" stroke="${col}" stroke-width="${e.accent ? 1.4 : 1.2}"${e.dash ? ' stroke-dasharray="5,4"' : ''}${spec.er ? '' : ` marker-end="url(#${e.accent ? 'arrow-accent' : 'arrow'})"`}/>\n`;
}
for (const L of labels) {
  svg += `<rect x="${L.x0}" y="${L.y0}" width="${L.w}" height="${L.h}" rx="2" fill="${C.paper}"/>\n`;
  svg += `<text x="${L.cx}" y="${L.cy + 3}" fill="${L.e.accent ? C.accent : C.muted}" font-size="8" font-family="${font.mono}" text-anchor="middle" letter-spacing="0.12em">${esc(L.text)}</text>\n`;
}
for (const n of Object.values(nodes)) {
  const stroke = n.accent ? C.accent : C.ink, fill = n.accent ? 'rgba(191,69,32,0.08)' : '#ffffff';
  if (n.k === 'ent') {
    svg += `<rect x="${n.x0}" y="${n.y0}" width="${n.w}" height="${n.h}" rx="6" fill="#ffffff" stroke="${stroke}" stroke-width="1"/>\n`;
    if (n.accent) svg += `<path d="M ${n.x0 + 0.5} ${n.y0 + 28} V ${n.y0 + 6} Q ${n.x0 + 0.5} ${n.y0 + 0.5} ${n.x0 + 6} ${n.y0 + 0.5} H ${n.x1 - 6} Q ${n.x1 - 0.5} ${n.y0 + 0.5} ${n.x1 - 0.5} ${n.y0 + 6} V ${n.y0 + 28} Z" fill="${C.accent}" fill-opacity="0.08"/>\n`;
    svg += `<rect x="${n.x0 + 8}" y="${n.y0 + 8}" width="34" height="12" rx="2" fill="none" stroke="${C.muted}" stroke-width="0.8"/><text x="${n.x0 + 25}" y="${n.y0 + 17}" fill="${C.muted}" font-size="7" font-family="${font.mono}" text-anchor="middle" letter-spacing="0.08em">TABLE</text>\n`;
    svg += `<text x="${n.x0 + 50}" y="${n.y0 + 19}" fill="${C.ink}" font-size="12" font-weight="600" font-family="${font.sans}">${esc(n.t)}</text>\n`;
    svg += `<line x1="${n.x0}" y1="${n.y0 + 28}" x2="${n.x1}" y2="${n.y0 + 28}" stroke="${C.rule}" stroke-width="1"/>\n`;
    n.f.forEach((f, i) => { svg += `<text x="${n.x0 + 12}" y="${n.y0 + 28 + 14 * i + 16}" fill="${f.startsWith('#') ? C.ink : C.muted}" font-size="9" font-family="${font.mono}">${esc(f)}</text>\n`; });
    continue;
  }
  if (n.k === 'dot') { svg += `<circle cx="${n.x}" cy="${n.y}" r="4" fill="${C.ink}"/>\n`; continue; }
  if (n.k === 'dec') svg += `<polygon points="${n.x},${n.y0} ${n.x1},${n.y} ${n.x},${n.y1} ${n.x0},${n.y}" fill="${fill}" stroke="${stroke}" stroke-width="1"/>\n`;
  else if (n.k === 'term') svg += `<rect x="${n.x0}" y="${n.y0}" width="${n.w}" height="${n.h}" rx="20" fill="rgba(45,49,66,0.03)" stroke="rgba(45,49,66,0.30)" stroke-width="1"/>\n`;
  else if (n.k === 'ref') svg += `<rect x="${n.x0}" y="${n.y0}" width="${n.w}" height="${n.h}" rx="6" fill="${C.paper}" stroke="${C.muted}" stroke-width="1" stroke-dasharray="4,3"/>\n`;
  else svg += `<rect x="${n.x0}" y="${n.y0}" width="${n.w}" height="${n.h}" rx="6" fill="${fill}" stroke="${stroke}" stroke-width="1"/>\n`;
  const lines = n.t.split('|');
  const total = lines.length * 14 + (n.s ? 12 : 0);
  let y = n.y - total / 2 + 11;
  for (const l of lines) { svg += `<text x="${n.x}" y="${y}" fill="${C.ink}" font-size="12" font-weight="600" font-family="${font.sans}" text-anchor="middle">${esc(l)}</text>\n`; y += 14; }
  if (n.s) svg += `<text x="${n.x}" y="${y}" fill="${C.muted}" font-size="9" font-family="${font.mono}" text-anchor="middle">${esc(n.s)}</text>\n`;
}
// legend strip: only the kinds actually used
const ly = spec.height - 52;
const used = new Set(Object.values(nodes).map((n) => n.k));
const items = [
  ['term', (x) => `<rect x="${x}" y="${ly + 28}" width="24" height="12" rx="6" fill="rgba(45,49,66,0.03)" stroke="rgba(45,49,66,0.30)" stroke-width="1"/>`, 'Start / end'],
  ['step', (x) => `<rect x="${x}" y="${ly + 28}" width="24" height="12" rx="2" fill="#ffffff" stroke="${C.ink}" stroke-width="1"/>`, 'Step'],
  ['dec', (x) => `<polygon points="${x},${ly + 34} ${x + 12},${ly + 28} ${x + 24},${ly + 34} ${x + 12},${ly + 40}" fill="#ffffff" stroke="${C.ink}" stroke-width="1"/>`, 'Decision'],
  ['ref', (x) => `<rect x="${x}" y="${ly + 28}" width="24" height="12" rx="2" fill="${C.paper}" stroke="${C.muted}" stroke-width="1" stroke-dasharray="4,3"/>`, 'Jump to another step or diagram'],
  ['dot', (x) => `<circle cx="${x + 12}" cy="${ly + 34}" r="4" fill="${C.ink}"/>`, 'Merge'],
  ['ent', (x) => `<rect x="${x}" y="${ly + 28}" width="24" height="12" rx="2" fill="#ffffff" stroke="${C.ink}" stroke-width="1"/>`, 'Table · # primary key · → foreign key'],
  ['erline', (x) => `<line x1="${x}" y1="${ly + 34}" x2="${x + 24}" y2="${ly + 34}" stroke="${C.muted}" stroke-width="1.2"/>`, 'Relationship · 1 one · N many'],
  ['accent', (x) => spec.er ? `<rect x="${x}" y="${ly + 28}" width="24" height="12" rx="2" fill="rgba(191,69,32,0.08)" stroke="${C.accent}" stroke-width="1"/>` : `<line x1="${x}" y1="${ly + 34}" x2="${x + 24}" y2="${ly + 34}" stroke="${C.accent}" stroke-width="1.4" marker-end="url(#arrow-accent)"/>`, spec.accentLabel || 'Fixed after review'],
  ['dash', (x) => `<line x1="${x}" y1="${ly + 34}" x2="${x + 24}" y2="${ly + 34}" stroke="${C.muted}" stroke-width="1.2" stroke-dasharray="5,4" marker-end="url(#arrow)"/>`, 'Optional'],
];
if (spec.er) used.add('erline');
if (edges.some((e) => e.accent) || Object.values(nodes).some((n) => n.accent)) used.add('accent');
if (edges.some((e) => e.dash)) used.add('dash');
svg += `<line x1="40" y1="${ly}" x2="${spec.width - 40}" y2="${ly}" stroke="rgba(45,49,66,0.10)" stroke-width="0.8"/>
<text x="40" y="${ly + 16}" fill="${C.muted}" font-size="8" font-family="${font.mono}" letter-spacing="0.18em">LEGEND · SHAPE CARRIES TYPE</text>
`;
let lx = 40;
for (const [k, draw, text] of items) if (used.has(k)) {
  svg += draw(lx) + `<text x="${lx + 32}" y="${ly + 38}" fill="${C.muted}" font-size="8.5" font-family="${font.sans}">${text}</text>
`;
  lx += 32 + text.length * 5 + 36;
}

const html = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${esc(spec.title)}</title>
  <link href="https://fonts.googleapis.com/css2?family=Instrument+Serif:ital@0;1&family=Geist:wght@400;500;600&family=Geist+Mono:wght@400;500;600&display=swap" rel="stylesheet">
  <style>
    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
    :root { --color-paper: #f5f5f5; --color-ink: #2d3142; --color-muted: #4f5d75; --color-accent: #bf4520;
      --font-sans: 'Geist', system-ui, sans-serif; --font-serif: 'Instrument Serif', serif; --font-mono: 'Geist Mono', ui-monospace, monospace; }
    body { font-family: var(--font-sans); background: var(--color-paper); color: var(--color-ink); min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 3rem 2rem; }
    .frame { max-width: ${spec.width}px; width: 100%; min-width: 0; }
    .diagram-container { width: 100%; overflow-x: auto; }
    .eyebrow { font-family: var(--font-mono); font-size: 0.66rem; font-weight: 500; letter-spacing: 0.18em; text-transform: uppercase; color: var(--color-muted); margin-bottom: 0.5rem; }
    h1 { font-family: var(--font-serif); font-size: clamp(1.5rem, 2.4vw + 0.75rem, 2rem); font-weight: 400; letter-spacing: -0.02em; line-height: 1.15; margin-bottom: 1.5rem; }
    svg { width: 100%; min-width: ${spec.width}px; display: block; }
    @media print { .diagram-container { overflow-x: visible; } svg { min-width: 0; } }
  </style>
</head>
<body>
  <div class="frame">
    <p class="eyebrow">${esc(spec.eyebrow)}</p>
    <h1>${esc(spec.title)}</h1>
    <div class="diagram-container">
      <svg viewBox="0 0 ${spec.width} ${spec.height}" xmlns="http://www.w3.org/2000/svg" role="img" aria-labelledby="${spec.slug}-title ${spec.slug}-desc">
        <title id="${spec.slug}-title">${esc(spec.title)}</title>
        <desc id="${spec.slug}-desc">${esc(spec.desc)}</desc>
        <defs>
          <pattern id="dots" width="22" height="22" patternUnits="userSpaceOnUse"><circle cx="1" cy="1" r="0.9" fill="rgba(45,49,66,0.10)"/></pattern>
          <marker id="arrow" markerWidth="8" markerHeight="6" refX="7" refY="3" orient="auto"><polygon points="0 0, 8 3, 0 6" fill="#4f5d75"/></marker>
          <marker id="arrow-accent" markerWidth="8" markerHeight="6" refX="7" refY="3" orient="auto"><polygon points="0 0, 8 3, 0 6" fill="#bf4520"/></marker>
        </defs>
        <rect width="100%" height="100%" fill="#f5f5f5"/>
        <rect width="100%" height="100%" fill="url(#dots)" opacity="0.55"/>
${svg}      </svg>
    </div>
  </div>
</body>
</html>
`;
fs.writeFileSync(process.argv[3], html);
const nodeCount = Object.values(nodes).filter((n) => n.k !== 'dot').length;
console.log(`${spec.slug}: ${nodeCount} nodes, ${edges.length} edges, ${warns.length} warnings`);
warns.forEach((w) => console.log('  WARN ' + w));
