package top.zcwfeng.pusher;

import android.os.Bundle;
import android.view.TextureView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity{


    private TextureView textureView;
    private RtmpClient rtmpClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textureView = findViewById(R.id.textureview);
        rtmpClient = new RtmpClient(this);
        rtmpClient.initVideo(textureView,360, 480, 10, 640_000);
        rtmpClient.initAudio(44100,2);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        rtmpClient.release();
    }

    public void toggleCamera(View view){
        rtmpClient.toggleCamera();
    }

    public void startLive(View view){
        rtmpClient.startLive("rtmp://192.168.31.40:1935/myapp/zcw");
    }

    public void stopLive(View view){
        rtmpClient.stopLive();
    }


}