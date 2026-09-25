/**
 * GRCS Service Worker
 * يُستخدم فقط بالنسخة المستضافة عبر HTTPS (GitHub Pages) — لا علاقة له
 * ببناء WebToApp الذي يخدم الملف كـ file:// ولا يدعم Service Worker أصلاً.
 *
 * الدور: 1) تثبيت PWA + عمل أوفلاين (شبكة أولاً، احتياطي من الكاش عند الانقطاع)
 *        2) استقبال إشعارات Web Push (RFC 8291) — بديل FCM النيتيف لـiOS Safari 16.4+
 */

const CACHE_NAME = 'grcs-cache-v3';
const APP_SHELL = ['./', './index.html', './manifest.json', './icon-192.png', './icon-512.png', './icon-maskable-512.png', './apple-touch-icon.png'];

self.addEventListener('install', event => {
  self.skipWaiting();
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => Promise.all(APP_SHELL.map(u => cache.add(u).catch(() => {}))))
      .catch(e => console.error('SW install cache error:', e))
  );
});

self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', event => {
  const url = new URL(event.request.url);

  // لا نتدخل بأي طلب خارجي (Firestore، FCM، GitHub API، خطوط/أيقونات CDN) — يمر مباشرة للشبكة كالمعتاد
  if (url.origin !== location.origin) return;
  if (event.request.method !== 'GET') return;

  // شبكة أولاً (يضمن وصول آخر تحديث فور توفر الاتصال)، احتياطي من الكاش عند الانقطاع فقط
  event.respondWith(
    fetch(event.request)
      .then(resp => {
        const clone = resp.clone();
        caches.open(CACHE_NAME).then(cache => cache.put(event.request, clone)).catch(() => {});
        return resp;
      })
      .catch(() =>
        caches.match(event.request).then(cached => cached || caches.match('./index.html'))
      )
  );
});

/* ── استقبال إشعارات Web Push (iOS Safari 16.4+, Chrome, Firefox, إلخ) ── */
self.addEventListener('push', event => {
  let data = {};
  try {
    data = event.data ? event.data.json() : {};
  } catch (e) {
    data = { title: 'منظومة مخيمات النزوح', body: event.data ? event.data.text() : '' };
  }
  const title = data.title || 'منظومة مخيمات النزوح';
  const options = {
    body: data.body || '',
    icon: './icon-192.png',
    badge: './icon-192.png',
    dir: 'rtl',
    lang: 'ar',
    vibrate: [100, 50, 100]
  };
  event.waitUntil(self.registration.showNotification(title, options));
});

self.addEventListener('notificationclick', event => {
  event.notification.close();
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then(windowClients => {
      for (const client of windowClients) {
        if ('focus' in client) return client.focus();
      }
      if (clients.openWindow) return clients.openWindow('./');
    })
  );
});


