package io.trackrr.demo.androidsimpletracker;

import android.Manifest;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttMessageDeliveryCallback;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jp.co.stylez.gps.sdk.Gps;
import jp.co.stylez.gps.sdk.android.GpsAndroidClientManager;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TrackerActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    static final String LOG_TAG = TrackerActivity.class.getCanonicalName();

    static final String PREF_KEY_GPS_KEY = "PREF_KEY_GPS_KEY";
    static final String PREF_KEY_SECRET_KEY = "PREF_KEY_SECRET_KEY";

    SharedPreferences sharedPreferences;

    EditText txtGpsKey;
    EditText txtSecretKey;

    TextView txtLatitude;
    TextView txtLongitude;

    Button btnConnect;
    Button btnImsi;
    Button btnPublish;

    /**
     * Provides the entry point to Trackrr.IO SDK services.
     */
    GpsAndroidClientManager client;

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient googleApiClient;

    /**
     * Represents a geographical location.
     */
    protected Location lastLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker);

        txtGpsKey = (EditText) findViewById(R.id.txtGpsKey);
        txtSecretKey = (EditText) findViewById(R.id.txtSecretKey);

        txtLatitude = (TextView) findViewById(R.id.txtLatitude);
        txtLongitude = (TextView) findViewById(R.id.txtLongitude);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        txtGpsKey.setText(sharedPreferences.getString(PREF_KEY_GPS_KEY, ""));
        txtSecretKey.setText(sharedPreferences.getString(PREF_KEY_SECRET_KEY, ""));

        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(connectClick);

        btnImsi = (Button) findViewById(R.id.btnImsi);
        btnImsi.setOnClickListener(imsiClick);

        btnPublish = (Button) findViewById(R.id.btnPublish);
        btnPublish.setOnClickListener(publishClick);
        btnPublish.setEnabled(false);

        buildGoogleApiClient();
    }

    /**
     * Builds a GoogleApiClient. Uses the addApi() method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (lastLocation != null) {
            txtLatitude.setText(String.valueOf(lastLocation.getLatitude()));
            txtLongitude.setText(String.valueOf(lastLocation.getLongitude()));
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(LOG_TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(LOG_TAG, "Connection suspended");
        googleApiClient.connect();
    }

    View.OnClickListener connectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final String gpsKey = txtGpsKey.getText().toString();
            final String secret = txtSecretKey.getText().toString();

            if (gpsKey.equals("") || secret.equals("")) {
                new AlertDialog.Builder(TrackerActivity.this)
                        .setTitle(getResources().getString(R.string.alert_title_error))
                        .setMessage(getResources().getString(R.string.alert_message_require_error))
                        .setPositiveButton(getResources().getString(R.string.alert_ok), null)
                        .show();
                return;
            }

            // Save GPS_KEY, SECRET_KEY
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(PREF_KEY_GPS_KEY, gpsKey);
            editor.putString(PREF_KEY_SECRET_KEY, secret);
            editor.commit();

            client = new GpsAndroidClientManager(getApplicationContext(), gpsKey, secret);
            btnConnect.setText(getResources().getString(R.string.connecting));
            btnConnect.setEnabled(false);
            try {
                client.connect(new AWSIotMqttClientStatusCallback() {
                    @Override
                    public void onStatusChanged(final AWSIotMqttClientStatus status, final Throwable throwable) {
                        statusChangedForConnect(status);
                    }
                });
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }
    };

    View.OnClickListener publishClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final double latitude = lastLocation.getLatitude();
            final double longitude = lastLocation.getLongitude();

            final Gps gps = new Gps();
            final Map<String, String> userData = new HashMap<>();
            gps.setCreatedDate(System.currentTimeMillis());
            gps.setUserData(userData);
            final Gps.Coords coords = new Gps.Coords();
            coords.setLatitude(latitude);
            coords.setLongitude(longitude);
            gps.setCoords(coords);

            btnPublish.setText(getResources().getString(R.string.publishing));
            btnPublish.setEnabled(false);
            client.publish(gps, new AWSIotMqttMessageDeliveryCallback() {
                @Override
                public void statusChanged(final MessageDeliveryStatus status, final Object userData) {
                    statusChangedForPublish(status);
                }
            });
        }
    };

    View.OnClickListener imsiClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            OkHttpClient httpClient = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("http://metadata.soracom.io/v1/subscriber")
                    .build();
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(Call call, final Response response) throws IOException {
                    try {
                        String responseData = response.body().string();
                        JSONObject json = new JSONObject(responseData);
                        final String imsi = json.getString("imsi");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                txtGpsKey.setText(imsi);
                            }
                        });
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, "OkHttpClient response: JSONException Message = " + e.getMessage());
                    }
                }
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(LOG_TAG, "OkHttpClient failed: Exception Message = " + e.getMessage());
                }
            });
        }
    };

    protected void statusChangedForConnect(final AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (status == AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Connected) {
                    btnConnect.setText(getResources().getString(R.string.connected));
                    btnConnect.setEnabled(false);
                    btnPublish.setEnabled(true);
                }
                if (status == AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.ConnectionLost) {
                    btnConnect.setText(getResources().getString(R.string.connect));
                    btnConnect.setEnabled(true);
                    btnPublish.setEnabled(false);
                }
            }
        });
    }

    protected void statusChangedForPublish(final AWSIotMqttMessageDeliveryCallback.MessageDeliveryStatus status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (status == AWSIotMqttMessageDeliveryCallback.MessageDeliveryStatus.Success) {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.published), Toast.LENGTH_LONG).show();
                }
                if (status == AWSIotMqttMessageDeliveryCallback.MessageDeliveryStatus.Fail) {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.alert_message_publish_fail), Toast.LENGTH_LONG).show();
                }
                btnPublish.setText(getResources().getString(R.string.publish));
                btnPublish.setEnabled(true);
            }
        });
    }

}
