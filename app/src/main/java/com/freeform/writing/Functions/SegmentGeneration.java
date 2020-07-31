package com.freeform.writing.Functions;

import android.os.Environment;

import com.freeform.writing.Logger;
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
    private List<DataSet> movingAverage;
    private List<DataSet> movingVariance1;
    private List<DataSet> movingVariance2;
    private List<String> timeStamp;
    private List<Segment> segments;
    private Logger logger;
    private String segTag;
    private String inputDate;

    public SegmentGeneration(List<DataSet> accelerometerDataSet, Logger logger, String inputDate){
        this.accelerometerDataSet=accelerometerDataSet;
        this.logger = logger;
        this.inputDate = inputDate;
        movingAverage = new ArrayList<>();
        movingVariance1 = new ArrayList<>();
        movingVariance2 = new ArrayList<>();
        timeStamp = new ArrayList<>();
        segments = new ArrayList<>();
        segTag = "Segment Generation";
        logger.write(segTag,"Segment Generation module has started");
        File inDate = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/." + inputDate);
        if(!inDate.exists())
            inDate.mkdir();
        else{
            try {
                FileUtils.forceDelete(inDate);
                inDate.mkdir();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void run(){
        getmovingAverage();
        getmovingVariance(0);
        getmovingVariance(1);
        thresholdChecking();
    }

    public void getmovingAverage(){
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

    public void getmovingVariance(int check){
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


    /*public void thresholdChecking() {
        movingAverage.clear();
        movingVariance1.clear();
        //threshold checking
        final double segment_threshold = 0.0000002;
        int seg=0,segLen=groundTruthSegment.size(),i=0,len=movingVariance2.size();
        while(i<len){
            int j=i;
            long startTime=groundTruthSegment.get(seg).getStartTime(),endTime=groundTruthSegment.get(seg).getEndTime();
            while(startTime>Long.parseLong(movingVariance2.get(j).getTimeStamp())) j++;
            while(startTime<=Long.parseLong(movingVariance2.get(j).getTimeStamp())
                    && endTime>=Long.parseLong(movingVariance2.get(j).getTimeStamp())){

                int compareX = Double.compare(movingVariance2.get(j).getxAxis(),segment_threshold);
                int compareY = Double.compare(movingVariance2.get(j).getyAxis(),segment_threshold);
                int compareZ = Double.compare(movingVariance2.get(j).getzAxis(),segment_threshold);
                if(compareX<0 && compareY<0 && compareZ<0){

                    movingVariance2.get(j).setxAxis(0);
                    movingVariance2.get(j).setyAxis(0);
                    movingVariance2.get(j).setzAxis(0);
                }else{
                    timeStamp.add(movingVariance2.get(j).getTimeStamp());
                }
                j++;
            }
            i=j;
            seg++;
            if(seg==segLen) break;
        }
    }*/

    public void thresholdChecking(){
        movingAverage.clear();
        movingVariance1.clear();

        final double segment_threshold = 0.0000002;

        for(DataSet dataSet : movingVariance2){
            int compareX = Double.compare(dataSet.getxAxis(),segment_threshold);
            int compareY = Double.compare(dataSet.getyAxis(),segment_threshold);
            int compareZ = Double.compare(dataSet.getzAxis(),segment_threshold);
            if(compareX<0 && compareY<0 && compareZ<0)
                continue;
            else
                timeStamp.add(dataSet.getTimeStamp());
        }
    }

    public List<Segment> generateSegment() {
        try {
            File file = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/test/segments" + inputDate +".csv");
            if(file.exists()) FileUtils.forceDelete(file);
            FileWriter fileWriter = new FileWriter(Environment.getExternalStoragePublicDirectory(
                    "FreeForm-Writing/test/segments" + inputDate + ".csv"),true);
            long startTime,endTime,checkTime;
            int i=0,len=timeStamp.size();
            while(i<len){
                startTime = Long.parseLong(timeStamp.get(i));
                int j=i,count=0;
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
                    //if(groundTruthSegment.get(segG).getEndTime()>endTime && groundTruthSegment.get(segG).getStartTime()<startTime){
                        Segment segment = new Segment(startTime,endTime);
                        segments.add(segment);
                        String seg= segment.getStartTime() + "," + segment.getEndTime();
                        fileWriter.write(seg + "\n");
                        fileWriter.flush();
                        //segG++;
                        //if(segG==seglen) break;
                    //}
                }
                i=j;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        movingVariance2.clear();
        timeStamp.clear();
        return segments;
    }
}