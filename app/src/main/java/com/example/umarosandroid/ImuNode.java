package com.example.umarosandroid;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Looper;
import android.os.SystemClock;

import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import java.util.List;

import sensor_msgs.Imu;



/**
 * @author chadrockey@gmail.com (Chad Rockey)
 * @author axelfurlan@gmail.com (Axel Furlan)
 * Edited by: germanruizmudarra@gmail.com (Germán Ruiz-Mudarra)
 */

public class ImuNode implements NodeMain
{

    private ImuThread imuThread;
    private SensorListener sensorListener;
    private SensorManager sensorManager;
    private Publisher<Imu> publisher;
    private String nodeName;

    public ImuNode(SensorManager manager, String nodeName) // Contructor
    {
        this.sensorManager = manager; // Sensor manager instantiated in the main activity
        this.nodeName = nodeName;
    }

    public GraphName getDefaultNodeName()
    {
        return GraphName.of(nodeName+"/imuNode");
    }

    public void onError(Node node, Throwable throwable) {
    }

    public void onStart(ConnectedNode node) {
        try {
            this.publisher = node.newPublisher(nodeName+"/imu", "sensor_msgs/Imu");
            // 	Determine if we have the various needed sensors
            boolean hasAccel = false;
            boolean hasGyro = false;
            boolean hasQuat = false;

            List<Sensor> accelList = this.sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);

            if(accelList.size() > 0)
            {
                hasAccel = true;
            }

            List<Sensor> gyroList = this.sensorManager.getSensorList(Sensor.TYPE_GYROSCOPE);
            if(gyroList.size() > 0)
            {
                hasGyro = true;
            }

            List<Sensor> quatList = this.sensorManager.getSensorList(Sensor.TYPE_ROTATION_VECTOR);
            if(quatList.size() > 0)
            {
                hasQuat = true;
            }

            // Start sensor event listener and ROS publications (imuThread)
            this.sensorListener = new SensorListener(publisher, hasAccel, hasGyro, hasQuat);
            this.imuThread = new ImuThread(this.sensorManager, sensorListener);
            this.imuThread.start();
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
    public void onShutdown(Node arg0)
    {
        this.imuThread.shutdown();

        try
        {
            this.imuThread.join();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    //@Override
    public void onShutdownComplete(Node arg0)
    {
    }

    // Class used for listening and storing sensors raw data
    private class SensorListener implements SensorEventListener
    {

        private Publisher<Imu> publisher;

        private boolean hasAccel;
        private boolean hasGyro;
        private boolean hasQuat;

        private long accelTime;
        private long gyroTime;
        private long quatTime;

        private Imu imu;

        private SensorListener(Publisher<Imu> publisher, boolean hasAccel, boolean hasGyro, boolean hasQuat)
        {
            this.publisher = publisher;
            this.hasAccel = hasAccel;
            this.hasGyro = hasGyro;
            this.hasQuat = hasQuat;
            this.accelTime = 0;
            this.gyroTime = 0;
            this.quatTime = 0;
            this.imu = this.publisher.newMessage();
        }

        //	@Override
        public void onAccuracyChanged(Sensor sensor, int accuracy)
        {
        }

        //	@Override
        public void onSensorChanged(SensorEvent event)
        {
            if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            {
                this.imu.getLinearAcceleration().setX(event.values[0]);
                this.imu.getLinearAcceleration().setY(event.values[1]);
                this.imu.getLinearAcceleration().setZ(event.values[2]);

                double[] tmpCov = {0.01,0,0, 0,0.01,0, 0,0,0.01};// TODO Make Parameter
                this.imu.setLinearAccelerationCovariance(tmpCov);
                this.accelTime = event.timestamp;
            }
            else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
            {
                this.imu.getAngularVelocity().setX(event.values[0]);
                this.imu.getAngularVelocity().setY(event.values[1]);
                this.imu.getAngularVelocity().setZ(event.values[2]);
                double[] tmpCov = {0.0025,0,0, 0,0.0025,0, 0,0,0.0025};// TODO Make Parameter
                this.imu.setAngularVelocityCovariance(tmpCov);
                this.gyroTime = event.timestamp;
            }
            else if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR)
            {
                float[] quaternion = new float[4];
                SensorManager.getQuaternionFromVector(quaternion, event.values);
                this.imu.getOrientation().setW(quaternion[0]);
                this.imu.getOrientation().setX(quaternion[1]);
                this.imu.getOrientation().setY(quaternion[2]);
                this.imu.getOrientation().setZ(quaternion[3]);
                double[] tmpCov = {0.001,0,0, 0,0.001,0, 0,0,0.001};// TODO Make Parameter
                this.imu.setOrientationCovariance(tmpCov);
                this.quatTime = event.timestamp;
            }

            // Currently storing event times in case I filter them in the future.  Otherwise they are used to determine if all sensors have reported.
            if((this.accelTime != 0 || !this.hasAccel) && (this.gyroTime != 0 || !this.hasGyro) && (this.quatTime != 0 || !this.hasQuat))
            {
                // Convert event.timestamp (nanoseconds uptime) into system time, use that as the header stamp
                long time_delta_millis = System.currentTimeMillis() - SystemClock.uptimeMillis();
                this.imu.getHeader().setStamp(Time.fromMillis(time_delta_millis + event.timestamp/1000000));
                this.imu.getHeader().setFrameId(nodeName+"/imu");// TODO Make parameter

                publisher.publish(this.imu);

                // Create a new message
                this.imu = this.publisher.newMessage();

                // Reset times
                this.accelTime = 0;
                this.gyroTime = 0;
                this.quatTime = 0;
            }
        }
    }

    private class ImuThread extends Thread
    {
        private final SensorManager sensorManager;
        private SensorListener sensorListener;
        private Looper threadLooper;

        private final Sensor accelSensor;
        private final Sensor gyroSensor;
        private final Sensor quatSensor;

        private ImuThread(SensorManager sensorManager, SensorListener sensorListener)
        {
            this.sensorManager = sensorManager;
            this.sensorListener = sensorListener;
            this.accelSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            this.gyroSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            this.quatSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }


        public void run()
        {
            Looper.prepare();
            this.threadLooper = Looper.myLooper();
            this.sensorManager.registerListener(this.sensorListener, this.accelSensor, SensorManager.SENSOR_DELAY_FASTEST);
            this.sensorManager.registerListener(this.sensorListener, this.gyroSensor, SensorManager.SENSOR_DELAY_FASTEST);
            this.sensorManager.registerListener(this.sensorListener, this.quatSensor, SensorManager.SENSOR_DELAY_FASTEST);
            Looper.loop();
        }


        public void shutdown()
        {
            this.sensorManager.unregisterListener(this.sensorListener);
            if(this.threadLooper != null)
            {
                this.threadLooper.quit();
            }
        }
    }
}
