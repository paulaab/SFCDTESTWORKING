package de.vodafone.innogarage.testsfcdtool;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telecom.Connection;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainActivity extends AppCompatActivity {

    /*--------Connection Variables------------*/
    public static int socketPortForBroadcast = 45555;
    public static int socketServerPortForSFCD = 45556;
    public int bufferSize = 4096;
    public Context globalContext;
    public ConnectionManager connectionManager;
    public volatile boolean serverOn;
    private Handler handler;


    /*--------Layout Variables----------------*/
    private Button buttonState;
    private volatile boolean close;


    /*--------Devices List Variables----------*/
    private volatile List<Connection> connectionList;
    private ListView listDevices;
    private ListViewAdapter listAdapter;
    private volatile List<Button> buttonList;










    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //setTheme(R.style.MyAppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        globalContext = this;
        //Initialize views
        listDevices = (ListView) findViewById(R.id.mSFCDList);
        buttonState = (Button) findViewById(R.id.buttonState);
        //Initialize list
        connectionList = new CopyOnWriteArrayList<Connection>();
        listAdapter = new ListViewAdapter(globalContext, connectionList);
        listDevices.setAdapter(listAdapter);
        buttonList = new ArrayList<Button>();
        //Initialize Server
        connectionManager = new ConnectionManager();
        serverOn = true;

        /*/ Create the Handler object (on the main thread by default)
        handler = new Handler();
// Define the code block to be executed
        Runnable runnableCode = new Runnable() {
            @Override
            public void run() {
                // Do something here on the main thread
                if (!connectionList.isEmpty()){
                    for (Connection connec : connectionList){
                        if(connec.isClose()){
                            connectionList.remove(connec);
                            listAdapter.notifyDataSetChanged();
                        }
                    }
                }
                Log.d("Handlers", "Called on main thread");
                // Repeat this the same runnable code block again another 2 seconds
                handler.postDelayed(this, 2000);
            }
        };
// Start the initial runnable task by posting through the handler
        handler.post(runnableCode);*/

        /*timerTask2 = new TimerTask() {
            @Override
            public void run() {

                for (Connection connec : connectionList){
                    if(connec.isClose()){
                        connectionList.remove(connec);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                listAdapter.notifyDataSetChanged();
                            }
                        });

                    }
                }



            }
        };
        timer2.schedule(timerTask2, 0, 500);*/








    }

        /*=============================Connection Manager===================================*/
    //Creates Server socket and binds to a socket object for every new connection accepted

    public class ConnectionManager {
        private ServerSocket serverSocket;

        public ConnectionManager() {

            //Creating Server Socket
            try{
                serverSocket = new ServerSocket(socketServerPortForSFCD);
            }catch (IOException e){
                Log.e("ConnectionManager","Unable to create Server socket: ");
                e.printStackTrace();
            }
            new ConnectionListenerForSFCD().start();


        }


        private class ConnectionListenerForSFCD extends Thread{
            @Override
            public void run() {
                while (serverOn){
                    Socket clientSocket = null;
                    try{
                        clientSocket = serverSocket.accept();
                        Log.e("ConnectionManager "," Connection Listener :Waiting for connections!\n");
                        connectionList.add(new Connection(clientSocket));
                        Log.e("ConnectionManager "," Connection Listener: New SFCD added. IP= " + clientSocket.getInetAddress().toString());
                        Log.e("ConnectionManager","LIST LENGTH : " + connectionList.size());
runOnUiThread(new Runnable() {
    @Override
    public void run() {
        Log.e("inrunoi","LIST LENGTH : " + connectionList.size());
        listAdapter.notifyDataSetChanged();
        Log.e("inrunoi","Notified");

    }
});


                        //Connections List changed. Notify ListView adapter
                    }catch (IOException e){
                        e.printStackTrace();
                        Log.e("ConnectionManager","Could not accept client request");
                    }
                }
            }
        }



        public void setServerState (boolean serverState){
            serverOn = serverState;

        }

        public void clearConnections() throws IOException {
            if (!connectionList.isEmpty()){
                for (Connection connect : connectionList){
                    connect.cliSocket.close();

                }
                connectionList.clear();
            }

        }

        public void sendInvitation(){
            new Broadcaster().start();
        }
    }

    /*===================================== Connection =====================================*/
    //Starts reception of messages
    public class Connection {
        private Socket cliSocket;
        private int errorCounter = 0;
        private boolean focus, online;
        private List<JSONObject> incomingData;
        public volatile JSONObject incomingJson;
        private InputStream inputStream;
        //Client Data
        private String clientName;
        private String clientIP;
        private String mode;
        //public Double latitude;
        //public Double longitude;



        private Connection(Socket cliSocket){
            this.cliSocket = cliSocket;
            clientName = cliSocket.getInetAddress().getHostName();
            clientIP = cliSocket.getInetAddress().toString();
            focus = false;
            close = false;
            online=false;


            //Get incoming stream and place it in a List
            try{
                inputStream = cliSocket.getInputStream();
            }catch (IOException e){
                e.printStackTrace();
                Log.e("Connection","Could not get incoming stream");
            }
            new InputStreamThread().start();
        }

        private class InputStreamThread extends Thread {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            public void run(){
                JSONObject jsonObject;
                String line = null;

                while (!close) {
                    try {
                        line = bufferedReader.readLine();
                       // Log.e("breader","LINE :"+ line);
                    } catch (IOException e) {
                        Log.e("readLine", "Could not read line, following error happened: ");
                        e.printStackTrace();
                    }

                    if (line == null) {
                        //Many errors trying to read line. Assuming connection with SFCD is over
                        errorCounter = errorCounter + 1;
                        if (errorCounter > 5) {
                            Log.e("\nInputStreamThread", "SFCD " + clientName + " seems to be inactive. Stopping receiving thread...\n");
                            close = true;
                            // TODO: Implement method to remove this connection.
                            // connectionList.remove(Connection);

                            try {
                                cliSocket.close();
                                cliSocket = null;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Log.e("\nInputStreamThread", "SFCD " + clientName + " Connection removed");
                        }
                    }
                    //Save incoming message into a json object
                    else {
                        try {
                            line = line.substring(line.indexOf("{") );
                            Log.e("Buffer after conv","LINE :"+ line);
                            //System.out.print("Converted line: "+line);

                            jsonObject = new JSONObject(line);

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e("InputStreamThread: ", cliSocket.getInetAddress() + "   Could not save Json Object with incoming stream");
                            jsonObject = new JSONObject();
                        }
                        //Save into list of incoming data objects
                        incomingJson = jsonObject;
                        // incomingData.add(jsonObject);
                        //Log.e("Connection: ", cliSocket.getInetAddress() + " Message received: " + jsonObject.toString() + " => Placed in incomingData, parsed as JSON");
                        //Check if Mode is online or not
                        try {
                            mode = jsonObject.getJSONObject("gstatus").getString("mode");
                            if (mode.equalsIgnoreCase("ONLINE")) {

                                online = true;

                            } else {

                                online = false;

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            System.out.print("Could not save Mode value");
                        }

                        //Display Marker for every message received.
                        /*
                        try {
                            Double latitude = jsonObject.getJSONObject("gstatus").getDouble("latitude");
                            Double longitude = jsonObject.getJSONObject("gstatus").getDouble("longitude");
                            int snr = jsonObject.getJSONArray("serving").getJSONObject(0).getInt("SNR");
                            displayMarker(latitude, longitude, snr, 1.0F);

                        }
                        catch (JSONException e){
                            System.out.print("Could not get coordinates data:");
                            e.printStackTrace();
                        }*/
                    }

                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public String getName() {return clientName;}
        public String getIP(){return clientIP;}
        public List<JSONObject> getIncomingData(){return incomingData;}
        public JSONObject getIncomingJson(){return incomingJson;}
        //public Double getlongitude(){return longitude;}
        //public Double getlatitude(){return latitude;}
        public boolean isFocus() {return focus;}
        public void setFocus(boolean focus) {this.focus = focus;}
        public boolean isOnline() {return online;}
        public boolean isClose(){return close;}

    }


    /*===================================== Layout =========================================*/
    /*---------------------------------- Menu on Toolbar------------------------------------*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_invite:
                //serverOn = true;
                if(!connectionList.isEmpty()){
                    try {
                        connectionManager.clearConnections();
                        close = true;
                        if(!buttonList.isEmpty()){
                            buttonList.clear();
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                listAdapter.notifyDataSetChanged();
                                buttonState.setVisibility(View.INVISIBLE);

                            }
                        });
                        Thread.sleep(10);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                connectionManager.sendInvitation();
                Toast.makeText(MainActivity.this,"Invitation sent",Toast.LENGTH_SHORT).show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*---------------------------------List of devices adapter------------------------------*/
    public class ListViewAdapter extends BaseAdapter {

        List<Connection> connectionList;
        Context context;
        LayoutInflater inflater;

        public ListViewAdapter(Context c,List<Connection> connectionList) {
            this.context = c;
            this.connectionList = connectionList;
            inflater = (LayoutInflater.from(c));
        }



        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            convertView = inflater.inflate(R.layout.sfcdlist_row, parent, false);
            TextView tv_hostname = (TextView) convertView.findViewById(R.id.client_name);
            TextView tv_clientIP = (TextView) convertView.findViewById(R.id.client_ip);
            final Button button = (Button) convertView.findViewById(R.id.detailsButton);

            button.setBackgroundColor(Color.GRAY);
            button.setText("Select");


            //Button buttonState= (Button) findViewById(R.id.buttonState);

            final Connection con = connectionList.get(position);
            button.setTag(con.getIP());
            buttonList.add(button);


            button.setOnClickListener(new View.OnClickListener(){

                public void onClick(View v){



                    for(Connection conn : connectionList){
                        conn.setFocus(false);
                    }
                    for(Button b : buttonList){
                        b.setBackgroundColor(Color.GRAY);
                        b.setText("Select");
                    }
                    button.setBackgroundColor(getResources().getColor(R.color.lightGreen));
                    button.setText("Selected");

                    con.setFocus(true);
                    if(con.isOnline()){

                        buttonState.setVisibility(View.VISIBLE);
                        buttonState.setBackgroundColor(getResources().getColor(R.color.lightGreen));
                        buttonState.setText("ONLINE: "+con.getName());

                    }else{

                        buttonState.setVisibility(View.VISIBLE);
                        buttonState.setBackgroundColor(getResources().getColor(R.color.red));
                        buttonState.setText("OFFLINE: "+con.getName());
                    }

                }
            });

            tv_hostname.setText(con.getName());
            tv_clientIP.setText(con.getIP());

            return convertView;
        }



        @Override
        public int getCount() {
            return connectionList.size() ;
        }

        @Override
        public Object getItem(int position) {
            return connectionList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

    }










}
