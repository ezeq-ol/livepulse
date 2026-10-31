package com.livepulse.app;
import java.util.Objects;
public final class PlatformLinksTest{
    private static int checks=0;
    private static void eq(String url,String expected){checks++;String actual=PlatformLinks.packageFor(url);if(!Objects.equals(actual,expected))throw new AssertionError(url+": "+actual);}
    public static void main(String[] args){
        eq("https://twitch.tv/gordaopd","tv.twitch.android.app");eq("https://www.twitch.tv/gordaopd","tv.twitch.android.app");eq("https://m.twitch.tv/gordaopd","tv.twitch.android.app");eq("https://twitch.tv/videos/12345?t=1m","tv.twitch.android.app");
        eq("https://kick.com/canal","com.kick.mobile");eq("https://www.kick.com/canal","com.kick.mobile");
        eq("https://www.youtube.com/watch?v=abcdefghijk&t=20","com.google.android.youtube");eq("https://youtube.com/@canal/live","com.google.android.youtube");eq("https://m.youtube.com/shorts/abcdefghijk","com.google.android.youtube");eq("https://youtu.be/abcdefghijk","com.google.android.youtube");eq("HTTPS://WWW.YOUTUBE.COM/watch?v=abcdefghijk","com.google.android.youtube");eq("https://kick.com:443/canal","com.kick.mobile");
        for(String url:new String[]{null,"","bad url","http://twitch.tv/canal","javascript:alert(1)","intent://canal","https://twitch.tv.evil.test/canal","https://evil-twitch.tv/canal","https://twitch.tv@evil.test/canal","https://user@twitch.tv/canal","https://twitch.tv:8080/canal","https://id.twitch.tv/oauth2/authorize","https://accounts.google.com/","https://example.com/canal","https:///kick.com/canal"})eq(url,"");
        System.out.println("PlatformLinksTest: "+checks+" verificacoes aprovadas.");
    }
}
