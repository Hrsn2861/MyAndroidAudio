package com.example.myaudio;

import android.util.Log;

import java.lang.reflect.Array;
import java.util.Arrays;

public class MFCC {

    private String TAG = "MFCC Calculator ";

    protected int numFrames;
    protected int samplerate = 16000;
    protected double winlen = 0.025;
    protected double winstep = 0.01;
    protected int numcep = 13;
    protected int nfilt = 26;
    protected int nfft = 512;
    protected int lowfreq = 0;
    protected int highfreq = samplerate / 2;
    protected double preEmphasize = 0.97;
    protected int ceptlifter = 22;
    protected boolean appendEnergy = true;
    protected double[][] framedSignal;
    protected double[][] magSpectrum;
    protected double[][] powSpectrum;
    protected double[] energy;
    protected double hammngCoef = 0.46;

    protected double[][] cept;
    double[][] feat;

    double[][] fbank;

    DFT dft;
    DCT dct;

    public MFCC() {
        getFilterBanks();
        // for(int i=0;i<fbank.length;i++) {
        //     Log.d("Check ----- ",Arrays.toString(fbank[i]));
        // }
    }

    public double[][] RunMFCC(double[] signal) {
        // 0. initial
        highfreq = samplerate / 2;
        dft = new DFT();
        dct = new DCT(numcep);

        /*
        String test = "";
        for(int i=0;i<signal.length;i++) {
            test = test + signal[signal.length-i-1];
        }
        Log.d("Inverse Signal ========",test);
        */

        // 1. pre emphasis
        signal = preEmphasis(signal);

        // 2. framing the signal
        procFrames(signal);

        // 3. fbank alog
        fbank(signal);

        cept = new double[numFrames][numcep];

        for(int i=0;i<numFrames;i++) {
            assert feat[i].length == cept[i].length;
            System.arraycopy(dct.performDCT(feat[i]),0,cept[i],0,cept[i].length);
        }

        Lifter();

        if(appendEnergy) {
            for(int i=0;i<cept.length;i++) {
                if(energy[i] == 0) {
                    Log.e("Error with energy", "zero found");
                    // System.exit(0);
                }
                cept[i][0] = Math.log(energy[i]);
            }
        }

        return cept;

    }

    public void Lifter() {
        if(ceptlifter > 0) {
            int nframes = cept.length;
            int ncoeff = cept[0].length;
            double[] n = new double[ncoeff];
            double[] lift = new double[ncoeff];
            for(int i=0;i<ncoeff;i++) {
                n[i] = i;
            }
            for(int i=0;i<ncoeff;i++) {
                lift[i] = 1 + (ceptlifter/2.0)*Math.sin(Math.PI*n[i]/ceptlifter);
            }
            for(int i=0;i<nframes;i++) {
                for(int j=0;j<ncoeff;j++) {
                    cept[i][j] = cept[i][j] * lift[j];
                }
            }
        }
    }

    // 将得到的信号进行预加重
    public double[] preEmphasis(double[] signal) {
        double[] new_signal = new double[signal.length];
        new_signal[0] = signal[0];

        for(int i=1;i<signal.length;i++) {
            new_signal[i] = signal[i] - preEmphasize * signal[i-1];
        }

        return  new_signal;
    }

    public void procFrames(double[] signal) {
        long slen = signal.length;
        int frame_len = (int)Math.round(winlen * samplerate);
        int frame_step = (int)Math.round(winstep * samplerate);

        if(slen < frame_len) numFrames = 1;
        else {
            numFrames = 1 + (int)(Math.ceil((1.0*slen - frame_len) / frame_step));
        }

        int padlen = (int)((numFrames - 1) * frame_step + frame_len);

        double[] padsignal = new double[padlen];

        Log.d(TAG,padlen+" "+frame_len+" "+frame_step+" "+numFrames+" "+slen);

        System.arraycopy(signal,0,padsignal,0,signal.length);

        // 建立帧的二维数组
        framedSignal = new double[numFrames][frame_len];
        magSpectrum = new double[numFrames][frame_len];
        powSpectrum = new double[numFrames][frame_len];
        energy = new double[numFrames];

        for(int i=0;i<numFrames;i++) {
            System.arraycopy(padsignal,i*frame_step,framedSignal[i],0,frame_len);
        }

        return;

    }

    public void fbank(double[] signal) {
        // 对于每一帧加上汉明窗
        for(int f=0;f<framedSignal.length;f++) {
            for(int i=0;i<framedSignal[f].length;i++) {
                framedSignal[f][i] *= ((1.0 - hammngCoef) - hammngCoef * Math.cos((2*Math.PI*i)/(framedSignal[f].length-1)));
            }
        }

        // 对于每一帧求DFT
        // 在这里面因为做了rfft而不是fft，所以只有前一部分有数值；amplitude长度是nfft/2+1;
        for(int f=0;f<magSpectrum.length;f++) {
            // Log.d("Before DFT ++++++++", Arrays.toString(framedSignal[f]));
            dft.computeDFT(framedSignal[f],nfft);
            // 这里面amplitude的长度为nfft/2+1，剩下的都为0；
            // assert amplitude.length == nfft/2 + 1;
            System.arraycopy(dft.amplitude,0,magSpectrum[f],0,dft.amplitude.length);
        }

        // 对于每一个数据点计算pow
        for(int f=0;f<powSpectrum.length;f++) {
            for(int i=0;i<powSpectrum[f].length;i++) {
                powSpectrum[f][i] = Math.pow(magSpectrum[f][i],2) / nfft;
                energy[f] += powSpectrum[f][i];// 计算每一帧的总能量
            }
        }

        // find filters
        // getFilterBanks(); 一次性的filter
        assert fbank[0].length == (nfft/2+1);
        feat = new double[numFrames][nfilt];
        // feat = powSpectrum dot filter^T;
        for(int i=0;i<feat.length;i++) {
            for(int j=0;j<feat[i].length;j++) {
                for(int k=0;k<Math.floor(nfft/2)+1;k++) {
                    feat[i][j] += powSpectrum[i][k] * fbank[j][k];
                }
            }
        }

        // feat = np.log(feat)
        for(int i=0;i<feat.length;i++) {
            for(int j=0;j<feat[i].length;j++) {
                feat[i][j] = Math.log(feat[i][j]);
            }
            // Log.d("result ++++++++++", Arrays.toString(feat[i]));
        }

    }

    private void getFilterBanks() {
        double lowMel = hz2mel(lowfreq);
        double highMel = hz2mel(highfreq);

        double step = (highMel - lowMel) / (nfilt + 1);
        double[] melpoints = new double[nfilt+2];
        for(int i=0;i<melpoints.length;i++) {
            melpoints[i] = i * step + lowMel;
        }

        double[] bin = new double[nfilt+2];
        for(int i=0;i<bin.length;i++) {
            bin[i] = Math.floor((nfft+1)*mel2hz(melpoints[i])/samplerate);
        }

        int col = (int)Math.floor(nfft/2) + 1;
        fbank = new double[nfilt][col];

        for(int j=0;j<nfilt;j++) {
            for(int i=(int)bin[j];i<(int)bin[j+1];i++)
                fbank[j][i] = (i - bin[j]) / (bin[j+1] - bin[j]);
            for(int i=(int)bin[j+1];i<(int)bin[j+2];i++)
                fbank[j][i] = (bin[j+2] - i) / (bin[j+2] - bin[j+1]);
        }

        // 这个是一个稀疏带状矩阵

    }

    private double mel2hz(double x) {
        double temp = Math.pow(10, x / 2595) - 1;
        return 700 * (temp);
    }

    protected double hz2mel(double freq) {
        return 2595 * log10(1 + freq / 700);
    }

    private double log10(double value) {
        return Math.log(value) / Math.log(10);
    }

}
