const isAndroid=typeof window.LivePulseNative!=='undefined';
let settingsDirty=false;
let state,filter='all',audioData=null,audioName='',audioContext,activeAudio,armed=false;
const $=s=>document.querySelector(s),names={twitch:'Twitch',kick:'Kick',youtube:'YouTube'},icons={twitch:'▣',kick:'K',youtube:'▶'},types={live:'AO VIVO',video:'VÍDEO',short:'SHORT',agendada:'AGENDADA',replay:'REPLAY'};
const escape=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
let bridgeSequence=0;const bridgeRequests=new Map();
window.LivePulseBridgeReply=(id,result)=>{const pending=bridgeRequests.get(id);if(!pending)return;bridgeRequests.delete(id);clearTimeout(pending.timer);try{pending.resolve(JSON.parse(result));}catch(error){pending.reject(Error('Resposta local inválida.'));}};
function nativeRequest(url,method,data){if(typeof LivePulseNative.requestAsync!=='function')return Promise.resolve(JSON.parse(LivePulseNative.request(url,method,JSON.stringify(data||{}))));return new Promise((resolve,reject)=>{const id='r'+(++bridgeSequence);const timer=setTimeout(()=>{bridgeRequests.delete(id);reject(Error('A operação local demorou demais. Tente novamente.'));},20000);bridgeRequests.set(id,{resolve,reject,timer});try{LivePulseNative.requestAsync(id,url,method,JSON.stringify(data||{}));}catch(error){clearTimeout(timer);bridgeRequests.delete(id);reject(error);}});}
async function api(url,method='GET',data){if(isAndroid){const result=await nativeRequest(url,method,data);if(!result.ok)throw Error(result.error);return result.data;}const r=await fetch(url,{method,headers:{'Content-Type':'application/json'},body:data?JSON.stringify(data):undefined});const b=await r.json();if(!r.ok)throw Error(b.error||'Falha na operação.');return b;}
function toast(msg,error=false,title=''){const el=document.createElement('div');el.className='toast'+(error?' error':'');if(title){const b=document.createElement('b');b.textContent=title;el.append(b);}el.append(document.createTextNode(msg));$('#toasts').append(el);setTimeout(()=>el.remove(),9000);}
function view(id){document.querySelectorAll('.view').forEach(e=>e.classList.toggle('hidden',e.id!==id));document.querySelectorAll('nav button').forEach(e=>e.classList.toggle('selected',e.dataset.view===id));$('#breadcrumb').textContent={dashboard:'Visão geral',history:'Notificações',sound:'Estúdio de som',integrations:'Integrações'}[id];}
document.querySelectorAll('[data-view]').forEach(b=>b.onclick=()=>view(b.dataset.view));
function render(){
  renderMonitor();
  $('#stat-channels').textContent=state.channels.filter(c=>!c.paused).length;$('#stat-live').textContent=state.channels.filter(c=>!c.paused&&!c.error&&c.live).length;$('#stat-alerts').textContent=state.events.length;$('#nav-count').textContent=state.events.length;
  $('#poll-time').textContent=state.lastPoll?'Atualizado às '+new Date(state.lastPoll).toLocaleTimeString('pt-BR',{hour:'2-digit',minute:'2-digit'}):'Aguardando primeira consulta';
  renderCustomIntegrations();
  const channels=state.channels.filter(c=>filter==='all'||c.platform===filter);
  const channelMarkup=channels.length?channels.map(c=>`<article class="channel"><div class="channel-top"><div class="avatar ${c.platform}-bg">${icons[c.platform]||'⌘'}</div><div><h3>${escape(c.name||c.slug)}</h3><small>${escape(platformName(c))} · ${escape(c.slug)}</small></div><span class="badge ${!c.paused&&!c.error&&c.live?'live':''}">${c.paused?'PAUSADO':c.error?'CONFIGURAR':c.live?'● AO VIVO':c.checkedAt?'OFFLINE':'AGUARDANDO'}</span></div>${c.error?`<div class="channel-error">${escape(c.error)}</div>`:''}${c.platform==='youtube'?`<div class="checks">${Object.entries(types).map(([k,v])=>`<label><input type="checkbox" data-channel="${c.id}" data-type="${k}" ${(c.filters||[]).includes(k)?'checked':''}> ${v}</label>`).join('')}</div>`:''}<div class="channel-actions"><button data-pause="${c.id}">${c.paused?'▷ Retomar':'Ⅱ Pausar'}</button><a class="quiet" href="${escape(c.url)}" target="_blank" rel="noreferrer">Abrir canal ↗</a><button data-remove="${c.id}">Remover</button></div></article>`).join(''):'<div class="empty">◎<strong>Seu próximo favorito começa aqui</strong>Cole o link de um canal acima. Os alertas aparecerão neste espaço.</div>';
  if($('#channels').dataset.baseMarkup!==channelMarkup){$('#channels').innerHTML=channelMarkup;$('#channels').dataset.baseMarkup=channelMarkup;}
  $('#events').innerHTML=state.events.length?state.events.map(e=>`<article class="event"><div class="avatar ${e.platform}-bg">${icons[e.platform]||'⌘'}</div><div><small class="eyebrow">${types[e.type]||e.type} · ${escape(e.channel)}</small><h3>${escape(e.title)}</h3><time>${new Date(e.time).toLocaleString('pt-BR')}</time>${e.classification?`<p>${escape(e.classification)}</p>`:''}</div><a href="${escape(e.url)}" target="_blank" rel="noreferrer">Assistir ↗</a></article>`).join(''):'<div class="empty">◷<strong>Tudo tranquilo por aqui</strong>Novas notificações vão aparecer automaticamente.</div>';
  for(const p of ['twitch','kick','youtube'])$(`#${p}-status`).textContent=(p==='youtube'?state.configured.youtubeKey:state.configured[p+'Id']&&state.configured[p+'Secret'])?'● Configurado':'○ Aguardando credenciais';
  if(!settingsDirty)$('#settings-form select').value=state.settings.interval;
}
async function refresh(){state=await api('/api/state');render();}
function arm(){armed=true;if(!audioContext)audioContext=new AudioContext();return audioContext.resume();}
async function play(){
  if(isAndroid){LivePulseNative.previewSound();return;}
  if(!armed){toast('Ative os alertas e som para ouvir as próximas notificações.');return;}
  if(activeAudio){activeAudio.pause();activeAudio=null;}
  const volume=Number($('#volume').value)/100;
  if(audioData){activeAudio=new Audio(audioData);activeAudio.volume=volume;await activeAudio.play();return;}
  await audioContext.resume();const now=audioContext.currentTime;
  [660,880,1100].forEach((freq,i)=>{const osc=audioContext.createOscillator(),gain=audioContext.createGain();osc.type='sine';osc.frequency.value=freq;gain.gain.setValueAtTime(0,now+i*.13);gain.gain.linearRampToValueAtTime(volume*.2,now+i*.13+.015);gain.gain.exponentialRampToValueAtTime(.0001,now+i*.13+.55);osc.connect(gain);gain.connect(audioContext.destination);osc.start(now+i*.13);osc.stop(now+i*.13+.6);});
}
$('#enable').onclick=async()=>{if(isAndroid){LivePulseNative.enable();setTimeout(()=>refresh().catch(()=>{}),1200);return;}try{await arm();if('Notification'in window&&Notification.permission==='default')await Notification.requestPermission();$('#enable').textContent='● Alertas e som ativados';toast('Áudio liberado. Mantenha esta janela aberta.');await play();}catch(e){toast(e.message,true);}};
$('#test').onclick=async()=>{if(isAndroid){LivePulseNative.testNotification();toast('Teste enviado. Se não ouvir, confira o volume e o modo Não perturbe.',false,'Notificação teste');return;}try{await arm();await play();toast('Este é o som das suas próximas notificações.','', '♬ Notificação teste');if('Notification'in window&&Notification.permission==='granted')new Notification('LivePulse • Notificação teste',{body:'Seu som de notificação está configurado.'});}catch(e){toast('Não foi possível reproduzir este áudio. Escolha outro formato.',true);}};
$('#focus-link').onclick=()=>{$('#channel-url').focus();$('#channel-url').scrollIntoView({behavior:'smooth',block:'center'});};
$('#add-form').onsubmit=async e=>{e.preventDefault();const btn=e.target.querySelector('button');btn.disabled=true;try{state=await api('/api/channels','POST',{url:$('#channel-url').value.trim(),integrationId:$('#channel-integration').value,slug:$('#custom-slug').value.trim()});$('#channel-url').value='';render();toast('Canal adicionado. A primeira consulta estabelece o histórico; lives já ativas também geram alerta.');}catch(e){toast(e.message,true);}finally{btn.disabled=false;}};
$('#check').onclick=async()=>{try{await api('/api/check','POST');toast('Consultando as plataformas…');}catch(e){toast(e.message,true);}};
document.querySelectorAll('[data-filter]').forEach(b=>b.onclick=()=>{filter=b.dataset.filter;document.querySelectorAll('[data-filter]').forEach(x=>x.classList.toggle('active',x===b));render();});
$('#channels').onclick=async e=>{const pause=e.target.dataset.pause,remove=e.target.dataset.remove;try{if(pause){state=await api('/api/channels/'+pause,'PATCH',{paused:!state.channels.find(c=>c.id===pause).paused});render();}if(remove&&confirm('Remover este canal do monitoramento?')){state=await api('/api/channels/'+remove,'DELETE');render();}}catch(e){toast(e.message,true);}};
$('#channels').onchange=async e=>{if(!e.target.dataset.channel)return;const id=e.target.dataset.channel,checked=[...document.querySelectorAll(`input[data-channel="${id}"]:checked`)].map(x=>x.dataset.type);try{state=await api('/api/channels/'+id,'PATCH',{filters:checked});render();}catch(e){toast(e.message,true);}};
$('#settings-form').addEventListener('input',()=>{settingsDirty=true;});
$('#settings-form').onsubmit=async e=>{e.preventDefault();try{state=await api('/api/settings','POST',Object.fromEntries(new FormData(e.target)));e.target.reset();settingsDirty=false;render();toast('Integrações salvas. Use Verificar agora para consultar os canais.');}catch(e){toast(e.message,true);}};
$('#clear-history').onclick=async()=>{if(!confirm('Limpar todo o histórico de notificações?'))return;try{state=await api('/api/history','DELETE');render();}catch(e){toast(e.message,true);}};
$('#volume').value=localStorage.getItem('livepulse-volume')||75;
const updateVolume=()=>{$('#volume-value').textContent=$('#volume').value+'%';localStorage.setItem('livepulse-volume',$('#volume').value);if(isAndroid)LivePulseNative.setVolume(Number($('#volume').value));};$('#volume').oninput=updateVolume;if(!isAndroid)updateVolume();
$('#audio-file').onchange=async e=>{const f=e.target.files[0];if(!f)return;if(f.size>5*1024*1024){toast('Escolha um arquivo de até 5 MB.',true);return;}try{const data=await new Promise((resolve,reject)=>{const reader=new FileReader();reader.onload=()=>resolve(reader.result);reader.onerror=reject;reader.readAsDataURL(f);});await api('/api/sound','POST',{name:f.name,data});audioData=data;audioName=f.name;$('#sound-name').textContent=audioName;toast('Som personalizado salvo. Clique em Testar notificação para ouvir.');}catch(e){toast(e.message||'Erro ao importar áudio.',true);}};
$('#reset-sound').onclick=async()=>{try{await api('/api/sound','DELETE');audioData=null;$('#sound-name').textContent='Pulse • som original';toast('Som original restaurado.');}catch(e){toast(e.message,true);}};
const stream=isAndroid?{addEventListener(){}}:new EventSource('/api/events');stream.addEventListener('state',e=>{state=JSON.parse(e.data);render();});
let queue=Promise.resolve();stream.addEventListener('notification',e=>{const n=JSON.parse(e.data);toast(n.title,false,`${types[n.type]} · ${n.channel}`);queue=queue.then(()=>play(n)).then(()=>new Promise(r=>setTimeout(r,1000))).catch(()=>toast('O navegador bloqueou o áudio. Ative os alertas novamente.',true));if('Notification'in window&&Notification.permission==='granted'){const notification=new Notification(`${n.channel} · ${types[n.type]}`,{body:n.title,tag:n.id,silent:true});notification.onclick=()=>window.open(n.url,'_blank','noopener');}refresh().catch(()=>{});});
stream.onerror=()=>{$('#poll-time').textContent='Reconectando ao servidor…';};
Promise.all([refresh().then(()=>{if(isAndroid){$('#volume').value=state.settings.volume??75;$('#volume-value').textContent=$('#volume').value+'%';}}),api('/api/sound').then(s=>{audioData=s.data||null;audioName=s.name||'';if(audioName)$('#sound-name').textContent=audioName;})]).catch(e=>toast('Não foi possível conectar ao servidor: '+e.message,true));
setInterval(()=>refresh().catch(()=>{}),isAndroid?4000:30000);

function platformName(c){return state.integrations?.find(i=>i.id===c.integrationId)?.name||c.platformName||names[c.platform]||'Personalizada';}
function renderCustomIntegrations(){
 const integrations=state.integrations||[],select=$('#channel-integration'),selected=select.value;
 const signature=JSON.stringify(integrations.map(i=>[i.id,i.name]));if(select.dataset.signature!==signature){select.dataset.signature=signature;select.innerHTML='<option value="">Automática · Twitch, Kick e YouTube</option>'+integrations.map(i=>`<option value="${i.id}">${escape(i.name)}</option>`).join('');
 if(integrations.some(i=>i.id===selected))select.value=selected;}
 $('#custom-slug-label').classList.toggle('hidden',!select.value);$('#custom-slug').required=!!select.value;
 $('#custom-integrations').innerHTML=integrations.length?integrations.map(i=>`<article class="integration"><div class="platform-icon custom-bg">⌘</div><div><h2>${escape(i.name)}</h2><p>${escape(i.endpoint)}</p><p class="hint">${i.hasToken?'Token salvo no servidor':'Sem autenticação'} · Consulta GET / JSON</p><button class="quiet" data-edit-integration="${i.id}">Editar</button> <button class="quiet" data-delete-integration="${i.id}">Excluir</button></div></article>`).join(''):'<p class="hint">Nenhuma integração personalizada cadastrada ainda.</p>';
}
$('#channel-integration').onchange=()=>{const custom=!!$('#channel-integration').value;$('#custom-slug-label').classList.toggle('hidden',!custom);$('#custom-slug').required=custom;$('#channel-url').placeholder=custom?'https://plataforma.com/seu-canal':'Cole o link da Twitch, Kick ou YouTube…';};
$('#custom-integration-form').onsubmit=async e=>{e.preventDefault();const button=e.target.querySelector('[type="submit"]');button.disabled=true;try{state=await api('/api/integrations','POST',Object.fromEntries(new FormData(e.target)));e.target.reset();render();toast('Integração personalizada salva. Selecione-a no Seu radar para adicionar um canal.');}catch(err){toast(err.message,true);}finally{button.disabled=false;}};
$('#cancel-custom').onclick=()=>$('#custom-integration-form').reset();
$('#custom-integrations').onclick=async e=>{const edit=e.target.dataset.editIntegration,remove=e.target.dataset.deleteIntegration;
 if(edit){const i=state.integrations.find(i=>i.id===edit),form=$('#custom-integration-form');for(const key of ['id','name','endpoint','livePath','liveValue','idPath','titlePath','urlPath','avatarPath'])form.elements[key].value=i[key]||'';form.elements.bearer.value='';form.elements.bearer.placeholder=i.hasToken?'Deixe vazio para manter o token salvo':'Token de acesso';form.scrollIntoView({behavior:'smooth',block:'center'});}
 if(remove&&confirm('Excluir esta integração personalizada?')){try{state=await api('/api/integrations/'+remove,'DELETE');if($('#custom-integration-form').elements.id.value===remove)$('#custom-integration-form').reset();render();toast('Integração excluída.');}catch(err){toast(err.message,true);}}
};

function renderMonitor(){
 const enabled=isAndroid?state.settings.monitorEnabled:armed,active=isAndroid?state.monitorActive:armed;
 document.body.classList.toggle('radar-enabled',!!enabled);
 $('#monitor-title').textContent=enabled?(active?'Seu radar está ativo':'Radar em modo econômico'):'Seu radar está pronto';
 $('#monitor-description').textContent=enabled?(isAndroid?(active?'Consultas diretas neste aparelho. Toque em pausar quando quiser.':'O Android agenda as consultas em segundo plano, a partir de 15 minutos.'):'Alertas sonoros ativos enquanto esta janela estiver aberta.'):'Adicione um canal e ative os alertas para começar.';
 $('#pause-monitor').classList.toggle('hidden',!isAndroid||!enabled);
 $('#enable').innerHTML=svgIcon('bell')+(enabled?'Radar ativado':'Ativar radar e som');
 $('#radar-signal').textContent=state.channels.some(c=>c.live&&!c.error&&!c.paused)?'LIVE NO SEU RADAR':'PRONTO PARA CONECTAR';
}
$('#pause-monitor').onclick=()=>{if(isAndroid){LivePulseNative.stopMonitor();setTimeout(()=>refresh().catch(()=>{}),300);}};
document.addEventListener('click',event=>{const a=event.target.closest('a[href]');if(isAndroid&&a&&/^https:\/\//.test(a.href)&&!a.href.startsWith('https://app.livepulse.local')){event.preventDefault();LivePulseNative.openLink(a.href);}});
function svgIcon(name){
 const paths={dashboard:'<rect x="3" y="3" width="7" height="7" rx="2"/><rect x="14" y="3" width="7" height="7" rx="2"/><rect x="3" y="14" width="7" height="7" rx="2"/><rect x="14" y="14" width="7" height="7" rx="2"/>',history:'<path d="M3 11a9 9 0 1 1 3 8M3 4v7h7M12 7v5l3 2"/>',sound:'<path d="M9 18V5l12-2v13M9 9l12-2"/><ellipse cx="6" cy="18" rx="3" ry="3"/><ellipse cx="18" cy="16" rx="3" ry="3"/>',integrations:'<rect x="3" y="3" width="7" height="7" rx="2"/><rect x="14" y="3" width="7" height="7" rx="2"/><rect x="3" y="14" width="7" height="7" rx="2"/><path d="M17.5 14v7M14 17.5h7"/>',bell:'<path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4"/>',check:'<path d="M20 7a9 9 0 1 0 1 9M20 3v5h-5M8 12l3 3 6-7"/>',play:'<path d="M8 4l13 8-13 8z"/>',plus:'<path d="M12 5v14M5 12h14"/>'};
 return '<svg class="ui-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">'+(paths[name]||paths.bell)+'</svg>';
}
document.querySelectorAll('nav [data-view]').forEach(button=>{for(const node of [...button.childNodes])if(node.nodeType===Node.TEXT_NODE)node.remove();button.insertAdjacentHTML('afterbegin',svgIcon(button.dataset.view));});
$('#enable').innerHTML=svgIcon('bell')+'Ativar radar e som';
$('#check').innerHTML=svgIcon('check')+'Verificar agora';
$('#test').innerHTML=svgIcon('play')+'Testar notificação';
$('#focus-link').innerHTML=svgIcon('plus')+'Monitorar um canal';
if(isAndroid){document.documentElement.classList.add('android-app');const prefs=document.createElement('button');prefs.className='quiet';prefs.type='button';prefs.textContent='Permissões de notificação do Android';prefs.onclick=()=>LivePulseNative.notificationSettings();$('#sound .sound-panel').append(prefs);}
