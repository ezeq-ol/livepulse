package com.livepulse.app;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/** Only the Android application sandbox and Keystore hold user state and credentials. */
public final class Store {
    private static Store instance;
    public final Context context;
    private final AtomicFile file;
    public JSONObject state;
    public volatile boolean running=false;
    public static synchronized Store get(Context c) {
        if(instance==null) instance=new Store(c.getApplicationContext());
        return instance;
    }
    private Store(Context c) {
        context=c; file=new AtomicFile(new File(c.getFilesDir(),"livepulse.enc"));
        try {
            byte[] all=file.readFully();
            if(all.length<29)throw new IOException("Estado inválido");
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,java.util.Arrays.copyOfRange(all,0,12)));
            state=new JSONObject(new String(cipher.doFinal(all,12,all.length-12),"UTF-8"));
        } catch(FileNotFoundException e) { state=fresh(); }
        catch(Exception e) { throw new IllegalStateException("Não foi possível abrir os dados locais. Os arquivos foram preservados.",e); }
        try{PlatformConfig.applyDefaults(state.getJSONObject("credentials"));if(!state.getJSONObject("settings").has("remindersEnabled"))state.getJSONObject("settings").put("remindersEnabled",true);JSONArray channels=state.getJSONArray("channels");for(int i=0;i<channels.length();i++){JSONArray f=channels.getJSONObject(i).getJSONArray("filters");boolean has=false;for(int j=0;j<f.length();j++)if(f.getString(j).equals("unknown"))has=true;if(!has&&state.optInt("schemaVersion")<21)f.put("unknown");}state.put("schemaVersion",21);}catch(Exception e){throw new IllegalStateException(e);}
    }
    private JSONObject fresh() {
        try{return new JSONObject("{\"channels\":[],\"events\":[],\"settings\":{\"interval\":120,\"volume\":75,\"monitorEnabled\":false},\"credentials\":{},\"integrations\":[],\"lastPoll\":null}");}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    private javax.crypto.SecretKey key() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        String alias="livepulse-local-v2";
        if(ks.containsAlias(alias))return (javax.crypto.SecretKey)ks.getKey(alias,null);
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
    public synchronized void save() throws Exception {
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
        byte[] data=cipher.doFinal(state.toString().getBytes("UTF-8"));
        FileOutputStream stream=null;
        try{stream=file.startWrite();stream.write(cipher.getIV());stream.write(data);file.finishWrite(stream);}
        catch(Exception e){if(stream!=null)file.failWrite(stream);throw e;}
    }
    public synchronized JSONObject snapshot() throws Exception {
        JSONObject copy=new JSONObject(state.toString());
        JSONObject creds=copy.getJSONObject("credentials"),configured=new JSONObject();
        for(String k:new String[]{"twitchId","twitchSecret","kickId","kickSecret","youtubeKey"})configured.put(k,!creds.optString(k).isEmpty());
        copy.remove("credentials"); copy.remove("soundUri");copy.remove("oauth");copy.remove("authPending");copy.remove("googlePendingAccount");
        JSONObject accounts=new JSONObject(),oauth=state.optJSONObject("oauth");for(String p:new String[]{"twitch","kick","youtube"})accounts.put(p,oauth!=null&&oauth.optJSONObject(p)!=null&&!oauth.getJSONObject(p).optString("access_token").isEmpty());copy.put("accounts",accounts);
        JSONArray integrations=copy.getJSONArray("integrations");
        for(int i=0;i<integrations.length();i++){JSONObject v=integrations.getJSONObject(i);v.put("hasToken",!v.optString("bearer").isEmpty());v.remove("bearer");}
        copy.put("configured",configured).put("running",running).put("native",true).put("monitorActive",MonitorService.active);
        copy.put("updateSettings",new JSONObject(Updater.settings(this).toString())).put("updates",Updater.status(this));
        return copy;
    }
    public synchronized JSONObject find(String array,String id) throws Exception {
        JSONArray list=state.getJSONArray(array);
        for(int i=0;i<list.length();i++)if(id.equals(list.getJSONObject(i).optString("id")))return list.getJSONObject(i);
        throw new IllegalArgumentException("Item não encontrado.");
    }
    public synchronized void remove(String array,String id) throws Exception {
        JSONArray list=state.getJSONArray(array),next=new JSONArray();
        for(int i=0;i<list.length();i++)if(!id.equals(list.getJSONObject(i).optString("id")))next.put(list.get(i));
        state.put(array,next);
    }
}
