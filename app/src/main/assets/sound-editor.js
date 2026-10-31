'use strict';
// A draft belongs to one explicit channel/event; selecting or testing never saves it.
const soundDrafts=new Map();let soundEditorBusy=false,soundPreview=null,soundSavedScope="";
const soundSave=document.createElement('button');soundSave.id='save-sound';soundSave.type='button';soundSave.className='primary';soundSave.textContent='Salvar mudanças';$('#sound .sound-actions').append(soundSave);
const soundDiscard=document.createElement('button');soundDiscard.id='discard-sound';soundDiscard.type='button';soundDiscard.className='quiet';soundDiscard.textContent='Descartar mudanças';$('#sound .sound-actions').append(soundDiscard);
const soundStatus=document.createElement('div');soundStatus.id='sound-draft-status';soundStatus.className='sound-draft-status';soundStatus.setAttribute('role','status');$('#sound .sound-panel').insertBefore(soundStatus,$('.upload'));
const soundSteps=document.createElement('p');soundSteps.className='sound-editor-steps';soundSteps.textContent='1. Selecione canal e evento  →  2. Escolha o áudio  →  3. Teste  →  4. Salve. Mudanças pendentes são descartadas ao fechar o app.';$('#sound .sound-panel').insertBefore(soundSteps,$('.sound-scope'));
$('#sound-channel').parentElement.firstChild.textContent='Canal que receberá o som';$('#sound-event').parentElement.firstChild.textContent='Tipo de evento';
$('#test').textContent='▷ Testar som';$('#test').className='quiet';$('#reset-sound').textContent='Usar som herdado / original';
const editorMime={mp3:'audio/mpeg',wav:'audio/wav',ogg:'audio/ogg',opus:'audio/ogg',m4a:'audio/mp4',aac:'audio/aac',flac:'audio/flac'};
function stopSoundPreview(){if(soundPreview){soundPreview.pause();soundPreview.src='';soundPreview=null;}}
function editorTarget(){const channel=state?.channels.find(c=>c.id===$('#sound-channel').value);return (channel?(channel.name||channel.slug):'Todos os canais')+' · '+($('#sound-event').selectedOptions[0]?.textContent||'Todos os eventos');}
function renderSoundEditor(){
 if(!state)return;const draft=soundDrafts.get(scope22());
 soundStatus.classList.toggle('has-draft',!!draft);soundStatus.classList.toggle('tested',!!draft?.tested);
 const message=draft?(draft.remove?'Alteração pendente: usar o próximo som disponível.':(draft.tested?'Áudio testado: ':'Arquivo escolhido: ')+draft.name)+' Toque em Salvar mudanças para aplicar.':'Som atual salvo. Escolher ou testar outro arquivo não altera os alertas.';
 soundStatus.textContent=editorTarget()+' — '+message;
 if(draft)$('#sound-name').textContent=draft.remove?'Usar som herdado / original':draft.name;
 const saved=!draft&&soundSavedScope===scope22();soundSave.disabled=!draft||soundEditorBusy;soundSave.textContent=soundEditorBusy?'Atualizando…':saved?'✓ Alterações salvas com sucesso':'Salvar mudanças';soundSave.classList.toggle('sound-save-success',saved);soundDiscard.disabled=!draft||soundEditorBusy;if(saved)soundStatus.textContent=editorTarget()+' — Alterações salvas com sucesso. Estúdio atualizado; você já pode escolher outro canal ou evento.';
 for(const id of ['sound-channel','sound-event','audio-file','test','reset-sound'])$('#'+id).disabled=soundEditorBusy;
}
const beforeSoundEditorRender=render;render=function(){beforeSoundEditorRender();renderSoundEditor();};
for(const id of ['sound-channel','sound-event'])$('#'+id).addEventListener('change',()=>{stopSoundPreview();soundSavedScope='';renderSoundEditor();});
$('#audio-file').onchange=async event=>{
 const file=event.target.files[0];if(!file)return;const scope=scope22();
 if(file.size<1||file.size>5*1024*1024){toast('Escolha um áudio entre 1 byte e 5 MB.',true);event.target.value='';return;}
 const mime=file.type.startsWith('audio/')?file.type:editorMime[file.name.split('.').pop().toLowerCase()];if(!mime){toast('Escolha um arquivo de áudio MP3, WAV, OGG, M4A, AAC ou FLAC.',true);event.target.value='';return;}
 stopSoundPreview();soundSavedScope='';soundEditorBusy=true;renderSoundEditor();
 try{const data=await new Promise((resolve,reject)=>{const reader=new FileReader();reader.onload=()=>resolve(reader.result);reader.onerror=()=>reject(Error('Não foi possível ler o áudio.'));reader.readAsDataURL(new Blob([file],{type:mime}));});soundDrafts.set(scope,{name:file.name,data,tested:false});toast('Arquivo escolhido. Teste e toque em Salvar mudanças para aplicar.');}catch(error){toast(error.message,true);}finally{soundEditorBusy=false;event.target.value='';renderSoundEditor();}
};
$('#test').onclick=async()=>{
 if(soundEditorBusy)return;const scope=scope22(),draft=soundDrafts.get(scope);stopSoundPreview();soundEditorBusy=true;renderSoundEditor();
 try{let data=draft&&!draft.remove?draft.data:null;if(!data){const preview=await api('/api/preview-sound','POST',{scope,excludeSelected:!!draft?.remove});data=preview.data||'/pulse.wav';}soundPreview=new Audio(data);soundPreview.volume=Number($('#volume').value)/100;await soundPreview.play();if(draft)soundDrafts.set(scope,{...draft,tested:true});toast(draft?'Prévia reproduzida. Salve para aplicar aos próximos alertas.':'Som salvo reproduzido.',false,'Teste de som');}catch(error){toast('Não foi possível reproduzir este áudio. Confira o volume ou escolha outro formato.',true);}finally{soundEditorBusy=false;renderSoundEditor();}
};
soundSave.onclick=async()=>{
 if(soundEditorBusy)return;const scope=scope22(),draft=soundDrafts.get(scope);if(!draft)return;const target=editorTarget();stopSoundPreview();soundEditorBusy=true;renderSoundEditor();
 let persisted=false;try{await api('/api/sound-rules',draft.remove?'DELETE':'POST',draft.remove?{scope}:{scope,name:draft.name,data:draft.data});persisted=true;soundDrafts.delete(scope);$('#audio-file').value='';await refresh();soundSavedScope=scope;toast('Alterações salvas com sucesso. Estúdio atualizado para '+target+'.');}catch(error){toast(persisted?'O som foi salvo, mas não foi possível atualizar o Estúdio. Tente abrir a tela novamente.':'Não foi possível salvar: '+error.message,true);}finally{soundEditorBusy=false;effectiveSound22();renderSoundEditor();}
};
soundDiscard.onclick=()=>{stopSoundPreview();soundDrafts.delete(scope22());effectiveSound22();renderSoundEditor();};
$('#reset-sound').onclick=()=>{stopSoundPreview();soundSavedScope='';soundDrafts.set(scope22(),{remove:true,tested:false});renderSoundEditor();};
const soundEditorView=view;view=function(id){if(id!=='sound')stopSoundPreview();soundEditorView(id);if(id==='sound')renderSoundEditor();};
if(state)renderSoundEditor();
