package com.livepulse.app;

import org.json.*;
import javax.net.ssl.HttpsURLConnection;
import java.net.*;
import java.io.*;
import java.util.*;

public final class Net {
    public static String enc(String s) throws Exception{return URLEncoder.encode(s,"UTF-8").replace("+","%20");}
    public static URL https(String text) throws Exception {
        URL u=new URL(text);
        if(!"https".equals(u.getProtocol())||u.getUserInfo()!=null||u.getHost().isEmpty())throw new IllegalArgumentException("Use uma URL HTTPS sem usuário ou senha.");
        return u;
    }
    public static String text(String url,String bearer,String clientId,String form) throws Exception {
        HttpsURLConnection c=(HttpsURLConnection)https(url).openConnection();
        c.setConnectTimeout(12000);c.setReadTimeout(15000);c.setInstanceFollowRedirects(false);
        c.setRequestProperty("Accept","application/json, text/html");c.setRequestProperty("Accept-Language","en-US");
        if(bearer!=null&&!bearer.isEmpty())c.setRequestProperty("Authorization","Bearer "+bearer);
        if(clientId!=null)c.setRequestProperty("Client-Id",clientId);
        try {
            if(form!=null){c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/x-www-form-urlencoded");try(OutputStream os=c.getOutputStream()){os.write(form.getBytes("UTF-8"));}}
            int status=c.getResponseCode();if(status<200||status>=300)throw new IOException("API retornou "+status+". Confira credenciais, endereço e cota.");
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] buffer=new byte[8192];int n,total=0;
                while((n=in.read(buffer))!=-1){total+=n;if(total>8*1024*1024)throw new IOException("Resposta da API muito grande.");out.write(buffer,0,n);}
                return out.toString("UTF-8");
            }
        }finally{c.disconnect();}
    }
    public static JSONObject json(String url,String bearer,String clientId,String form)throws Exception{return new JSONObject(text(url,bearer,clientId,form));}
    public static JSONObject oauth(String url,String form)throws Exception{
        HttpsURLConnection c=(HttpsURLConnection)https(url).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(12000);c.setReadTimeout(15000);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
        try{try(OutputStream out=c.getOutputStream()){out.write(form.getBytes("UTF-8"));}int code=c.getResponseCode();InputStream input=code>=200&&code<300?c.getInputStream():c.getErrorStream();if(input==null)throw new IOException("Autorização retornou "+code);try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(out.size()+n>65536)throw new IOException("Resposta de autorização inválida.");out.write(buf,0,n);}return new JSONObject(out.toString("UTF-8"));}}finally{c.disconnect();}
    }
    public static JSONObject validateTwitch(String token)throws Exception{
        HttpsURLConnection c=(HttpsURLConnection)https("https://id.twitch.tv/oauth2/validate").openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(12000);c.setReadTimeout(12000);c.setRequestProperty("Authorization","OAuth "+token);
        try{if(c.getResponseCode()!=200)throw new IOException("Sessão Twitch expirada.");try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>65536)throw new IOException("Resposta inválida.");out.write(b,0,n);}return new JSONObject(out.toString("UTF-8"));}}finally{c.disconnect();}
    }
    public static String query(String base,Map<String,String> params)throws Exception{
        StringBuilder url=new StringBuilder(base);url.append('?');
        for(Map.Entry<String,String> e:params.entrySet())url.append(enc(e.getKey())).append('=').append(enc(e.getValue())).append('&');
        return url.toString();
    }
    public static Map<String,String> params(String...values){Map<String,String> m=new LinkedHashMap<>();for(int i=0;i<values.length;i+=2)m.put(values[i],values[i+1]);return m;}
}
