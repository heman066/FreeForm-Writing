package com.freeform.writing;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class SegmentGeneration {

    private final double segment_threshold = 0.0000002;

    private List<DataSet> accelerometerDataSet;
    private List<DataSet> movingAverage;
    private List<DataSet> movingVariance1;
    private List<DataSet> movingVariance2;
    private List<Pair<String,String>> timeStamp;

    public SegmentGeneration(List<DataSet> accelerometerDataSet){
        this.accelerometerDataSet=accelerometerDataSet;
        movingAverage = new ArrayList<>();
        movingVariance1 = new ArrayList<>();
        movingVariance2 = new ArrayList<>();
        timeStamp = new ArrayList<>();
    }

    public void getSegment(){
        for(DataSet dataSet : accelerometerDataSet){
            DataSet data = new DataSet(dataSet.getTimeStamp(),0,0,0);
            movingAverage.add(data);
            movingVariance1.add(data);
            movingVariance2.add(data);
        }

        getmovingAverage();
        getmovingVariance(0);
        getmovingVariance(1);
        thresholdChecking();
        generateSegment();
    }

    private void getmovingAverage(){
        int length = accelerometerDataSet.size();

        for(int i=0;i<length;i++){
            double x=0,y=0,z=0;
            int j=i,size=0;
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
            movingAverage.get(i).setxAxis(x);
            y/=size;
            movingAverage.get(i).setyAxis(y);
            z/=size;
            movingAverage.get(i).setzAxis(z);
        }
    }

    private void getmovingVariance(int check){
        int length;
        if (check==0)length = movingAverage.size();
        else length = movingVariance1.size();

        for(int i=0;i<=length-10;i++){
            double xMean=0,yMean=0,zMean=0;
            int j=i,size=0;
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
                movingVariance1.get(i).setxAxis(xVar);
                movingVariance1.get(i).setyAxis(yVar);
                movingVariance1.get(i).setzAxis(zVar);
            }else{
                movingVariance2.get(i).setxAxis(xVar);
                movingVariance2.get(i).setyAxis(yVar);
                movingVariance2.get(i).setzAxis(zVar);
            }
        }
    }


    private void thresholdChecking() {
        //threshold checking
        List <DataSet> check = new ArrayList<>();
        for(int i=0;i<movingVariance2.size();i++){
            if(movingVariance2.get(i).getxAxis()<=segment_threshold
                    && movingVariance2.get(i).getyAxis()<=segment_threshold
                    && movingVariance2.get(i).getzAxis()<=segment_threshold){

                movingVariance2.get(i).setxAxis(0);
                movingVariance2.get(i).setyAxis(0);
                movingVariance2.get(i).setzAxis(0);
                check.add(movingVariance2.get(i));
            }
        }
        for(DataSet dataSet : check){
            movingVariance2.remove(dataSet);
        }
        check.clear();
    }
    private void generateSegment() {
    }
}
