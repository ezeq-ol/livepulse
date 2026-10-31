"""Gera o feed público a partir dos metadados reais do APK, sem publicar nada."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
from urllib.parse import urlsplit

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--apk',required=True,type=Path)
    p.add_argument('--apk-url',required=True)
    p.add_argument('--output',type=Path,default=Path('updates.json'))
    p.add_argument('--notes',default='Melhorias no LivePulse.')
    p.add_argument('--aapt2',type=Path)
    a=p.parse_args()
    url=urlsplit(a.apk_url)
    if url.scheme!='https' or not url.hostname or url.username or url.password:
        p.error('Use uma URL HTTPS pública do APK, sem usuário ou senha.')
    if not a.apk.is_file() or not 0<a.apk.stat().st_size<=150*1024*1024:
        p.error('Informe um APK existente de até 150 MB.')
    if not a.aapt2:
        sdk=Path(os.environ.get('ANDROID_SDK_ROOT',str(Path(os.environ.get('LOCALAPPDATA',''))/'Android'/'Sdk')))
        choices=sorted(sdk.glob('build-tools/*/aapt2.exe'),key=lambda x:tuple(int(n) for n in re.findall(r'\d+',x.parent.name)))
        if not choices:p.error('Não foi encontrado aapt2. Use --aapt2 com o caminho do Android SDK.')
        a.aapt2=choices[-1]
    badging=subprocess.check_output([str(a.aapt2),'dump','badging',str(a.apk)],encoding='utf-8')
    package=re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'",badging)
    sdk=re.search(r"sdkVersion:'(\d+)'",badging)
    if not package or package[1]!='com.livepulse.app':p.error('O APK não é do LivePulse.')
    digest=hashlib.sha256()
    with a.apk.open('rb') as f:
        for chunk in iter(lambda:f.read(1024*1024),b''):digest.update(chunk)
    result=dict(schemaVersion=1,packageName=package[1],versionCode=int(package[2]),versionName=package[3],apkUrl=a.apk_url,sha256=digest.hexdigest(),sizeBytes=a.apk.stat().st_size,minSdk=int(sdk[1]) if sdk else 26,releaseNotes=a.notes)
    a.output.parent.mkdir(parents=True,exist_ok=True)
    a.output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(f'Arquivo criado: {a.output}. Nada foi publicado.')

if __name__=='__main__':main()
