/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <linux/i2c.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include "jni.h"
#include "tool.h"

#define classPathName "com/jni/GoodixJNI"

static jobject gObject;

static JavaVM* jvm=0;

void setJavaVM(JavaVM* javaVM)
{
    jvm = javaVM;
}

JavaVM* getJavaVM()
{
    return jvm;
}

JNIEnv* getJNIEnv()
{
    union {
        JNIEnv* env;
        void* dummy;
    }u;
    jint jniError = 0;

    jniError = getJavaVM()->AttachCurrentThread(&u.env, 0);
    if(jniError == JNI_OK){
  	return u.env;
    }
    else{
    	DEBUG("getJNIEnv is error.");
    }
}

static  jint I2CReadRegister
  (JNIEnv *env, jclass thiz , jbyteArray data, jint len, jint addr )
{
	s32 ret = -1;
	u8 temp[len];
	ret=read_register(temp,len,addr);
	if(ret>0)
	{
		DEBUG_ARRAY(temp,len);
	}
	env->SetByteArrayRegion(data,0, len, (jbyte*)temp);
	//DEBUG("JNI_i2c_read_data:%d",ret);
	return ret;
}

static  jint I2CWriteRegister
  (JNIEnv *env, jclass thiz  , jbyteArray data,jint len, jint add)
{
	jint ret=-1;
	u8* writeData = (u8*)env->GetByteArrayElements( data, 0);
	ret=write_register(writeData, len,add);
	if(ret>0)
	{
		DEBUG_ARRAY(writeData,len);
	}
	env->ReleaseByteArrayElements( data,(jbyte *)writeData,0);
	return ret;
}

static void delay
  (JNIEnv *env, jclass thiz, jint ms)
{
	usleep(ms*1000);
}

static jint reset_guitar(JNIEnv *env, jclass thiz)
{
	return guitar_reset();
}

static jint downLinkCode(JNIEnv *env, jclass thiz)
{
	return download_link_code();
}

static jbyte getCheckSm
	(JNIEnv *env, jclass thiz,jbyteArray data,jint len)
{
	u8 ret = -1;
	u8 *temp = (u8*)env->GetByteArrayElements(data, 0);
	ret = calculate_check_sum(temp,len);
	env->ReleaseByteArrayElements(data,(jbyte *)temp,0);
	DEBUG("cal culate check sum is %d",ret);
	return ret ;
}

static jboolean checkCRC
(JNIEnv *env, jclass thiz, jbyteArray buf, jint len, jbyte rcvlen)
{
	 bool ret = false;
	 u8 *temp = (u8*)env->GetByteArrayElements(buf, 0);
	 ret = check_crc(temp,len,rcvlen);
	 env->ReleaseByteArrayElements(buf,(jbyte *)temp,0);
	 return ret ;
}

static jboolean readPairState(JNIEnv *env, jclass thiz)
{
    return read_pair_state();
}

static jboolean clearPairBuf (JNIEnv *env, jclass thiz)
{
    return clear_pair_buf();
}

static jboolean enterSlaveMode(JNIEnv *env, jclass thiz)
{
    return enter_slave_mode();
}

static jboolean enterMasterMode(JNIEnv *env, jclass thiz)
{
    return enter_master_mode();
}

static jboolean enterTransferMode(JNIEnv *env, jclass thiz)
{
    return enter_transfer_mode();
}

static jboolean exitSlaveMode(JNIEnv *env, jclass thiz)
{
    return exit_slave_mode();
}

static jboolean exitMasterMode(JNIEnv *env, jclass thiz)
{
    return exit_master_mode();
}

static jboolean exitTransferMode(JNIEnv *env, jclass thiz)
{
    return exit_transfer_mode();
}

static jboolean sendData(JNIEnv *env, jclass thiz, jbyteArray buf, jint length)
{
    bool ret = false;
    u8 *jbuf = (u8*)env->GetByteArrayElements(buf, 0);
    ret = send_data(jbuf, length);
    env->ReleaseByteArrayElements(buf,(jbyte *)jbuf,0);
    return ret ;
}

static jint receiveData(JNIEnv *env, jclass thiz, jbyteArray buf)
{
    int ret = 0;
    u8 *jbuf = (u8*)env->GetByteArrayElements(buf, 0);
    ret = receive_data(jbuf);
    env->ReleaseByteArrayElements(buf,(jbyte *)jbuf,0);
    return ret;    
}

static jboolean CheckAuthorization(JNIEnv *env, jclass thiz)
{
	return check_authorization();
}
static JNINativeMethod methods[] = {
  {"I2CWriteRegister","([BII)I",(void*)I2CWriteRegister},
  {"I2CReadRegister","([BII)I",(void*)I2CReadRegister},
  {"delay","(I)V",(void*)delay},
  {"GuitarReset","()I",(void*)reset_guitar},
  {"downLinkCode","()I",(void*)downLinkCode},
  {"getCheckSm","([BI)B",(void*)getCheckSm},
  {"checkCRC","([BIB)Z",(void*)checkCRC},
  {"readPairState", "()Z", (void*)readPairState},
  {"clearPairBuf", "()Z", (void*)clearPairBuf},
  {"enterSlaveMode", "()Z", (void*)enterSlaveMode},
  {"enterMasterMode", "()Z", (void*)enterMasterMode},
  {"enterTransferMode", "()Z", (void*)enterTransferMode},
  {"exitSlaveMode", "()Z", (void*)exitSlaveMode},
  {"exitMasterMode", "()Z", (void*)exitMasterMode},
  {"exitTransferMode", "()Z", (void*)exitTransferMode},
  {"sendData", "([BI)Z", (void*)sendData},
  {"receiveData", "([B)I", (void*)receiveData},
  {"CheckAuthorization", "()Z", (void*)CheckAuthorization},
};

/*
 * Register several native methods for one class.
 */
static int registerNativeMethods(JNIEnv* env, const char* className,
    JNINativeMethod* gMethods, int numMethods)
{
    jclass clazz;

    clazz = env->FindClass(className);
    if (clazz == NULL) {
    	DEBUG("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
    	DEBUG("RegisterNatives failed for '%s'", className);
    	env->DeleteLocalRef(clazz);
        return JNI_FALSE;
    }
    env->DeleteLocalRef(clazz);
    return JNI_TRUE;
}

/*
 * Register native methods for all classes we know about.
 *
 * returns JNI_TRUE on success.
 */
static int registerNatives(JNIEnv* env)
{
  if (!registerNativeMethods(env, classPathName,
                 methods, sizeof(methods) / sizeof(methods[0]))) {
    return JNI_FALSE;
  }

  return JNI_TRUE;
}


// ----------------------------------------------------------------------------

/*
 * This is called by the VM when the shared library is first loaded.
 */

typedef union {
    JNIEnv* env;
    void* venv;
} UnionJNIEnvToVoid;

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    UnionJNIEnvToVoid uenv;
    uenv.venv = NULL;
    jint result = -1;
    JNIEnv* env = NULL;
#ifdef FILE_DEBUG
			log_fp = fopen(LOG_FILE_PATH, "w+");
#endif

    DEBUG("JNI_OnLoad");

    setJavaVM(vm);

    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK) {
    	DEBUG("ERROR: GetEnv failed");
        goto bail;
    }
    env = uenv.env;

    if (registerNatives(env) != JNI_TRUE) {
    	DEBUG("ERROR: registerNatives failed");
        goto bail;
    }

    result = JNI_VERSION_1_4;

bail:
    return result;
}
