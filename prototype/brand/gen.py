# مولّد شارات الهوية (تصميم أصلي بالكامل بالمتجهات)
import math,os
def hexc(l,c,h,a=None):
    hr=math.radians(h);A=c*math.cos(hr);B=c*math.sin(hr)
    l_=l+.3963377774*A+.2158037573*B;m_=l-.1055613458*A-.0638541728*B;s_=l-.0894841775*A-1.2914855480*B
    l3,m3,s3=l_**3,m_**3,s_**3
    r=4.0767416621*l3-3.3077115913*m3+.2309699292*s3
    g=-1.2684380046*l3+2.6097574011*m3-.3413193965*s3
    b=-.0041960863*l3-.7034186147*m3+1.7076147010*s3
    f=lambda x:max(0,min(1,12.92*x if x<=.0031308 else 1.055*x**(1/2.4)-.055))
    return '#%02x%02x%02x'%tuple(round(f(v)*255) for v in (r,g,b))
H=lambda *a:hexc(*a)

def star(cx,cy,R,r=None):
    r=r or R*.3;pts=[]
    for i in range(16):
        a=math.radians(i*22.5-90);rad=R if i%2==0 else r
        pts.append('%.1f,%.1f'%(cx+rad*math.cos(a),cy+rad*math.sin(a)))
    return 'M'+' L'.join(pts)+'Z'

def leaf(x,y,ang,L,w,droop):
    a=math.radians(ang);tx=x+L*math.cos(a);ty=y+L*math.sin(a)+droop
    mx=(x+tx)/2;my=(y+ty)/2-droop*.4;px=-math.sin(a)*w;py=math.cos(a)*w
    return 'M%.1f,%.1f Q%.1f,%.1f %.1f,%.1f Q%.1f,%.1f %.1f,%.1f Z'%(x,y,mx+px,my+py,tx,ty,mx-px,my-py,x,y)
def palm(bx,by,cx,cy,col,trunk,sc=1):
    mx=(bx+cx)/2+ (cx-bx)*.25;my=(by+cy)/2
    s='<path d="M%d,%d Q%d,%d %d,%d" fill="none" stroke="%s" stroke-width="%d" stroke-linecap="round"/>'%(bx,by,mx,my+30,cx,cy,trunk,int(15*sc))
    ls=''.join('<path d="%s"/>'%leaf(cx,cy,a,95*sc,15*sc,28*sc) for a in (-170,-140,-110,-70,-40,-10,20,160,195))
    return s+'<g fill="%s">%s</g><circle cx="%d" cy="%d" r="%d" fill="%s"/>'%(col,ls,cx,cy+4,int(9*sc),trunk)
def bird(x,y,s,col):
    return '<path transform="translate(%d %d) scale(%.2f)" d="M-22,0 Q-11,-14 0,-3 Q11,-14 22,0 Q11,-6 0,2 Q-11,-6 -22,0Z" fill="%s"/>'%(x,y,s,col)
def flag(x,y,w,h):
    out='';n=14;bands=[('#101010',0),('#ffffff',1),('#0a7a3e',2)]
    for col,k in bands:
        pts=[];pts2=[]
        for i in range(n+1):
            t=i/n;px=x+w*t;wave=math.sin(t*math.pi*1.6)*7*(t)
            pts.append((px,y+h*k/3+wave));pts2.append((px,y+h*(k+1)/3+wave))
        d='M'+' L'.join('%.1f,%.1f'%p for p in pts)+' L'+' L'.join('%.1f,%.1f'%p for p in reversed(pts2))+'Z'
        out+='<path d="%s" fill="%s"/>'%(d,col)
    out+='<path d="M%.1f,%.1f L%.1f,%.1f L%.1f,%.1f Z" fill="#ce1126"/>'%(x,y,x+w*.42,y+h/2+1,x,y+h)
    return out+'<path d="M%.1f,%.1f H%.1f" stroke="none"/>'%(x,y,x)

def scene(P,u,with_flag=True,with_sun=False,stars=False):
    g=''
    g+='<defs><radialGradient id="%sgl" cx=".45" cy="1" r=".9"><stop offset="0" stop-color="%s"/><stop offset="1" stop-color="%s"/></radialGradient></defs>'%(u,P['glow1'],P['door'])
    if with_sun:g+='<circle cx="560" cy="700" r="215" fill="%s" opacity=".95"/><circle cx="560" cy="700" r="270" fill="%s" opacity=".25"/>'%(P['sun'],P['sun'])
    g+='<ellipse cx="540" cy="836" rx="345" ry="36" fill="%s"/>'%P['ground']
    g+=palm(790,790,832,500,P['palm'],P['trunk'],1.15)+palm(880,800,930,610,P['palm2'],P['trunk'],.95)
    for bx,by,s in ((600,430,1.0),(668,392,.85),(724,452,.7)):g+=bird(bx,by,s,P['bird'])
    # الخيمة
    g+='<path d="M430,545 L700,575 L868,805 L618,825Z" fill="%s"/>'%P['tent_side']
    g+='<path d="M430,545 L618,825 L238,815Z" fill="%s"/>'%P['tent_front']
    g+='<path d="M430,545 L700,575 L868,805 L618,825Z" fill="none" stroke="%s" stroke-width="5" stroke-linejoin="round"/>'%P['seam']
    g+='<path d="M430,545 L238,815 M430,545 L618,825" stroke="%s" stroke-width="5" stroke-linecap="round" fill="none"/>'%P['seam']
    g+='<path d="M430,598 L352,815 Q430,832 508,820Z" fill="url(#%sgl)"/>'%u
    g+='<path d="M430,598 Q400,700 352,815" fill="none" stroke="%s" stroke-width="4" opacity=".6"/>'%P['seam']
    g+='<path d="M430,545 L330,816 L238,815Z" fill="%s" opacity=".35"/>'%P['fold']
    g+='<path d="M575,668 L694,682 L752,752 L640,746Z" fill="%s" stroke="%s" stroke-width="6" stroke-linejoin="round"/>'%(P['win'],P['seam'])
    g+='<path d="%s" fill="%s" transform="translate(668 710) scale(1.2 .95)"/>'%(star(0,0,34,9),P['star'])
    g+='<path d="M430,545 L215,842 M700,575 L905,838" stroke="%s" stroke-width="4" fill="none"/>'%P['rope']
    for sx,sy in ((215,842),(905,838)):g+='<ellipse cx="%d" cy="%d" rx="20" ry="13" fill="%s"/>'%(sx,sy,P['stake'])
    # نار
    g+='<g transform="translate(436 818)"><path d="M-30,0 L22,-10 M-22,-12 L30,0" stroke="%s" stroke-width="9" stroke-linecap="round"/><path d="M0,-4 C-22,-26 -12,-50 0,-82 C12,-52 30,-30 18,-4Z" fill="%s"/><path d="M2,-6 C-8,-22 -2,-38 3,-54 C12,-38 16,-24 11,-6Z" fill="%s"/></g>'%(P['log'],P['flame'],P['flame2'])
    if with_flag:
        g+='<path d="M430,545 V418" stroke="%s" stroke-width="6" stroke-linecap="round"/>'%P['pole']+flag(433,420,118,72)
    return g

def badge(P,u,text_lines=True,**kw):
    s='<defs><clipPath id="%sc"><circle cx="500" cy="500" r="448"/></clipPath>%s</defs>'%(u,P.get('bgdef','').replace('UID',u))
    s+='<circle cx="500" cy="500" r="488" fill="%s"/>'%P['ring']
    for i in range(28):
        a=math.radians(i*360/28);s+='<path d="%s" fill="%s"/>'%(star(500+468*math.cos(a),500+468*math.sin(a),17,5),P['bandstar'])
    s+='<circle cx="500" cy="500" r="448" fill="%s"/>'%P['disc'].replace('UID',u)
    s+='<circle cx="500" cy="500" r="442" fill="none" stroke="%s" stroke-width="3" opacity=".6"/>'%P['bandstar']
    if P.get('mark'):
        pet='<path d="M0,-60 Q34,-150 0,-250 Q-34,-150 0,-60Z"/>'
        s+='<g clip-path="url(#%sc)" transform="translate(500 560)" fill="none" stroke="%s" stroke-width="3" opacity="%s">'%(u,P['mark'],P.get('markop','.1'))
        s+=''.join('<g transform="rotate(%d)">%s</g>'%(i*22.5,pet) for i in range(16))
        s+=''.join('<circle r="%d"%s/>'%(r,' stroke-dasharray="3 9"' if k%2 else '') for k,r in enumerate((90,190,300,400)))+'</g>'
    if kw.get('stars'):
        import random;random.seed(7)
        for _ in range(26):
            x=random.randint(120,880);y=random.randint(100,420)
            if (x-500)**2+(y-500)**2<420**2:s+='<path d="%s" fill="%s" opacity="%.2f"/>'%(star(x,y,random.choice((6,8,11)),2.5),P['textcol'],random.choice((.5,.7,.9)))
        s+='<path d="M810,190 a58,58 0 1 0 60,70 a46,46 0 1 1 -60,-70Z" fill="%s"/>'%P['moon']
    if text_lines:
        s+='<g font-family="Lalezar,\'Alexandria\',sans-serif" fill="%s" text-anchor="middle"><text x="500" y="196" font-size="98">منظومة إدارة</text><text x="500" y="308" font-size="128">المخيمات</text></g>'%P['textcol']
    s+=''.join('<path d="%s" fill="%s" opacity=".85"/>'%(star(500+(i-3)*46,878,i%2 and 11 or 15,4),P['textcol']) for i in range(7))
    s+='<g transform="translate(500 -76) translate(-560 0)" clip-path="url(#%sc)">'%u
    s+=scene(P,u,kw.get('flag',True),kw.get('sun',False))+'</g>'
    return s

def icon(P,u,**kw):
    # أيقونة تكيفية: طبقة خلفية + طبقة أمامية داخل المنطقة الآمنة
    s=scene(P,u,kw.get('flag',True),kw.get('sun',False))
    return '<g transform="translate(54 53) scale(.0875) translate(-560 -625)">%s</g>'%s

def base(**o):
    d=dict(glow1=H(.93,.14,92),door=H(.3,.07,50),ground=H(.82,.05,90),palm=H(.56,.14,140),palm2=H(.5,.13,145),trunk=H(.45,.08,55),bird=H(.25,.04,260),
           tent_front=H(.82,.17,72),tent_side=H(.74,.16,62),seam=H(.3,.06,55),fold=H(.64,.14,55),win=H(.9,.1,85),star=H(.4,.08,55),rope=H(.3,.05,55),stake=H(.45,.07,50),
           log=H(.35,.06,50),flame=H(.72,.2,45),flame2=H(.92,.16,95),pole=H(.8,.01,250),sun=H(.88,.14,80),moon=H(.95,.06,95))
    d.update(o);return d
MODELS={}
# 1: الشارة الكوبالتية
MODELS['m1a']=base(ring=H(.38,.15,262),bandstar=H(.96,.03,90),disc=H(.96,.025,88),textcol=H(.33,.14,262),mark=H(.38,.15,262),markop='.09')
MODELS['m1b']=base(ring=H(.36,.09,150),bandstar=H(.96,.03,100),disc=H(.96,.03,100),textcol=H(.3,.08,150),mark=H(.36,.09,150),markop='.1',palm=H(.5,.12,130),palm2=H(.44,.11,135))
# 2: الفجر
MODELS['m2a']=base(ring=H(.78,.13,82),bandstar=H(.3,.1,265),disc='url(#UIDsky)',bgdef='<linearGradient id="UIDsky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="%s"/><stop offset=".55" stop-color="%s"/><stop offset="1" stop-color="%s"/></linearGradient>'%(H(.28,.11,268),H(.5,.13,240),H(.86,.12,80)),
    textcol=H(.97,.02,90),palm=H(.22,.06,255),palm2=H(.2,.06,255),trunk=H(.2,.05,255),bird=H(.97,.02,90),ground=H(.24,.07,255),mark=H(.97,.02,90),markop='.07',sun=H(.9,.14,85),tent_front=H(.97,.02,90),tent_side=H(.86,.04,230),fold=H(.7,.06,240),win=H(.82,.15,75),star=H(.3,.1,265),seam=H(.25,.08,262),door=H(.22,.08,265),rope=H(.97,.02,90),stake=H(.35,.04,250))
MODELS['m2b']=dict(MODELS['m2a'],ring=H(.62,.17,45),bandstar=H(.95,.04,85),bgdef='<linearGradient id="UIDsky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="%s"/><stop offset=".5" stop-color="%s"/><stop offset="1" stop-color="%s"/></linearGradient>'%(H(.3,.14,320),H(.55,.19,20),H(.87,.14,75)),sun=H(.93,.12,95),tent_side=H(.86,.05,30),fold=H(.72,.07,30),palm=H(.22,.07,320),palm2=H(.2,.07,320),trunk=H(.2,.06,320),ground=H(.25,.08,320),door=H(.22,.09,320))
# 3: الليل
MODELS['m3a']=base(ring=H(.78,.13,82),bandstar=H(.22,.08,262),disc=H(.2,.07,262),textcol=H(.96,.03,90),mark=H(.78,.13,82),markop='.1',palm=H(.34,.08,200),palm2=H(.3,.07,205),trunk=H(.28,.05,200),ground=H(.3,.06,262),bird=H(.9,.03,90),pole=H(.85,.01,250),moon=H(.95,.07,95))
MODELS['m3b']=dict(MODELS['m3a'],ring=H(.72,.12,160),bandstar=H(.2,.06,185),disc=H(.19,.05,185),mark=H(.72,.12,160),palm=H(.4,.1,150),palm2=H(.35,.09,155),ground=H(.28,.05,185),moon=H(.93,.07,110))
KW={'m1':dict(),'m2':dict(sun=True,flag=False),'m3':dict(stars=True)}
def badge_svg(k,text=True):
    P=MODELS[k];return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1000 1000">%s</svg>'%badge(P,k,text,**KW[k[:2]])
if __name__=='__main__':
    d=os.path.dirname(os.path.abspath(__file__))
    for k in MODELS:open(os.path.join(d,k+'.svg'),'w',encoding='utf-8').write(badge_svg(k))
    print(len(MODELS))
