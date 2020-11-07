package top.zcwfeng.pusher;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Size;

import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

public class ImageUtils {

    static {
        System.loadLibrary("ImageUtils");
    }

    static byte[] scaleBytes;
    static ByteBuffer yuv420;

    public static byte[] getBytes(ImageProxy image, int rotationDegrees, int width, int height) {
        //图像格式
//        int format = image.getFormat();
        // https://developer.android.google.cn/training/camerax/analyze
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            //抛出异常
            throw new IllegalStateException("根据文档，CameraX返回的是YUV420");
        }
        // TODO: 2020/11/1 解决了内存抖动
        // 3个元素 0：Y，1：U，2：V
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        // byte[]
        int size = image.getWidth() * image.getHeight() * 3 / 2;
        if (yuv420 == null || yuv420.capacity() < size) {
            //用于保存获取的I420数据。大小为:y+u+v, width*height + width/2*height/2 + width/2*height/2
            yuv420 = ByteBuffer.allocate(size);
        }
        yuv420.position(0);


        /**
         * Y数据
         */
        //y数据的这个值只能是：1
        ImageProxy.PlaneProxy plane = planes[0];
        //pixelStride = 1 : 取值无间隔
        //pixelStride = 2 : 间隔1个字节取值
        // y的此数据应该都是1
        int pixelStride = plane.getPixelStride();
        //大于等于宽， 表示连续的两行数据的间隔
        //  如：640x480的数据，
        //  可能得到640
        //  可能得到650，表示每行最后10个字节为补位的数据
        int rowStride = plane.getRowStride();
        ByteBuffer buffer = plane.getBuffer();

        //1、rowStride 等于Width ，那么就是一个空数组
        //2、rowStride 大于Width ，那么就是每行多出来的数据大小个byte
        byte[] skipRow = new byte[rowStride - image.getWidth()];
        byte[] row = new byte[image.getWidth()];
        for (int i = 0; i < image.getHeight(); i++) {
            buffer.get(row);
            yuv420.put(row);
            // 不是最后一行才有无效占位数据，最后一行因为后面跟着U 数据，没有无效占位数据，不需要丢弃
            if (i < image.getHeight() - 1) {
                buffer.get(skipRow);
            }
        }

        /**
         * U、V
         */
        for (int i = 1; i < 3; i++) {
            plane = planes[i];
            pixelStride = plane.getPixelStride();
            // uv数据的rowStride可能是
            // 如：640的宽
            // 可能得到320， pixelStride 为1
            // 可能大于320同时小于640，有为了补位的无效数据  pixelStride 为1
            // 可能得到640 uv数据在一起，pixelStride为2
            // 可能大于640，有为了补位的无效数据 pixelStride为2
            rowStride = plane.getRowStride();
            buffer = plane.getBuffer();

            //每次处理一行数据
            int uvWidth = image.getWidth() / 2;
            int uvHeight = image.getHeight() / 2;

            // 一次处理一个字节
            for (int j = 0; j < uvHeight; j++) {
                for (int k = 0; k < rowStride; k++) {
                    //最后一行
                    if (j == uvHeight - 1) {
                        //uv没混合在一起
                        if (pixelStride == 1) {
                            //rowStride ：大于等于Width/2
                            // 结合外面的if：
                            //  如果是最后一行，我们就不管结尾的占位数据了
                            if (k >= uvWidth) {
                                break;
                            }
                        } else if (pixelStride == 2) {
                            //uv混在了一起
                            // rowStride：大于等于 Width
                            // TODO: 2020/10/31  uv混合
                            // 有的手机是 uvuv 的混合
                            // planes[2]:uvu
                            // planes[3]:vuv
                            if (k >= image.getWidth() - 1) {
                                break;
                            }
                        }
                    }

                    byte b = buffer.get();
                    // uv没有混合在一起
                    if (pixelStride == 2) {
                        // uv混合在一起了
                        //1、偶数位下标的数据是我们本次要获得的U/V数据
                        //2、占位无效数据要丢弃，不保存
                        if (k < image.getWidth() && k % 2 == 0) {
                            yuv420.put(b);
                        }
                    } else if (pixelStride == 1) {
                        if (k < uvWidth) {
                            yuv420.put(b);
                        }
                    }

                }
            }
        }

        int srcWidth = image.getWidth();
        int srcHeight = image.getHeight();
        //I420
        byte[] result = yuv420.array();

        if (rotationDegrees == 90 || rotationDegrees == 270) {
            //旋转之后 ，图像宽高交换
            // TODO: 2020/11/1 result 修改值，避免内存抖动
            rotation(result, image.getWidth(), image.getHeight(), rotationDegrees);
            srcWidth = image.getHeight();
            srcHeight = image.getWidth();
        }

        if (srcWidth != width || srcHeight != height) {
            // TODO: 2020/11/1 jni对scalBytes 修改值，避免内存抖动
            int scaleSize = width * height * 3 / 2;
            if (scaleBytes == null || scaleBytes.length < scaleSize) {
                scaleBytes = new byte[scaleSize];
            }
            scale(result, scaleBytes, srcWidth, srcHeight, width, height);
            return scaleBytes;
        }

        return result;
    }

    private static native void rotation(byte[] data, int width, int height, int degress);

    private static native void scale(byte[] src, byte[] dst, int srcWidth, int srcHeight, int dstWidth, int dstHeight);

    public static Size[] getSupportSize(Context context) {
        CameraManager cm = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : cm.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cm.getCameraCharacteristics(cameraId);
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                return map.getOutputSizes(ImageFormat.YUV_420_888);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

}