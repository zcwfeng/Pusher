//
// Created by 张传伟 on 2020/10/23.
//

#include "JavaCallHelper.h"


JavaCallHelper::JavaCallHelper(JavaVM *_javaVm, JNIEnv *_env, jobject &_jobj): javaVM(_javaVm), env(_env){
    jobj = env->NewGlobalRef(_jobj);
    jclass jclazz = env->GetObjectClass(jobj);

    jmid_error = env->GetMethodID(jclazz,"onError","(I)V");
    jmid_prepare = env->GetMethodID(jclazz,"onPrepare","()V");
    jmid_progress = env->GetMethodID(jclazz,"onProgress","(I)V");

}

JavaCallHelper::~JavaCallHelper() {
    env->DeleteGlobalRef(jobj);
    jobj = 0;
}
// 如果是主线程直接调用，如果是子线程，必须AttachCurrentThread当前线程的env绑定
void JavaCallHelper::onError(int code, int thread) {
    if (thread == THREAD_CHILD) {
        //子线程
        JNIEnv *jniEnv;
        if (javaVM->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
            return;
        }
        jniEnv->CallVoidMethod(jobj, jmid_error, code);
        javaVM->DetachCurrentThread();
    } else {
        env->CallVoidMethod(jobj, jmid_error, code);
    }

}

void JavaCallHelper::onPrepare(int thread) {
    if (thread == THREAD_CHILD) {
        JNIEnv *jniEnv;
        if (javaVM->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
            return;
        }
        jniEnv->CallVoidMethod(jobj, jmid_prepare);
        javaVM->DetachCurrentThread();
    } else {
        env->CallVoidMethod(jobj, jmid_prepare);
    }
}

void JavaCallHelper::onProgress(int progress, int thread) {
    if (thread == THREAD_CHILD) {
        JNIEnv *jniEnv;
        if (javaVM->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
            return;
        }
        jniEnv->CallVoidMethod(jobj, jmid_progress, progress);
        javaVM->DetachCurrentThread();
    } else {
        env->CallVoidMethod(jobj, jmid_progress, progress);
    }
}