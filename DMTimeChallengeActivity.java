package se.andreasmikaelsson.dungeonswap;

import android.content.Context;
import android.graphics.Color;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DMTimeChallengeActivity extends AppCompatActivity implements View.OnClickListener {

    private String SERVICE_NAME = "Server Device";
    private String SERVICE_TYPE = "_dswap._tcp.";
    private static final String REQUEST_CONNECT_CLIENT = "request-connect-client-challenge";
    private static final String REQUEST_JOIN_CHALLENGE = "join";
    //private SocketServerThread socketServerThread;
    private NsdManager mNsdManager;
    private int SocketServerPort = 6000;
    private List<String> clientIPs;
    private static final String TAG = "NSDServer";
    private Button addPhase;
    private Button createChallenge;
    private Button startChallenge;
    private Map<String, Integer> phaseOrder;
    private EditText phaseName;
    private EditText phaseTime;
    private ImageView numberOne;
    private ImageView numberTwo;
    private TextView phaseNameStartedChallenge;
    private TextView phaseTimer;


    public void showToast(final String toast){
        DMTimeChallengeActivity.this.runOnUiThread(new Runnable(){
            public void run(){
                Toast.makeText(DMTimeChallengeActivity.this,toast,Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dm_time_challenge);

        addPhase = (Button) findViewById(R.id.dm_button_add_phase);
        createChallenge = (Button) findViewById(R.id.dm_button_create_time_challenge);
        startChallenge = (Button) findViewById(R.id.dm_button_start_time_challenge);
        numberOne = (ImageView) findViewById(R.id.dm_time_challenge_image_number_one);
        numberTwo = (ImageView) findViewById(R.id.dm_time_challenge_image_number_two);
        phaseNameStartedChallenge = (TextView) findViewById(R.id.dm_time_challenge_started_text_phase_name);
        phaseTimer = (TextView) findViewById(R.id.dm_time_challenge_started_text_timer);

        addPhase.setOnClickListener(this);
        createChallenge.setOnClickListener(this);
        startChallenge.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.dm_button_add_phase:
                addphase();
                break;
            case R.id.dm_button_create_time_challenge:
                createTimeChallenge();
                break;
            case R.id.dm_button_start_time_challenge:
                startTimeChallenge();
                break;
        }
    }

    private void addphase() {
        phaseOrder = new HashMap<String, Integer>();

        phaseName = (EditText) findViewById(R.id.dm_time_challenge_phase_name);
        phaseTime = (EditText) findViewById(R.id.dm_time_challenge_phase_time);
        TextView nameStatus = (TextView) findViewById(R.id.dm_time_challenge_status_name);
        TextView timeStatus = (TextView) findViewById(R.id.dm_time_challenge_status_time);

        String phaseNameString = phaseName.getText().toString();
        int phaseTimeInt = Integer.valueOf(phaseTime.getText().toString());
        nameStatus.append(phaseNameString + "\n");
        timeStatus.append(String.valueOf(phaseTimeInt) + "\n");

        phaseOrder.put(phaseNameString, phaseTimeInt);

        phaseName.setText("");
        phaseTime.setText("");
    }

    private void createTimeChallenge() {

        createChallenge.setEnabled(false);
        createChallenge.setText("done");
        createChallenge.setBackgroundColor(Color.parseColor("#669900"));
        addPhase.setEnabled(false);

        try {
            if (mNsdManager != null) {
                mNsdManager.unregisterService(mRegistrationListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "onCreate: No service to unregister.");
            showToast("onCreate: No service to unregister.");
        }

        mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        registerService(9000);

        clientIPs = new ArrayList<String>();

        JSONObject jsonData = new JSONObject();

        Set keys = phaseOrder.keySet();

        for (Iterator i = keys.iterator(); i.hasNext(); ) {
            String key = (String) i.next();
            String value = String.valueOf(phaseOrder.get(key));

            try {
                jsonData.put(value, key);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "can't put phaseOrder in jsondata");
            }
        }

        String jsonDataString = jsonData.toString();

        SocketServerThread sendJsonData = new SocketServerThread(jsonDataString);
        sendJsonData.start();
    }

    private void startTimeChallenge() {
        numberOne.setVisibility(View.GONE);
        numberTwo.setVisibility(View.GONE);
        phaseName.setVisibility(View.GONE);
        phaseTime.setVisibility(View.GONE);
        phaseNameStartedChallenge.setVisibility(View.VISIBLE);
        phaseTimer.setVisibility(View.VISIBLE);
        final Handler mHandler = new Handler();

        int time = 10000; //default value for timer 10 s
        Set keys = phaseOrder.keySet();
        for (Iterator i = keys.iterator(); i.hasNext(); ) {
            String key = i.toString();
            String value = String.valueOf(phaseOrder.get(key));
            final String phaseNameString = key;
            phaseNameStartedChallenge.setText(phaseNameString);

            try {
                time = Integer.valueOf(value) * 1000;
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

    public void registerService(int port) {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(SERVICE_NAME);
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setPort(port);
        try {
            mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
        } catch (Exception e) {
            Log.e(TAG, "Registration listener already in use.");
            showToast("Registration listener already in use.");
        }
    }

    NsdManager.RegistrationListener mRegistrationListener = new NsdManager.RegistrationListener() {

        @Override
        public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
            String mServiceName = NsdServiceInfo.getServiceName();
            SERVICE_NAME = mServiceName;
            Log.d(TAG, "Registered name : " + mServiceName);
        }

        @Override
        public void onRegistrationFailed(NsdServiceInfo serviceInfo,
                                         int errorCode) {
            Log.e(TAG, "Registration failed.");
            showToast("Registration failed.");
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
            // Service has been unregistered. This only happens when you
            // call
            // NsdManager.unregisterService() and pass in this listener.
            Log.d(TAG, "Service Unregistered : " + serviceInfo.getServiceName());
            showToast("Service Unregistered");
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo,
                                           int errorCode) {
            Log.e(TAG, "Unregistration failed.");
            showToast("Unregistration failed.");
        }
    };

    private class SocketServerThread extends Thread {

        private String jsonDataPhases;

        public SocketServerThread(String jsonDataString) {
            this.jsonDataPhases = jsonDataString;
        }

        @Override
        public void run() {

            Socket socket = null;
            ServerSocket serverSocket = null;
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;

            int count_joined_players = 0;

            try {
                Log.i(TAG, "Creating server socket");
                serverSocket = new ServerSocket(SocketServerPort);

                Boolean stayInWhileLoop = true;

                while (stayInWhileLoop) {
                    socket = serverSocket.accept();
                    dataInputStream = new DataInputStream(
                            socket.getInputStream());
                    dataOutputStream = new DataOutputStream(
                            socket.getOutputStream());

                    String messageFromClient, messageToClient, request;

                    //If no message sent from client, this code will block the Thread
                    messageFromClient = dataInputStream.readUTF();

                    final JSONObject jsondata;

                    try {
                        jsondata = new JSONObject(messageFromClient);
                        request = jsondata.getString("request");

                        if (request.equals(REQUEST_CONNECT_CLIENT)) {
                            String clientIPAddress = jsondata.getString("ipAddress");

                            // Add client IP to a list
                            clientIPs.add(clientIPAddress);
                            showToast("Accepted");

                            messageToClient = "Connection Accepted";

                            // Important command makes client able to send message
                            dataOutputStream.writeUTF(messageToClient);

                        } else if (request.equals(REQUEST_JOIN_CHALLENGE)) {

                            String mess = jsondata.getString("playerName");
                            showToast(mess + "has joined");

                            count_joined_players++;

                            if (count_joined_players == 1) {
                                DMTimeChallengeActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //Do stuff in UI thread
                                        startChallenge.setVisibility(View.VISIBLE);
                                    }
                                });
                            }

                            //Do stuff with sent mess from player
                            if (mess != null) {
                                messageToClient = "You joined";
                            } else {
                                messageToClient = "Message null";
                            }

                            dataOutputStream.writeUTF(messageToClient);

                            String requestPhases = dataInputStream.readUTF();

                            if (requestPhases.equals("request phases")) {
                                dataOutputStream.writeUTF(jsonDataPhases);
                            } else {
                                showToast("Shit");
                                Log.e(TAG, "Not request phase string.");
                            }

                        } else {
                            // There might be other queries, but as of now nothing.
                            dataOutputStream.flush();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Unable to get request");
                        dataOutputStream.flush();
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                showToast("Couldn't create server socket.");
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    protected void onPause() {
        try {
            if (mNsdManager != null) {
                mNsdManager.unregisterService(mRegistrationListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "onStop: Service couldn't be unregistered.");
            showToast("onStop: Service couldn't be unregistered.");
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (mNsdManager != null) {
                registerService(9000);
            }
        } catch (Exception e) {
            Log.e(TAG, "onResume: Service couldn't be registered.");
            showToast("onResume: Service couldn't be registered.");
        }
    }

    @Override
    protected void onStop() {
        try {
            if (mNsdManager != null) {
                mNsdManager.unregisterService(mRegistrationListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "onStop: Service couldn't be unregistered.");
            showToast("onStop: Service couldn't be unregistered.");
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        try {
            if (mNsdManager != null) {
                mNsdManager.unregisterService(mRegistrationListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "onDestroy: Service couldn't be unregistered.");
            showToast("onDestroy: Service couldn't be unregistered.");
        }
        super.onDestroy();
    }
}
