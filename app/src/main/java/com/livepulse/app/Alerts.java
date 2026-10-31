package com.livepulse.app;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.*;
import android.net.Uri;
import android.os.*;
import org.json.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class Alerts {
    private static final AtomicInteger ids=new AtomicInteger(100);
    private static MediaPlayer player;
    private static String playingId="";
    private static final java.util.ArrayDeque<JSONObject> audioQueue=new java.util.ArrayDeque<>();
    public static final String EVENTS="livepulse_events_v2",MONITOR="livepulse_monitor";
    public static void channels(Context c){
        NotificationManager n=c.getSystemService(NotificationManager.class);
        NotificationChannel events=new NotificationChannel(EVENTS,"Alertas de lives",NotificationManager.IMPORTANCE_HIGH);
        events.setDescription("Lives, vídeos e Shorts. Som configurado no Estúdio de som do app.");events.setSound(null,null);events.enableVibration(true);n.createNotificationChannel(events);
        NotificationChannel monitor=new NotificationChannel(MONITOR,"Monitoramento local",NotificationManager.IMPORTANCE_LOW);monitor.setSound(null,null);n.createNotificationChannel(monitor);
    }
    public static boolean permitted(Context c){return (Build.VERSION.SDK_INT<33||c.checkSelfPermission("android.permission.POST_NOTIFICATIONS")==PackageManager.PERMISSION_GRANTED)&&c.getSystemService(NotificationManager.class).areNotificationsEnabled();}
    public static PendingIntent open(Context c){return PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    public static Notification ongoing(Context c){
        channels(c);
        Intent stop=new Intent(c,MonitorService.class).setAction("stop");PendingIntent action=PendingIntent.getService(c,1,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(c,MONITOR).setSmallIcon(R.drawable.ic_notification).setContentTitle("LivePulse · radar ativo").setContentText("Consultando as plataformas neste aparelho").setContentIntent(open(c)).setOngoing(true).setOnlyAlertOnce(true).addAction(new Notification.Action.Builder(null,"Pausar radar",action).build()).build();
    }
    public static void notifyEvent(Context c,JSONObject e,boolean test){
        channels(c);if(!permitted(c))return;
        String title=test?"LivePulse · Notificação teste":e.optString("channel","LivePulse")+" · "+label(e.optString("type"));
        PendingIntent intent=open(c);int nid=test?99:Reminders.notificationId(e.optString("id"));
        if(!test){Intent watch=new Intent(c,MainActivity.class).setAction("watch").setData(Uri.parse("livepulse://event/"+e.optString("id"))).putExtra("eventId",e.optString("id")).putExtra("watchUrl",e.optString("url")).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);intent=PendingIntent.getActivity(c,nid,watch,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
        String extra=details(e);if(e.optInt("reminderCount")>0)title="Lembrete "+e.optInt("reminderCount")+" · "+title;
        Notification.Builder builder=new Notification.Builder(c,EVENTS).setSmallIcon(NotificationIcons.small(e.optString("platform"))).setColor(NotificationIcons.color(e.optString("platform"))).setContentTitle(title).setContentText(test?"Seu som personalizado está pronto.":e.optString("title")).setSubText(extra).setStyle(new Notification.BigTextStyle().bigText(e.optString("title")+"\n"+extra)).setAutoCancel(true).setContentIntent(intent).setCategory(Notification.CATEGORY_EVENT);
        if(!test){Intent ack=new Intent(c,AlertActions.class).setAction("ack").setData(Uri.parse("livepulse://ack/"+e.optString("id"))).putExtra("eventId",e.optString("id"));PendingIntent ackIntent=PendingIntent.getBroadcast(c,nid,ack,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);builder.addAction(new Notification.Action.Builder(null,"Assistir",intent).build()).addAction(new Notification.Action.Builder(null,"Já vi · parar lembretes",ackIntent).build());}
        c.getSystemService(NotificationManager.class).notify(nid,builder.build());
        if(!test&&!e.optString("thumbnail").isEmpty())new Thread(()->{try{android.graphics.Bitmap image=thumbnail(c,e);if(image==null)return;Store s=Store.get(c);synchronized(s){if(s.find("events",e.optString("id")).optBoolean("acknowledged"))return;}builder.setLargeIcon(image).setStyle(new Notification.BigPictureStyle().bigPicture(image).setSummaryText(e.optString("title")+" · "+extra)).setOnlyAlertOnce(true);c.getSystemService(NotificationManager.class).notify(nid,builder.build());}catch(Exception ignored){}},"livepulse-image").start();
        NotificationChannel channel=c.getSystemService(NotificationManager.class).getNotificationChannel(EVENTS);
        if(channel!=null&&channel.getImportance()!=NotificationManager.IMPORTANCE_NONE)play(c,e);
    }
    private static String details(JSONObject e){String platform=e.optString("platformName",e.optString("platform"));String text=platform+" · "+label(e.optString("type"));for(String k:new String[]{"category","viewerCount","duration","scheduledAt"})if(!e.optString(k).isEmpty())text+=" · "+e.optString(k)+(k.equals("viewerCount")?" assistindo":"");return text;}
    private static android.graphics.Bitmap thumbnail(Context c,JSONObject e)throws Exception{
        File cache=new File(c.getCacheDir(),"thumb-"+Reminders.notificationId(e.optString("id"))+".jpg");if(cache.exists())return android.graphics.BitmapFactory.decodeFile(cache.getAbsolutePath());
        javax.net.ssl.HttpsURLConnection connection=(javax.net.ssl.HttpsURLConnection)Net.https(e.getString("thumbnail")).openConnection();connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(4000);connection.setReadTimeout(4000);
        try{if(connection.getResponseCode()!=200)return null;byte[] buffer=new byte[8192];int n,total=0;try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){while((n=in.read(buffer))!=-1){total+=n;if(total>2*1024*1024)return null;out.write(buffer,0,n);}byte[] bytes=out.toByteArray();android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();options.inJustDecodeBounds=true;android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(options.outWidth<1||options.outHeight<1)return null;options.inSampleSize=Math.max(1,Math.max(options.outWidth,options.outHeight)/640);options.inJustDecodeBounds=false;android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(bitmap!=null){try(OutputStream file=new FileOutputStream(cache)){bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG,80,file);}}return bitmap;}}finally{connection.disconnect();}
    }
    private static String label(String type){switch(type){case"live":return"AO VIVO";case"short":return"SHORT";case"video":return"VÍDEO";case"agendada":return"AGENDADA";case"unknown":return"CONTEÚDO NOVO";default:return"REPLAY";}}
    public static void play(Context c){play(c,new JSONObject());}
    private static void nextSound(Context c){if(player==null&&!audioQueue.isEmpty())play(c,audioQueue.removeFirst());}
    public static void cancelSound(Context context,String id){Context c=context.getApplicationContext();new Handler(Looper.getMainLooper()).post(()->{audioQueue.removeIf(e->id.equals("all")||id.equals(e.optString("id")));if(player!=null&&(id.equals("all")||id.equals(playingId))){player.release();player=null;playingId="";}nextSound(c);});}
    public static synchronized void play(Context context,JSONObject event){
        Context c=context.getApplicationContext();
        new Handler(Looper.getMainLooper()).post(()->{
            try {
                if(!event.optString("id").isEmpty()){Store s=Store.get(c);synchronized(s){try{if(s.find("events",event.optString("id")).optBoolean("acknowledged")){nextSound(c);return;}}catch(Exception ignored){nextSound(c);return;}}}
                AudioManager audio=c.getSystemService(AudioManager.class);NotificationManager manager=c.getSystemService(NotificationManager.class);
                if(audio.getRingerMode()!=AudioManager.RINGER_MODE_NORMAL||manager.getCurrentInterruptionFilter()!=NotificationManager.INTERRUPTION_FILTER_ALL){audioQueue.clear();return;}
                if(player!=null){if(audioQueue.size()<100)audioQueue.addLast(event);return;}
                Store store=Store.get(c);float volume;File file;
                synchronized(store){volume=store.state.getJSONObject("settings").optInt("volume",75)/100f;if(event.has("soundScope")){String scope=event.optString("soundScope");JSONObject rule=SoundBank.rules(store).optJSONObject(scope);if(rule!=null)file=SoundBank.file(c,rule);else{JSONObject sample=new JSONObject();String[] parts=scope.split(":");if(parts.length==3)sample.put("channelId",parts[1]).put("type",parts[2]);if(parts.length==2)sample.put("type",parts[1]);file=SoundBank.resolve(c,store,sample);}}else file=SoundBank.resolve(c,store,event);}
                player=new MediaPlayer();player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
                if(file!=null&&file.exists())player.setDataSource(file.getAbsolutePath());
                else{android.content.res.AssetFileDescriptor fd=c.getAssets().openFd("pulse.wav");player.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());fd.close();}
                playingId=event.optString("id");player.setVolume(volume,volume);player.setOnCompletionListener(p->{p.release();if(player==p){player=null;playingId="";}nextSound(c);});
                player.setOnErrorListener((p,what,extra)->{p.release();if(player==p){player=null;playingId="";}nextSound(c);return true;});
                player.setOnPreparedListener(MediaPlayer::start);player.prepareAsync();
            }catch(Exception e){android.util.Log.e("LivePulse","Não foi possível tocar o áudio",e);if(player!=null){player.release();player=null;}}
        });
    }
}
