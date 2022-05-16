package com.example.umarosandroid;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.lifecycle.ProcessCameraProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.common.util.concurrent.ListenableFuture;

import org.ros.address.InetAddressFactory;
import org.ros.android.MasterChooser;
import org.ros.android.NodeMainExecutorService;
import org.ros.exception.RosRuntimeException;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class MainActivity extends AppCompatActivity {
    private static final int MASTER_CHOOSER_REQUEST_CODE = 0;

    TextView nameView;

    TextView cameraView;
    TextView imuView;
    TextView audioView;
    TextView gpsView;



    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch ramblerSwitch;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch cuadrigaSwitch;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch roverJ8Switch;


    boolean enableCamera;
    boolean enableAudio;
    boolean enableGps;
    boolean enableImu;

    boolean enableCuadriga;
    boolean enableRambler;
    boolean enableRoverJ8;
    //boolean enableNlp;

    String nodeName = "";

    private ServiceConnection nodeMainExecutorServiceConnection;
    private NodeMainExecutorService nodeMainExecutorService;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationManager mLocationManager;

    // IMU and Camera instances and views
    private SensorManager sensorManager;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    //private PreviewView previewView;

    private AudioManager mAudioManager;


    private MutableLiveData<NodeMainExecutor> nodeMainExecutorMutableLiveData = new MutableLiveData<>();
    private MutableLiveData<NodeConfiguration> nodeConfigurationMutableLiveData= new MutableLiveData<>();

    //public static final String ENABLE_CUADRIGA = "com.example.umarosandroid.ENABLE_CUADRIGA";
    //public static final String ENABLE_RAMBLER = "com.example.umarosandroid.ENABLE_RAMBLER";
    //public static final String ENABLE_ROVERJ8 = "com.example.umarosandroid.ENABLE_ROVERJ8";

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Intent intent = getIntent();
        String masterUri = intent.getStringExtra(CustomMasterChooser.MASTER_URI);

        nodeName = intent.getStringExtra(CustomMasterChooser.NODE_NAME); //get the input string by the user

        enableCamera = intent.getBooleanExtra(CustomMasterChooser.ENABLE_CAMERA,false);
        enableAudio = intent.getBooleanExtra(CustomMasterChooser.ENABLE_AUDIO,false);
        enableGps= intent.getBooleanExtra(CustomMasterChooser.ENABLE_GPS,false);
        enableImu = intent.getBooleanExtra(CustomMasterChooser.ENABLE_IMU,false);

        enableCuadriga = intent.getBooleanExtra(CustomMasterChooser.ENABLE_CUADRIGA,false);
        enableRambler = intent.getBooleanExtra(CustomMasterChooser.ENABLE_RAMBLER,false);
        enableRoverJ8 = intent.getBooleanExtra(CustomMasterChooser.ENABLE_ROVERJ8,false);
      //  enableNlp = intenAt.getBooleanExtra(CustomMasterChooser.ENABLE_NLP,false);

        System.out.println(masterUri);
        URI customUri = null;
        try {
            customUri = new URI(masterUri);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        nameView = (TextView) findViewById(R.id.ID); //get the object (with its content) in the first activity (setup window)
        nameView.setText("SAR ID: "+ nodeName); //nodeName is defined in the object content so here is known
        cameraView = (TextView) findViewById(R.id.cameraText);
        imuView = (TextView) findViewById(R.id.ImuText);
        audioView = (TextView) findViewById(R.id.audioText);
        gpsView = (TextView) findViewById(R.id.GPSText);

        nodeMainExecutorServiceConnection = new NodeMainExecutorServiceConnection(customUri);

        cuadrigaSwitch = (Switch) findViewById(R.id.cuadrigaSwitch); //It is associated with the object called "cuadrigaSwitch"
        ramblerSwitch = (Switch) findViewById(R.id.ramblerSwitch);
        roverJ8Switch = (Switch) findViewById(R.id.roverJ8Switch);

        cuadrigaSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableCuadriga = isChecked;
                CuadrigaNode cuadrigaNode = new CuadrigaNode(nodeName,enableCuadriga);
                System.out.println("CUADRIGA toggled");
                if(enableCuadriga) {
                    System.out.println("CUADRIGA is coming...");
                    System.out.println(enableCuadriga);
                    nodeMainExecutorMutableLiveData.getValue().execute(cuadrigaNode,
                            nodeConfigurationMutableLiveData.getValue());
                } else {
                    //do stuff when Switch if OFF
                    System.out.println("CUADRIGA is free!");
                    System.out.println(enableCuadriga);
                    nodeMainExecutorMutableLiveData.getValue().execute(cuadrigaNode,
                            nodeConfigurationMutableLiveData.getValue());
                }
        }});

        ramblerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableRambler = isChecked;
                RamblerNode ramblerNode = new RamblerNode(nodeName,enableRambler);
                System.out.println("RAMBLER toggled");

                if(enableRambler) {
                    System.out.println("RAMBLER is coming...");
                    System.out.println(enableRambler);
                    nodeMainExecutorMutableLiveData.getValue().execute(ramblerNode,
                            nodeConfigurationMutableLiveData.getValue());
                } else {
                    System.out.println("RAMBLER is free!");
                    System.out.println(enableRambler);
                    nodeMainExecutorMutableLiveData.getValue().execute(ramblerNode,
                            nodeConfigurationMutableLiveData.getValue());
                }
            }});

        roverJ8Switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableRoverJ8 = isChecked;
                System.out.println("ROVER J8 toggled");
                RoverJ8Node roverJ8Node = new RoverJ8Node(nodeName,enableRoverJ8);
                if(enableRoverJ8) {
                    System.out.println("ROVER J8 is coming...");
                    System.out.println(enableRoverJ8);
                    nodeMainExecutorMutableLiveData.getValue().execute(roverJ8Node,
                            nodeConfigurationMutableLiveData.getValue());

                } else {
                    //do stuff when Switch if OFF
                    System.out.println("ROVER J8 is free!");
                    System.out.println(enableRoverJ8);
                    nodeMainExecutorMutableLiveData.getValue().execute(roverJ8Node,
                            nodeConfigurationMutableLiveData.getValue());
                }
            }});

        if(enableCamera) {
            cameraProviderFuture = ProcessCameraProvider.getInstance(this);
            cameraView.setText(R.string.camera_on);

        }
        else {
            cameraView.setText(R.string.camera_off);
        }

        if(enableAudio) {
            audioView.setText(R.string.audio_on);
        }
        else {
            audioView.setText(R.string.audio_off);
        }

        if(enableGps) {
            mLocationManager = (LocationManager)this.getSystemService(LOCATION_SERVICE);
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            final boolean gpsEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (!gpsEnabled) {
                // Build an alert dialog here that requests that the user enable
                // the location services, then when the user clicks the "OK" button,
                // call enableLocationSettings()
                enableLocationSettings();
            }

            gpsView.setText(R.string.gps_on);

        }
        else {
            gpsView.setText(R.string.gps_off);
        }

        if(enableImu) {
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            imuView.setText(R.string.imu_on);

        }
        else {
            imuView.setText(R.string.imu_off);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        final Intent intent = new Intent(this, NodeMainExecutorService.class);

        intent.setAction(NodeMainExecutorService.ACTION_START);
        intent.putExtra(NodeMainExecutorService.EXTRA_NOTIFICATION_TICKER, getString(R.string.app_name));
        intent.putExtra(NodeMainExecutorService.EXTRA_NOTIFICATION_TITLE, getString(R.string.app_name));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        if (!bindService(intent, nodeMainExecutorServiceConnection, BIND_AUTO_CREATE)) {
            Toast.makeText(this, "Failed to bind NodeMainExecutorService.", Toast.LENGTH_LONG).show();
        }
    }

    private void enableLocationSettings() {
        Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(settingsIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(nodeMainExecutorServiceConnection);
        final Intent intent = new Intent(this, NodeMainExecutorService.class);
        stopService(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == MASTER_CHOOSER_REQUEST_CODE) {
                final String host;
                final String networkInterfaceName = data.getStringExtra("ROS_MASTER_NETWORK_INTERFACE");
                // Handles the default selection and prevents possible errors
                if (TextUtils.isEmpty(networkInterfaceName)) {
                    host = InetAddressFactory.newNonLoopback().getHostAddress();
                } else {
                    try {
                        final NetworkInterface networkInterface = NetworkInterface.getByName(networkInterfaceName);
                        host = InetAddressFactory.newNonLoopbackForNetworkInterface(networkInterface).getHostAddress();
                    } catch (final SocketException e) {
                        throw new RosRuntimeException(e);
                    }
                }
                nodeMainExecutorService.setRosHostname(host);
                if (data.getBooleanExtra("ROS_MASTER_CREATE_NEW", false)) {
                    nodeMainExecutorService.startMaster(data.getBooleanExtra("ROS_MASTER_PRIVATE", true));
                } else {
                    final URI uri;
                    try {
                        uri = new URI(data.getStringExtra("ROS_MASTER_URI"));
                    } catch (final URISyntaxException e) {
                        throw new RosRuntimeException(e);
                    }
                    nodeMainExecutorService.setMasterUri(uri);
                }
                // Run init() in a new thread as a convenience since it often requires network access.
                new Thread(() -> init(nodeMainExecutorService)).start();
            } else {
                // Without a master URI configured, we are in an unusable state.
                nodeMainExecutorService.forceShutdown();
            }
        }
    }

    /** Gets the main executor. */
    public LiveData<NodeMainExecutor> getNodeMainExec() {
        return nodeMainExecutorMutableLiveData;
    }

    /** Gets the node configuration. */
    public LiveData<NodeConfiguration> getNodeConfig() {
        return nodeConfigurationMutableLiveData;
    }


    protected void init(NodeMainExecutor nodeMainExecutor) {

        //Network configuration with ROS master
        final NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(
                InetAddressFactory.newNonLoopback().getHostAddress()
        );
        nodeConfiguration.setMasterUri(nodeMainExecutorService.getMasterUri());

        Handler mainHandler = new Handler(getMainLooper());
        mainHandler.post(()-> {
                    this.nodeMainExecutorMutableLiveData.setValue(nodeMainExecutor);
                    this.nodeConfigurationMutableLiveData.setValue(nodeConfiguration);
                });
        // Run nodes

        if(enableCamera) {
            CameraNode cameraNode = new CameraNode(this,cameraProviderFuture,nodeName);
            nodeMainExecutor.execute(cameraNode, nodeConfiguration);
        }
        if(enableAudio) {
            AudioNode audioNode = new AudioNode(nodeName);
            nodeMainExecutor.execute(audioNode, nodeConfiguration);

        }
        if(enableGps) {
            GPSNode gpsNode = new GPSNode(this,mLocationManager,nodeName);
            nodeMainExecutor.execute(gpsNode,nodeConfiguration);

        }
        if(enableImu) {
            ImuNode imuNode = new ImuNode(sensorManager, nodeName);
            nodeMainExecutor.execute(imuNode, nodeConfiguration);
        }
    }

    
    @SuppressWarnings("NonStaticInnerClassInSecureContext")
    private final class NodeMainExecutorServiceConnection implements ServiceConnection {

        private final URI customMasterUri;

        public NodeMainExecutorServiceConnection(final URI customUri) {
            customMasterUri = customUri;
        }

        @Override
        public void onServiceConnected(final ComponentName name, final IBinder binder) {
            nodeMainExecutorService = ((NodeMainExecutorService.LocalBinder) binder).getService();

            if (customMasterUri != null) {
                nodeMainExecutorService.setMasterUri(customMasterUri);
                final String host = InetAddressFactory.newNonLoopback().getHostAddress();
                nodeMainExecutorService.setRosHostname(host);
            }
            nodeMainExecutorService.addListener(executorService -> {
                // We may have added multiple shutdown listeners and we only want to
                // call finish() once.
                if (!isFinishing()) {
                    finish();
                }
            });
            if (nodeMainExecutorService.getMasterUri() == null) {

                startActivityForResult(
                        new Intent(MainActivity.this, MasterChooser.class),
                        MASTER_CHOOSER_REQUEST_CODE
                );

            } else {
                init(nodeMainExecutorService);
            }
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            Toast.makeText(MainActivity.this, "Service disconnected", Toast.LENGTH_LONG).show();
        }
    }
}