package com.freeform.writing;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.freeform.writing.Functions.Calibration;
import com.freeform.writing.Functions.LowPassFilter;
import com.freeform.writing.Functions.PenUpDownClustering;
import com.freeform.writing.Functions.SegmentGeneration;
import com.freeform.writing.Model.DataSet;
import com.freeform.writing.Model.Segment;
import com.freeform.writing.Model.SegmentWithPenUpDown;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

public class MyWorker extends Worker {

    private List<DataSet> incomingAccelerometerDataset;
    private List<DataSet> incomingGyroscopeDataset;
    private List<DataSet> updatedAccelerometer;
    private List<DataSet> updatedGyroscope;
    private List<SegmentWithPenUpDown> segments;

    private File rawAcc, rawGyro;

    private String TAG = "MyWorker";

    private BasicFunctionHandler handler;
    private Logger logger;
    private int NOTIFICATION_ID;
    private String CHANNEL_ID = "This is channel id";
    private String date;
    private long moduleStart;
    private long moduleEnd;

    public MyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        if(!Python.isStarted()){
            Python.start(new AndroidPlatform(getApplicationContext()));
        }
        init();
        handler.displayProgressBarNotification(date,"processing data",NOTIFICATION_ID);
        logger.write(TAG,"Process started for date:- " + date);
        fetchData();
        equalize();
        generateSegment();
        if (segments.size() == 0){
            handler.displayNotification(date,"No Segments to Generate",NOTIFICATION_ID);
            logger.write("","No Segments to Generate");
            File file = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/." + date);
            try {
                FileUtils.forceDelete(file);
                FileUtils.forceDelete(rawAcc);
                FileUtils.forceDelete(rawGyro);
            } catch (IOException e) {
                e.printStackTrace();
            }
            stopWorker();
        } else {
            applyLowPass();
            applyCalibration();
            getPdfFromImage();
            end();
        }
        return Result.success();
    }

    private void init() {
        incomingAccelerometerDataset = new ArrayList<>();
        incomingGyroscopeDataset = new ArrayList<>();
        updatedAccelerometer = new ArrayList<>();
        updatedGyroscope = new ArrayList<>();
        segments = new ArrayList<>();

        Data data = getInputData();
        rawAcc = new File(data.getString("accFilePath"));
        rawGyro = new File(data.getString("gyroFilePath"));

        handler = new BasicFunctionHandler(getApplicationContext());
        logger = new Logger();
        NOTIFICATION_ID = Integer.parseInt(getDateFromName(rawAcc.getName())) + getRandomNumber();
        date = getDateFromName(rawAcc.getName());
        moduleStart = System.currentTimeMillis();
    }

    private void fetchData() {
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    InputStream inputStream = new FileInputStream(rawAcc);
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
                    String inputLine="";
                    File file1 = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/.FFWList/" + rawAcc.getName());
                    if(file1.exists()) FileUtils.forceDelete(file1);
                    FileWriter fileWriter = new FileWriter(Environment.getExternalStoragePublicDirectory(
                            "FreeForm-Writing/.FFWList/" + rawAcc.getName()),true);
                    boolean isCorrupted = false;
                    while ((inputLine = bufferedReader.readLine())!=null){
                        //Split the data by ','
                        String[] tokens = inputLine.split(",");
                        //if (tokens[0].equals("dummy"))
                        //    break;
                        //Read the data
                        if (!isLong(tokens[0])){
                            isCorrupted = true;
                            continue;
                        }
                        DataSet dataSet = new DataSet("0",0,0,0);
                        dataSet.setTimeStamp(tokens[0]);
                        if(tokens.length>=2 && tokens[1].length()>0 && isDouble(tokens[1])){
                            dataSet.setxAxis(Double.parseDouble(tokens[1]));
                        }
                        else {
                            isCorrupted = true;
                            continue;
                        }
                        if(tokens.length>=3 && tokens[2].length()>0 && isDouble(tokens[2])){
                            dataSet.setyAxis(Double.parseDouble(tokens[2]));
                        }
                        else {
                            isCorrupted = true;
                            continue;
                        }
                        if(tokens.length>=4 && tokens[3].length()>0 && isDouble(tokens[3])){
                            dataSet.setzAxis(Double.parseDouble(tokens[3]));
                        }
                        else {
                            isCorrupted = true;
                            continue;
                        }

                        String msg = dataSet.getTimeStamp()+","+dataSet.getxAxis()+","+dataSet.getyAxis()+","+dataSet.getzAxis();
                        fileWriter.write(msg + "\n");
                        fileWriter.flush();
                        incomingAccelerometerDataset.add(dataSet);
                    }
                    /*if (isCorrupted){
                        handler.displayNotification(date,rawAcc.getName() + " file is Corrupted",NOTIFICATION_ID);
                        stopWorker();
                    }*/
                } catch (MalformedInputException e){
                    handler.displayNotification(date,rawAcc.getName() + " file is Corrupted",NOTIFICATION_ID);
                    stopWorker();
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread1.start();

        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream inputStream = new FileInputStream(rawGyro);
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
                    String inputLine="";
                    File file1 = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/.FFWList/" + rawGyro.getName());
                    if(file1.exists()) FileUtils.forceDelete(file1);
                    FileWriter fileWriter = new FileWriter(Environment.getExternalStoragePublicDirectory(
                            "FreeForm-Writing/.FFWList/" + rawGyro.getName()),true);
                    boolean isCorrupted = false;
                    while ((inputLine = bufferedReader.readLine())!=null){
                        //Split the data by ','
                        String[] tokens = inputLine.split(",");
                        //if (tokens[0].equals("dummy"))
                        //    break;
                        //Read the data
                        if (!isLong(tokens[0])){
                            isCorrupted = true;
                            continue;
                        }
                        DataSet dataSet = new DataSet("0",0,0,0);
                        dataSet.setTimeStamp(tokens[0]);
                        if(tokens.length>=2 && tokens[1].length()>0 && isDouble(tokens[1])){
                            dataSet.setxAxis(Double.parseDouble(tokens[1]));
                        }
                        else {
                            isCorrupted = true;
                            continue;
                        }
                        if(tokens.length>=3 && tokens[2].length()>0 && isDouble(tokens[2])){
                            dataSet.setyAxis(Double.parseDouble(tokens[2]));
                        }
                        else {
                            isCorrupted = true;
                            continue;
                        }
                        if(tokens.length>=4 && tokens[3].length()>0 && isDouble(tokens[3])){
                            dataSet.setzAxis(Double.parseDouble(tokens[3]));
                        }
                        else {
                            isCorrupted = true;
                            continue;
                        }

                        String msg = dataSet.getTimeStamp()+","+dataSet.getxAxis()+","+dataSet.getyAxis()+","+dataSet.getzAxis();
                        fileWriter.write(msg + "\n");
                        fileWriter.flush();
                        incomingGyroscopeDataset.add(dataSet);
                    }
                    /*if (isCorrupted){
                        handler.displayNotification(date,rawGyro.getName() + " file is Corrupted",NOTIFICATION_ID);
                        stopWorker();
                    }*/
                } catch (MalformedInputException e){
                    handler.displayNotification(date,rawGyro.getName() + " file is Corrupted",NOTIFICATION_ID);
                    stopWorker();
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread2.start();
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private  void equalize(){
        int i = 0,j = 0;
        long a = Long.parseLong(incomingAccelerometerDataset.get(0).getTimeStamp());
        long g = Long.parseLong(incomingGyroscopeDataset.get(0).getTimeStamp());
        int diff = (int) Math.abs(a - g);
        List<DataSet> garbage = new ArrayList<>();
        if (a < g){
            a = Long.parseLong(incomingAccelerometerDataset.get(1).getTimeStamp());
            int check = (int) Math.abs(a - g);
            while(diff > 20){
                garbage.add(incomingAccelerometerDataset.get(i));
                i++;
                diff = check;
                a = Long.parseLong(incomingAccelerometerDataset.get(i+1).getTimeStamp());
                check = (int) Math.abs(a - g);
            }
            if (diff > check)
                garbage.add(incomingAccelerometerDataset.get(i));
            for (DataSet dataSet : garbage){
                incomingAccelerometerDataset.remove(dataSet);
            }
        } else if (a > g){
            g = Long.parseLong(incomingGyroscopeDataset.get(1).getTimeStamp());
            int check = (int) Math.abs(a - g);
            while(diff > 20){
                garbage.add(incomingGyroscopeDataset.get(j));
                j++;
                diff = check;
                g = Long.parseLong(incomingGyroscopeDataset.get(j+1).getTimeStamp());
                check = (int) Math.abs(a - g);
            }
            if (diff > check)
                garbage.add(incomingGyroscopeDataset.get(j));
            for (DataSet dataSet : garbage) {
                incomingGyroscopeDataset.remove(dataSet);
            }
        }
    }

    private void generateSegment() {
        SegmentGeneration segmentGeneration = new SegmentGeneration(incomingAccelerometerDataset,logger, date);
        segmentGeneration.run();
        List<Segment> buffSegment = segmentGeneration.generateSegment();
        //logger.write("","Segment length: " + buffSegment.size());
        long startTime = System.currentTimeMillis();
        PenUpDownClustering penUpDownClustering = new PenUpDownClustering(incomingGyroscopeDataset,buffSegment,date,logger);
        segments = penUpDownClustering.run();
        long endTime = System.currentTimeMillis();
        double time = ((double)(endTime - startTime))/1000.0;
        logger.write("","PenUpDown Clustering Module Successful, Time elapsed:- " + time + "seconds");
    }

    private void applyLowPass() {
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                String saveDir = String.valueOf(Environment.getExternalStoragePublicDirectory("FreeForm-Writing/." + date + "/lowAcc.csv"));
                logger.write(TAG,"LowPass started for raw accelerometer");
                String filePath = String.valueOf(Environment.getExternalStoragePublicDirectory("FreeForm-Writing/.FFWList/" + rawAcc.getName()));
                LowPassFilter lowPassFilterAcc = new LowPassFilter(incomingAccelerometerDataset,segments,logger,date,filePath,saveDir);
                updatedAccelerometer = lowPassFilterAcc.applyLowPassFilter();
                long endTime = System.currentTimeMillis();
                double time = ((double)(endTime - startTime))/1000.0;
                logger.write("","LowPass Applied on Accelerometer data, Time elapsed:- " + time + "seconds");
            }
        });
        thread1.start();

        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                String saveDirGyro = String.valueOf(Environment.getExternalStoragePublicDirectory("FreeForm-Writing/." + date + "/lowGyro.csv"));
                logger.write(TAG,"LowPass started for raw gyroscope");
                String filePath = String.valueOf(Environment.getExternalStoragePublicDirectory("FreeForm-Writing/.FFWList/" + rawGyro.getName()));
                LowPassFilter lowPassFilterGyro = new LowPassFilter(incomingGyroscopeDataset,segments,logger,date,filePath,saveDirGyro);
                updatedGyroscope = lowPassFilterGyro.applyLowPassFilter();
                long endTime = System.currentTimeMillis();
                double time = ((double)(endTime - startTime))/1000.0;
                logger.write("","LowPass Applied on Gyroscope data, Time elapsed:- " + time + "seconds");
            }
        });
        thread2.start();
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void applyCalibration() {
        incomingAccelerometerDataset.clear();
        incomingGyroscopeDataset.clear();
        long startTime = System.currentTimeMillis();
        Calibration calibration = new Calibration(updatedAccelerometer,updatedGyroscope,segments,date,logger);
        calibration.analyze();
        long endTime = System.currentTimeMillis();
        double time = ((double)(endTime - startTime))/1000.0;
        logger.write("","Calibration Module Successful, Time elapsed:- " + time + "seconds");
    }

    private void getPdfFromImage() {
        long startTime = System.currentTimeMillis();
        File inData = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/." + date + "/images");
        int i=0,k=0,n=1,lenX=6,lenY=10,totalFile=inData.listFiles().length;
        Bitmap[] parts = new Bitmap[60];
        for(File file : inData.listFiles()){
            Bitmap b = BitmapFactory.decodeFile(file.getAbsolutePath());
            //Bitmap bm = Bitmap.createScaledBitmap(b,1160,1160,true);
            parts[i] = b;
            i++;
            k++;
            if(i == lenX*lenY || k == totalFile){
                Bitmap result = Bitmap.createBitmap(parts[0].getWidth() * 6, parts[0].getHeight() * 10, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(result);
                Paint paint = new Paint();
                for (int j = 0; j < i; j++) {
                    canvas.drawBitmap(parts[j], parts[j].getWidth() * (j % 6), parts[j].getHeight() * (j / 6), paint);
                }

                i=0;
                FileOutputStream os = null;
                String name = n + ".png";
                n++;

                try {
                    File working = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/." + date + "/.Working");
                    if(!working.exists())
                        working.mkdir();
                    os = new FileOutputStream(String.valueOf(Environment.getExternalStoragePublicDirectory("FreeForm-Writing/." + date + "/.Working/" + name)));
                    result.compress(Bitmap.CompressFormat.PNG, 100, os);
                    os.flush();
                    os.close();
                    Log.e(TAG,"Image Saved");
                } catch(Exception e) {
                    Log.v("error saving","error saving");
                    e.printStackTrace();
                }
            }
        }

        try {
            FileUtils.forceDelete(inData);

            Document document = new Document();
            File fileIn = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/Output/" + date + "_1.pdf");
            PdfWriter.getInstance(document,new FileOutputStream(fileIn.getAbsolutePath()));
            document.open();

            File outputImg = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/." + date + "/.Working");
            for(File file : outputImg.listFiles()){
                Image image = Image.getInstance(file.getAbsolutePath());

                float scaler = ((document.getPageSize().getWidth() - document.leftMargin()
                        - document.rightMargin() - 0) / image.getWidth()) * 100; // 0 means you have no indentation. If you have any, change it.
                image.scalePercent(scaler);
                image.setAlignment(Image.ALIGN_CENTER | Image.ALIGN_TOP);

                document.add(image);
                FileUtils.forceDelete(file);
            }

            document.close();
            File fileOut = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/Output/" + date + ".pdf");
            if (!fileOut.exists()){
                FileUtils.copyFile(fileIn,fileOut);
                FileUtils.forceDelete(fileIn);
            } else {
                File fileOut1 = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/Output/" + date + "_2.pdf");
                FileUtils.copyFile(fileOut,fileOut1);
                FileUtils.forceDelete(fileOut);
                PdfReader reader1 = new PdfReader(fileOut1.getAbsolutePath());
                PdfReader reader2 = new PdfReader(fileIn.getAbsolutePath());
                Document doc = new Document();
                FileOutputStream fos = new FileOutputStream(String.valueOf(Environment.getExternalStoragePublicDirectory("FreeForm-Writing/Output/" + date + ".pdf")));
                PdfCopy copy = new PdfCopy(doc, fos);
                doc.open();
                PdfImportedPage page;
                PdfCopy.PageStamp stamp;
                Phrase phrase;
                BaseFont bf = BaseFont.createFont();
                Font font = new Font(bf, 9);
                int z = reader1.getNumberOfPages();
                for (int p = 1; p <= reader1.getNumberOfPages(); p++) {
                    page = copy.getImportedPage(reader1, p);
                    stamp = copy.createPageStamp(page);
                    phrase = new Phrase("page " + p, font);
                    ColumnText.showTextAligned(stamp.getOverContent(), Element.ALIGN_CENTER, phrase, 520, 5, 0);
                    stamp.alterContents();
                    copy.addPage(page);
                }
                for (int p = 1; p <= reader2.getNumberOfPages(); p++) {
                    page = copy.getImportedPage(reader2, p);
                    stamp = copy.createPageStamp(page);
                    phrase = new Phrase("page " + (z + p), font);
                    ColumnText.showTextAligned(stamp.getOverContent(), Element.ALIGN_CENTER, phrase, 520, 5, 0);
                    stamp.alterContents();
                    copy.addPage(page);
                }
                doc.close();
                reader1.close();
                reader2.close();
                FileUtils.forceDelete(fileIn);
                FileUtils.forceDelete(fileOut1);
            }

        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        File file = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/." + date);
        try {
            FileUtils.forceDelete(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis();
        double time = ((double)(endTime - startTime))/1000.0;
        logger.write("","PDF Generated Successfully, Time elapsed:- " + time + "seconds");
    }

    private String getDateFromName(String fileName){
        String name = FilenameUtils.getBaseName(fileName);
        Pattern p =Pattern.compile("_");
        String [] s = p.split(name);
        return s[s.length-1];
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            CharSequence name = "Personal Notification";
            String desc = "include all the personal notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID,name,importance);
            notificationChannel.setDescription(desc);
            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    private boolean isLong(String input){
        try {
            Long.parseLong(input);
            return true;
        } catch (Exception e){
            return false;
        }
    }

    private boolean isDouble(String input){
        try {
            Double.parseDouble(input);
            return true;
        } catch (Exception e){
            return false;
        }
    }

    private int getRandomNumber(){
        Random r = new Random();
        return r.nextInt(21 - 1) + 1;
    }

    private void stopWorker(){
        WorkManager.getInstance(getApplicationContext()).cancelWorkById(getId());
    }

    private void end() {
        //File file = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/Output/" + date + ".pdf");
        //handler.displayOnCLickNotification("Output",file.getName() + " generated" , NOTIFICATION_ID,file);
        handler.displayNotification(date,"Pdf generated",NOTIFICATION_ID);
        moduleEnd = System.currentTimeMillis();
        double time = ((double)(moduleEnd - moduleStart))/1000.0;
        logger.write(TAG,"Process completed successfully, Time elapsed:- " + time + "seconds");
        try {
            FileUtils.forceDelete(rawAcc);
            FileUtils.forceDelete(rawGyro);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
