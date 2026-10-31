package com.livepulse.app;
import org.json.JSONObject;
public final class KickOAuthTest{
    static int checks;
    static void check(boolean ok){checks++;if(!ok)throw new AssertionError("Kick OAuth validation failed");}
    static JSONObject pending()throws Exception{return new JSONObject().put("platform","kick").put("state","random-state-123").put("clientId","client-id").put("expiresAt",2000);}
    static void reject(JSONObject p,String state,long now,String clientId){checks++;try{KickOAuth.validate(p,state,now,clientId);}catch(IllegalArgumentException e){return;}throw new AssertionError("Expected rejection");}
    public static void main(String[] args)throws Exception{
        check(KickOAuth.isAppReturn("livepulse://oauth/kick?code=abc&state=xyz"));
        for(String url:new String[]{null,"","https://livepulse://oauth/kick","https://ezeq-ol.github.io/livepulse/kick-callback.html?code=abc","livepulse://user@oauth/kick","livepulse://oauth:80/kick","livepulse://oauth/kick/","livepulse://oauth/other","livepulse://oauth/kick#fragment","livepulse://oauth.evil/kick"})check(!KickOAuth.isAppReturn(url));
        KickOAuth.validate(pending(),"random-state-123",1000,"client-id");check(true);
        reject(null,"random-state-123",1000,"client-id");reject(pending(),null,1000,"client-id");reject(pending(),"wrong",1000,"client-id");reject(pending(),"random-state-123",2000,"client-id");reject(pending(),"random-state-123",2001,"client-id");reject(pending(),"random-state-123",1000,"different-id");reject(pending().put("platform","twitch"),"random-state-123",1000,"client-id");JSONObject missing=pending();missing.remove("clientId");reject(missing,"random-state-123",1000,"client-id");
        check(KickOAuth.REDIRECT.startsWith("https://"));check(KickOAuth.APP_RETURN.equals("livepulse://oauth/kick"));System.out.println("KickOAuthTest: "+checks+" verificacoes aprovadas.");
    }
}
