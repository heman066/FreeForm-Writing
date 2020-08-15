package com.freeform.writing.Functions;

import android.os.Environment;
import android.util.Log;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.freeform.writing.Logger;
import com.freeform.writing.Model.DataSet;
import com.freeform.writing.Model.Segment;
import com.freeform.writing.Model.SegmentWithPenUpDown;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class PenUpDownClustering{
    private List<DataSet> rawGyroscopeDataSet;
    private List<Segment> segments;
    private List<SegmentWithPenUpDown> updatedSegments;
    private Map<Long,DataWithIndex> maxX, minX, maxY, minY, maxZ, minZ;
    private HashMap<DataWithTimeStamp,Double> axisInputData;
    private long timeStamp;
    private String inputDate;
    private File working;
    private List<Segment> timeSegmentList;
    private FileWriter fileW;
    private Logger logger;

    public PenUpDownClustering(List<DataSet> rawGyroscopeDataSet, List<Segment> segments, String inputDate, Logger logger) {
        this.rawGyroscopeDataSet = rawGyroscopeDataSet;
        this.segments = segments;
        this.inputDate = inputDate;
        this.logger = logger;
        working = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/." + inputDate + "/.Working");
        if (!working.exists())
            working.mkdir();
        maxX = new LinkedHashMap<>();
        maxY = new LinkedHashMap<>();
        maxZ = new LinkedHashMap<>();
        minX = new LinkedHashMap<>();
        minY = new LinkedHashMap<>();
        minZ = new LinkedHashMap<>();
        axisInputData = new HashMap<>();
        timeSegmentList = new ArrayList<>();
        updatedSegments = new ArrayList<>();
        try {
            fileW = new FileWriter(Environment.getExternalStoragePublicDirectory("FreeForm-Writing/." + inputDate + "/segments.csv"),true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<SegmentWithPenUpDown> run() {
        getMinMax();
        /*try {
            FileWriter fileWriter = new FileWriter(Environment.getExternalStoragePublicDirectory("FreeForm-Writing/." + inputDate + "/updatedseg.csv"),true);
            for (SegmentWithPenUpDown segment : updatedSegments){
                //Log.e("PenUpDown","just checking");
                String msg = segment.getStartTime() + " , " + segment.getEndTime() + " , " + segment.getPenUpDownSegment().get(0).getStartTime() + " , " + segment.getPenUpDownSegment().get(0).getEndTime();
                fileWriter.write(msg + "\n");
                fileWriter.flush();
            }
            Log.e("PenUpDown",updatedSegments.size() + "");
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        //Log.e("PenUpDown",updatedSegments.size() + "");
        return updatedSegments;
    }

    private void getMinMax(){
        //logger.write("","check 1 getMinMax " + updatedSegments.size());
        int i = 0, size = rawGyroscopeDataSet.size();
        int seg = 0, segLen = segments.size();
        long startTime, endTime;
        while(i<size){
            int j = i, sInd, eInd;
            startTime = segments.get(seg).getStartTime();
            endTime = segments.get(seg).getEndTime();
            while(startTime > Long.parseLong(rawGyroscopeDataSet.get(j).getTimeStamp())){
                j++;
                if (j == size)
                    break;
            }
            if (j == size)
                break;
            sInd = j;
            boolean isSuccessful = false;
            while (endTime >= Long.parseLong(rawGyroscopeDataSet.get(j).getTimeStamp())){
                double x = rawGyroscopeDataSet.get(j).getxAxis();
                double y = rawGyroscopeDataSet.get(j).getyAxis();
                double z = rawGyroscopeDataSet.get(j).getzAxis();
                DataWithIndex dx = new DataWithIndex(x,j);
                DataWithIndex dy = new DataWithIndex(y,j);
                DataWithIndex dz = new DataWithIndex(z,j);
                long timeStamp = Long.parseLong(rawGyroscopeDataSet.get(j).getTimeStamp());
                if (j != 0 && j != size-1){
                    if(x > rawGyroscopeDataSet.get(j-1).getxAxis() && x > rawGyroscopeDataSet.get(j+1).getxAxis())
                        maxX.put(timeStamp,dx);
                    else if(x < rawGyroscopeDataSet.get(j-1).getxAxis() && x < rawGyroscopeDataSet.get(j+1).getxAxis())
                        minX.put(timeStamp,dx);
                    if(y > rawGyroscopeDataSet.get(j-1).getyAxis() && y > rawGyroscopeDataSet.get(j+1).getyAxis())
                        maxY.put(timeStamp,dy);
                    else if(y < rawGyroscopeDataSet.get(j-1).getyAxis() && y < rawGyroscopeDataSet.get(j+1).getyAxis())
                        minY.put(timeStamp,dy);
                    if(z > rawGyroscopeDataSet.get(j-1).getzAxis() && z > rawGyroscopeDataSet.get(j+1).getzAxis())
                        maxZ.put(timeStamp,dz);
                    else if(z < rawGyroscopeDataSet.get(j-1).getzAxis() && z < rawGyroscopeDataSet.get(j+1).getzAxis())
                        minZ.put(timeStamp,dz);
                }
                j++;
                if (j == size){
                    break;
                }
            }
            if (j == size)
                break;
            eInd = j-1;
            calculateAxis_CrossAxis_MinMax_Difference(sInd,eInd);
            List<Double> clusteredOutput = applyKmeansClustering();
            getPenUpDownTimeSegments(clusteredOutput,seg);
            seg++;
            i = j;
            if (seg == segLen)
                break;
        }
    }

    private void getPenUpDownTimeSegments(List<Double> clusteredOutput, int segmentIndex) {
        long sTime = segments.get(segmentIndex).getStartTime();
        long eTime = segments.get(segmentIndex).getEndTime();
        //Log.e("PenUpDown", "getPenUpDownTimeSegments: clustered output size:-" + clusteredOutput.size());
        for (double inst : clusteredOutput){
            for (Map.Entry<DataWithTimeStamp,Double> entry : axisInputData.entrySet()){
                long startTime = entry.getKey().startTime;
                long endTime = entry.getKey().endTime;
                double data = entry.getValue();
                if(inst == data)
                    timeSegmentList.add(new Segment(startTime,endTime));
            }
        }
        clusteredOutput.clear();
        axisInputData.clear();

        List<Segment> buffsegments = new ArrayList<>();
        long time = 0;

        if(timeSegmentList.size() == 0 || timeSegmentList.size() == 1){
            buffsegments = timeSegmentList;
            if(timeSegmentList.size() == 1)
                time += timeSegmentList.get(0).getEndTime() - timeSegmentList.get(0).getStartTime();
        }
        else {
            Collections.sort(timeSegmentList, new IntervalComparator());

            Segment first = timeSegmentList.get(0);
            long start = first.getStartTime();
            long end = first.getEndTime();

            for (int i = 1; i < timeSegmentList.size(); i++) {
                Segment current = timeSegmentList.get(i);
                if (current.getStartTime() <= end) {
                    end = Math.max(current.getEndTime(), end);
                } else {
                    buffsegments.add(new Segment(start, end));
                    time += end - start;
                    start = current.getStartTime();
                    end = current.getEndTime();
                }
            }
            buffsegments.add(new Segment(start, end));
            time += end - start;
        }

        timeSegmentList.clear();
        if (time <= 500){
            SegmentWithPenUpDown segment = new SegmentWithPenUpDown(sTime,eTime,buffsegments);
            updatedSegments.add(segment);
            try {
                String msg = sTime + " , " + eTime + " , " + buffsegments.size() + " , " + time + " outSeg";
                fileW.write(msg + "\n");
                fileW.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else{
            try {
                String msg = sTime + " , " + eTime + " , " + buffsegments.size() + " , " + time;
                fileW.write(msg + "\n");
                fileW.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private List<Double> applyKmeansClustering() {
        List<Double> output = new ArrayList<>();
        try {
            Python python = Python.getInstance();
            PyObject pyo = python.getModule("Clustering");
            pyo.callAttr("Clustering",inputDate);

            InputStream inputStream = new FileInputStream(Environment.getExternalStoragePublicDirectory("FreeForm-Writing/." + inputDate + "/.Working/outputData.csv"));
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
            String inputLine="";
            while ((inputLine = bufferedReader.readLine())!=null){
                //Split the data by ','
                //String[] tokens = inputLine.split(",");
                output.add(Double.parseDouble(inputLine));
            }
            //Log.e("PenUpDown", "applyKmeansClustering: outputSize:-" + output.size());
            File inputData = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/." + inputDate + "/.Working/inputData.csv");
            File outputData = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/." + inputDate + "/.Working/outputData.csv");
            FileUtils.forceDelete(inputData);
            FileUtils.forceDelete(outputData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }

    private void calculateAxis_CrossAxis_MinMax_Difference(int sInd, int eInd) {
        try {
            FileWriter fileWriter = new FileWriter(working.getAbsolutePath() + "/inputData.csv");
            //fileWriter.write("11.0" + "\n");
            //fileWriter.flush();
            //for x axis
            sameAxisDiff(maxX,minX,fileWriter);
            for(Map.Entry<Long, DataWithIndex> entry : maxX.entrySet() ){
                long key = entry.getKey(), ansTimeStampL, ansTimeStampR;
                int i = entry.getValue().getIndex();
                double maxData = entry.getValue().getAxisData(), ansL=0, ansR=0;
                timeStamp = key;

                //min on yAxis left
                ansL = getDiffLeft(minY,key,i,maxData,sInd);
                ansTimeStampL = timeStamp;
                //min on yAxis right
                ansR = getDiffRight(minY,key,i,maxData,eInd);
                ansTimeStampR = timeStamp;
                addToClusterInputSet(ansL,ansR,ansTimeStampL,ansTimeStampR,key,fileWriter);

                //min on zAxis left
                ansL = getDiffLeft(minZ,key,i,maxData,sInd);
                ansTimeStampL = timeStamp;
                //min on zAxis right
                ansR = getDiffRight(minZ,key,i,maxData,eInd);
                ansTimeStampR = timeStamp;
                addToClusterInputSet(ansL,ansR,ansTimeStampL,ansTimeStampR,key,fileWriter);
            }

            //for yAxis
            sameAxisDiff(maxY,minY,fileWriter);
            for(Map.Entry<Long, DataWithIndex> entry : maxY.entrySet() ){
                long key = entry.getKey(), ansTimeStampL, ansTimeStampR;
                int i = entry.getValue().getIndex();
                double maxData = entry.getValue().getAxisData(), ansL=0, ansR=0;
                timeStamp = key;

                //min on xAxis left
                ansL = getDiffLeft(minX,key,i,maxData,sInd);
                ansTimeStampL = timeStamp;
                //min on xAxis right
                ansR = getDiffRight(minX,key,i,maxData,eInd);
                ansTimeStampR = timeStamp;
                addToClusterInputSet(ansL,ansR,ansTimeStampL,ansTimeStampR,key,fileWriter);

                //min on zAxis left
                ansL = getDiffLeft(minZ,key,i,maxData,sInd);
                ansTimeStampL = timeStamp;
                //min on zAxis right
                ansR = getDiffRight(minZ,key,i,maxData,eInd);
                ansTimeStampR = timeStamp;
                addToClusterInputSet(ansL,ansR,ansTimeStampL,ansTimeStampR,key,fileWriter);
            }

            //for zAxis
            sameAxisDiff(maxZ,minZ,fileWriter);
            for(Map.Entry<Long, DataWithIndex> entry : maxZ.entrySet() ){
                long key = entry.getKey(), ansTimeStampL, ansTimeStampR;
                int i = entry.getValue().getIndex();
                double maxData = entry.getValue().getAxisData(), ansL=0, ansR=0;
                timeStamp = key;

                //min on xAxis left
                ansL = getDiffLeft(minX,key,i,maxData,eInd);
                ansTimeStampL = timeStamp;
                //min on xAxis right
                ansR = getDiffRight(minX,key,i,maxData,sInd);
                ansTimeStampR = timeStamp;
                addToClusterInputSet(ansL,ansR,ansTimeStampL,ansTimeStampR,key,fileWriter);

                //min on yAxis left
                ansL = getDiffLeft(minY,key,i,maxData,sInd);
                ansTimeStampL = timeStamp;
                //min on yAxis right
                ansR = getDiffRight(minY,key,i,maxData,eInd);
                ansTimeStampR = timeStamp;
                addToClusterInputSet(ansL,ansR,ansTimeStampL,ansTimeStampR,key,fileWriter);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        minX.clear();
        minY.clear();
        minZ.clear();
        maxX.clear();
        maxY.clear();
        maxZ.clear();
    }

    private void sameAxisDiff(Map<Long, DataWithIndex> max, Map<Long, DataWithIndex> min, FileWriter fileWriter) throws IOException {
        Iterator< Map.Entry< Long, DataWithIndex> > itrMax = max.entrySet().iterator();
        Iterator< Map.Entry< Long, DataWithIndex> > itrMin = min.entrySet().iterator();
        while (itrMax.hasNext() && itrMin.hasNext()){
            Map.Entry<Long, DataWithIndex> entryMax = itrMax.next();
            Map.Entry<Long, DataWithIndex> entryMin = itrMin.next();
            long maxTime = entryMax.getKey(), minTime =entryMin.getKey();
            DataWithIndex dMax = entryMax.getValue();
            DataWithIndex dMin = entryMin.getValue();
            double ans = Math.abs(dMax.getAxisData() - dMin.getAxisData());
            if (maxTime > minTime){
                DataWithTimeStamp data = new DataWithTimeStamp(ans,minTime,maxTime);
                //fileWriter.write( minTime + "," + ans + "\n");
                fileWriter.write( "" + ans + "\n");
                fileWriter.flush();
                axisInputData.put(data,ans);
            } else {
                DataWithTimeStamp data = new DataWithTimeStamp(ans,maxTime,minTime);
                //fileWriter.write( maxTime + "," + ans + "\n");
                fileWriter.write( "" + ans + "\n");
                fileWriter.flush();
                axisInputData.put(data,ans);
            }
        }
        //if only maxima is left
        List<Long> keyListMin = new ArrayList<>(min.keySet());
        long keyMin = keyListMin.get(keyListMin.size() - 1);
        while (itrMax.hasNext()){
            Map.Entry<Long, DataWithIndex> entryMax = itrMax.next();
            double ans = entryMax.getValue().getAxisData() - min.get(keyMin).getAxisData();
            long maxTime = entryMax.getKey();
            if(maxTime > keyMin){
                DataWithTimeStamp data = new DataWithTimeStamp(ans,keyMin,maxTime);
                //fileWriter.write( keyMin + "," + ans + "\n");
                fileWriter.write( "" + ans + "\n");
                fileWriter.flush();
                axisInputData.put(data,ans);
            } else {
                DataWithTimeStamp data = new DataWithTimeStamp(ans,maxTime,keyMin);
                //fileWriter.write( maxTime + "," + ans + "\n");
                fileWriter.write( "" + ans + "\n");
                fileWriter.flush();
                axisInputData.put(data,ans);
            }
        }

        //if only minima is left
        List<Long> keyListMax = new ArrayList<>(max.keySet());
        long keyMax = keyListMax.get(keyListMax.size() - 1);
        while (itrMin.hasNext()){
            Map.Entry<Long, DataWithIndex> entryMin = itrMin.next();
            double ans = max.get(keyMax).getAxisData() - entryMin.getValue().getAxisData();
            long minTime = entryMin.getKey();
            if(minTime > keyMax){
                DataWithTimeStamp data = new DataWithTimeStamp(ans,keyMax,minTime);
                //fileWriter.write( keyMax + "," + ans + "\n");
                fileWriter.write( "" + ans + "\n");
                fileWriter.flush();
                axisInputData.put(data,ans);
            } else {
                DataWithTimeStamp data = new DataWithTimeStamp(ans,minTime,keyMax);
                //fileWriter.write( minTime + "," + ans + "\n");
                fileWriter.write( "" + ans + "\n");
                fileWriter.flush();
                axisInputData.put(data,ans);
            }
        }
    }

    private double getDiffLeft(Map<Long,DataWithIndex> min, long keyTime, int index, double maxData, int sInd){
        double minData;
        while (!min.containsKey(keyTime)){
            index--;
            if(index < sInd)
                break;
            keyTime = Long.parseLong(rawGyroscopeDataSet.get(index).getTimeStamp());
        }
        if(index >= sInd){
            minData = min.get(keyTime).getAxisData();
            timeStamp = keyTime;
            return maxData - minData;
        }
        return 0;
    }

    private double getDiffRight(Map<Long,DataWithIndex> min, long keyTime, int index, double maxData, int eInd){
        int size = rawGyroscopeDataSet.size();
        double minData;
        while (!min.containsKey(keyTime)){
            index++;
            if (index == eInd || index == size)
                break;
            keyTime = Long.parseLong(rawGyroscopeDataSet.get(index).getTimeStamp());
        }
        if(index != eInd && index !=size){
            minData = min.get(keyTime).getAxisData();
            timeStamp = keyTime;
            return maxData - minData;
        }
        return 0;
    }

    private void addToClusterInputSet(double ansL, double ansR, long ansTimeStampL, long ansTimeStampR, long key, FileWriter fileWriter) throws IOException {
        if (ansL != 0 && ansR != 0){
            if(ansL > ansR){
                DataWithTimeStamp dy = new DataWithTimeStamp(ansL,ansTimeStampL,key);
                axisInputData.put(dy,ansL);
                //fileWriter.write(   ansTimeStampL + "," + ansL + "\n");
                fileWriter.write(   "" + ansL + "\n");
                fileWriter.flush();
            } else{
                DataWithTimeStamp dy = new DataWithTimeStamp(ansR,key,ansTimeStampR);
                axisInputData.put(dy,ansR);
                //fileWriter.write(   key + "," + ansR + "\n");
                fileWriter.write(   "" + ansR + "\n");
                fileWriter.flush();
            }
        } else if (ansL != 0){
            DataWithTimeStamp dy = new DataWithTimeStamp(ansL,ansTimeStampL,key);
            axisInputData.put(dy,ansL);
            //fileWriter.write(   ansTimeStampL + "," + ansL + "\n");
            fileWriter.write(   "" + ansL + "\n");
            fileWriter.flush();
        } else if (ansR != 0){
            DataWithTimeStamp dy = new DataWithTimeStamp(ansR,key,ansTimeStampR);
            axisInputData.put(dy,ansR);
            //fileWriter.write(   key + "," + ansR + "\n");
            fileWriter.write(   "" + ansR + "\n");
            fileWriter.flush();
        }
    }

    class DataWithTimeStamp{
        double data;
        long startTime, endTime;

        public DataWithTimeStamp(double data, long startTime, long endTime) {
            this.data = data;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        private double getData() {
            return data;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }
    }

    class DataWithIndex{
        double axisData;
        int index;

        private DataWithIndex( double axisData, int index) {
            this.axisData = axisData;
            this.index = index;
        }

        private double getAxisData() {
            return axisData;
        }

        private int getIndex() {
            return index;
        }
    }

    class IntervalComparator implements Comparator<Segment>
    {

        @Override
        public int compare(Segment o1, Segment o2) {
            return 0;
        }
    }

}
