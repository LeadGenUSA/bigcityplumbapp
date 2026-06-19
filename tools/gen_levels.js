// Generates new Pipe Drop levels from a start cell + a list of moves, deriving
// the correct pipe shape for every cell (straight where the path goes through,
// elbow where it turns), scrambling the starting rotations, and verifying each
// level is solvable. Prints the LEVELS JS to paste into index.html.
const Side = { Up: 0, Right: 1, Down: 2, Left: 3 };
const DX = [0, 1, 0, -1];
const DY = [-1, 0, 1, 0];
const OPP = [Side.Down, Side.Left, Side.Up, Side.Right];
const SHAPES = {
  straight: [Side.Left, Side.Right],
  elbow: [Side.Right, Side.Down],
};
const rotated = (s, k) => ((s + k) % 4 + 4) % 4;

// Build the ordered list of path cells from a start column (at y=1) and moves
// like ["D",2] (down), ["L",3] (left), ["R",4] (right).
function buildPath(startX, moves) {
  const cells = [{ x: startX, y: 1 }];
  let { x, y } = cells[0];
  const dir = { D: Side.Down, U: Side.Up, L: Side.Left, R: Side.Right };
  for (const [d, n] of moves) {
    for (let i = 0; i < n; i++) {
      x += DX[dir[d]]; y += DY[dir[d]];
      cells.push({ x, y });
    }
  }
  return cells;
}

// Direction (Side) from cell a to adjacent cell b.
function dirBetween(a, b) {
  for (let s = 0; s < 4; s++) if (a.x + DX[s] === b.x && a.y + DY[s] === b.y) return s;
  return -1;
}

// Rotation steps so an `elbow` exposes exactly {p1, p2}.
function elbowRot(p1, p2) {
  for (let k = 0; k < 4; k++) {
    const set = SHAPES.elbow.map(s => rotated(s, k));
    if (set.includes(p1) && set.includes(p2)) return k;
  }
  throw new Error("no elbow rot for " + p1 + "," + p2);
}

function makeLevel(def) {
  const { name, cols, rows } = def;
  const path = buildPath(def.startX, def.moves);
  const entry = path[0].x, exit = path[path.length - 1].x;

  // --- validate ---
  const seen = new Set();
  path.forEach((c, i) => {
    if (c.x < 0 || c.x >= cols || c.y < 1 || c.y > rows - 2)
      throw new Error(`${name}: cell ${c.x},${c.y} out of bounds`);
    const k = c.x + "," + c.y;
    if (seen.has(k)) throw new Error(`${name}: repeated cell ${k}`);
    seen.add(k);
    if (i > 0 && dirBetween(path[i - 1], c) < 0)
      throw new Error(`${name}: non-adjacent step at ${k}`);
  });
  if (path[path.length - 1].y !== rows - 2)
    throw new Error(`${name}: path must end at y=${rows - 2}, ended y=${path[path.length - 1].y}`);

  // --- derive shapes + scrambled rotations ---
  const tiles = path.map((c, i) => {
    const inDir = i === 0 ? Side.Down : dirBetween(c, path[i - 1]); // side facing prev (entry is above)
    const outDir = i === path.length - 1 ? Side.Down : dirBetween(c, path[i + 1]); // side facing next (exit is below)
    const inPort = i === 0 ? Side.Up : inDir;     // port on the cell
    const outPort = outDir;
    let shape, solved;
    if (inPort === OPP[outPort]) { // straight through
      shape = "straight";
      solved = (inPort === Side.Up || inPort === Side.Down) ? 1 : 0; // vertical=1, horizontal=0
    } else {
      shape = "elbow";
      solved = elbowRot(inPort, outPort);
    }
    const rot = (solved + ((c.x * 5 + c.y * 11 + 1) % 4)) % 4; // deterministic scramble
    return { x: c.x, y: c.y, shape, rot };
  });

  return { name, cols, rows, entry, exit, tiles };
}

// Same solvability check as tools/audit_levels.js.
function shapeAllows(shape, a, b) {
  if (a === b) return false;
  const diff = ((a - b) % 4 + 4) % 4;
  if (shape === "straight") return diff === 2;
  if (shape === "elbow") return diff === 1 || diff === 3;
  return true;
}
function solvable(L) {
  const cells = new Map();
  for (const t of L.tiles) cells.set(t.x + "," + t.y, t.shape);
  const exit = { x: L.exit, y: L.rows - 1 };
  const stack = [{ x: L.entry, y: 1, dir: Side.Down }];
  const seen = new Set();
  while (stack.length) {
    const s = stack.pop();
    if (s.x === exit.x && s.y === exit.y && s.dir === Side.Down) return true;
    const k = s.x + "," + s.y;
    if (!cells.has(k)) continue;
    const id = k + "|" + s.dir;
    if (seen.has(id)) continue;
    seen.add(id);
    const shape = cells.get(k), inPort = OPP[s.dir];
    for (let out = 0; out < 4; out++) {
      if (out === inPort || !shapeAllows(shape, inPort, out)) continue;
      stack.push({ x: s.x + DX[out], y: s.y + DY[out], dir: out });
    }
  }
  return false;
}

const DEFS = [
  { name: "Crossover",     cols: 6, rows: 7,  startX: 1, moves: [["D",1],["R",2],["D",1],["L",2],["D",1],["R",3],["D",1]] },
  { name: "Spillway",      cols: 6, rows: 8,  startX: 4, moves: [["D",1],["L",3],["D",2],["R",3],["D",1],["L",2],["D",1]] },
  { name: "The Gauntlet",  cols: 6, rows: 8,  startX: 1, moves: [["D",1],["R",4],["D",2],["L",4],["D",1],["R",2],["D",1]] },
  { name: "Backflow",      cols: 7, rows: 8,  startX: 5, moves: [["D",1],["L",4],["D",2],["R",5],["D",1],["L",4],["D",1]] },
  { name: "Deep Dive",     cols: 7, rows: 9,  startX: 2, moves: [["D",1],["L",2],["D",2],["R",6],["D",2],["L",4],["D",1]] },
  { name: "The Long Haul", cols: 7, rows: 10, startX: 3, moves: [["D",1],["R",3],["D",2],["L",6],["D",2],["R",4],["D",2]] },
];

const levels = DEFS.map(makeLevel);
let allOk = true;
for (const L of levels) {
  const ok = solvable(L);
  if (!ok) allOk = false;
  console.error(`${ok ? "OK     " : "UNSOLVE"} ${L.name} (${L.cols}x${L.rows}, ${L.tiles.length} pipes, entry ${L.entry} -> exit ${L.exit})`);
}
console.error(allOk ? "\nAll new levels solvable.\n" : "\nSOME UNSOLVABLE\n");

// Emit JS to stdout
const lines = levels.map(L => {
  const tiles = L.tiles.map(t => `    {x:${t.x},y:${t.y},shape:"${t.shape}",rot:${t.rot}},`).join("\n");
  return `  { name: "${L.name}", cols: ${L.cols}, rows: ${L.rows}, entry: ${L.entry}, exit: ${L.exit}, tiles: [\n${tiles}\n  ]},`;
});
console.log(lines.join("\n"));
process.exit(allOk ? 0 : 1);
