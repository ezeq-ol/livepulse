package com.livepulse.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.util.*;

public final class MainActivity extends Activity {
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private LocalApi api;
    private final java.util.concurrent.ExecutorService bridgeWorker=java.util.concurrent.Executors.newFixedThreadPool(2);
    private boolean resumeAfterPermission=false;
    private boolean pendingUpdateView=false, pendingInstallPermission=false;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable foregroundTick=new Runnable(){public void run(){
        Store s=Store.get(MainActivity.this);int interval=120;boolean enabled=false;
        synchronized(s){try{enabled=s.state.getJSONObject("settings").optBoolean("monitorEnabled");interval=s.state.getJSONObject("settings").optInt("interval",120);}catch(Exception ignored){}}
        if(enabled&&!MonitorService.active)Monitor.enqueue(MainActivity.this);
        handler.postDelayed(this,Math.max(60,interval)*1000L);
    }};
    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        try{api=new LocalApi(this);}catch(Exception e){new AlertDialog.Builder(this).setTitle("Dados locais preservados").setMessage(e.getMessage()).setPositiveButton("Fechar",(d,w)->finish()).show();return;}
        Alerts.channels(this);
        web=new WebView(this);web.setBackgroundColor(Color.rgb(10,16,24));
        web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(true);
        web.getSettings().setAllowFileAccessFromFileURLs(false);web.getSettings().setAllowUniversalAccessFromFileURLs(false);
        web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);web.getSettings().setSupportMultipleWindows(false);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        web.addJavascriptInterface(new Bridge(),"LivePulseNative");
        web.setWebViewClient(new WebViewClient(){
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){
                Uri u=request.getUrl();if(!"https".equals(u.getScheme())||!"app.livepulse.local".equals(u.getHost()))return blocked();
                String p=u.getPath();String name=p==null||p.equals("/")?"index.html":p.substring(1);
                if(name.matches("avatars/[a-f0-9-]{36}")){try{return new WebResourceResponse("image/jpeg",null,200,"OK",Collections.singletonMap("Cache-Control","private, max-age=86400"),new FileInputStream(Profiles.file(MainActivity.this,name.substring(8))));}catch(Exception e){return blocked();}}
                if(!Arrays.asList("index.html","style.css","design.css","app.js","upgrade.js","upgrade.css","polish.js","polish.css","updates.js","updates.css","sound-editor.js","sound-editor.css","pulse.wav").contains(name))return blocked();
                try{
                    String mime=name.endsWith(".css")?"text/css":name.endsWith(".js")?"text/javascript":name.endsWith(".wav")?"audio/wav":"text/html";
                    Map<String,String> headers=new HashMap<>();headers.put("Content-Security-Policy","default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; media-src 'self' data: blob:; connect-src 'none'; frame-src 'none'; object-src 'none'; base-uri 'none'");headers.put("X-Content-Type-Options","nosniff");
                    return new WebResourceResponse(mime,"UTF-8",200,"OK",headers,getAssets().open(name));
                }catch(IOException e){return blocked();}
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){Uri u=request.getUrl();if("https".equals(u.getScheme())&&"app.livepulse.local".equals(u.getHost()))return false;external(u);return true;}
            @Override public void onPageFinished(WebView view,String url){if(url.startsWith("https://app.livepulse.local/")){web.evaluateJavascript("document.documentElement.classList.add('android-app')",null);if(pendingUpdateView){web.evaluateJavascript("view('updates')",null);pendingUpdateView=false;}}}
        });
        web.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onShowFileChooser(WebView view,ValueCallback<Uri[]> callback,FileChooserParams params){
                if(fileCallback!=null)fileCallback.onReceiveValue(null);fileCallback=callback;
                Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("audio/*");
                try{startActivityForResult(intent,42);}catch(Exception e){fileCallback.onReceiveValue(null);fileCallback=null;Toast.makeText(MainActivity.this,"Nenhum seletor de arquivos disponível.",Toast.LENGTH_LONG).show();}return true;
            }
            @Override public boolean onJsConfirm(WebView view,String url,String message,JsResult result){new AlertDialog.Builder(MainActivity.this).setTitle("LivePulse").setMessage(message).setPositiveButton("Confirmar",(d,w)->result.confirm()).setNegativeButton("Cancelar",(d,w)->result.cancel()).setOnCancelListener(d->result.cancel()).show();return true;}
        });
        FrameLayout root=new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(10,16,24));
        root.addView(web,new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);
        // Let the system own the status/navigation bars and consume their insets once.
        root.setOnApplyWindowInsetsListener((view,insets)->{view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets.consumeSystemWindowInsets();});
        web.loadUrl("https://app.livepulse.local/");
        handleIntent(getIntent());
    }
    public void configureYouTubeKey(){
        android.widget.EditText input=new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("API Key do YouTube");
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this)
            .setTitle("Consultas privadas do YouTube")
            .setMessage("Você pode conectar sua conta Google ou configurar uma chave para consultar canais públicos. A chave será salva criptografada somente neste aparelho e não será incluída nos backups.")
            .setView(input).setNegativeButton("Cancelar",null).setPositiveButton("Salvar neste aparelho",null).create();
        dialog.setOnShowListener(ignored->dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String key=input.getText().toString().trim();
            if(key.isEmpty()){input.setError("Informe uma chave para continuar.");return;}
            try{Store store=Store.get(this);synchronized(store){store.state.getJSONObject("credentials").put("youtubeKey",key);store.save();}Monitor.clearTokens();dialog.dismiss();showMessage("Configuração privada do YouTube salva neste aparelho.");}
            catch(Exception error){showMessage("Não foi possível salvar a configuração privada do YouTube.");}
        }));
        dialog.show();
        if(dialog.getWindow()!=null)dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
    }
    public void configureKickSecret(){
        android.widget.EditText input=new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Client Secret da Kick");
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this)
            .setTitle("Configuração privada da Kick")
            .setMessage("O Client ID já está integrado. No modo local, a Kick também exige o Secret do aplicativo. Ele será salvo criptografado apenas neste aparelho. Sem essa configuração ou um serviço de autenticação, a conexão não pode ser concluída.")
            .setView(input).setNegativeButton("Cancelar",null).setPositiveButton("Salvar e conectar",null).create();
        dialog.setOnShowListener(ignored->dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String secret=input.getText().toString().trim();
            if(secret.isEmpty()){input.setError("Informe o Secret para continuar.");return;}
            try{Store store=Store.get(this);synchronized(store){store.state.getJSONObject("credentials").put("kickSecret",secret);store.save();}dialog.dismiss();Auth.connect(this,"kick");}
            catch(Exception error){showMessage("Não foi possível salvar a configuração privada da Kick.");}
        }));
        dialog.show();
        if(dialog.getWindow()!=null)dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
    }
    public void openUpdateInstaller(File file){
        if(isFinishing()||isDestroyed())return;
        if(!getPackageManager().canRequestPackageInstalls()){
            new AlertDialog.Builder(this).setTitle("Permitir atualização do LivePulse").setMessage("O Android precisa permitir que o LivePulse abra o instalador. Ative a permissão para esta fonte e volte; a instalação ainda precisa da sua confirmação.").setNegativeButton("Agora não",null).setPositiveButton("Abrir configurações",(dialog,which)->{try{pendingInstallPermission=true;startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())));}catch(Exception e){pendingInstallPermission=false;showMessage("Não foi possível abrir as configurações do Android.");}}).show();return;
        }
        try{Uri uri=androidx.core.content.FileProvider.getUriForFile(this,getPackageName()+".updates",file);startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));}catch(Exception e){showMessage("Nenhum instalador disponível ou instalação bloqueada pelo Android.");}
    }
    private WebResourceResponse blocked(){return new WebResourceResponse("text/plain","UTF-8",403,"Blocked",Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));}
    private void external(Uri uri){if(!"https".equals(uri.getScheme()))return;try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception e){Toast.makeText(this,"Nenhum navegador disponível.",Toast.LENGTH_SHORT).show();}}
    private void watch(String link){
        if(link==null)return;
        Uri uri=Uri.parse(link);if(!"https".equalsIgnoreCase(uri.getScheme()))return;
        String platformPackage=PlatformLinks.packageFor(link);
        if(!platformPackage.isEmpty()){
            Intent nativeView=new Intent(Intent.ACTION_VIEW,uri).addCategory(Intent.CATEGORY_BROWSABLE).setPackage(platformPackage);
            try{startActivity(nativeView);return;}catch(android.content.ActivityNotFoundException|SecurityException ignored){}
        }
        external(uri);
    }
    public final class Bridge {
        @JavascriptInterface public String request(String path,String method,String json){return api.call(path,method,json);}
        @JavascriptInterface public void requestAsync(String id,String path,String method,String json){if(id==null||!id.matches("r[0-9]{1,12}"))return;bridgeWorker.execute(()->{String result=api.call(path,method,json);runOnUiThread(()->{if(web!=null)web.evaluateJavascript("window.LivePulseBridgeReply&&window.LivePulseBridgeReply("+JSONObject.quote(id)+","+JSONObject.quote(result)+")",null);});});}
        @JavascriptInterface public void enable(){runOnUiThread(()->{
            if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){resumeAfterPermission=true;requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7);}
            else enableMonitor();
        });}
        @JavascriptInterface public void stopMonitor(){runOnUiThread(()->MonitorService.disable(MainActivity.this));}
        @JavascriptInterface public void testNotification(String scope){runOnUiThread(()->{JSONObject event=new JSONObject();try{event.put("soundScope",scope==null?"default":scope);if(scope!=null&&scope.startsWith("channel:")){String[] parts=scope.split(":");Store s=Store.get(MainActivity.this);synchronized(s){JSONObject channel=s.find("channels",parts[1]);event.put("platform",channel.optString("platform"));}}}catch(Exception ignored){}if(!Alerts.permitted(MainActivity.this)){Alerts.play(MainActivity.this,event);Toast.makeText(MainActivity.this,"Som de teste. Permita notificações para receber alertas.",Toast.LENGTH_LONG).show();return;}Alerts.notifyEvent(MainActivity.this,event,true);});}
        @JavascriptInterface public void downloadUpdate(){Updater.download(MainActivity.this);}
        @JavascriptInterface public void installUpdate(){Updater.install(MainActivity.this);}
        @JavascriptInterface public void connectAccount(String platform){runOnUiThread(()->Auth.connect(MainActivity.this,platform));}
        @JavascriptInterface public void disconnectAccount(String platform){if(Arrays.asList("twitch","kick","youtube").contains(platform))Auth.disconnect(MainActivity.this,platform);}
        @JavascriptInterface public void configureYouTubeApi(){runOnUiThread(()->configureYouTubeKey());}
        @JavascriptInterface public void setupGuide(String platform){runOnUiThread(()->showGuide(platform));}
        @JavascriptInterface public void exportBackup(){runOnUiThread(()->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json").putExtra(Intent.EXTRA_TITLE,"LivePulse-backup.json");startActivityForResult(i,61);});}
        @JavascriptInterface public void importBackup(){runOnUiThread(()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");startActivityForResult(i,62);});}
        @JavascriptInterface public void previewSound(){Alerts.play(MainActivity.this);}
        @JavascriptInterface public void setVolume(int value){Store s=Store.get(MainActivity.this);synchronized(s){try{s.state.getJSONObject("settings").put("volume",Math.max(0,Math.min(100,value)));s.save();}catch(Exception ignored){}}}
        @JavascriptInterface public void openLink(String link){runOnUiThread(()->watch(link));}
        @JavascriptInterface public void notificationSettings(){runOnUiThread(()->{Intent intent=new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName());startActivity(intent);});}
    }
    private void enableMonitor(){
        if(!Alerts.permitted(this)){Toast.makeText(this,"As notificações estão bloqueadas. Ative nas configurações do Android.",Toast.LENGTH_LONG).show();return;}
        try{
            Store s=Store.get(this);synchronized(s){s.state.getJSONObject("settings").put("monitorEnabled",true);s.save();}
            MonitorJob.schedule(this);startForegroundService(new Intent(this,MonitorService.class));Toast.makeText(this,"Radar local ativado.",Toast.LENGTH_SHORT).show();
        }catch(Exception e){Toast.makeText(this,"Não foi possível iniciar o radar: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){super.onRequestPermissionsResult(requestCode,permissions,results);if(requestCode==7&&resumeAfterPermission){resumeAfterPermission=false;if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)enableMonitor();else Toast.makeText(this,"Permita notificações para receber os alertas.",Toast.LENGTH_LONG).show();}}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent intent){super.onActivityResult(requestCode,resultCode,intent);if(requestCode==42&&fileCallback!=null){fileCallback.onReceiveValue(resultCode==RESULT_OK&&intent!=null&&intent.getData()!=null?new Uri[]{intent.getData()}:null);fileCallback=null;}
        if(requestCode==51&&resultCode==RESULT_OK&&intent!=null)Auth.googleCallback(this,intent);
        if((requestCode==61||requestCode==62)&&resultCode==RESULT_OK&&intent!=null&&intent.getData()!=null){Uri uri=intent.getData();new Thread(()->{try{if(requestCode==61){Backup.write(this,uri);showMessage("Backup salvo com canais, preferências, histórico e áudios. Contas e segredos não são exportados.");}else{JSONObject backup=Backup.load(this,uri);JSONObject clean=Backup.validate(backup);runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Restaurar backup?").setMessage("Substituir "+clean.optJSONArray("channels").length()+" canais, histórico e sons? Os lembretes importados ficarão desligados. Suas contas atuais serão mantidas.").setNegativeButton("Cancelar",null).setPositiveButton("Restaurar",(dialog,which)->{MonitorService.disable(this);Monitor.executor.execute(()->{try{Backup.restore(this,backup);showMessage("Backup restaurado. Ative o radar quando quiser.");}catch(Exception e){showMessage("Backup não restaurado: "+e.getMessage());}});}).show());}}catch(Exception e){showMessage("Não foi possível concluir: "+e.getMessage());}},"livepulse-backup").start();}
    }
    public void chooseGoogle(){Store s=Store.get(this);boolean configured;synchronized(s){configured=s.state.optJSONObject("settings").optBoolean("googleSetupAcknowledged");}if(configured){Auth.google(this);return;}showGuide("youtube",()->{synchronized(s){try{s.state.getJSONObject("settings").put("googleSetupAcknowledged",true);s.save();}catch(Exception ignored){}}Auth.google(this);});}
    public void openExternal(String uri){runOnUiThread(()->external(Uri.parse(uri)));}
    public void showMessage(String text){runOnUiThread(()->Toast.makeText(this,text,Toast.LENGTH_LONG).show());}
    public void showGuide(String platform){showGuide(platform,null);}
    private void showGuide(String platform,Runnable next){
        String text,link;
        if(platform.equals("youtube")){String sha="";try{android.content.pm.PackageInfo pi=getPackageManager().getPackageInfo(getPackageName(),PackageManager.GET_SIGNATURES);byte[] digest=java.security.MessageDigest.getInstance("SHA-1").digest(pi.signatures[0].toByteArray());StringBuilder v=new StringBuilder();for(byte b:digest){if(v.length()>0)v.append(':');v.append(String.format(java.util.Locale.ROOT,"%02X",b&255));}sha=v.toString();}catch(Exception ignored){}
            text="1. No Google Cloud, ative YouTube Data API v3.\n2. Configure a tela de consentimento e inclua sua conta como testadora se o projeto estiver em teste.\n3. Crie uma credencial OAuth do tipo Android com os dados abaixo.\n4. Volte e escolha sua conta. Sem cadastro, o Google pode recusar a autorização.\n\nPacote: "+getPackageName()+"\nSHA-1: "+sha+"\n\nSem Google Play Services, você ainda pode usar uma chave de API.";link="https://console.cloud.google.com/apis/credentials";
        }else if(platform.equals("kick")){text="1. Crie um aplicativo no portal Kick.\n2. Cadastre exatamente este retorno: "+Auth.KICK_REDIRECT+"\n3. Salve Client ID e Client Secret no formulário de Integrações.\n4. Toque em Conectar Kick e autorize no navegador.\n5. Na página de retorno HTTPS, toque em Continuar no LivePulse.\n\nOs dados são seus e ficam criptografados neste aparelho. Não envie o Client Secret em chats.";link="https://kick.com/settings/developer";}
        else{text="1. Crie um aplicativo no console Twitch e escolha cliente público para Device Code Flow.\n2. Salve apenas o Client ID em Integrações.\n3. Toque em Conectar Twitch e autorize o código no navegador.\n\nVocê não precisa digitar a senha da Twitch no LivePulse.";link="https://dev.twitch.tv/console/apps";}
        final String guide=text;AlertDialog.Builder dialog=new AlertDialog.Builder(this).setTitle("Configurar "+platform).setMessage(text).setNegativeButton("Abrir portal",(d,w)->openExternal(link)).setNeutralButton("Copiar instruções",(d,w)->{android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);clipboard.setPrimaryClip(ClipData.newPlainText("LivePulse: configuração",guide));});dialog.setPositiveButton(next==null?"Entendi":"Escolher conta",(d,w)->{if(next!=null)next.run();});dialog.show();
    }
    private void handleIntent(Intent intent){if(intent==null)return;if("show-updates".equals(intent.getAction())){pendingUpdateView=true;if(web!=null)web.evaluateJavascript("if(document.getElementById('updates'))view('updates')",null);intent.setAction(null);}if(intent.getData()!=null)Auth.callback(this,intent.getData());String id=intent.getStringExtra("eventId");if(id!=null&&id.matches("[a-f0-9-]{36}")){String url=null;Store s=Store.get(this);synchronized(s){try{url=s.find("events",id).optString("url");}catch(Exception ignored){}}if(url!=null){Reminders.acknowledge(this,id);watch(url);}intent.removeExtra("eventId");intent.removeExtra("watchUrl");}}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleIntent(intent);}
    @Override protected void onResume(){super.onResume();if(web!=null){web.onResume();handler.removeCallbacks(foregroundTick);handler.post(foregroundTick);Updater.schedule(this);Updater.check(this,false);if(pendingInstallPermission){pendingInstallPermission=false;if(getPackageManager().canRequestPackageInstalls())Updater.install(this);else showMessage("Permissão não concedida. Você pode tentar instalar novamente depois.");}}}
    @Override protected void onPause(){handler.removeCallbacks(foregroundTick);if(web!=null)web.onPause();super.onPause();}
    @Override public void onBackPressed(){if(web!=null)web.evaluateJavascript("document.querySelector('#dashboard').classList.contains('hidden')",result->{if("true".equals(result))web.evaluateJavascript("view('dashboard');window.scrollTo(0,0)",null);else moveTaskToBack(true);});else super.onBackPressed();}
    @Override protected void onDestroy(){handler.removeCallbacks(foregroundTick);bridgeWorker.shutdown();if(fileCallback!=null)fileCallback.onReceiveValue(null);if(web!=null){web.removeJavascriptInterface("LivePulseNative");web.destroy();web=null;}super.onDestroy();}
}
