package com.example.acp2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.FaceDetector;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import android.media.MediaPlayer;
import android.media.MediaRecorder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    private MediaRecorder recorder = new MediaRecorder();
    private Handler handler = new Handler();

    private boolean recording = false;

    private File sd = Environment.getExternalStorageDirectory();
    private File fileName = null;

    // FIXME
    private URL url = this.prepareURL("http://test.test");

    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_MEDIA_LOCATION
    };
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        setupRecorder();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // give some input to the user
                if (recording) {
                    stopRecording();
                } else {
                    recordAudio();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void setupRecorder() {
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);

        // check correct file format
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setAudioSamplingRate(16000);
        fileName = getNewFile();
        recorder.setOutputFile(fileName);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e("ACP2-mic", "prepare() failed", e);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private String generateUUID(){
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
    private File getNewFile(){
        String uuid = generateUUID();
        return  new File(this.sd, uuid);
    }

    private void recordAudio() {
        recorder.start();
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendAudioData();
                try {
                    fileName.createNewFile();
                } catch (IOException e) {
                    Log.e("ACP2-mic", "Error", e);
                } finally {
                    Log.println(Log.INFO, "ACP2-mic", String.valueOf(fileName.length()));
                }
            }
        }, 1, 8000);
    }

    private void stopRecording() {
        // give some input to the user
        Toast.makeText(this, "Stops recording", Toast.LENGTH_LONG).show();
        //handler.removeCallbacks(runnable);

        recording = false;
        recorder.stop();
        recorder.release();
        recorder = null;
    }

    private URL prepareURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            Log.e("ACP2-mic", "Invalid url");
        }
        return null;
    }

    private int readAudioFile() {
        try {
            FileInputStream fileInputStream = new FileInputStream(fileName);
           return fileInputStream.read();

        } catch (FileNotFoundException e) {
            Log.e("ACP2-mic", "File not found");
        } catch (IOException e) {
            Log.e("ACP2-mic", "Could not open file");
        }
        return -1;
    }

    private void sendAudioData() {
        try {
            if (url == null) {
                Log.e("-mic", "Invalid url");

            } else {
                HttpURLConnection client = (HttpURLConnection) url.openConnection();

                // here we should read the data from audio file, but for now just skipping it
                client.setRequestMethod("POST");
                client.setDoOutput(true);
                OutputStream stream = new BufferedOutputStream(client.getOutputStream());

                stream.write(this.readAudioFile());
                stream.flush();
                stream.close();
            }
        } catch (Exception e) {
            Log.e("ACP2-mic", "Could not send data to server");
        }
    }

}
