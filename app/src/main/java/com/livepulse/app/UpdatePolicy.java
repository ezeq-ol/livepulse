package com.livepulse.app;
import org.json.*;
public final class UpdatePolicy {
    public static final long MAX_APK_BYTES=150L*1024*1024;
    public static JSONObject manifest(JSONObject input,String packageName)throws Exception{
        if(input.optInt("schemaVersion")!=1)throw new IllegalArgumentException("Formato de atualização incompatível.");
        if(!packageName.equals(input.optString("packageName")))throw new IllegalArgumentException("A atualização é de outro aplicativo.");
        long code=input.getLong("versionCode"),size=input.getLong("sizeBytes");if(code<1||code>Integer.MAX_VALUE||size<1||size>MAX_APK_BYTES)throw new IllegalArgumentException("Versão ou tamanho inválido.");
        String url=input.getString("apkUrl"),hash=input.getString("sha256").toLowerCase(java.util.Locale.ROOT),name=input.getString("versionName");Net.https(url);if(!hash.matches("[a-f0-9]{64}")||name.isEmpty()||name.length()>100)throw new IllegalArgumentException("Dados da atualização inválidos.");
        String notes=input.optString("releaseNotes","");if(notes.length()>6000)throw new IllegalArgumentException("Notas de atualização muito longas.");int minSdk=input.optInt("minSdk",26);if(minSdk<1||minSdk>1000)throw new IllegalArgumentException("Requisito de Android inválido.");
        return new JSONObject().put("schemaVersion",1).put("packageName",packageName).put("versionCode",code).put("versionName",name).put("apkUrl",url).put("sha256",hash).put("sizeBytes",size).put("releaseNotes",notes).put("minSdk",minSdk);
    }
    public static boolean newer(JSONObject manifest,long installed,int sdk){return manifest.optLong("versionCode")>installed&&manifest.optInt("minSdk",26)<=sdk;}
    public static boolean matches(String expected,String actual){return expected!=null&&actual!=null&&expected.matches("(?i)[a-f0-9]{64}")&&java.security.MessageDigest.isEqual(expected.toLowerCase(java.util.Locale.ROOT).getBytes(java.nio.charset.StandardCharsets.US_ASCII),actual.toLowerCase(java.util.Locale.ROOT).getBytes(java.nio.charset.StandardCharsets.US_ASCII));}
    public static boolean due(JSONObject settings,long now,boolean force){return !settings.optString("feedUrl").isEmpty()&&(force||(settings.optBoolean("enabled",true)&&now-settings.optLong("checkedAt")>=15L*60*1000));}
}
