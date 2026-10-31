package com.livepulse.app;
import android.accounts.*;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.util.Base64;
import android.widget.Toast;
import org.json.*;
import java.security.*;
import java.util.concurrent.*;
import com.google.android.gms.auth.api.identity.*;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Tasks;
import java.util.Collections;

public final class Auth {
    private static final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor();
    private static final String YOUTUBE_SCOPE="https://www.googleapis.com/auth/youtube.readonly";
    public static final String KICK_REDIRECT=KickOAuth.REDIRECT;
    private static JSONObject accounts(Store s)throws Exception{JSONObject a=s.state.optJSONObject("oauth");if(a==null){a=new JSONObject();s.state.put("oauth",a);}return a;}
    public static void message(Context c,String p,String msg){Store s=Store.get(c);synchronized(s){try{JSONObject a=s.state.optJSONObject("authStatus");if(a==null){a=new JSONObject();s.state.put("authStatus",a);}a.put(p,msg);s.save();}catch(Exception ignored){}}}
    private static String form(String...parts)throws Exception{StringBuilder s=new StringBuilder();for(int i=0;i<parts.length;i+=2){if(i>0)s.append('&');s.append(Net.enc(parts[i])).append('=').append(Net.enc(parts[i+1]));}return s.toString();}
    private static void save(Context c,String p,JSONObject token)throws Exception{
        if(token.optString("access_token").isEmpty())throw new IllegalArgumentException("A plataforma não autorizou a conexão. Confira o cadastro do aplicativo.");
        token.put("expiresAt",System.currentTimeMillis()+Math.max(60,token.optLong("expires_in",3000)-60)*1000L);
        Store s=Store.get(c);synchronized(s){accounts(s).put(p,token);s.save();}message(c,p,"Conta conectada. Você pode monitorar seus canais.");Monitor.clearTokens();
    }
    public static void connect(MainActivity a,String platform){
        if(platform.equals("youtube")){a.chooseGoogle();return;}
        if(!platform.equals("twitch")&&!platform.equals("kick"))return;
        worker.execute(()->{try{
            Store s=Store.get(a);JSONObject credentials;synchronized(s){credentials=new JSONObject(s.state.getJSONObject("credentials").toString());}
            String id=credentials.optString(platform+"Id"),secret=credentials.optString(platform+"Secret");
            if(platform.equals("kick")&&secret.isEmpty()){a.runOnUiThread(()->a.configureKickSecret());return;}
            if(id.isEmpty()){a.runOnUiThread(()->a.showGuide(platform));return;}
            if(platform.equals("twitch")){
                JSONObject device=Net.oauth("https://id.twitch.tv/oauth2/device",form("client_id",id,"scopes",""));if(device.optString("device_code").isEmpty())throw new IllegalArgumentException("Não foi possível gerar o código. Cadastre um aplicativo Twitch do tipo público.");
                device.put("platform","twitch").put("expiresAt",System.currentTimeMillis()+device.optLong("expires_in",600)*1000);
                synchronized(s){s.state.put("authPending",device);s.save();}message(a,platform,"Autorize no navegador. Código: "+device.optString("user_code"));
                String uri=device.getString("verification_uri");if(!Net.https(uri).getHost().equals("www.twitch.tv")&&!Net.https(uri).getHost().equals("id.twitch.tv"))throw new IllegalArgumentException("Endereço de autorização inválido.");a.openExternal(uri);pollTwitch(a.getApplicationContext(),device,id);
            }else{
                byte[] random=new byte[32];new SecureRandom().nextBytes(random);String verifier=Base64.encodeToString(random,Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING),state=Rules.id();String challenge=Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes("US-ASCII")),Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);
                synchronized(s){s.state.put("authPending",new JSONObject().put("platform","kick").put("clientId",id).put("state",state).put("verifier",verifier).put("expiresAt",System.currentTimeMillis()+600000));s.save();}
                message(a,platform,"Autorize sua conta Kick no navegador.");a.openExternal("https://id.kick.com/oauth/authorize?"+form("client_id",id,"response_type","code","redirect_uri",KICK_REDIRECT,"scope","channel:read user:read","state",state,"code_challenge",challenge,"code_challenge_method","S256"));
            }
        }catch(Exception e){message(a,platform,"Conexão não concluída. Confira o cadastro e a internet.");a.showMessage(e.getMessage());}});
    }
    private static void pollTwitch(Context c,JSONObject device,String id){
        int interval=Math.max(5,device.optInt("interval",5));worker.schedule(()->{try{
            Store s=Store.get(c);synchronized(s){JSONObject current=s.state.optJSONObject("authPending");if(current==null||!current.optString("device_code").equals(device.optString("device_code")))return;}
            if(System.currentTimeMillis()>device.getLong("expiresAt")){message(c,"twitch","O código expirou. Toque em Conectar novamente.");return;}
            JSONObject token=Net.oauth("https://id.twitch.tv/oauth2/token",form("client_id",id,"scopes","","device_code",device.getString("device_code"),"grant_type","urn:ietf:params:oauth:grant-type:device_code"));
            if(token.has("access_token")){JSONObject validated=Net.validateTwitch(token.getString("access_token"));if(!id.equals(validated.optString("client_id")))throw new IllegalArgumentException("Client ID não corresponde à autorização.");token.put("validatedAt",System.currentTimeMillis());save(c,"twitch",token);synchronized(s){s.state.remove("authPending");s.save();}return;}
            String error=token.optString("error");if(error.equals("authorization_pending")||token.optString("message").toLowerCase(java.util.Locale.ROOT).contains("pending")){pollTwitch(c,device,id);return;}if(error.equals("slow_down")){device.put("interval",interval+5);pollTwitch(c,device,id);return;}
            message(c,"twitch","Autorização cancelada ou expirada. Toque em Conectar para tentar novamente.");
        }catch(Exception e){message(c,"twitch","Falha de conexão durante a autorização. Tente Conectar novamente.");}},interval,TimeUnit.SECONDS);
    }
    public static void callback(MainActivity a,Uri uri){if(uri==null||!KickOAuth.isAppReturn(uri.toString()))return;
        worker.execute(()->{try{
            Store s=Store.get(a);JSONObject pending,credentials;String code=uri.getQueryParameter("code");
            synchronized(s){
                pending=s.state.optJSONObject("authPending");credentials=new JSONObject(s.state.getJSONObject("credentials").toString());
                KickOAuth.validate(pending,uri.getQueryParameter("state"),System.currentTimeMillis(),credentials.optString("kickId"));
                if(uri.getQueryParameter("error")!=null){s.state.remove("authPending");s.save();throw new IllegalArgumentException("Autorização cancelada.");}
                if(code==null||code.isEmpty()||code.length()>4096)throw new IllegalArgumentException("Código de autorização ausente ou inválido.");
                pending=new JSONObject(pending.toString());s.state.remove("authPending");s.save();
            }
            JSONObject token=Net.oauth("https://id.kick.com/oauth/token",form("grant_type","authorization_code","client_id",credentials.getString("kickId"),"client_secret",credentials.getString("kickSecret"),"redirect_uri",KICK_REDIRECT,"code_verifier",pending.getString("verifier"),"code",code));save(a,"kick",token);a.showMessage("Conta Kick conectada.");
        }catch(Exception e){message(a,"kick","Não foi possível conectar. Verifique o Client ID e o endereço HTTPS de retorno no portal Kick e inicie uma nova conexão.");a.showMessage(e.getMessage());}});
    }
    private static AuthorizationRequest googleRequest(){return AuthorizationRequest.builder().setRequestedScopes(Collections.singletonList(new Scope(YOUTUBE_SCOPE))).build();}
    public static void google(MainActivity a){
        Identity.getAuthorizationClient(a).authorize(googleRequest()).addOnSuccessListener(result->{
            if(result.hasResolution()){try{a.startIntentSenderForResult(result.getPendingIntent().getIntentSender(),51,null,0,0,0);}catch(Exception e){a.showMessage("Não foi possível abrir a autorização Google.");}}
            else googleResult(a,result);
        }).addOnFailureListener(e->{message(a,"youtube","Confira o cadastro OAuth Android, SHA-1, tela de consentimento e YouTube Data API v3 no Google Cloud.");a.showMessage("Conexão Google não concluída. Abra o guia de configuração.");});
    }
    public static void googleResult(MainActivity a,AuthorizationResult result){worker.execute(()->{try{save(a,"youtube",new JSONObject().put("access_token",result.getAccessToken()).put("expires_in",3000));a.showMessage("Conta Google autorizada para YouTube.");}catch(Exception e){a.showMessage("Google não retornou autorização válida.");}});}
    public static void googleCallback(MainActivity a,Intent intent){try{googleResult(a,Identity.getAuthorizationClient(a).getAuthorizationResultFromIntent(intent));}catch(Exception e){message(a,"youtube","Autorização cancelada ou não concluída.");a.showMessage("Autorização Google não concluída.");}}
    public static String token(Context c,String p)throws Exception{
        Store s=Store.get(c);JSONObject auth,credentials;synchronized(s){JSONObject all=accounts(s);if(all.optJSONObject(p)==null)return null;auth=new JSONObject(all.getJSONObject(p).toString());credentials=new JSONObject(s.state.getJSONObject("credentials").toString());}
        if(auth.optLong("expiresAt")>System.currentTimeMillis()){
            if(p.equals("twitch")&&auth.optLong("validatedAt")<System.currentTimeMillis()-3600000){Net.validateTwitch(auth.getString("access_token"));synchronized(s){accounts(s).getJSONObject(p).put("validatedAt",System.currentTimeMillis());s.save();}}
            return auth.getString("access_token");
        }
        if(p.equals("youtube")){AuthorizationResult result=Tasks.await(Identity.getAuthorizationClient(c).authorize(googleRequest()),30,TimeUnit.SECONDS);if(result.hasResolution())throw new IllegalArgumentException("Reconecte o Google em Integrações para renovar a autorização.");String value=result.getAccessToken();auth.put("access_token",value).put("expires_in",3000);save(c,p,auth);return value;}
        String body=form("grant_type","refresh_token","refresh_token",auth.getString("refresh_token"),"client_id",credentials.getString(p+"Id"));if(p.equals("kick"))body+="&client_secret="+Net.enc(credentials.getString("kickSecret"));JSONObject next=Net.oauth(p.equals("kick")?"https://id.kick.com/oauth/token":"https://id.twitch.tv/oauth2/token",body);if(!next.has("refresh_token"))next.put("refresh_token",auth.getString("refresh_token"));if(p.equals("twitch")){Net.validateTwitch(next.getString("access_token"));next.put("validatedAt",System.currentTimeMillis());}save(c,p,next);return next.getString("access_token");
    }
    public static void disconnect(Context c,String p){Store s=Store.get(c);JSONObject token;synchronized(s){try{token=accounts(s).optJSONObject(p);accounts(s).remove(p);s.state.remove("authPending");if(p.equals("youtube"))s.state.remove("googlePendingAccount");s.save();}catch(Exception e){return;}}message(c,p,"Conta desconectada neste aparelho.");worker.execute(()->{try{if(token==null)return;String access=token.getString("access_token");if(p.equals("youtube")){Tasks.await(Identity.getAuthorizationClient(c).revokeAccess(RevokeAccessRequest.builder().setScopes(Collections.singletonList(new Scope(YOUTUBE_SCOPE))).build()),30,TimeUnit.SECONDS);}else if(p.equals("kick"))Net.text("https://id.kick.com/oauth/revoke",null,null,form("token",access,"token_hint_type","access_token"));else{String id;synchronized(s){id=s.state.getJSONObject("credentials").optString("twitchId");}Net.text("https://id.twitch.tv/oauth2/revoke",null,null,form("client_id",id,"token",access));}}catch(Exception e){message(c,p,"Conexão local removida. Sem rede, a revogação deve ser feita nas configurações da plataforma.");}Monitor.clearTokens();});}
}
