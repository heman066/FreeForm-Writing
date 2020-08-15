package com.freeform.writing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.freeform.writing.Functions.Ip;
import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements ServerCommunication {

    private Button btnAccSelect, btnGyroSelect, btnReset;
    private TextView txtAcc, txtGyro;
    private Button btnReceive;
    private ImageButton btnHotspot;

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

    private DialogProperties properties;

    private String rawGyroPath, rawAccPath;
    private String mainActivity = "Main Activity";
    private boolean isAccLoaded, isGyroLoaded;

    private WifiManager.LocalOnlyHotspotReservation mReservation;
    private WifiManager wifiManager;

    private static final String MESSAGE_PATH = "/message";

    private GoogleApiClient googleApiClient;
    private String nodeId;

    private Button btnProcess, btnProcessAllData;
    private ProgressDialog progressDialog;
    private BasicFunctionHandler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        while(!hasPermissions(this,permissions)){
            ActivityCompat.requestPermissions(this,permissions,MULTIPLE_PERMISSIONS);
        }
        AddDirectory.addDirectory();
        init();
        onClickListners();
    }

    private void init() {
        txtAcc = findViewById(R.id.txtAcc);
        txtGyro = findViewById(R.id.txtGyro);
        btnReset = findViewById(R.id.btn_reset);
        btnAccSelect = findViewById(R.id.btnAccSelect);
        btnGyroSelect = findViewById(R.id.btnGyroSelect);
        btnReceive = findViewById(R.id.btn_receive);
        btnHotspot = findViewById(R.id.btn_hotspot);
        btnProcess = findViewById(R.id.process);
        btnProcessAllData = findViewById(R.id.process_all_data);

        isAccLoaded = isGyroLoaded = false;

        progressDialog = new ProgressDialog(this);
        handler = new BasicFunctionHandler(this);

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
                        File file = new File(files[0]);
                        btnAccSelect.setVisibility(View.GONE);
                        rawAccPath = file.getAbsolutePath();
                        txtAcc.setText(file.getName());
                        isAccLoaded = true;
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
                        File file = new File(files[0]);
                        btnGyroSelect.setVisibility(View.GONE);
                        rawGyroPath = file.getAbsolutePath();
                        txtGyro.setText(file.getName());
                        isGyroLoaded = true;
                    }
                });
                dialog.show();
            }
        });

        btnProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAccLoaded && isGyroLoaded){
                    progressDialog.setMessage("Processing...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    Data data = new Data.Builder()
                            .putString("accFilePath",rawAccPath)
                            .putString("gyroFilePath",rawGyroPath)
                            .build();
                    OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MyWorker.class)
                            .setInputData(data)
                            .build();
                    WorkManager.getInstance(getApplicationContext())
                            .beginUniqueWork(rawAccPath + rawGyroPath, ExistingWorkPolicy.KEEP,request)
                            .enqueue();

                    rawGyroPath = rawAccPath = "";
                    txtAcc.setText("Select Accelerometer Data");
                    txtGyro.setText("Select Gyroscope Data");
                    btnAccSelect.setVisibility(View.VISIBLE);
                    btnGyroSelect.setVisibility(View.VISIBLE);
                    isAccLoaded = isGyroLoaded = false;
                    progressDialog.dismiss();
                    handler.showAlertDialog("","Data Processing Started");
                } else {
                    handler.showAlertDialog("","Please Select All Required Files");
                }
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnAccSelect.setVisibility(View.VISIBLE);
                btnGyroSelect.setVisibility(View.VISIBLE);
                rawGyroPath = rawAccPath = "";
                txtAcc.setText("Select Accelerometer Data");
                txtGyro.setText("Select Gyroscope Data");
                isAccLoaded = isGyroLoaded = false;
            }
        });

        btnProcessAllData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.setMessage("Processing...");
                progressDialog.setCancelable(false);
                progressDialog.show();

                getAllData();

                progressDialog.dismiss();
                handler.showAlertDialog("","Data Processing Started");
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

                turnOnHotspot();
            }
        });

        btnReceive.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                if (mReservation != null){
                    new ServerTask(MainActivity.this,handler).execute();
                    //new Thread(new ServerTask(MainActivity.this,handler)).start();
                } else
                    handler.showAlertDialog("","Turn on the WiFi-HotSpot within the App");
            }
        });
    }

    private void getAllData() {
        File indata= Environment.getExternalStoragePublicDirectory("FreeForm-Writing/Input Data/");
        for (File acc : indata.listFiles()){
            if (acc.getName().contains("ACC")){
                String date = getDateFromName(acc.getName());
                for (File gyro : indata.listFiles()){
                    if (gyro.getName().contains("GYRO") && gyro.getName().contains(date)){
                        String accPath = acc.getAbsolutePath();
                        String gyroPath = gyro.getAbsolutePath();
                        Data data = new Data.Builder()
                                .putString("accFilePath",accPath)
                                .putString("gyroFilePath",gyroPath)
                                .build();
                        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MyWorker.class)
                                .setInputData(data)
                                .build();
                        WorkManager.getInstance(getApplicationContext())
                                .beginUniqueWork(accPath + gyroPath,ExistingWorkPolicy.KEEP,request)
                                .enqueue();
                        break;
                    }
                }
            }
        }
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

                    final String info = SSID + "::" + password + "::" + ip + "::0";

                    if (nodeId != null) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Wearable.MessageApi.sendMessage(googleApiClient, nodeId, MESSAGE_PATH, info.getBytes(Charset.forName("UTF-8")));
                            }
                        }).start();
                    }
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

    private String getDateFromName(String fileName){
        String name = FilenameUtils.getBaseName(fileName);
        Pattern p =Pattern.compile("_");
        String [] s = p.split(name);
        return s[s.length-1];
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
        if (mReservation != null)
            mReservation.close();
        googleApiClient.disconnect();
    }

    @Override
    public void getMessage() {
        final String message = "Files Sent" + "::1";
        if (nodeId != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Wearable.MessageApi.sendMessage(googleApiClient, nodeId, MESSAGE_PATH, message.getBytes(Charset.forName("UTF-8")));
                }
            }).start();
        }
    }
}
