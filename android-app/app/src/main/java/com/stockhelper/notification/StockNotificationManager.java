package com.stockhelper.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.stockhelper.R;
import com.stockhelper.data.local.entities.StockEntity;
import com.stockhelper.ui.main.MainActivity;

public class StockNotificationManager {
    
    private static final String CHANNEL_ID = "stock_alert_channel";
    private static final String CHANNEL_NAME = "股票提醒";
    private static final String CHANNEL_DESC = "股票价格达到目标时的提醒通知";
    private static final int NOTIFICATION_ID_BASE = 1000;
    
    private final Context context;
    private final NotificationManager notificationManager;
    
    public StockNotificationManager(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    public void showSellNotification(StockEntity stock) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                stock.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        String title = "股票卖出提醒";
        String message = String.format("%s(%s) 已达到目标收益率 %.2f%%，建议卖出！",
                stock.getName(),
                stock.getCode(),
                stock.getReturnRate());
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setContentIntent(pendingIntent);
        
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID_BASE + stock.getId(), builder.build());
        }
    }
    
    public void cancelNotification(int stockId) {
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID_BASE + stockId);
        }
    }
}
