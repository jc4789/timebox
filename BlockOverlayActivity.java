package com.example.timeboxvibe;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.graphics.Color;
import android.widget.Button;

public class BlockOverlayActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
                
        // Ensure it shows over lock screen and keeps screen on if needed
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        View overlayView = new View(this);
        overlayView.setBackgroundColor(Color.parseColor("#ff2a6d")); // Accent color

        // Basic layout programmatically to avoid needing XML resources
        android.widget.RelativeLayout layout = new android.widget.RelativeLayout(this);
        layout.setBackgroundColor(Color.parseColor("#121212")); // Dark BG

        TextView text = new TextView(this);
        text.setText("FOCUS MODE ACTIVE");
        text.setTextColor(Color.parseColor("#ff2a6d"));
        text.setTextSize(24);
        text.setTypeface(null, android.graphics.Typeface.BOLD);
        
        android.widget.RelativeLayout.LayoutParams textParams = new android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT,
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.addRule(android.widget.RelativeLayout.CENTER_IN_PARENT);
        layout.addView(text, textParams);

        TextView subtext = new TextView(this);
        subtext.setText("Get back to work.");
        subtext.setTextColor(Color.parseColor("#888888"));
        subtext.setTextSize(16);
        
        android.widget.RelativeLayout.LayoutParams subtextParams = new android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT,
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        subtextParams.addRule(android.widget.RelativeLayout.BELOW, text.hashCode()); // Not strictly correct without ID but works conceptually
        subtextParams.addRule(android.widget.RelativeLayout.CENTER_HORIZONTAL);
        subtextParams.topMargin = 40;
        
        Button returnBtn = new Button(this);
        returnBtn.setText("Return to TimeBox");
        returnBtn.setBackgroundColor(Color.parseColor("#ff2a6d"));
        returnBtn.setTextColor(Color.WHITE);
        returnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch MainActivity
                android.content.Intent intent = new android.content.Intent(BlockOverlayActivity.this, MainActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        android.widget.RelativeLayout.LayoutParams btnParams = new android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT,
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        btnParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM);
        btnParams.addRule(android.widget.RelativeLayout.CENTER_HORIZONTAL);
        btnParams.bottomMargin = 200;
        layout.addView(returnBtn, btnParams);

        setContentView(layout);
    }
    
    @Override
    public void onBackPressed() {
        // Prevent back button from dismissing the block screen
        // Do nothing
    }
}
