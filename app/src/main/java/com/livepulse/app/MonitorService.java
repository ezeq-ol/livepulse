package com.livepulse.app;

import android.app.*;
import android.content.*;
import android.os.*;

public final class MonitorService extends Service {
    public static volatile boolean active=false;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable remind=new Runnable(){public void run(){Reminders.tick(MonitorService.this);handler.postDelayed(this,10000);}};
    private final Runnable tick=new Runnable(){public void run(){
        Store s=Store.get(MonitorService.this);Monitor.enqueue(MonitorService.this);
        int interval=120;synchronized(s){try{interval=s.state.getJSONObject("settings").optInt("interval",120);}catch(Exception ignored){}}
        handler.postDelayed(this,Math.max(60,interval)*1000L);
    }};
    @Override public void onCreate(){super.onCreate();Alerts.channels(this);}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null&&"stop".equals(intent.getAction())){disable(this);stopSelf();return START_NOT_STICKY;}
        try{startForeground(1,Alerts.ongoing(this));active=true;handler.removeCallbacks(tick);handler.post(tick);handler.removeCallbacks(remind);handler.post(remind);}
        catch(Exception e){active=false;stopSelf();}
        return START_NOT_STICKY;
    }
    public static void disable(Context c){
        Store s=Store.get(c);synchronized(s){try{s.state.getJSONObject("settings").put("monitorEnabled",false);s.save();}catch(Exception ignored){}}
        c.getSystemService(android.app.job.JobScheduler.class).cancel(MonitorJob.ID);c.stopService(new Intent(c,MonitorService.class));active=false;
    }
    @Override public void onTimeout(int startId,int fgsType){active=false;handler.removeCallbacks(tick);handler.removeCallbacks(remind);stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}
    @Override public void onDestroy(){active=false;handler.removeCallbacks(tick);handler.removeCallbacks(remind);super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
