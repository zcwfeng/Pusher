//
// Created by 张传伟 on 2020/10/23.
//

#ifndef ZCWPLAYER_JAVACALLHELPER_H
#define ZCWPLAYER_JAVACALLHELPER_H
#include <jni.h>

//标记线程 因为子线程需要attach
#define THREAD_MAIN 1
#define THREAD_CHILD 2


class JavaCallHelper {
public:
    JavaCallHelper(JavaVM *_javaVm, JNIEnv *_env, jobject &_jobj);
    ~JavaCallHelper();

    void onError(int code,int thread = THREAD_MAIN);
    void onPrepare(int thread = THREAD_MAIN);
    void onProgress(int progress,int thread = THREAD_MAIN);

public:
    JavaVM *javaVM;
    JNIEnv *env;
    jobject jobj;
    jmethodID jmid_error;
    jmethodID jmid_prepare;
    jmethodID jmid_progress;
};


#endif //ZCWPLAYER_JAVACALLHELPER_H
