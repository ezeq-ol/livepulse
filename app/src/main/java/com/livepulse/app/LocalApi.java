package com.livepulse.app;

import android.content.Context;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.util.*;

public final class LocalApi {
    private final Context context;private final Store store;
    public LocalApi(Context c){context=c;store=Store.get(c);}
    public String call(String path,String method,String text){
        try{return new JSONObject().put("ok",true).put("data",route(path,method,new JSONObject(text==null||text.isEmpty()?"{}":text))).toString();}
        catch(Exception e){return "{\"ok\":false,\"error\":"+JSONObject.quote(e.getMessage()==null?"Não foi possível concluir a operação.":e.getMessage())+"}";}
    }
    private Object route(String path,String method,JSONObject b)throws Exception{
        synchronized(store){
            if(path.equals("/api/state")&&method.equals("GET"))return store.snapshot();
            if(path.equals("/api/updates/config")&&method.equals("POST")){Updater.configure(context,b.optString("feedUrl"),b.optBoolean("enabled",true));return store.snapshot();}
            if(path.equals("/api/updates/check")&&method.equals("POST")){Updater.check(context,true);return store.snapshot();}
            if(path.equals("/api/check")&&method.equals("POST")){Monitor.enqueue(context);return new JSONObject().put("ok",true);}
            if(path.equals("/api/profile-refresh")&&method.equals("POST")){JSONObject c=store.find("channels",b.getString("id"));c.put("profileCheckedAt",0);c.remove("profileRetryAt");store.save();Monitor.enqueue(context);return store.snapshot();}
            if(path.equals("/api/ack")&&method.equals("POST")){Reminders.acknowledge(context,b.getString("id"));return store.snapshot();}
            if(path.equals("/api/classification")&&method.equals("POST")){String type=b.getString("type");if(!Arrays.asList("live","video","short","agendada","replay","unknown").contains(type))throw new IllegalArgumentException("Tipo inválido.");JSONObject e=store.find("events",b.getString("id"));if(!e.optString("platform").equals("youtube"))throw new IllegalArgumentException("Correção disponível para YouTube.");e.put("type",type).put("classificationOverride",true).put("classification","Classificação corrigida por você");store.save();return store.snapshot();}
            if(path.equals("/api/sound-rules")){
                String scope=SoundBank.scope(b);
                if(method.equals("POST"))return SoundBank.put(context,store,b);
                if(method.equals("DELETE")){SoundBank.rules(store).remove(scope);if(scope.equals("default")){store.state.remove("soundName");new File(context.getFilesDir(),"custom-sound.bin").delete();}store.save();return store.snapshot();}
                return SoundBank.rules(store);
            }
            if(path.equals("/api/channels")&&method.equals("POST")){
                JSONObject c;
                if(!b.optString("integrationId").isEmpty()){
                    JSONObject integration=store.find("integrations",b.getString("integrationId"));String url=Rules.required(b,"url",2000);Net.https(url);
                    c=new JSONObject().put("platform","custom").put("integrationId",integration.getString("id")).put("platformName",integration.getString("name")).put("url",url).put("slug",Rules.required(b,"slug",200));
                }else c=Rules.channel(Rules.required(b,"url",2000));
                JSONArray list=store.state.getJSONArray("channels");
                for(int i=0;i<list.length();i++){JSONObject old=list.getJSONObject(i);if(old.getString("url").equals(c.getString("url"))&&old.optString("integrationId").equals(c.optString("integrationId")))throw new IllegalArgumentException("Este canal já foi adicionado.");}
                c.put("id",Rules.id()).put("name",b.optString("name",c.getString("slug"))).put("filters",new JSONArray("[\"live\",\"video\",\"short\",\"unknown\"]")).put("seen",new JSONArray()).put("paused",false);list.put(c);store.save();Monitor.enqueue(context);return store.snapshot();
            }
            if(path.startsWith("/api/channels/")&&(method.equals("PATCH")||method.equals("DELETE"))){
                String id=path.substring(path.lastIndexOf('/')+1);JSONObject c=store.find("channels",id);
                if(method.equals("DELETE"))store.remove("channels",id);
                else{if(b.has("paused"))c.put("paused",b.getBoolean("paused"));if(b.has("filters")){JSONArray incoming=b.getJSONArray("filters"),filters=new JSONArray();for(int i=0;i<incoming.length();i++){String v=incoming.getString(i);if(Arrays.asList("live","video","short","agendada","replay","unknown").contains(v))filters.put(v);}c.put("filters",filters);}}
                store.save();return store.snapshot();
            }
            if(path.equals("/api/integrations")&&method.equals("POST")){
                JSONObject previous=b.optString("id").isEmpty()?null:store.find("integrations",b.getString("id"));JSONObject integration=Rules.integration(b,previous);
                if(previous!=null)store.remove("integrations",previous.getString("id"));store.state.getJSONArray("integrations").put(integration);store.save();return store.snapshot();
            }
            if(path.startsWith("/api/integrations/")&&method.equals("DELETE")){
                String id=path.substring(path.lastIndexOf('/')+1);JSONArray channels=store.state.getJSONArray("channels");for(int i=0;i<channels.length();i++)if(id.equals(channels.getJSONObject(i).optString("integrationId")))throw new IllegalArgumentException("Remova os canais desta integração antes de excluí-la.");store.remove("integrations",id);store.save();return store.snapshot();
            }
            if(path.equals("/api/settings")&&method.equals("POST")){
                if(b.has("remindersEnabled")){store.state.getJSONObject("settings").put("remindersEnabled",b.getBoolean("remindersEnabled"));if(!b.getBoolean("remindersEnabled"))Alerts.cancelSound(context,"all");Reminders.schedule(context);}
                if(b.has("interval"))store.state.getJSONObject("settings").put("interval",Math.max(60,Math.min(3600,b.optInt("interval",120))));
                for(String k:new String[]{"twitchId","twitchSecret","kickId","kickSecret","youtubeKey"})if(!b.optString(k).trim().isEmpty())store.state.getJSONObject("credentials").put(k,b.getString(k).trim());
                store.save();Monitor.executor.execute(Monitor::clearTokens);return store.snapshot();
            }
            if(path.equals("/api/history")&&method.equals("DELETE")){Reminders.acknowledge(context,"all");store.state.put("events",new JSONArray());store.save();return store.snapshot();}
            if(path.equals("/api/sound")){
                if(method.equals("DELETE")){store.state.remove("soundName");new File(context.getFilesDir(),"custom-sound.bin").delete();store.save();return new JSONObject().put("ok",true);}
                if(method.equals("POST")){
                    String data=b.optString("data");if(!data.matches("^data:audio/[a-zA-Z0-9.+-]+;base64,[A-Za-z0-9+/=]+$"))throw new IllegalArgumentException("Escolha um arquivo de áudio válido.");
                    byte[] bytes=Base64.decode(data.substring(data.indexOf(',')+1),Base64.DEFAULT);if(bytes.length>5*1024*1024)throw new IllegalArgumentException("O áudio deve ter até 5 MB.");
                    android.util.AtomicFile file=new android.util.AtomicFile(new File(context.getFilesDir(),"custom-sound.bin"));FileOutputStream out=null;try{out=file.startWrite();out.write(bytes);file.finishWrite(out);}catch(Exception e){if(out!=null)file.failWrite(out);throw e;}
                    store.state.put("soundName",b.optString("name","Som personalizado"));store.save();return new JSONObject().put("ok",true);
                }
                if(method.equals("GET"))return new JSONObject().put("name",store.state.optString("soundName")).put("native",true);
            }
            throw new IllegalArgumentException("Operação não encontrada.");
        }
    }
}
