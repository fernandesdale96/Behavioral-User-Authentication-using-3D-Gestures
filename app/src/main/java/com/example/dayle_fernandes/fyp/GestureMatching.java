package com.example.dayle_fernandes.fyp;


import android.app.Activity;
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
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


import static com.example.dayle_fernandes.fyp.R.id.comp_graph;
import static com.example.dayle_fernandes.fyp.R.id.gest_graph;

public class GestureMatching extends Activity implements SensorEventListener{

    private ArrayList<Pair<Long,double[]>> sensorLog;
    private long startTime = 0;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private int compare_count = 0;

    private Button recorder;
    int gnum;
    private GraphView plot;
    private GraphView plot1;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        setContentView(R.layout.compare_gesture);
        gnum = 0;
        plot = (GraphView) findViewById(comp_graph);
        plot1 = (GraphView) findViewById(gest_graph);

        recorder = (Button) findViewById(R.id.record_gesture);
        recorder.setOnTouchListener(hold_button_record);
    }

    private View.OnTouchListener hold_button_record = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View v, MotionEvent event){
            if(event.getAction() == MotionEvent.ACTION_DOWN){
                //Gather Sensor Data
                sensorLog = new ArrayList<>();
                sensorManager.registerListener(GestureMatching.this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
            }
            if(event.getAction() == MotionEvent.ACTION_UP){
                //Stop Gathering Sensor Data
                Log.d("Debug:", "Up Action: Sensor Stops Recording");
                sensorManager.unregisterListener(GestureMatching.this);
                if(sensorLog.size() > 150){
                    SaveCSV(sensorLog,compare_count);
                    plotSensorLog(sensorLog, plot);
                    try{
                        plotRecordedGestures(plot1, 2);
                    }catch (Exception e){
                        Log.d("Plot Log:","File Not Found");
                        e.printStackTrace();
                    }
                    double[][] recordedGesture = GestureCompare.preProcess(sensorLog);
                    if(compareDistance(recordedGesture,sensorLog)){
                        Log.d("Gesture Match:", "True");
                        Toast.makeText(GestureMatching.this,"Gesture match: True",Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Log.d("Gesture Match:", "False");
                        Toast.makeText(GestureMatching.this,"Gesture match: False",Toast.LENGTH_SHORT).show();
                    }
                    compare_count++;
                }
                else{
                    Log.d("Record Error:", "Gesture not Long Enough");
                }

            }

            return true;
        }
    };


    public void plotRecordedGestures(GraphView plot, int a) throws IOException {
        File sdcard = Environment.getExternalStorageDirectory();
        String fname = sdcard.getAbsolutePath()+"/FYP/gesture/" + "gesture" + Integer.toString(a)+".csv";
        CSVReader reader = new CSVReader(new FileReader(fname), ',' , '"' , 0);

        List<String[]> records = reader.readAll();
        plot.removeAllSeries();
        LineGraphSeries<DataPoint> xseries = new LineGraphSeries<>();
        LineGraphSeries<DataPoint> yseries = new LineGraphSeries<>();
        LineGraphSeries<DataPoint> zseries = new LineGraphSeries<>();

        String[] temp = records.get(0);
        long start = Long.parseLong(temp[0]);
        Log.d("File opened:", Long.toString(start));
        for(int i = 0; i < records.size();i++){
            String[] record = records.get(i);
            long t = Long.parseLong(record[0]);
            double x = Double.parseDouble(record[1]);
            double y = Double.parseDouble(record[2]);
            double z = Double.parseDouble(record[3]);
            if(i == 0){
                xseries.appendData(new DataPoint(0,x), false, 250);
                yseries.appendData(new DataPoint(0,y), false, 250);
                zseries.appendData(new DataPoint(0,z), false, 250);
            }
            else{
                double time = (t- start)/Math.pow(10,6);
                xseries.appendData(new DataPoint(time,x),false,250);
                yseries.appendData(new DataPoint(time,y),false,250);
                zseries.appendData(new DataPoint(time,z),false,250);
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

    public void plotSensorLog(ArrayList<Pair<Long,double[]>> sensorLog, GraphView plot){

        plot.removeAllSeries();
        long start = sensorLog.get(0).first;
        String gesture = "Compare Gesture " + Integer.toString(gnum + 1);

        LineGraphSeries<DataPoint> xseries = new LineGraphSeries<>();
        LineGraphSeries<DataPoint> yseries = new LineGraphSeries<>();
        LineGraphSeries<DataPoint> zseries = new LineGraphSeries<>();

        for(int i=0; i< sensorLog.size();i++){
            long t = sensorLog.get(i).first;
            double[] record = sensorLog.get(i).second;
            double x = record[0];
            double y = record[1];
            double z = record[2];

            if(i == 0){
                xseries.appendData(new DataPoint(0,x), false, 250);
                yseries.appendData(new DataPoint(0,y), false, 250);
                zseries.appendData(new DataPoint(0,z), false, 250);
            }
            else{
                double time = (t- start)/Math.pow(10,6);
                xseries.appendData(new DataPoint(time,x),false,250);
                yseries.appendData(new DataPoint(time,y),false,250);
                zseries.appendData(new DataPoint(time,z),false,250);
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

    public void SaveCSV(ArrayList<Pair<Long,double[]>> sensorLog, int gnum){
        File sdcard = Environment.getExternalStorageDirectory();
        File dir = new File(sdcard.getAbsolutePath() + "/FYP/compare");
        dir.mkdir();
        try{
            if(gnum == 0){
                FileUtils.cleanDirectory(dir);
            }
        }catch (Exception e){};

        String fname = sdcard.getAbsolutePath()+"/FYP/compare/" + "compare" + Integer.toString(gnum)+".csv";


        try{
            CSVWriter writer = new CSVWriter(new FileWriter(fname, true));
            for(int i=0; i< sensorLog.size();i++){
                Long t = sensorLog.get(i).first;
                double[] record = sensorLog.get(i).second;
                double x = record[0];
                double y = record[1];
                double z = record[2];
                String[] append = new String[] {Long.toString(t), Double.toString(x), Double.toString(y),Double.toString(z)};
                System.out.print(append);
                writer.writeNext(append);
            }
            writer.close();
            Log.d("CSV Save:", "Success");
        }catch (Exception e){
            e.printStackTrace();
            Log.d("CSV Save:","Error");
        }

    }

    public double calculateGestureTime(ArrayList<Pair<Long,double[]>> sensorLog){
        long[] times = new long[sensorLog.size()];

        for(int i = 0; i < sensorLog.size(); i++){
            Long t = sensorLog.get(i).first;
            times[i] = t;
        }
        long start = times[0];
        long end = times[sensorLog.size() - 1];
        long elapsedTime = end - start;
        double gestureTime = (double) TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
        return gestureTime;
    }

    private boolean compareDistance(double[][] recordedGesture, ArrayList<Pair<Long,double[]>> sensorLog){
        FileInputStream c1fis, c2fis, c3fis;
        ObjectInputStream c1ois, c2ois, c3ois;
        double[][] gesture1, gesture2, gesture3;
        double currentGestureTime = calculateGestureTime(sensorLog);
        double averageGestureTime = RecordGesture.getAverageGestureTime();
        boolean flag = false;

        try{
            c1fis = new FileInputStream(new File(this.getFilesDir(), RecordGesture.SAVE_DIRECTORY + "gesture0"));
            c2fis = new FileInputStream(new File(this.getFilesDir(), RecordGesture.SAVE_DIRECTORY + "gesture1"));
            c3fis = new FileInputStream(new File(this.getFilesDir(), RecordGesture.SAVE_DIRECTORY + "gesture2"));

            c1ois = new ObjectInputStream(c1fis);
            c2ois = new ObjectInputStream(c2fis);
            c3ois = new ObjectInputStream(c3fis);

            gesture1 = (double[][]) c1ois.readObject();
            gesture2 = (double[][]) c2ois.readObject();
            gesture3 = (double[][]) c3ois.readObject();

            double average_distance = (GestureCompare.computePathDistance(gesture1,recordedGesture) +
                    GestureCompare.computePathDistance(gesture2,recordedGesture) +
                    GestureCompare.computePathDistance(gesture3,recordedGesture))/3;
            double initial_gesture_distance = RecordGesture.getDistance();

            double gesture_ratio = average_distance/initial_gesture_distance;
            double time_ratio = currentGestureTime/averageGestureTime;

            Log.d("Initial Distance:", Double.toString(initial_gesture_distance));
            Log.d("Average Distance:", Double.toString(average_distance));
            Log.d("Gesture Ratio:", Double.toString(gesture_ratio));
            Log.d("Time Ratio:", Double.toString(time_ratio));

            if(gesture_ratio < 1.2 && gesture_ratio > 0.8){
                if(time_ratio < 1.2 && time_ratio > 0.8){
                    flag =  true;
                }
            }
            else{
                flag =  false;
            }

        }catch (Exception e){
            e.printStackTrace();
            flag =  false;
        }
        return flag;

    }




    @Override
    public void onSensorChanged(SensorEvent event){
        if(event.sensor == accelerometer){
            if(startTime == 0){
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
