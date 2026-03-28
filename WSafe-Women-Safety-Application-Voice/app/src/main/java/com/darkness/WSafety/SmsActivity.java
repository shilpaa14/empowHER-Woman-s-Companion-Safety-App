package com.darkness.WSafety;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.Locale;

public class SmsActivity extends AppCompatActivity {
    Button start, stop, helpline, activateVoice;
    SpeechRecognizer speechRecognizer;
    Intent recognizerIntent;
    boolean isVoiceActive = false;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(SmsActivity.this, MainActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms);

        stop = findViewById(R.id.stopService);
        start = findViewById(R.id.startService);
        helpline = findViewById(R.id.btn_helpline);
        activateVoice = findViewById(R.id.activateVoice); // New button for voice activation

        start.setOnClickListener(this::startServiceV);
        stop.setOnClickListener(this::stopService);
        helpline.setOnClickListener(this::helplines);
        activateVoice.setOnClickListener(this::toggleVoiceRecognition);

        // Set up Speech Recognizer
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
            public void onError(int error) {}

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    for (String match : matches) {
                        if (match.equalsIgnoreCase("SOS") || match.equalsIgnoreCase("HELP") || match.equalsIgnoreCase("Bachao") || match.equalsIgnoreCase("Sahayiku") || match.equalsIgnoreCase("Kaapadi") || match.equalsIgnoreCase("Kaappaatrungal") || match.equalsIgnoreCase("Sahaayam cheyandi") || match.equalsIgnoreCase("Madat kara") || match.equalsIgnoreCase("Banchantu") || match.equalsIgnoreCase("Banchan")) {
                            triggerSOSAlarm();
                            break;
                        }
                    }
                }
                if (isVoiceActive) {
                    startListening(); // Continue listening if voice activation is still on
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    public void helplines(View view) {
        startActivity(new Intent(SmsActivity.this, HelplineCall.class));
    }

    public void stopService(View view) {
        Intent notificationIntent = new Intent(this, ServiceMine.class);
        notificationIntent.setAction("stop");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ServiceMine.isRunning) {
                getApplicationContext().startForegroundService(notificationIntent);
                Snackbar.make(findViewById(android.R.id.content), "Service Stopped!", Snackbar.LENGTH_LONG).show();
            }
        } else {
            if (ServiceMine.isRunning) {
                getApplicationContext().startService(notificationIntent);
                Snackbar.make(findViewById(android.R.id.content), "Service Stopped!", Snackbar.LENGTH_LONG).show();
            }
        }
    }

    public void startServiceV(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Intent notificationIntent = new Intent(this, ServiceMine.class);
            notificationIntent.setAction("Start");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getApplicationContext().startForegroundService(notificationIntent);
                Snackbar.make(findViewById(android.R.id.content), "Service Started!", Snackbar.LENGTH_LONG).show();
            } else {
                getApplicationContext().startService(notificationIntent);
                Snackbar.make(findViewById(android.R.id.content), "Service Started!", Snackbar.LENGTH_LONG).show();
            }
        }
    }

    // Method to toggle voice recognition
    public void toggleVoiceRecognition(View view) {
        isVoiceActive = !isVoiceActive;
        if (isVoiceActive) {
            startListening();
            Toast.makeText(this, "Voice recognition activated", Toast.LENGTH_SHORT).show();
        } else {
            speechRecognizer.stopListening();
            Toast.makeText(this, "Voice recognition deactivated", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to start listening
    private void startListening() {
        speechRecognizer.startListening(recognizerIntent);
    }

    // Method to trigger SOS alarm
    private void triggerSOSAlarm() {
        Toast.makeText(this, "SOS Alarm Triggered!", Toast.LENGTH_LONG).show();
        Intent voiceAlertIntent = new Intent(this, ServiceMine.class);
        voiceAlertIntent.setAction("VOICE_ALERT");
        startService(voiceAlertIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}
