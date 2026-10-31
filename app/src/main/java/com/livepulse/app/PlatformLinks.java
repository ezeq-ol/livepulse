package com.livepulse.app;
import java.net.URI;
import java.util.Locale;
/** Only recognized HTTPS platform hosts may select a known official app. */
public final class PlatformLinks {
    public static String packageFor(String address){
        try{
            URI uri=new URI(address);String host=uri.getHost();
            if(!"https".equalsIgnoreCase(uri.getScheme())||host==null||uri.getUserInfo()!=null||(uri.getPort()!=-1&&uri.getPort()!=443))return "";
            host=host.toLowerCase(Locale.ROOT);
            switch(host){
                case "twitch.tv":case "www.twitch.tv":case "m.twitch.tv":return "tv.twitch.android.app";
                case "kick.com":case "www.kick.com":return "com.kick.mobile";
                case "youtube.com":case "www.youtube.com":case "m.youtube.com":case "youtu.be":return "com.google.android.youtube";
                default:return "";
            }
        }catch(Exception ignored){return "";}
    }
}
