package com.livepulse.app;
import android.content.Context;
import android.graphics.*;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import java.util.Set;
import java.util.concurrent.*;

public final class Profiles {
    private static final ExecutorService executor=Executors.newFixedThreadPool(2);
    private static final Set<String> pending=ConcurrentHashMap.newKeySet();
    public static File file(Context c,String id){if(!id.matches("[a-f0-9-]{36}"))throw new IllegalArgumentException("ID de canal inválido.");return new File(c.getFilesDir(),"avatar-"+id+".jpg");}
    public static void request(Store store,JSONObject copy,Callable<JSONObject> metadata)throws Exception{
        String id=copy.getString("id");synchronized(store){JSONObject c;try{c=store.find("channels",id);}catch(Exception e){return;}if(!ProfileData.due(c,file(store.context,id).exists(),System.currentTimeMillis())||!pending.add(id))return;c.put("profileLoading",true);}
        executor.execute(()->{try{
            JSONObject profile=metadata.call();String image=profile.optString("image");
            synchronized(store){if(!profile.optString("name").isEmpty())store.find("channels",id).put("name",profile.getString("name"));}
            if(image.isEmpty())throw new IOException("A plataforma não forneceu uma foto de perfil.");
            byte[] jpeg=download(image);
            synchronized(store){JSONObject c=store.find("channels",id);AtomicFile target=new AtomicFile(file(store.context,id));FileOutputStream stream=null;try{stream=target.startWrite();stream.write(jpeg);target.finishWrite(stream);}catch(Exception e){if(stream!=null)target.failWrite(stream);throw e;}
                c.put("avatarVersion",System.currentTimeMillis()).put("profileCheckedAt",System.currentTimeMillis()).put("profileLoading",false).put("profileError",JSONObject.NULL).remove("profileRetryAt");
                if(!profile.optString("name").isEmpty())c.put("name",profile.getString("name"));store.save();
            }
        }catch(Exception e){synchronized(store){try{JSONObject c=store.find("channels",id);c.put("profileLoading",false).put("profileError","Foto indisponível nesta consulta. Confira as permissões da integração.").put("profileRetryAt",System.currentTimeMillis()+15*60*1000);store.save();}catch(Exception ignored){}}}finally{pending.remove(id);}});
    }
    private static byte[] download(String address)throws Exception{
        URL url=Net.https(address);
        for(int redirect=0;redirect<4;redirect++){
            HttpsURLConnection connection=(HttpsURLConnection)url.openConnection();connection.setConnectTimeout(5000);connection.setReadTimeout(5000);connection.setInstanceFollowRedirects(false);
            try{int status=connection.getResponseCode();if(status>=300&&status<400){String location=connection.getHeaderField("Location");if(location==null)throw new IOException("Redirecionamento sem imagem.");url=Net.https(new URL(url,location).toString());continue;}if(status!=200)throw new IOException("Foto não disponível.");
                try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>2*1024*1024)throw new IOException("Foto muito grande.");out.write(buffer,0,n);}byte[] bytes=out.toByteArray();BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(options.outWidth<1||options.outHeight<1)throw new IOException("Foto inválida.");options.inSampleSize=Math.max(1,Math.max(options.outWidth,options.outHeight)/256);options.inJustDecodeBounds=false;Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(bitmap==null)throw new IOException("Formato de foto inválido.");try(ByteArrayOutputStream encoded=new ByteArrayOutputStream()){bitmap.compress(Bitmap.CompressFormat.JPEG,88,encoded);bitmap.recycle();return encoded.toByteArray();}}
            }finally{connection.disconnect();}
        }
        throw new IOException("Muitos redirecionamentos para a foto.");
    }
}
