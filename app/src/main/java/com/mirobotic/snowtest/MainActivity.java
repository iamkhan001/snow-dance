package com.mirobotic.snowtest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.csjbot.cosclient.constant.ClientConstant;
import com.csjbot.cosclient.entity.CommonPacket;
import com.csjbot.cosclient.entity.MessagePacket;
import com.csjbot.cosclient.listener.ClientEvent;
import com.csjbot.cosclient.listener.EventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private RobotService robotService;
    private EventListener eventListener;
    private RobotService.OnEventFailedListener failedListener;
    private Context context;
    private MediaPlayer player;
    private TextView tvSpeechResult;


    private ServiceConnection robotConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RobotService.RobotServiceBinder robotServiceBinder = (RobotService.RobotServiceBinder) service;
            robotService = robotServiceBinder.getService();
            robotService.connectRobot(eventListener,failedListener,60002);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = MainActivity.this;


        eventListener = new EventListener() {
            @Override
            public void onEvent(final ClientEvent event) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch (event.eventType) {
                            case ClientConstant.EVENT_RECONNECTED:
                                showMessage("Reconnected");
                                Log.d(TAG, " EVENT_RECONNECTED");
                                break;
                            case ClientConstant.EVENT_CONNECT_SUCCESS:
                                showMessage("Event Connect Success");
                                Log.d(TAG, " EVENT_CONNECT_SUCCESS");
                                break;
                            case ClientConstant.EVENT_CONNECT_FAILD:
                                showMessage("Connected");
                                Log.d(TAG, "EVENT_CONNECT_FAILD" + event.data);
                                break;
                            case ClientConstant.EVENT_CONNECT_TIME_OUT:
                                showMessage("Connection Failed");
                                Log.d(TAG, "EVENT_CONNECT_TIME_OUT  " + event.data);
                                break;
                            case ClientConstant.SEND_FAILED:
                                showMessage("Send Failed");
                                Log.d(TAG, "SEND_FAILED");
                                break;
                            case ClientConstant.EVENT_DISCONNET:
                                showMessage("Disconnected");
                                Log.d(TAG, "EVENT_DISCONNECT");
                                break;
                            case ClientConstant.EVENT_PACKET:
                                MessagePacket packet = (MessagePacket) event.data;
                                String json = ((CommonPacket) packet).getContentJson();
                                Log.d(TAG, json);
                                handleResponse(json);
                                break;
                            default:
                                break;
                        }
                    }
                });

            }
        };

        failedListener = new RobotService.OnEventFailedListener() {
            @Override
            public void onEventFailed(final String cause) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showMessage(cause);
                    }
                });
            }
        };


        Button btnStartDance, btnStopDance;
        final Button btnSing;

        btnStartDance = findViewById(R.id.btnStartDance);
        btnStopDance = findViewById(R.id.btnStopDancing);
        btnSing = findViewById(R.id.btnStartSinging);
        tvSpeechResult = findViewById(R.id.tvSpeechResult);

        btnStartDance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject object = new JSONObject();
                try {
                    object.put("msg_id",Request.DANCE_START_REQ);
                    robotService.sendCommand(object.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });


        btnStopDance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject object = new JSONObject();
                try {
                    object.put("msg_id",Request.DANCE_STOP_REQ);
                    robotService.sendCommand(object.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });



        findViewById(R.id.btnStartSpeech).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject object = new JSONObject();
                try {
                    object.put("msg_id",Request.SPEECH_START_MULTI_RECOG_REQ);
                    robotService.sendCommand(object.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });


        findViewById(R.id.btnStartSpeech).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject object = new JSONObject();
                try {
                    object.put("msg_id",Request.SPEECH_STOP_MULTI_RECOG_REQ);
                    robotService.sendCommand(object.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        btnSing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                playSong();

            }
        });

        Intent intent = new Intent(this, RobotService.class);
        bindService(intent, robotConnection, Context.BIND_AUTO_CREATE);


        player = new MediaPlayer();

    }

    private void handleResponse(String json){
        try {
            JSONObject object = new JSONObject(json);

            Log.e(TAG,"Result >> "+json);

            int errorCode = 0;

            if (object.has("error_code")){
                errorCode = object.getInt("error_code");
            }

            String event = object.getString("msg_id");

            if (errorCode == 0){
                showMessage("Response Success");

                switch (event){

                    case Request.DANCE_START_RSP:
                        showMessage("Dance Start!");

                        break;
                    case Request.DANCE_STOP_RSP:
                        showMessage("Dance Stop!");
                        break;

                    case Request.SPEECH_START_MULTI_RECOG_RSP:
                        showMessage("Speech Start Recognition");
                        break;

                    case Request.SPEECH_STOP_MULTI_RECOG_RSP:
                        showMessage("Speech Stop Recognition");
                        break;

                    case Request.SPEECH_TO_TEXT_NOTIFICATION:
                        try {
                            String text = object.getString("text");
                            tvSpeechResult.setText(text);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        break;
                }

            }else {
                showMessage(event+"\n"+ErrorMessege.getErrorMsg(errorCode));
            }


        }catch (Exception e){
            e.printStackTrace();
        }
    }


    private void showMessage(final String msg){

        Log.e(TAG,"error: >> "+msg);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void playSong() {
        try {
            if (player.isPlaying()) {
                player.stop();
                player.release();
                player = new MediaPlayer();
                return;
            }

            AssetFileDescriptor descriptor = getAssets().openFd("song.mp3");
            player.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();

            player.prepare();
            player.setVolume(1f, 1f);
            player.setLooping(true);
            player.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
