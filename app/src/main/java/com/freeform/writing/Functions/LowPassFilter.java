package com.freeform.writing.Functions;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.freeform.writing.Model.DataSet;
import com.freeform.writing.Model.Segment;

import java.util.ArrayList;
import java.util.List;

public class LowPassFilter {
    private List<DataSet> dataSets;
    private List<Segment> segmentDataSet;
    private List<DataSet> updatedDatasets;
    private List<Double> xAxis,yAxis,zAxis;

    public LowPassFilter(List<DataSet> dataSets, List<Segment> segmentDataSet) {
        this.dataSets = dataSets;
        this.segmentDataSet = segmentDataSet;
        updatedDatasets = new ArrayList<>();
        xAxis = new ArrayList<Double>();
        yAxis = new ArrayList<>();
        zAxis = new ArrayList<>();
    }

    public List<DataSet> applyLowPassFilter(){
        int i=0,len=dataSets.size(),lenSeg=segmentDataSet.size(),seg=0;
        while(i<len){
            int j=i,sInd;
            while(segmentDataSet.get(seg).getStartTime() > Long.parseLong(dataSets.get(j).getTimeStamp())){
                updatedDatasets.add(dataSets.get(j));
                j++;
            }
            sInd = j;
            while(segmentDataSet.get(seg).getEndTime() >= Long.parseLong(dataSets.get(j).getTimeStamp())){
                xAxis.add(dataSets.get(j).getxAxis());
                yAxis.add(dataSets.get(j).getyAxis());
                zAxis.add(dataSets.get(j).getzAxis());
                j++;
            }
            int size=xAxis.size();
            Python python = Python.getInstance();
            PyObject pyo = python.getModule("lowpass");

            double[] x = pyo.callAttr("lowpass",xAxis.toArray(),size).toJava(double[].class);
            //short[] xs = new short[x.length / 2];
            //ByteBuffer.wrap(x).order(ByteOrder.nativeOrder()).asShortBuffer().get(xs);

            double[] y = pyo.callAttr("lowpass",yAxis.toArray(),size).toJava(double[].class);
            //short[] ys = new short[y.length / 2];
            //ByteBuffer.wrap(x).order(ByteOrder.nativeOrder()).asShortBuffer().get(ys);

            double[] z = pyo.callAttr("lowpass",zAxis.toArray(),size).toJava(double[].class);
            //short[] zs = new short[z.length / 2];
            //ByteBuffer.wrap(x).order(ByteOrder.nativeOrder()).asShortBuffer().get(zs);

            xAxis.clear();
            yAxis.clear();
            zAxis.clear();

            for(int k=0;k<size;k++){
                DataSet dataSet = new DataSet(dataSets.get(sInd).getTimeStamp(),
                        (double)x[k],
                        (double)y[k],
                        (double)z[k]);
                updatedDatasets.add(dataSet);
                sInd++;
            }
            seg++;
            i=j;
            if(seg == lenSeg){
                for(int k=i;k<len;k++) updatedDatasets.add(dataSets.get(k));
                break;
            }
        }
        return updatedDatasets;
    }
}
