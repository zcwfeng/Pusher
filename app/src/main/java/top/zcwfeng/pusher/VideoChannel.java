package top.zcwfeng.pusher;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.TextureView;
import android.view.ViewGroup;

import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.lifecycle.LifecycleOwner;


public
class VideoChannel implements Preview.OnPreviewOutputUpdateListener, ImageAnalysis.Analyzer {
    private TextureView displayer;
    private HandlerThread handlerThread;
    private CameraX.LensFacing currentFacing = CameraX.LensFacing.BACK;
    private RtmpClient rtmpClient;
    private LifecycleOwner lifecycleOwner;

    public VideoChannel(TextureView displayer, RtmpClient rtmpClient, LifecycleOwner lifecycleOwner) {
        this.displayer = displayer;
        this.rtmpClient = rtmpClient;
        this.lifecycleOwner = lifecycleOwner;
        // 子线成回调
        handlerThread = new HandlerThread("Analyze-thread");
        handlerThread.start();
        CameraX.bindToLifecycle(lifecycleOwner, getPreview(), getAnalysis());
    }

    private Preview getPreview() {
//        分辨率并不是最终的分辨率，CameraX会自动根据设备的支持情况，结合你的参数，设置一个最为接近的分辨率
        PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setTargetResolution(new Size(rtmpClient.getWidth(), rtmpClient.getHeight()))
                .setLensFacing(currentFacing)
                .build();
        Preview preview = new Preview(previewConfig);
        preview.setOnPreviewOutputUpdateListener(this);
        return preview;
    }

    private ImageAnalysis getAnalysis() {
        ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
                .setCallbackHandler(new Handler(handlerThread.getLooper()))
                .setLensFacing(currentFacing)
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .setTargetResolution(new Size(rtmpClient.getWidth(), rtmpClient.getHeight()))
                .build();
        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
        imageAnalysis.setAnalyzer(this);
        return imageAnalysis;
    }


    @Override
    public void onUpdated(Preview.PreviewOutput output) {
        SurfaceTexture surfaceTexture = output.getSurfaceTexture();
        if (displayer != null && displayer.getSurfaceTexture() != surfaceTexture) {
            if(displayer.isAvailable()){
                // 当切换摄像头会报错
                ViewGroup parent = (ViewGroup) displayer.getParent();
                parent.removeView(displayer);
                parent.addView(displayer,0);
                parent.requestLayout();
            }
            displayer.setSurfaceTexture(surfaceTexture);
        }
    }

    public void toggleCamera() {
        CameraX.unbindAll();
        if (currentFacing == CameraX.LensFacing.BACK) {
            currentFacing = CameraX.LensFacing.FRONT;
        } else {
            currentFacing = CameraX.LensFacing.BACK;

        }
        CameraX.bindToLifecycle(lifecycleOwner, getPreview(), getAnalysis());

    }

    @Override
    public void analyze(ImageProxy image, int rotationDegrees) {
        if (rtmpClient.isConnected()) {
            byte[] bytes = ImageUtils.getBytes(image, rotationDegrees,
                    rtmpClient.getWidth(), rtmpClient.getHeight());
            rtmpClient.sendVideo(bytes);
        }
    }

    public void release() {
        handlerThread.quitSafely();
    }
}
