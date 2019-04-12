package com.krs.client;

import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
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
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class ClientActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int SERVERPORT = 8080;

    public static String SERVER_IP = "13.233.28.141";
    private ClientThread clientThread;
    private Thread thread;
    private LinearLayout msgList;
    private Handler handler;
    private int clientTextColor;
    private EditText edMessage;
    private String TAG = ClientActivity.class.getSimpleName();

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


        setTitle("Client");
        clientTextColor = ContextCompat.getColor(this, R.color.green);
        handler = new Handler();
        msgList = findViewById(R.id.msgList);
        edMessage = findViewById(R.id.edMessage);
    }

    public TextView textView(String message, int color) {
        if (null == message || message.trim().isEmpty()) {
            message = "<Empty Message>";
        }
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
            thread = new Thread(clientThread);
            thread.start();
            showMessage("Connected to Server...", clientTextColor);
            return;
        }

        if (view.getId() == R.id.send_data) {
            String clientMessage = edMessage.getText().toString().trim();
            //   clientMessage="kunjan,10";
            showMessage(clientMessage, Color.BLUE);
            if (null != clientThread) {
                clientThread.sendMessage(clientMessage);
                edMessage.setText("data,mac=raju:weight=123");

            }
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
                InputStream is = socket.getInputStream();
                byte[] buffer = new byte[1024];
                int read;
                String message = null;
                ObjectInputStream ois = null;

               /* ois = new ObjectInputStream(socket.getInputStream());
                String message = (String) ois.readObject();
                System.out.println("Message: " + message);
               // ois.close();
                Thread.sleep(100);*/

                while ((read = is.read(buffer)) != -1) {
                    message = new String(buffer, 0, read);
                    System.out.print(message);
                    System.out.flush();
                    Log.e(TAG, "message from server: " + message);
                    showMessage("Server: " + message, clientTextColor);
                }
                ;

               /* while (!Thread.currentThread().isInterrupted()) {
                    Log.e(TAG,"client is listening...");

                    *//*this.dis = new DataInputStream(socket.getInputStream());
                    String message=this.dis.readLine();
                    Log.e(TAG,"message from server: "+message);*//*

                 *//*int bytesExpected = is.available(); //it is waiting here
                    byte[] buffer1 = new byte[bytesExpected];
                    int readCount = is.read(buffer1);
                    Log.e(TAG,"message from server: readCount "+readCount);*//*

                   // this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                   // this.input.ready();
                   // message = input.readLine();
                    //Log.e(TAG,"message from server2: "+message);
                    if (null == message || "Disconnect".contentEquals(message)) {
                        Thread.interrupted();
                        message = "Server Disconnected.";
                        showMessage(message, Color.RED);
                        break;
                    }
                    showMessage("Server: " + message, clientTextColor);
                }*/

            } catch (UnknownHostException e1) {
                e1.printStackTrace();
                Log.e(TAG, "UnknownHostException: ");
            } catch (IOException e1) {
                e1.printStackTrace();
                Log.e(TAG, "IOException:");
            }
        }

        void sendMessage(final String message) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (null != socket) {
                            PrintWriter out = new PrintWriter(new BufferedWriter(
                                    new OutputStreamWriter(socket.getOutputStream())),
                                    true);
                            /*PrintWriter out = new PrintWriter(socket.getOutputStream());
                            out.println();
                            out.flush();*/
                            Log.e(TAG,"sendMessage: "+message);
                            out.println(message);
                        }
                    } catch (Exception e) {
                        Log.e(TAG,"Exception: "+e.getMessage());
                        e.printStackTrace();
                    }
                }
            }).start();
        }
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
}