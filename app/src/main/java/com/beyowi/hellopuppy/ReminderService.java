package com.beyowi.hellopuppy;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

/**
 * Created by User on 10/03/2015.
 */

public class ReminderService extends BroadcastReceiver {
    private static final int NOTIF_ID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        //Create intent for click on notification
        Intent resultIntent = new Intent(context, MainActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        //Create notification
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.loulou_notif)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(context.getString(R.string.notification_desc))
                .setAutoCancel(true)
                .setContentIntent(resultPendingIntent)
                .setLights(Color.BLUE, 500, 500);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean("notifications_new_picture_sound", true)){mBuilder.setSound(alarmSound);}
        if (prefs.getBoolean("notifications_new_picture_vibrate", true)){
            long[] vibration_pattern = {0,100,100};
            mBuilder.setVibrate(vibration_pattern);
        }
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        mNotifyMgr.notify(NOTIF_ID, mBuilder.build());
    }
}
