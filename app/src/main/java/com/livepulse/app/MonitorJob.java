package com.livepulse.app;

import android.app.job.*;
import android.content.*;

/** Battery-friendly fallback after Android ends an active monitoring session. */
public final class MonitorJob extends JobService {
    public static final int ID=47831;
    public static void schedule(Context c){
        JobInfo job=new JobInfo.Builder(ID,new ComponentName(c,MonitorJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(15*60*1000L).setPersisted(true).build();
        c.getSystemService(JobScheduler.class).schedule(job);
    }
    @Override public boolean onStartJob(JobParameters params){
        Store s=Store.get(this);boolean enabled;synchronized(s){enabled=s.state.optJSONObject("settings").optBoolean("monitorEnabled");}
        if(!enabled||MonitorService.active)return false;
        Monitor.executor.execute(()->{Monitor.poll(this);jobFinished(params,false);});return true;
    }
    @Override public boolean onStopJob(JobParameters params){return true;}
}
