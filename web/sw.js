// Service worker do GymTracker web: guarda o "casco" do app para abrir rápido e funcionar mesmo com a rede ruim.
// Só arquivos do próprio site: as chamadas ao Supabase (outro domínio) nunca passam por aqui.
const VERSION = "gymtracker-web-v1";
const SHELL = [
  "./", "index.html", "manifest.webmanifest", "config.js", "catalog.json",
  "css/app.css", "vendor/supabase-js-2.76.1.js",
  "icons/icon-192.png", "icons/icon-512.png", "icons/apple-touch-icon.png"
];

self.addEventListener("install", (e) => {
  e.waitUntil(caches.open(VERSION).then((c) => c.addAll(SHELL)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", (e) => {
  e.waitUntil(
    caches.keys().then((keys) => Promise.all(keys.filter((k) => k !== VERSION).map((k) => caches.delete(k)))).then(() => self.clients.claim())
  );
});

// config.js e os scripts do app: rede primeiro (para pegar versão nova), cache se estiver sem rede.
// O resto do casco: cache primeiro, atualizando por baixo dos panos.
self.addEventListener("fetch", (e) => {
  const req = e.request;
  const url = new URL(req.url);
  if (req.method !== "GET" || url.origin !== self.location.origin || /\/(rest|auth)\/v1\//.test(url.pathname)) return;
  const networkFirst = url.pathname.endsWith("config.js") || url.pathname.includes("/js/") || url.pathname.endsWith("/") || url.pathname.endsWith("index.html");
  if (networkFirst) {
    e.respondWith(fetch(req).then((res) => { const copy = res.clone(); caches.open(VERSION).then((c) => c.put(req, copy)); return res; }).catch(() => caches.match(req)));
  } else {
    e.respondWith(caches.match(req).then((hit) => {
      const fresh = fetch(req).then((res) => { const copy = res.clone(); caches.open(VERSION).then((c) => c.put(req, copy)); return res; }).catch(() => hit);
      return hit || fresh;
    }));
  }
});
