"""Resolve official Google/Maven artifacts for the standalone SDK build (no Gradle)."""
from pathlib import Path
import urllib.request, urllib.error, xml.etree.ElementTree as E, zipfile, json, re, hashlib
ROOT=Path(__file__).resolve().parent
CACHE=ROOT/'dependencies';CACHE.mkdir(exist_ok=True)
LOCK=json.loads((ROOT/'dependencies.lock.json').read_text(encoding='utf-8')) if (ROOT/'dependencies.lock.json').exists() else []
NS={'m':'http://maven.apache.org/POM/4.0.0'}
repos=['https://dl.google.com/dl/android/maven2/','https://repo.maven.apache.org/maven2/']
def download(g,a,v,ext):
    target=CACHE/(a+'-'+v+'.'+ext)
    if target.exists():return target
    name=g.replace('.','/')+'/'+a+'/'+v+'/'+a+'-'+v+'.'+ext
    last=None
    for repo in repos:
        try:
            with urllib.request.urlopen(repo+name,timeout=30) as r:target.write_bytes(r.read())
            return target
        except urllib.error.HTTPError as ex:
            if ex.code!=404:raise
            last=ex
    raise last
def version(v):return tuple(int(x) if x.isdigit() else 0 for x in re.split('[.-]',v))
chosen={};queue=[('com.google.android.gms','play-services-auth','21.5.0')]
while queue:
    g,a,v=queue.pop(0);v=v.strip('[]');key=(g,a)
    if key in chosen and version(chosen[key][0])>=version(v):continue
    pom=E.parse(download(g,a,v,'pom')).getroot()
    def text(el,path,default=''):
        item=el.find(path,NS);return item.text.strip() if item is not None and item.text else default
    packaging=text(pom,'m:packaging','jar');chosen[key]=(v,packaging)
    properties={x.tag.split('}')[-1]:x.text for x in list(pom.find('m:properties',NS) or [])}
    for dep in pom.findall('m:dependencies/m:dependency',NS):
        if text(dep,'m:scope') in ('test','provided','system') or text(dep,'m:optional')=='true':continue
        dg,da,dv=[text(dep,'m:'+k) for k in ('groupId','artifactId','version')]
        if dv.startswith('${'):dv=properties.get(dv[2:-1],dv)
        if not dv or '${' in dv:raise RuntimeError('Unresolved dependency '+str((dg,da,dv)))
        queue.append((dg,da,dv))
inventory=[]
for (g,a),(v,ext) in sorted(chosen.items()):
    if ext not in ('aar','jar'):continue
    f=download(g,a,v,ext)
    expected=next((i.get('sha256') for i in LOCK if i['group']==g and i['artifact']==a and i['version']==v),None)
    if expected and hashlib.sha256(f.read_bytes()).hexdigest()!=expected:raise RuntimeError('Dependency checksum mismatch: '+f.name)
    entry={'group':g,'artifact':a,'version':v,'file':f.name,'type':ext}
    if ext=='aar':
        folder=CACHE/(a+'-'+v);folder.mkdir(exist_ok=True)
        with zipfile.ZipFile(f) as z:z.extractall(folder)
        manifest=E.parse(folder/'AndroidManifest.xml').getroot();entry['package']=manifest.attrib.get('package','')
    inventory.append(entry)
(CACHE/'inventory.json').write_text(json.dumps(inventory,indent=2),encoding='utf-8')
print('Official Google authorization dependencies ready:',len(inventory),'artifacts')
