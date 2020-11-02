# For more information about using CMake with Android Studio, read the
cmake_minimum_required(VERSION 3.10.2)
project("pusher")
add_library(
        native-lib
        SHARED
        native-lib.cpp)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/lib -L${CMAKE_SOURCE_DIR}/faac/${ANDROID_ABI}/lib")
include_directories(
        ${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/include
        ${CMAKE_SOURCE_DIR}/faac/${ANDROID_ABI}/include
)
#推流
target_link_libraries(
        native-lib
        x264
        faac
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