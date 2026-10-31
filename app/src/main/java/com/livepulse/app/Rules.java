package com.livepulse.app;

import org.json.*;
import java.net.*;
import java.util.*;

public final class Rules {
    public static String id(){return UUID.randomUUID().toString();}
    public static String required(JSONObject b,String k,int max) {
        String s=b.optString(k,"").trim();if(s.isEmpty()||s.length()>max)throw new IllegalArgumentException("Preencha "+k+" (até "+max+" caracteres).");return s;
    }
    public static JSONObject channel(String text) throws Exception {
        URL u=Net.https(text);if(u.getPort()!=-1)throw new IllegalArgumentException("Use o endereço público padrão do canal.");
        String host=u.getHost().toLowerCase(Locale.ROOT).replaceFirst("^www\\.","");
        String p=u.getPath().replaceAll("^/+|/+$","");String[] parts=p.split("/");
        JSONObject c=new JSONObject();
        if(host.equals("twitch.tv")||host.equals("kick.com")){
            if(parts.length!=1||!p.matches("[a-zA-Z0-9_-]{1,50}")||Arrays.asList("directory","videos","settings","subscriptions","search","clips").contains(p))throw new IllegalArgumentException("Use o link do canal, não um clipe ou vídeo.");
            p=p.toLowerCase(Locale.ROOT);return c.put("platform",host.equals("kick.com")?"kick":"twitch").put("slug",p).put("url","https://"+host+"/"+p);
        }
        if(host.equals("youtube.com")||host.equals("m.youtube.com")||host.equals("youtu.be")){
            c.put("platform","youtube");String video=null;
            if(host.equals("youtu.be"))video=parts[0];
            else if(parts[0].equals("watch")){
                for(String q:(u.getQuery()==null?"":u.getQuery()).split("&"))if(q.startsWith("v="))video=q.substring(2);
            }else if((parts[0].equals("shorts")||parts[0].equals("live"))&&parts.length>1)video=parts[1];
            if(video!=null){if(!video.matches("[\\w-]{11}"))throw new IllegalArgumentException("ID de vídeo inválido.");return c.put("videoId",video).put("slug",video).put("url","https://www.youtube.com/watch?v="+video);}
            if(parts[0].startsWith("@")&&parts[0].length()>1)return c.put("slug",URLDecoder.decode(parts[0],"UTF-8")).put("url","https://www.youtube.com/"+parts[0]);
            if(parts.length>1&&parts[0].equals("channel")&&parts[1].matches("UC[\\w-]{22}"))return c.put("slug",parts[1]).put("url","https://www.youtube.com/channel/"+parts[1]);
            throw new IllegalArgumentException("YouTube: use /@canal, /channel/UC… ou o link de um vídeo.");
        }
        throw new IllegalArgumentException("Escolha uma integração personalizada para esta plataforma.");
    }
    private static String field(JSONObject b,String k,boolean required) {
        String s=b.optString(k,"").trim();if((required&&s.isEmpty())||s.length()>200||(!s.isEmpty()&&!s.matches("[\\w-]+(?:\\.[\\w-]+)*")))throw new IllegalArgumentException("Campo "+k+" inválido. Exemplo: data.stream.id");return s;
    }
    public static JSONObject integration(JSONObject b,JSONObject previous)throws Exception{
        String endpoint=required(b,"endpoint",2000);Net.https(endpoint.replace("{channel}","canal"));if(!endpoint.contains("{channel}"))throw new IllegalArgumentException("A URL da API precisa conter {channel}.");
        String bearer=b.optString("bearer","").trim();if(bearer.isEmpty()&&previous!=null)bearer=previous.optString("bearer");if(bearer.contains("\n")||bearer.contains("\r"))throw new IllegalArgumentException("Token inválido.");
        JSONObject i=new JSONObject().put("id",previous==null?id():previous.getString("id")).put("name",required(b,"name",60)).put("endpoint",endpoint).put("bearer",bearer).put("liveValue",required(b,"liveValue",100));
        for(String k:new String[]{"livePath","idPath","titlePath","urlPath","avatarPath"})i.put(k,field(b,k,!k.equals("urlPath")&&!k.equals("avatarPath")));
        return i;
    }
    public static Object at(Object obj,String path){
        for(String key:path.split("\\.")){
            if(obj instanceof JSONObject)obj=((JSONObject)obj).opt(key);
            else if(obj instanceof JSONArray&&key.matches("\\d+")){try{obj=((JSONArray)obj).opt(Integer.parseInt(key));}catch(Exception e){return null;}}
            else return null;
        }
        return obj==JSONObject.NULL?null:obj;
    }
    public static String type(JSONObject v,Set<String> shorts){
        String live=v.optJSONObject("snippet").optString("liveBroadcastContent");JSONObject detail=v.optJSONObject("liveStreamingDetails");
        if(live.equals("live")&&(detail==null||detail.optString("actualEndTime").isEmpty()))return "live";
        if(live.equals("upcoming"))return "agendada";
        if(detail!=null)return "replay";
        return shorts.contains(v.optString("id"))?"short":"video";
    }
    public static String contentType(JSONObject v,Set<String> shorts,Set<String> videos){
        String base=type(v,shorts);if(!base.equals("video"))return base;
        return videos.contains(v.optString("id"))?"video":"unknown";
    }
    public static String eventKey(String id,String type){return id+":"+(Arrays.asList("video","short","unknown").contains(type)?"upload":type);}
    public static List<JSONObject> custom(JSONObject integration,JSONObject channel,JSONObject response)throws Exception{
        Object live=at(response,integration.getString("livePath"));if(live==null)throw new IllegalArgumentException("Campo de status não encontrado na API.");
        List<JSONObject> list=new ArrayList<>();if(!String.valueOf(live).equals(integration.getString("liveValue")))return list;
        Object id=at(response,integration.getString("idPath")),title=at(response,integration.getString("titlePath"));
        if(!(id instanceof String||id instanceof Number)||String.valueOf(id).trim().isEmpty())throw new IllegalArgumentException("A API precisa retornar um ID único por transmissão.");
        if(!(title instanceof String)||((String)title).trim().isEmpty())throw new IllegalArgumentException("Título não encontrado na API.");
        String path=integration.optString("urlPath");Object link=path.isEmpty()?channel.getString("url"):at(response,path);if(!(link instanceof String))throw new IllegalArgumentException("Link da live não encontrado.");Net.https((String)link);
        list.add(new JSONObject().put("key",String.valueOf(id)).put("type","live").put("title",title).put("url",link).put("platformName",integration.getString("name")));return list;
    }
}
