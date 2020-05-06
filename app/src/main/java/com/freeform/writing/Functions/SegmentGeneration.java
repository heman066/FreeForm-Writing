package com.freeform.writing.Functions;

import android.os.Environment;
import android.util.Log;

import com.freeform.writing.Model.DataSet;
import com.freeform.writing.Model.Segment;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SegmentGeneration {


    private List<DataSet> accelerometerDataSet;
    private List<DataSet> gyroscopeDataSet;
    private List<Segment> segmentDataSet;
    private List<DataSet> movingAverage;
    private List<DataSet> movingVariance1;
    private List<DataSet> movingVariance2;
    private List<String> timeStamp;
    private List<Segment> segments;

    public SegmentGeneration(List<DataSet> accelerometerDataSet, List<DataSet> gyroscopeDataSet, List<Segment> segmentDataSet){
        this.accelerometerDataSet=accelerometerDataSet;
        this.gyroscopeDataSet=gyroscopeDataSet;
        this.segmentDataSet=segmentDataSet;
        movingAverage = new ArrayList<>();
        movingVariance1 = new ArrayList<>();
        movingVariance2 = new ArrayList<>();
        timeStamp = new ArrayList<>();
        segments = new ArrayList<>();
    }

    public void getSegment(){
        getmovingAverage();
        getmovingVariance(0);
        getmovingVariance(1);
        thresholdChecking();
        generateSegment();

        LowPassFilter lowPassFilter = new LowPassFilter(movingVariance2,segmentDataSet);
        List<DataSet> updatedAccelerometer = lowPassFilter.applyLowPassFilter();
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
        Log.e("SegmentGeneration","Work Done");
    }

    private void getmovingAverage(){
        int length = accelerometerDataSet.size();

        for(int i=0;i<length;i++){
            double x=0,y=0,z=0,size=0;
            int j=i;
            while(j<i+5 && j<length){
                x+=accelerometerDataSet.get(j).getxAxis();
                y+=accelerometerDataSet.get(j).getyAxis();
                z+=accelerometerDataSet.get(j).getzAxis();
                j++;
                size++;
            }
            j=i-1;
            while(j>=i-5 && j>=0){
                x+=accelerometerDataSet.get(j).getxAxis();
                y+=accelerometerDataSet.get(j).getyAxis();
                z+=accelerometerDataSet.get(j).getzAxis();
                j--;
                size++;
            }
            x/=size;
            y/=size;
            z/=size;
            DataSet dataSet = new DataSet(accelerometerDataSet.get(i).getTimeStamp(),x,y,z);
            movingAverage.add(dataSet);
        }
    }

    private void getmovingVariance(int check){
        int length;
        if (check==0)length = movingAverage.size();
        else length = movingVariance1.size();

        for(int i=0;i<=length-10;i++){
            double xMean=0,yMean=0,zMean=0,size=0;
            int j=i;
            while(j<i+5 && j<length){
                if(check==0){
                    xMean+=movingAverage.get(j).getxAxis();
                    yMean+=movingAverage.get(j).getyAxis();
                    zMean+=movingAverage.get(j).getzAxis();
                }else{
                    xMean+=movingVariance1.get(j).getxAxis();
                    yMean+=movingVariance1.get(j).getyAxis();
                    zMean+=movingVariance1.get(j).getzAxis();
                }
                size++;
                j++;
            }
            j=i-1;
            while(j>=i-5 && j>=0){
                if(check==0){
                    xMean+=movingAverage.get(j).getxAxis();
                    yMean+=movingAverage.get(j).getyAxis();
                    zMean+=movingAverage.get(j).getzAxis();
                }else{
                    xMean+=movingVariance1.get(j).getxAxis();
                    yMean+=movingVariance1.get(j).getyAxis();
                    zMean+=movingVariance1.get(j).getzAxis();
                }
                size++;
                j--;
            }
            xMean/=size;
            yMean/=size;
            zMean/=size;
            double xVar=0,yVar=0,zVar=0;
            j=i;
            while(j<i+5 && j<length){
                if(check==0){
                    xVar+=(movingAverage.get(j).getxAxis()-xMean)*(movingAverage.get(j).getxAxis()-xMean);
                    yVar+=(movingAverage.get(j).getyAxis()-yMean)*(movingAverage.get(j).getyAxis()-yMean);
                    zVar+=(movingAverage.get(j).getzAxis()-zMean)*(movingAverage.get(j).getzAxis()-zMean);
                }else{
                    xVar+=(movingVariance1.get(j).getxAxis()-xMean)*(movingVariance1.get(j).getxAxis()-xMean);
                    yVar+=(movingVariance1.get(j).getyAxis()-yMean)*(movingVariance1.get(j).getyAxis()-yMean);
                    zVar+=(movingVariance1.get(j).getzAxis()-zMean)*(movingVariance1.get(j).getzAxis()-zMean);
                }
                j++;
            }
            j=i-1;
            while(j>=i-5 && j>=0){
                if(check==0){
                    xVar+=(movingAverage.get(j).getxAxis()-xMean)*(movingAverage.get(j).getxAxis()-xMean);
                    yVar+=(movingAverage.get(j).getyAxis()-yMean)*(movingAverage.get(j).getyAxis()-yMean);
                    zVar+=(movingAverage.get(j).getzAxis()-zMean)*(movingAverage.get(j).getzAxis()-zMean);
                }else{
                    xVar+=(movingVariance1.get(j).getxAxis()-xMean)*(movingVariance1.get(j).getxAxis()-xMean);
                    yVar+=(movingVariance1.get(j).getyAxis()-yMean)*(movingVariance1.get(j).getyAxis()-yMean);
                    zVar+=(movingVariance1.get(j).getzAxis()-zMean)*(movingVariance1.get(j).getzAxis()-zMean);
                }
                j--;
            }
            xVar/=size-1;
            yVar/=size-1;
            zVar/=size-1;
            if(check==0){
                DataSet dataSet = new DataSet(movingAverage.get(i).getTimeStamp(),xVar,yVar,zVar);
                movingVariance1.add(dataSet);
            }else{
                DataSet dataSet = new DataSet(movingVariance1.get(i).getTimeStamp(),xVar,yVar,zVar);
                movingVariance2.add(dataSet);
            }
        }
    }


    private void thresholdChecking() {
        //threshold checking
        final double segment_threshold = 0.0000002;
        for(int i=0;i<movingVariance2.size();i++){
            int compareX = Double.compare(movingVariance2.get(i).getxAxis(),segment_threshold);
            int compareY = Double.compare(movingVariance2.get(i).getyAxis(),segment_threshold);
            int compareZ = Double.compare(movingVariance2.get(i).getzAxis(),segment_threshold);
            if(compareX<0 && compareY<0 && compareZ<0){

                movingVariance2.get(i).setxAxis(0);
                movingVariance2.get(i).setyAxis(0);
                movingVariance2.get(i).setzAxis(0);
            }else{
                timeStamp.add(movingVariance2.get(i).getTimeStamp());
            }
        }
    }
    private void generateSegment() {
        try {
            File file = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/test/segmentGenerated.csv");
            if(file.exists()) FileUtils.forceDelete(file);
            FileWriter fileWriter = new FileWriter(Environment.getExternalStoragePublicDirectory(
                    "FreeForm-Writing/test/segmentGenerated.csv"),true);
            long startTime,endTime,checkTime;
            int i=0,len=timeStamp.size();
            while(i<len){
                startTime = Long.parseLong(timeStamp.get(i));
                int j=i+1,count=0;
                checkTime=startTime;
                endTime = Long.parseLong(timeStamp.get(j));
                while((endTime-checkTime)<1000){
                    j++;
                    count++;
                    checkTime=endTime;
                    if(j==len)break;
                    endTime=Long.parseLong(timeStamp.get(j));
                }
                endTime=checkTime;
                if(endTime-startTime>=1000){
                    Segment segment = new Segment(startTime,endTime);
                    segments.add(segment);
                    String seg= segment.getStartTime() + "," + segment.getEndTime();
                    fileWriter.write(seg + "\n");
                    fileWriter.flush();
                }
                i=j;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}