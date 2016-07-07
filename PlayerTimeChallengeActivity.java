package se.andreasmikaelsson.dungeonswap;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Iterator;

public class PlayerTimeChallengeActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String REQUEST_JOIN_CHALLENGE = "join";
    private String SERVICE_NAME = "Client Device";
    private String SERVICE_TYPE = "_dswap._tcp.";
    private InetAddress hostAddress;
    private int hostPort;
    private NsdManager mNsdManager;
    private int SocketServerPort = 6000;
    private static final String REQUEST_CONNECT_CLIENT = "request-connect-client-challenge";
    private static final String TAG = "NSDClient";
    private EditText playerNameView;
    private String playerName;
    private Button joinChallengeButton;
    private TextView phaseNameStartedChallenge;
    private TextView phaseTimer;
    private JSONObject jsondataFromThread;


    public void showToast(final String toast){
        PlayerTimeChallengeActivity.this.runOnUiThread(new Runnable(){
            public void run(){
                Toast.makeText(PlayerTimeChallengeActivity.this,toast,Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_time_challenge);

        joinChallengeButton = (Button) findViewById(R.id.player_button_join_time_challenge);
        joinChallengeButton.setOnClickListener(this);
        phaseNameStartedChallenge = (TextView) findViewById(R.id.player_time_challenge_status_name);
        phaseTimer = (TextView) findViewById(R.id.player_time_challenge_status_time);

        try {
            if (mNsdManager != null) {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "onCreate: No discovery listener to unregister.");
            showToast("onCreate: No discovery listener to unregister.");
        }

        mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        try {
            mNsdManager.discoverServices(SERVICE_TYPE,
                    NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Discover listener already in use.");
            showToast("onCreate: Discover listener already in use.");
        }
    }

    NsdManager.DiscoveryListener mDiscoveryListener = new NsdManager.DiscoveryListener() {

        // Called as soon as service discovery begins.
        @Override
        public void onDiscoveryStarted(String regType) {
            Log.d(TAG, "Service discovery started");
        }

        @Override
        public void onServiceFound(NsdServiceInfo service) {
            // A service was found! Do something with it.
            Log.d(TAG, "Service discovery success : " + service);
            Log.d(TAG, "Host = "+ service.getServiceName());
            Log.d(TAG, "port = " + String.valueOf(service.getPort()));

            if (!service.getServiceType().equals(SERVICE_TYPE)) {
                // Service type is the string containing the protocol and
                // transport layer for this service.
                Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
            } else if (service.getServiceName().equals(SERVICE_NAME)) {
                // The name of the service tells the user what they'd be
                // connecting to. It could be "Bob's Chat App".
                Log.d(TAG, "Same machine: " + SERVICE_NAME);
                showToast("Error: This device connected to this device!");
            } else {
                Log.d(TAG, "Diff Machine : " + service.getServiceName());
                // connect to the service and obtain serviceInfo
                showToast("Service found!");
                mNsdManager.resolveService(service, mResolveListener);
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo service) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            Log.e(TAG, "service lost" + service);
            showToast("Service lost!");
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.i(TAG, "Discovery stopped: " + serviceType);
            showToast("Discovery stopped!");
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            showToast("Start discovery failed!");
            mNsdManager.stopServiceDiscovery(this);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            showToast("Stop discovery failed!");
            mNsdManager.stopServiceDiscovery(this);
        }
    };

    NsdManager.ResolveListener mResolveListener = new NsdManager.ResolveListener() {

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.d(TAG, "Resolve Succeeded. " + serviceInfo);

            if (serviceInfo.getServiceName().equals(SERVICE_NAME)) {
                Log.d(TAG, "Same IP.");
                return;
            }

            // Obtain port and IP
            hostPort = serviceInfo.getPort();
            hostAddress = serviceInfo.getHost();

            /* Once the client device resolves the service and obtains
             * server's ip address, connect to the server and send data
             */

            connectToHost();
        }


        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            // Called when the resolve fails. Use the error code to debug.
            Log.e(TAG, "Resolve failed " + errorCode);
            Log.e(TAG, "serivce = " + serviceInfo);
        }
    };

    private void connectToHost() {

        if (hostAddress == null) {
            Log.e(TAG, "Host Address is null");
            return;
        }

        String ipAddress = getLocalIpAddress();
        JSONObject jsonData = new JSONObject();

        try {
            jsonData.put("request", REQUEST_CONNECT_CLIENT);
            jsonData.put("ipAddress", ipAddress);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "can't put request");
            return;
        }

        new SocketServerTask().execute(jsonData);
    }

    private String getLocalIpAddress() {
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        return ip;
    }

    private void joinChallenge() {
        playerNameView = (EditText) findViewById(R.id.player_time_challenge_player_name);
        if (!playerNameView.getEditableText().toString().isEmpty()) {
            playerName = playerNameView.getEditableText().toString();
        }else {
            playerName = "";
        }
        JSONObject jsonData = new JSONObject();

        try {
            jsonData.put("request", REQUEST_JOIN_CHALLENGE);
            jsonData.put("playerName", playerName);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "can't put request");
            return;
        }
        new  SocketServerTask().execute(jsonData);
    }

    private void startTimeChallenge(JSONObject jsondata) {

        jsondataFromThread = jsondata;

        PlayerTimeChallengeActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int time = 10000; //default value for timer 10 s
                phaseNameStartedChallenge.setVisibility(View.VISIBLE);
                phaseTimer.setVisibility(View.VISIBLE);

                Iterator<String> iter = jsondataFromThread.keys();
                while (iter.hasNext()) {
                    String key = iter.next();
                    Object value = null;
                    try {
                        value = jsondataFromThread.get(key);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    final String phaseNameString = key;
                    phaseNameStartedChallenge.setText(phaseNameString);

                    try {
                        time = Integer.valueOf(value.toString()) * 1000;
                    } catch (Exception e) {
                        Log.e(TAG, "value couldn't be converted to integer!");
                    }

                    new CountDownTimer(time, 1000) {

                        public void onTick(long millisUntilFinished) {
                            phaseTimer.setText(String.valueOf(millisUntilFinished / 1000));
                        }

                        public void onFinish() {
                            phaseTimer.setText(phaseNameString + " done!");
                        }
                    }.start();
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.player_button_join_time_challenge:
                joinChallenge();
        }
    }

    private class SocketServerTask extends AsyncTask<JSONObject, Void, Void> {
        private JSONObject jsonData;
        private String success;

        @Override
        protected Void doInBackground(JSONObject... params) {
            Socket socket = null;
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;
            jsonData = params[0];

            try {
                // Create a new Socket instance and connect to host
                socket = new Socket(hostAddress, SocketServerPort);

                dataOutputStream = new DataOutputStream(
                        socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());

                // transfer JSONObject as String to the server
                dataOutputStream.writeUTF(jsonData.toString());
                Log.i(TAG, "waiting for response from host");

                // Thread will wait till server replies
                String response = dataInputStream.readUTF();

                if (response != null && response.equals("Connection Accepted")) {
                    success = "true_connection";
                    PlayerTimeChallengeActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            joinChallengeButton.setVisibility(View.VISIBLE);
                        }
                    });
                } else if (response != null && response.equals("You joined")) {
                    success = "true_join";
                    JSONObject jsondataFromJoin = null;

                    dataOutputStream.writeUTF("request phases");

                    // Thread will wait till server replies
                    String responseFromJoin = dataInputStream.readUTF();

                    try {
                        jsondataFromJoin = new JSONObject(responseFromJoin);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    startTimeChallenge(jsondataFromJoin);

                } else {
                    success = "false_connection";
                }

            } catch (IOException e) {
                e.printStackTrace();
                success = "false_connection";
            } finally {

                // close socket
                if (socket != null) {
                    try {
                        Log.i(TAG, "closing the socket");
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // close input stream
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // close output stream
                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            switch (success) {
                case "true_connection":
                    Toast.makeText(PlayerTimeChallengeActivity.this, "Connection Done", Toast.LENGTH_SHORT).show();
                    break;
                case "true_join":
                    Toast.makeText(PlayerTimeChallengeActivity.this, "Join Done", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(PlayerTimeChallengeActivity.this, "Unable to connect", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    protected void onPause() {
        try {
            if (mNsdManager != null) {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "onPause: Failed to unregister discovery listener.");
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (mNsdManager != null) {
                mNsdManager.discoverServices(
                        SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "onResume: Discover listener already in use.");
            showToast("onResume: Discover listener already in use.");
        }
    }

    @Override
    protected void onStop() {
        try {
            if (mNsdManager != null) {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "onStop: Failed to unregister discovery listener.");
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        try {
            if (mNsdManager != null) {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "onDestroy: Failed to unregister discovery listener.");
        }
        super.onDestroy();
    }
}
