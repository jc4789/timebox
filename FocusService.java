package com.example.timeboxvibe;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.timeboxvibe.ui.main.TimerEngine;
import com.example.timeboxvibe.ui.main.TimerPreset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FocusService extends Service {

    private static final String CHANNEL_ID = "FocusServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    public static TimerEngine engine;
    private final Handler tickHandler = new Handler();
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;

    private boolean isRinging = false;
    private double vibeIntensity = 0.8;
    private double volume = 0.5;
    private boolean isStrictMode = false;
    private boolean tickEnabled = false;
    private ToneGenerator tickTone;
    private String selectedSound = "synth-chime";
    private String taskName = "Focus Time";

    private final List<String> BLOCKED_PACKAGES = Arrays.asList(
        "com.instagram.android", 
        "com.zhiliaoapp.musically", // TikTok
        "com.twitter.android",
        "com.reddit.frontpage",
        "com.facebook.katana",
        "com.snapchat.android",
        "com.google.android.youtube"
    );

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (engine != null && engine.isActive() && !engine.isRinging()) {
                // Play native tick sound if enabled and app is in background
                if (tickEnabled && tickTone != null && !MainActivity.isForeground) {
                    tickTone.startTone(ToneGenerator.TONE_PROP_BEEP, 20); // 20ms beep
                }

                // Task 5: App Blocker logic
                if (isStrictMode) {
                    checkForegroundApp();
                }

                // Tick the engine
                TimerEngine.TickEvent event = engine.tick();

                // Broadcast state to UI
                broadcastState();

                // Process tick events
                if (event instanceof TimerEngine.TickEvent.IntervalComplete) {
                    if (engine.isRinging()) {
                        triggerPersistentAlarm();
                    } else {
                        triggerGentleReminder();
                    }
                } else if (event instanceof TimerEngine.TickEvent.SequenceComplete) {
                    if (engine.isRinging()) {
                        triggerPersistentAlarm();
                    } else {
                        stopAlarmAndService();
                    }
                }

                // Schedule next tick aligned to second boundary
                if (engine.isActive() && !engine.isRinging()) {
                    long now = SystemClock.uptimeMillis();
                    long next = now + (1000 - (now % 1000));
                    tickHandler.postAtTime(this, next);
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vibratorManager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TimeBox::AlarmWakeLock");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();
        
        // Extract/update settings first (passed in almost all intents)
        vibeIntensity = intent.getDoubleExtra("vibeIntensity", vibeIntensity);
        volume = intent.getDoubleExtra("volume", volume);
        isStrictMode = intent.getBooleanExtra("strictMode", isStrictMode);
        boolean newTickEnabled = intent.getBooleanExtra("tickEnabled", tickEnabled);
        if (newTickEnabled != tickEnabled) {
            tickEnabled = newTickEnabled;
            if (tickEnabled) {
                if (tickTone == null) {
                    try {
                        tickTone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 30);
                    } catch (Exception e) {}
                }
            } else {
                if (tickTone != null) {
                    tickTone.release();
                    tickTone = null;
                }
            }
        }
        String snd = intent.getStringExtra("selectedSound");
        if (snd != null) selectedSound = snd;

        String task = intent.getStringExtra("taskName");
        if (task != null) taskName = task;

        if ("STOP_SERVICE".equals(action)) {
            stopAlarmAndService();
            return START_NOT_STICKY;
        } else if ("PAUSE_TIMER".equals(action)) {
            if (engine != null) {
                engine.pause();
                tickHandler.removeCallbacks(tickRunnable);
                stopAlarmAudioAndVibe();
                broadcastState();
            }
            return START_NOT_STICKY;
        } else if ("RESET_TIMER".equals(action)) {
            if (engine != null) {
                engine.reset();
                tickHandler.removeCallbacks(tickRunnable);
                stopAlarmAudioAndVibe();
                broadcastState();
            }
            return START_NOT_STICKY;
        } else if ("SKIP_TIMER".equals(action)) {
            if (engine != null) {
                engine.skip();
                broadcastState();
            }
            return START_NOT_STICKY;
        } else if ("DISMISS_ALARM".equals(action)) {
            if (engine != null) {
                engine.dismissAlarm();
                stopAlarmAudioAndVibe();
                broadcastState();
                if (engine.isActive()) {
                    long now = SystemClock.uptimeMillis();
                    tickHandler.removeCallbacks(tickRunnable);
                    tickHandler.postAtTime(tickRunnable, now + 1000);
                }
            }
            return START_NOT_STICKY;
        } else if ("UPDATE_SETTINGS".equals(action)) {
            return START_NOT_STICKY;
        }

        // Starting the timer
        String presetId = intent.getStringExtra("presetId");
        if (presetId != null) {
            String name = intent.getStringExtra("presetName");
            String mode = intent.getStringExtra("presetMode");
            List<Integer> sequence = intent.getIntegerArrayListExtra("presetSequence");
            if (sequence == null) {
                sequence = new ArrayList<>();
            }
            int dualBigDuration = intent.getIntExtra("dualBigDuration", 3600);
            int dualMidDuration = intent.getIntExtra("dualMidDuration", 900);
            int dualSmallDuration = intent.getIntExtra("dualSmallDuration", 90);
            String alarmBehavior = intent.getStringExtra("alarmBehavior");
            if (alarmBehavior == null) alarmBehavior = "alarm";
            String description = intent.getStringExtra("presetDescription");
            if (description == null) description = "";

            TimerPreset preset = new TimerPreset(
                presetId, name, mode, sequence,
                dualBigDuration, dualMidDuration, dualSmallDuration,
                alarmBehavior, description
            );
            engine = new TimerEngine(preset);
        }

        if (engine != null) {
            engine.start();
            
            // Start tick loop
            tickHandler.removeCallbacks(tickRunnable);
            long now = SystemClock.uptimeMillis();
            tickHandler.postAtTime(tickRunnable, now + 1000);

            // Broadcast initial state
            broadcastState();

            // Set foreground notification
            Notification notification = buildNotification("Timer Running", "Task: " + taskName);
            startForeground(NOTIFICATION_ID, notification);
        }

        return START_NOT_STICKY;
    }

    private void checkForegroundApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm == null) return;

            long time = System.currentTimeMillis();
            UsageEvents events = usm.queryEvents(time - 10000, time);
            UsageEvents.Event event = new UsageEvents.Event();
            String currentApp = null;

            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                if (event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
                    currentApp = event.getPackageName();
                }
            }

            if (currentApp != null && BLOCKED_PACKAGES.contains(currentApp)) {
                // Trigger Block Overlay
                Intent blockIntent = new Intent(this, BlockOverlayActivity.class);
                blockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(blockIntent);
            }
        }
    }

    private void triggerPersistentAlarm() {
        isRinging = true;

        // Start looping alarm sound via SoundPreviewPlayer
        try {
            com.example.timeboxvibe.ui.main.SoundPreviewPlayer.INSTANCE.playAlarm(this, selectedSound, (float) volume);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Start Vibration Loop
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int amplitude = (int) (255 * vibeIntensity);
                if (amplitude < 1) amplitude = 1;
                
                long[] timings = {0, 1000, 1000};
                int[] amplitudes = {0, amplitude, 0};
                
                VibrationEffect effect = VibrationEffect.createWaveform(timings, amplitudes, 0); // 0 means loop at index 0
                vibrator.vibrate(effect);
            } else {
                long[] pattern = {0, 1000, 1000};
                vibrator.vibrate(pattern, 0); // 0 means repeat
            }
        }

        // Wake up the screen so the user can see the dismiss button
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
        }

        // Launch MainActivity over the lock screen so user can dismiss it
        // Only if not currently in foreground (avoiding JS reloads)
        if (!MainActivity.isForeground) {
            Intent activityIntent = new Intent(this, MainActivity.class);
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(activityIntent);
        }

        // Update notification
        Notification notification = buildNotification("Time's Up!", taskName);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    private void triggerGentleReminder() {
        // Play brief 1-second gentle reminder sound
        try {
            com.example.timeboxvibe.ui.main.SoundPreviewPlayer.INSTANCE.playGentleReminder(this, selectedSound, (float) volume);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Vibrate for exactly 1 second
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int amplitude = (int) (255 * vibeIntensity);
                if (amplitude < 1) amplitude = 1;
                VibrationEffect effect = VibrationEffect.createOneShot(1000, amplitude);
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(1000);
            }
        }
    }

    private void stopAlarmAudioAndVibe() {
        if (vibrator != null) {
            vibrator.cancel();
        }
        try {
            com.example.timeboxvibe.ui.main.SoundPreviewPlayer.INSTANCE.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    private void stopAlarmAndService() {
        tickHandler.removeCallbacks(tickRunnable);
        if (engine != null) {
            engine.pause();
        }
        stopAlarmAudioAndVibe();
        if (tickTone != null) {
            tickTone.release();
            tickTone = null;
        }
        stopForeground(true);
        stopSelf();
    }

    private void broadcastState() {
        if (engine == null) return;
        Intent intent = new Intent("TIMER_STATE_UPDATE");
        intent.putExtra("timeRemaining", engine.getTimeRemaining());
        intent.putExtra("totalDuration", engine.getTotalDuration());
        intent.putExtra("midTimeRemaining", engine.getMidTimeRemaining());
        intent.putExtra("midTotalDuration", engine.getMidTotalDuration());
        intent.putExtra("bigTimeRemaining", engine.getBigTimeRemaining());
        intent.putExtra("bigTotalDuration", engine.getBigTotalDuration());
        intent.putExtra("currentIndex", engine.getCurrentIndex());
        intent.putExtra("isActive", engine.isActive());
        intent.putExtra("isRinging", engine.isRinging());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private Notification buildNotification(String title, String content) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent stopIntent = new Intent(this, FocusService.class);
        stopIntent.setAction("STOP_SERVICE");
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", stopPendingIntent)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Focus Service Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            serviceChannel.setSound(null, null);
            serviceChannel.enableVibration(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAlarmAndService();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopAlarmAndService();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
