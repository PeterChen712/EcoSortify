package com.example.glean.helper;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.example.glean.R;
import com.google.android.material.card.MaterialCardView;

/**
 * Helper untuk menampilkan notifikasi status koneksi yang modern dan tidak mengganggu
 */
public class NetworkNotificationHelper {
    
    private MaterialCardView notificationCard;
    private TextView notificationText;
    private boolean isShowing = false;
    
    public static NetworkNotificationHelper create(Context context, ViewGroup parent) {
        NetworkNotificationHelper helper = new NetworkNotificationHelper();
        helper.initNotificationView(context, parent);
        return helper;
    }
    
    private void initNotificationView(Context context, ViewGroup parent) {
        // Create notification card
        notificationCard = new MaterialCardView(context);
        ViewGroup.MarginLayoutParams layoutParams = new ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (int) (48 * context.getResources().getDisplayMetrics().density) // 48dp height
        );
        layoutParams.setMargins(
            (int) (16 * context.getResources().getDisplayMetrics().density), // 16dp margin
            (int) (8 * context.getResources().getDisplayMetrics().density),  // 8dp top margin
            (int) (16 * context.getResources().getDisplayMetrics().density), // 16dp margin
            0
        );
        notificationCard.setLayoutParams(layoutParams);
          // Card styling
        notificationCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.warning_background));
        notificationCard.setRadius(12 * context.getResources().getDisplayMetrics().density);
        notificationCard.setCardElevation(4 * context.getResources().getDisplayMetrics().density);
        notificationCard.setStrokeColor(ContextCompat.getColor(context, R.color.warning_border));
        notificationCard.setStrokeWidth((int) (1 * context.getResources().getDisplayMetrics().density));
        
        // Create text view
        notificationText = new TextView(context);
        notificationText.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        notificationText.setGravity(android.view.Gravity.CENTER_VERTICAL);
        notificationText.setPadding(
            (int) (16 * context.getResources().getDisplayMetrics().density),
            0,
            (int) (16 * context.getResources().getDisplayMetrics().density),
            0
        );
        notificationText.setTextColor(ContextCompat.getColor(context, R.color.warning_text));
        notificationText.setTextSize(14);
        notificationText.setTypeface(null, android.graphics.Typeface.BOLD);
        
        // Add text to card
        notificationCard.addView(notificationText);
        
        // Initially hidden
        notificationCard.setVisibility(View.GONE);
        notificationCard.setAlpha(0f);
        
        // Add to parent
        parent.addView(notificationCard, 0); // Add at top
    }
    
    public void showOfflineNotification() {
        if (isShowing) return;
        
        notificationText.setText("🌐 Tidak ada koneksi internet");
        notificationCard.setVisibility(View.VISIBLE);
        notificationCard.animate()
            .alpha(1f)
            .setDuration(300)
            .start();
        
        isShowing = true;
    }
    
    public void hideNotification() {
        if (!isShowing) return;
        
        notificationCard.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction(() -> {
                notificationCard.setVisibility(View.GONE);
                isShowing = false;
            })
            .start();
    }
    
    public boolean isShowing() {
        return isShowing;
    }
    
    public void destroy() {
        if (notificationCard != null && notificationCard.getParent() != null) {
            ((ViewGroup) notificationCard.getParent()).removeView(notificationCard);
        }
    }
}
