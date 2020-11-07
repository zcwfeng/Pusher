cmake_minimum_required(VERSION 3.4.1)
project("pusher")
include_directories(${CMAKE_SOURCE_DIR}/librtmp)
add_subdirectory(librtmp)
add_library(
        native-lib
        SHARED
        native-lib.cpp
        JavaCallHelper.cpp
        VideoChannel.cpp
        AudioChannel.cpp
)

include_directories(${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/include)
include_directories(${CMAKE_SOURCE_DIR}/faac/${ANDROID_ABI}/include)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/lib")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/faac/${ANDROID_ABI}/lib")
#推流
target_link_libraries(
        native-lib
        x264
        faac
        rtmp
        log)


#图像处理
include_directories(${CMAKE_SOURCE_DIR}/libyuv/include)

add_subdirectory(libyuv)

add_library(
        ImageUtils
        SHARED
        ImageUtils.cpp
)

target_link_libraries(
        ImageUtils
        yuv)