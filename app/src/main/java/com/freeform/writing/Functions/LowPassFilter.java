package com.freeform.writing.Functions;

import com.freeform.writing.Model.DataSet;
import com.freeform.writing.Model.Segment;

import java.util.ArrayList;
import java.util.List;

public class LowPassFilter {
    private List<DataSet> gyroscopeDataSet;
    private List<Segment> segmentDataSet;
    private List<DataSet> updatedGyroscope;

    public LowPassFilter(List<DataSet> gyroscopeDataSet, List<Segment> segmentDataSet) {
        this.gyroscopeDataSet = gyroscopeDataSet;
        this.segmentDataSet = segmentDataSet;
        updatedGyroscope = new ArrayList<>();
    }

    public List<DataSet> applyLowPassFilter(){
        updatedGyroscope.addAll(gyroscopeDataSet);
        int i=0,lenGyro=gyroscopeDataSet.size(),lenSeg=segmentDataSet.size(),seg=0;
        while(i<lenGyro){
            int j=i,sInd,eInd;
            long startTime,endTime;
            while(segmentDataSet.get(seg).getStartTime() > Long.parseLong(gyroscopeDataSet.get(j).getTimeStamp())) j++;
            startTime = Long.parseLong(gyroscopeDataSet.get(j).getTimeStamp());
            sInd = j;
            while(segmentDataSet.get(seg).getEndTime() >= Long.parseLong(gyroscopeDataSet.get(j).getTimeStamp())) j++;
            endTime = Long.parseLong(gyroscopeDataSet.get(j-1).getTimeStamp());
            eInd = j-1;
            lowPassFilter(startTime,endTime,sInd,eInd);
            seg++;
            if(seg == lenSeg) break;
            i=j;
        }
        return updatedGyroscope;
    }

    private void lowPassFilter(long startTime, long endTime,int sInd,int eInd) {
        double T = (double)( endTime - startTime)/1000.0 ;
        double alpha = T/(T + 0.5);
        for(int i=sInd;i<=eInd;i++){
            double x,y,z;
            if(i-1<0)x=y=z=0;
            else{
                x = updatedGyroscope.get(i-1).getxAxis();
                y = updatedGyroscope.get(i-1).getyAxis();
                z = updatedGyroscope.get(i-1).getzAxis();
            }
            double xAxis = x + alpha * (gyroscopeDataSet.get(i).getxAxis() - x);
            double yAxis = y + alpha * (gyroscopeDataSet.get(i).getyAxis() - y);
            double zAxis = z + alpha * (gyroscopeDataSet.get(i).getzAxis() - z);
            updatedGyroscope.get(i).setxAxis(xAxis);
            updatedGyroscope.get(i).setyAxis(yAxis);
            updatedGyroscope.get(i).setzAxis(zAxis);
        }
    }
}
