package com.livepulse.app;
import android.content.*;
public final class ReminderBoot extends BroadcastReceiver{public void onReceive(Context c,Intent i){if(Intent.ACTION_BOOT_COMPLETED.equals(i.getAction()))Reminders.schedule(c);}}
