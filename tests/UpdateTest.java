package com.livepulse.app;
import org.json.*;
import java.util.*;
public final class UpdateTest {
    static int checks=0;
    static void eq(Object a,Object b){checks++;if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
    interface Action{void run()throws Exception;}
    static void rejects(Action a)throws Exception{checks++;try{a.run();}catch(Exception e){return;}throw new AssertionError("Expected rejection");}
    static JSONObject release()throws Exception{return new JSONObject().put("schemaVersion",1).put("packageName","com.livepulse.app").put("versionCode",20400).put("versionName","2.4.0").put("sizeBytes",1000).put("sha256",String.join("",Collections.nCopies(64,"A"))).put("apkUrl","https://example.com/release.apk").put("minSdk",26);}
    public static void main(String[] args)throws Exception{
        JSONObject r=UpdatePolicy.manifest(release(),"com.livepulse.app");eq(r.getString("sha256"),String.join("",Collections.nCopies(64,"a")));
        eq(UpdatePolicy.newer(r,20300,26),true);eq(UpdatePolicy.newer(r,20400,35),false);eq(UpdatePolicy.newer(r,20500,35),false);eq(UpdatePolicy.newer(r,20300,25),false);
        rejects(()->UpdatePolicy.manifest(release().put("packageName","other.app"),"com.livepulse.app"));rejects(()->UpdatePolicy.manifest(release().put("schemaVersion",2),"com.livepulse.app"));rejects(()->UpdatePolicy.manifest(release().put("sizeBytes",0),"com.livepulse.app"));rejects(()->UpdatePolicy.manifest(release().put("sizeBytes",UpdatePolicy.MAX_APK_BYTES+1),"com.livepulse.app"));rejects(()->UpdatePolicy.manifest(release().put("versionCode",0),"com.livepulse.app"));rejects(()->UpdatePolicy.manifest(release().put("versionCode",2147483648L),"com.livepulse.app"));rejects(()->UpdatePolicy.manifest(release().put("sha256","abc"),"com.livepulse.app"));rejects(()->UpdatePolicy.manifest(release().put("apkUrl","http://example.com/a.apk"),"com.livepulse.app"));rejects(()->UpdatePolicy.manifest(release().put("apkUrl","https://user:pass@example.com/a.apk"),"com.livepulse.app"));rejects(()->UpdatePolicy.manifest(release().put("minSdk",-1),"com.livepulse.app"));rejects(()->UpdatePolicy.manifest(release().put("versionName",""),"com.livepulse.app"));
        eq(UpdatePolicy.matches(r.getString("sha256"),r.getString("sha256").toUpperCase(Locale.ROOT)),true);eq(UpdatePolicy.matches(r.getString("sha256"),"wrong"),false);eq(UpdatePolicy.matches(null,""),false);eq(UpdatePolicy.matches("",null),false);
        JSONObject p=new JSONObject().put("feedUrl","").put("enabled",true);eq(UpdatePolicy.due(p,2000000,true),false);p.put("feedUrl","https://example.com/updates.json").put("checkedAt",1500000);eq(UpdatePolicy.due(p,2000000,false),false);eq(UpdatePolicy.due(p,2400000,false),true);p.put("enabled",false);eq(UpdatePolicy.due(p,2400000,false),false);eq(UpdatePolicy.due(p,2400000,true),true);
        eq(NotificationIcons.small("twitch"),R.drawable.ic_twitch_notification);eq(NotificationIcons.small("youtube"),R.drawable.ic_youtube_notification);eq(NotificationIcons.small("kick"),R.drawable.ic_kick_notification);eq(NotificationIcons.small("custom"),R.drawable.ic_custom_notification);eq(NotificationIcons.small("other"),R.drawable.ic_notification);eq(NotificationIcons.color("twitch"),0xff9146ff);eq(NotificationIcons.color("youtube"),0xffef4f58);eq(NotificationIcons.color("kick"),0xff53fc18);
        System.out.println("UpdateTest: "+checks+" verificações aprovadas.");
    }
}
