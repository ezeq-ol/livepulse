package com.livepulse.app;
import org.json.JSONObject;
public final class PlatformConfigTest {
 public static void main(String[] args) throws Exception {
  JSONObject fresh=new JSONObject(); PlatformConfig.applyDefaults(fresh);
  if(!fresh.getString("kickId").equals("01M68VM8J38XD44FBDQM6G6ZYE"))throw new AssertionError("Missing Kick default");
  if(!fresh.getString("twitchId").equals("u9uxx2f1pbcx1us3k96213lcti9pyg"))throw new AssertionError("Missing Twitch default");
  if(fresh.has("kickSecret") || fresh.has("twitchSecret"))throw new AssertionError("Secrets must not be bundled");
  JSONObject saved=new JSONObject().put("kickId","other-client").put("kickSecret","test-only-secret").put("twitchId","other-twitch");
  String before=saved.toString();PlatformConfig.applyDefaults(saved);
  if(!before.equals(saved.toString()))throw new AssertionError("Existing credentials changed");
  PlatformConfig.applyDefaults(fresh);
  if(fresh.length()!=2)throw new AssertionError("Unexpected credential defaults");
  System.out.println("PlatformConfigTest: 5 verificacoes aprovadas.");
 }
}
