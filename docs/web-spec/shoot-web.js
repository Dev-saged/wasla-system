// يلتقط شاشات الويب ببيانات fixture.json: node shoot-web.js <index.html> <outDir> [light|dark]
const { chromium } = require('playwright'); const fs = require('fs'); const path = require('path');
(async () => {
  const [src, out, theme = 'light'] = process.argv.slice(2);
  const fx = JSON.parse(fs.readFileSync(path.join(__dirname, 'fixture.json'), 'utf8'));
  const L = fs.readFileSync(src, 'utf8').split('\n');
  const end = L.findIndex((l, i) => i > 15000 && l.includes('</script>'));
  L.splice(end, 0, 'window.__G={State,nav,enterApp,dbPut,dbOpen,renderBottomNav};');
  const tmp = path.join(out, '_harness.html'); fs.mkdirSync(out, { recursive: true }); fs.writeFileSync(tmp, L.join('\n'));
  const b = await chromium.launch();
  const failed = new Set();
  for (const role of (process.env.ROLES || 'admin,delegate,dataentry,beneficiary').split(',')) {
    const ctx = await b.newContext({ viewport: { width: 412, height: 915 }, deviceScaleFactor: 1, locale: 'ar', colorScheme: theme });
    const pg = await ctx.newPage();
    pg.on('requestfailed', r => failed.add(new URL(r.url()).host));
    pg.on('pageerror', e => console.log('ERR', role, e.message.slice(0, 160)));
    await pg.addInitScript(t => { try { localStorage.setItem('grcs_theme', t); } catch (e) {} }, theme);
    await pg.goto('file://' + tmp); await pg.waitForFunction(() => window.__G, null, { timeout: 15000 }); await pg.waitForLoadState('networkidle').catch(() => {}); await ctx.setOffline(true);
    const shot = async n => { await pg.waitForTimeout(900); await pg.screenshot({ path: path.join(out, `${role}-${n}.png`) }); };
    if (role === 'admin') { await pg.waitForTimeout(2600); await pg.screenshot({ path: path.join(out, 'splash.png') }).catch(() => {}); await pg.evaluate(() => __G.nav('screen-login')); await shot('login'); }
    await pg.evaluate(async ({ fx, role }) => {
      const G = window.__G; await G.dbOpen();
      for (const c of fx.camps) await G.dbPut('camps', { ...c, totalFamilies: 0, lastUpdated: fx.now });
      const fams = {};
      for (const p of fx.persons) { await G.dbPut('persons', p); (fams[p.familyId] ||= { id: p.familyId, campId: p.campId, memberCount: 0, headName: '', createdAt: fx.now }).memberCount++; if (p.isFamilyHead) fams[p.familyId].headName = p.fullName; }
      for (const f of Object.values(fams)) await G.dbPut('families', f);
      for (const u of Object.values(fx.users)) await G.dbPut('users', u);
      G.State.online = false; G.State.user = fx.users[role]; await G.enterApp();
    }, { fx, role });
    await pg.waitForTimeout(1500); await shot('home');
    await pg.evaluate(() => window.scrollTo(0, 0));
    const full = async n => { await pg.waitForTimeout(700); await pg.screenshot({ path: path.join(out, `${role}-${n}-full.png`), fullPage: true }); };
    await full('home');
    const screens = { admin: ['camps-admin', 'messages-list', 'accounts-admin', 'settings', 'audit', 'quality', 'aid', 'similar', 'query', 'export'], delegate: ['persons', 'messages-list', 'export', 'settings', 'camp-info', 'person-form', 'import', 'rapid', 'quality', 'aid'], dataentry: ['persons', 'settings'], beneficiary: ['messages-list', 'settings'] }[role];
    for (const s of screens) { await pg.evaluate(s => { try { __G.nav('screen-' + s); } catch (e) { console.log(e); } }, s); await shot(s); await full(s); }
    if (role === 'delegate') {
      await pg.evaluate(() => __G.nav('screen-persons')); await pg.waitForTimeout(800);
      const card = await pg.$('#persons-list .person-card, #persons-list > *'); if (card) { await card.click(); await shot('person-detail'); }
    }
    await ctx.close();
  }
  console.log('failed hosts:', [...failed].join(', ')); await b.close(); fs.unlinkSync(tmp);
})();
