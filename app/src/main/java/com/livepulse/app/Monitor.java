package com.livepulse.app;

import android.content.Context;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.time.Instant;

public final class Monitor {
    public static final ExecutorService executor=Executors.newSingleThreadExecutor();
    private static final Map<String,String> tokens=new HashMap<>();
    private static final Map<String,Long> expiry=new HashMap<>();
    private static final Set<String> TYPES=new HashSet<>(Arrays.asList("live","video","short","agendada","replay"));
    private static final AtomicBoolean queued=new AtomicBoolean(false);
    public static void enqueue(Context c){if(queued.compareAndSet(false,true))executor.execute(()->{try{poll(c);}finally{queued.set(false);}});}
    public static void clearTokens(){synchronized(tokens){tokens.clear();expiry.clear();}}
    private static JSONObject creds(Store s)throws Exception{synchronized(s){return new JSONObject(s.state.getJSONObject("credentials").toString());}}
    private static String token(Store s,String p)throws Exception{
        synchronized(tokens){
            String connected=Auth.token(s.context,p);if(connected!=null)return connected;
            if(expiry.containsKey(p)&&expiry.get(p)>System.currentTimeMillis())return tokens.get(p);
            JSONObject c=creds(s);String id=c.optString(p+"Id"),secret=c.optString(p+"Secret");
            if(id.isEmpty()||secret.isEmpty())throw new IllegalArgumentException("Configure Client ID e Client Secret de "+p+" em Integrações.");
            String form="grant_type=client_credentials&client_id="+Net.enc(id)+"&client_secret="+Net.enc(secret);
            JSONObject d=Net.json(p.equals("kick")?"https://id.kick.com/oauth/token":"https://id.twitch.tv/oauth2/token",null,null,form);
            String value=d.getString("access_token");tokens.put(p,value);expiry.put(p,System.currentTimeMillis()+Math.max(1,d.optLong("expires_in",3600)-60)*1000);return value;
        }
    }
    private static JSONObject yt(Store s,String endpoint,String...params)throws Exception{
        String key=creds(s).optString("youtubeKey"),connected=Auth.token(s.context,"youtube");if(key.isEmpty()&&connected==null)throw new IllegalArgumentException("Conecte sua conta Google ou configure a chave YouTube Data API v3 em Integrações.");
        Map<String,String> p=Net.params(params);if(!key.isEmpty())p.put("key",key);return Net.json(Net.query("https://www.googleapis.com/youtube/v3/"+endpoint,p),connected,null,null);
    }
    private static void collectShorts(Object o,Set<String> ids)throws Exception{
        if(o instanceof JSONObject){JSONObject obj=(JSONObject)o,r=obj.optJSONObject("reelWatchEndpoint");if(r!=null&&!r.optString("videoId").isEmpty())ids.add(r.optString("videoId"));Iterator<String> keys=obj.keys();while(keys.hasNext())collectShorts(obj.opt(keys.next()),ids);}
        else if(o instanceof JSONArray){JSONArray a=(JSONArray)o;for(int i=0;i<a.length();i++)collectShorts(a.opt(i),ids);}
    }
    private static void collectVideos(Object o,Set<String> ids)throws Exception{
        if(o instanceof JSONObject){JSONObject j=(JSONObject)o;for(String k:new String[]{"videoRenderer","gridVideoRenderer","watchEndpoint"}){JSONObject v=j.optJSONObject(k);if(v!=null&&v.optString("videoId").matches("[\\w-]{11}"))ids.add(v.getString("videoId"));}JSONObject lock=j.optJSONObject("lockupViewModel");if(lock!=null&&lock.optString("contentId").matches("[\\w-]{11}"))ids.add(lock.getString("contentId"));Iterator<String> keys=j.keys();while(keys.hasNext())collectVideos(j.opt(keys.next()),ids);}
        else if(o instanceof JSONArray){JSONArray a=(JSONArray)o;for(int i=0;i<a.length();i++)collectVideos(a.opt(i),ids);}
    }
    private static Set<String> tabIds(String channelId,String tab)throws Exception{
        String html=Net.text("https://www.youtube.com/channel/"+channelId+"/"+tab,null,null,null),marker="var ytInitialData = ";int start=html.indexOf(marker);Set<String> ids=new HashSet<>();
        if(start>=0){int end=html.indexOf(";</script>",start);JSONObject data=new JSONObject(html.substring(start+marker.length(),end));JSONArray tabs=data.getJSONObject("contents").getJSONObject("twoColumnBrowseResultsRenderer").getJSONArray("tabs");for(int i=0;i<tabs.length();i++){JSONObject t=tabs.getJSONObject(i).optJSONObject("tabRenderer");if(t!=null&&t.optBoolean("selected")&&t.optString("title").toLowerCase(Locale.ROOT).contains(tab)){if(tab.equals("shorts"))collectShorts(t.opt("content"),ids);else collectVideos(t.opt("content"),ids);}}}
        return ids;
    }
    private static List<JSONObject> youtube(Store s,JSONObject c)throws Exception{
        if(c.optString("uploads").isEmpty()){
            String channelId=c.optString("channelId"),slug=c.getString("slug");
            if(c.has("videoId")){JSONArray v=yt(s,"videos","part","snippet","id",c.getString("videoId")).getJSONArray("items");if(v.length()==0)throw new IllegalArgumentException("Vídeo não encontrado.");channelId=v.getJSONObject(0).getJSONObject("snippet").getString("channelId");}
            JSONArray channels=yt(s,"channels","part","snippet,contentDetails",!channelId.isEmpty()||slug.startsWith("UC")?"id":"forHandle",!channelId.isEmpty()?channelId:slug).getJSONArray("items");
            if(channels.length()==0)throw new IllegalArgumentException("Canal do YouTube não encontrado.");
            JSONObject ch=channels.getJSONObject(0);c.put("channelId",ch.getString("id")).put("name",ch.getJSONObject("snippet").getString("title")).put("uploads",ch.getJSONObject("contentDetails").getJSONObject("relatedPlaylists").getString("uploads"));
            Profiles.request(s,c,()->ProfileData.youtube(ch));
        }
        else Profiles.request(s,c,()->{JSONArray profiles=yt(s,"channels","part","snippet","id",c.getString("channelId")).getJSONArray("items");if(profiles.length()==0)throw new IllegalArgumentException("Canal não encontrado.");return ProfileData.youtube(profiles.getJSONObject(0));});
        JSONArray items=yt(s,"playlistItems","part","contentDetails","playlistId",c.getString("uploads"),"maxResults","25").getJSONArray("items");
        Set<String> ids=new LinkedHashSet<>();for(int i=0;i<items.length();i++)ids.add(items.getJSONObject(i).getJSONObject("contentDetails").getString("videoId"));
        JSONArray pending=c.optJSONArray("pending");if(pending!=null)for(int i=0;i<pending.length();i++)if(ids.size()<50)ids.add(pending.getString(i));
        if(ids.isEmpty())return new ArrayList<>();
        JSONArray videos=yt(s,"videos","part","snippet,contentDetails,liveStreamingDetails","id",String.join(",",ids)).getJSONArray("items");
        Set<String> shortIds=new HashSet<>(),videoIds=new HashSet<>();
        try{shortIds=tabIds(c.getString("channelId"),"shorts");}catch(Exception ignored){}
        try{videoIds=tabIds(c.getString("channelId"),"videos");}catch(Exception ignored){}
        JSONArray next=new JSONArray();List<JSONObject> result=new ArrayList<>();
        for(int i=0;i<videos.length();i++){
            JSONObject v=videos.getJSONObject(i);String type=Rules.contentType(v,shortIds,videoIds),id=v.getString("id");if(type.equals("live")||type.equals("agendada"))next.put(id);
            JSONObject snippet=v.getJSONObject("snippet"),detail=v.optJSONObject("liveStreamingDetails");String image="";JSONObject thumbnails=snippet.optJSONObject("thumbnails");if(thumbnails!=null){JSONObject thumb=thumbnails.optJSONObject("medium");if(thumb==null)thumb=thumbnails.optJSONObject("default");if(thumb!=null)image=thumb.optString("url");}
            result.add(new JSONObject().put("key",Rules.eventKey(id,type)).put("contentId",id).put("type",type).put("title",snippet.getString("title")).put("url","https://www.youtube.com/watch?v="+id).put("thumbnail",image).put("publishedAt",snippet.optString("publishedAt")).put("duration",v.getJSONObject("contentDetails").optString("duration")).put("scheduledAt",detail==null?"":detail.optString("scheduledStartTime")).put("viewerCount",detail==null?"":detail.optString("concurrentViewers")).put("classification",type.equals("short")?"Confirmado na aba Shorts do canal":type.equals("video")?"Confirmado na aba Vídeos do canal":type.equals("unknown")?"Novo conteúdo; formato ainda não confirmado. Não foi classificado apenas pela duração.":type.equals("agendada")?"Live ou estreia agendada, identificada pela API":"Transmissão identificada pela API do YouTube"));
        }
        c.put("pending",next);return result;
    }
    private static List<JSONObject> inspect(Store s,JSONObject c)throws Exception{
        String p=c.getString("platform");if(p.equals("youtube"))return youtube(s,c);
        if(p.equals("custom")){
            JSONObject i;synchronized(s){i=new JSONObject(s.find("integrations",c.getString("integrationId")).toString());}
            JSONObject data=Net.json(i.getString("endpoint").replace("{channel}",Net.enc(c.getString("slug"))),i.optString("bearer"),null,null);
            if(!i.optString("avatarPath").isEmpty())Profiles.request(s,c,()->{Object image=Rules.at(data,i.getString("avatarPath"));if(!(image instanceof String))throw new IllegalArgumentException("Campo da foto não encontrado.");return ProfileData.profile(c.optString("name"),(String)image);});
            return Rules.custom(i,c,data);
        }
        String token=token(s,p);List<JSONObject> result=new ArrayList<>();
        if(p.equals("twitch")){
            Profiles.request(s,c,()->ProfileData.twitch(Net.json("https://api.twitch.tv/helix/users?login="+Net.enc(c.getString("slug")),token,creds(s).getString("twitchId"),null)));
            JSONArray streams=Net.json("https://api.twitch.tv/helix/streams?user_login="+Net.enc(c.getString("slug")),token,creds(s).getString("twitchId"),null).getJSONArray("data");
            for(int i=0;i<streams.length();i++){JSONObject v=streams.getJSONObject(i);result.add(new JSONObject().put("key",v.getString("id")).put("type","live").put("title",v.getString("title")).put("url",c.getString("url")).put("thumbnail",v.optString("thumbnail_url").replace("{width}","640").replace("{height}","360")).put("category",v.optString("game_name")).put("viewerCount",v.optString("viewer_count")));}
        }else if(p.equals("kick")){
            JSONArray channels=Net.json("https://api.kick.com/public/v1/channels?slug="+Net.enc(c.getString("slug")),token,null,null).getJSONArray("data");
            if(channels.length()==0)throw new IllegalArgumentException("Canal Kick não encontrado.");JSONObject channel=channels.getJSONObject(0),stream=channel.optJSONObject("stream");
            String broadcaster=String.valueOf(channel.get("broadcaster_user_id"));Profiles.request(s,c,()->ProfileData.kick(Net.json("https://api.kick.com/public/v1/users?id="+Net.enc(broadcaster),token,null,null),broadcaster));
            if(stream!=null&&stream.optBoolean("is_live"))result.add(new JSONObject().put("key",String.valueOf(channel.get("broadcaster_user_id"))+":"+stream.getString("start_time")).put("type","live").put("title",channel.optString("stream_title","Live na Kick")).put("url",c.getString("url")).put("thumbnail",stream.optString("thumbnail")).put("viewerCount",stream.optString("viewer_count")).put("category",channel.optJSONObject("category")==null?"":channel.getJSONObject("category").optString("name")));
        }else throw new IllegalArgumentException("Integração desconhecida.");return result;
    }
    public static void poll(Context context){
        Store s=Store.get(context);List<JSONObject> channels=new ArrayList<>();
        synchronized(s){if(s.running)return;s.running=true;try{JSONArray a=s.state.getJSONArray("channels");for(int i=0;i<a.length();i++)if(!a.getJSONObject(i).optBoolean("paused"))channels.add(new JSONObject(a.getJSONObject(i).toString()));}catch(Exception e){s.running=false;return;}}
        try {
            for(JSONObject copy:channels){
                List<JSONObject> items=null;String error=null;
                try{items=inspect(s,copy);}catch(Exception e){error=e.getMessage()==null?"Falha de conexão. Verifique a internet.":e.getMessage();}
                List<JSONObject> notifications=new ArrayList<>();
                synchronized(s){
                    JSONObject c;try{c=s.find("channels",copy.getString("id"));}catch(Exception ignored){continue;}if(c.optBoolean("paused"))continue;
                    c.put("checkedAt",Instant.now().toString()).put("error",error==null?JSONObject.NULL:error);
                    if(error==null){
                        for(String k:new String[]{"channelId","uploads","pending"})if(copy.has(k))c.put(k,copy.get(k));
                        Set<String> seen=new LinkedHashSet<>();JSONArray a=c.optJSONArray("seen");if(a!=null)for(int i=0;i<a.length();i++)seen.add(a.getString(i).replaceAll("^([\\w-]{11}):(video|short|unknown)$","$1:upload"));
                        JSONArray filters=c.getJSONArray("filters");Set<String> allowed=new HashSet<>();for(int i=0;i<filters.length();i++)allowed.add(filters.getString(i));
                        boolean live=false;for(JSONObject item:items){String key=item.getString("key"),type=item.getString("type");if(type.equals("live"))live=true;
                            if(!seen.contains(key)&&(c.optBoolean("initialized")||type.equals("live"))&&allowed.contains(type)){
                                JSONObject event=new JSONObject(item.toString()).put("id",Rules.id()).put("channelId",c.getString("id")).put("channel",c.optString("name",c.getString("slug"))).put("platform",c.getString("platform")).put("time",Instant.now().toString()).put("acknowledged",false).put("nextReminder",System.currentTimeMillis()+60000).put("reminderCount",0);
                                if(c.has("integrationId"))event.put("integrationId",c.getString("integrationId"));
                                JSONArray old=s.state.getJSONArray("events"),next=new JSONArray();next.put(event);for(int j=0;j<Math.min(299,old.length());j++)next.put(old.get(j));s.state.put("events",next);notifications.add(event);
                            }
                            if(!type.equals("unknown")&&item.has("contentId")){JSONArray history=s.state.getJSONArray("events");for(int j=0;j<history.length();j++){JSONObject event=history.getJSONObject(j);if(item.optString("contentId").equals(event.optString("contentId"))&&event.optString("type").equals("unknown")&&!event.optBoolean("classificationOverride")){event.put("type",type).put("classification",item.optString("classification"));}}}
                            if(!type.equals("unknown")||allowed.contains("unknown")||!c.optBoolean("initialized"))seen.add(key);
                        }
                        List<String> all=new ArrayList<>(seen);JSONArray nextSeen=new JSONArray();for(int i=Math.max(0,all.size()-1000);i<all.size();i++)nextSeen.put(all.get(i));c.put("seen",nextSeen).put("initialized",true).put("live",live);
                    }
                    s.save();
                }
                for(JSONObject event:notifications)Alerts.notifyEvent(context,event,false);
                Reminders.schedule(context);
            }
        } catch(Exception e){android.util.Log.e("LivePulse","Polling failed",e);}
        finally{synchronized(s){s.running=false;try{s.state.put("lastPoll",Instant.now().toString());s.save();}catch(Exception e){android.util.Log.e("LivePulse","State save failed",e);}}}
    }
}
