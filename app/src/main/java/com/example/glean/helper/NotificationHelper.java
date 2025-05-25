package com.example.glean.helper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.glean.MainActivity;
import com.example.glean.R;
import com.example.glean.fragment.ChallengeFragment;
import com.example.glean.fragment.PloggingFragment;

public class NotificationHelper {

    // Notification Channels
    public static final String CHANNEL_ID_TRACKING = "location_tracking_channel";
    public static final String CHANNEL_ID_CHALLENGES = "challenges_channel";
    public static final String CHANNEL_ID_ACHIEVEMENTS = "achievements_channel";
    public static final String CHANNEL_ID_REMINDERS = "reminders_channel";

    // Notification IDs
    public static final int NOTIFICATION_ID_TRACKING = 1001;
    public static final int NOTIFICATION_ID_CHALLENGE = 2001;
    public static final int NOTIFICATION_ID_ACHIEVEMENT = 3001;
    public static final int NOTIFICATION_ID_REMINDER = 4001;

    /**
     * Create all notification channels for the app (call once in Application class)
     */
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

            // Tracking channel (silent, for foreground service)
            NotificationChannel trackingChannel = new NotificationChannel(
                    CHANNEL_ID_TRACKING,
                    "Location Tracking",
                    NotificationManager.IMPORTANCE_LOW);
            trackingChannel.setDescription("Used for tracking location during plogging activities");
            
            // Challenges channel (default importance)
            NotificationChannel challengesChannel = new NotificationChannel(
                    CHANNEL_ID_CHALLENGES,
                    "Challenges",
                    NotificationManager.IMPORTANCE_DEFAULT);
            challengesChannel.setDescription("Notifications about challenge progress and updates");
            
            // Achievements channel (high importance)
            NotificationChannel achievementsChannel = new NotificationChannel(
                    CHANNEL_ID_ACHIEVEMENTS,
                    "Achievements",
                    NotificationManager.IMPORTANCE_HIGH);
            achievementsChannel.setDescription("Notifications about new badges and achievements");
            
            // Reminders channel (default importance)
            NotificationChannel remindersChannel = new NotificationChannel(
                    CHANNEL_ID_REMINDERS,
                    "Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT);
            remindersChannel.setDescription("Activity reminders to keep you plogging");

            // Create all channels
            notificationManager.createNotificationChannel(trackingChannel);
            notificationManager.createNotificationChannel(challengesChannel);
            notificationManager.createNotificationChannel(achievementsChannel);
            notificationManager.createNotificationChannel(remindersChannel);
        }
    }

    /**
     * Show a notification for tracking location during a plogging session
     */
    public static NotificationCompat.Builder createTrackingNotification(Context context) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        
        return new NotificationCompat.Builder(context, CHANNEL_ID_TRACKING)
                .setContentTitle("Tracking Location")
                .setContentText("GleanGo is tracking your plogging activity")
                .setSmallIcon(R.drawable.ic_plogging)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);
    }

    /**
     * Show a notification for challenge updates
     */
    public static void showChallengeNotification(Context context, String title, String message) {
        Intent intent = new Intent(context, MainActivity.class);
        // Add flags to open the challenges fragment
        intent.putExtra("OPEN_FRAGMENT", "challenges");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_CHALLENGES)
                .setSmallIcon(R.drawable.ic_challenge)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        
        try {
            notificationManager.notify(NOTIFICATION_ID_CHALLENGE, builder.build());
        } catch (SecurityException e) {
            // Handle missing notification permission (Android 13+)
            e.printStackTrace();
        }
    }

    /**
     * Show a notification for achievement unlocked
     */
    public static void showAchievementNotification(Context context, String badgeName) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("OPEN_FRAGMENT", "profile");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_ACHIEVEMENTS)
                .setSmallIcon(R.drawable.badge_gold)
                .setContentTitle("New Achievement Unlocked!")
                .setContentText("You've earned the " + badgeName + " badge. Check it out in your profile!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        
        try {
            notificationManager.notify(NOTIFICATION_ID_ACHIEVEMENT, builder.build());
        } catch (SecurityException e) {
            // Handle missing notification permission
            e.printStackTrace();
        }
    }

    /**
     * Show a notification reminding user to plog
     */
    public static void showReminderNotification(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("OPEN_FRAGMENT", "plogging");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
                .setSmallIcon(R.drawable.ic_plogging)
                .setContentTitle("Time to Plog!")
                .setContentText("It's been a while since your last plogging activity. Ready for another run?")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        
        try {
            notificationManager.notify(NOTIFICATION_ID_REMINDER, builder.build());
        } catch (SecurityException e) {
            // Handle missing notification permission
            e.printStackTrace();
        }
    }
    
    /**
     * Show a notification when plogging activity is completed
     */
    public static void showPloggingCompletedNotification(Context context, int points, int trashItems) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("OPEN_FRAGMENT", "stats");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        
        String message = "Great job! You collected " + trashItems + " items and earned " + points + " points.";
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_ACHIEVEMENTS)
                .setSmallIcon(R.drawable.ic_plogging)
                .setContentTitle("Plogging Activity Completed")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        
        try {
            notificationManager.notify(NOTIFICATION_ID_ACHIEVEMENT + 1, builder.build());
        } catch (SecurityException e) {
            // Handle missing notification permission
            e.printStackTrace();
        }
    }
}