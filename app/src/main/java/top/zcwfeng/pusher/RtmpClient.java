package top.zcwfeng.pusher;

import android.util.Log;
import android.view.TextureView;

import androidx.lifecycle.LifecycleOwner;

public class RtmpClient {

    private static final String TAG = "RtmpClient";

    static {
        System.loadLibrary("native-lib");
    }

    private LifecycleOwner lifecycleOwner;


    private int width;
    private int height;
    private boolean isConnect;
    private VideoChannel videoChannel;
    private AudioChannel audioChannel;

    public RtmpClient(LifecycleOwner lifecycleOwner) {
        this.lifecycleOwner = lifecycleOwner;
        nativeInit();
    }


    public void initVideo(TextureView displayer, int width, int height, int fps, int bitRate) {
        this.width = width;
        this.height = height;
        videoChannel = new VideoChannel(displayer, this, lifecycleOwner);
        initVideoEnc(width, height, fps, bitRate);
    }

    public void initAudio(int sampleRate,int channels){
        audioChannel = new AudioChannel(sampleRate, channels, this);
        int inputByteNum = initAudioEnc(sampleRate, channels);
        audioChannel.setInputByteNum(inputByteNum);
    }


    public void startLive(String url) {
        connect(url);
    }

    public boolean isConnected() {
        return isConnect;
    }

    public void stopLive() {
        isConnect = false;
        audioChannel.stop();
        disConnect();
        Log.e(TAG, "停止直播==========");
    }

    /**
     * JNICall
     *
     * @param isConnect
     */
    public void onPrepare(boolean isConnect) {
        this.isConnect = isConnect;
        audioChannel.start();
        Log.e(TAG, "开始直播==========");
    }


    public void sendVideo(byte[] buffer) {
        nativeSendVideo(buffer);
    }

    public void sendAudio(byte[] buffer,int len){
        nativeSendAudio(buffer, len);
    }

    public void toggleCamera() {
        videoChannel.toggleCamera();
    }

    public void release() {
        videoChannel.release();
        audioChannel.release();
        releaseVideoEnc();
        releaseAudioEnc();
        nativeDeInit();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }


    private native void nativeInit();

    private native void connect(String url);

    private native void disConnect();

    private native void nativeDeInit();

    private native void nativeSendVideo(byte[] buffer);

    private native void nativeSendAudio(byte[] buffer,int len);

    private native void initVideoEnc(int width, int height, int fps, int bitRate);

    private native int initAudioEnc(int sampleRate,int channels);

    private native void releaseVideoEnc();

    private native void releaseAudioEnc();

}
