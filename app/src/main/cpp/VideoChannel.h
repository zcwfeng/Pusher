//
// Created by 张传伟 on 2020/11/2.
//

#ifndef PUSHER_VIDEOCHANNEL_H
#define PUSHER_VIDEOCHANNEL_H

#include <x264.h>
#include "Callback.h"

/**
 * 封装x264的编码工作
 */
class VideoChannel {
public:
    ~VideoChannel();

    VideoChannel();

    void encode(uint8_t *data);

public:
    void openCodec(int widh, int height, int fps, int bitrate);

    void setCallBack(Callback callback);

    void resetPts() { i_pts = 0; }

private:
    x264_t *codec = 0;
    int ySize;
    int uSize;
    int64_t i_pts = 0;

    void sendVideoConfig(uint8_t *sps, uint8_t *pps, int spslen, int ppslen);

    void sendFrame(int type, uint8_t *payload, int payload1);

    Callback callback;

    int height;
    int width;
};


#endif //PUSHER_VIDEOCHANNEL_H
