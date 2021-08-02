package com.example.umarosandroid;

import android.Manifest;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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


public class MainActivity extends AppCompatActivity {
    private static final int MASTER_CHOOSER_REQUEST_CODE = 0;

    TextView nameView;

    TextView cameraView;
    TextView imuView;
    TextView audioView;
    TextView gpsView;

    boolean enableCamera;
    boolean enableAudio;
    boolean enableGps;
    boolean enableImu;
    boolean enableNlp;


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

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Intent intent = getIntent();
        String masterUri = intent.getStringExtra(CustomMasterChooser.MASTER_URI);

        nodeName = intent.getStringExtra(CustomMasterChooser.NODE_NAME);

        enableCamera = intent.getBooleanExtra(CustomMasterChooser.ENABLE_CAMERA,false);
        enableAudio = intent.getBooleanExtra(CustomMasterChooser.ENABLE_AUDIO,false);
        enableGps= intent.getBooleanExtra(CustomMasterChooser.ENABLE_GPS,false);
        enableImu = intent.getBooleanExtra(CustomMasterChooser.ENABLE_IMU,false);
        enableNlp = intent.getBooleanExtra(CustomMasterChooser.ENABLE_NLP,false);

        System.out.println(masterUri);
        URI customUri = null;
        try {
            customUri = new URI(masterUri);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        nameView = (TextView) findViewById(R.id.nameText);
        nameView.setText("Name: "+ nodeName);
        cameraView = (TextView) findViewById(R.id.cameraText);
        imuView = (TextView) findViewById(R.id.ImuText);
        audioView = (TextView) findViewById(R.id.audioText);
        gpsView = (TextView) findViewById(R.id.GPSText);

        nodeMainExecutorServiceConnection = new NodeMainExecutorServiceConnection(customUri);


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

    protected void init(NodeMainExecutor nodeMainExecutor) {

        //Network configuration with ROS master
        final NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(
                InetAddressFactory.newNonLoopback().getHostAddress()
        );
        nodeConfiguration.setMasterUri(nodeMainExecutorService.getMasterUri());

        // Run nodes
        if(enableCamera) {
            CameraNode cameraNode = new CameraNode(this,cameraProviderFuture,nodeName);
            nodeMainExecutor.execute(cameraNode, nodeConfiguration);
        }
        if(enableAudio) {
            AudioNode audioNode = new AudioNode(nodeName);
            nodeMainExecutor.execute(audioNode,nodeConfiguration);

            NLPNode nlpNode = new NLPNode(nodeName,enableNlp);
            nodeMainExecutor.execute(nlpNode,nodeConfiguration);
        }
        if(enableGps) {
            GPSNode gpsNode = new GPSNode(this,mLocationManager,nodeName);
            nodeMainExecutor.execute(gpsNode,nodeConfiguration);

        }
        if(enableImu) {
            ImuNode imuNode = new ImuNode(sensorManager,nodeName);
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