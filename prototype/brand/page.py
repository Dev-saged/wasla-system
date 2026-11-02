import gen,os,html
d=os.path.dirname(os.path.abspath(__file__))
def disc_bg(k):
    P=gen.MODELS[k];return P['disc'].replace('UID',k+'i'),P.get('bgdef','').replace('UID',k+'i')
sym='<svg width="0" height="0" style="position:absolute" aria-hidden="true"><defs>'
for k,P in gen.MODELS.items():
    kw=gen.KW[k[:2]]
    sym+='<symbol id="b-%s" viewBox="0 0 1000 1000">%s</symbol>'%(k,gen.badge(P,k,True,**kw))
    fill,bgd=disc_bg(k)
    sym+='<symbol id="i-%s" viewBox="0 0 108 108"><defs>%s</defs><rect width="108" height="108" fill="%s"/>%s</symbol>'%(k,bgd,fill,gen.icon(P,k+'i',**kw))
sym+='</defs></svg>'
def ic(k,sz,m):return '<span class="ic %s" style="width:%dpx;height:%dpx"><svg viewBox="0 0 108 108"><use href="#i-%s"/></svg></span>'%(m,sz,sz,k)
INFO=[('m1','١. الشارة الكوبالتية','الأقرب لروح التصميم المرجعي: قرص عاجي دافئ وخيمة برتقالية تتصدّر، لكن بحلقة كوبالتية مطرّزة بنجوم الثمانية، وبوصلة تطريز على نافذة الخيمة بدل الخريطة، وعلم صغير على القمة، ونخلتان وطيور. نسخة «زيتونية» لنفس التصميم للمخيمات الزراعية أو لهوية أهدأ.','نقية وواضحة بحجم صغير، الأنسب كأيقونة رئيسية وكشعار لشاشة الدخول.'),
('m2','٢. الفجر','مشهد كامل داخل الشارة: سماء تتدرج من الكوبالت إلى ضوء الأفق، وشمس تشرق خلف خيمة بيضاء، والنخلة والطيور بصورة ظل. النافذة تحمل نجمة التطريز. نسخة «غروب» بالبنفسجي والبرتقالي.','الأكثر فخامة وإلهاماً، وتناسب شاشة الافتتاح الحيّة (الشمس ترتفع ببطء).'),
('m3','٣. الليل المضيء','ليل عميق وقمر ونجوم متناثرة، والخيمة تتوهّج بالبرتقالي مع نار صغيرة في بابها، وحلقة ذهبية. نسخة «زمردية». توحي بالأمان والدفء وسط العتمة، وهي الأكثر تميّزاً بين أيقونات الأنظمة.','الأنسب للمظهر الداكن، وتبرز على شاشات الجوال الفاتحة والداكنة معاً.')]
cards=''
for m,t,desc,why in INFO:
    a,b=m+'a',m+'b'
    cards+='''<article class="card"><div><h3>%s</h3><p>%s</p><p class="why"><b>التوصية:</b> %s</p></div>
<div class="pair"><figure><svg class="bd" viewBox="0 0 1000 1000"><use href="#b-%s"/></svg><figcaption>اللون الأساسي</figcaption></figure><figure><svg class="bd" viewBox="0 0 1000 1000"><use href="#b-%s"/></svg><figcaption>لون بديل</figcaption></figure></div>
<div class="irow"><div class="cell">%s<span>دائري</span></div><div class="cell">%s<span>مربع مدوّر</span></div><div class="cell">%s<span>48</span></div><div class="cell">%s<span>24</span></div><div class="cell">%s<span>بديل</span></div></div>
<div class="strip dk">%s<i class="dec"></i>%s<i class="dec sq"></i></div><div class="strip lt">%s<i class="dec"></i>%s<i class="dec sq"></i></div></article>'''%(t,desc,why,a,b,ic(a,104,'c'),ic(a,92,'q'),ic(a,48,'c'),ic(a,24,'c'),ic(b,92,'q'),ic(a,54,'q'),ic(b,54,'q'),ic(a,54,'q'),ic(b,54,'q'))
css='''
:root{--bg:oklch(.975 .006 250);--sf:oklch(1 0 0);--ink:oklch(.22 .03 250);--mute:oklch(.45 .03 250);--line:oklch(.9 .008 250);--pri:oklch(.45 .14 262);--f:"IBM Plex Sans Arabic","Noto Sans Arabic",system-ui,sans-serif}
@media (prefers-color-scheme:dark){:root:not([data-theme="light"]){--bg:oklch(.17 .02 255);--sf:oklch(.22 .025 255);--ink:oklch(.95 .01 250);--mute:oklch(.74 .025 250);--line:oklch(.32 .025 255);--pri:oklch(.78 .11 255);color-scheme:dark}}
:root[data-theme="dark"]{--bg:oklch(.17 .02 255);--sf:oklch(.22 .025 255);--ink:oklch(.95 .01 250);--mute:oklch(.74 .025 250);--line:oklch(.32 .025 255);--pri:oklch(.78 .11 255);color-scheme:dark}
*{box-sizing:border-box}body{background:var(--bg);color:var(--ink);font-family:var(--f);line-height:1.6;padding-block:24px 64px;padding-left:max(16px,env(safe-area-inset-left,0px));padding-right:max(16px,env(safe-area-inset-right,0px))}
.wrap{max-width:1180px;margin-inline:auto}h1,h2,h3,p{margin:0}h1{font-size:clamp(1.5rem,4vw,2.2rem);line-height:1.25;text-wrap:balance}
.lead{color:var(--mute);max-width:66ch;margin-top:8px}
.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,340px),1fr));gap:20px;margin-top:24px}
.card{background:var(--sf);border:1px solid var(--line);border-radius:22px;padding:20px;display:grid;gap:18px;align-content:start}
.card h3{font-size:1.15rem;margin-bottom:6px}.card p{color:var(--mute);font-size:.9rem}.why{margin-top:8px}.why b{color:var(--ink)}
.pair{display:grid;grid-template-columns:1fr 1fr;gap:10px}.pair figure{margin:0;display:grid;gap:4px;justify-items:center}.bd{width:100%;height:auto;aspect-ratio:1}
figcaption,.cell span{font-size:.74rem;color:var(--mute);text-align:center}
.irow{display:flex;flex-wrap:wrap;gap:14px;align-items:flex-end}.cell{display:grid;gap:4px;justify-items:center}
.ic{display:block;overflow:hidden;flex:none}.ic svg{display:block;width:100%;height:100%}.c{border-radius:50%}.q{border-radius:30%}
.strip{display:flex;gap:12px;padding:12px;border-radius:16px;align-items:center;justify-content:center}.strip.dk{background:oklch(.2 .02 250)}.strip.lt{background:oklch(.93 .015 230)}
.dec{width:54px;height:54px;border-radius:50%;display:block}.dec.sq{border-radius:30%}.strip.dk .dec{background:oklch(.34 .02 250)}.strip.lt .dec{background:oklch(.82 .015 230)}
.note{margin-top:24px;background:var(--sf);border:1px solid var(--line);border-radius:16px;padding:16px;color:var(--mute);font-size:.9rem}.note b{color:var(--ink)}
'''
out='''<title>شارات منظومة المخيمات</title>
<meta name="claude:viewport-fit-sides" content="cover">
<link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Lalezar&family=IBM+Plex+Sans+Arabic:wght@400;500;600;700&display=swap" rel="stylesheet">
<style>%s</style>
<main class="wrap" dir="rtl" lang="ar"><header><h1>ثلاث شارات للمنظومة بمستوى التفاصيل المرجعي</h1>
<p class="lead">حلّلت ملفَّي PSD: الشارة دائرية بحلقة بنية سميكة، وخلفية عاجية بنقش ماندالا باهت، وخيمة برتقالية مشبعة هي البطل، ويحيط بها علم ونخلتان وطيور ونار. الخيمة والنخيل والنقش والنار عناصر جاهزة (صور من مكتبات)، لذلك رسمت كل شيء من جديد كمتجهات أصلية بلا أي أصل مأخوذ، وبهويتنا: الكوبالت والتطريز وخط «لالزار» للعناوين.</p></header>
%s<div class="grid">%s</div>
<p class="note"><b>الأيقونة التكيّفية</b> تعرض نفس المشهد داخل المنطقة الآمنة وبدون نص. الأيقونة الأحادية للثيم تبقى «الخيمة والباب» لأن المشهد الملوّن لا يصلح كظل واحد. اختر نموذجاً ولوناً، وأطبّقه على الأيقونة وشاشة الافتتاح وشعار الدخول وأثبّت ألوانه في BrandConfig.</p></main>'''%(css,sym,cards)
open(os.path.join(d,'badges.html'),'w',encoding='utf-8').write(out)
