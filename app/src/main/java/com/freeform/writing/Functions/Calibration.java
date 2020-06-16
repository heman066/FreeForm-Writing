package com.freeform.writing.Functions;

import android.os.Environment;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.freeform.writing.Logger;
import com.freeform.writing.Model.DataSet;
import com.freeform.writing.Model.Segment;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Calibration {

    private List<DataSet> accelerometerDataset;
    private List<DataSet> gyroscopeDataset;
    private List<DataSet> calibratedDataset;
    private List<Segment> segments;
    private List<Double> xAxis,yAxis,zAxis;
    private String inputDate;
    private File working;
    private Logger logger;
    private String calTAG;
    private long loadStart, loadEnd;

    public Calibration(List<DataSet> accelerometerDataset, List<DataSet> gyroscopeDataset, List<Segment> segments, String inputDate, Logger logger) {
        this.accelerometerDataset = accelerometerDataset;
        this.gyroscopeDataset = gyroscopeDataset;
        this.calibratedDataset = new ArrayList<>();
        this.segments = segments;
        this.inputDate = inputDate;
        this.logger = logger;
        this.xAxis = new ArrayList<>();
        this.yAxis = new ArrayList<>();
        this.zAxis = new ArrayList<>();
        calTAG = "Calibration";
        logger.write(calTAG,"Calibration module started");
        loadStart = System.currentTimeMillis();
        working = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/." + inputDate);
        if(!working.exists()) working.mkdir();
    }

    public void analyze(){
        int aLen = accelerometerDataset.size(),gLen = gyroscopeDataset.size(),i=0,j=0,seg=0,segLen=segments.size();
        double xr=0,yr=0,zr=0;
        long startTime=0,endTime=0;
        String name="";
        while(i<aLen && j<gLen){
            if(seg<segLen){
                startTime=segments.get(seg).getStartTime();
                endTime=segments.get(seg).getEndTime();
            }
            double ax = accelerometerDataset.get(i).getxAxis();
            double ay = accelerometerDataset.get(i).getyAxis();
            double az = accelerometerDataset.get(i).getzAxis();
            double gx = gyroscopeDataset.get(j).getxAxis();
            double gy = gyroscopeDataset.get(j).getyAxis();
            double gz = gyroscopeDataset.get(j).getzAxis();
            double x = Math.atan2(ay,ax);
            double y = Math.atan2((-ax),Math.sqrt((ay*ay) + (az*az)));
            double x1 = gx + (gy*(Math.sin(x)) + gz*(Math.cos(x)))*(Math.tan(y));
            double y1 = gy*(Math.cos(x)) - gz*(Math.sin(x));
            double z1 = (gy*(Math.sin(x)) + gz*(Math.cos(x)))/Math.cos(y);
            String timeStamp = accelerometerDataset.get(i).getTimeStamp();
            DataSet dataSet = new DataSet(timeStamp,x1,y1,z1);
            if(startTime<=Long.parseLong(timeStamp) && endTime>=Long.parseLong(timeStamp) && seg<segLen){
                if(startTime == Long.parseLong(timeStamp)){
                    xAxis.clear();
                    yAxis.clear();
                    zAxis.clear();
                    name="IMG_" + timeStamp + "_To_";
                    xr=yr=zr=0;
                }
                xr+=x1*0.02;
                yr+=y1*0.02;
                zr+=z1*0.02;
                xAxis.add(xr);
                yAxis.add(yr);
                zAxis.add(zr);
                if(endTime == Long.parseLong(timeStamp)){
                    seg++;
                    name+=timeStamp + ".png";
                    Python python = Python.getInstance();
                    PyObject pyo = python.getModule("graph");
                    pyo.callAttr("graph",xAxis.toArray(),yAxis.toArray(),zAxis.toArray(),xAxis.size(),name,working.getName());
                }
            }
            calibratedDataset.add(dataSet);
            String msg = dataSet.getTimeStamp()+","+dataSet.getxAxis()+","+dataSet.getyAxis()+","+dataSet.getzAxis();
            i++;
            j++;
        }
        loadEnd = System.currentTimeMillis();
        double time =((double)(loadEnd - loadStart))/1000.0;
        logger.write(calTAG,"Calibration completed and output image generated, elapsed time: " + time + " seconds");
    }
}
