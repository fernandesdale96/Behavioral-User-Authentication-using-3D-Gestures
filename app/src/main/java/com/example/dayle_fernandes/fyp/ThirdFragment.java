package com.example.dayle_fernandes.fyp;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dayle_fernandes on 18-Mar-18.
 */

public class ThirdFragment extends Fragment {
    View view;
    GraphView graph;
    TextView gesture_match, speed_ratio, distance_ratio;
    GestureMatching gestureAct;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        view = inflater.inflate(R.layout.record_gesture3, container,false);
        graph = (GraphView) view.findViewById(R.id.gest_graph3);
        gesture_match = (TextView) view.findViewById(R.id.gest_match3);
        speed_ratio = (TextView) view.findViewById(R.id.speed_ratio3);
        distance_ratio = (TextView) view.findViewById(R.id.distance_ratio3);

        gestureAct = (GestureMatching) getActivity();

        ArrayList<Pair<Long, double[]>> sensorLog = gestureAct.sensorLog;
        double gestureTime = gestureAct.gestureTime;
        double[][] recordedGesture = GestureCompare.preProcess(sensorLog);
        compareGesture(recordedGesture,gestureTime);

        try {

            plotRecordedGestures(graph, 3);
        } catch (Exception e) {
            Log.d("Plot Log:", "File Not Found");
            e.printStackTrace();
        }

        return view;

    }

    public void compareGesture(double[][] recordedGesture, double gestureTime){
        FileInputStream c2fis;
        ObjectInputStream c2ois;
        double[][]  otherGesture;
        String path = "/data/user/0/com.example.dayle_fernandes.fyp/files/" + RecordGesture.SAVE_DIRECTORY + "gesture2";
        boolean flag = false;

        try{
            c2fis = new FileInputStream(new File(path));
            c2ois = new ObjectInputStream(c2fis);
            otherGesture = (double[][]) c2ois.readObject();

            double warp_distace = GestureCompare.computePathDistance(recordedGesture,otherGesture);
            double[] timeArray = RecordGesture.getTimeList();
            double recordTime = timeArray[2];


            double initial_gesture_distance = RecordGesture.getDistance();

            double gesture_ratio = warp_distace / initial_gesture_distance;
            double time_ratio = gestureTime / recordTime;

            distance_ratio.setText(String.format("%.4f",gesture_ratio));
            speed_ratio.setText(String.format("%.4f",time_ratio));
            if (gesture_ratio < 1.25 && gesture_ratio > 0.75) {
                if (time_ratio < 1.15 && time_ratio > 0.85) {
                    flag = true;
                }
            } else {
                flag = false;
            }

            if(flag){
                gesture_match.setText("True");
            }else{
                gesture_match.setText("False");
            }



        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void plotRecordedGestures(GraphView plot, int a) throws IOException {
        File sdcard = Environment.getExternalStorageDirectory();
        String fname = sdcard.getAbsolutePath() + "/FYP/gesture/" + "gesture" + Integer.toString(a - 1) + ".csv";
        CSVReader reader = new CSVReader(new FileReader(fname), ',', '"', 0);

        List<String[]> records = reader.readAll();
        plot.removeAllSeries();
        LineGraphSeries<DataPoint> xseries = new LineGraphSeries<>();
        LineGraphSeries<DataPoint> yseries = new LineGraphSeries<>();
        LineGraphSeries<DataPoint> zseries = new LineGraphSeries<>();

        String[] temp = records.get(0);
        long start = Long.parseLong(temp[0]);
        Log.d("File opened:", Long.toString(start));
        for (int i = 0; i < records.size(); i++) {
            String[] record = records.get(i);
            long t = Long.parseLong(record[0]);
            double x = Double.parseDouble(record[1]);
            double y = Double.parseDouble(record[2]);
            double z = Double.parseDouble(record[3]);
            if (i == 0) {
                xseries.appendData(new DataPoint(0, x), false, 250);
                yseries.appendData(new DataPoint(0, y), false, 250);
                zseries.appendData(new DataPoint(0, z), false, 250);
            } else {
                double time = (t - start) / Math.pow(10, 6);
                xseries.appendData(new DataPoint(time, x), false, 250);
                yseries.appendData(new DataPoint(time, y), false, 250);
                zseries.appendData(new DataPoint(time, z), false, 250);
            }

        }

        xseries.setTitle("X Axis");
        xseries.setColor(Color.GREEN);
        yseries.setTitle("Y Axis");
        yseries.setColor(Color.BLUE);
        zseries.setTitle("Z Axis");
        zseries.setColor(Color.RED);

        plot.getViewport().setMinY(-7.0);
        plot.getViewport().setMaxY(7.0);
        plot.getViewport().setYAxisBoundsManual(true);
        plot.setTitle("Recorded Gesture " + a);
        plot.addSeries(xseries);
        plot.addSeries(yseries);
        plot.addSeries(zseries);
        plot.getLegendRenderer().setVisible(true);
        plot.refreshDrawableState();

    }
}
