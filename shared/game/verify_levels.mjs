// QA tool: confirms every Pipe Drop level is solvable and reports the minimum
// number of rotations (a natural "par"). Reads LEVELS straight from index.html.
// Run: node verify_levels.mjs
import { readFileSync } from "node:fs";

const html = readFileSync(new URL("./index.html", import.meta.url), "utf8");
const m = html.match(/const LEVELS = (\[[\s\S]*?\n\];)/);
if (!m) { console.error("Could not find LEVELS array in index.html"); process.exit(1); }
// eslint-disable-next-line no-eval
const LEVELS = eval(m[1].replace(/;\s*$/, ""));

const Side = { Up: 0, Right: 1, Down: 2, Left: 3 };
const DX = [0, 1, 0, -1];
const DY = [-1, 0, 1, 0];
const OPP = [2, 3, 0, 1];
const SHAPES = {
  straight: [Side.Left, Side.Right],
  elbow:    [Side.Right, Side.Down],
  tee:      [Side.Left, Side.Right, Side.Down],
  cross:    [Side.Up, Side.Right, Side.Down, Side.Left],
  entry:    [Side.Down],
  exit:     [Side.Up],
};
const rot = (s, k) => ((s + k) % 4 + 4) % 4;
const connSet = (shape, r) => new Set(SHAPES[shape].map(s => rot(s, r)));
const tapCost = (a, b) => Math.min(((b - a) % 4 + 4) % 4, ((a - b) % 4 + 4) % 4);

// Min rotation taps for a tile (authored rot a) to include all sides in `need`.
// Locked tiles (entry/exit) can't rotate — return 0 only if already satisfied.
function orientCost(shape, a, need, locked) {
  let best = Infinity;
  for (let r = 0; r < 4; r++) {
    const set = connSet(shape, r);
    if ([...need].every(s => set.has(s))) {
      if (locked) { if (r === a) best = Math.min(best, 0); }
      else best = Math.min(best, tapCost(a, r));
    }
  }
  return best;
}

const key = (x, y) => x + "," + y;

function solve(L) {
  const tiles = new Map();
  tiles.set(key(L.entry, 0),      { shape: "entry", rot: 0, locked: true });
  tiles.set(key(L.exit, L.rows-1),{ shape: "exit",  rot: 0, locked: true });
  for (const t of L.tiles) tiles.set(key(t.x, t.y), { shape: t.shape, rot: t.rot, locked: false });

  const exitKey = key(L.exit, L.rows - 1);
  const entryKey = key(L.entry, 0);

  // Dijkstra over states "cellKey|inSide" (inSide = side of cell facing previous cell; -1 = start).
  const dist = new Map();
  const start = entryKey + "|-1";
  dist.set(start, 0);
  // simple priority list (levels are tiny)
  const pq = [[0, entryKey, -1]];
  while (pq.length) {
    pq.sort((a, b) => a[0] - b[0]);
    const [d, ck, inSide] = pq.shift();
    if (d > (dist.get(ck + "|" + inSide) ?? Infinity)) continue;
    if (ck === exitKey) return d; // reached exit
    const [cx, cy] = ck.split(",").map(Number);
    const tile = tiles.get(ck);
    for (let out = 0; out < 4; out++) {
      if (out === inSide) continue;
      const need = new Set(inSide === -1 ? [out] : [inSide, out]);
      const c = orientCost(tile.shape, tile.rot, need, tile.locked);
      if (!isFinite(c)) continue;
      const nx = cx + DX[out], ny = cy + DY[out];
      const nk = key(nx, ny);
      const nbr = tiles.get(nk);
      if (!nbr) continue;
      const nIn = OPP[out];
      // entering exit must be from above (exit only connects its Up side)
      if (nk === exitKey && nIn !== Side.Up) continue;
      // entry tile can't be re-entered
      if (nk === entryKey) continue;
      const nd = d + c;
      const sKey = nk + "|" + nIn;
      if (nd < (dist.get(sKey) ?? Infinity)) {
        dist.set(sKey, nd);
        pq.push([nd, nk, nIn]);
      }
    }
  }
  return Infinity; // unsolvable
}

let allOk = true;
console.log("Lvl  Name                  Grid   Tiles  Solvable  Par(min rot)");
console.log("---  --------------------  -----  -----  --------  ------------");
LEVELS.forEach((L, i) => {
  const par = solve(L);
  const ok = isFinite(par);
  if (!ok) allOk = false;
  console.log(
    String(i + 1).padEnd(4),
    L.name.padEnd(21),
    (L.cols + "x" + L.rows).padEnd(6),
    String(L.tiles.length).padEnd(6),
    (ok ? "YES" : "NO ").padEnd(9),
    ok ? String(par) : "—"
  );
});
console.log("\n" + (allOk ? "All levels solvable." : "!!! Some levels are UNSOLVABLE."));
