package com.example.myaudio;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Locale;

class MyRecorderBeta {

    private static final int SAMPLE_RATE = 16000;
    private static final int BUFFER_SIZE = 640;
    public AudioRecord audioRecord;
    private Thread recordThread;

    private int mAudioSampleRate = SAMPLE_RATE;
    private int mAudioSource = MediaRecorder.AudioSource.MIC;
    private int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int mAudioChannel = AudioFormat.CHANNEL_IN_MONO;

    private volatile boolean isQueryLog;
    private volatile boolean isStartRecord = false;

    public void startRecord() {
        stopRecord();
        if (recordThread != null) {
            try {
                recordThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

        final int buffersize = audioRecord.getMinBufferSize(mAudioSampleRate, mAudioChannel, mAudioFormat);

        try {
            audioRecord = new AudioRecord(mAudioSource, mAudioSampleRate, mAudioChannel, mAudioFormat, buffersize * 10);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // start recording

        isStartRecord = true;

        recordThread = new Thread() {
            @Override
            public void run() {
                super.run();

                try {
                    if (audioRecord != null) {
                        audioRecord.startRecording();
                    }
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    isStartRecord = false;
                }
                isQueryLog = true;
                while (isStartRecord) {
                    try {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int readBytes = audioRecord.read(buffer, 0, buffer.length);
                        if (readBytes > 0) {
                            ProcessData(buffer);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        isStartRecord = false;
                    }
                }

                try {
                    if (audioRecord != null) {
                        audioRecord.release();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    audioRecord = null;
                }
            }
        };

        recordThread.start();

    }

    public void stopRecord() {
        isStartRecord = false;
    }

    public void ProcessData(byte[] data) {

    }
}

public class MainActivity extends AppCompatActivity {

    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
    };

    public static MainActivity main = null;

    public MyRecorderBeta myRecorderBeta = new MyRecorderBeta();

    public MyRecorder myRecorder = new MyRecorder();
    public Visualizer visualizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this,PERMISSIONS, 1);
        InitButtons();
    }

    MathView mathView;


    protected void InitButtons() {

        Button record_button =  findViewById(R.id.recordbutton);
        Button play_button =  findViewById(R.id.playbutton);
        mathView = (MathView)findViewById(R.id.mymathview);

        record_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Recording!", Toast.LENGTH_SHORT).show();
                startRecord();
            }
        });

        play_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Stop Recording", Toast.LENGTH_SHORT).show();
                stopRecord();
            }
        });
    }

    // below are the methods

    private static final int SAMPLE_RATE = 16000;
    private static final int BUFFER_SIZE = 640;
    public AudioRecord audioRecord;
    private Thread recordThread;

    private int mAudioSampleRate = SAMPLE_RATE;
    private int mAudioSource = MediaRecorder.AudioSource.MIC;
    private int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int mAudioChannel = AudioFormat.CHANNEL_IN_MONO;

    private volatile boolean isQueryLog;
    private volatile boolean isStartRecord = false;

    public void startRecord() {

        stopRecord();

        if (recordThread != null) {
            try {
                recordThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

        final int buffersize = audioRecord.getMinBufferSize(mAudioSampleRate, mAudioChannel, mAudioFormat);

        try {
            audioRecord = new AudioRecord(mAudioSource, mAudioSampleRate, mAudioChannel, mAudioFormat, buffersize * 10);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // start recording

        isStartRecord = true;

        recordThread = new Thread() {
            @Override
            public void run() {
                super.run();
                Looper.prepare();
                isStartRecord = true;
                try {
                    if (audioRecord != null) {
                        audioRecord.startRecording();
                    }
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    isStartRecord = false;
                }
                isQueryLog = true;
                while (isStartRecord) {
                    try {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int readBytes = audioRecord.read(buffer, 0, buffer.length);
                        if (readBytes > 0) {
                            ProcessData(buffer);
                            Message msg = new Message();
                            msg.obj = buffer;
                            handler.sendMessage(msg);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        isStartRecord = false;
                    }
                }

                try {
                    if (audioRecord != null) {
                        Log.d("MainActivity","audio released");
                        audioRecord.release();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    audioRecord = null;
                }

            }
        };

        recordThread.start();
    }

    public void stopRecord() {
        isStartRecord = false;
    }

    public void ProcessData(byte[] data) {

    }

    public byte[] mainBuffer;

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            byte[] displaydata = new byte[8];
            byte[] data = (byte[])(msg.obj);
            for(int i=0;i<8;i++)
                displaydata[i] = data[i];
            mainBuffer = data;
            DrawData(data);
        }
    };

    public byte[] getMainBuffer() {
        return mainBuffer;
    }

    public void DrawData(byte[] data) {
        mathView.setData(data);
    }

}
