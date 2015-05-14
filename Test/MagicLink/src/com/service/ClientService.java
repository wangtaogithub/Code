package com.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;

import com.floatwindow.FloatWindowService;
import com.floatwindow.LinkFloatWindowManager;
import com.floatwindow.LoadingFlash;
import com.jni.GoodixJNI;
import com.log.LinkLog;
import com.magic.LinkShowText;
import com.magic.MagicLink;
import com.reflectUtils.ClsUtils;
import com.tools.FileOperate;
import com.tools.GoodixLink;
import com.tools.GoodixLink.ServerOrClient;
import com.tools.LinkSlaveDilalog;
import com.wifihotspot.WifiAdmin;
import android.bluetooth.BluetoothProfile;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;

public class ClientService extends Service{	
    private int mLimitSize = 1024*100;
    private int fileSize;
    private String mSsid;
    private String hostIpAddr = null;
    private SocketAddress hostSoAddr = null;
    private String mFilePath ;
    private Context mContext;
    private Handler mHandler;
    private String mMsg = null;
    private WifiAdmin mWifiAdmin;
    private boolean ifWifiConnected = false;
 
    private boolean fileTransferSuccess = false;
    private boolean mThreadRunning = false;
    private Thread mThread = null;
    private BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    private static BluetoothDevice mDevice = null;
    private Socket mWifiSocket = null;
    private byte mClickTims = 0;
    private Timer clickTimer;
    private TimerTask clickTask;
    private LinkSlaveDilalog mDialog = null;
    private IBinder mBinder = new ClientBinder(); 
    public static boolean isNeedCoseBT = false;
    private Timer mTimer = null;
    private TimerTask wdtTask = null;
    private boolean wifiFrameTimeoutFlag = false;
    private InputStream wifiClientIs = null;
	private FileOutputStream wifiClientFos = null;
	
    public class ClientBinder extends Binder
    {
    	ClientService getService()
    	{
    		return ClientService.this;
    	}
    }
    
	@Override
	public IBinder onBind(Intent intent) {
		LinkLog.log.Add("Clientservice onBind.");
		new clientThread().start();
		return mBinder;
	}
	
	@Override
	public void onCreate() {
		LinkLog.log.Add("ClientService onCreate.");
		
		mThreadRunning = true;
		fileTransferSuccess = false;
		mContext = ClientService.this;
		super.onCreate();
		if(mTimer == null) {
			mTimer = new Timer();
		}
		if(wdtTask == null) {
			wdtTask = new TimerTask() {
				@Override
				public void run() {
					if(wifiFrameTimeoutFlag) {
						if(wifiClientIs != null) {
							GoodixLink.LOGD("Wifi frame time out,close input stream!");
							
							try {
								wifiClientIs.close();
							} catch (IOException e) {
								GoodixLink.LOGE("Close input stream in wdtTask exception:"+e.getMessage());
							}
							/**/
						}
					}
					wifiFrameTimeoutFlag = true;
//					GoodixLink.LOGD("wifiFrameTimeoutFlag:true");
				}
			};
		}
		mWifiAdmin = new WifiAdmin(this) {
			@Override
			public Intent myRegisterReceiver(BroadcastReceiver receiver,
					IntentFilter filter) {
				ClientService.this.registerReceiver(receiver, filter);
				return null;
			}

			@Override
			public void myUnregisterReceiver(BroadcastReceiver receiver) {
				ClientService.this.unregisterReceiver(receiver);
			}

			@Override
			public void onNotifyWifiConnected() {
				ifWifiConnected = true;
				mWifiAdmin.releaseWifiLock();
				GoodixLink.LOGD("wifi have connected success!");
				GoodixLink.LOGD("###############################");
			}

			@Override
			public void onNotifyWifiConnectFailed() {
				ifWifiConnected = false;
				mWifiAdmin.releaseWifiLock();
				GoodixLink.LOGD("wifi have connected failed!");
				GoodixLink.LOGD("###############################");
			}
		};
		mWifiAdmin.creatWifiLock();
		mWifiAdmin.acquireWifiLock();	
		mHandler = new Handler()
		{
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				switch(msg.what)
				{
				case 0:
					GoodixLink.ShowToast(ClientService.this, msg.obj.toString());
					break;
				case 1:
					showDialog(mContext);
					break;
				}			
			}			
		};
	}
		
	@Override
	public void onDestroy() {
		super.onDestroy();
		mThreadRunning = false;
		if(mThread != null)
		{
			mThread.destroy();
			mThread = null;
		}
		if(mWifiSocket!=null)
		{
			try {
				mWifiSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			mWifiSocket = null;
		}
		if(mDialog!=null)
		{
			mDialog.cancel();
			mDialog = null;
		}
		if(mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}
		if(wdtTask != null) {
			wdtTask.cancel();
			wdtTask = null;
		}
		LinkLog.log.Add("ClientService onDestroy");
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);						
	}
	
	private class clientThread extends Thread
	{
		@Override
		public void run() {

			int mDataLen = 0,rcvLen;	
		    byte[] buffer = new byte[300];
		    LinkLog.log.Add("ClientService begin receive data.....");
	
			mDataLen = GoodixJNI.receiveData(buffer);
			if(mDataLen > 0&&mDataLen>buffer[1])				
			{								
				GoodixLink.LOGD("*******GoodixLink client received data successfully*******");	
				GoodixLink.Debug();	
				rcvLen = buffer[1]>0?buffer[1]:(buffer[1]+256);
				try {
					switch(buffer[0]&0xE0)
					{
					case 0xE0: //link
						switch(buffer[0]&0x1E)
						{
						case 0x00:
							mFilePath = FileOperate.createFile(new String(buffer,2,rcvLen));					        
					        for(int i=0;i<4;i++)
					        {
					        	int data = buffer[rcvLen+2+i];
					        	if(data<0)
					        	{      		
					        		data += 256;
					        	}
					        	fileSize += data<<((3-i)*8);
					        }
					        boolean ret = true;
							if(fileSize == 0)
							{
						    	GoodixLink.runStatus = GoodixLink.TRANSFER_END;
						    	ClientComplete("Data Received!\n This is an empty file");	    	
							}
							else
							{
								ret = linkClient();								
							}
							if(mThreadRunning)
							{
								MagicService.vibrateTips(ret);
							}
							break;
						case 0x02:
							LinkFloatWindowManager.removeSmallWindow(getApplicationContext());
							MagicService.vibrateTips(true);
							MagicService.lightUpScreen();
							GoodixLink.isBTA2dp = true;
							BluetoothProfile(buffer,mDataLen);
							GoodixJNI.delay(3000);   //非正常操作BUG
							GoodixLink.runStatus = GoodixLink.TRANSFER_END;
					    	ClientComplete("Data Received!\n This is an bluetooth Mac Address.");
							break;
						default:
							GoodixLink.LOGE("Client received a error first frame data:"+buffer[0]);
							GoodixLink.runStatus = GoodixLink.ERROR;
							ClientFail("Receive failed! \nThe first frame data receive failed(Invalid Frame Head).");	
							break;
						}
						break;
					case 0xA0: //wifi						
						GoodixLink.LOGD("Client enter wifi mode");
						FloatWindowService.refreshTips("WIFI");
						MagicService.vibrateTips(true);
						MagicService.lightUpScreen();
						LoadingFlash.mLoadingFlash.startFlash();
						//GoodixLink.LOGD("begin deal IP address");
						int[] temp_buf = new int[4];
						temp_buf[0] = (int)((buffer[2] > 0) ? buffer[2] : (256 + buffer[2]));
						temp_buf[1] = (int)((buffer[3] > 0) ? buffer[3] : (256 + buffer[3]));
						temp_buf[2] = (int)((buffer[4] > 0) ? buffer[4] : (256 + buffer[4]));
						temp_buf[3] = (int)((buffer[5] > 0) ? buffer[5] : (256 + buffer[5]));
						
						GoodixLink.LOGD("Client wifi begin connect to wifi hotspot");
						//Resolve host ip addr
						hostIpAddr = Integer.toString(temp_buf[0])+"."+
										   Integer.toString(temp_buf[1])+"."+
										   Integer.toString(temp_buf[2])+"."+
										   Integer.toString(temp_buf[3]);
						GoodixLink.LOGD("Client received IP:"+hostIpAddr);
						hostSoAddr = new InetSocketAddress(hostIpAddr, GoodixLink.wifiPort);
						//Resolve random suffix
//						GoodixLink.LOGD("Client received suffix[byte]:"+Integer.toHexString(buffer[6]&0xFF)+Integer.toHexString(buffer[7]&0xFF)
//																+Integer.toHexString(buffer[8]&0xFF)+Integer.toHexString(buffer[9]&0xFF));
						int suffix = 0;
						int m = 6,n = 0;
						for(; (m<10)&&(n<4); m++,n++)
						{
							suffix |= (((int)buffer[m]&0xFF)<<(n*8));
						}
						GoodixLink.LOGD("Client received random suffix:"+Integer.toHexString(suffix));						
						mFilePath = FileOperate.createFile(new String(buffer,10,rcvLen-8));	
						mSsid = GoodixLink.WIFI_HOTSPOT_SSID+Integer.toHexString(suffix);
						
						//Resolve file size
				        for(int i=0;i<4;i++)
				        {
				        	int data = buffer[rcvLen+2+i];
				        	if(data<0)
				        	{      		
				        		data += 256;
				        	}
				        	fileSize += data<<((3-i)*8);
				        }

						if(fileSize == 0)
						{
							LoadingFlash.mLoadingFlash.stopFlash();
							GoodixLink.runStatus = GoodixLink.TRANSFER_END;
					    	ClientComplete("File received successfully!\n It is an empty file");	    	
						}
						else
						{
							wifiClient();
						}
						
						break;
					default:
						GoodixLink.LOGE("Client received a error first frame data!");
						GoodixLink.runStatus = GoodixLink.ERROR;
						ClientFail("Receive failed! \nThe first frame data receive failed(Invalid Frame Head).");	
						break;
					}							
				} catch (Exception e) {	
					LinkLog.log.Add("Link receive failed! \n"+e.getMessage());
					MagicService.vibrateTips(false);
					GoodixLink.runStatus = GoodixLink.ERROR;
					ClientFail("Receive failed! \nThe first frame data receive failed(Exception:)"+e.getMessage());					
				}
			}		
			else
			{
				LinkLog.log.Add("Client receive first frame data failed.");
				MagicService.vibrateTips(false);
				GoodixLink.runStatus = GoodixLink.ERROR;
				FloatWindowService.refreshTips("Failed");
				ClientFail("Receive failed! \nThe first frame data receive fail(Error Data Length).");	
			}			
		}		
	}
	
	private boolean linkClient()
	{
		File file = new File(mFilePath);
		byte[] buf=new byte[255];
		int rcvLen = 0,errPacketNum=0,wroteBytes = 0,ret;
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			GoodixLink.LOGD("GoodixLink begin reveive data.");

			while(wroteBytes<fileSize && mThreadRunning)
			{				
				ret = GoodixJNI.receiveData(buf);
				if(ret>0)
				{				
					try {						
						rcvLen = buf[1];
						if(rcvLen<0)
						{
							rcvLen += 256;
						}
						fos.write(buf, 2, rcvLen);
						wroteBytes += rcvLen;						
						FloatWindowService.refreshTips((wroteBytes*100/fileSize)+"%");
						if((buf[0]&0x01) == 0x01)   //最后一段
						{
							fos.close();
							GoodixLink.runStatus = GoodixLink.TRANSFER_END;
							fileTransferSuccess = true;
							GoodixLink.LOGD("client write data to file success.");
							String msg = null;
							if(file.getName().contains(".##########")){
								msg = "Received successfully!";
							}
							else {
								msg = "File received successfully!\nPath:" + file.getPath();
							}
							ClientComplete(msg);
								
							return true;//数据读取完成结束goodixlink读线程
						}
					}  catch (Exception e) {
						GoodixLink.runStatus = GoodixLink.ERROR;
						GoodixLink.LOGE(e.getMessage());	 
						ClientFail("File receive failed! \nException:"+e.getMessage());
						break;
					}						
				}
				else
				{
					errPacketNum ++;
					if(ret == -1&&errPacketNum<2)
					{
						continue;
					}
					break;
				}
			}
			if(!mThreadRunning)
			{
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return false;
			}
			if(wroteBytes>fileSize || errPacketNum > 0)
			{
				GoodixLink.runStatus = GoodixLink.ERROR;
				mMsg = "File receive failed(Error Data)!.";
				GoodixLink.LOGE(mMsg);
			}
			ClientFail(mMsg);
		} catch (FileNotFoundException e) {				
			GoodixLink.LOGE(e.getMessage());
			GoodixLink.runStatus = GoodixLink.ERROR;
			ClientFail("File receive failed.\nException:"+e.getMessage());
			return false;
		}
		return false;
	}
	
	private void wifiClient()
	{		
		File file = new File(mFilePath);		
		ifWifiConnected = false;
		int clientIpAddr = mWifiAdmin.getIPAddress();
		GoodixLink.LOGD("wifi client ip:"+(clientIpAddr&0xFF) +
						"." + ((clientIpAddr>>8)&0xFF) + 
						"." + ((clientIpAddr>>16)&0xFF)+ 
						"." + ((clientIpAddr>>24)&0xFF));
		GoodixLink.LOGD("Wifi Client begin create socket!");

		if(mWifiAdmin.isWifiContected(mContext) == WifiAdmin.WIFI_CONNECTED && mWifiAdmin.getBSSID() != mSsid)
		{
			GoodixLink.LOGD("Wifi has been connected,wait for disconnected!");
			mWifiAdmin.disconnectWifi(mWifiAdmin.getNetworkId());
			GoodixJNI.delay(500);
		}
		createWificonnect(mSsid);
		
		int timeOut = 0;
		
		while(mThreadRunning&&!ifWifiConnected&&(timeOut++ < 300))
		{ 
      		if(mWifiAdmin.isWifiContected(mContext) == WifiAdmin.WIFI_CONNECTED)
      		{
      			GoodixLink.LOGD("Wifi client connect success");
      			ifWifiConnected = true;
      		}
      		GoodixJNI.delay(100);
		}
		
		if(!mThreadRunning)
		{
			LoadingFlash.mLoadingFlash.stopFlash();
			return ;
		}
		if(timeOut >= 300)
		{
			GoodixLink.runStatus = GoodixLink.ERROR;
			GoodixLink.LOGE("Link service wait wifi connected timeout!");
			mMsg = "Create wifi connection timeout";
			ClientFail(mMsg);
			LoadingFlash.mLoadingFlash.stopFlash();
			return ;
		}
		
		int retry = 0;
		mWifiSocket = new Socket();
		mWifiSocket.setPerformancePreferences(0, 0, 1);
		try {
			mWifiSocket.setSoLinger(true, 0);
			mWifiSocket.setKeepAlive(true);
			mWifiSocket.setReuseAddress(true);
		} catch (SocketException e2) {
			GoodixLink.LOGE("Wifi socket set Exception:"+e2.getMessage());
		}
		try {
			if(!mWifiSocket.isConnected())
			{
				mWifiSocket.connect(hostSoAddr, 30000);
			}
			
		} catch (IOException e1) {
			GoodixLink.LOGE("Wifi client socket connect Exception:" + e1.getMessage());
			retry = 50;
		}
//		try {
//			mWifiSocket.setSoTimeout(1000);
//		} catch (SocketException e1) {
//			GoodixLink.LOGE("mWifiSocket set timeout exception:"+e1.getMessage());
//		}
//		while(retry++ < 30&&mThreadRunning){
//			try {
//				mWifiSocket = new Socket(hostIpAddr, GoodixLink.wifiPort);
//				break;
//			} catch (UnknownHostException e) {
//				GoodixLink.LOGE(e.getMessage());
//				mMsg = "Wifi connection created failed," + e.getMessage();
//			} catch (IOException e) {
//				GoodixLink.LOGE("wifisocket connect IOException:"+e.getMessage());
//				mMsg = "Wifi connection created failed," + e.getMessage();
//			}finally{
//				
//			}
//			//GoodixJNI.delay(100);
//		}
		LoadingFlash.mLoadingFlash.stopFlash();
		if(!mThreadRunning)
		{
			return;
		}
		if(retry >= 50)
		{
			mMsg = "Wifi socket created failed,timeout!";
			mWifiAdmin.disconnectWifi(mWifiAdmin.getNetworkId());
			GoodixLink.LOGE(mMsg);
			ClientFail(mMsg);
			return ;
		}
		mMsg = "Wifi connction created successfully.\n Begin receive data!";
		
		try {			
			wifiClientIs = mWifiSocket.getInputStream();
			wifiClientFos = new FileOutputStream(file);
			GoodixLink.LOGD("Wifi Client begin receiving data!");

			copyFile(wifiClientIs, wifiClientFos);
			if(!mThreadRunning)
			{
				return;
			}
			GoodixLink.runStatus = GoodixLink.TRANSFER_END;
			fileTransferSuccess = true;
            GoodixLink.LOGD("Wifi Client receive data successfully!");

			mMsg = "Data Received successfully!\nPath:"+file.getPath();
            ClientComplete(mMsg);
			
		} catch (Exception e) {
			GoodixLink.LOGE(e.getMessage());
            GoodixLink.runStatus = GoodixLink.ERROR;
            mMsg = "Wifi file receive failed! \nException:"+e.getMessage();
            ClientFail("Wifi file receive failed! \nException:"+e.getMessage());
		} finally {
			GoodixLink.LOGD("mWifiAdmin disconnected!");
			if(wifiClientIs != null) {
			    try {
					wifiClientIs.close();
					wifiClientFos.close();
					wifiClientIs = null;
					wifiClientFos = null;
				} catch (IOException e) {
					GoodixLink.LOGE("wifiClientIs close IO Exception");
				}
			}
			mWifiAdmin.disconnectWifi(mWifiAdmin.getNetworkId());
            if (mWifiSocket != null) {
                if (mWifiSocket.isConnected()) {
                    try {
                    	GoodixLink.LOGD("mWifiSocket closed");
                    	mWifiSocket.close();
                    } catch (IOException e) {
                        GoodixLink.LOGD("close mWifiSocket IO Exception!");
                    }
                }
                mWifiSocket = null;
            }              
        }
	}
	
	private boolean BluetoothProfile(byte[] buf,int len)
	{
		String macAddr = GoodixJNI.IntToHex((buf[2]));
		int i = 0;
		for(i = 1; i < buf[1];i++)
		{
			macAddr += ":";
			macAddr += GoodixJNI.IntToHex(buf[2+i]);			
		}
		GoodixLink.LOGD("link received bluetooth MAC address:"+macAddr);
		if(mBtAdapter==null)
		{
			GoodixLink.LOGE("Not support bluetooth");
			return false;
		}
		if(!mBtAdapter.isEnabled())
		{
			GoodixLink.LOGD("Bluetooth is disabled,enable it!");	
			mBtAdapter.enable();
			i = 0;
			while(!mBtAdapter.isEnabled())
			{
				GoodixJNI.delay(100);
				if(i++>200)
				{
					break;
				}
			}
		}
		
		isNeedCoseBT = false;
		mBtAdapter.cancelDiscovery();
		mDevice = mBtAdapter.getRemoteDevice(macAddr);
		GoodixLink.LOGD("Received Device is "+mDevice);
		int bondstate = mDevice.getBondState();
		int a2dpState = mBtAdapter.getProfileConnectionState(BluetoothProfile.A2DP);
		int headset = mBtAdapter.getProfileConnectionState(BluetoothProfile.HEADSET);
		GoodixLink.LOGD("a2dpstate:" + a2dpState + "and bondstate:" + bondstate);
		if(bondstate != BluetoothDevice.BOND_NONE && (a2dpState == BluetoothProfile.STATE_CONNECTED||headset == BluetoothProfile.STATE_CONNECTED))
		{			
//			ShowTips("正在断开连接...",0);
			GoodixLink.LOGD("a2dp is already connected, disconnect it !");
			isNeedCoseBT = true;
			MagicService.disconnect(mDevice);	
		}						
		else {
			if(bondstate == BluetoothDevice.BOND_NONE)
			{
//				ShowTips("正在连接...",0);
				mBtAdapter.cancelDiscovery();
				GoodixLink.LOGD("create bond !");
				//mDevice.createBond();
				try {
					ClsUtils.invokeMethod(mDevice, "createBond");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else if(bondstate == BluetoothDevice.BOND_BONDED)
			{
//				ShowTips("正在连接...",0);
				//mBtAdapter.cancelDiscovery();
				GoodixLink.LOGD("create headset and a2dp connection !");
				MagicService.connect(mDevice);
			}
			else {
				ShowTips("Invalid bondstate!", 0);
				GoodixLink.LOGD("invalid bondstate !");
			}
		}
		return true;
	}
	
	private void createWificonnect(String SSID)
	{
		short retry = 0;
		GoodixLink.LOGD("Link server create wifi connect");
		if(mWifiAdmin == null){
			GoodixLink.LOGE("Link service null wifiadmin!");
			return;
		}
		GoodixLink.LOGD("Wifi client begin open wifi");
		mWifiAdmin.openWifi();		//此句没有生效?后面需要等待开启成功
		while(retry++<600)
        {
            if(mWifiAdmin.checkState() == WifiManager.WIFI_STATE_ENABLED)
            {
                GoodixLink.LOGD("Wifi client open wifi success");
                break;
            }
			try{   
              	Thread.sleep(100);  
            }  
            catch(InterruptedException ie){
           }  
        }
		
        android.provider.Settings.System.putInt(mContext.getContentResolver(), android.provider.Settings.System.WIFI_USE_STATIC_IP, 1); // to define it use static ip's
        android.provider.Settings.System.putString(mContext.getContentResolver(), android.provider.Settings.System.WIFI_STATIC_IP,"192.168.43.15");
        android.provider.Settings.System.putString(mContext.getContentResolver(), android.provider.Settings.System.WIFI_STATIC_NETMASK,"255.255.255.0");
        android.provider.Settings.System.putString(mContext.getContentResolver(), android.provider.Settings.System.WIFI_STATIC_DNS1,"192.168.43.1");
        android.provider.Settings.System.putString(mContext.getContentResolver(), android.provider.Settings.System.WIFI_STATIC_GATEWAY,"192.168.43.1"); 
		
		GoodixLink.LOGD("Wifi client begin connect.");
		mWifiAdmin.addNetwork(SSID, GoodixLink.WIFI_HOTSPOT_KEY, WifiAdmin.TYPE_WPA);
	}
		          
    private void ClientComplete(String msg)
    {
    	//20130916
    	if(fileTransferSuccess)
    	{  		
    		GoodixLink.receivedFilePath = mFilePath;
			GoodixLink.LOGD("open file "+mFilePath);
    		if(mFilePath.contains(".##########"))
			{
				Intent intent = new Intent(ClientService.this,LinkShowText.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.putExtra("link.filepath", mFilePath);
				startActivity(intent);
				LinkFloatWindowManager.removeSmallWindow(getApplicationContext());
			}
			else
			{
				if(GoodixJNI.readSetting(this, MagicLink.mOpenFileState))
				{
					FileOperate.doOpenFile(mFilePath, mContext);
					LinkFloatWindowManager.removeSmallWindow(getApplicationContext());
				}
			}  		
    	}
    	else
    	{
    		if(GoodixJNI.readSetting(this, MagicLink.mOpenFileState))
			{
				LinkFloatWindowManager.removeSmallWindow(getApplicationContext());
			}
    	}
    	
    	if(GoodixLink.serverOrClient == ServerOrClient.CLIENT&&mThreadRunning)
    	{
    		Intent intent = new Intent(GoodixLink.GOODIXLINK_CILENT_COMPLETE_ACTION);
    		intent.putExtra(GoodixLink.GOODIXLINK_MESSAGE_INFO, msg);
    		sendOrderedBroadcast(intent,null);    		
    	}       	
    }
    
    private void ClientFail(String msg)
    {
    	FileOperate.deleteFile(mFilePath);
    	FloatWindowService.refreshTips("Failed");
    	ClientComplete(msg);
    }
    
    private void copyFile(InputStream is,FileOutputStream fos) throws IOException,Exception  
    {
    	byte[] buf = new byte[mLimitSize];
		int len = 0,wroteBytes = 0;
		wifiFrameTimeoutFlag = false;
		mTimer.schedule(wdtTask, 5000, 5000);
		wifiFrameTimeoutFlag = false;
		while((wroteBytes < fileSize)&&(len = is.read(buf))!=-1&&mThreadRunning)
		{
			fos.write(buf, 0, len);
			wroteBytes += len;
			int progress = (int) (wroteBytes*100f/fileSize);
			FloatWindowService.refreshTips(progress+"%");	
			wifiFrameTimeoutFlag = false;
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
		is.close();
		fos.close();		
    }

    private void ShowTips(String msg,int what)
    {
    	if(mHandler!=null)
    	{
    		Message msg1 = mHandler.obtainMessage();
    		msg1.obj = msg;
    		msg1.what = what;
    		mHandler.sendMessage(msg1);
    	}
    }

    private void showDialog(Context context)
	{
		mClickTims = 0;
		clickTimer = new Timer();
		mDialog=new LinkSlaveDilalog(mContext);
				
		View.OnClickListener cancelListener = new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				mClickTims++;
				if(mClickTims == 1&&GoodixLink.serverOrClient == ServerOrClient.CLIENT)
				{					
					if(clickTask != null)
					{
						clickTask.cancel();	
						clickTask = null;
					}
					clickTask = new TimerTask()
					{
						@Override
						public void run() {
							mClickTims = 0;
						}						
					};
					clickTimer.schedule(clickTask, 5000);
					//setAlertDialogCloseable(dialog, false);
					ShowTips("Click again, exit the transmission.",0);
					return ;
				}
				
				if(clickTask != null)
				{
					clickTask.cancel();
					clickTask = null;
				}
				mClickTims = 0;
				GoodixLink.runStatus = GoodixLink.ERROR;
				ClientFail("Transfer be cancel");
				//setAlertDialogCloseable(dialog, true);	
				mDialog.cancel();
			}
		};	
		mDialog.setCancelButton(cancelListener);
		
		mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);//设定为系统级警告，关键 
		
		mDialog.show();
		mDialog.setCanceledOnTouchOutside(false);
		//mDialog.setCancelable(false);
	}
}
