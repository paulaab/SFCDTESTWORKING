package de.vodafone.innogarage.testsfcdtool;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telecom.Connection;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback {

    /*--------Connection Variables------------*/
    public static int socketPortForBroadcast = 45555;
    public static int socketServerPortForSFCD = 45556;
    public ConnectionManager connectionManager;
    public volatile boolean serverOn;
    private volatile boolean close;


    /*--------Layout Variables----------------*/
    private Button buttonState;
    public Context globalContext;
    private Button buttonTitle;

    private ListView lvServing;
    private ListView lvIntrafreq;
    private ListView lvInterfreq;
    public ArrayAdapter<String> arrayAdapter;
    List<String> servingList = new ArrayList<String>();
    //ArrayList<HashMap<String, String>> servingList;
    ArrayList<HashMap<String, String>> intrafreqList;
    ArrayList<HashMap<String, String>> interfreqList;
    ArrayList<String> arrayList;


    /*--------Devices List Variables----------*/
    private volatile List<Connection> connectionList;
    private ListView listDevices;
    private ListViewAdapter listAdapter;
    private volatile List<Button> buttonList;

    /*-------------Graph Variables------------*/
    /*-------------Map Variables--------------*/
    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean mPermissionDenied = false;

    /*----------Additional Variables---------*/
    TimerTask timerTask;
    TimerTask timerTask2;
    Timer timer2 = new Timer();
    Timer timer = new Timer();

    /*------------Textviews------------------*/
    //Location
    TextView altitude;
    TextView ept;
    TextView climb;
    TextView eps;
    TextView epv;
    TextView epx;
    TextView speed;
    TextView track;
    TextView longitudetx;
    TextView latitudetx;
    TextView satellites;
    TextView modetx;
    //GStatus
    TextView ltebw;
    TextView rsrprxdr;
    TextView rsrprxm;
    TextView grsrq;
    TextView gsinr;
    TextView gmode;
    TextView ltecastate;
    TextView cellid;
    TextView currenttime;
    TextView ltetxchan;
    TextView gtac;
    TextView emmstatereg;
    TextView rrcstate;
    TextView temperature;
    TextView systemmode;
    TextView psstate;
    TextView emmstateserv;
    TextView lteband;
    TextView lterxchan;
    TextView gtxpower;
    TextView imsregstate;
    TextView resetcounter;
    TextView pccrxdrssi;
    TextView pccrxrmrssi;






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        globalContext = this;

        //Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync((OnMapReadyCallback) this);


        //Initialize views
        listDevices = (ListView) findViewById(R.id.mSFCDList);
        buttonState = (Button) findViewById(R.id.buttonState);
        buttonTitle = (Button) findViewById(R.id.buttonTitle);

        //Initialize list
        connectionList = new CopyOnWriteArrayList<Connection>();
        listAdapter = new ListViewAdapter(globalContext, connectionList);
        listDevices.setAdapter(listAdapter);
        buttonList = new ArrayList<Button>();


        servingList = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,servingList);
        intrafreqList = new ArrayList<>();
        interfreqList = new ArrayList<>();
        lvServing = (ListView) findViewById(R.id.listServing);
        //lvInterfreq = (ListView) findViewById(R.id.listInterfreq);
        //lvIntrafreq = (ListView) findViewById(R.id.listIntrafreq);


        //Initialize Server
        connectionManager = new ConnectionManager();
        serverOn = true;

        //Initialize Textviews
        //LOCATION
        altitude = (TextView) findViewById(R.id.altitude);
        ept = (TextView) findViewById(R.id.ept);
        climb = (TextView) findViewById(R.id.climb);
        eps = (TextView) findViewById(R.id.eps);
        epv = (TextView) findViewById(R.id.epv);
        epx = (TextView) findViewById(R.id.epx);
        speed = (TextView) findViewById(R.id.speed);
        track = (TextView) findViewById(R.id.track);
        longitudetx = (TextView) findViewById(R.id.longitude);
        latitudetx = (TextView) findViewById(R.id.latitude);
        satellites = (TextView) findViewById(R.id.satellites);
        modetx = (TextView) findViewById(R.id.mode);
        //GSTATUS
        ltebw = (TextView) findViewById(R.id.ltebw);
        rsrprxdr = (TextView) findViewById(R.id.rsrprxdr);
        rsrprxm = (TextView) findViewById(R.id.rsrprxm);
        grsrq = (TextView) findViewById(R.id.grsrq);
        gsinr = (TextView) findViewById(R.id.gsinr);
        gmode = (TextView) findViewById(R.id.gmode);
        ltecastate = (TextView) findViewById(R.id.ltecastate);
        cellid = (TextView) findViewById(R.id.cellid);
        currenttime= (TextView) findViewById(R.id.currenttime);
        ltetxchan = (TextView) findViewById(R.id.ltetxchan);
        gtac = (TextView) findViewById(R.id.gtac);
        emmstatereg = (TextView) findViewById(R.id.emmstatereg);
        rrcstate = (TextView) findViewById(R.id.rrcstate);
        temperature = (TextView) findViewById(R.id.temperature);
        systemmode = (TextView) findViewById(R.id.systemmode);
        psstate = (TextView) findViewById(R.id.psstate);
        emmstateserv = (TextView) findViewById(R.id.emmstateserv);
        lteband = (TextView) findViewById(R.id.lteband);
        lterxchan = (TextView) findViewById(R.id.lterxchan);
        gtxpower = (TextView) findViewById(R.id.gtxpower);
        imsregstate = (TextView) findViewById(R.id.imsregstate);
        resetcounter = (TextView) findViewById(R.id.resetcounter);
        pccrxdrssi  = (TextView) findViewById(R.id.pccrxdrssi);
        pccrxrmrssi = (TextView) findViewById(R.id.pccrxrmrssi);

        buttonTitle.setText("Please select a device");
        lvServing.setAdapter(arrayAdapter);

        timerTask = new TimerTask() {
            @Override
            public void run() {

                try {
                    new DisplayResults().execute();
                    //Thread.sleep(10);
                    for(Connection cons: connectionList){
                        if(!cons.isFocus()){
                            cons.wipeList();
                        }
                    }

                }
                catch (Exception ex){
                    ex.printStackTrace();
                    Log.e("TimerScanner","Error executing ScannerTask");

                }

            }
        };
        timer.schedule(timerTask, 0, 1500);



















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
        public volatile List<JSONObject> incomingData = new CopyOnWriteArrayList<>();
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
                            // TODO: Check if following method to remove this connection works.
                            try {
                                cliSocket.close();
                                cliSocket = null;
                                for (Connection connec : connectionList){
                                    if(connec.isClose()){
                                        connectionList.remove(connec);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                listAdapter.notifyDataSetChanged();
                                                Log.e("\nInputStreamThread", "SFCD " + clientName + " Connection removed");
                                            }
                                        });

                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

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
                        //incomingJson = jsonObject;
                        incomingData.add(jsonObject);
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

                        try {
                            Double latitude = jsonObject.getJSONObject("gstatus").getDouble("latitude");
                            Double longitude = jsonObject.getJSONObject("gstatus").getDouble("longitude");
                            int snr = jsonObject.getJSONArray("serving").getJSONObject(0).getInt("SNR");
                            displayMarker(latitude, longitude, snr);

                        }
                        catch (JSONException e){
                            System.out.print("Could not get coordinates data:");
                            e.printStackTrace();
                        }
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
        public void wipeList(){
            if(!incomingData.isEmpty()){
                incomingData.remove(0);
            }

        }

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

    /*------------------------------------------Cleanviews----------------------------------*/
    public void clearViews(){
        ltebw.setText("N/A");
        rsrprxdr.setText("N/A");
        rsrprxm.setText("N/A");
        grsrq.setText("N/A");
        gsinr.setText("N/A");
        gmode.setText("N/A");
        ltecastate.setText("N/A");
        cellid.setText("N/A");
        currenttime.setText("N/A");
        ltetxchan.setText("N/A");
        gtac.setText("N/A");
        emmstatereg.setText("N/A");
        rrcstate.setText("N/A");
        temperature.setText("N/A");
        systemmode.setText("N/A");
        psstate.setText("N/A");
        emmstateserv.setText("N/A");
        lteband.setText("N/A");
        lterxchan.setText("N/A");
        gtxpower.setText("N/A");
        imsregstate.setText("N/A");
        resetcounter.setText("N/A");
        pccrxdrssi.setText("N/A");
        pccrxrmrssi.setText("N/A");
        altitude.setText("N/A");
        ept.setText("N/A");
        climb.setText("N/A");
        eps.setText("N/A");
        epv.setText("N/A");
        epx.setText("N/A");
        speed.setText("N/A");
        track.setText("N/A");
        longitudetx.setText("N/A");
        latitudetx.setText("N/A");
        satellites.setText("N/A");
        modetx.setText("N/A");


    }

    public void clearButton(){

        buttonState.setVisibility(View.INVISIBLE);

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
                        buttonTitle.setText(con.getName().toUpperCase());

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

    /*----------------------------Displaying results on screen------------------------------*/

    public class DisplayResults extends AsyncTask<Void,Void,Wrapper>{
        @Override
        protected void onPreExecute() {
            servingList.clear();
            interfreqList.clear();
            intrafreqList.clear();
        }

        @Override
        protected Wrapper doInBackground(Void... voids) {
            Wrapper w = new Wrapper();
            JSONObject actMsg = null;
            Connection con = null;
            List<JSONObject> msgList;
            if(!connectionList.isEmpty()){
                for (Connection temp : connectionList){
                    //temp.wipeList();
                    if(temp.isFocus()){
                        con = temp;
                    }
                }
                if (con == null){
                    con = connectionList.get(0);
                }
                msgList = con.getIncomingData();

               if(!msgList.isEmpty()){
                   w.actMsg  = msgList.get(0);
                    msgList.remove(0);

                   try {
                       JSONArray sarr = w.actMsg.getJSONArray("serving");
                       if (sarr != null) {
                           // looping through all objects
                           for (int i = 0; i < sarr.length(); i++) {
                               JSONObject c = sarr.getJSONObject(i);
                               Iterator<?> keys = c.keys();
                               while (keys.hasNext()) {
                                   String mKey = (String) keys.next();
                                   String info = mKey + i+" : "+c.getString(mKey);
                                   servingList.add(info);

                               }






                               /*String searfcn = c.getString("EARFCN");
                               String smcc = c.getString("MCC");
                               String smnc = c.getString("MNC");
                               String stac = c.getString("TAC");
                               String scid = c.getString("CID");
                               String sbd = c.getString("Bd");
                               String sd = c.getString("D");
                               String su = c.getString("U");
                               String ssnr = c.getString("SNR");
                               String spci = c.getString("PCI");
                               String srsrq = c.getString("RSRQ");
                               String srsrp = c.getString("RSRP");
                               String srssi = c.getString("RSSI");
                               String srxlv = c.getString("RXLV");


                               // tmp hash map for single contact
                               HashMap<String, String> serving = new HashMap<>();

                               // adding each child node to HashMap key => value
                               serving.put("EARFCN", searfcn);
                               serving.put("MCC", smcc);
                               serving.put("MNC", smnc);
                               serving.put("TAC", stac);
                               serving.put("CID", scid);
                               serving.put("Bd", sbd);
                               serving.put("D", sd);
                               serving.put("U", su);
                               serving.put("SNR", ssnr);
                               serving.put("PCI", spci);
                               serving.put("RSRQ", srsrq);
                               serving.put("RSRP", srsrp);
                               serving.put("RSSI", srssi);
                               serving.put("RXLV", srxlv);

                               // adding contact to contact list
                               servingList.add(serving);*/
                           }
                           }
                       } catch(JSONException e){
                           Log.e("JSON Background", "Json parsing error: " + e.getMessage());


                       }


                   }







                //actMsg = con.getIncomingJson();
            }

            return w;
        }

        @Override
        protected void onPostExecute(Wrapper w) {
            if(!connectionList.isEmpty()){
                if(w.actMsg!=null){
                    Iterator<?> keys = w.actMsg.keys();
                    while (keys.hasNext()){
                        String mKey = (String) keys.next();
                        switch (mKey){
                            case "gstatus":
                                try {
                                    JSONObject gobj = w.actMsg.getJSONObject("gstatus");
                                    ltebw.setText(gobj.getString("ltebw(mhz)"));
                                    rsrprxdr.setText(gobj.getString("rsrp(dbm)pccrxdrssi"));
                                    rsrprxm.setText(gobj.getString("rsrp(dbm)pccrxmrssi"));
                                    grsrq.setText(gobj.getString("rsrq(db)"));
                                    gsinr.setText(gobj.getString("sinr(db)"));
                                    gmode.setText(gobj.getString("mode"));
                                    ltecastate.setText(gobj.getString("ltecastate"));
                                    cellid.setText(gobj.getString("cellid"));
                                    currenttime.setText(gobj.getString("currenttime"));
                                    ltetxchan.setText(gobj.getString("ltetxchan"));
                                    gtac.setText(gobj.getString("tac"));
                                    emmstatereg.setText(gobj.getString("emmstatereg"));
                                    rrcstate.setText(gobj.getString("rrcstate"));
                                    temperature.setText(gobj.getString("temperature"));
                                    systemmode.setText(gobj.getString("systemmode"));
                                    psstate.setText(gobj.getString("psstate"));
                                    emmstateserv.setText(gobj.getString("emmstateserv"));
                                    lteband.setText(gobj.getString("lteband"));
                                    lterxchan.setText(gobj.getString("lterxchan"));
                                    gtxpower.setText(gobj.getString("txpower"));
                                    imsregstate.setText(gobj.getString("imsregstate"));
                                    resetcounter.setText(gobj.getString("resetcounter"));
                                    pccrxdrssi.setText(gobj.getString("pccrxdrssi"));
                                    pccrxrmrssi.setText(gobj.getString("pccrxmrssi"));
                                } catch (JSONException e){
                                    System.out.println("Could not extract gstatus object -->");
                                    e.printStackTrace();
                                }
                                break;

                            case "location":
                                try {
                                    JSONObject lobj = w.actMsg.getJSONObject("location");
                                    if (lobj != null) {
                                        altitude.setText(lobj.getString("altitude"));
                                        ept.setText(lobj.getString("ept"));
                                        climb.setText(lobj.getString("climb"));
                                        eps.setText(lobj.getString("eps"));
                                        epv.setText(lobj.getString("epv"));
                                        epx.setText(lobj.getString("epx"));
                                        speed.setText(lobj.getString("speed"));
                                        track.setText(lobj.getString("track"));
                                        longitudetx.setText(lobj.getString("longitude"));
                                        latitudetx.setText(lobj.getString("latitude"));
                                        satellites.setText(lobj.getString("satellites"));
                                        modetx.setText(lobj.getString("mode"));
                                    }
                                } catch (JSONException e) {
                                    System.out.println("Could not extract location object -->");
                                    e.printStackTrace();
                                }

                                break;

                            case "serving":
                                try {
                                    JSONArray sarr = w.actMsg.getJSONArray("serving");
                                    if (sarr != null) {

                                        JSONObject sobj = sarr.getJSONObject(0);




                                    }

                                } catch (JSONException e) {
                                    System.out.println("Could not extract serving object -->");
                                    e.printStackTrace();
                                }
                                break;
                            }
                            arrayAdapter.notifyDataSetChanged();

                        }
                    }
                    else {
                    //clearViews();
                    }
                    /*
                ListAdapter adapter = new SimpleAdapter(MainActivity.this, servingList, R.layout.list_item,
                        new String[] { "EARFCN","MCC" },
                        new int[] { R.id.key,R.id.value });

                a
                lvServing.setAdapter(adapter);*/


                }
                else {
                    clearViews();
                    clearButton();
                }
            }
        }

    //-------------------------------------------------------------------------------------------------------------------------------------------------------------
//-----------------------------------------------------------------Map Code------------------------------------------------------------------------------------
//-------------------------------------------------------------------------------------------------------------------------------------------------------------
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        Log.d("MapReady", "map is ready");
        // Add a marker in Sydney and move the camera
        mMap.setOnMyLocationButtonClickListener(this);
        enableMyLocation();
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

    }
    //----------------------------------------------------------------------------My current location-----------------------------------------------------------
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }



    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }


    //---------------------------------------------------------------------------- End my Location-----------------------------------------------------------

    //---------------------------------------------------------------------------- Display a Marker on the Map-----------------------------------------------
    public void displayMarker(final Double Lati, final Double Longi, int snr){

        final long TimeToLive = 1000;
        final BitmapDescriptor myicon;
        int id = 0;
        if (snr <= 10){
            id = getResources().getIdentifier("red", "drawable", getPackageName());
        }
        else if(snr >= 20){
            id = getResources().getIdentifier("yellow", "drawable", getPackageName());
        }
        else{
            id = getResources().getIdentifier("green", "drawable", getPackageName());
        }

        myicon = BitmapDescriptorFactory.fromResource(id);

        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                LatLng pos = new LatLng(Lati,Longi);
                Marker mar = mMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .icon(myicon)

                );
                fadeTime(TimeToLive,mar);
            }

        });
    }

/*-----Customize characteristics of the markers: Size and time to fade--------*/


    public void fadeTime(long duration, Marker marker) {

        final Marker myMarker = marker;
        ValueAnimator myAnim = ValueAnimator.ofFloat(1, 0);
        myAnim.setDuration(duration);
        myAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                myMarker.setAlpha((float) animation.getAnimatedValue());
            }
        });
        myAnim.start();
    }


//-------------------------------------------------------------------------------------------------------------------------------------------------------------
//-----------------------------------------------------------------End Map Code-------------------------------------------------------------------------------
//-------------------------------------------------------------------------------------------------------------------------------------------------------------

    public class Wrapper{
        public JSONObject actMsg;
        public String [] keysArray;
        public String [] valuesArray;


    }







}
