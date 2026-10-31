package com.livepulse.app;
import org.json.*;
import java.util.*;
public final class FeaturesTest{
 static int checks=0;
 static void eq(Object a,Object b){checks++;if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
 interface Action{void run()throws Exception;}
 static void rejects(Action a)throws Exception{checks++;try{a.run();}catch(Exception expected){return;}throw new AssertionError("Expected validation failure");}
 public static void main(String[] args)throws Exception{
  String ch="11111111-1111-1111-1111-111111111111",event="22222222-2222-2222-2222-222222222222";
  JSONObject e=new JSONObject().put("acknowledged",false).put("nextReminder",60000);
  eq(Reminders.due(e,59999),false);eq(Reminders.due(e,60000),true);e.put("acknowledged",true);eq(Reminders.due(e,120000),false);eq(Reminders.due(new JSONObject(),120000),false);
  eq(Reminders.notificationId(event),Reminders.notificationId(event));
  JSONObject rules=new JSONObject().put("default",new JSONObject());eq(SoundBank.select(rules,ch,"live"),"default");rules.put("event:live",new JSONObject());eq(SoundBank.select(rules,ch,"live"),"event:live");rules.put("channel:"+ch+":any",new JSONObject());eq(SoundBank.select(rules,ch,"live"),"channel:"+ch+":any");rules.put("channel:"+ch+":live",new JSONObject());eq(SoundBank.select(rules,ch,"live"),"channel:"+ch+":live");eq(SoundBank.select(rules,ch,"short"),"channel:"+ch+":any");
  rejects(()->SoundBank.scope(new JSONObject().put("scope","../../secret")));
  JSONObject v=new JSONObject("{\"id\":\"abcdefghijk\",\"snippet\":{\"liveBroadcastContent\":\"none\"}}");
  eq(Rules.contentType(v,Collections.emptySet(),Collections.emptySet()),"unknown");eq(Rules.contentType(v,Collections.singleton("abcdefghijk"),Collections.emptySet()),"short");eq(Rules.contentType(v,Collections.emptySet(),Collections.singleton("abcdefghijk")),"video");
  eq(Rules.eventKey("abcdefghijk","unknown"),Rules.eventKey("abcdefghijk","short"));eq(Rules.eventKey("abcdefghijk","video"),Rules.eventKey("abcdefghijk","short"));
  eq(Rules.eventKey("abcdefghijk","live").equals(Rules.eventKey("abcdefghijk","agendada")),false);
  JSONObject channel=Rules.channel("https://twitch.tv/teste").put("id",ch).put("name","Teste").put("filters",new JSONArray("[\"live\"]")).put("seen",new JSONArray());
  JSONObject data=new JSONObject().put("channels",new JSONArray().put(channel)).put("integrations",new JSONArray()).put("events",new JSONArray().put(new JSONObject().put("id",event).put("url","https://twitch.tv/teste").put("acknowledged",false).put("nextReminder",60000))).put("settings",new JSONObject().put("volume",900).put("interval",1).put("monitorEnabled",true)).put("credentials",new JSONObject().put("token","not-for-import"));
  JSONObject backup=new JSONObject().put("format","livepulse-backup").put("version",1).put("data",data),clean=Backup.validate(backup);
  eq(clean.has("credentials"),false);eq(clean.getJSONObject("settings").getBoolean("monitorEnabled"),false);eq(clean.getJSONObject("settings").getBoolean("remindersEnabled"),false);eq(clean.getJSONObject("settings").getInt("volume"),100);eq(clean.getJSONObject("settings").getInt("interval"),60);eq(clean.getJSONArray("events").getJSONObject(0).getBoolean("acknowledged"),true);eq(clean.getJSONArray("events").getJSONObject(0).has("nextReminder"),false);
  rejects(()->Backup.validate(new JSONObject(backup.toString()).put("version",99)));
  channel.put("url","https://twitch.tv.evil.test/a");rejects(()->Backup.validate(backup));
  channel.put("url","https://twitch.tv/teste");data.getJSONArray("events").getJSONObject(0).put("url","javascript:alert(1)");rejects(()->Backup.validate(backup));
  System.out.println("PASS: "+checks+" reminder, sound, backup and YouTube assertions");
 }
}
