package com.livepulse.app;
import org.json.*;
public final class ProfileData {
    public static JSONObject twitch(JSONObject response)throws Exception{
        JSONArray data=response.getJSONArray("data");if(data.length()==0)throw new IllegalArgumentException("Perfil Twitch não encontrado.");JSONObject user=data.getJSONObject(0);
        return profile(user.optString("display_name"),user.optString("profile_image_url"));
    }
    public static JSONObject kick(JSONObject response,String expectedId)throws Exception{
        JSONArray data=response.getJSONArray("data");for(int i=0;i<data.length();i++){JSONObject user=data.getJSONObject(i);if(expectedId.equals(String.valueOf(user.opt("user_id"))))return profile(user.optString("name"),user.optString("profile_picture"));}
        throw new IllegalArgumentException("A Kick não retornou o perfil solicitado.");
    }
    public static JSONObject youtube(JSONObject channel)throws Exception{
        JSONObject snippet=channel.getJSONObject("snippet"),thumbnails=snippet.optJSONObject("thumbnails");String url="";
        if(thumbnails!=null)for(String size:new String[]{"medium","high","default"}){JSONObject thumb=thumbnails.optJSONObject(size);if(thumb!=null&&!thumb.optString("url").isEmpty()){url=thumb.getString("url");break;}}
        return profile(snippet.optString("title"),url);
    }
    public static JSONObject profile(String name,String url)throws Exception{
        if(!url.isEmpty())Net.https(url);return new JSONObject().put("name",name).put("image",url);
    }
    public static boolean due(JSONObject channel,boolean hasFile,long now){
        if(channel.optLong("profileRetryAt")>now)return false;
        return !hasFile||channel.optLong("profileCheckedAt")<now-24*60*60*1000L;
    }
}
