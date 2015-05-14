package com.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import com.floatwindow.FloatWindowService;
import com.floatwindow.LinkFloatWindowManager;
import com.floatwindow.LoadingFlash;
import com.jni.GoodixJNI;
import com.log.LinkLog;
import com.magic.R;
import com.tools.GoodixLink;
import com.tools.GoodixLink.ServerOrClient;
import com.wifihotspot.WifiApAdmin;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

public class ServerService extends Service{
	private int mLimitSize = 1024*100;
	public Handler mHandler;
	private int mFileSize = 0;
    private Thread mThread = null;
    private boolean mThreadRunning = false;
    private IBinder mBinder = new ServerBinder();  
    private static SoundPool mSoundPool = null; 
    private static int mSuccessId;
    private static int mFailedId;
	private Socket mWifiApSocket = null;
	private ServerSocket mWifiApSs = null;
	private int mWifiApState ;
	private int WIFIAP_CREATING = 0;
	private int WIFIAP_CREATE_SUCCESS = 1;
	private int WIFIAP_CREATE_FAILED = 1;
	private Timer mTimer = null;
	private TimerTask wdtTask = null;
	private boolean wifiFrameTimeoutFlag = false;
	private OutputStream os = null;
	private FileInputStream fis = null;
	
    public class ServerBinder extends Binder
    {
    	ServerService getService()
    	{
    		return ServerService.this;
    	}
    }
    
	@Override
	public IBinder onBind(Intent intent) {
		GoodixLink.LOGI("server service onBind- ");
		new ServerThread().start();
		return mBinder;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		mThreadRunning = true;
		mSoundPool = new SoundPool(3, AudioManager.STREAM_SYSTEM, 5);
		mSuccessId = mSoundPool.load(this, R.raw.notice, 0);
		mFailedId = mSoundPool.load(this, R.raw.failed, 0);
		GoodixLink.LOGI("server service onCreate- ");
		if(mTimer == null) {
			mTimer = new Timer();
		}
		if(wdtTask == null) {
			wdtTask = new TimerTask() {
				@Override
				public void run() {
					if(wifiFrameTimeoutFlag) {
						
						if(os != null) {
							try {
								os.close();
								GoodixLink.LOGD("Wifi frame wdt timeout,close output stream!");
							} catch (IOException e) {
								GoodixLink.LOGE("close output stream in wdtTask exception:"+e.getMessage());
							}
						}
						/**/
					}
					wifiFrameTimeoutFlag = true;
//					GoodixLink.LOGD("wifiFrameTimeoutFlag:true");
				}
			};
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mThreadRunning = false;
		if(mThread!=null&&mThread.isAlive())
		{
			mThread.destroy();
			mThread = null;
		}
		if(mWifiApSocket!=null)
		{
			try {
				mWifiApSocket.close();
			} catch (IOException e) {
				GoodixLink.LOGE("Server service onDestroy close mWifiApSocket failed:"+e.toString());
			}
			mWifiApSocket = null;
		}
		if(mWifiApSs!=null)
		{
			try {
				mWifiApSs.close();
			} catch (IOException e) {
			    GoodixLink.LOGE("Server service onDestroy close mWifiApSs failed:"+e.toString());
			}
			mWifiApSs = null;
		}
//		if(mFileSize > GoodixLink.LinkLimit)
//		{
//			deleteWifiHotspot(ServerService.this);
//		}
		if(mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}
		if(wdtTask != null) {
			wdtTask.cancel();
			wdtTask = null;
		}
		GoodixLink.LOGI("server service onDestroy- ");
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		GoodixLink.LOGI("server service onStart- ");	
	}
	
	private class ServerThread extends Thread
	{
		@Override
		public void run() {
			boolean ret;
			byte[] buf=new byte[255];
			File file = new File(GoodixLink.sendFilePath);
			if(!file.exists())
			{
				GoodixLink.LOGE("Server send file is not exists.");
				GoodixLink.runStatus = GoodixLink.ERROR;
				ServerFail("Transfer failed! \nThe file "+GoodixLink.sendFilePath+"does not exist.");
				return ;
			}
			
			mFileSize = (int) file.length();
			byte[] name = file.getName().getBytes();
			if(mFileSize > GoodixLink.LinkLimit)  //wifi
			{
				FloatWindowService.refreshTips("WIFI");
				int length = name.length + 8;				
				buf[0] = (byte) 0xA0;
	    		buf[1] = (byte) length;

				int i = 6;
				buf[2] = (byte) 192;
				buf[3] = (byte) 168;
				buf[4] = (byte) 43;
				buf[5] = (byte) 1;
				// Package a random number
				SharedPreferences settings = getSharedPreferences("settings", Context.MODE_PRIVATE);
				GoodixLink.suffixNum =  settings.getInt("wifiAp_ssid_suffixNum", 0xdef);
				GoodixLink.LOGD("Link Server send suffix[int]:" + Integer.toHexString(GoodixLink.suffixNum));
				int j;
				for(j=0; (j<4)&(i<10); j++,i++)
				{
					buf[i] = (byte)((GoodixLink.suffixNum>>(j*8))&0xFF);
				}
				GoodixLink.LOGD("LinkService send suffix[byte]:" 
					+ Integer.toHexString(buf[6]&0xFF)+Integer.toHexString(buf[7]&0xFF) 
					+ Integer.toHexString(buf[8]&0xFF)+Integer.toHexString(buf[9]&0xFF));

				System.arraycopy(name, 0, buf, 10, name.length);
				buf[length+2]=(byte) (mFileSize>>24);
				buf[length+3]=(byte) (mFileSize>>16);
				buf[length+4]=(byte) (mFileSize>>8);
				buf[length+5]=(byte) (mFileSize&0xFF);

				if(!GoodixJNI.sendData(buf, length+6))				
	    		{
	    			GoodixLink.LOGE("Link server send first frame failed");
					GoodixLink.runStatus = GoodixLink.ERROR;	
					ServerFail("Transfer failed! \nLink server send first frame failed");
					return;
	    		}
				MagicService.vibrateTips(true);
				GoodixJNI.delay(20);
    			MagicService.lightUpScreen();
    			
    			LoadingFlash.mLoadingFlash.startFlash();
    			mWifiApState = WIFIAP_CREATING;
    			createWifiHotspot(ServerService.this,GoodixLink.WIFI_HOTSPOT_SSID+Integer.toHexString(GoodixLink.suffixNum));
    			while(mWifiApState == WIFIAP_CREATING && mThreadRunning)
    			{
//    				LinkLog.log.Add("mWifiApState = "+mWifiApState);			//there is a timeout in createWifiHotspot function
    			}
    			LoadingFlash.mLoadingFlash.stopFlash();
    			if(mThreadRunning)
    			{
    				if(mWifiApState == WIFIAP_CREATE_SUCCESS)
    				{
    					wifiServer(file);
    				}
    				else if(mWifiApState == WIFIAP_CREATE_FAILED)
    				{
    					GoodixLink.runStatus = GoodixLink.ERROR;
    					ServerFail("Transfer failed! \nWifi server create hotspot failed");
    				}
    			}    				
			}		       
			else    //link 
			{			
				buf[0]=(byte) 0xE0;
				buf[1] = (byte) name.length;
				System.arraycopy(name, 0, buf, 2, name.length);
				buf[name.length+2]=(byte) (mFileSize>>24);
				buf[name.length+3]=(byte) (mFileSize>>16);
				buf[name.length+4]=(byte) (mFileSize>>8);
				buf[name.length+5]=(byte) (mFileSize&0xFF);
				GoodixLink.LOGD("Transfer file name info.");

				ret = GoodixJNI.sendData(buf, name.length+6); //1个头区分goodixlink传输方式+1文件名长度+N文件名+4文件总长
				GoodixLink.Debug();
				if(!ret)
				{
					GoodixLink.LOGE("goodixlink transfer first package failed");
					GoodixLink.runStatus = GoodixLink.ERROR;
					ServerFail("Transfer failed! \nThe first frame data sending failed.");
					return;
				}				
				GoodixLink.LOGD("goodixlink transfer first package successful");			
				ret = true;
				if(mFileSize == 0)
				{
					GoodixLink.runStatus = GoodixLink.TRANSFER_END;
					ServerComplete("Transfer Complete!\n This is an empty file");
				}
				else
				{
					ret = linkServer(file);					
				}	
				if(mThreadRunning&&ret)
				{
					MagicService.vibrateTips(ret);
				}
			}			
		}		
	}
	
	private void wifiServer(File file)
	{		
		try {
			LoadingFlash.mLoadingFlash.startFlash();
			GoodixLink.LOGD("Wifi server begin create socket");
			mWifiApSs = new ServerSocket(GoodixLink.wifiPort);	
			if(!mThreadRunning)
			{
				LoadingFlash.mLoadingFlash.stopFlash();
				return;
			}
			mWifiApSs.setReuseAddress(true);
			mWifiApSs.setSoTimeout(25000);
			mWifiApSocket = mWifiApSs.accept();
			mWifiApSocket.setKeepAlive(true);
//			mWifiApSocket.setPerformancePreferences(connectionTime, latency, bandwidth)
			GoodixLink.LOGD("Wifi server create socket success");
			LoadingFlash.mLoadingFlash.stopFlash();
			if(!mThreadRunning)
			{
				return;
			}
			GoodixLink.LOGD("Wifi server begin send data");
			os = mWifiApSocket.getOutputStream();
			fis = new FileInputStream(file);
			GoodixLink.runStatus = GoodixLink.TRANSFER_ING;
			copyFile(os, fis);
			if(!mThreadRunning)
			{
				return;
			}
			GoodixLink.runStatus = GoodixLink.TRANSFER_END;
	        GoodixLink.LOGD("Wifi ap finished sending data!"); 
	        ServerComplete("Transfer Complete! ");
	        
		}catch (Exception e) {
			String msg = e.toString();
			if(msg == null) {
			    msg = "Wifi ap create socket timeout!";
			}
			GoodixLink.LOGE("Transfer failed! \n"+msg);					
			GoodixLink.runStatus = GoodixLink.ERROR;	    				
			ServerFail("Transfer failed! \n"+msg);
		}
		finally {
			try{
			    if(mWifiApSocket != null) {
			        mWifiApSocket.close();
			        mWifiApSocket = null;
			    }
			    if(mWifiApSs != null) {
			        mWifiApSs.close();
			        mWifiApSs = null;
			    }
			    if(os != null) {
			        os.close();
			        os = null;
			    }
			    if(fis != null) {
			        fis.close();
			        fis = null;
			    }
			}catch(Exception e) {
				GoodixLink.LOGE("free resource catch exception:" + e.toString());
			}
		}	
				
	}
	
	private boolean linkServer(File file) 
	{
		int wroteBytes = 0,len;
		byte[] buf = new byte[300];
		FileInputStream fis;
		try {
			fis = new FileInputStream(file);
			GoodixLink.runStatus = GoodixLink.TRANSFER_ING;
			while((len = fis.read(buf, 2, GoodixLink.LinkPacketSize))!=-1&&mThreadRunning)
			{
				wroteBytes += len;
				FloatWindowService.refreshTips((wroteBytes*100/mFileSize)+"%");
				buf[0]=(byte) (wroteBytes==mFileSize?0xC1:0xC0);
				buf[1]=(byte) len;
				 //文件内容+1个头区分goodixlink+数据长度					
				if(!GoodixJNI.sendData(buf, len+2))
				{
					GoodixLink.LOGE("goodixlink transfer data failed.");					
					GoodixLink.runStatus = GoodixLink.ERROR;	    				
					ServerFail("Transfer failed! \nLink transfer data failed.");
					break ;
				}
				GoodixLink.Debug();	    				    			   			    			
			}

	        if(!mThreadRunning)
	        {
	        	fis.close();
	        	return false;
	        }
	        fis.close();
			if(GoodixLink.runStatus == GoodixLink.TRANSFER_ING)
			{
				GoodixLink.runStatus = GoodixLink.TRANSFER_END;
				ServerComplete("Transfer Complete! ");
				return true;
			}			
			
		} catch (Exception e) {
			GoodixLink.LOGE("Transfer failed! \n"+e.getMessage());					
			GoodixLink.runStatus = GoodixLink.ERROR;	    				
			ServerFail("Transfer failed! \n"+e.getMessage());
			return false;
		} 
		return false;
	}
	    
	private void ServerFail(String msg) {
		if(GoodixLink.serverOrClient == ServerOrClient.SERVER&&mThreadRunning)
    	{
			FloatWindowService.refreshTips("Failed");
			Intent intent = new Intent(GoodixLink.GOODIXLINK_SERVER_COMPLETE_ACTION);
    		intent.putExtra(GoodixLink.GOODIXLINK_MESSAGE_INFO, msg);
    		sendOrderedBroadcast(intent,null);     
    		mSoundPool.play(mFailedId, 2, 2, 0, 0, 1);
    		LinkFloatWindowManager.removeSmallWindow(getApplicationContext());
    	} 
	}
	
    private void ServerComplete(String msg)
    {
    	if(GoodixLink.serverOrClient == ServerOrClient.SERVER&&mThreadRunning)
    	{
    		Intent intent = new Intent(GoodixLink.GOODIXLINK_SERVER_COMPLETE_ACTION);
    		intent.putExtra(GoodixLink.GOODIXLINK_MESSAGE_INFO, msg);
    		sendOrderedBroadcast(intent,null); 
    		mSoundPool.play(mSuccessId, 2, 2, 0, 0, 1);
    		LinkFloatWindowManager.removeSmallWindow(getApplicationContext());
    	}    	
    }
 
    private void copyFile(OutputStream os,FileInputStream fis) throws IOException,Exception
    {
    	int len;
    	int wroteBytes=0;
    	
    	wifiFrameTimeoutFlag = false;
    	mTimer.schedule(wdtTask, 5000, 5000);
    	wifiFrameTimeoutFlag = false;
		byte[] buf=new byte[mLimitSize];
		while((len = fis.read(buf))!=-1&&mThreadRunning)
		{
			os.write(buf, 0, len);
			os.flush();
			wroteBytes += len;
			int progress = (int) (wroteBytes*100f/mFileSize);
			FloatWindowService.refreshTips( progress +"%");
			wifiFrameTimeoutFlag = false;
			//GoodixLink.LOGD("wroteBytes"+wroteBytes+",mFileSize"+mFileSize+",progress"+progress);
			
		}
		FloatWindowService.refreshTips("100%");
		if(mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}
		if(wdtTask != null) {
			wdtTask.cancel();
			wdtTask = null;
		}
		os.close();
		fis.close();
    }

	private void createWifiHotspot(Context mContext,String SSID)
	{
		GoodixLink.LOGD("Link share create wifi hotspot.");
		WifiApAdmin wifiAp = new WifiApAdmin(mContext){

			@Override
			public void onNotifyWifiApCreated() {
				mWifiApState = WIFIAP_CREATE_SUCCESS;
				LinkLog.log.Add("mWifiApState = WIFIAP_CREATE_SUCCESS");
			}

			@Override
			public void onNotifyWifiApCreateFailed() {
				mWifiApState = WIFIAP_CREATE_FAILED;
				LinkLog.log.Add("mWifiApState = WIFIAP_CREATE_FAILED");
			}
			
		};	
		wifiAp.startWifiAp( SSID, GoodixLink.WIFI_HOTSPOT_KEY);
	}

	private void deleteWifiHotspot(Context mContext)
	{
		WifiManager wifiMgt = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
//		if(WifiApAdmin.isWifiApEnabled(wifiMgt))
		{
			GoodixLink.LOGD("Tuning off wifi hotspot!");
			WifiApAdmin.closeWifiAp(mContext);
		}
		
		if (!wifiMgt.isWifiEnabled()) 
		{
			wifiMgt.setWifiEnabled(true);
        }       
	}
}
