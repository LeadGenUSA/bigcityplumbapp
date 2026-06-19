// Runtime smoke test: executes the game's <script> against stubbed browser
// APIs and drives the new flood code paths, to catch ReferenceErrors / typos
// that a syntax check alone misses. Not a substitute for on-device testing.
const fs = require("fs");
const path = require("path");
const vm = require("vm");

const html = fs.readFileSync(path.join(__dirname, "..", "shared", "game", "index.html"), "utf8");
const script = html.match(/<script>([\s\S]*)<\/script>/)[1];

// --- minimal browser stubs ---
const noop = () => {};
const ctxProxy = new Proxy({}, {
  get: (t, k) => (k in t ? t[k] : () => ({ addColorStop: noop })),
  set: (t, k, v) => { t[k] = v; return true; },
});
function fakeEl() {
  return new Proxy({
    style: {}, classList: { add: noop, remove: noop, toggle: noop, contains: () => false },
    addEventListener: noop, removeEventListener: noop, appendChild: noop,
    getContext: () => ctxProxy, getBoundingClientRect: () => ({ left: 0, top: 0, width: 320, height: 480 }),
    width: 320, height: 480, clientWidth: 320, clientHeight: 480, offsetHeight: 40,
    textContent: "", onclick: null, disabled: false,
  }, { get: (t, k) => (k in t ? t[k] : noop), set: (t, k, v) => { t[k] = v; return true; } });
}
const store = {};
const audioNode = () => ({
  type: "", frequency: { setValueAtTime: noop, exponentialRampToValueAtTime: noop, value: 0 },
  gain: { setValueAtTime: noop, exponentialRampToValueAtTime: noop, value: 0 },
  connect: () => audioNode(), start: noop, stop: noop,
});
class AudioContextStub {
  constructor() { this.currentTime = 0; this.sampleRate = 44100; this.destination = {}; this.state = "running"; }
  resume() {} createOscillator() { return audioNode(); } createGain() { return audioNode(); }
  createBuffer(c, n) { return { getChannelData: () => new Float32Array(n) }; }
  createBufferSource() { return audioNode(); }
  createBiquadFilter() { return audioNode(); }
}
class ImageStub { set src(_) { } get naturalWidth() { return 0; } get naturalHeight() { return 0; } }

const ctx = {
  console,
  document: new Proxy({ getElementById: fakeEl, querySelector: fakeEl, createElement: fakeEl, body: fakeEl(), addEventListener: noop },
    { get: (t, k) => (k in t ? t[k] : noop) }),
  window: {}, navigator: { vibrate: noop }, addEventListener: noop,
  localStorage: { getItem: (k) => (k in store ? store[k] : null), setItem: (k, v) => { store[k] = v; } },
  performance: { now: () => Date.now() },
  requestAnimationFrame: (cb) => { ctx.__raf = cb; return 1; },
  AudioContext: AudioContextStub, webkitAudioContext: AudioContextStub,
  Image: ImageStub, Path2D: function () { return new Proxy({}, { get: () => noop }); },
  setTimeout: () => 0, clearTimeout: noop, setInterval: () => 0,
  Math, Date, JSON, isNaN, parseInt, parseFloat,
};
ctx.window = ctx; ctx.globalThis = ctx;
vm.createContext(ctx);

try {
  vm.runInContext(script, ctx, { filename: "game.js" });
  console.log("✓ script executed (boot + loadLevel(0) ran)");

  // Drive the flood paths explicitly.
  ctx.floodLevel = 0.5;
  ctx.draw();                                   // flood-water render branch
  console.log("✓ draw() with rising water");

  ctx.floodLast = ctx.performance.now() - 50;
  ctx.floodActive = true;
  ctx.floodTick(ctx.performance.now());         // timer tick
  console.log("✓ floodTick() advanced level to " + ctx.floodLevel.toFixed(3));

  ctx.startDrop();                              // level 0 unsolved -> "Not quite" + pause
  console.log("✓ startDrop() on unsolved board");

  ctx.triggerFlood();                           // lose state + Sound.flood()
  console.log("✓ triggerFlood() (flood SFX + overlay)");

  // Note: board/dropProgress/LEVELS are lexical (const/let) and not exposed on
  // the VM global, so the win/drain path can't be driven from here — it's simple
  // and is exercised in real play.
  console.log("\nAll smoke checks passed (no runtime errors in flood code).");
} catch (e) {
  console.error("RUNTIME ERROR:", e && e.stack || e);
  process.exit(1);
}
