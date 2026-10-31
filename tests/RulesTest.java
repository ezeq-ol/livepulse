package com.livepulse.app;
import org.json.*;
import java.util.*;

public final class RulesTest {
    private static int assertions=0;
    static void eq(Object a,Object b){assertions++;if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
    interface Action{void run()throws Exception;}
    static void rejects(Action action)throws Exception{assertions++;try{action.run();}catch(Exception expected){return;}throw new AssertionError("Expected failure");}
    public static void main(String[] args)throws Exception{
        eq(Rules.channel("https://www.twitch.tv/MeuCanal").getString("slug"),"meucanal");
        eq(Rules.channel("https://kick.com/canal").getString("platform"),"kick");
        rejects(()->Rules.channel("https://twitch.tv/canal/clip/abc"));
        rejects(()->Rules.channel("https://kick.com.evil.test/canal"));
        rejects(()->Rules.channel("http://kick.com/canal"));
        rejects(()->Rules.channel("https://user:pass@kick.com/canal"));
        eq(Rules.channel("https://youtube.com/@canal/live").getString("slug"),"@canal");
        eq(Rules.channel("https://youtu.be/abcdefghijk").getString("videoId"),"abcdefghijk");
        eq(Rules.channel("https://youtube.com/shorts/abcdefghijk").getString("videoId"),"abcdefghijk");
        rejects(()->Rules.channel("https://youtube.com/watch?v=short"));
        JSONObject v=new JSONObject("{\"id\":\"a\",\"snippet\":{\"liveBroadcastContent\":\"live\"}}");
        eq(Rules.type(v,Collections.emptySet()),"live");v.getJSONObject("snippet").put("liveBroadcastContent","upcoming");eq(Rules.type(v,Collections.emptySet()),"agendada");
        v.getJSONObject("snippet").put("liveBroadcastContent","none");eq(Rules.type(v,Collections.singleton("a")),"short");eq(Rules.type(v,Collections.emptySet()),"video");
        v.put("liveStreamingDetails",new JSONObject().put("actualEndTime","2026-10-30"));eq(Rules.type(v,Collections.singleton("a")),"replay");
        JSONObject config=new JSONObject().put("name","Outra plataforma").put("endpoint","https://api.example.com/{channel}").put("livePath","data.is_live").put("liveValue","true").put("idPath","data.stream.id").put("titlePath","data.stream.title").put("urlPath","");
        JSONObject integration=Rules.integration(config,null);eq(integration.getString("name"),"Outra plataforma");
        JSONObject previous=new JSONObject(integration.toString()).put("bearer","segredo");eq(Rules.integration(config,previous).getString("bearer"),"segredo");
        JSONObject channel=new JSONObject().put("url","https://example.com/canal");
        JSONObject response=new JSONObject("{\"data\":{\"is_live\":true,\"stream\":{\"id\":123,\"title\":\"Live nova\"}}}");
        List<JSONObject> events=Rules.custom(integration,channel,response);eq(events.size(),1);eq(events.get(0).getString("key"),"123");eq(events.get(0).getString("url"),"https://example.com/canal");
        response.getJSONObject("data").put("is_live",false);eq(Rules.custom(integration,channel,response).size(),0);
        rejects(()->Rules.custom(integration,channel,new JSONObject()));
        response.getJSONObject("data").put("is_live",true).getJSONObject("stream").remove("id");rejects(()->Rules.custom(integration,channel,response));
        eq(Rules.at(new JSONObject("{\"data\":[{\"id\":42}]}"),"data.0.id"),42);
        eq(Net.enc("canal/á"),"canal%2F%C3%A1");
        rejects(()->Net.https("javascript:alert(1)"));
        System.out.println("PASS: "+assertions+" native Android rule assertions");
    }
}
