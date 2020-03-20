package com.example.myaudio;

import android.util.Log;

import java.util.Arrays;

public class DFT {
    protected int numPoints;
    protected int maxAmp = 8000;
    public double[] real;
    public double[] imag;
    public double[] amplitude;

    public DFT() {

    }

    public void computeDFT(double[] framedSignal,int nfft) {
        int rfft = nfft/2+1;
        numPoints = framedSignal.length;
        real = new double[numPoints];
        imag = new double[numPoints];
        amplitude = new double[rfft];

        double[] real_buffer = new double[numPoints];
        double[] imag_buffer = new double[numPoints];
        for(int k=0;k<rfft;k++) {
            for(int n=0;n<numPoints;n++) {
                real_buffer[n] = framedSignal[n] * Math.cos(2*Math.PI/numPoints*k*n);
                imag_buffer[n] = framedSignal[n] * Math.sin(2*Math.PI/numPoints*k*n);
                real[k] += real_buffer[n];
                imag[k] += imag_buffer[n];
            }
            amplitude[k] = (Math.sqrt(real[k]*real[k]+imag[k]*imag[k]));
        }
        Log.d("DFT singnal --------", Arrays.toString(framedSignal));
        Log.d("real -------- ",Arrays.toString(real));
        Log.d("imag -------- ",Arrays.toString(imag));
        Log.d("amplitude -------- ",Arrays.toString(amplitude));
    }
}
