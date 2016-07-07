package se.andreasmikaelsson.dungeonswap;

import android.app.Activity;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdManager.RegistrationListener;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DMCombatActivity extends Activity implements View.OnClickListener {

    private static final String REQUEST_SEND_INITIATIVE = "say_this";

    private Map<String, Integer> initiativeOrder = new HashMap<String, Integer>();
    private Button getInitiative;
    private TextView waitingForInitiative;
    private TextView mStatusNameView;
    private TextView mStatusScoreView;

    private String SERVICE_NAME = "Server Device";
    private String SERVICE_TYPE = "_dswap._tcp.";
    private static final String REQUEST_CONNECT_CLIENT = "request-connect-client";
    private SocketServerThread socketServerThread;
    private NsdManager mNsdManager;

    private int SocketServerPort = 6000;

    private List<String> clientIPs;

    private static final String TAG = "NSDServer";

    public void showToast(final String toast){
        DMCombatActivity.this.runOnUiThread(new Runnable(){
            public void run(){
                Toast.makeText(DMCombatActivity.this,toast,Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dm_combat);

        waitingForInitiative = (TextView) findViewById(R.id.dm_text_waiting);
        getInitiative = (Button) findViewById(R.id.dm_button_get_initiative);
        getInitiative.setOnClickListener(this);
        mStatusNameView = (TextView) findViewById(R.id.dm_status_name);
        mStatusScoreView = (TextView) findViewById(R.id.dm_status_score);

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
        socketServerThread = new SocketServerThread();
        socketServerThread.start();
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

    RegistrationListener mRegistrationListener = new NsdManager.RegistrationListener() {

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
            Log.d(TAG,
                    "Service Unregistered : " + serviceInfo.getServiceName());
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

        @Override
        public void run() {

            Socket socket = null;
            ServerSocket serverSocket = null;
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;

            int count_sent_initiatives = 0;

            try {
                Log.i(TAG, "Creating server socket");
                serverSocket = new ServerSocket(SocketServerPort);

                while (true) {
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

                        } else if (request.equals(REQUEST_SEND_INITIATIVE)) {
                            //runMe();

                            String mess = jsondata.getString("ipAddress");
                            showToast(mess);

                            count_sent_initiatives++;
                            showToast(String.valueOf(count_sent_initiatives));

                            if (count_sent_initiatives == 1) {
                                DMCombatActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        waitingForInitiative.setVisibility(View.GONE);
                                        getInitiative.setVisibility(View.VISIBLE);
                                    }
                                });
                            }

                            //Add player name and initiative to map
                            if (mess != null) {
                                String[] msgSplit = mess.split("/");
                                int initiativeScore = Integer.valueOf(msgSplit[0]);
                                String playerName = msgSplit[1];
                                initiativeOrder.put(playerName, initiativeScore);
                                messageToClient = "Initiative Accepted";
                            } else {
                                messageToClient = "Initiative not saved";
                            }

                            dataOutputStream.writeUTF(messageToClient);

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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.dm_button_get_initiative:
                mStatusNameView.setText("");
                mStatusScoreView.setText("");
                showInitiativeOrder(initiativeOrder);
                break;
        }
    }

    public void showInitiativeOrder(Map initOrder) {
        initOrder = sortByValue(initOrder);

        Set keys = initOrder.keySet();
        for (Iterator i = keys.iterator(); i.hasNext(); ) {
            String key = (String) i.next();
            String value = String.valueOf(initOrder.get(key));
            mStatusNameView.append(key + "\n");
            mStatusScoreView.append(value + "\n");
        }
    }

    public static <K, V extends Comparable<? super V>> Map<K, V>
    sortByValue( Map<K, V> map )
    {
        List<Map.Entry<K, V>> list =
                new LinkedList<>( map.entrySet() );
        Collections.sort( list, new Comparator<Map.Entry<K, V>>()
        {
            @Override
            public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 )
            {
                return (( o1.getValue() ).compareTo( o2.getValue() ) * -1);
            }
        } );

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list)
        {
            result.put( entry.getKey(), entry.getValue() );
        }
        return result;
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
