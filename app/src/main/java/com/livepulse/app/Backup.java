package com.livepulse.app;
import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.util.*;

public final class Backup {
    public static JSONObject export(Context c)throws Exception{
        Store s=Store.get(c);synchronized(s){
            JSONObject data=new JSONObject(s.state.toString());data.remove("credentials");data.remove("oauth");data.remove("authPending");data.remove("authStatus");data.remove("googlePendingAccount");
            JSONArray integrations=data.getJSONArray("integrations");for(int i=0;i<integrations.length();i++)integrations.getJSONObject(i).remove("bearer");
            JSONObject media=new JSONObject(),rules=SoundBank.rules(s);Iterator<String> keys=rules.keys();long total=0;
            while(keys.hasNext()){String key=keys.next();File file=SoundBank.file(c,rules.getJSONObject(key));if(file.exists()){byte[] bytes=read(new FileInputStream(file),5*1024*1024);total+=bytes.length;if(total>25*1024*1024)throw new IOException("Os áudios do backup ultrapassam 25 MB.");media.put(key,new JSONObject().put("name",rules.getJSONObject(key).optString("name")).put("base64",Base64.encodeToString(bytes,Base64.NO_WRAP)));}}
            File legacy=new File(c.getFilesDir(),"custom-sound.bin");if(!media.has("default")&&data.has("soundName")&&legacy.exists()){byte[] bytes=read(new FileInputStream(legacy),5*1024*1024);media.put("default",new JSONObject().put("name",data.optString("soundName")).put("base64",Base64.encodeToString(bytes,Base64.NO_WRAP)));}
            data.remove("soundRules");data.remove("soundName");return new JSONObject().put("format","livepulse-backup").put("version",1).put("createdAt",java.time.Instant.now().toString()).put("data",data).put("audio",media);
        }
    }
    private static byte[] read(InputStream input,int limit)throws Exception{try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n,total=0;while((n=in.read(buffer))!=-1){total+=n;if(total>limit)throw new IOException("Arquivo maior que o limite permitido.");out.write(buffer,0,n);}return out.toByteArray();}}
    public static JSONObject load(Context c,Uri uri)throws Exception{return new JSONObject(new String(read(c.getContentResolver().openInputStream(uri),38*1024*1024),"UTF-8"));}
    public static void write(Context c,Uri uri)throws Exception{byte[] bytes=export(c).toString().getBytes("UTF-8");try(OutputStream out=c.getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("Não foi possível salvar o arquivo.");out.write(bytes);}}
    public static JSONObject validate(JSONObject backup)throws Exception{
        if(!backup.optString("format").equals("livepulse-backup")||backup.optInt("version")!=1)throw new IllegalArgumentException("Backup LivePulse incompatível.");
        JSONObject d=backup.getJSONObject("data"),clean=new JSONObject();JSONArray channels=d.getJSONArray("channels"),integrations=d.getJSONArray("integrations"),events=d.getJSONArray("events");if(channels.length()>200||integrations.length()>100||events.length()>300)throw new IllegalArgumentException("Backup ultrapassa o limite de itens.");
        Set<String> ids=new HashSet<>();JSONArray newIntegrations=new JSONArray();
        for(int i=0;i<integrations.length();i++){JSONObject in=integrations.getJSONObject(i);String id=in.getString("id");if(!id.matches("[a-f0-9-]{36}")||!ids.add(id))throw new IllegalArgumentException("ID de integração inválido.");JSONObject entry=new JSONObject(in.toString());entry.put("bearer","");newIntegrations.put(Rules.integration(entry,new JSONObject().put("id",id)));}
        Set<String> channelIds=new HashSet<>();JSONArray newChannels=new JSONArray();
        for(int i=0;i<channels.length();i++){JSONObject in=channels.getJSONObject(i);String id=in.getString("id");if(!id.matches("[a-f0-9-]{36}")||!channelIds.add(id))throw new IllegalArgumentException("ID de canal inválido.");JSONObject ch;
            if(in.optString("platform").equals("custom")){Net.https(in.getString("url"));if(!ids.contains(in.optString("integrationId")))throw new IllegalArgumentException("Integração de canal ausente no backup.");ch=new JSONObject().put("platform","custom").put("url",in.getString("url")).put("slug",Rules.required(in,"slug",200)).put("integrationId",in.getString("integrationId")).put("platformName",in.optString("platformName","Personalizada"));}
            else ch=Rules.channel(in.getString("url"));
            ch.put("id",id).put("name",in.optString("name",ch.getString("slug"))).put("paused",in.optBoolean("paused")).put("initialized",in.optBoolean("initialized"));
            JSONArray filters=in.getJSONArray("filters");for(int j=0;j<filters.length();j++)if(!Arrays.asList("live","video","short","agendada","replay","unknown").contains(filters.getString(j)))throw new IllegalArgumentException("Filtro inválido.");ch.put("filters",filters);
            JSONArray seen=in.optJSONArray("seen");if(seen==null)seen=new JSONArray();if(seen.length()>1000)throw new IllegalArgumentException("Histórico de canal inválido.");for(int j=0;j<seen.length();j++)if(seen.getString(j).length()>300)throw new IllegalArgumentException("ID de conteúdo inválido.");ch.put("seen",seen);newChannels.put(ch);
        }
        JSONArray newEvents=new JSONArray();Set<String> eventIds=new HashSet<>();for(int i=0;i<events.length();i++){JSONObject e=new JSONObject(events.getJSONObject(i).toString());if(!e.optString("id").matches("[a-f0-9-]{36}")||!eventIds.add(e.getString("id")))throw new IllegalArgumentException("ID de alerta inválido.");Net.https(e.getString("url"));if(!e.optString("thumbnail").isEmpty())Net.https(e.getString("thumbnail"));e.put("acknowledged",true);e.remove("nextReminder");newEvents.put(e);}
        JSONObject settings=d.getJSONObject("settings");clean.put("settings",new JSONObject().put("interval",Math.max(60,Math.min(3600,settings.optInt("interval",120)))).put("volume",Math.max(0,Math.min(100,settings.optInt("volume",75)))).put("monitorEnabled",false).put("remindersEnabled",false));
        return clean.put("channels",newChannels).put("integrations",newIntegrations).put("events",newEvents).put("lastPoll",JSONObject.NULL);
    }
    public static void restore(Context c,JSONObject backup)throws Exception{
        JSONObject clean=validate(backup),audio=backup.optJSONObject("audio"),rules=new JSONObject();List<File> written=new ArrayList<>();long total=0;
        try{if(audio!=null){if(audio.length()>64)throw new IllegalArgumentException("Máximo de 64 sons por backup.");Iterator<String> keys=audio.keys();while(keys.hasNext()){String scope=keys.next();SoundBank.scope(new JSONObject().put("scope",scope));JSONObject item=audio.getJSONObject(scope);byte[] bytes=Base64.decode(item.getString("base64"),Base64.DEFAULT);total+=bytes.length;if(bytes.length==0||bytes.length>5*1024*1024||total>25*1024*1024)throw new IllegalArgumentException("Áudios do backup excedem o limite permitido.");File file=new File(c.getFilesDir(),"audio-"+Rules.id()+".bin");try(OutputStream out=new FileOutputStream(file)){out.write(bytes);}written.add(file);rules.put(scope,new JSONObject().put("file",file.getName()).put("name",item.optString("name","Som importado")));}}
            MonitorService.disable(c);Reminders.acknowledge(c,"all");Store s=Store.get(c);synchronized(s){JSONObject old=s.state;clean.put("credentials",old.getJSONObject("credentials")).put("oauth",old.optJSONObject("oauth")==null?new JSONObject():old.getJSONObject("oauth")).put("soundRules",rules);s.state=clean;try{s.save();}catch(Exception e){s.state=old;throw e;}}
        }catch(Exception e){for(File f:written)f.delete();throw e;}
    }
}
