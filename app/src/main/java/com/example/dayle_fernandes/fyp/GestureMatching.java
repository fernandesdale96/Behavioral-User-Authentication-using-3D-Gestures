package com.example.dayle_fernandes.fyp;


import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.opencsv.CSVWriter;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


import static com.example.dayle_fernandes.fyp.R.id.comp_graph;

public class GestureMatching extends Activity implements SensorEventListener {

    private ArrayList<Pair<Long, double[]>> sensorLog;
    private long startTime = 0;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private int compare_count = 0;

    private Button recorder;
    int gnum;
    GraphView plot;
    TextView gesture_match;
    TextView speed_ratio;
    TextView distance_ratio;
    Button firstFragment, secondFragment, thirdFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        setContentView(R.layout.compare_gesture);
        gnum = 0;
        firstFragment = (Button) findViewById(R.id.firstFragment);
        secondFragment = (Button) findViewById(R.id.secondFragment);
        thirdFragment = (Button) findViewById(R.id.thirdFragment);

        firstFragment.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                loadFragment(new FirstFragment());
            };

        });
        secondFragment.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                loadFragment(new SecondFragment());
            };

        });
        thirdFragment.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                loadFragment(new ThirdFragment());
            };

        });

        plot = (GraphView) findViewById(comp_graph);

        gesture_match = (TextView) findViewById(R.id.gest_match);
        speed_ratio = (TextView) findViewById(R.id.speed_ratio);
        distance_ratio = (TextView) findViewById(R.id.distance_ratio);

        recorder = (Button) findViewById(R.id.record_gesture);
        recorder.setOnTouchListener(hold_button_record);
    }

    private void loadFragment(Fragment fragment){
        FragmentManager fm = getFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.replace(R.id.frameLayout,fragment);
        fragmentTransaction.commit();
    }

    private View.OnTouchListener hold_button_record = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                //Gather Sensor Data
                sensorLog = new ArrayList<>();
                sensorManager.registerListener(GestureMatching.this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                //Stop Gathering Sensor Data
                Log.d("Debug:", "Up Action: Sensor Stops Recording");
                sensorManager.unregisterListener(GestureMatching.this);
                if (sensorLog.size() > 150) {
                    SaveCSV(sensorLog, compare_count);
                    plotSensorLog(sensorLog, plot);
                    double[][] recordedGesture = GestureCompare.preProcess(sensorLog);
                    if (compareDistance(recordedGesture, sensorLog)) {
                        Log.d("Gesture Match:", "True");
                        gesture_match.setText("True");
                    } else {
                        Log.d("Gesture Match:", "False");
                        gesture_match.setText("False");
                    }
                    compare_count++;
                } else {
                    Log.d("Record Error:", "Gesture not Long Enough");
                }

            }

            return true;
        }
    };



    public void plotSensorLog(ArrayList<Pair<Long, double[]>> sensorLog, GraphView plot) {

        plot.removeAllSeries();
        long start = sensorLog.get(0).first;
        String gesture = "Compare Gesture " + Integer.toString(gnum + 1);

        LineGraphSeries<DataPoint> xseries = new LineGraphSeries<>();
        LineGraphSeries<DataPoint> yseries = new LineGraphSeries<>();
        LineGraphSeries<DataPoint> zseries = new LineGraphSeries<>();

        for (int i = 0; i < sensorLog.size(); i++) {
            long t = sensorLog.get(i).first;
            double[] record = sensorLog.get(i).second;
            double x = record[0];
            double y = record[1];
            double z = record[2];

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
        plot.setTitle(gesture);
        plot.addSeries(xseries);
        plot.addSeries(yseries);
        plot.addSeries(zseries);
        plot.getLegendRenderer().setVisible(true);
        plot.refreshDrawableState();
        gnum++;

    }

    public void SaveCSV(ArrayList<Pair<Long, double[]>> sensorLog, int gnum) {
        File sdcard = Environment.getExternalStorageDirectory();
        File dir = new File(sdcard.getAbsolutePath() + "/FYP/compare");
        dir.mkdir();
        try {
            if (gnum == 0) {
                FileUtils.cleanDirectory(dir);
            }
        } catch (Exception e) {
        }
        ;

        String fname = sdcard.getAbsolutePath() + "/FYP/compare/" + "compare" + Integer.toString(gnum) + ".csv";


        try {
            CSVWriter writer = new CSVWriter(new FileWriter(fname, true));
            for (int i = 0; i < sensorLog.size(); i++) {
                Long t = sensorLog.get(i).first;
                double[] record = sensorLog.get(i).second;
                double x = record[0];
                double y = record[1];
                double z = record[2];
                String[] append = new String[]{Long.toString(t), Double.toString(x), Double.toString(y), Double.toString(z)};
                System.out.print(append);
                writer.writeNext(append);
            }
            writer.close();
            Log.d("CSV Save:", "Success");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("CSV Save:", "Error");
        }

    }

    public double calculateGestureTime(ArrayList<Pair<Long, double[]>> sensorLog) {
        long[] times = new long[sensorLog.size()];

        for (int i = 0; i < sensorLog.size(); i++) {
            Long t = sensorLog.get(i).first;
            times[i] = t;
        }
        long start = times[0];
        long end = times[sensorLog.size() - 1];
        long elapsedTime = end - start;
        double gestureTime = (double) TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
        return gestureTime;
    }

    private boolean compareDistance(double[][] recordedGesture, ArrayList<Pair<Long, double[]>> sensorLog) {
        FileInputStream c1fis, c2fis, c3fis;
        ObjectInputStream c1ois, c2ois, c3ois;
        double[][] gesture1, gesture2, gesture3;
        double currentGestureTime = calculateGestureTime(sensorLog);
        double averageGestureTime = RecordGesture.getAverageGestureTime();
        boolean flag = false;

        try {
            c1fis = new FileInputStream(new File(this.getFilesDir(), RecordGesture.SAVE_DIRECTORY + "gesture0"));
            c2fis = new FileInputStream(new File(this.getFilesDir(), RecordGesture.SAVE_DIRECTORY + "gesture1"));
            c3fis = new FileInputStream(new File(this.getFilesDir(), RecordGesture.SAVE_DIRECTORY + "gesture2"));

            c1ois = new ObjectInputStream(c1fis);
            c2ois = new ObjectInputStream(c2fis);
            c3ois = new ObjectInputStream(c3fis);

            gesture1 = (double[][]) c1ois.readObject();
            gesture2 = (double[][]) c2ois.readObject();
            gesture3 = (double[][]) c3ois.readObject();

            double average_distance = (GestureCompare.computePathDistance(gesture1, recordedGesture) +
                    GestureCompare.computePathDistance(gesture2, recordedGesture) +
                    GestureCompare.computePathDistance(gesture3, recordedGesture)) / 3;
            double initial_gesture_distance = RecordGesture.getDistance();

            double gesture_ratio = average_distance / initial_gesture_distance;
            double time_ratio = currentGestureTime / averageGestureTime;

            Log.d("Initial Distance:", Double.toString(initial_gesture_distance));
            Log.d("Average Distance:", Double.toString(average_distance));
            Log.d("Gesture Ratio:", Double.toString(gesture_ratio));
            Log.d("Time Ratio:", Double.toString(time_ratio));
            distance_ratio.setText(String.format("%.4f",gesture_ratio));
            speed_ratio.setText(String.format("%.4f",time_ratio));
            if (gesture_ratio < 1.2 && gesture_ratio > 0.8) {
                if (time_ratio < 1.2 && time_ratio > 0.8) {
                    flag = true;
                }
            } else {
                flag = false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            flag = false;
        }
        return flag;

    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) {
            if (startTime == 0) {
                startTime = event.timestamp;
            }
            sensorLog.add(new Pair<>(event.timestamp, new double[]{event.values[0], event.values[1], event.values[2]}));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Do nothing
    }


}
