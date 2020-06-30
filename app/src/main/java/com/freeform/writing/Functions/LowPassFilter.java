package com.freeform.writing.Functions;

import android.os.Environment;

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
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LowPassFilter {
    private List<DataSet> dataSets;
    private List<SegmentWithPenUpDown> segmentDataSet;
    private List<DataSet> updatedDatasets;
    private List<Double> xAxis,yAxis,zAxis;
    private List<Long> startTimes, endTimes, timeStamps;
    private Logger logger;
    private String inputDate;
    private String filePath, saveDir;

    public LowPassFilter(List<DataSet> dataSets, List<SegmentWithPenUpDown> segmentDataSet, Logger logger, String inputDate, String filePath, String saveDir) {
        this.dataSets = dataSets;
        this.segmentDataSet = segmentDataSet;
        this.logger = logger;
        this.inputDate = inputDate;
        this.filePath = filePath;
        this.saveDir = saveDir;
        updatedDatasets = new ArrayList<>();
        xAxis = new ArrayList<>();
        yAxis = new ArrayList<>();
        zAxis = new ArrayList<>();
        startTimes = new ArrayList<>();
        endTimes = new ArrayList<>();
        timeStamps = new ArrayList<>();
    }

    /*public List<DataSet> applyLowPassFilter(){
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
    }*/

    public List<DataSet> applyLowPassFilter(){
        /*for(DataSet dataSet : dataSets){
            xAxis.add(dataSet.getxAxis());
            yAxis.add(dataSet.getyAxis());
            zAxis.add(dataSet.getzAxis());
            timeStamps.add(Long.valueOf(dataSet.getTimeStamp()));
        }*/
        for(SegmentWithPenUpDown segment : segmentDataSet){
            startTimes.add(segment.getStartTime());
            endTimes.add(segment.getEndTime());
        }

        Python python = Python.getInstance();
        PyObject pyo = python.getModule("LPass");

        int size = dataSets.size(),segLen = segmentDataSet.size();

        /*double[] x = pyo.callAttr("LPass",xAxis.toArray(),timeStamps.toArray(),
                startTimes.toArray(),endTimes.toArray(),size,segLen).toJava(double[].class);

        double[] y = pyo.callAttr("LPass",yAxis.toArray(),timeStamps.toArray(),
                startTimes.toArray(),endTimes.toArray(),size,segLen).toJava(double[].class);

        double[] z = pyo.callAttr("LPass",zAxis.toArray(),timeStamps.toArray(),
                startTimes.toArray(),endTimes.toArray(),size,segLen).toJava(double[].class);*/

        pyo.callAttr("LPass",startTimes.toArray(),endTimes.toArray(),filePath,saveDir);

        /*for(int i=0;i<dataSets.size();i++){
            DataSet dataSet = new DataSet(dataSets.get(i).getTimeStamp(),
                    (double) x[i],
                    (double)y[i],
                    (double)z[i]);
            updatedDatasets.add(dataSet);
        }*/
        //updatedDatasets.add(new DataSet("z",2,2,2));
        getLowPassOutputFile();
        return updatedDatasets;
    }

    private void getLowPassOutputFile() {
        try {
            File file = new File(saveDir);
            InputStream inputStream = new FileInputStream(file);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
            String inputLine="";
            FileUtils.forceDelete(file);
            //FileWriter fileWriter = new FileWriter(saveDir,true);
            while ((inputLine = bufferedReader.readLine())!=null){
                //Split the data by ','
                String[] tokens = inputLine.split(",");
                //Read the data
                DataSet dataSet = new DataSet("0",0,0,0);
                BigDecimal bigDecimal = new BigDecimal(tokens[0]);
                long timeStamp = bigDecimal.longValue();
                dataSet.setTimeStamp(String.valueOf(timeStamp));
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
                updatedDatasets.add(dataSet);

                String msg = dataSet.getTimeStamp()+","+dataSet.getxAxis()+","+dataSet.getyAxis()+","+dataSet.getzAxis();
                //fileWriter.write(msg + "\n");
                //fileWriter.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
