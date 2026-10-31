package com.livepulse.app;
import android.app.*;
import android.content.*;
import android.os.SystemClock;
import org.json.*;
import java.util.*;

public final class Reminders {
    public static boolean due(JSONObject e,long now){return !e.optBoolean("acknowledged")&&e.optLong("nextReminder",Long.MAX_VALUE)<=now;}
    public static int notificationId(String id){return 1000+(id.hashCode()&0x3fffffff);}
    public static void tick(Context c){
        Store s=Store.get(c);List<JSONObject> due=new ArrayList<>();
        synchronized(s){try{if(s.state.getJSONObject("settings").optBoolean("remindersEnabled",true)){JSONArray events=s.state.getJSONArray("events");long now=System.currentTimeMillis();for(int i=0;i<events.length();i++){JSONObject e=events.getJSONObject(i);if(due(e,now)){e.put("nextReminder",now+60000).put("reminderCount",e.optInt("reminderCount")+1);due.add(new JSONObject(e.toString()));}}s.save();}}catch(Exception e){android.util.Log.e("LivePulse","Reminder failed",e);}}
        for(JSONObject e:due)Alerts.notifyEvent(c,e,false);
        schedule(c);
    }
    public static void acknowledge(Context c,String id){
        Store s=Store.get(c);synchronized(s){try{JSONArray events=s.state.getJSONArray("events");for(int i=0;i<events.length();i++){JSONObject e=events.getJSONObject(i);if(id.equals("all")||id.equals(e.optString("id"))){e.put("acknowledged",true);c.getSystemService(NotificationManager.class).cancel(notificationId(e.optString("id")));}}s.save();}catch(Exception ignored){}}
        Alerts.cancelSound(c,id);schedule(c);
    }
    private static PendingIntent alarm(Context c){return PendingIntent.getBroadcast(c,88,new Intent(c,AlertActions.class).setAction("remind"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    public static void schedule(Context c){
        Store s=Store.get(c);long next=Long.MAX_VALUE;
        synchronized(s){try{if(s.state.getJSONObject("settings").optBoolean("remindersEnabled",true)){JSONArray a=s.state.getJSONArray("events");for(int i=0;i<a.length();i++){JSONObject e=a.getJSONObject(i);if(!e.optBoolean("acknowledged"))next=Math.min(next,e.optLong("nextReminder",Long.MAX_VALUE));}}}catch(Exception ignored){}}
        AlarmManager a=c.getSystemService(AlarmManager.class);if(next==Long.MAX_VALUE){a.cancel(alarm(c));return;}
        // Inexact by design: Android may batch this in Doze; no exact-alarm exemption requested.
        a.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime()+Math.max(1000,next-System.currentTimeMillis()),alarm(c));
    }
}
