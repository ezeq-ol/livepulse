package com.livepulse.app;

import org.json.JSONObject;

/** Public application identifiers only. Never put Client Secrets here. */
public final class PlatformConfig {
    private PlatformConfig() {}
    // Fill with the Client ID of your own PUBLIC Twitch application.
    public static final String TWITCH_CLIENT_ID = "u9uxx2f1pbcx1us3k96213lcti9pyg";
    public static final String KICK_CLIENT_ID = "01M68VM8J38XD44FBDQM6G6ZYE";
    // Reference for Google Cloud administration; Google Identity uses package + signing certificate.
    public static final String GOOGLE_ANDROID_CLIENT_ID = "844855666493-kvoa3oi89r5isnmltfrbbbhkvgfb25rh.apps.googleusercontent.com";

    public static final String YOUTUBE_API_KEY = "";

    public static void applyDefaults(JSONObject credentials) throws Exception {
        putIfMissing(credentials, "twitchId", TWITCH_CLIENT_ID);
        putIfMissing(credentials, "kickId", KICK_CLIENT_ID);
        putIfMissing(credentials, "youtubeKey", YOUTUBE_API_KEY);
    }
    private static void putIfMissing(JSONObject credentials, String name, String value) throws Exception {
        if (credentials.optString(name).trim().isEmpty() && !value.trim().isEmpty())
            credentials.put(name, value.trim());
    }
}
