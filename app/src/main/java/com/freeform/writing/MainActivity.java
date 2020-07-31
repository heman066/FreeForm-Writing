package com.freeform.writing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.work.OneTimeWorkRequest;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.freeform.writing.Functions.Calibration;
import com.freeform.writing.Functions.Ip;
import com.freeform.writing.Functions.LowPassFilter;
import com.freeform.writing.Functions.PenUpDownClustering;
import com.freeform.writing.Functions.SegmentGeneration;
import com.freeform.writing.Model.DataSet;
import com.freeform.writing.Model.Segment;
import com.freeform.writing.Model.SegmentWithPenUpDown;
import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private List<DataSet> incomingAccelerometerDataset;
    private List<DataSet> incomingGyroscopeDataset;
    private List<DataSet> updatedAccelerometer;
    private List<DataSet> updatedGyroscope;
    private List<SegmentWithPenUpDown> segments;

    private RelativeLayout rel,rel4;
    private Button btnAccSelect, btnGyroSelect, btnGetAllData, btnReset;
    private Button btnGenarateSeg, btnLowPass , btnCalibrate, btnPdf;
    private TextView txtAcc, txtGyro;
    private Button btnReceive;
    private ImageButton btnHotspot;

    private Handler mHandler;

    public static final int MULTIPLE_PERMISSIONS = 10;
    private final String[] permissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET};

    private SegmentGeneration segmentGeneration;

    private DialogProperties properties;

    private String inputDate;
    private String rawGyroPath, rawAccPath;
    private String mainActivity = "Main Activity";
    private Logger logger;
    private long appStartTime, appEndTime;

    private WifiManager.LocalOnlyHotspotReservation mReservation;
    private WifiManager wifiManager;

    private static final String MESSAGE_PATH = "/message";

    private GoogleApiClient googleApiClient;
    private String nodeId;

    private boolean isAccLoaded, isGyroLoaded;

    private List<OneTimeWorkRequest> workerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        while(!hasPermissions(this,permissions)){
            ActivityCompat.requestPermissions(this,permissions,MULTIPLE_PERMISSIONS);
        }
        AddDirectory.addDirectory();
        logger = new Logger();
        logger.write(mainActivity,"App has Started");
        appStartTime = System.currentTimeMillis();
        logger.write(mainActivity,"Current battery percentage: " + getBatteryPercentage());

        init();
        onClickListners();

    }

    private void init() {
        incomingAccelerometerDataset = new ArrayList<>();
        incomingGyroscopeDataset = new ArrayList<>();
        updatedAccelerometer = new ArrayList<>();
        segments = new ArrayList<>();

        btnCalibrate = findViewById(R.id.btn_calibrate);
        btnGenarateSeg = findViewById(R.id.btn_segments);
        btnLowPass = findViewById(R.id.btn_lowPass);
        btnPdf = findViewById(R.id.btn_pdf);
        btnReset = findViewById(R.id.btn_reset);
        btnAccSelect = findViewById(R.id.btnAccSelect);
        btnGyroSelect = findViewById(R.id.btnGyroSelect);
        btnGetAllData = findViewById(R.id.btn_getAllData);
        btnReceive = findViewById(R.id.btn_receive);
        btnHotspot = findViewById(R.id.btn_hotspot);

        isAccLoaded = isGyroLoaded = false;

        rel = findViewById(R.id.rel);
        rel4 = findViewById(R.id.rel4);

        txtAcc = findViewById(R.id.txtAcc);
        txtGyro = findViewById(R.id.txtGyro);

        mHandler = new Handler(Looper.getMainLooper());

        properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(DialogConfigs.DEFAULT_DIR);
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.offset = new File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = null;

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if(!Python.isStarted()){
            Python.start(new AndroidPlatform(this));
        }


        googleApiClient = new GoogleApiClient.Builder(this).addApi(Wearable.API)
                .addConnectionCallbacks(mConnectionCallbacks).build();
        googleApiClient.connect();

        workerList = new ArrayList<>();
    }

    private final GoogleApiClient.ConnectionCallbacks mConnectionCallbacks
            = new GoogleApiClient.ConnectionCallbacks() {

        @Override
        public void onConnected(@Nullable final Bundle bundle) {
            findNodes();
        }

        @Override
        public void onConnectionSuspended(final int i) {
        }
    };

    private void findNodes() {
        Wearable.NodeApi.getConnectedNodes(googleApiClient).setResultCallback(
                new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(@NonNull final NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                        List<Node> nodes = getConnectedNodesResult.getNodes();
                        if (nodes != null && !nodes.isEmpty()) {
                            nodeId = nodes.get(0).getId();
                        }
                    }
                });
    }

    private void onClickListners() {
        btnAccSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final FilePickerDialog dialog = new FilePickerDialog(MainActivity.this,properties);
                dialog.setTitle("Select Accelerometer File");
                dialog.setDialogSelectionListener(new DialogSelectionListener() {
                    @Override
                    public void onSelectedFilePaths(final String[] files) {

                        logger.write(mainActivity,"Accelerometer File is selected");
                        final long loadStart = System.currentTimeMillis();
                        final File file = new File(files[0]);


                        incomingAccelerometerDataset.clear();
                        new Thread(new Runnable() {
                            @RequiresApi(api = Build.VERSION_CODES.O)
                            @Override
                            public void run() {
                                boolean isCorrupted = false;
                                try {
                                    InputStream inputStream ;
                                    inputStream = new FileInputStream(file);
                                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
                                    String inputLine="";
                                    File file1 = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/.FFWList/" + file.getName());
                                    if(file1.exists()) FileUtils.forceDelete(file1);
                                    FileWriter fileWriter = new FileWriter(Environment.getExternalStoragePublicDirectory(
                                            "FreeForm-Writing/.FFWList/" + file.getName()),true);
                                    while ((inputLine = bufferedReader.readLine())!=null){
                                        //Split the data by ','
                                        String[] tokens = inputLine.split(",");
                                        if (!isLong(tokens[0])){
                                            isCorrupted = true;
                                            break;
                                        }
                                        //Read the data
                                        DataSet dataSet = new DataSet("0",0,0,0);
                                        dataSet.setTimeStamp(tokens[0]);
                                        if(tokens.length>=2 && tokens[1].length()>0 && isDouble(tokens[1])){
                                            dataSet.setxAxis(Double.parseDouble(tokens[1]));
                                        }
                                        else {
                                            isCorrupted = true;
                                            break;
                                        }
                                        if(tokens.length>=3 && tokens[2].length()>0 && isDouble(tokens[2])){
                                            dataSet.setyAxis(Double.parseDouble(tokens[2]));
                                        }
                                        else {
                                            isCorrupted = true;
                                            break;
                                        }
                                        if(tokens.length>=4 && tokens[3].length()>0 && isDouble(tokens[3])){
                                            dataSet.setzAxis(Double.parseDouble(tokens[3]));
                                        }
                                        else {
                                            isCorrupted = true;
                                            break;
                                        }
                                        incomingAccelerometerDataset.add(dataSet);

                                        String msg = dataSet.getTimeStamp()+","+dataSet.getxAxis()+","+dataSet.getyAxis()+","+dataSet.getzAxis();
                                        fileWriter.write(msg + "\n");
                                        fileWriter.flush();
                                    }
                                } catch (MalformedInputException e){
                                    logger.write(mainActivity,file.getName() + " file is Corrupted");
                                    e.printStackTrace();
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                if (!isCorrupted){
                                    long loadEnd = System.currentTimeMillis();
                                    double time =((double)(loadEnd - loadStart))/1000.0;
                                    isAccLoaded = true;
                                    logger.write(mainActivity,"Loading time of the accelerometer file " + file.getName() + " is: " + time + " seconds");
                                    btnAccSelect.setVisibility(View.GONE);
                                    inputDate = getDateFromName(file.getName());
                                    rawAccPath = file.getAbsolutePath();
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            txtAcc.setText(file.getName());
                                        }
                                    });
                                } else {
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(MainActivity.this, file.getName() + " file is Corrupted",Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            }
                        }).start();
                    }
                });

                dialog.show();
            }
        });

        btnGyroSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final FilePickerDialog dialog = new FilePickerDialog(MainActivity.this,properties);
                dialog.setTitle("Select Gyroscope File");
                dialog.setDialogSelectionListener(new DialogSelectionListener() {
                    @Override
                    public void onSelectedFilePaths(final String[] files) {

                        logger.write(mainActivity,"Gyroscope File is selected");
                        final long loadStart = System.currentTimeMillis();
                        final File file = new File(files[0]);

                        incomingGyroscopeDataset.clear();
                        new Thread(new Runnable() {
                            @RequiresApi(api = Build.VERSION_CODES.O)
                            @Override
                            public void run() {
                                boolean isCorrupted = false;
                                try {
                                    InputStream inputStream ;
                                    inputStream = new FileInputStream(file);
                                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
                                    String inputLine="";
                                    File file1 = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/.FFWList/" + file.getName());
                                    if(file1.exists()) FileUtils.forceDelete(file1);
                                    FileWriter fileWriter = new FileWriter(Environment.getExternalStoragePublicDirectory(
                                            "FreeForm-Writing/.FFWList/" + file.getName()),true);
                                    while ((inputLine = bufferedReader.readLine())!=null){
                                        //Split the data by ','
                                        String[] tokens = inputLine.split(",");
                                        if (!isLong(tokens[0])){
                                            isCorrupted = true;
                                            break;
                                        }
                                        //Read the data
                                        DataSet dataSet = new DataSet("0",0,0,0);
                                        dataSet.setTimeStamp(tokens[0]);
                                        if(tokens.length>=2 && tokens[1].length()>0 && isDouble(tokens[1])){
                                            dataSet.setxAxis(Double.parseDouble(tokens[1]));
                                        }
                                        else {
                                            isCorrupted = true;
                                            break;
                                        }
                                        if(tokens.length>=3 && tokens[2].length()>0 && isDouble(tokens[2])){
                                            dataSet.setyAxis(Double.parseDouble(tokens[2]));
                                        }
                                        else {
                                            isCorrupted = true;
                                            break;
                                        }
                                        if(tokens.length>=4 && tokens[3].length()>0 && isDouble(tokens[3])){
                                            dataSet.setzAxis(Double.parseDouble(tokens[3]));
                                        }
                                        else {
                                            isCorrupted = true;
                                            break;
                                        }
                                        incomingGyroscopeDataset.add(dataSet);

                                        String msg = dataSet.getTimeStamp()+","+dataSet.getxAxis()+","+dataSet.getyAxis()+","+dataSet.getzAxis();
                                        fileWriter.write(msg + "\n");
                                        fileWriter.flush();
                                    }
                                } catch (MalformedInputException e){
                                    //Toast.makeText(MainActivity.this, file.getName() + " file is Corrupted",Toast.LENGTH_LONG).show();
                                    logger.write(mainActivity,file.getName() + " file is Corrupted");
                                    e.printStackTrace();
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                if (!isCorrupted){
                                    long loadEnd = System.currentTimeMillis();
                                    double time =((double)(loadEnd - loadStart))/1000.0;
                                    isGyroLoaded = true;
                                    logger.write(mainActivity,"Loading time of the gyroscope file " + file.getName() + " is: " + time + " seconds");
                                    btnGyroSelect.setVisibility(View.GONE);
                                    rawGyroPath = file.getAbsolutePath();
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            txtGyro.setText(file.getName());
                                        }
                                    });
                                } else{
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(MainActivity.this, file.getName() + " file is Corrupted",Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            }
                        }).start();
                    }
                });

                dialog.show();
            }
        });

        btnGetAllData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getIncomingDataSet();
            }
        });

        final boolean[] segGenerated = {false}, lowpassApplied = {false}, isCalibrated = {false};
        final boolean[] segCheck = {false};

        btnGenarateSeg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!segCheck[0]){
                    if(incomingAccelerometerDataset.size() == 0 || incomingGyroscopeDataset.size() == 0)
                        Toast.makeText(MainActivity.this,"Please Select all Files",Toast.LENGTH_LONG).show();
                    else if( isAccLoaded || isGyroLoaded)
                        Toast.makeText(MainActivity.this,"Please Wait for Files to be Loaded",Toast.LENGTH_LONG).show();
                    else{
                        final long loadStart = System.currentTimeMillis();
                        Thread t1 = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                segCheck[0] = true;
                                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
                                try {
                                    equalize();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                segmentGeneration = new SegmentGeneration(incomingAccelerometerDataset,logger, inputDate);
                                segmentGeneration.run();
                                final List<Segment> buffSegment = segmentGeneration.generateSegment();
                                Log.e("Normal Seg",buffSegment.size() + "");
                                logger.write(mainActivity,"PenUpDownClustering module started");
                                Thread thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        PenUpDownClustering penUpDownClustering = new PenUpDownClustering(incomingGyroscopeDataset,buffSegment,inputDate);
                                        segments = penUpDownClustering.run();
                                    }
                                });
                                thread.start();
                                try {
                                    thread.join();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        btnGetAllData.setVisibility(View.GONE);
                                        btnGenarateSeg.setVisibility(View.GONE);
                                        Toast.makeText(MainActivity.this,"Segment Generation Successful",Toast.LENGTH_LONG).show();
                                    }
                                });
                                Log.e("Seg",segments.size() + "");
                                segGenerated[0] = true;
                                long loadEnd = System.currentTimeMillis();
                                double time =((double)(loadEnd - loadStart))/1000.0;
                                logger.write(mainActivity,"Segments are generated, elapsed time: " + time + " seconds");
                            }
                        });
                        t1.start();
                    }
                } else
                    Toast.makeText(MainActivity.this,"Process is ongoing please wait",Toast.LENGTH_LONG).show();
            }
        });

        final boolean[] lowCheck = {false,false};
        btnLowPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!lowCheck[0] && !lowCheck[1]){
                    if(!segGenerated[0])
                        Toast.makeText(MainActivity.this,"Please Generate Segment First",Toast.LENGTH_LONG).show();
                    else{
                        final long loadStart = System.currentTimeMillis();
                        final boolean[] com1 = {false};
                        final boolean[] com2 = {false};
                        Thread t3 = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                lowCheck[0] = true;
                                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
                                String saveDir = String.valueOf(Environment.getExternalStoragePublicDirectory("FreeForm-Writing/." + inputDate + "/lowAcc.csv"));
                                logger.write(mainActivity,"LowPass started for raw accelerometer");
                                LowPassFilter lowPassFilterAcc = new LowPassFilter(incomingAccelerometerDataset,segments,logger,inputDate,rawAccPath,saveDir);
                                updatedAccelerometer = lowPassFilterAcc.applyLowPassFilter();
                                com1[0] = true;
                                if(com2[0]){
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            btnLowPass.setVisibility(View.GONE);
                                            Toast.makeText(MainActivity.this,"LowPass Filter applied",Toast.LENGTH_LONG).show();
                                        }
                                    });

                                    lowpassApplied[0] = true;
                                    long loadEnd = System.currentTimeMillis();
                                    double time =((double)(loadEnd - loadStart))/1000.0;
                                    logger.write(mainActivity,"LowPass Filter applied, elapsed time: " + time + " seconds");

                                }
                            }
                        });
                        t3.start();

                        Thread t4 = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                lowCheck[1] = true;
                                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
                                String saveDir = String.valueOf(Environment.getExternalStoragePublicDirectory("FreeForm-Writing/." + inputDate + "/lowGyro.csv"));
                                logger.write(mainActivity,"LowPass started for raw gyroscope");
                                LowPassFilter lowPassFilterGyro = new LowPassFilter(incomingGyroscopeDataset,segments,logger,inputDate,rawGyroPath,saveDir);
                                updatedGyroscope = lowPassFilterGyro.applyLowPassFilter();
                                com2[0] = true;
                                if(com1[0]){
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            btnLowPass.setVisibility(View.GONE);
                                            Toast.makeText(MainActivity.this,"LowPass Filter applied",Toast.LENGTH_LONG).show();
                                        }
                                    });

                                    lowpassApplied[0] = true;
                                    long loadEnd = System.currentTimeMillis();
                                    double time =((double)(loadEnd - loadStart))/1000.0;
                                    logger.write(mainActivity,"LowPass Filter applied, elapsed time: " + time + " seconds");
                                }
                            }
                        });
                        t4.start();
                    }
                } else
                    Toast.makeText(MainActivity.this,"Process is ongoing please wait",Toast.LENGTH_LONG).show();
            }
        });

        final boolean[] calCheck = {false};
        btnCalibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!calCheck[0]){
                    if(!lowpassApplied[0])
                        Toast.makeText(MainActivity.this,"Please apply LowPass Filter first",Toast.LENGTH_LONG).show();
                    else{
                        final long loadStart = System.currentTimeMillis();
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                calCheck[0] = true;
                                incomingAccelerometerDataset.clear();
                                incomingGyroscopeDataset.clear();

                                Calibration calibration = new Calibration(updatedAccelerometer,updatedGyroscope,segments,inputDate,logger);
                                calibration.analyze();

                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        btnCalibrate.setVisibility(View.GONE);
                                        Toast.makeText(MainActivity.this,"Calibration Successful",Toast.LENGTH_LONG).show();
                                    }
                                });
                                isCalibrated[0] = true;
                                long loadEnd = System.currentTimeMillis();
                                double time =((double)(loadEnd - loadStart))/1000.0;
                                logger.write(mainActivity,"Calibration successful, elapsed time: " + time + " seconds");
                            }
                        });
                        thread.start();
                    }
                } else
                    Toast.makeText(MainActivity.this,"Process is ongoing please wait",Toast.LENGTH_LONG).show();
            }
        });

        final boolean[] pdfCheck = {false};
        btnPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!pdfCheck[0]){
                    if(!isCalibrated[0])
                        Toast.makeText(MainActivity.this,"Please Calibrate first",Toast.LENGTH_LONG).show();
                    else{
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                pdfCheck[0] = true;
                                getPdfFromImage();
                                File file = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/." + inputDate);
                                try {
                                    FileUtils.forceDelete(file);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this,"PDF generated",Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }).start();
                    }
                } else
                    Toast.makeText(MainActivity.this,"Process is ongoing please wait",Toast.LENGTH_LONG).show();
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                incomingAccelerometerDataset.clear();
                incomingGyroscopeDataset.clear();
                if(updatedAccelerometer!=null) updatedAccelerometer.clear();
                if(updatedGyroscope!=null) updatedGyroscope.clear();
                segments.clear();

                btnGenarateSeg.setVisibility(View.VISIBLE);
                btnGetAllData.setVisibility(View.VISIBLE);
                btnLowPass.setVisibility(View.VISIBLE);
                btnCalibrate.setVisibility(View.VISIBLE);
                btnAccSelect.setVisibility(View.VISIBLE);
                btnGyroSelect.setVisibility(View.VISIBLE);

                txtAcc.setText("Select Accelerometer Data");
                txtGyro.setText("Select Gyroscope Data");

                isAccLoaded = isGyroLoaded = false;
                logger.write(mainActivity,"Reset, you can add new files");
            }
        });

        btnHotspot.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
                ConnectivityManager cm =
                        (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                boolean isConnected = activeNetwork != null &&
                        activeNetwork.isConnectedOrConnecting();
                //if (!manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) && !isConnected)
                //   enableGPS();

                turnOnHotspot();
            }
        });

        btnReceive.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                if (mReservation != null){
                    new Thread(new ServerTask()).start();
                } else
                    Toast.makeText(MainActivity.this,"Turn on the WiFi-HotSpot",Toast.LENGTH_SHORT).show();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void turnOnHotspot() {
        if (mReservation == null){

            wifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {

                @Override
                public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                    super.onStarted(reservation);
                    mReservation = reservation;
                    Toast.makeText(MainActivity.this,"WiFi-HotSpot Enabled",Toast.LENGTH_SHORT).show();
                    btnHotspot.setImageResource(R.drawable.ic_portable_wifi_off_black_24dp);

                    String SSID = mReservation.getWifiConfiguration().SSID;
                    String password = mReservation.getWifiConfiguration().preSharedKey;
                    String ip = Ip.getDottedDecimalIP(Ip.ipadd());
                    Log.e("Main",SSID);
                    Log.e("Main",password);
                    Log.e("Main",ip);

                    final String info = SSID + "::" + password + "::" + ip;

                    if (nodeId != null) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Wearable.MessageApi.sendMessage(googleApiClient, nodeId, MESSAGE_PATH, info.getBytes(Charset.forName("UTF-8")));
                            }
                        }).start();
                    }
                    /*Asset asset = Asset.createFromBytes(info.getBytes(Charset.forName("UTF-8")));
                    PutDataRequest putDataRequest = PutDataRequest.create("/connection");
                    putDataRequest.putAsset("info",asset);
                    Task<DataItem> putTask = Wearable.getDataClient(getApplicationContext()).putDataItem(putDataRequest);*/
                }

                @Override
                public void onStopped() {
                    super.onStopped();
                }

                @Override
                public void onFailed(int reason) {
                    super.onFailed(reason);
                }
            }, new Handler());
        } else{
            Toast.makeText(MainActivity.this,"WiFi-HotSpot Disabled",Toast.LENGTH_SHORT).show();
            mReservation.close();
            mReservation = null;
            btnHotspot.setImageResource(R.drawable.ic_wifi_tethering_black_24dp);
        }
    }

    public void enableGPS() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder
                .setMessage(R.string.gps_msg)
                .setCancelable(false)
                .setTitle("Turn on Location")
                .setPositiveButton(R.string.enable_gps,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                Intent callGPSSettingIntent = new Intent(
                                        Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivityForResult(callGPSSettingIntent, 5);
                            }
                        });
        alertDialogBuilder.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    private void getIncomingDataSet() {
        File inData = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/Input Data/");
        File[] listFile = inData.listFiles();
        if(listFile.length==0){
            Toast.makeText(MainActivity.this,"Input directory is Empty",Toast.LENGTH_LONG).show();
            return;
        }
        for(final File file:listFile){
            if(file.getName().contains("ACC")){
                logger.write(mainActivity,"Accelerometer File is selected");
                rawAccPath = file.getAbsolutePath();
            }
            else if(file.getName().contains("GYRO")){
                rawGyroPath = file.getAbsolutePath();
                logger.write(mainActivity,"Gyroscope File is selected");
            }
            new Thread(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void run() {
                    try {
                        final long loadStart = System.currentTimeMillis();
                        InputStream inputStream ;
                        inputStream = new FileInputStream(file);
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
                        String inputLine="";

                        File file1 = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/.FFWList/" + file.getName());
                        if(file1.exists()) FileUtils.forceDelete(file1);
                        FileWriter fileWriter = new FileWriter(Environment.getExternalStoragePublicDirectory(
                                "FreeForm-Writing/.FFWList/" + file.getName()),true);

                        boolean isCorrupted = false;

                        if(file.getName().contains("ACC"))
                            incomingAccelerometerDataset.clear();
                        else
                            incomingGyroscopeDataset.clear();
                        while ((inputLine = bufferedReader.readLine())!=null){
                            //Split the data by ','
                            String[] tokens = inputLine.split(",");
                            //Read the data
                            if (!isLong(tokens[0])){
                                isCorrupted = true;
                                break;
                            }
                            DataSet dataSet = new DataSet("0",0,0,0);
                            dataSet.setTimeStamp(tokens[0]);
                            if(tokens.length>=2 && tokens[1].length()>0 && isDouble(tokens[1])){
                                dataSet.setxAxis(Double.parseDouble(tokens[1]));
                            }
                            else {
                                isCorrupted = true;
                                break;
                            }
                            if(tokens.length>=3 && tokens[2].length()>0 && isDouble(tokens[2])){
                                dataSet.setyAxis(Double.parseDouble(tokens[2]));
                            }
                            else {
                                isCorrupted = true;
                                break;
                            }
                            if(tokens.length>=4 && tokens[3].length()>0 && isDouble(tokens[3])){
                                dataSet.setzAxis(Double.parseDouble(tokens[3]));
                            }
                            else {
                                isCorrupted = true;
                                break;
                            }

                            String msg = dataSet.getTimeStamp()+","+dataSet.getxAxis()+","+dataSet.getyAxis()+","+dataSet.getzAxis();
                            fileWriter.write(msg + "\n");
                            fileWriter.flush();
                            if(file.getName().contains("ACC")){
                                inputDate = getDateFromName(file.getName());
                                incomingAccelerometerDataset.add(dataSet);
                            }else if(file.getName().contains("GYRO")){
                                incomingGyroscopeDataset.add(dataSet);
                            }
                        }
                        final boolean check = isCorrupted;
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (check){
                                    Toast.makeText(MainActivity.this, file.getName() + " file is Corrupted",Toast.LENGTH_SHORT).show();
                                    logger.write(mainActivity,file.getName() + " file is Corrupted");
                                }
                                else {
                                    if(file.getName().contains("ACC"))
                                        txtAcc.setText(file.getName());
                                    else if(file.getName().contains("GYRO"))
                                        txtGyro.setText(file.getName());
                                }
                            }
                        });
                        if (!check){
                            long loadEnd = System.currentTimeMillis();
                            double time =((double)(loadEnd - loadStart))/1000.0;
                            if(file.getName().contains("ACC")){
                                logger.write(mainActivity,"Loading time of the accelerometer file " + file.getName() + " is: " + time + " seconds");
                                isAccLoaded = true;
                            }
                            else if(file.getName().contains("GYRO")){
                                logger.write(mainActivity,"Loading time of the gyroscope file " + file.getName() + " is: " + time + " seconds");
                                isGyroLoaded = true;
                            }
                        }
                        //FileUtils.forceDelete(file);
                    } catch (MalformedInputException e){
                        Toast.makeText(MainActivity.this, file.getName() + " file is Corrupted",Toast.LENGTH_SHORT).show();
                        logger.write(mainActivity,file.getName() + " file is Corrupted");
                        e.printStackTrace();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        btnGetAllData.setVisibility(View.GONE);
    }

    private void getPdfFromImage() {
        long loadStart = System.currentTimeMillis();
        File inData = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/." + inputDate + "/images");
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
                    File working = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/." + inputDate + "/.Working");
                    if(!working.exists())
                        working.mkdir();
                    os = new FileOutputStream(String.valueOf(Environment.getExternalStoragePublicDirectory("FreeForm-Writing/." + inputDate + "/.Working/" + name)));
                    result.compress(Bitmap.CompressFormat.PNG, 100, os);
                    os.flush();
                    os.close();
                    Log.e("MainActivity","Image Saved");
                } catch(Exception e) {
                    Log.v("error saving","error saving");
                    e.printStackTrace();
                }
            }
        }

        try {
            //FileUtils.forceDelete(inData);

            Document document = new Document();
            PdfWriter.getInstance(document,new FileOutputStream(String.valueOf(Environment.getExternalStoragePublicDirectory("FreeForm-Writing/Output/" + inputDate + ".pdf"))));
            document.open();

            File outputImg = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/." + inputDate + "/.Working");
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
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        long loadEnd = System.currentTimeMillis();
        double time =((double)(loadEnd - loadStart))/1000.0;
        logger.write(mainActivity,"Pdf generated from output image, elapsed time: " + time + " seconds");
    }

    private String getDateFromName(String fileName){
        String name = FilenameUtils.getBaseName(fileName);
        Pattern p =Pattern.compile("_");
        String [] s = p.split(name);
        return s[s.length-1];
    }

    private void equalize() throws IOException {
        int i = 0,j = 0;
        long a = Long.parseLong(incomingAccelerometerDataset.get(0).getTimeStamp());
        long g = Long.parseLong(incomingGyroscopeDataset.get(0).getTimeStamp());
        int diff = (int) Math.abs(a - g);
        List<DataSet> garbage = new ArrayList<>();
        if (a < g){
            //FileWriter fileWriter = new FileWriter(Environment.getExternalStoragePublicDirectory("FreeForm-Writing/garbAcc.csv"),true);
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
                //String msg = dataSet.getTimeStamp() + "," + dataSet.getxAxis() + "," + dataSet.getyAxis() + "," + dataSet.getzAxis();
                //fileWriter.write(msg + "\n");
                //fileWriter.flush();
            }
        } else if (a > g){
            //FileWriter fileWriter = new FileWriter(Environment.getExternalStoragePublicDirectory("FreeForm-Writing/garbGyro.csv"),true);
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
                //String msg = dataSet.getTimeStamp() + "," + dataSet.getxAxis() + "," + dataSet.getyAxis() + "," + dataSet.getzAxis();
                //fileWriter.write(msg + "\n");
                //fileWriter.flush();
            }
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

    private double getBatteryPercentage(){
        BatteryManager bm = (BatteryManager)getSystemService(BATTERY_SERVICE);
        double batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        return batLevel;
    }

    public boolean hasPermissions(Context context, String... permissions){
        if(context!=null && permissions!=null ){
            for(String permission:permissions){
                if(ActivityCompat.checkSelfPermission(context,permission)!= PackageManager.PERMISSION_GRANTED){
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        appEndTime = System.currentTimeMillis();
        double time = ((double)(appEndTime - appStartTime))/1000.0;
        if (mReservation != null)
            mReservation.close();
        googleApiClient.disconnect();
        logger.write(mainActivity,"App is closing");
        logger.write(mainActivity,"Total time elapsed in the app: " + time + "seconds");
        logger.write(mainActivity,"Current battery percentage: " + getBatteryPercentage());
    }
}
