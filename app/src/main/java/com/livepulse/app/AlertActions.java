package com.livepulse.app;
import android.content.*;
public final class AlertActions extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        if("ack".equals(i.getAction())){Reminders.acknowledge(c,i.getStringExtra("eventId"));return;}
        if("android.intent.action.BOOT_COMPLETED".equals(i.getAction())){Reminders.schedule(c);return;}
        if("remind".equals(i.getAction())){PendingResult result=goAsync();new Thread(()->{try{Reminders.tick(c);}finally{result.finish();}},"livepulse-reminders").start();}
    }
}
