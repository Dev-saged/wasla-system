import re, sys, collections
src = open('index.html', encoding='utf-8').read().split('\n')
L = lambda a, b: '\n'.join(src[a-1:b])
out = []
# screens
scr = [(i+1, re.search(r'id="(screen-[^"]+)"', l).group(1)) for i, l in enumerate(src) if re.search(r'<section id="screen-', l)]
ends = [s[0] for s in scr[1:]] + [1726]
out.append('# Screens (markup line ranges)\n')
for (a, n), e in zip(scr, ends):
    seg = L(a, e-1)
    ids = re.findall(r'id="([^"]+)"', seg)
    out.append(f'- {n}: lines {a}-{e-1}; ids={len(ids)}')
# functions
fn = [(i+1, m.group(1)) for i, l in enumerate(src) for m in [re.match(r'\s*(?:async\s+)?function\s+(\w+)', l)] if m]
out.append('\n# Functions (name:line)\n')
out.append(', '.join(f'{n}:{a}' for a, n in fn))
# role gating
out.append('\n# Role gating lines\n')
for i, l in enumerate(src):
    if re.search(r"role\s*===?\s*'|isAdmin\(|isDelegate\(|isDataEntry\(|isBen\(|guardRole\(|canEdit|canDelete", l) and i > 1726:
        out.append(f'{i+1}: {l.strip()[:150]}')
open('docs/web-spec/INDEX.md', 'w', encoding='utf-8').write('\n'.join(out))
print(len(scr), 'screens', len(fn), 'functions')
