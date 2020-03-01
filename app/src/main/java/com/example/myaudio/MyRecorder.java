package com.example.myaudio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.Process;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MyRecorder {

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

