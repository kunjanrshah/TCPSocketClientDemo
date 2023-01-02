package com.kerwin.myapplication;

import android.app.Activity;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class ClientActivity extends Activity implements View.OnClickListener {

    public static final int SERVERPORT = 3333;
    public static String SERVER_IP = "192.168.4.1"; // //192.168.1.104
    //public static final int SERVERPORT = 8090;
    //public static String SERVER_IP = "13.233.28.141";
    private ClientThread clientThread;
    private LinearLayout msgList;
    private Handler handler;
    private int clientTextColor;

    private EditText edMsgSend,edtMsgReceive;
    private TextView txtConnect,txtSwitchid1,txtSwitch1,txtUnits1,txtKw1,txtAmp1,txtSwitchid2,txtSwitch2,txtUnits2,txtKw2,txtAmp2;
    private final String TAG = ClientActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_client);
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
            Log.e(TAG, "Client ip: " + ip);
            //  SERVER_IP=ip;
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("ESP DEMO");
        clientTextColor = getResources().getColor(R.color.green);
        handler = new Handler();
        msgList = (LinearLayout) findViewById(R.id.msgList);
        edMsgSend = (EditText) findViewById(R.id.edMsgSend);
        edtMsgReceive = (EditText) findViewById(R.id.edtMsgReceive);

        txtConnect= (TextView) findViewById(R.id.txtConnect);
        txtSwitchid1= (TextView) findViewById(R.id.txtSwitchid1);
        txtSwitch1 = (TextView) findViewById(R.id.txtSwitch1);
        txtUnits1 = (TextView) findViewById(R.id.txtUnits1);
        txtKw1 = (TextView) findViewById(R.id.txtKw1);
        txtAmp1 = (TextView) findViewById(R.id.txtAmp1);

        txtSwitchid2= (TextView) findViewById(R.id.txtSwitchid2);
        txtSwitch2 = (TextView) findViewById(R.id.txtSwitch2);
        txtUnits2 = (TextView) findViewById(R.id.txtUnits2);
        txtKw2 = (TextView) findViewById(R.id.txtKw2);
        txtAmp2 = (TextView) findViewById(R.id.txtAmp2);

    }


    private TextView textView(String message, int color) {
        if (null == message || message.trim().isEmpty()) {
            message = "<Empty Message>";
        }
      //  if (!message.contains("raju") && !message.contains("Connect")) {
            edtMsgReceive.setText(message);

            // 1 1 0 0 0 0 0 0 4 15 2 0 0 0 0 0 0 0 0 3 0 0 0 0 0 0 0 0 4 0 0 0 0 0 0 0 0
            String msg[]= message.split(" ");
            if(msg.length>20){
                txtConnect.setText(msg[0]);
                txtSwitchid1.setText(msg[1]);
                txtUnits1.setText(msg[2]+""+msg[3]+" Units");
                txtKw1.setText(msg[4]+""+msg[5]+" Watts");
                txtAmp1.setText(msg[6]+""+msg[7]+" Amp");
                txtSwitch1.setText(Integer.toBinaryString(Integer.parseInt(msg[9])));

                txtSwitchid2.setText(msg[10]);
                txtUnits2.setText(msg[11]+""+msg[12]+" Units");
                txtKw2.setText(msg[13]+""+msg[14]+" Watts");
                txtAmp2.setText(msg[15]+""+msg[16]+" Amp");
                txtSwitch2.setText(Integer.toBinaryString(Integer.parseInt(msg[18])));
            }

       // }

        TextView tv = new TextView(this);
        tv.setTextColor(color);
        tv.setText(message + " [" + getTime() + "]");
        tv.setTextSize(20);
        tv.setPadding(0, 5, 0, 0);
        return tv;

    }

    public void showMessage(final String message, final int color) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                msgList.addView(textView(message, color));
            }
        });
    }


    @Override
    public void onClick(View view) {

        if (view.getId() == R.id.connect_server) {
            msgList.removeAllViews();
            showMessage("Connecting to Server...", clientTextColor);
            clientThread = new ClientThread();
            Thread thread = new Thread(clientThread);
            thread.start();

            return;
        }

        if (view.getId() == R.id.send_data) {
            String clientMessage = edMsgSend.getText().toString().trim();
            //clientMessage = "data,mac=raju:weight=" + clientMessage;
            // clientMessage = "331";
            //showMessage(clientMessage, Color.BLUE);
            //Toast.makeText(this, "" + clientMessage, Toast.LENGTH_SHORT).show();
            if (null != clientThread) {
                clientThread.sendMessage(clientMessage);
                //  edMsgSend.setText("data,mac=raju:weight=123");
            }
        }
        /*if (view.getId() == R.id.listen_data) {
            //String clientMessage = "listen,mac=raju"
          //  String clientMessage = "331";
            //   clientMessage="kunjan,10";
            //showMessage(clientMessage, Color.BLUE);
            if (null != clientThread) {
                clientThread.sendMessage(clientMessage);
                // edMsgSend.setText("data,mac=raju:weight=123");
            }
        }*/


    }

    String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != clientThread) {
            clientThread.sendMessage("Disconnect");
            clientThread = null;
        }
    }

    class ClientThread implements Runnable {

        private Socket socket;
        private BufferedReader input;
        private DataInputStream dis;

        @Override
        public void run() {

            try {
                Log.e(TAG, "server ip: " + SERVER_IP);
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket(serverAddr, SERVERPORT);
                InputStream is = socket.getInputStream();               // SOCKET READ
                byte[] buffer = new byte[1024];
                int read;
                String message = null;

                while ((read = is.read(buffer)) != -1) {
                    message = new String(buffer, 0, read);
                    System.out.print(message);
                    System.out.flush();
                    Log.e(TAG, "message from server: " + message);
                    showMessage(message, clientTextColor);
                }


            } catch (UnknownHostException e1) {
                e1.printStackTrace();
                Log.e(TAG, "UnknownHostException: ");
            } catch (IOException e1) {
                e1.printStackTrace();
                Log.e(TAG, "IOException:");
            }
             showMessage("Connected to Server...", clientTextColor);
        }

        void sendMessage(final String message) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (null != socket) {
//  SOCKET WRITE
                            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                            /*PrintWriter out = new PrintWriter(socket.getOutputStream());
                            out.println();
                            out.flush();*/
                            Log.e(TAG, "sendMessage: " + message);
                            out.println(message);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Exception: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
}