package com.example.umarosandroid;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Looper;

import androidx.core.app.ActivityCompat;

import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.message.Time;

import sensor_msgs.NavSatFix;
import sensor_msgs.NavSatStatus;

import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

/**
 * @author chadrockey@gmail.com (Chad Rockey)
 * @author axelfurlan@gmail.com (Axel Furlan)
 * @edit germanruizmudarra@gmail.com (Germán Ruiz-Mudarra)
 */
public class GPSNode implements NodeMain {
    private final Context context;

    private NavSatThread navSatThread;
    private final LocationManager locationManager;
    private NavSatListener navSatFixListener;
    private Publisher<NavSatFix> publisher;
    private String nodeName;

    public GPSNode(Context context, LocationManager manager, String nodeName) {
        this.context = context;
        this.locationManager = manager;
        this.nodeName = nodeName;
    }

    private class NavSatThread extends Thread {
        LocationManager locationManager;
        NavSatListener navSatListener;
        private Looper threadLooper;
        private final Context context;


        private NavSatThread(LocationManager locationManager, NavSatListener navSatListener, Context context) {
            this.locationManager = locationManager;
            this.navSatListener = navSatListener;
            this.context = context;
        }

        public void run() {
            Looper.prepare();
            threadLooper = Looper.myLooper();
            if (ActivityCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this.navSatListener);
            Looper.loop();
        }

        public void shutdown(){
            this.locationManager.removeUpdates(this.navSatListener);
            if(threadLooper != null){
                threadLooper.quit();
            }
        }
    }

    private class NavSatListener implements LocationListener {

        private Publisher<NavSatFix> publisher;

        private volatile byte currentStatus;

        private NavSatListener(Publisher<NavSatFix> publisher) {
            this.publisher = publisher;
            this.currentStatus = NavSatStatus.STATUS_FIX; // Default to fix until we are told otherwise.
        }

        //	@Override
        public void onLocationChanged(Location location)
        {
            NavSatFix fix = this.publisher.newMessage();
            fix.getHeader().setStamp(Time.fromMillis(System.currentTimeMillis()));
            fix.getHeader().setFrameId(nodeName+"/gps");

            fix.getStatus().setStatus(currentStatus);
            fix.getStatus().setService(NavSatStatus.SERVICE_GPS);

            fix.setLatitude(location.getLatitude());
            fix.setLongitude(location.getLongitude());
            fix.setAltitude(location.getAltitude());
            fix.setPositionCovarianceType(NavSatFix.COVARIANCE_TYPE_APPROXIMATED);
            double deviation = location.getAccuracy();
            double covariance = deviation*deviation;
            double[] tmpCov = {covariance,0,0, 0,covariance,0, 0,0,covariance};
            fix.setPositionCovariance(tmpCov);
            publisher.publish(fix);
            System.out.println("GPS sent");
        }

        //	@Override
        public void onProviderDisabled(String provider) {
        }

        //	@Override
        public void onProviderEnabled(String provider) {
        }

        //	@Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                case LocationProvider.OUT_OF_SERVICE:
                    currentStatus = NavSatStatus.STATUS_NO_FIX;
                    System.out.println(currentStatus);
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    currentStatus = NavSatStatus.STATUS_NO_FIX;
                    break;
                case LocationProvider.AVAILABLE:
                    currentStatus = NavSatStatus.STATUS_FIX;
                    break;
            }
        }
    }


    //@Override
    public void onStart(ConnectedNode node)
    {
        try
        {
            this.publisher = node.newPublisher(nodeName+"/fix", "sensor_msgs/NavSatFix");
            this.navSatFixListener = new NavSatListener(publisher);
            this.navSatThread = new NavSatThread(this.locationManager, this.navSatFixListener, this.context);
            this.navSatThread.start();
        }
        catch (Exception e)
        {
            if (node != null)
            {
                //node.getLog().fatal(e);
            }
            else
            {
                e.printStackTrace();
            }
        }
    }

    //@Override
    public void onShutdown(Node arg0) {
        this.navSatThread.shutdown();
        try {
            this.navSatThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //@Override
    public void onShutdownComplete(Node arg0) {
    }

    public GraphName getDefaultNodeName()
    {
        return GraphName.of(nodeName+"/gpsNode");
    }

    public void onError(Node node, Throwable throwable)
    {
    }

}
