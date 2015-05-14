
LOCAL_PATH := $(call my-dir)
LOCAL_CERTIFICATE := platform
include $(CLEAR_VARS)

APP_ABI := armeabi armeabi-v7a x86
LOCAL_CFLAGS = -Wno-psabi
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_LDLIBS += -L$(SYSROOT)/usr/lib -llog
LOCAL_CPP_EXTENSION:=.cpp

LOCAL_MODULE    := GoodixTools
LOCAL_SRC_FILES := jniNative.cpp \
goodixLink.cpp\
generic.cpp\

include $(BUILD_SHARED_LIBRARY)

