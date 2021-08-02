# UMA-ROS-Android
This app makes a smartphone able to publish data from its sensors to
other devices via ROS1 for monitoring purposes or to apply analysis algorithms based on Edge/Cloud Computing methods.

The target version of the app is Android 10 (properly tested), but should be compatible with versions greater than 6.0.

Current supported sensors are:
- Camera (using cameraX)
- Microphone (using AudioRecord)
- GPS (LocationManager)
- IMU (SensorManager)

The code of two latter are based in a previous solution called [android_sensors_driver](https://github.com/ros-android/android_sensors_driver),  
slightly modified for integration with the rest of the app.

Audio captured is published and also saved as a WAV file in the internal storage for offline analysis.

### Installation
The installation is made through Android Studio IDE. Since rosjava supports Gradle and Maven repositories, there's no
need for downloading any other dependencies manually. Everything will be automatically installed once you open and build the project
in Android Studio (Shift+F9).

To install the app, make sure that the desired SDK version is installed and run the app (Shift+F10), which will install it in your device.

### Configuration
The configuration screen to connect to the ROS network is based in the [default Master Chooser](https://github.com/rosjava/android_core/blob/kinetic/android_core_components/src/org/ros/android/MasterChooser.java)  provided with rosjava. This version is a modified one where the commonly not used configuration parameters were scraped. Instead, now there are some switches for the activation of the different sensors and a text box is available to give the smartphone a identifier.

This makes the execution of the app in multiple smartphones simultaneously in the same ROS network.

<img src="figs/custom_master_chooser.jpg" alt="Custom Master Chooser" width="300" />

### Running
Once the connection with the ROS Master is established the Main Activity is executed, where all the nodes checked with the switches run and publish the data.
The GUI only shows information about the sensors running since our use case was the coupling of the smartphone to a robot, with no interaction from the user needed (other than configuration).

<img src="figs/main_activity.jpg" alt="Custom Master Chooser" width="300" />

Topics in this case:
```
/android0/camera/compressed
/android0/camera/camera_info
/android0/audio
/android0/fix
/android0/IMU
```

Previous versions of the app showed a preview view of the camera in the Main Activity GUI. This can be easily reimplemented thanks to cameraX.

### How to publish more sensors

