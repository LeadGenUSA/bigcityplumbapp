// Audits every Pipe Drop level for solvability, replicating the game's exact
// connection model. With pipes free to rotate, a level is solvable iff there is
// a simple path from the entry (top) to the exit (bottom) where each tile's
// shape can realise the required in/out port pair:
//   straight -> the two ports must be opposite (go straight through)
//   elbow    -> the two ports must be perpendicular (a 90 turn)
//   tee/cross -> any pair of distinct sides
// entry exposes only Down; exit only Up.
const fs = require("fs");
const path = require("path");

const htmlPath = path.join(__dirname, "..", "shared", "game", "index.html");
const src = fs.readFileSync(htmlPath, "utf8");

// Extract the `const LEVELS = [ ... ];` array literal and eval it.
const m = src.match(/const LEVELS = (\[[\s\S]*?\n\]);/);
if (!m) { console.error("Could not find LEVELS array"); process.exit(2); }
const LEVELS = eval(m[1]);

const Side = { Up: 0, Right: 1, Down: 2, Left: 3 };
const DX = [0, 1, 0, -1];
const DY = [-1, 0, 1, 0];
const OPP = [Side.Down, Side.Left, Side.Up, Side.Right];

// Can `shape` connect ports a and b (distinct sides 0..3)?
function shapeAllows(shape, a, b) {
  if (a === b) return false;
  const diff = ((a - b) % 4 + 4) % 4;
  switch (shape) {
    case "straight": return diff === 2;            // opposite
    case "elbow":    return diff === 1 || diff === 3; // perpendicular
    case "tee":      return true;                  // any 2 of its 3 sides
    case "cross":    return true;
    default:         return false;
  }
}

function solvable(L) {
  const cells = new Map(); // "x,y" -> shape
  for (const t of L.tiles) cells.set(t.x + "," + t.y, t.shape);
  const entry = { x: L.entry, y: 0 };
  const exit = { x: L.exit, y: L.rows - 1 };

  // DFS over (cell, dir-we-travelled-to-get-here). Start: leave entry going Down.
  const start = { x: entry.x, y: entry.y + 1, dir: Side.Down };
  const seen = new Set();
  const stack = [start];
  while (stack.length) {
    const s = stack.pop();
    const k = s.x + "," + s.y;
    // Reached the exit cell by moving Down into it (entering its Up side)?
    if (s.x === exit.x && s.y === exit.y && s.dir === Side.Down) return true;
    if (s.x < 0 || s.y < 0 || s.x >= L.cols || s.y >= L.rows) continue;
    if (!cells.has(k)) continue;             // no pipe here
    const stateId = k + "|" + s.dir;
    if (seen.has(stateId)) continue;
    seen.add(stateId);
    const shape = cells.get(k);
    const inPort = OPP[s.dir];               // side facing where we came from
    for (let out = 0; out < 4; out++) {      // try leaving through each side
      if (out === inPort) continue;
      if (!shapeAllows(shape, inPort, out)) continue;
      stack.push({ x: s.x + DX[out], y: s.y + DY[out], dir: out });
    }
  }
  return false;
}

let bad = 0;
LEVELS.forEach((L, i) => {
  const ok = solvable(L);
  if (!ok) bad++;
  console.log(`${String(i + 1).padStart(2)}. ${ok ? "OK      " : "UNSOLVE!"} ${L.name}`);
});
console.log(bad === 0 ? "\nAll levels solvable." : `\n${bad} level(s) unsolvable.`);
process.exit(bad === 0 ? 0 : 1);
