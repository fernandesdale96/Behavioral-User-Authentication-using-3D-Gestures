package com.example.dayle_fernandes.fyp;

import android.util.Log;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import static org.apache.commons.math3.stat.StatUtils.mean;



import java.util.ArrayList;



public class DynamicTimeWarping {


    public static Double compute_warp(double[][] template, double[][] pattern) {
        int rows = template.length;
        //To Create NxM Dynamic Time Warping Matrix
        Array2DRowRealMatrix mat = new Array2DRowRealMatrix(template[0].length,pattern[0].length);
        //Normailizing Sensor Data
        //Ensure the features used have 0 mean and unit variance
        for(int i =0;i<rows;i++){
            double[] template_temp = template[i];
            double[] pattern_temp = pattern[i];

            double template_mean = mean(template_temp);
            double pattern_mean = mean(pattern_temp);

            double template_std = new StandardDeviation().evaluate(template_temp);
            double pattern_std = new StandardDeviation().evaluate(pattern_temp);

            //Normailze all values in the row
            for(int j =0; j < template_temp.length;j++){
                template_temp[j] = (template_temp[j] - template_mean)/template_std;

            }

            for(int j =0; j < pattern_temp.length;j++){
                pattern_temp[j] = (pattern_temp[j] - pattern_mean)/pattern_std;

            }

            Array2DRowRealMatrix template_remap = remap(pattern_temp.length,template_temp);
            Array2DRowRealMatrix pattern_remap = remap(template_temp.length,pattern_temp);

            pattern_remap = (Array2DRowRealMatrix) pattern_remap.transpose();
            Array2DRowRealMatrix submatrix = template_remap.subtract(pattern_remap);
            for(int x = 0; x < submatrix.getColumnDimension();x++){
               for(int y = 0; y < submatrix.getRowDimension();y++){
                  submatrix.setEntry(y,x,Math.pow(submatrix.getEntry(y,x),2));
                }
            }
            mat = mat.add(submatrix);

        }

        Log.d("Time Warping:","Feature normalization complete. Start finding path");

        Array2DRowRealMatrix temp_mat = new Array2DRowRealMatrix(template[0].length, pattern[0].length);
        temp_mat.setEntry(0,0,mat.getEntry(0,0));

        for(int i =1; i < template[0].length;i++){
            temp_mat.setEntry(i,0,mat.getEntry(i,0)+temp_mat.getEntry(i-1,0));

        }

        for(int j=1; j< pattern[0].length;j++){
            temp_mat.setEntry(0,j,mat.getEntry(0,j) + temp_mat.getEntry(0,j-1));
        }

        for(int i = 1; i < template[0].length;i++){
            for(int j = 1; j < pattern[0].length;j++){
                temp_mat.setEntry(i,j,mat.getEntry(i,j) + min(temp_mat.getEntry(i - 1, j), temp_mat.getEntry(i - 1, j - 1), temp_mat.getEntry(i, j - 1)));
            }
        }
        //Time Warping distance here
        Double warp_dist = temp_mat.getEntry(temp_mat.getRowDimension() - 1, temp_mat.getColumnDimension() - 1);
        Log.d("Distance:", Double.toString(warp_dist));
        return warp_dist;

        }


    public static double min(double a, double b, double c){
        return Math.min(Math.min(a, b), c);
    }


    private static Array2DRowRealMatrix remap(int length, double[] data){
        Array2DRowRealMatrix remap_matrix = new Array2DRowRealMatrix(new double[data.length][1]);
        remap_matrix.setColumn(0,data);

        ArrayList<Double> ones = new ArrayList<>();

        for(int i=0; i<length; i++){
            ones.add((double)1);
        }
        double[] ones_array = ArrayUtils.toPrimitive(ones.toArray(new Double[ones.size()]));
        Array2DRowRealMatrix ones_matrix = new Array2DRowRealMatrix(new double[1][length]);
        ones_matrix.setRow(0,ones_array);
        return remap_matrix.multiply(ones_matrix);

    }
}
