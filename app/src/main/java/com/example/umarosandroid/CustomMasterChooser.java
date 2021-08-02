/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.example.umarosandroid;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import org.ros.exception.RosRuntimeException;
import org.ros.internal.node.client.MasterClient;
import org.ros.internal.node.xmlrpc.XmlRpcTimeoutException;
import org.ros.namespace.GraphName;
import org.ros.node.NodeConfiguration;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Allows the user to configue a master {@link URI} then it returns that
 * {@link URI} to the calling {@link Activity}.
 * <p>
 * When this {@link Activity} is started, the last used (or the default)
 * {@link URI} is displayed to the user.
 *
 * @author ethan.rublee@gmail.com (Ethan Rublee)
 * @author damonkohler@google.com (Damon Kohler)
 * @author munjaldesai@google.com (Munjal Desai)
 * @edit germanruizmudarra@gmail.com (Germán Ruiz-Mudarra)
 */
public class CustomMasterChooser extends AppCompatActivity {

    /**
     * The key with which the last used {@link URI} will be stored as a
     * preference.
     */
    private static final String PREFS_KEY_NAME = "URI_KEY";

    /**
     * Lookup text for catching a ConnectionException when attempting to
     * connect to a master.
     */
    private static final String CONNECTION_EXCEPTION_TEXT = "ECONNREFUSED";

    /**
     * Lookup text for catching a UnknownHostException when attemping to
     * connect to a master.
     */
    private static final String UNKNOW_HOST_TEXT = "UnknownHost";

    /**
     * Default port number for master URI. Appended if the URI does not
     * contain a port number.
     */
    private static final int DEFAULT_PORT = 11311;

    /**
     *The preferences key used for obtaining the number of recent Master URIs.
     */
    private static final String RECENT_COUNT_KEY_NAME = "RECENT_MASTER_URI_COUNT";

    /**
     * The preference key prefix used for obtaining the recent Master URIs.
     */
    private static final String RECENT_PREFIX_KEY_NAME = "RECENT_MASTER_URI_";

    /**
     * Number of recent Master URIs to store into preferences.
     */
    private static final int RECENT_MASTER_HISTORY_COUNT = 5;

    public static final String MASTER_URI = "com.example.umarosandroid.MASTER_URI";

    public static final String NODE_NAME = "com.example.umarosandroid.NODE_NAME";

    public static final String ENABLE_CAMERA = "com.example.umarosandroid.ENABLE_CAMERA";
    public static final String ENABLE_AUDIO = "com.example.umarosandroid.ENABLE_AUDIO";
    public static final String ENABLE_GPS = "com.example.umarosandroid.ENABLE_GPS";
    public static final String ENABLE_IMU = "com.example.umarosandroid.ENABLE_IMU";

    public static final String ENABLE_NLP = "com.example.umarosandroid.ENABLE_NLP";

    private AutoCompleteTextView uriText;

    EditText nodeNameEdit;

    Switch cameraSwitch;
    Switch audioSwitch;
    Switch gpsSwitch;
    Switch imuSwitch;

    Switch nlpSwitch;

    String nodeName_s = "";

    boolean enableCamera;
    boolean enableAudio;
    boolean enableGps;
    boolean enableImu;
    boolean enableNlp;

    private Button connectButton;
    private LinearLayout connectionLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The request code used in ActivityCompat.requestPermissions()
        // and returned in the Activity's onRequestPermissionsResult()
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        setContentView(R.layout.activity_setup);
        final Pattern uriPattern = RosURIPattern.URI;

        uriText = (AutoCompleteTextView) findViewById(R.id.MasterUri);
        uriText.setThreshold(RosURIPattern.HTTP_PROTOCOL_LENGTH);

        nodeNameEdit = (EditText) findViewById(R.id.NodeName);

        cameraSwitch = (Switch) findViewById(R.id.CameraSwitch);
        audioSwitch = (Switch) findViewById(R.id.AudioSwitch);
        gpsSwitch = (Switch) findViewById(R.id.GPSSwitch);
        imuSwitch = (Switch) findViewById(R.id.IMUSwitch);
        nlpSwitch = (Switch) findViewById(R.id.NLPSwitch);

        connectButton = (Button) findViewById(R.id.createorconnect);
        connectionLayout = (LinearLayout) findViewById(R.id.connection_layout);


        ArrayAdapter<String> uriAdapter = new ArrayAdapter<>
                (this,android.R.layout.select_dialog_item,getRecentMasterURIs());
        uriText.setAdapter(uriAdapter);

        uriText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final String uri = s.toString();
                if(!uriPattern.matcher(uri).matches()) {
                    uriText.setError("Please enter valid URI");
                    connectButton.setEnabled(false);
                }
                else {
                    uriText.setError(null);
                    connectButton.setEnabled(true);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Get the URI from preferences and display it. Since only primitive types
        // can be saved in preferences the URI is stored as a string.
        String uri =
                getPreferences(MODE_PRIVATE).getString(PREFS_KEY_NAME,
                        NodeConfiguration.DEFAULT_MASTER_URI.toString());
        uriText.setText(uri);

        cameraSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableCamera = isChecked;
                System.out.println("Camera toggled");

            }
        });
        audioSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableAudio = isChecked;
                System.out.println("Audio toggled");

            }
        });
        gpsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableGps = isChecked;
                System.out.println("GPS toggled");

            }
        });
        imuSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableImu = isChecked;
                System.out.println("IMU toggled");
            }
        });
        nlpSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableNlp = isChecked;
                System.out.println("NLP toggled");
            }
        });

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String tmpURI = uriText.getText().toString();

                // Check to see if the URI has a port.
                final Pattern portPattern = RosURIPattern.PORT;
                if(!portPattern.matcher(tmpURI).find()) {
                    // Append the default port to the URI and update the TextView.
                    tmpURI = String.format(Locale.getDefault(),"%s:%d/",tmpURI,DEFAULT_PORT);
                    uriText.setText(tmpURI);
                }

                // Set the URI for connection.
                final String uri = tmpURI;

                // Prevent further edits while we verify the URI.
                // Note: This was placed after the URI port check due to odd behavior
                // with setting the connectButton to disabled.
                uriText.setEnabled(false);
                connectButton.setEnabled(false);

                // Make sure the URI can be parsed correctly and that the master is
                // reachable.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectionLayout.setVisibility(View.VISIBLE);
                        boolean result;

                        try {
                            MasterClient masterClient = new MasterClient(new URI(uri));
                            masterClient.getUri(GraphName.of("android/master_chooser_activity"));
                            toast("Connected!");
                            result = true;
                        } catch (URISyntaxException e) {
                            toast("Invalid URI.");
                            result = false;
                        } catch (XmlRpcTimeoutException e) {
                            toast("Master unreachable!");
                            result = false;
                        }
                        catch (Exception e) {
                            String exceptionMessage = e.getMessage();
                            if(exceptionMessage.contains(CONNECTION_EXCEPTION_TEXT))
                                toast("Unable to communicate with master!");
                            else if(exceptionMessage.contains(UNKNOW_HOST_TEXT))
                                toast("Unable to resolve URI hostname!");
                            else
                                toast("Communication error!");
                            result = false;
                        }

                        connectionLayout.setVisibility(View.GONE);

                        if (result) {
                            //Update Recent Master URI
                            addRecentMasterURI(uri);
                            // If the displayed URI is valid then pack that into the intent.
                            // Package the intent to be consumed by the calling activity.
                            Intent mIntent = new Intent(CustomMasterChooser.this, MainActivity.class);
                            mIntent.putExtra(MASTER_URI,uriText.getText().toString());
                            mIntent.putExtra(NODE_NAME,nodeNameEdit.getText().toString());
                            mIntent.putExtra(ENABLE_CAMERA,enableCamera);
                            mIntent.putExtra(ENABLE_AUDIO,enableAudio);
                            mIntent.putExtra(ENABLE_GPS,enableGps);
                            mIntent.putExtra(ENABLE_IMU,enableImu);
                            mIntent.putExtra(ENABLE_NLP,enableNlp);
                            startActivity(mIntent);
                            setResult(RESULT_OK, mIntent);

                        } else {
                            connectButton.setEnabled(true);
                            uriText.setEnabled(true);
                        }
                    }
                });
            }
        });

    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        //Prevent user from going back to Launcher Activity since no Master is connected.
        this.moveTaskToBack(true);
    }

    protected void toast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CustomMasterChooser.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }






    /**
     * Adds the given URI to the list of recent Master URIs stored in shared preferences.
     * This implementation does not use
     * {@link android.content.SharedPreferences.Editor#putStringSet(String, Set)}
     * since it is not available in API 10.
     * @param uri Master URI string to store.
     */
    private void addRecentMasterURI(String uri) {
        List<String> recentURIs = getRecentMasterURIs();
        if (!recentURIs.contains(uri)) {
            recentURIs.add(0, uri);
            if (recentURIs.size() > RECENT_MASTER_HISTORY_COUNT)
                recentURIs = recentURIs.subList(0, RECENT_MASTER_HISTORY_COUNT);
        }

        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        editor.putString(PREFS_KEY_NAME, uri);
        for (int i = 0; i < recentURIs.size(); i++) {
            editor.putString(RECENT_PREFIX_KEY_NAME + String.valueOf(i), recentURIs.get(i));
        }

        editor.putInt(RECENT_COUNT_KEY_NAME, recentURIs.size());
        editor.apply();
    }

    /**
     * Gets a list of recent Master URIs from shared preferences. This implementation does not use
     * {@link android.content.SharedPreferences.Editor#putStringSet(String, Set)}
     * since it is not available in API 10.
     * @return List of recent Master URI strings
     */
    private List<String> getRecentMasterURIs() {
        List<String> recentURIs;
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        int numRecent = prefs.getInt(RECENT_COUNT_KEY_NAME, 0);
        recentURIs = new ArrayList<>(numRecent);
        for (int i = 0; i < numRecent; i++) {
            String uri = prefs.getString(RECENT_PREFIX_KEY_NAME + String.valueOf(i), "");
            if (!uri.isEmpty()) {
                recentURIs.add(uri);
            }
        }

        return recentURIs;
    }

    /**
     * Regular expressions used with ROS URIs.
     *
     * The majority of the expressions and variables were copied from
     * {@link android.util.Patterns}. The {@link android.util.Patterns} class could not be
     * utilized because the PROTOCOL regex included other web protocols besides http. The
     * http protocol is required by ROS.
     */
    private static class RosURIPattern
    {
        /* A word boundary or end of input.  This is to stop foo.sure from matching as foo.su */
        private static final String WORD_BOUNDARY = "(?:\\b|$|^)";

        /**
         * Valid UCS characters defined in RFC 3987. Excludes space characters.
         */
        private static final String UCS_CHAR = "[" +
                "\u00A0-\uD7FF" +
                "\uF900-\uFDCF" +
                "\uFDF0-\uFFEF" +
                "\uD800\uDC00-\uD83F\uDFFD" +
                "\uD840\uDC00-\uD87F\uDFFD" +
                "\uD880\uDC00-\uD8BF\uDFFD" +
                "\uD8C0\uDC00-\uD8FF\uDFFD" +
                "\uD900\uDC00-\uD93F\uDFFD" +
                "\uD940\uDC00-\uD97F\uDFFD" +
                "\uD980\uDC00-\uD9BF\uDFFD" +
                "\uD9C0\uDC00-\uD9FF\uDFFD" +
                "\uDA00\uDC00-\uDA3F\uDFFD" +
                "\uDA40\uDC00-\uDA7F\uDFFD" +
                "\uDA80\uDC00-\uDABF\uDFFD" +
                "\uDAC0\uDC00-\uDAFF\uDFFD" +
                "\uDB00\uDC00-\uDB3F\uDFFD" +
                "\uDB44\uDC00-\uDB7F\uDFFD" +
                "&&[^\u00A0[\u2000-\u200A]\u2028\u2029\u202F\u3000]]";

        /**
         * Valid characters for IRI label defined in RFC 3987.
         */
        private static final String LABEL_CHAR = "a-zA-Z0-9" + UCS_CHAR;

        /**
         * RFC 1035 Section 2.3.4 limits the labels to a maximum 63 octets.
         */
        private static final String IRI_LABEL =
                "[" + LABEL_CHAR + "](?:[" + LABEL_CHAR + "\\-]{0,61}[" + LABEL_CHAR + "]){0,1}";

        private static final Pattern IP_ADDRESS
                = Pattern.compile(
                "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                        + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                        + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                        + "|[1-9][0-9]|[0-9]))");

        /**
         * Regular expression that matches domain names without a TLD
         */
        private static final String RELAXED_DOMAIN_NAME =
                "(?:" + "(?:" + IRI_LABEL + "(?:\\.(?=\\S))" +"?)+" +
                        "|" + IP_ADDRESS + ")";

        private static final String HTTP_PROTOCOL = "(?i:http):\\/\\/";

        public static final int HTTP_PROTOCOL_LENGTH = ("http://").length();

        private static final String PORT_NUMBER = "\\:\\d{1,5}\\/?";

        /**
         *  Regular expression pattern to match valid rosmaster URIs.
         *  This assumes the port number and trailing "/" will be auto
         *  populated (default port: 11311) if left out.
         */
        public static final Pattern URI = Pattern.compile("("
                + WORD_BOUNDARY
                + "(?:"
                + "(?:" + HTTP_PROTOCOL + ")"
                + "(?:" + RELAXED_DOMAIN_NAME + ")"
                + "(?:" + PORT_NUMBER + ")?"
                + ")"
                + WORD_BOUNDARY
                + ")");

        public static final Pattern PORT = Pattern.compile(PORT_NUMBER);
    }
}