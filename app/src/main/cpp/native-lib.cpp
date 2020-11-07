#include <jni.h>
#include <string>
#include <pthread.h>
#include <rtmp.h>
#include "JavaCallHelper.h"
#include "VideoChannel.h"
#include "AudioChannel.h"
#include <android/log.h>

VideoChannel *videoChannel = 0;
AudioChannel *audioChannel = 0;
JavaVM *javaVm = 0;
char *path = 0;
RTMP *rtmp = 0;
JavaCallHelper *helper = 0;
uint64_t startTime;
pthread_mutex_t mutex;//互斥量
pthread_t pid;

/**
 * 回调，类似java
 * @param packet
 */
void callback(RTMPPacket *packet) {

    if (rtmp) {
        packet->m_nInfoField2 = rtmp->m_stream_id;
        packet->m_nTimeStamp = RTMP_GetTime() - startTime;
        //1. 放到队列中
        RTMP_SendPacket(rtmp, packet, 1);
    }
    RTMPPacket_Free(packet);
    delete (packet);
}

void *connect(void *args) {
    int ret;
    rtmp = RTMP_Alloc();// 申请堆内存
    RTMP_Init(rtmp);

    do {
        ret = RTMP_SetupURL(rtmp, path);
        if (!ret) {
            //TODO: 通知java地址传入的有问题
//            __android_log_print(ANDROID_LOG_ERROR, "X264", "%s,%s", "RTMP_SetupURL", path);
            break;
        }
        // 打开输出模式，这里推流的时候.（拉流的时候可以不用开启）
        RTMP_EnableWrite(rtmp);
        ret = RTMP_Connect(rtmp, 0);
        if (!ret) {
            //TODO: 通知java服务器链接失败
            __android_log_print(ANDROID_LOG_ERROR, "X264", "通知java服务器链接失败,%s",
                                "RTMP_Connect");

            break;
        }

        ret = RTMP_ConnectStream(rtmp, 0);
        if (!ret) {
            //TODO: 通知java未连接到流（相当于握手失败）
            __android_log_print(ANDROID_LOG_ERROR, "X264",
                                "通知java未连接到流（相当于握手失败）,%s", "RTMP_ConnectStream");

            break;
        }
        // 发送audio specific config (告诉播放器怎么解码推流的音频)
        RTMPPacket *packet = audioChannel->getAudioConfig();
        callback(packet);
    } while (false);

    if (!ret) {
        if (rtmp) {
            RTMP_Close(rtmp);
            RTMP_Free(rtmp);
            rtmp = 0;
        }

    }
    delete (path);
    path = 0;
    // 通知java可以开始推流了，（在子线程通知Java）
    helper->onPrepare(ret);
    startTime = RTMP_GetTime();
    return 0;
}


JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVm = vm;
    return JNI_VERSION_1_4;
}
//JNIEXPORT void JNI_OnUnload(JavaVM* vm, void* reserved){
//    vm->DetachCurrentThread();
//    vm = 0;
//}

extern "C"
JNIEXPORT void JNICALL
Java_top_zcwfeng_pusher_RtmpClient_nativeInit(JNIEnv *env, jobject thiz) {
    helper = new JavaCallHelper(javaVm, env, thiz);
    pthread_mutex_init(&mutex, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_top_zcwfeng_pusher_RtmpClient_nativeDeInit(JNIEnv *env, jobject thiz) {
    if (helper) {
        delete (helper);
        helper = 0;
    }
    pthread_mutex_destroy(&mutex);
}

extern "C"
JNIEXPORT void JNICALL
Java_top_zcwfeng_pusher_RtmpClient_connect(JNIEnv *env, jobject thiz, jstring url_) {
    const char *url = env->GetStringUTFChars(url_, 0);
    // 因为你在这里并不知道调用的是java的主线程还是子线程，所以需要JavaVM ,env 绑定线程
    path = new char[strlen(url) + 1];
    strcpy(path, url);
    // 启动子线程url
    pthread_create(&pid, 0, connect, 0);
    env->ReleaseStringUTFChars(url_, url);
}

extern "C"
JNIEXPORT void JNICALL
Java_top_zcwfeng_pusher_RtmpClient_disConnect(JNIEnv *env, jobject thiz) {
//    主线程
    pthread_mutex_lock(&mutex);
    if (rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
        rtmp = 0;
    }
    if (videoChannel) {
        videoChannel->resetPts();// 停止直播重置
    }
    pthread_mutex_unlock(&mutex);
}



extern "C"
JNIEXPORT void JNICALL
Java_top_zcwfeng_pusher_RtmpClient_nativeSendVideo(JNIEnv *env, jobject thiz, jbyteArray buffer) {
    jbyte *data = env->GetByteArrayElements(buffer, 0);
    pthread_mutex_lock(&mutex);
    //x264编码,编码和推流
    videoChannel->encode(reinterpret_cast<uint8_t *>(data));

    pthread_mutex_unlock(&mutex);
    env->ReleaseByteArrayElements(buffer, data, 0);

}

extern "C"
JNIEXPORT void JNICALL
Java_top_zcwfeng_pusher_RtmpClient_initVideoEnc(JNIEnv *env, jobject thiz, jint width, jint height,
                                                jint fps, jint bit_rate) {
//准备好编码器，单独封装VideoChannel 封装x264操作
    videoChannel = new VideoChannel;
    videoChannel->openCodec(width, height, fps, bit_rate);
    videoChannel->setCallBack(callback);

}

extern "C"
JNIEXPORT void JNICALL
Java_top_zcwfeng_pusher_RtmpClient_releaseVideoEnc(JNIEnv *env, jobject thiz) {
    if (videoChannel) {
        delete (videoChannel);
        videoChannel = 0;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_top_zcwfeng_pusher_RtmpClient_nativeSendAudio(JNIEnv *env, jobject thiz, jbyteArray buffer,
                                                   jint len) {
    jbyte *data = env->GetByteArrayElements(buffer, 0);


    pthread_mutex_lock(&mutex);
    audioChannel->encode(reinterpret_cast<int32_t *>(data), len);
    pthread_mutex_unlock(&mutex);

    env->ReleaseByteArrayElements(buffer, data, 0);
}


extern "C"
JNIEXPORT jint JNICALL
Java_top_zcwfeng_pusher_RtmpClient_initAudioEnc(JNIEnv *env, jobject thiz, jint sample_rate,
                                                jint channels) {
//准备好编码器，单独封装VideoChannel 封装x264操作
    audioChannel = new AudioChannel;
    audioChannel->setCallback(callback);
    audioChannel->openCodec(sample_rate, channels);
    return audioChannel->getInputByteNum();
}
extern "C"
JNIEXPORT void JNICALL
Java_top_zcwfeng_pusher_RtmpClient_releaseAudioEnc(JNIEnv *env, jobject thiz) {
    if (audioChannel) {
        delete (audioChannel);
        audioChannel = 0;
    }

}
