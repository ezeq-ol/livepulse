package com.livepulse.app;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.Build;
import org.json.*;
import java.io.*;
import java.net.URL;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.net.ssl.HttpsURLConnection;

public final class Updater {
    private static final ExecutorService worker=Executors.newSingleThreadExecutor();
    private static final AtomicBoolean checking=new AtomicBoolean(),downloading=new AtomicBoolean(),installing=new AtomicBoolean();
    private static final AtomicInteger epoch=new AtomicInteger();
    private static volatile long bytes=0,total=0;
    private static long installed=-1;
    private static String installedName="";
    public static final int NOTICE_ID=47833;
    private static final String CHANNEL="livepulse_updates";
    public static JSONObject settings(Store s)throws Exception{
        JSONObject settings=s.state.optJSONObject("updateSettings");
        String url="";boolean enabled=true;
        try(InputStream in=s.context.getAssets().open("update-config.json")){byte[] config=read(in,8192);JSONObject j=new JSONObject(new String(config,"UTF-8"));url=j.optString("feedUrl","").trim();enabled=j.optBoolean("enabled",true);if(!url.isEmpty())Net.https(url);}catch(Exception ignored){}
        if(settings!=null){
            if(settings.optString("feedUrl").trim().isEmpty()&&!url.isEmpty()){
                settings.put("feedUrl",url).put("checkedAt",0);
                if(!settings.has("enabled"))settings.put("enabled",enabled);
                s.save();
            }
            return settings;
        }
        settings=new JSONObject().put("feedUrl",url).put("enabled",enabled).put("checkedAt",0);s.state.put("updateSettings",settings);return settings;
    }
    private static JSONObject record(Store s)throws Exception{JSONObject r=s.state.optJSONObject("updates");if(r==null){r=new JSONObject();s.state.put("updates",r);}return r;}
    public static synchronized long installedVersion(Context c)throws Exception{if(installed<0){PackageInfo p=c.getPackageManager().getPackageInfo(c.getPackageName(),0);installed=Build.VERSION.SDK_INT>=28?p.getLongVersionCode():p.versionCode;installedName=p.versionName;}return installed;}
    public static JSONObject status(Store s)throws Exception{
        JSONObject r=record(s),manifest=r.optJSONObject("available");long version=installedVersion(s.context);boolean available=manifest!=null&&UpdatePolicy.newer(manifest,version,Build.VERSION.SDK_INT);
        JSONObject result=new JSONObject().put("installedVersionCode",version).put("installedVersionName",installedName).put("checking",checking.get()).put("downloading",downloading.get()).put("installing",installing.get()).put("downloadedBytes",bytes).put("totalBytes",total).put("error",r.optString("error")).put("downloadError",r.optString("downloadError")).put("available",available?manifest:JSONObject.NULL).put("ready",available&&r.optBoolean("ready")&&apk(s.context).isFile());
        return result;
    }
    public static void configure(Context c,String input,boolean enabled)throws Exception{
        String url=input==null?"":input.trim();if(url.length()>2000)throw new IllegalArgumentException("Endereço muito longo.");if(!url.isEmpty())Net.https(url);
        Store s=Store.get(c);synchronized(s){epoch.incrementAndGet();JSONObject settings=settings(s);settings.put("feedUrl",url).put("enabled",enabled).put("checkedAt",0);s.state.put("updates",new JSONObject());s.save();}
        c.getSystemService(NotificationManager.class).cancel(NOTICE_ID);schedule(c);if(enabled&&!url.isEmpty())check(c,true);
    }
    public static void schedule(Context c){
        Store s=Store.get(c);boolean enabled=false;synchronized(s){try{JSONObject p=settings(s);enabled=p.optBoolean("enabled",true)&&!p.optString("feedUrl").isEmpty();}catch(Exception ignored){}}
        android.app.job.JobScheduler scheduler=c.getSystemService(android.app.job.JobScheduler.class);if(!enabled){scheduler.cancel(UpdateJob.ID);return;}
        if(scheduler.getPendingJob(UpdateJob.ID)==null)scheduler.schedule(new android.app.job.JobInfo.Builder(UpdateJob.ID,new ComponentName(c,UpdateJob.class)).setRequiredNetworkType(android.app.job.JobInfo.NETWORK_TYPE_ANY).setPeriodic(15*60*1000L).setPersisted(true).build());
    }
    public static void check(Context c,boolean force){check(c,force,()->{});}
    public static void check(Context context,boolean force,Runnable complete){
        Context c=context.getApplicationContext();Store s=Store.get(c);String feed;
        synchronized(s){try{JSONObject p=settings(s);if(!UpdatePolicy.due(p,System.currentTimeMillis(),force)){complete.run();return;}feed=p.getString("feedUrl");}catch(Exception e){complete.run();return;}}
        if(!checking.compareAndSet(false,true)){complete.run();return;}int generation=epoch.get();
        worker.execute(()->{try{
            synchronized(s){record(s).put("error","");}
            JSONObject manifest;try(Response r=response(feed)){manifest=UpdatePolicy.manifest(new JSONObject(new String(read(r.connection.getInputStream(),131072),"UTF-8")),c.getPackageName());}
            if(generation!=epoch.get())return;boolean newer=UpdatePolicy.newer(manifest,installedVersion(c),Build.VERSION.SDK_INT),announce=false;
            synchronized(s){if(generation!=epoch.get())return;JSONObject r=record(s),old=r.optJSONObject("available");boolean unchanged=old!=null&&old.optLong("versionCode")==manifest.optLong("versionCode")&&old.optString("sha256").equals(manifest.optString("sha256"));if(!unchanged)r.put("ready",false);settings(s).put("checkedAt",System.currentTimeMillis());r.put("error","").put("available",newer?manifest:JSONObject.NULL);if(newer&&r.optLong("notifiedVersion")!=manifest.optLong("versionCode")){announce=true;r.put("notifiedVersion",manifest.getLong("versionCode"));}s.save();}
            if(announce)notifyAvailable(c,manifest);
        }catch(Exception e){if(generation==epoch.get())synchronized(s){if(generation!=epoch.get())return;try{settings(s).put("checkedAt",System.currentTimeMillis());record(s).put("error","Não foi possível verificar atualizações. Confira a conexão e o endereço do arquivo JSON.");s.save();}catch(Exception ignored){}}}
        finally{checking.set(false);complete.run();if(generation!=epoch.get())check(c,true);}});
    }
    private static void notifyAvailable(Context c,JSONObject manifest){
        NotificationManager manager=c.getSystemService(NotificationManager.class);NotificationChannel channel=new NotificationChannel(CHANNEL,"Atualizações do LivePulse",NotificationManager.IMPORTANCE_DEFAULT);channel.setDescription("Aviso de nova versão; nenhum APK é instalado automaticamente.");manager.createNotificationChannel(channel);if(!Alerts.permitted(c))return;
        Intent intent=new Intent(c,MainActivity.class).setAction("show-updates").addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);PendingIntent pending=PendingIntent.getActivity(c,NOTICE_ID,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        manager.notify(NOTICE_ID,new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_update_notification).setColor(0xffb6a0ff).setContentTitle("Existe uma atualização disponível").setContentText("LivePulse "+manifest.optString("versionName")+" · Toque para ver e baixar.").setContentIntent(pending).setAutoCancel(true).addAction(new Notification.Action.Builder(null,"Ver atualização",pending).build()).build());
    }
    private static File folder(Context c){File f=new File(c.getCacheDir(),"updates");if(!f.exists()&&!f.mkdirs())throw new IllegalStateException("Não foi possível preparar o download.");return f;}
    public static File apk(Context c){return new File(folder(c),"update.apk");}
    public static void download(MainActivity activity){
        Context c=activity.getApplicationContext();Store s=Store.get(c);JSONObject manifest;
        synchronized(s){try{manifest=record(s).getJSONObject("available");if(!UpdatePolicy.newer(manifest,installedVersion(c),Build.VERSION.SDK_INT))throw new IllegalArgumentException("Nenhuma atualização compatível disponível.");manifest=new JSONObject(manifest.toString());record(s).put("downloadError","").put("ready",false);}catch(Exception e){activity.showMessage("Nenhuma atualização compatível disponível.");return;}}
        if(!downloading.compareAndSet(false,true))return;final JSONObject release=manifest;int generation=epoch.get();bytes=0;total=release.optLong("sizeBytes");
        worker.execute(()->{File temporary=null;try{
            temporary=new File(folder(c),"pending.apk");MessageDigest digest=MessageDigest.getInstance("SHA-256");
            try(Response r=response(release.getString("apkUrl"));InputStream in=r.connection.getInputStream();FileOutputStream out=new FileOutputStream(temporary)){
                byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1){if(generation!=epoch.get())throw new IOException("Origem alterada.");if(bytes+n>total||bytes+n>UpdatePolicy.MAX_APK_BYTES)throw new IOException("Tamanho do APK não corresponde à publicação.");out.write(buffer,0,n);digest.update(buffer,0,n);bytes+=n;}out.flush();out.getFD().sync();
            }
            if(bytes!=total||!UpdatePolicy.matches(release.getString("sha256"),hex(digest.digest())))throw new SecurityException("A integridade do APK não confere.");verify(c,temporary,release,false);
            if(generation!=epoch.get())throw new IOException("Origem alterada.");File target=apk(c);if(target.exists()&&!target.delete())throw new IOException("Não foi possível substituir o download anterior.");if(!temporary.renameTo(target))throw new IOException("Não foi possível concluir o download.");
            synchronized(s){if(generation!=epoch.get())return;record(s).put("ready",true).put("downloadError","").put("downloadManifest",release);s.save();}activity.showMessage("Atualização verificada. Toque em Instalar para abrir o instalador Android.");
        }catch(Exception e){if(temporary!=null)temporary.delete();if(generation==epoch.get())synchronized(s){if(generation!=epoch.get())return;try{record(s).put("ready",false).put("downloadError",e instanceof SecurityException?e.getMessage():"O download não foi concluído ou não corresponde à versão publicada. Tente novamente.");s.save();}catch(Exception ignored){}}}
        finally{downloading.set(false);}});
    }
    public static void install(MainActivity a){
        if(!installing.compareAndSet(false,true))return;worker.execute(()->{try{Store s=Store.get(a);JSONObject release;synchronized(s){if(!record(s).optBoolean("ready"))throw new IllegalArgumentException("Baixe a atualização primeiro.");release=new JSONObject(record(s).getJSONObject("downloadManifest").toString());}File file=apk(a);verify(a,file,release,true);a.runOnUiThread(()->a.openUpdateInstaller(file));}catch(Exception e){a.showMessage("Instalação não iniciada: "+e.getMessage());}finally{installing.set(false);}});
    }
    private static void verify(Context c,File file,JSONObject release,boolean hash)throws Exception{
        if(!file.isFile()||file.length()!=release.getLong("sizeBytes"))throw new SecurityException("Tamanho do download inválido.");
        if(hash){MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)digest.update(b,0,n);}if(!UpdatePolicy.matches(release.getString("sha256"),hex(digest.digest())))throw new SecurityException("A integridade do APK não confere.");}
        int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;PackageInfo incoming=c.getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(),flags),current=c.getPackageManager().getPackageInfo(c.getPackageName(),flags);
        if(incoming==null||!c.getPackageName().equals(incoming.packageName))throw new SecurityException("O APK pertence a outro aplicativo ou é inválido.");long code=Build.VERSION.SDK_INT>=28?incoming.getLongVersionCode():incoming.versionCode;
        if(code!=release.getLong("versionCode")||code<=installedVersion(c)||incoming.applicationInfo==null||incoming.applicationInfo.minSdkVersion>Build.VERSION.SDK_INT)throw new SecurityException("Versão incompatível com este aparelho.");
        if(!certificates(incoming).equals(certificates(current))||certificates(current).isEmpty())throw new SecurityException("A assinatura não corresponde ao LivePulse instalado.");
    }
    private static Set<String> certificates(PackageInfo p)throws Exception{Set<String> set=new HashSet<>();android.content.pm.Signature[] signatures=Build.VERSION.SDK_INT>=28?(p.signingInfo==null?null:p.signingInfo.getApkContentsSigners()):p.signatures;if(signatures!=null)for(android.content.pm.Signature signature:signatures)set.add(hex(MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())));return set;}
    private static String hex(byte[] bytes){StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();}
    private static byte[] read(InputStream input,int max)throws Exception{try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>max)throw new IOException("Arquivo muito grande.");out.write(b,0,n);}return out.toByteArray();}}
    private static final class Response implements AutoCloseable{final HttpsURLConnection connection;Response(HttpsURLConnection c){connection=c;}public void close(){connection.disconnect();}}
    private static Response response(String input)throws Exception{
        URL url=Net.https(input);for(int redirect=0;redirect<5;redirect++){HttpsURLConnection c=(HttpsURLConnection)url.openConnection();c.setConnectTimeout(12000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(false);c.setRequestProperty("Accept","application/json, application/octet-stream");int code;try{code=c.getResponseCode();}catch(Exception e){c.disconnect();throw e;}
            if(code>=300&&code<400){String location=c.getHeaderField("Location");c.disconnect();if(location==null)throw new IOException("Redirecionamento inválido.");url=Net.https(new URL(url,location).toString());continue;}if(code!=200){c.disconnect();throw new IOException("Servidor indisponível.");}return new Response(c);
        }throw new IOException("Muitos redirecionamentos.");
    }
}
