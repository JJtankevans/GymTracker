// Servidor de teste: serve a pasta web/ e imita o pedaço do Supabase (auth + PostgREST + rpc) que o app web usa.
// Só para testes locais/CI: nada aqui fala com um Supabase de verdade.
import http from "node:http";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { randomUUID } from "node:crypto";

const WEB = path.join(path.dirname(fileURLToPath(import.meta.url)), "..", "web");
const MIME = { ".html": "text/html", ".js": "text/javascript", ".css": "text/css", ".json": "application/json", ".png": "image/png", ".webmanifest": "application/manifest+json" };
const b64 = (o) => Buffer.from(JSON.stringify(o)).toString("base64url");
const jwt = (sub, email) => `${b64({ alg: "HS256", typ: "JWT" })}.${b64({ sub, email, role: "authenticated", exp: Math.floor(Date.now() / 1000) + 3600, aud: "authenticated" })}.sig`;

export function seed() {
  const ME = "00000000-0000-0000-0000-000000000001";
  const day = (n, h = 18) => { const d = new Date(); d.setDate(d.getDate() - n); d.setHours(h, 0, 0, 0); return d.toISOString(); };
  const t = { profiles: [], muscle_groups: [], exercises: [], group_exercises: [], workouts: [], workout_sets: [], ranked_players: [], duels: [] };
  t.profiles.push({ id: ME, username: "thiago", avatar_url: null, weekly_goal: 3 });
  const gPeito = { id: "g-peito", profile_id: ME, name: "Peito e tríceps", color_hex: "#C6F432", created_at: day(30) };
  const gCosta = { id: "g-costa", profile_id: ME, name: "Costas e bíceps", color_hex: "#38BDF8", created_at: day(29) };
  t.muscle_groups.push(gPeito, gCosta);
  const ex = (id, name, reps = 10) => ({ id, profile_id: ME, name, default_sets: 3, default_reps: reps, notes: null, created_at: day(30) });
  t.exercises.push(ex("e-supino", "Supino reto"), ex("e-cruz", "Crucifixo"), ex("e-triceps", "Tríceps corda", 12), ex("e-remada", "Remada curvada"));
  t.group_exercises.push({ group_id: "g-peito", exercise_id: "e-supino" }, { group_id: "g-peito", exercise_id: "e-cruz" }, { group_id: "g-peito", exercise_id: "e-triceps" }, { group_id: "g-costa", exercise_id: "e-remada" });
  // 4 treinos no passado (3 dias seguidos até ontem)
  [[1, "g-peito"], [2, "g-costa"], [3, "g-peito"], [9, "g-peito"]].forEach(([n, g], i) => {
    const id = `w-old${i}`;
    t.workouts.push({ id, profile_id: ME, muscle_group_id: g, started_at: day(n, 18), finished_at: day(n, 19), notes: i === 0 ? "Foi pesado" : null, finished_exercise_ids: [] });
    if (g === "g-peito") [60, 62.5, 65].forEach((kg, k) => t.workout_sets.push({ id: `s-${id}-${k}`, workout_id: id, exercise_id: "e-supino", set_number: k + 1, reps: 10 - k, weight_kg: kg + (i === 0 ? 0 : -5), rest_seconds: 90, completed_at: day(n, 18) }));
    else t.workout_sets.push({ id: `s-${id}-0`, workout_id: id, exercise_id: "e-remada", set_number: 1, reps: 12, weight_kg: 50, rest_seconds: 90, completed_at: day(n, 18) });
  });
  return t;
}

export function startServer({ port = 0, withRanked = true } = {}) {
  const db = seed();
  const ME = db.profiles[0].id;
  const users = new Map([["thiago@teste.com", { id: ME, password: "senha123" }]]);
  const state = { me: null, calls: [] };
  const own = { db, state, users };

  const send = (res, code, body, extra = {}) => {
    const s = body === undefined ? "" : typeof body === "string" ? body : JSON.stringify(body);
    res.writeHead(code, { "content-type": "application/json", "access-control-allow-origin": "*", "access-control-allow-headers": "*", "access-control-allow-methods": "*", "access-control-expose-headers": "*", ...extra });
    res.end(s);
  };
  const session = (id, email) => ({ access_token: jwt(id, email), token_type: "bearer", expires_in: 3600, expires_at: Math.floor(Date.now() / 1000) + 3600, refresh_token: "r" + id, user: { id, email, aud: "authenticated", role: "authenticated", app_metadata: {}, user_metadata: {} } });

  // ---- filtros PostgREST ----
  const coerce = (v) => (v === "null" ? null : v === "true" ? true : v === "false" ? false : v);
  const cmp = (a, b) => (a < b ? -1 : a > b ? 1 : 0);
  function matches(row, key, expr) {
    const [op, ...rest] = expr.split(".");
    const val = rest.join(".");
    const cur = row[key];
    switch (op) {
      case "eq": return String(cur) === val;
      case "neq": return String(cur) !== val;
      case "is": return val === "null" ? cur == null : String(cur) === val;
      case "not": { const [op2, ...r2] = rest; return !matches(row, key, op2 + "." + r2.join(".")); }
      case "in": return val.replace(/^\(|\)$/g, "").split(",").map((x) => x.replace(/^"|"$/g, "")).includes(String(cur));
      case "gt": return cur > val; case "gte": return cur >= val; case "lt": return cur < val; case "lte": return cur <= val;
      default: throw new Error("filtro não suportado: " + op);
    }
  }
  function rest(req, res, url, body) {
    const table = url.pathname.replace("/rest/v1/", "");
    const q = url.searchParams;
    state.calls.push(`${req.method} ${table}`);
    if (table.startsWith("rpc/")) return rpc(table.slice(4), body ? JSON.parse(body) : {}, res);
    const rows = db[table];
    if (!rows) return send(res, 404, { message: `relation "${table}" does not exist`, code: "42P01" });
    const filters = [...q.entries()].filter(([k]) => !["select", "order", "limit", "offset", "on_conflict", "columns"].includes(k));
    const apply = (list) => list.filter((r) => filters.every(([k, v]) => matches(r, k, v)));
    const wantObj = (req.headers.accept || "").includes("vnd.pgrst.object");
    const out = (list, code = 200) => {
      if (wantObj) return list.length === 1 ? send(res, code, list[0]) : send(res, 406, { code: "PGRST116", message: "JSON object requested, multiple (or no) rows returned" });
      return send(res, code, list);
    };
    if (req.method === "GET" || req.method === "HEAD") {
      let list = apply(rows).slice();
      const order = q.get("order");
      if (order) for (const spec of order.split(",").reverse()) { const [col, dir] = spec.split("."); list.sort((a, b) => (dir === "desc" ? -1 : 1) * cmp(a[col], b[col])); }
      const off = parseInt(q.get("offset") || "0", 10); const lim = q.get("limit");
      list = list.slice(off, lim ? off + parseInt(lim, 10) : undefined);
      return out(list);
    }
    if (req.method === "POST") {
      const arr = [].concat(JSON.parse(body));
      const made = arr.map((r) => ({ id: table === "group_exercises" ? undefined : randomUUID(), created_at: new Date().toISOString(), ...(table === "workouts" ? { started_at: new Date().toISOString(), finished_at: null, notes: null, finished_exercise_ids: [] } : {}), ...r }));
      if (table === "group_exercises") made.forEach((m) => delete m.id);
      rows.push(...made);
      return (req.headers.prefer || "").includes("return=representation") ? out(made, 201) : send(res, 201);
    }
    if (req.method === "PATCH") {
      const hit = apply(rows); const patch = JSON.parse(body);
      hit.forEach((r) => Object.assign(r, patch));
      return (req.headers.prefer || "").includes("return=representation") ? out(hit) : send(res, 204);
    }
    if (req.method === "DELETE") {
      const hit = new Set(apply(rows));
      db[table] = rows.filter((r) => !hit.has(r));
      if (table === "workouts") db.workout_sets = db.workout_sets.filter((s) => db.workouts.some((w) => w.id === s.workout_id));
      if (table === "exercises" || table === "muscle_groups") { /* vínculos ficam órfãos: o app filtra */ }
      return send(res, 204);
    }
    send(res, 405, { message: "método" });
  }

  function rpc(name, args, res) {
    if (name === "records_summary") {
      const by = new Map();
      for (const s of db.workout_sets) { const l = by.get(s.exercise_id) || []; l.push(s); by.set(s.exercise_id, l); }
      return send(res, 200, [...by].map(([id, l]) => {
        const best = Math.max(...l.map((s) => s.weight_kg));
        const prev = l.filter((s) => s.weight_kg < best).map((s) => s.weight_kg);
        return { exercise_id: id, exercise_name: db.exercises.find((e) => e.id === id)?.name || "?", best_weight_kg: best, best_date: "2026-11-01", best_one_rm_kg: best * 1.33, previous_best_kg: prev.length ? Math.max(...prev) : null, sessions: new Set(l.map((s) => s.workout_id)).size };
      }));
    }
    if (name === "delete_my_account") { state.deleted = true; return send(res, 200, null); }
    if (name.startsWith("ranked_")) {
      if (!withRanked) return send(res, 404, { code: "PGRST202", message: "Could not find the function public." + name });
      if (name === "ranked_join") { db.ranked_players.push({ profile_id: ME, rp: 0, peak_rp: 0, weeks_played: 0 }); return send(res, 200, 0); }
      if (name === "ranked_board") return send(res, 200, [
        { pos: 1, profile_id: "u2", username: "Bruno", avatar_url: null, rp: 410, points: 320, is_me: false },
        { pos: 2, profile_id: ME, username: "thiago", avatar_url: null, rp: 380, points: 210, is_me: true },
        { pos: 3, profile_id: "u3", username: "Carla", avatar_url: null, rp: 395, points: 150, is_me: false },
        { pos: 4, profile_id: "u4", username: "Diego", avatar_url: null, rp: 120, points: 90, is_me: false },
        { pos: 5, profile_id: "u5", username: "Elisa", avatar_url: null, rp: 300, points: 0, is_me: false }]);
      if (name === "ranked_last_result") return send(res, 200, [{ week_start: "2026-10-26", points: 180, pos: 2, cohort_size: 5, delta: 30, rp_after: 380 }]);
      if (name === "ranked_my_duels") return send(res, 200, db.duels);
      if (name === "ranked_duel_create") { db.duels.push({ id: randomUUID(), ranked: args.p_ranked, status: "pending", starts_at: null, ends_at: null, challenger_id: ME, challenger_name: "thiago", challenger_avatar: null, challenger_rp: 380, pt_challenger: null, opponent_id: args.p_opponent, opponent_name: "Bruno", opponent_avatar: null, opponent_rp: 410, pt_opponent: null, winner_id: null, delta_challenger: null, delta_opponent: null }); return send(res, 200, null); }
      return send(res, 200, 0);
    }
    send(res, 404, { code: "PGRST202", message: `Could not find the function public.${name}` });
  }

  const server = http.createServer((req, res) => {
    const url = new URL(req.url, "http://x");
    if (req.method === "OPTIONS") return send(res, 204);
    const chunks = [];
    req.on("data", (c) => chunks.push(c));
    req.on("end", () => {
      const body = Buffer.concat(chunks).toString();
      try {
        if (url.pathname.startsWith("/auth/v1/")) {
          const ep = url.pathname.slice(9);
          if (ep === "token") {
            const { email, password } = JSON.parse(body);
            const u = users.get(email);
            if (!u || u.password !== password) return send(res, 400, { error: "invalid_grant", error_description: "Invalid login credentials", msg: "Invalid login credentials" });
            state.me = u.id; return send(res, 200, session(u.id, email));
          }
          if (ep === "signup") {
            const { email, password } = JSON.parse(body);
            if (users.has(email)) return send(res, 422, { msg: "User already registered", code: "user_already_exists" });
            const id = randomUUID(); users.set(email, { id, password });
            return send(res, 200, own.confirmEmail ? { id, email, aud: "authenticated" } : session(id, email));
          }
          if (ep === "user") return send(res, 200, { id: state.me || ME, email: "thiago@teste.com", aud: "authenticated" });
          if (ep === "logout") return send(res, 204);
          return send(res, 404, { msg: "auth" });
        }
        if (url.pathname.startsWith("/rest/v1/")) return rest(req, res, url, body);
        if (url.pathname === "/config.js") return send(res, 200, `window.GYM_CONFIG = { url: "http://${req.headers.host}", key: "test-anon-key" };`, { "content-type": "text/javascript" });
        let p = path.join(WEB, decodeURIComponent(url.pathname));
        if (!p.startsWith(WEB)) return send(res, 403, "no");
        if (fs.existsSync(p) && fs.statSync(p).isDirectory()) p = path.join(p, "index.html");
        if (!fs.existsSync(p)) return send(res, 404, "not found", { "content-type": "text/plain" });
        res.writeHead(200, { "content-type": MIME[path.extname(p)] || "application/octet-stream", "cache-control": "no-store" });
        fs.createReadStream(p).pipe(res);
      } catch (e) { console.error(e); send(res, 500, { message: String(e) }); }
    });
  });
  return new Promise((resolve) => server.listen(port, "127.0.0.1", () => resolve({ server, port: server.address().port, ...own, url: `http://127.0.0.1:${server.address().port}`, close: () => new Promise((r) => server.close(r)) })));
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const s = await startServer({ port: 8099 });
  console.log("Servidor de teste em", s.url, "(login thiago@teste.com / senha123)");
}
