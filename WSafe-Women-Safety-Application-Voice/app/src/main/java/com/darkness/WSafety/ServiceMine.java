package com.darkness.WSafety;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.github.tbouron.shakedetector.library.ShakeDetector;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ServiceMine extends Service {

    static boolean isRunning = false;
    static MediaPlayer mediaPlayer;
    FusedLocationProviderClient fusedLocationClient;
    SpeechRecognizer speechRecognizer;
    Intent recognizerIntent;
    PowerManager.WakeLock wakeLock; // To keep CPU awake

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    SmsManager manager = SmsManager.getDefault();
    String myLocation;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize MediaPlayer for siren sound
        mediaPlayer = MediaPlayer.create(getBaseContext(), R.raw.siren);
        mediaPlayer.setLooping(true);

        // Initialize FusedLocationProviderClient for location updates
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        updateLocation();

        // Set up ShakeDetector
        ShakeDetector.create(this, this::triggerSOSAlarm);

        // Set up SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {}

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                // Restart listening on error
                startVoiceRecognition();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    for (String match : matches) {
                        if (match.equalsIgnoreCase("SOS") || match.equalsIgnoreCase("HELP") || match.equalsIgnoreCase("Bachao") || match.equalsIgnoreCase("Sahayiku") || match.equalsIgnoreCase("Kaapadi") || match.equalsIgnoreCase("Kaappaatrungal") || match.equalsIgnoreCase("Sahaayam cheyandi") || match.equalsIgnoreCase("Madat kara") || match.equalsIgnoreCase("Banchantu") || match.equalsIgnoreCase("Banchan") )  {
                            triggerSOSAlarm();
                            break;
                        }
                    }
                }
                startVoiceRecognition(); // Restart listening after each result
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        // Acquire a partial wake lock to keep CPU on
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WSafety::WakeLockTag");
        wakeLock.acquire();

        // Start voice recognition
        startVoiceRecognition();
    }

    private void updateLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        myLocation = "http://maps.google.com/maps?q=loc:" + location.getLatitude() + "," + location.getLongitude();
                    } else {
                        myLocation = "Unable to Find Location :(";
                    }
                });
    }

    private void triggerSOSAlarm() {
        mediaPlayer.start();
        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE);
        Set<String> emergencyNumbers = sharedPreferences.getStringSet("enumbers", new HashSet<>());
        for (String number : emergencyNumbers) {
            manager.sendTextMessage(number, null, "I'm in trouble! My location:\n" + myLocation, null, null);
        }
        Toast.makeText(this, "SOS Alarm Triggered!", Toast.LENGTH_LONG).show();
    }

    private void startVoiceRecognition() {
        speechRecognizer.startListening(recognizerIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if ("STOP".equalsIgnoreCase(intent.getAction())) {
            stopForegroundService();
        } else {
            startForegroundService();
        }
        return START_NOT_STICKY;
    }

    private void startForegroundService() {
        Intent notificationIntent = new Intent(this, SmsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_MUTABLE);
        Notification notification = new Notification.Builder(this, "MYID")
                .setContentTitle("Women Safety")
                .setContentText("Voice command active. Say 'HELP','Bachao', 'Sahayiku', 'Kapadi' or 'SOS' etc to trigger alarm.")
                .setSmallIcon(R.drawable.girlpower)
                .setContentIntent(pendingIntent)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("MYID", "ChannelName", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
        startForeground(1, notification);
        isRunning = true;
    }

    private void stopForegroundService() {
        mediaPlayer.stop();
        speechRecognizer.stopListening();
        stopForeground(true);
        stopSelf();
        isRunning = false;
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
    }
}
