package com.freeform.writing;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.freeform.writing.Functions.Calibration;
import com.freeform.writing.Functions.LowPassFilter;
import com.freeform.writing.Functions.SegmentGeneration;
import com.freeform.writing.Model.DataSet;
import com.freeform.writing.Model.Segment;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<DataSet> incomingAccelerometerDataset;
    private List<DataSet> incomingGyroscopeDataset;
    private List<Segment> incomingSegmentDataset;
    private List<DataSet> updatedAccelerometer;
    private List<DataSet> updatedGyroscope;
    private List<Segment> segments;

    public static final int MULTIPLE_PERMISSIONS = 10;
    private final String[] permissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};

    private SegmentGeneration segmentGeneration;

    private boolean isAnalyzed = false,isRead=false;
    private Button graph;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        while(!hasPermissions(this,permissions)){
            ActivityCompat.requestPermissions(this,permissions,MULTIPLE_PERMISSIONS);
        }
        AddDirectory.addDirectory();
        init();
        onClickListners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!isRead){
            isRead=true;
            readIncomingDataSets();
        }
    }

    private void init() {
        isAnalyzed = false;
        isRead=false;
        incomingAccelerometerDataset = new ArrayList<>();
        incomingGyroscopeDataset = new ArrayList<>();
        incomingSegmentDataset = new ArrayList<>();
        updatedAccelerometer = new ArrayList<>();
        segments = new ArrayList<>();
        graph = findViewById(R.id.btn_analyze);
        if(!Python.isStarted()){
            Python.start(new AndroidPlatform(this));
        }
    }

    private void onClickListners() {
        graph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isAnalyzed){
                    isAnalyzed=true;
                    segmentGeneration.getmovingAverage();
                    segmentGeneration.getmovingVariance(0);
                    segmentGeneration.getmovingVariance(1);
                    segmentGeneration.thresholdChecking();
                    segments = segmentGeneration.generateSegment();

                    LowPassFilter lowPassFilterAcc = new LowPassFilter(incomingAccelerometerDataset,incomingSegmentDataset);
                    updatedAccelerometer = lowPassFilterAcc.applyLowPassFilter();
                    File file = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/.FFWList/updatedAccelerometer.csv");
                    try {
                        if(file.exists()) FileUtils.forceDelete(file);
                        FileWriter fileWriter = new FileWriter(Environment.getExternalStoragePublicDirectory(
                                "FreeForm-Writing/.FFWList/updatedAccelerometer.csv"),true);
                        for(DataSet dataSet : updatedAccelerometer){
                            String msg = dataSet.getTimeStamp()+","+dataSet.getxAxis()+","+dataSet.getyAxis()+","+dataSet.getzAxis();
                            fileWriter.write(msg + "\n");
                            fileWriter.flush();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    LowPassFilter lowPassFilterGyro = new LowPassFilter(incomingGyroscopeDataset,incomingSegmentDataset);
                    updatedGyroscope = lowPassFilterGyro.applyLowPassFilter();
                    File file1 = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/.FFWList/updatedGyro.csv");
                    try {
                        if(file1.exists()) FileUtils.forceDelete(file);
                        FileWriter fileWriter = new FileWriter(Environment.getExternalStoragePublicDirectory(
                                "FreeForm-Writing/.FFWList/updatedGyro.csv"),true);
                        for(DataSet dataSet : updatedGyroscope){
                            String msg = dataSet.getTimeStamp()+","+dataSet.getxAxis()+","+dataSet.getyAxis()+","+dataSet.getzAxis();
                            fileWriter.write(msg + "\n");
                            fileWriter.flush();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    incomingAccelerometerDataset.clear();
                    incomingGyroscopeDataset.clear();

                    Calibration calibration = new Calibration(updatedAccelerometer,updatedGyroscope,segments);
                    calibration.analyze();

                    graph.setText("Graph");
                    Toast.makeText(MainActivity.this,"Analysis Successful",Toast.LENGTH_LONG).show();
                }else{
                    Intent intent = new Intent(MainActivity.this,FilesAvailableAcitivity.class);
                    startActivity(intent);
                }
            }
        });
    }

    private void readIncomingDataSets() {
        InputStream inputStream1 = getResources().openRawResource(R.raw.acc_09272019);
        InputStream inputStream2 = getResources().openRawResource(R.raw.gyro_09272019);
        InputStream inputStream3 = getResources().openRawResource(R.raw.groundtruth_09272019);

        BufferedReader bufferedReader1 = new BufferedReader(new InputStreamReader(inputStream1, Charset.forName("UTF-8")));
        String inputLine1="";
        incomingAccelerometerDataset.clear();
        incomingGyroscopeDataset.clear();
        incomingSegmentDataset.clear();

        try {
            File file = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/.FFWList/incomingAccelerometerDataset.csv");
            if(file.exists()) FileUtils.forceDelete(file);
            FileWriter fileWriter = new FileWriter(Environment.getExternalStoragePublicDirectory(
                    "FreeForm-Writing/.FFWList/incomingAccelerometerDataset.csv"),true);
            while ((inputLine1 = bufferedReader1.readLine())!=null){
                //Split the data by ','
                String[] tokens = inputLine1.split(",");
                //Read the data
                DataSet dataSet = new DataSet("0",0,0,0);
                dataSet.setTimeStamp(tokens[0]);
                if(tokens.length>=2 && tokens[1].length()>0){
                    dataSet.setxAxis(Double.parseDouble(tokens[1]));
                }
                else dataSet.setxAxis(0);
                if(tokens.length>=3 && tokens[2].length()>0){
                    dataSet.setyAxis(Double.parseDouble(tokens[2]));
                }
                else dataSet.setyAxis(0);
                if(tokens.length>=4 && tokens[3].length()>0){
                    dataSet.setzAxis(Double.parseDouble(tokens[3]));
                }
                else dataSet.setzAxis(0);
                incomingAccelerometerDataset.add(dataSet);

                String msg = dataSet.getTimeStamp()+","+dataSet.getxAxis()+","+dataSet.getyAxis()+","+dataSet.getzAxis();
                fileWriter.write(msg + "\n");
                fileWriter.flush();
            }

        } catch (IOException e) {
            Log.wtf("MainActivity","Failed to read Accelerometer line:- " + inputLine1,e);
            e.printStackTrace();
        }
        BufferedReader bufferedReader2 = new BufferedReader(new InputStreamReader(inputStream2, Charset.forName("UTF-8")));
        String inputLine2="";
        try {
            File file = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/.FFWList/incomingGyroscopeDataset.csv");
            if(file.exists()) FileUtils.forceDelete(file);
            FileWriter fileWriter = new FileWriter(Environment.getExternalStoragePublicDirectory(
                    "FreeForm-Writing/.FFWList/incomingGyroscopeDataset.csv"),true);
            while ((inputLine2 = bufferedReader2.readLine())!=null){
                //Split the data by ','
                String[] tokens = inputLine2.split(",");
                //Read the data
                DataSet dataSet = new DataSet("0",0,0,0);
                dataSet.setTimeStamp(tokens[0]);
                if(tokens.length>=2 && tokens[1].length()>0){
                    dataSet.setxAxis(Double.parseDouble(tokens[1]));
                }
                else dataSet.setxAxis(0);
                if(tokens.length>=3 && tokens[2].length()>0){
                    dataSet.setyAxis(Double.parseDouble(tokens[2]));
                }
                else dataSet.setyAxis(0);
                if(tokens.length>=4 && tokens[3].length()>0){
                    dataSet.setzAxis(Double.parseDouble(tokens[3]));
                }
                else dataSet.setzAxis(0);
                incomingGyroscopeDataset.add(dataSet);

                String msg = dataSet.getTimeStamp()+","+dataSet.getxAxis()+","+dataSet.getyAxis()+","+dataSet.getzAxis();
                fileWriter.write(msg + "\n");
                fileWriter.flush();
            }
        } catch (IOException e) {
            Log.wtf("MainActivity","Failed to read Gyroscope line:- " + inputLine2,e);
            e.printStackTrace();
        }
        BufferedReader bufferedReader3 = new BufferedReader(new InputStreamReader(inputStream3, Charset.forName("UTF-8")));
        String inputLine3="";
        try {
            while ((inputLine3 = bufferedReader3.readLine())!=null){
                //Split the data by ','
                String[] tokens = inputLine3.split(",");
                //Read the data
                Segment segment = new Segment(0,0);
                segment.setStartTime(Long.parseLong(tokens[0]));
                if(tokens.length>=2 && tokens[1].length()>0){
                    String endTime = "";
                    for(int i=1;i<tokens[1].length();i++) endTime+=tokens[1].charAt(i);
                    segment.setEndTime(Long.parseLong(endTime));
                }
                else segment.setEndTime(0);
                incomingSegmentDataset.add(segment);
            }
        } catch (IOException e) {
            Log.wtf("MainActivity","Failed to read GroundTruth line:- " + inputLine3,e);
            e.printStackTrace();
        }

        segmentGeneration = new SegmentGeneration(incomingAccelerometerDataset,incomingSegmentDataset);
    }

    public boolean hasPermissions(Context context, String... permissions){
        if(context!=null && permissions!=null ){
            for(String permission:permissions){
                if(ActivityCompat.checkSelfPermission(context,permission)!= PackageManager.PERMISSION_GRANTED){
                    return false;
                }
            }
        }
        return true;
    }
}
