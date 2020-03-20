package com.example.myaudio;

import androidx.annotation.RequiresApi;
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

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
    };

    public static MainActivity main = null;

    @Override
    @RequiresApi(23)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this,PERMISSIONS, 1);
        InitButtons();
    }

    public MathView mathView;
    
    @RequiresApi(23)
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

        mfcc = new MFCC();
    }

    // below are the methods

    private static final int SAMPLE_RATE = 16000;
    private static final int BUFFER_SIZE = 800;             // 读取进来两帧，可以做三次mfcc
    public AudioRecord audioRecord;
    private Thread recordThread;
    private MFCC mfcc;

    private int mAudioSampleRate = SAMPLE_RATE;
    private int mAudioSource = MediaRecorder.AudioSource.MIC;
    private int mAudioFormat = AudioFormat.ENCODING_PCM_FLOAT;
    private int mAudioChannel = AudioFormat.CHANNEL_IN_MONO;

    private volatile boolean isQueryLog;
    private volatile boolean isStartRecord = false;

    @RequiresApi(23)
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
            audioRecord = new AudioRecord(mAudioSource, mAudioSampleRate, mAudioChannel, mAudioFormat, buffersize * 1);
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
                        float[] buffer = new float[BUFFER_SIZE];
                        // int readBytes = audioRecord.read(buffer, 0, buffer.length);
                        int readBytes = audioRecord.read(buffer, 0, buffer.length,AudioRecord.READ_NON_BLOCKING);
                        if (readBytes > 0) {
                            Message msg = new Message();
                            ProcessData(buffer);
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

    public void ProcessData(float[] data) {
        double[] buf_data = new double[data.length];

        for(int i=0;i<data.length;i++) buf_data[i] = data[i];

        double[][] result = mfcc.RunMFCC(buf_data);

        for(int i=0;i<result.length;i++) {
            Log.d("This is the result", Arrays.toString(result[i]));
        }

    }

    public float[] mainBuffer;

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            float[] data = (float[])(msg.obj);
            double[] fdata = new double[data.length];
            for(int i=0;i<data.length;i++) {
                fdata[i] = data[i];
            }
            mainBuffer = data;
            DrawData(data);
        }
    };

    public float[] getMainBuffer() {
        return mainBuffer;
    }

    public void DrawData(float[] data) {
        mathView.setData(data);
    }

}
