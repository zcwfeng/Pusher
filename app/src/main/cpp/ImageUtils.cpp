//
// Created by 张传伟 on 2020/10/31.
//

#include <jni.h>
#include <libyuv.h>

extern "C"
JNIEXPORT void

JNICALL
Java_top_zcwfeng_pusher_ImageUtils_rotation(JNIEnv *env, jclass clazz, jbyteArray data_,
                                            jint width, jint height, jint degress) {

    jbyte *data = env->GetByteArrayElements(data_, 0);
    uint8_t *src = reinterpret_cast<uint8_t *>(data);
    int ySize = width * height;
    int uSize = (width >> 1) * (height >> 1);
    int size = (ySize * 3) >> 1;
    uint8_t *src_y = src;
    uint8_t *src_u = src + ySize;
    uint8_t *src_v = src + ySize + uSize;

    uint8_t dst[size];
    uint8_t *dst_y = dst;
    uint8_t *dst_u = dst + ySize;
    uint8_t *dst_v = dst + ySize + uSize;
    libyuv::I420Rotate(src_y, width, src_u, width >> 1, src_v, width >> 1,
                       dst_y, height, dst_u, height >> 1, dst_v, height >> 1,
                       width, height, static_cast<libyuv::RotationMode>(degress));


    jbyteArray result = env->NewByteArray(size);
    env->SetByteArrayRegion(result, 0, size, reinterpret_cast<const jbyte *>(dst));

    env->ReleaseByteArrayElements(data_, data, 0);
    env->SetByteArrayRegion(data_, 0, size, reinterpret_cast<const jbyte *>(dst));

}
extern "C"
JNIEXPORT void JNICALL
Java_top_zcwfeng_pusher_ImageUtils_scale(JNIEnv *env, jclass clazz, jbyteArray src_,
                                         jbyteArray dst_,
                                         jint src_width, jint src_height, jint dst_width,
                                         jint dst_height) {
    jbyte *data = env->GetByteArrayElements(src_, 0);
    uint8_t *src = reinterpret_cast<uint8_t *>(data);
    int64_t size = (dst_width * dst_height * 3) >> 1;
    uint8_t dst[size];


    uint8_t *src_y;
    int src_stride_y;
    uint8_t *src_u;
    int src_stride_u;
    uint8_t *src_v;
    int src_stride_v;

    uint8_t *dst_y;
    int dst_stride_y;
    uint8_t *dst_u;
    int dst_stride_u;
    uint8_t *dst_v;
    int dst_stride_v;

    src_stride_y = src_width;
    src_stride_u = src_width >> 1;
    src_stride_v = src_stride_u;

    dst_stride_y = dst_width;
    dst_stride_u = dst_width >> 1;
    dst_stride_v = dst_stride_u;


    int src_y_size = src_width * src_height;
    int src_u_size = src_stride_u * (src_height >> 1);
    src_y = src;
    src_u = src + src_y_size;
    src_v = src + src_y_size + src_u_size;

    int dst_y_size = dst_width * dst_height;
    int dst_u_size = dst_stride_u * (dst_height >> 1);
    dst_y = dst;
    dst_u = dst + dst_y_size;
    dst_v = dst + dst_y_size + dst_u_size;


    libyuv::I420Scale(src_y, src_stride_y,
                      src_u, src_stride_u,
                      src_v, src_stride_v,
                      src_width, src_height,
                      dst_y, dst_stride_y,
                      dst_u, dst_stride_u,
                      dst_v, dst_stride_v,
                      dst_width, dst_height,
                      libyuv::FilterMode::kFilterNone
    );


    env->ReleaseByteArrayElements(src_, data, 0);
    env->SetByteArrayRegion(dst_, 0, size, reinterpret_cast<const jbyte *>(dst));
}