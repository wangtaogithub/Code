package com.tools;

import java.util.UUID;
import com.jni.GoodixJNI;
import com.log.LinkLog;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

public class GoodixLink {

	//定义一些公用的变量
	public enum ServerOrClient{
		NONE,
		SERVER,
		CLIENT
	};
	public static ServerOrClient serverOrClient = ServerOrClient.NONE;
	public static Uri sendFileUri = null;   //the path to the file to be transmitted 
	public static String receivedFilePath = null;
	public static boolean isDebugMode = false;
	public static String sendFilePath = null;
	public final static int NONE=0;
	public final static int TRANSFER_START=1;
	public final static int TRANSFER_ING=2;
	public final static int TRANSFER_END=3;
	public final static int ERROR=4;
	public static int runStatus=NONE;

	//定义公用常量
//	public static ExecutorService FULL_TASK_EXECUTOR = null;
    public static final String ACTION_SEND_FILE_WIFI = "com.android.wifidirect.SEND_FILE";
    public static final String ACTION_SEND_FILE_LINK = "com.android.goodixlink.SEND_FILE";
    public static final String ACTION_SEND_FILE_BLUE = "com.android.bluetooth.SEND_FILE";
	public static final String ACTION_SEND_FILE_WIFI_SHA = "com.android.wifihotspot.SEND_FILE";
	public static final String ACTION_OTHER = "com.android.goodixlink.other";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";
    public static final String EXTRAS_REMOTE_DEVICE_ADDRESS = "dev_address";
    public static final String EXTRAS_FILE_LENGTH = "file_length";
    public static final String EXTRAS_WIFIAP_IPADDR = "wifiap_ipaddr";
    public static final String EXTRAS_WIFIAP_SSID = "wifiap_ssid";
    
	public static String LOG_TAG = "GOODIX";
	public static String CONNECT_STATE = "goodixlink.connect.status";	
	public static String GOODIXLINK_MESSAGE_ACTION = "goodixlink.meaasge.action";
	public static String GOODIXLINK_SERVER_COMPLETE_ACTION = "goodixlink.server.transfer.complete.action";
	public static String GOODIXLINK_CILENT_COMPLETE_ACTION = "goodixlink.client.transfer.complete.action";
	public static String GOODIXLINK_SHOW_BAR_ACTION = "goodixlink.show.progressbar.action";
	public static String GOODIXLINK_MESSAGE_INFO = "goodixlink.meaasge.INFO";		
	public static UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	public static int LinkPacketSize = 250;
	public static int LinkLimit = 1024*1;
	public static final int wifiPort = 9006;
	private static String bugFile = GoodixJNI.getStoragePath()+"/GLog.txt";
	
	public static final int G = 1024*1024*1024;
    public static final int M = 1024 * 1024;
    public static final int K = 1024;
    public static final int B = 1;

	public static final String WIFI_HOTSPOT_SSID = "GL";
	public static final String WIFI_HOTSPOT_KEY = "h23456789";
	
	public static boolean isBTA2dp = false;
	public static long lastTime = 0;
	public static int suffixNum = 0;
    public static void Debug()
    {
		if(isDebugMode)
		{
			Log.i(GoodixLink.LOG_TAG, "start read link debug data");
			byte[] buf = new byte[30];
			int ret = GoodixJNI.I2CReadRegister(buf,28,0xAE30);
			if(ret > 0)
			{
				//将读取到的数据以一定格式存入文件中    				
				GoodixJNI.ConserveFile(bugFile, buf,28);
			}
		}
    }  
    //20130709-------------------------------------------
    public static void delay(int ms)
    {
    	try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			LOGE(e.getMessage());
		}
    }
    
    private static boolean DEBUG = true;

    public static void LOGI(String msg)
    {
    	if(msg == null)
		{
			msg = "GoodixLink LOGI received null msg";
		}
    	if(DEBUG)
    	{
    		//Log.i(LOG_TAG, msg);
    		LinkLog.log.Add(msg);
    	}  	

    }
    
    public static void LOGE(String msg)
    {
		if(msg == null)
		{
			msg = "GoodixLink LOGE received null msg";
		}
		if(DEBUG)
    	{
    		//Log.e(LOG_TAG, msg);
    		LinkLog.log.Add(msg);
    	}
 	
    }
    
    public static void LOGD(String msg)
    {
		if(msg == null)
		{
			msg = "GoodixLink LOGD received null msg";
		}
		if(DEBUG)
    	{
    		//Log.d(LOG_TAG, msg);
    		LinkLog.log.Add(msg);
    	}

    }
/*
    public static boolean readPairState()
    {
    	byte[] buf = {0,0};
    	int ret = -1;
    	ret = GoodixJNI.I2CReadRegister(buf, 2, 0x81B2);
		if(ret < 0)
		{
			LinkLog.log.Add("Fail: readPairState# IIC error.");
			return false;
		}

		if(buf[0]==buf[1]&&(buf[0]&0xFF)==0x55)
		{
			return true;			
		}				
    	return false;
    }
    
    public static boolean clearPairBuf()
    {
    	byte[] buf = {0,0};
    	byte timeout_count = 0;
    	while(timeout_count++ < 100)
    	{
    		buf[0]=0;
    		buf[1]=0;
    		GoodixJNI.I2CWriteRegister(buf, 2, 0x81B2); 
    		buf[0]=0x55;
    		buf[1]=0x5A;
    		GoodixJNI.I2CReadRegister(buf, 2, 0x81B2);
        	if(buf[0]==buf[1]&&(buf[0]&0xFF)==0x00)
        	{
        		break;
        	}
    	};
    	
    	if(timeout_count >= 100)
    	{
    		LinkLog.log.Add("Fail: clearPairBuf[0x81B2]# timeout.");
    		return false;
    	}
    	
		return true;
    }
  */  
    public static void ShowToast(Context context,String mag)
    {
    	Toast.makeText(context, mag, Toast.LENGTH_SHORT).show();
    }    
	//锁屏
 	public static boolean lockScreen(Context context,DevicePolicyManager mDevicePolicyManager,PowerManager mPowerManager)
	{   
	    ComponentName componentName = new ComponentName(context, LockScreenAdmin.class);
        // 判断该组件是否有系统管理员的权限
        boolean isAdminActive = mDevicePolicyManager.isAdminActive(componentName);
        if(isAdminActive){       	
           
        	LinkLog.log.Add("Begin waiting screen off.");
            while(mPowerManager.isScreenOn())
            {
            	mDevicePolicyManager.lockNow(); // 锁屏 
            }
            LinkLog.log.Add("Screen is off.");
            return true;
        }
        else{
        	LinkLog.log.Add("Fail: lockScreen# no permissition");
            return false;
        }
	}
	//解锁、亮屏
	public static boolean unLockScreen(PowerManager mPowerManager,PowerManager.WakeLock mPowerScreenOn)
	{		
		LinkLog.log.Add("unLockScreen screen bright:"+mPowerManager.isScreenOn());
		//if(!pm.isScreenOn())
		LinkLog.log.Add("Waiting screen on");
		while(!mPowerManager.isScreenOn())
		{
			mPowerScreenOn.acquire();
		}
		LinkLog.log.Add("Screen is on");
        return true;
	}
	/*
    //Link命令操作
    public static boolean enterSlaveMode()
    {
    	byte[] buf = {0x20,0};
    	int ret;
    	ret = GoodixJNI.I2CWriteRegister(buf, 1, 0x8046);
    	ret = GoodixJNI.I2CWriteRegister(buf, 1, 0x8040);
    	GoodixJNI.delay(40);
    	ret = GoodixJNI.I2CReadRegister(buf, 2, 0x81B0);
    	if(ret < 0){
    		LinkLog.log.Add("Fail: enterSlaveMode# IIC error.");
    		return false;
    	}
    	if((buf[0]&0xFF)==0x88&&buf[0]==buf[1])
    	{
    		return true;
    	}
    	//LinkLog.log.Add("Fail: enterSlaveMode[0x20->0x8040] #" + buf[0] + " " + buf[1]);
    	return false;
    }
    
    public static boolean enterMasterMode()
    {
    	byte[] buf = {0x21,0};
    	int ret = -1;
    	int retry = 0;
    	
    	while(retry++ < 10) {
    		buf[0] = 0x21;
    		buf[1] = 0x00;
	    	GoodixJNI.I2CWriteRegister(buf, 1, 0x8046);
	    	GoodixJNI.I2CWriteRegister(buf, 1, 0x8040);
	    	GoodixJNI.delay(40);
	    	ret = GoodixJNI.I2CReadRegister(buf, 2, 0x81B0);
	    	if(ret<0){
	    		LinkLog.log.Add("Fail: enterMasterMode# IIC error.");
	    		continue;
	    	}
	    	if((buf[0]&0xFF)==0x99&&buf[0]==buf[1]){
	    		return true;
	    	}
	    	LinkLog.log.Add("Fail: enterMasterMode[0x21->0x8040]# " + buf[0] + " " + buf[1]);
    	}
    	GoodixLink.LOGE("Enter master mode timeout");
    	return false;
    }
    
    public static boolean enterTransferMode()
    {
    	int retry = 0;
    	byte[] buf = {0x22,0};
	    int ret = -1;
    	while(retry++ < 10) {
	    	buf[0] = 0x22;
	    	buf[1] = 0x00;
	    	GoodixJNI.I2CWriteRegister(buf, 1, 0x8046);
	    	GoodixJNI.I2CWriteRegister(buf, 1, 0x8040);
	    	GoodixJNI.delay(100);					//compatible to multi-system
	    	ret = GoodixJNI.I2CReadRegister(buf, 2, 0x81B0);
	    	if(ret<0){
	    		LinkLog.log.Add("Fail: enterTransferMode# IIC error.");
	    		continue;
	    	}
	    	if((buf[0]&0xFF)==0xAA&&buf[0]==buf[1])        //receiver 
	    	{
	    		return true;
	    	}
	    	LinkLog.log.Add("Fail: enterTransferMode[0x22->0x8040]# " + buf[0] + " " + buf[1]);
    	}
    	GoodixLink.LOGE("Enter transfer mode timeout");
    	return false;
    }
    
    public static boolean exitSlaveMode()
    {
    	int retry = 0;
    	while(retry++ < 10) {
	    	byte[] buf = {0x28,0};
	    	int ret = -1;
	    	GoodixJNI.I2CWriteRegister(buf, 1, 0x8046);
	    	GoodixJNI.I2CWriteRegister(buf, 1, 0x8040);
	    	GoodixJNI.delay(40);
	    	ret = GoodixJNI.I2CReadRegister(buf, 2, 0x81B0);
	    	if(ret<0){
	    		LinkLog.log.Add("Fail: exitSlaveMode# IIC error.");
	    		continue;
	    	}
	    	
	    	if((buf[0]&0xFF)==0x88||buf[0]!=buf[1])        
	    	{
	    		LinkLog.log.Add("Fail: exitSlaveMode# " + buf[0] + " " + buf[1]);
	    		continue;
	    	}
	    	break;
    	}
    	
    	if(retry >= 10) {
    		GoodixLink.LOGE("Exit slave mode timeout");
    		return false;
    	}
    	
    	return true;
    }
       
    public static boolean exitMasterMode()
    {
    	int retry = 0;
    	while(retry++ < 10) {
	    	byte[] buf = {0x29,0};
	    	int ret = -1;
	    	GoodixJNI.I2CWriteRegister(buf, 1, 0x8046);
	    	GoodixJNI.I2CWriteRegister(buf, 1, 0x8040);
	    	GoodixJNI.delay(40);
	    	ret = GoodixJNI.I2CReadRegister(buf, 2, 0x81B0);
	    	if(ret < 0){
	    		LinkLog.log.Add("Fail: exitMasterMode# IIC error.");
	    		continue;
	    	}
	    	if((buf[0]&0xFF)==0x99||buf[0]!=buf[1])        
	    	{
	    		LinkLog.log.Add("Fail: exitMasterMode# " + buf[0] + " " + buf[1]);
	    		continue;
	    	}
	    	break;
    	}
    	
    	if(retry >= 10) {
    		GoodixLink.LOGE("Exit master mode timeout");
    		return false;
    	}
    	return true;
    }
    
    public static boolean exitTransferMode()
    {
    	int retry = 0;
    	byte[] buf = {0x2A,0};
	    int ret;
	    
    	while(retry++ < 10) {
	    	buf[0] = 0x2A;
	    	buf[1] = 0x00;
	    	GoodixJNI.I2CWriteRegister(buf, 1, 0x8046);
	    	GoodixJNI.I2CWriteRegister(buf, 1, 0x8040);
	    	if(true)
	    	{
	    		LinkLog.log.Add("exitTransferMode# reset guitar.");
	        	ret = GoodixJNI.GuitarReset();    	
	        	GoodixJNI.delay(100);
	        	if(ret < 0)
	        	{
	        		LinkLog.log.Add("Fail: exitTransferMode# Guitar reset failed.");
	        		continue;
	        	}
	    	}
	    	else
	    	{
	    		GoodixJNI.delay(100);
	        	GoodixJNI.I2CReadRegister(buf, 2, 0x81B0);
	        	if((buf[0]&0xFF)==0xAA||(buf[0]&0xFF)==0xBB||buf[0]!=buf[1])        
	        	{
	        		continue;
	        	}  
	        	LOGD("-------------exitTransferMode[0x2A->0x8040] success-------------");
	    	}
	    	break;
    	}
    	
    	if(retry >= 10) {
    		GoodixLink.LOGE("Exit transfer mode timeout");
    		return false;
    	}
    	   	
    	return true;
    }   

    public static boolean sendData(byte[] buf,int length)
    {
    	boolean running = true;
    	int ret = -1,idleTimes = 0;
    	byte[] sndFlag = new byte[10];
    	byte[] sndBuf = new byte[length+2];
    	int retry = 0;
    	//query 0xAB10 send state is not sending 0x02
    	while(retry++ < 5) {
	    	ret = GoodixJNI.I2CReadRegister(sndFlag, 4, 0xAB10);
	    	if(ret < 0)
	    	{
	    		LinkLog.log.Add("Fail: sendData# IIC error.");
	    		continue;
	    	}   	
	    	if(sndFlag[0]!=sndFlag[2]||sndFlag[1]!=sndFlag[3])
	    	{
	    		LinkLog.log.Add("Fail: sendData# state error " + sndFlag[0] + " " +
	    														 sndFlag[1] + " " +
	    														 sndFlag[2] + " " +
	    														 sndFlag[3]);
	    		continue;
	    	}   	
	    	if((sndFlag[0]&0xFF) == 0x02)
	    	{
	    		LinkLog.log.Add("sendData# Link is sending,exit send.");
	    		sndBuf[0] = 0;
	    		GoodixJNI.I2CWriteRegister(sndBuf, 1, 0xAB1F);
	    		continue;
	    	}
	    	//Calculate the checksum
	    	sndBuf[0] = (byte) length;
	    	sndBuf[length+1] = GoodixJNI.getCheckSm(buf,length);
	    	System.arraycopy(buf, 0, sndBuf, 1, length);
	    	//Writes data to the 0xAC90
	    	ret = GoodixJNI.I2CWriteRegister(sndBuf, length+2, 0xAC90);
	    	if(ret < 0)
	    	{
	        	LinkLog.log.Add("Fail: sendData# IIC error.");
	        	continue;
	    	}
	    	//refresh the update flag
	    	sndFlag[0] = (byte) 0xAA;
	    	ret = GoodixJNI.I2CWriteRegister(sndFlag, 1, 0xAD91);
	    	if(ret < 0)
	    	{
	        	LinkLog.log.Add("Fail: sendData# IIC error.");
	        	continue;
	    	}
	    	break;
    	}
    	
    	if(retry >= 5) {
    		GoodixLink.LOGE("GoodixLink send data timeout!");
    		return false;
    	}
    	
    	//query 0xAB10 send state is 0x03,send successful
    	while(running)
    	{
    		if(idleTimes>150)
    		{
    			LinkLog.log.Add("Fail: senData# wait 0xAB10 timeout.");
    			return false;
    		}
    		ret = GoodixJNI.I2CReadRegister(sndFlag, 4, 0xAB10);
        	if(ret < 0)
        	{
            	LinkLog.log.Add("Fail: sendData# IIC error.");
            	idleTimes ++;
        		GoodixJNI.delay(40);
        		continue;
        	}
        	if(sndFlag[1]!=sndFlag[3]||sndFlag[0]!=sndFlag[2])
        	{   
        		LinkLog.log.Add("Fail: sendData# state error " + sndFlag[0] + " " +
																 sndFlag[1] + " " +
																 sndFlag[2] + " " +
																 sndFlag[3]);
        		GoodixJNI.I2CReadRegister(sndFlag, 2, 0x81B0);
        		LinkLog.log.Add("0x81B0:"+sndFlag[0]+" "+sndFlag[1]);
        		idleTimes ++;
        		GoodixJNI.delay(40);
        		continue;
        	} 
        	LinkLog.log.Add("send AB10 :"+sndFlag[0] + ",Retries:" + idleTimes);
        	switch(sndFlag[0]&0xFF)
        	{ 
        	case 0x01:
        	case 0x02:
        	case 0x05:
        		idleTimes++;      		
        		break;
        	case 0x03:
        		sndBuf[0] = 0;
        		GoodixJNI.I2CWriteRegister(sndBuf, 1, 0xAB1F);
        		return true;
        	case 0x04:
        		ret = GoodixJNI.I2CWriteRegister(sndBuf, length+2, 0xAC90);
            	if(ret < 0)
            	{
            		LinkLog.log.Add("Fail: sendData# IIC error.");
            		running = false;;
            	}
            	//refresh the update flag
            	sndFlag[0] = (byte) 0xAA;
            	ret = GoodixJNI.I2CWriteRegister(sndFlag, 1, 0xAD91);
            	if(ret < 0)
            	{
            		LinkLog.log.Add("Fail: sendData# IIC error.");
            		running = false;;
            	}
            	idleTimes = 0;
            	break;
    		default:
    			idleTimes++;
    			break;
        	} 
        	GoodixJNI.delay(40);
    	}
    	GoodixLink.LOGE("Fail: sendData# reason " + sndFlag[0] + ",Retries:" + idleTimes);
    	return false;
    }
       
    public static int receiveData(byte[] buf)
    {
    	int ret = -1,rcvlength = 0,templength,idleTimes = 0;
    	byte[] rcvFlag = new byte[4];
    	byte[] tempbuf = new byte[260];
    	boolean running = true;
    	//query 0xAB10 receive state is 0x03,receive successful
    	while(running)
    	{
    		if(idleTimes>150)
    		{
    			LinkLog.log.Add("wait 0xAB10 timeout");
    			return -2;
    		}
    		ret = GoodixJNI.I2CReadRegister(rcvFlag, 4, 0xAB10);
        	if(ret < 0)
        	{
        		LinkLog.log.Add("Fail: receiveData# IIC error.");
        		idleTimes ++;
        		GoodixJNI.delay(40);
        		continue;
        	}   	
        	if(rcvFlag[0]!=rcvFlag[2]||rcvFlag[1]!=rcvFlag[3])
        	{      		
        		LinkLog.log.Add("Fail: receiveData# 0xAB10 data error,continue");
        		idleTimes ++;
        		GoodixJNI.delay(40);
        		continue;
        	} 
        	switch(rcvFlag[1]&0xFF)
        	{
        	case 0x01:
        	case 0x02:
        		idleTimes++;              		
        		break;
        	case 0x03:
        		running = false;
        		break;
        	case 0x04:
        		idleTimes=0;
        		break;
    		default:
    			idleTimes++;    
    			break;
        	}
        	GoodixJNI.delay(40);
        	LinkLog.log.Add("receiveData# receive AB10 :" + rcvFlag[1] + ",Retries:" + idleTimes);
    	}
    	
    	//query data buffer
    	int retry = 0;
    	while(retry++ < 5) {
	    	ret = GoodixJNI.I2CReadRegister(tempbuf, 2, 0xAE10);
	    	if(ret < 0)
	    	{
	    		LinkLog.log.Add("Fail: receiveData# IIC error.");
	    		continue;
	    	}     	
	    	if((tempbuf[0]&0x80)==0)
	    	{
	    		LinkLog.log.Add("Fail: receiveData# Link data not ready,exit recieve.");
	    		rcvFlag[0] = 0;
	    		GoodixJNI.I2CWriteRegister(rcvFlag, 1, 0xAB1F);
	    		continue;
	    	}
	    	
	    	rcvlength = tempbuf[1]>0?tempbuf[1]:tempbuf[1]+256;    	
	    	if(rcvlength > 252)
	    	{
	    		LinkLog.log.Add("Fail: receiveData# Link data length is out of range " + rcvlength);
	    		rcvFlag[0] = 0;
	    		GoodixJNI.I2CWriteRegister(rcvFlag, 1, 0xAB1F);
	    		continue;
	    	}
	    	templength = rcvlength;
	    	if(rcvlength%2==1)
	    	{
	    		templength = rcvlength+1;
	    	}
	    	//read the remain data
	    	ret = GoodixJNI.I2CReadRegister(tempbuf, templength+2, 0xAE12);
	    	if(ret < 0)
	    	{
	    		LinkLog.log.Add("Fail: receiveData# IIC error.");
	    		continue;
	    	} 
	
	    	if(!GoodixJNI.checkCRC(tempbuf, templength+2,(byte)rcvlength))
	    	{
	    		LinkLog.log.Add("Fail: receiveData# crc error");
	    		rcvFlag[0] = 0;
	    		GoodixJNI.I2CWriteRegister(rcvFlag, 1, 0xAB1F);
	    		continue;
	    	}
	    	break;
    	}
    	
    	if(retry >= 5 || rcvlength == 0) {
    		GoodixLink.LOGE("GoodixLink receive data failed!");
    		return -2;
    	}
    	
    	System.arraycopy(tempbuf, 0, buf, 0, rcvlength);
    	//clear 0xAE10,0xAB1F
    	rcvFlag[0] = 0;
    	rcvFlag[1] = 0;
    	GoodixJNI.I2CWriteRegister(rcvFlag, 2, 0xAE10);
    	GoodixJNI.I2CWriteRegister(rcvFlag, 1, 0xAB1F);
    	return rcvlength;
    }   
 */
}
