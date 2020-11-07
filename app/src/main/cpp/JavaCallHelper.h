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

    void onPrepare(bool isConnect,int thread = THREAD_CHILD);

public:
    JavaVM *javaVM;
    JNIEnv *env;
    jobject jobj;
    jmethodID jmid_prepare;
};


#endif //ZCWPLAYER_JAVACALLHELPER_H
