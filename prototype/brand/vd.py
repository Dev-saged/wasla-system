# يحوّل مشهد الشارة المختارة (m2a) إلى VectorDrawable لأندرويد + أيقونات قديمة
import re,os,sys,xml.etree.ElementTree as ET
sys.path.insert(0,os.path.dirname(os.path.abspath(__file__)))
import gen
RES='/home/claude/wasla-system/android/app/src/main/res'
P=gen.MODELS['m2a'];NS='{http://www.w3.org/2000/svg}'
def num(s):return [float(x) for x in re.findall(r'-?\d*\.?\d+',s)]
def circ(cx,cy,rx,ry=None):
    ry=ry or rx
    return 'M%g,%g a%g,%g 0 1,0 %g,0 a%g,%g 0 1,0 %g,0Z'%(cx-rx,cy,rx,ry,2*rx,rx,ry,-2*rx)
def bbox(d):
    n=num(re.sub(r'[A-Za-z]',' ',d));xs=n[0::2];ys=n[1::2];return min(xs),min(ys),max(xs),max(ys)
def attrs(e,fill_in):
    a=[];f=e.get('fill',fill_in);op=e.get('opacity')
    st=e.get('stroke')
    if f and f!='none':
        if f.startswith('url('):a.append(('grad',f[5:-1]))
        else:a.append(('fillColor',f))
        if op:a.append(('fillAlpha',op))
    if st and st!='none':
        a.append(('strokeColor',st));a.append(('strokeWidth',e.get('stroke-width','1')))
        if e.get('stroke-linecap'):a.append(('strokeLineCap',e.get('stroke-linecap')))
        if e.get('stroke-linejoin'):a.append(('strokeLineJoin',e.get('stroke-linejoin')))
        if op and not (f and f!='none'):a.append(('strokeAlpha',op))
    if e.get('fill-rule')=='evenodd':a.append(('fillType','evenOdd'))
    return a
def aname(k):return 'android:'+k
def render(frag,grads,fill_in=None,ind=2):
    out=''
    for e in frag:
        t=e.tag.replace(NS,'');pad=' '*ind
        if t in('defs',):continue
        if t=='g':
            tr=e.get('transform');inner=render(e,grads,e.get('fill',fill_in),ind+2)
            if tr:
                m=re.search(r'translate\(([-\d. ]+)\)',tr);s=re.search(r'scale\(([-\d. ]+)\)',tr);r=re.search(r'rotate\(([-\d.]+)\)',tr)
                g='%s<group'%pad
                if m:v=num(m.group(1));g+=' android:translateX="%g" android:translateY="%g"'%(v[0],v[1])
                if s:v=num(s.group(1));g+=' android:scaleX="%g" android:scaleY="%g"'%(v[0],v[-1])
                out+=g+'>\n'+inner+'%s</group>\n'%pad
            else:out+=inner
            continue
        if t=='path':d=e.get('d')
        elif t=='circle':d=circ(float(e.get('cx')),float(e.get('cy')),float(e.get('r')))
        elif t=='ellipse':d=circ(float(e.get('cx')),float(e.get('cy')),float(e.get('rx')),float(e.get('ry')))
        else:continue
        a=attrs(e,fill_in);tr=e.get('transform');pre=post=''
        if tr:
            m=re.search(r'translate\(([-\d. ]+)\)',tr);s=re.search(r'scale\(([-\d. ]+)\)',tr)
            g='%s<group'%pad
            if m:v=num(m.group(1));g+=' android:translateX="%g" android:translateY="%g"'%(v[0],v[1])
            if s:v=num(s.group(1));g+=' android:scaleX="%g" android:scaleY="%g"'%(v[0],v[-1])
            pre=g+'>\n';post='%s</group>\n'%pad
        gr=[x for x in a if x[0]=='grad']
        body='%s<path android:pathData="%s"'%(pad,d)
        for k,v in a:
            if k!='grad':body+='\n%s    %s="%s"'%(pad,aname(k),v)
        if gr:
            x0,y0,x1,y1=bbox(d);G=grads[gr[0][1]]
            cx=x0+G['cx']*(x1-x0);cy=y0+G['cy']*(y1-y0);r=G['r']*max(x1-x0,y1-y0)*.8
            body+='>\n%s  <aapt:attr name="android:fillColor">\n%s    <gradient android:type="radial" android:centerX="%g" android:centerY="%g" android:gradientRadius="%g">\n'%(pad,pad,cx,cy,r)
            for off,col in G['stops']:body+='%s      <item android:offset="%s" android:color="%s" />\n'%(pad,off,col)
            body+='%s    </gradient>\n%s  </aapt:attr>\n%s</path>\n'%(pad,pad,pad)
        else:body+=' />\n'
        out+=pre+body+post
    return out
def parse(frag):
    root=ET.fromstring('<svg xmlns="http://www.w3.org/2000/svg">'+frag+'</svg>')
    grads={}
    for g in root.iter(NS+'radialGradient'):
        grads[g.get('id')]=dict(cx=float(g.get('cx')),cy=float(g.get('cy')),r=float(g.get('r')),stops=[(s.get('offset'),s.get('stop-color')) for s in g])
    return root,grads
HEAD='<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    xmlns:aapt="http://schemas.android.com/aapt"\n    android:width="%gdp"\n    android:height="%gdp"\n    android:viewportWidth="%g"\n    android:viewportHeight="%g">\n'
def write(name,w,h,vw,vh,body):
    open(os.path.join(RES,'drawable',name+'.xml'),'w',encoding='utf-8').write(HEAD%(w,h,vw,vh)+body+'</vector>\n')
# المشهد بلا شمس
frag=gen.scene(P,'x',with_flag=False,with_sun=False)
root,grads=parse(frag)
scene=render(root,grads,None,4)
# ملف الافتتاح: قصّ 340..872
write('opening_scene',360,191.5,1000,532,'  <group android:translateY="-340">\n'+scene+'  </group>\n')
sun='<circle cx="560" cy="700" r="270" fill="%s" opacity=".25"/><circle cx="560" cy="700" r="215" fill="%s" opacity=".95"/>'%(P['sun'],P['sun'])
r2,g2=parse(sun);write('opening_sun',360,191.5,1000,532,'  <group android:translateY="-340">\n'+render(r2,g2,None,4)+'  </group>\n')
# أيقونة تكيفية: أمامية (المشهد + الشمس داخل المنطقة الآمنة)
fr=gen.scene(P,'x',with_flag=False,with_sun=True);rt,gr=parse(fr)
s=.0875
write('ic_launcher_foreground',108,108,108,108,'  <group android:translateX="%g" android:translateY="%g" android:scaleX="%g" android:scaleY="%g">\n'%(54-560*s,53-625*s,s,s)+render(rt,gr,None,4)+'  </group>\n')
# الخلفية: تدرج السماء
sky=[(0,gen.H(.28,.11,268)),(.55,gen.H(.5,.13,240)),(1,gen.H(.86,.12,80))]
bg='  <path android:pathData="M0,0h108v108h-108z">\n    <aapt:attr name="android:fillColor">\n      <gradient android:type="linear" android:startX="54" android:startY="0" android:endX="54" android:endY="108">\n'+''.join('        <item android:offset="%g" android:color="%s" />\n'%x for x in sky)+'      </gradient>\n    </aapt:attr>\n  </path>\n'
write('ic_launcher_background',108,108,108,108,bg)
# أحادية: خيمة بباب + أرض
mono='  <group android:translateX="%g" android:translateY="%g" android:scaleX="%g" android:scaleY="%g">\n    <path android:fillColor="#FF000000" android:fillType="evenOdd" android:pathData="M430,545 L700,575 L868,805 L618,825 L238,815Z M430,598 L352,815 Q430,832 508,820Z" />\n    <path android:pathData="M215,848 H905" android:strokeColor="#FF000000" android:strokeWidth="22" android:strokeLineCap="round" />\n  </group>\n'%(54-560*s,53-625*s,s,s)
write('ic_launcher_monochrome',108,108,108,108,mono)
print('ok',sky)
