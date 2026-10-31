package com.livepulse.app;
public final class NotificationIcons {
    public static int small(String platform){switch(platform){case"twitch":return R.drawable.ic_twitch_notification;case"youtube":return R.drawable.ic_youtube_notification;case"kick":return R.drawable.ic_kick_notification;case"custom":return R.drawable.ic_custom_notification;default:return R.drawable.ic_notification;}}
    public static int color(String platform){switch(platform){case"twitch":return 0xff9146ff;case"youtube":return 0xffef4f58;case"kick":return 0xff53fc18;case"custom":return 0xffa6a9ff;default:return 0xffb8f57c;}}
}
