package top.zcwfeng.pusher;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements Preview.OnPreviewOutputUpdateListener, ImageAnalysis.Analyzer {


    private TextureView textureView;
    private HandlerThread handlerThread;
    private CameraX.LensFacing currentFacing = CameraX.LensFacing.BACK;
    private RtmpClient rtmpClient;

    private FileOutputStream fos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textureView = findViewById(R.id.textureview);
        // 子线成回调
        handlerThread = new HandlerThread("Analyze-thread");
        handlerThread.start();
        rtmpClient = new RtmpClient(480, 640, 10, 640_000);

        CameraX.bindToLifecycle(this, getPreview(), getAnalysis());
        try {
            fos = new FileOutputStream("/sdcard/a.yuv");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Preview getPreview() {
//        分辨率并不是最终的分辨率，CameraX会自动根据设备的支持情况，结合你的参数，设置一个最为接近的分辨率
        PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setLensFacing(currentFacing)
                .setTargetResolution(new Size(640, 480))
                .build();
        Preview preview = new Preview(previewConfig);
        preview.setOnPreviewOutputUpdateListener(this);
        return preview;
    }

    public void toggleCamera(View view) {
        CameraX.unbindAll();
        if (currentFacing == CameraX.LensFacing.BACK) {
            currentFacing = CameraX.LensFacing.FRONT;
        } else {
            currentFacing = CameraX.LensFacing.BACK;

        }
        CameraX.bindToLifecycle(this, getPreview(), getAnalysis());

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        handlerThread.quitSafely();
        if (fos != null) {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private ImageAnalysis getAnalysis() {
        ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
                .setCallbackHandler(new Handler(handlerThread.getLooper()))
                .setLensFacing(currentFacing)
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .setTargetResolution(new Size(640, 480))
                .build();
        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
        imageAnalysis.setAnalyzer(this);
        return imageAnalysis;
    }


    @Override
    public void onUpdated(Preview.PreviewOutput output) {
        SurfaceTexture surfaceTexture = output.getSurfaceTexture();
        if (textureView != null && textureView.getSurfaceTexture() != surfaceTexture) {
            // 当切换摄像头会报错
            ViewGroup parent = (ViewGroup) textureView.getParent();
            parent.removeView(textureView);
            parent.addView(textureView);
            parent.requestLayout();
        }
        textureView.setSurfaceTexture(surfaceTexture);
    }


    @Override
    public void analyze(ImageProxy image, int rotationDegrees) {



        byte[] bytes = ImageUtils.getBytes(image, rotationDegrees, rtmpClient.getWidth(), rtmpClient.getHeight());
        try {
            if (fos != null)
                fos.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}