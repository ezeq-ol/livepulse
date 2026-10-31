package com.livepulse.app;
import android.content.Context;
import android.util.Base64;
import org.json.*;
import java.io.*;

public final class SoundBank {
    public static String previewMime(byte[] data){
        String head=new String(data,0,Math.min(12,data.length),java.nio.charset.StandardCharsets.ISO_8859_1);
        if(head.startsWith("RIFF")&&head.length()>=12&&head.substring(8).equals("WAVE"))return "audio/wav";
        if(head.startsWith("OggS"))return "audio/ogg";if(head.startsWith("fLaC"))return "audio/flac";
        if(head.length()>=8&&head.substring(4,8).equals("ftyp"))return "audio/mp4";
        if(data.length>=2&&(data[0]&255)==255&&((data[1]&246)==240))return "audio/aac";
        return "audio/mpeg";
    }
    public static String scope(JSONObject b){
        String s=b.optString("scope","default");
        if(!s.matches("default|event:(live|video|short|agendada|replay|unknown)|channel:[a-f0-9-]{36}:(any|live|video|short|agendada|replay|unknown)"))throw new IllegalArgumentException("Escolha um canal ou tipo de evento válido.");return s;
    }
    public static JSONObject rules(Store s)throws Exception{JSONObject rules=s.state.optJSONObject("soundRules");if(rules==null){rules=new JSONObject();s.state.put("soundRules",rules);}return rules;}
    public static JSONObject preview(Context c,Store s,JSONObject b)throws Exception{
        String scope=scope(b);JSONObject options=new JSONObject(rules(s).toString());boolean exclude=b.optBoolean("excludeSelected");if(exclude)options.remove(scope);
        String[] parts=scope.split(":");String key=parts.length==3?select(options,parts[1],parts[2]):parts.length==2?select(options,"",parts[1]):options.has("default")?"default":"";
        File audio=key.isEmpty()?null:file(c,options.getJSONObject(key));String name=key.isEmpty()?"Pulse · som original":options.getJSONObject(key).optString("name");
        if((audio==null||!audio.isFile())&&!(exclude&&scope.equals("default"))&&s.state.has("soundName")){File legacy=new File(c.getFilesDir(),"custom-sound.bin");if(legacy.isFile()){audio=legacy;name=s.state.optString("soundName");}}
        if(audio==null||!audio.isFile())return new JSONObject().put("data",JSONObject.NULL).put("name","Pulse · som original");
        if(audio.length()>5*1024*1024)throw new IllegalArgumentException("Arquivo de som maior que o permitido.");
        try(InputStream in=new FileInputStream(audio);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>5*1024*1024)throw new IOException("Áudio muito grande.");out.write(buffer,0,n);}byte[] bytes=out.toByteArray();return new JSONObject().put("data","data:"+previewMime(bytes)+";base64,"+Base64.encodeToString(bytes,Base64.NO_WRAP)).put("name",name);}
    }
    public static String select(JSONObject rules,String channelId,String type){
        for(String key:new String[]{"channel:"+channelId+":"+type,"channel:"+channelId+":any","event:"+type,"default"})if(rules.has(key))return key;
        return "";
    }
    public static File file(Context c,JSONObject rule){String name=rule.optString("file");if(!name.matches("audio-[a-f0-9-]{36}\\.bin"))throw new IllegalArgumentException("Referência de áudio inválida.");return new File(c.getFilesDir(),name);}
    public static JSONObject put(Context c,Store s,JSONObject b)throws Exception{
        String scope=scope(b),data=b.optString("data");if(rules(s).length()>=64&&!rules(s).has(scope))throw new IllegalArgumentException("Máximo de 64 regras de som. Remova uma opção para cadastrar outra.");if(!data.matches("^data:audio/[a-zA-Z0-9.+-]+;base64,[A-Za-z0-9+/=]+$"))throw new IllegalArgumentException("Escolha um arquivo de áudio válido.");
        byte[] bytes=Base64.decode(data.substring(data.indexOf(',')+1),Base64.DEFAULT);if(bytes.length==0||bytes.length>5*1024*1024)throw new IllegalArgumentException("O áudio deve ter entre 1 byte e 5 MB.");
        String filename="audio-"+Rules.id()+".bin";android.util.AtomicFile file=new android.util.AtomicFile(new File(c.getFilesDir(),filename));FileOutputStream out=null;
        try{out=file.startWrite();out.write(bytes);file.finishWrite(out);}catch(Exception e){if(out!=null)file.failWrite(out);throw e;}
        JSONObject rule=new JSONObject().put("name",b.optString("name","Som personalizado").substring(0,Math.min(200,b.optString("name","Som personalizado").length()))).put("file",filename);
        rules(s).put(scope,rule);s.save();return new JSONObject().put("ok",true).put("name",rule.getString("name"));
    }
    public static File resolve(Context c,Store s,JSONObject event)throws Exception{
        JSONObject rules=rules(s);String key=select(rules,event.optString("channelId"),event.optString("type"));
        if(!key.isEmpty()){File f=file(c,rules.getJSONObject(key));if(f.exists())return f;}
        File legacy=new File(c.getFilesDir(),"custom-sound.bin");return s.state.has("soundName")&&legacy.exists()?legacy:null;
    }
}
