package com.livepulse.app;
import android.app.job.*;
public final class UpdateJob extends JobService{
    public static final int ID=47839;
    @Override public boolean onStartJob(JobParameters params){Updater.check(this,false,()->jobFinished(params,false));return true;}
    @Override public boolean onStopJob(JobParameters params){return true;}
}
