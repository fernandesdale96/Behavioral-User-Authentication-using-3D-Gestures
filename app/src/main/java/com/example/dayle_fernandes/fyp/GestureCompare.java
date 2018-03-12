package com.example.dayle_fernandes.fyp;

import android.util.Log;
import android.util.Pair;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import java.util.ArrayList;
import java.util.Arrays;

import static org.apache.commons.math3.stat.StatUtils.mean;
import static org.apache.commons.math3.stat.StatUtils.sum;


public class GestureCompare {
    public static double[] rollingAverage(double[] signal, int windowSize){
        ArrayList<Double> window = new ArrayList<>();
        double[] smoothedSignal = signal.clone();
        for (int i = 0; i < Math.min(windowSize, smoothedSignal.length); i++) {
            window.add(smoothedSignal[i]);
            smoothedSignal[i] = mean(doubleArrayListToArray(window));
        }
        for (int i = windowSize; i < smoothedSignal.length; i++) {
            window.remove(0);
            window.add(smoothedSignal[i]);
            smoothedSignal[i] = mean(doubleArrayListToArray(window));
        }
        return smoothedSignal;
    }

    public static double[] doubleArrayListToArray(ArrayList<Double> inArray) {
        double[] ret = new double[inArray.size()];
        for (int i = 0; i < inArray.size(); i++) {
            ret[i] = inArray.get(i);
        }
        return ret;
    }

    public static double[][] smoothSensorLog(ArrayList<Pair<Long, double[]>> sensorLog) {
        double[] x = new double[sensorLog.size()];
        double[] y = new double[sensorLog.size()];
        double[] z = new double[sensorLog.size()];

        // load data to vectors
        for (int i = 0; i < sensorLog.size(); i++) {
            double[] record = sensorLog.get(i).second;
            x[i] = record[0];
            y[i] = record[1];
            z[i] = record[2];
        }

        Log.d("Smoothing:","Start");
        x = rollingAverage(x, 7);
        y = rollingAverage(y, 7);
        z = rollingAverage(z, 7);
        Log.d("Smoothing:","End");

        double[][] ret = new double[sensorLog.size()][3];
        for (int i = 0; i < sensorLog.size(); i++) {
            ret[i] = new double[]{x[i], y[i], z[i]};
        }
        return ret;
    }

    public static double[][] matrixTranspose(double[][] A) {
        double[][] ret = new double[A[0].length][A.length];
        for (int i = 0; i < A.length; i++) {
            for (int j = 0; j < A[0].length; j++) {
                ret[j][i] = A[i][j];
            }
        }
        return ret;
    }

    public static double[][] preProcess(ArrayList<Pair<Long, double[]>> sensorLog) {
        double[][] arr = matrixTranspose(smoothSensorLog(sensorLog));
        double[][] abs_arr = new double[3][arr[0].length];
        for (int i = 0; i < 3; i++) {

            double sm_std = new StandardDeviation().evaluate(arr[i]);
            double sm_m = mean(arr[i]);
            System.out.print(sm_std + sm_m);

            for (int j = 0; j < arr[i].length; j++) {
                arr[i][j] = (arr[i][j] - sm_m) / sm_std;
                abs_arr[i][j] = Math.abs(arr[i][j]);
            }

        }

        double eta = 0;
        int[] ps_arr = new int[3];
        int[] pe_arr = new int[3];
        for (int i = 0; i < 3; i++) {
            eta = sum(abs_arr[i]) / abs_arr[i].length;

            for (int j = 0; j < abs_arr[i].length; j++) {
                if (abs_arr[i][j] > eta) {
                    ps_arr[i] = j;
                    break;
                }

            }
            for (int j = abs_arr[i].length - 1; j >= 0; j--) {
                if (abs_arr[i][j] > eta) {
                    pe_arr[i] = j;
                    break;
                }
            }
        }
        int ps = Math.min(Math.min(ps_arr[0], ps_arr[1]), ps_arr[2]);
        int pe = Math.max(Math.max(pe_arr[0], pe_arr[1]), pe_arr[2]);
        /*String as = Integer.toString(ps);
        String aas = Integer.toString(pe);
        Log.d("PS: ", as);
        Log.d("PE: ", aas);*/

        double[][] subArr = new double[3][arr[0].length];
        for (int i = 0; i < 3; i++) {
            subArr[i] = Arrays.copyOfRange(arr[i], ps, pe);
        }

        return subArr;


    }


    public static double computePathDistance(double[][] template, double [][] pattern){
        Double warp_distance = DynamicTimeWarping.compute_warp(template, pattern);

        return warp_distance;
    }


}
