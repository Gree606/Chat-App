package android.example.vgchatapp;

import android.Manifest;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private ListView chatListView;
    private EditText messageEditText;
    private ImageButton recordButton;
    MediaRecorder recorder;
    MediaPlayer player;
    String recordFile;
    String outAudFile;
    Boolean mpStat = false;
    Spinner spinner_in;
    private ChatAdapter adapter;
    private String[] messages = {"hello", "how are you", "I'm your chatbot Lisa", "I'm here to help with general queries", "Tell me what you would like to do today", "Or if you have a query", "Ask your question", "Or just give yes if you'd like to connect to our customer care"};
    ImageView progressBar;
//    AnimationDrawable animationDrawable;
    String lang;
    String fileName;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        outAudFile = getApplicationContext().getExternalCacheDir().getAbsolutePath() + "/vgOutSpeech.3gp";

        spinner_in = findViewById(R.id.spinner_input_languages);
        ArrayAdapter<String> myInAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.inlangs));
        myInAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinner_in.setAdapter(myInAdapter);

        recordFile = getApplicationContext().getExternalCacheDir().getAbsolutePath() + "/inAudFile.3gp";

        chatListView = findViewById(R.id.chatListView);
        recordButton = (ImageButton) findViewById(R.id.recordButton);

        adapter = new ChatAdapter(this, R.layout.list_item_layout, R.id.messageTextView);
        chatListView.setAdapter(adapter);

        progressBar = findViewById(R.id.gifImageView);
        progressBar.setBackgroundResource(R.drawable.bubble_pb);
//        animationDrawable = (AnimationDrawable) progressBar.getBackground();


        ///////////////////////////////////////////Recorder Options////////////////////////////////////////////

        recordButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        recorder = new MediaRecorder();
                        if (mpStat == true) {
                            if (player.isPlaying()) {
                                player.stop();
                                player.release();
//                                player = new MediaPlayer();
                                mpStat = true;
                            }
                        }
                        startRecording();
                        recordButton.setBackgroundResource(R.drawable.recordicon3);
                        recordButton.setScaleX(1.5f);
                        recordButton.setScaleY(1.5f);
                        break;

                    case MotionEvent.ACTION_MOVE:
                        // touch move code
                        break;

                    case MotionEvent.ACTION_UP:
                        stopRecording();
                        recordButton.setBackgroundResource(R.drawable.recordicon2);
                        recordButton.setScaleX(1);
                        recordButton.setScaleY(1);
//                            PlayAudio pA=new PlayAudio();
//                        try {
//                            pA.playAudio(recordFile);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
                        progressBar.setVisibility(View.VISIBLE);
//                        animationDrawable.start();
                        sttToActivity(recordFile);
                        //Implementing Speech to text
                        break;
                }
                return true;
            }
        });

    }

    ///////////////////////////////////Recorder Functions////////////////////////////////////////
    public void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(recordFile);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e("Recorder Error", "prepare() failed");
        }

        recorder.start();
    }

    public void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    public void sttToActivity(String fName) {
        lang=spinner_in.getSelectedItem().toString().toLowerCase();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                                // No implementation needed
                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                                // No implementation needed
                            }

                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }
                        }
                };

                SSLContext sslContext = null;
                try {
                    sslContext = SSLContext.getInstance("TLS");
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                try {
                    sslContext.init(null, trustAllCerts, new SecureRandom());
                } catch (KeyManagementException e) {
                    e.printStackTrace();
                }

                Log.e("FileName:","file in:"+recordFile);
                OkHttpClient client = new OkHttpClient.Builder()
                        .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                        .hostnameVerifier((hostname, session) -> true)
                        .connectTimeout(2, TimeUnit.MINUTES) // connect timeout
                        .writeTimeout(2, TimeUnit.MINUTES) // write timeout3
                        .readTimeout(2, TimeUnit.MINUTES)
                        .build();
                MediaType mediaType = MediaType.parse("text/plain");
                RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("file", recordFile,
                                RequestBody.create(new File(recordFile), MediaType.parse("application/octet-stream")))
                        .addFormDataPart("wheelchair_id", null,
                                RequestBody.create(("0").getBytes(), MediaType.parse("application/json")))
                        .addFormDataPart("source_language", null,
                                RequestBody.create((lang).getBytes(), MediaType.parse("application/json")))
                        .addFormDataPart("target_language", null,
                                RequestBody.create((lang).getBytes(), MediaType.parse("application/json")))
                        .build();
                Request request = new Request.Builder()
                        .url(getResources().getString(R.string.vgUrl) + "/api")
                        .method("POST", body)
                        .build();
                try {
                    Response response = client.newCall(request).execute();
                    String jsonData = response.body().string();
                    Log.e("Response","Response is:"+response.toString());
                    JSONObject jObj = null;
                    try {
                        jObj = new JSONObject(jsonData);
                        String resAudio = jObj.getString("audio");
                        String caption = jObj.getString("caption");
                        String userSpeech = jObj.getString("stt");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.INVISIBLE);
//                                animationDrawable.stop();

                                adapter.setIsUser(true);
                                adapter.add(userSpeech);
                                chatListView.setSelection(adapter.getCount() - 1);

                                adapter.setIsUser(false);
                                adapter.add(caption);
                                chatListView.setSelection(adapter.getCount() - 1);
                            }
                        });
                        Log.e("Response success", resAudio);
                        byte[] decodedBytes = Base64.getDecoder().decode(resAudio);
                        saveAudio(decodedBytes);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("ResponseFail", "Check if API is returning value");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.INVISIBLE);
                                adapter.setIsUser(false);
                                adapter.add("Failed to get response");
                            }
                        });
                            /*runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "Response Json error", Toast.LENGTH_LONG).show();
                                }});*/
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("InputError",e.toString());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.INVISIBLE);
                            adapter.setIsUser(false);
                            adapter.add("Failed to get response, please check i/p file");
                            chatListView.setSelection(adapter.getCount() - 1);
                        }
                    });
                }

            }
        });
        thread.start();
    }

    public void saveAudio(byte[] audioFile) {

        outAudFile = getApplicationContext().getExternalCacheDir().getAbsolutePath() + "/vgOutSpeech";
        OutputStream os;
        try {
            os = new FileOutputStream(new File(outAudFile));
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream outFile = new DataOutputStream(bos);
            outFile.write(audioFile);
            outFile.flush();
            outFile.close();
        } catch (IOException e) {
            //Toast.makeText(context, "Add audio file", Toast.LENGTH_LONG).show();
            Log.e("TAG", "failed to get audio output");
        }
        playAudio();
    }


    public void playAudio() {
        try {
            if (mpStat == true) {
                if (player.isPlaying()) {
                    player.stop();
                    player.release();
                }
            }
            player = new MediaPlayer();
            player.setDataSource(outAudFile);
            Log.e("save path", outAudFile);
            player.prepare();
            while (!player.isPlaying()) {
                player.start();
                mpStat = true;
//            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//                @Override
//                public void onCompletion(MediaPlayer mediaPlayer) {
//                        mp.stop();
//                        mp.release();
//                    }
//                });
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("TAG", "prepare() failed for output audio");
        }
    }
}


