package com.example.dayle_fernandes.fyp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.opencsv.CSVWriter;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static com.example.dayle_fernandes.fyp.R.id.graph;


public class RecordGesture extends Activity implements SensorEventListener {

    private ArrayList<Pair<Long, double[]>> sensorLog;
    private long startTime = 0;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private int gestureCount = 0;
    private Button recorder;
    private static double initialDistance = 0;
    private static double averageGestureTime = 0;
    public static final String SAVE_DIRECTORY = "gestures/";
    final int REQUEST_CODE_ASK_PERMISSIONS = 123;
    private GraphView plot;
    private double[] timeList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        setContentView(R.layout.record_gesture);
        checkPermissions();
        plot = (GraphView) findViewById(graph);
        timeList = new double[3];

        recorder = (Button) findViewById(R.id.record_gesture);
        recorder.setOnTouchListener(hold_button_record);
    }

    private View.OnTouchListener hold_button_record = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                //Gather Sensor Data
                sensorLog = new ArrayList<>();
                sensorManager.registerListener(RecordGesture.this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                //Stop Gathering Sensor Data
                Log.d("Debug:", "Up Action: Sensor Stops Recording");
                sensorManager.unregisterListener(RecordGesture.this);
                if (sensorLog.size() > 150) {
                    plotSensorLog(sensorLog, gestureCount);
                    SaveGesture(sensorLog, RecordGesture.this, gestureCount);
                    SaveCSV(sensorLog, gestureCount);
                    calculateGestureTime(sensorLog, gestureCount);
                    gestureCount++;
                    String astring = Integer.toString(3 - gestureCount);
                    if (gestureCount != 3) {
                        Toast.makeText(RecordGesture.this, "Gesture Saved!! Please repeat gesture " + astring + " more times", Toast.LENGTH_SHORT).show();
                    }
                    if (gestureCount == 3) {
                        try {
                            averageGestureTime = calculateAverage(timeList);
                            initialDistance = getGestureDistance();
                            Toast.makeText(RecordGesture.this, "Distance " + Double.toString(initialDistance), Toast.LENGTH_SHORT).show();
                            Log.d("Average Gesture Time:", Double.toString(averageGestureTime));
                        } catch (Exception e) {
                        }

                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(RecordGesture.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        }, 5000);

                    }

                } else {
                    Toast.makeText(RecordGesture.this, "Gesture is not long enough, please try again", Toast.LENGTH_SHORT).show();
                    Log.d("Log Size:", Integer.toString(sensorLog.size()));
                }
            }
            return true;
        }
    };

    public void plotSensorLog(ArrayList<Pair<Long, double[]>> sensorLog, int gnum) {
        plot.removeAllSeries();
        long start = sensorLog.get(0).first;
        String gesture = "Gesture " + Integer.toString(gnum + 1);

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


    }

    public double calculateAverage(double[] timeList) {
        int sum = 0;
        for (int i = 0; i < timeList.length; i++) {
            sum += timeList[i];
        }

        return (double) sum / 3;
    }

    public void calculateGestureTime(ArrayList<Pair<Long, double[]>> sensorLog, int gnum) {
        long[] times = new long[sensorLog.size()];

        for (int i = 0; i < sensorLog.size(); i++) {
            Long t = sensorLog.get(i).first;
            times[i] = t;
        }
        long start = times[0];
        long end = times[sensorLog.size() - 1];
        long elapsedTime = end - start;
        double gestureTime = (double) TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
        Log.d("Elapsed Time:", Double.toString(gestureTime));
        timeList[gnum] = gestureTime;

    }


    public void SaveCSV(ArrayList<Pair<Long, double[]>> sensorLog, int gnum) {
        File sdcard = Environment.getExternalStorageDirectory();
        File dir = new File(sdcard.getAbsolutePath() + "/FYP/gesture");
        dir.mkdir();
        try {
            if (gnum == 0) {
                FileUtils.cleanDirectory(dir);
            }
        } catch (Exception e) {
        }
        ;

        String fname = sdcard.getAbsolutePath() + "/FYP/gesture/" + "gesture" + Integer.toString(gnum) + ".csv";


        try {
            CSVWriter writer = new CSVWriter(new FileWriter(fname, true));
            for (int i = 0; i < sensorLog.size(); i++) {
                Long t = sensorLog.get(i).first;
                double[] record = sensorLog.get(i).second;
                double x = record[0];
                double y = record[1];
                double z = record[2];
                String[] append = new String[]{Long.toString(t), Double.toString(x), Double.toString(y), Double.toString(z)};
                writer.writeNext(append);
            }
            writer.close();
            Log.d("CSV Save:", "Success");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("CSV Save:", "Error");
        }

    }

    public void SaveGesture(ArrayList<Pair<Long, double[]>> sensorLog, Activity act, int gesturNum) {
        File gestureDir = new File(act.getFilesDir(), SAVE_DIRECTORY);
        gestureDir.mkdir();
        try {
            if (gesturNum == 0) {
                FileUtils.cleanDirectory(gestureDir);
            }
        } catch (Exception e) {
        }
        ;

        String fname = "gesture" + Integer.toString(gesturNum);

        File gesture = new File(gestureDir, fname);
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(gesture);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            double[][] sensorData = GestureCompare.preProcess(sensorLog);
            os.writeObject(sensorData);
            Log.d("File Save:", "Success");

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(act, "File could not be saved", Toast.LENGTH_SHORT).show();
            Log.d("File Save:", "Error");
        }
    }

    private void checkPermissions() {
        int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_ASK_PERMISSIONS);
            return;
        }
        Toast.makeText(getBaseContext(), "Permission is already granted", Toast.LENGTH_LONG).show();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    Toast.makeText(RecordGesture.this, "Permission Granted", Toast.LENGTH_LONG).show();
                } else {
                    // Permission Denied
                    Toast.makeText(RecordGesture.this, "WRITE_EXTERNAL_STORAGE Denied", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) {
            if (startTime == 0) startTime = event.timestamp;
            sensorLog.add(new Pair<>(event.timestamp,
                    new double[]{event.values[0], event.values[1], event.values[2]}));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
//Do nothing
    }

    public static double getDistance() {
        return initialDistance;
    }

    public static double getAverageGestureTime() {
        return averageGestureTime;
    }

    private double getGestureDistance() throws IOException, ClassNotFoundException {
        FileInputStream c1fis, c2fis;
        ObjectInputStream c1ois, c2ois;

        double[][] gesture1, gesture2;
        c1fis = new FileInputStream(new File(this.getFilesDir(), SAVE_DIRECTORY + "gesture0"));
        c2fis = new FileInputStream(new File(this.getFilesDir(), SAVE_DIRECTORY + "gesture1"));
        c1ois = new ObjectInputStream(c1fis);
        c2ois = new ObjectInputStream(c2fis);
        gesture1 = (double[][]) c1ois.readObject();
        gesture2 = (double[][]) c2ois.readObject();
        double[][] gesture3 = GestureCompare.preProcess(sensorLog);
        double initdist = (GestureCompare.computePathDistance(gesture1, gesture2) +
                GestureCompare.computePathDistance(gesture2, gesture3) +
                GestureCompare.computePathDistance(gesture1, gesture3)) / 3;
        String astr = Double.toString(initdist);
        System.out.print(astr);
        Log.d("Initial Distance", astr);
        return initdist;
    }


}
