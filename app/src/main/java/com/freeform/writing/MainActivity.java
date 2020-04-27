package com.freeform.writing;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<DataSet> incomingAccelerometerDataset;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        incomingAccelerometerDataset = new ArrayList<>();
        readIncomingDataSets();
    }

    private void readIncomingDataSets() {
        InputStream inputStream = getResources().openRawResource(R.raw.ACC_09272019);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
        String inputLine="";
        try {
            while ((inputLine = bufferedReader.readLine())!=null){
                //Split the data by ','
                String[] tokens = inputLine.split(",");
                //Read the data
                DataSet dataSet = new DataSet("0",0,0,0);
                dataSet.setTimeStamp(tokens[0]);
                if(tokens.length>=2 && tokens[1].length()>0) dataSet.setxAxis(Double.parseDouble(tokens[1]));
                else dataSet.setxAxis(0);
                if(tokens.length>=3 && tokens[2].length()>0) dataSet.setyAxis(Double.parseDouble(tokens[2]));
                else dataSet.setyAxis(0);
                if(tokens.length>=4 && tokens[3].length()>0) dataSet.setzAxis(Double.parseDouble(tokens[3]));
                else dataSet.setzAxis(0);
                incomingAccelerometerDataset.add(dataSet);
            }
        } catch (IOException e) {
            Log.wtf("MainActivity","Failed to read line:- " + inputLine,e);
            e.printStackTrace();
        }
        SegmentGeneration segmentGeneration = new SegmentGeneration(incomingAccelerometerDataset);
        segmentGeneration.getSegment();
    }
}
