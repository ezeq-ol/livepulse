package com.livepulse.app;
import org.json.JSONObject;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
public final class KickOAuth {
    public static final String REDIRECT="https://ezeq-ol.github.io/livepulse/kick-callback.html";
    public static final String APP_RETURN="livepulse://oauth/kick";
    public static boolean isAppReturn(String address){
        try{URI uri=new URI(address);return "livepulse".equals(uri.getScheme())&&"oauth".equals(uri.getRawAuthority())&&"/kick".equals(uri.getRawPath())&&uri.getRawFragment()==null;}catch(Exception e){return false;}
    }
    public static void validate(JSONObject pending,String state,long now,String clientId){
        if(pending==null||!"kick".equals(pending.optString("platform"))||state==null||state.isEmpty()||state.length()>512||pending.optLong("expiresAt")<=now||clientId==null||clientId.isEmpty()||!clientId.equals(pending.optString("clientId"))||!MessageDigest.isEqual(state.getBytes(StandardCharsets.UTF_8),pending.optString("state").getBytes(StandardCharsets.UTF_8)))throw new IllegalArgumentException("Retorno de autorização inválido ou expirado. Inicie uma nova conexão Kick.");
    }
}
