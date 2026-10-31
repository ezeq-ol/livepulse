package com.livepulse.app;
import org.json.*;
import java.util.*;
public final class ProfileTest{
 static int checks=0;static void eq(Object a,Object b){checks++;if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}interface Action{void run()throws Exception;}static void rejects(Action a)throws Exception{checks++;try{a.run();}catch(Exception e){return;}throw new AssertionError("Expected failure");}
 public static void main(String[] args)throws Exception{
  JSONObject twitch=new JSONObject("{\"data\":[{\"display_name\":\"Canal Teste\",\"profile_image_url\":\"https://static-cdn.jtvnw.net/photo.png\",\"offline_image_url\":\"https://example.com/banner.png\"}]}");
  eq(ProfileData.twitch(twitch).getString("image"),"https://static-cdn.jtvnw.net/photo.png");eq(ProfileData.twitch(twitch).getString("name"),"Canal Teste");rejects(()->ProfileData.twitch(new JSONObject("{\"data\":[]}")));
  JSONObject kick=new JSONObject("{\"data\":[{\"user_id\":9,\"name\":\"Outra pessoa\",\"profile_picture\":\"https://example.com/other.png\"},{\"user_id\":42,\"name\":\"Kick Teste\",\"profile_picture\":\"https://example.com/profile.jpg\"}]}");
  eq(ProfileData.kick(kick,"42").getString("name"),"Kick Teste");eq(ProfileData.kick(kick,"42").getString("image"),"https://example.com/profile.jpg");rejects(()->ProfileData.kick(kick,"100"));
  JSONObject youtube=new JSONObject("{\"snippet\":{\"title\":\"YouTube Teste\",\"thumbnails\":{\"default\":{\"url\":\"https://yt3.googleusercontent.com/small\"},\"medium\":{\"url\":\"https://yt3.googleusercontent.com/profile\"}}}}");
  eq(ProfileData.youtube(youtube).getString("image"),"https://yt3.googleusercontent.com/profile");eq(ProfileData.youtube(youtube).getString("name"),"YouTube Teste");
  rejects(()->ProfileData.profile("Unsafe","http://example.com/profile.png"));rejects(()->ProfileData.profile("Unsafe","javascript:alert(1)"));
  long now=200000000L;JSONObject channel=new JSONObject().put("profileCheckedAt",now-1000);
  eq(ProfileData.due(channel,true,now),false);eq(ProfileData.due(channel,false,now),true);channel.put("profileCheckedAt",now-86400001L);eq(ProfileData.due(channel,true,now),true);channel.put("profileRetryAt",now+5000);eq(ProfileData.due(channel,false,now),false);
  String id="11111111-1111-1111-1111-111111111111";JSONObject sounds=new JSONObject().put("default",new JSONObject()).put("channel:"+id+":any",new JSONObject());eq(SoundBank.select(sounds,id,"live"),"channel:"+id+":any");eq(SoundBank.select(sounds,id,"short"),"channel:"+id+":any");eq(SoundBank.select(sounds,"22222222-2222-2222-2222-222222222222","live"),"default");
  System.out.println("PASS: "+checks+" profile, cache and channel sound assertions");
 }
}
