package com.example.myaudio;

import android.util.Log;

import java.util.Arrays;

public class DCT {

    /**
     * number of mfcc coeffs
     */
    int numCepstra;
    /**
     * number of Mel Filters
     */

    public DCT(int numCepstra) {
        this.numCepstra = numCepstra;
    }

    public double[] performDCT(double x[]) {
        double N = x.length;
        double cepc[] = new double[numCepstra];

        // perform DCT
        /*
        for (int n = 1; n <= numCepstra; n++) {
            for (int i = 1; i <= N; i++) {
                cepc[n - 1] += x[i - 1] * Math.cos(Math.PI * (n - 1) / N * (i - 0.5));
            }
        }
        */

        for(int k=0;k<numCepstra;k++) {
            for(int n=0;n<N;n++) {
                cepc[k] += x[n] *  Math.cos((Math.PI*k*(2*n+1))/(2*N));
            }
            cepc[k] *= 2;
        }

        cepc[0] *= Math.sqrt(1/(4*N));
        for(int i=1;i<numCepstra;i++) {
            cepc[i] *= Math.sqrt(1/(2*N));
        }

        return cepc;
    }

}
