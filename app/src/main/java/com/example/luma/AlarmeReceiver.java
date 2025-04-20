package com.example.luma;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class AlarmeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String noteTitle = intent.getStringExtra("NOTE_TITLE");

        // Créer une notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "note_alarm_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Note Reminder")
                .setContentText("It's time for your note: " + noteTitle)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Vérifier la permission avant d'afficher la notification
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                notificationManager.notify(1, builder.build());
            } catch (SecurityException e) {
                e.printStackTrace();
                // Gérer l'exception ou logger l'erreur
            }
        }
    }
}