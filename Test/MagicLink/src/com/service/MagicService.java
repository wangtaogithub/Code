package com.service;

import com.floatwindow.FloatWindowService;
import com.jni.GoodixJNI;
import com.log.LinkLog;
import com.magic.MagicLink;
import com.magic.R;
import com.reflectUtils.ClsUtils;
import com.tools.GoodixLink;
import com.tools.GoodixLink.ServerOrClient;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.KeyguardManager.KeyguardLock;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.widget.Toast;

public class MagicService extends Service{
	private static Vibrator mVibrator;
    private static SoundPool mSoundPool = null; 
    private static int mMagicId;
    private static int mFailedId;
	private boolean mLinkThreadRuning = false;
    private boolean mScreenOff = false;
    private CheckPairedThread mThread = null;
    private ServerService mServer = null;
    private ClientService mClient = null;
    //灭屏相关变量
    private static DevicePolicyManager mDevicePolicyManager=null;
    //锁屏问题
    private static PowerManager mPowerManager;
	private static KeyguardManager mKeyguard;
	private static KeyguardLock mKeylock;
	static PowerManager.WakeLock mPowerScreenOn;
	PowerManager.WakeLock mCPU;
    //定义蓝牙所需变量
	private static BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
	private static BluetoothA2dp mA2dp = null;
	private static BluetoothHeadset mHeadset = null;
	private static BluetoothDevice mDevice = null;
	private static boolean isHeadsetConnected = false;
	private static boolean isA2dpConnected = false;
	private boolean onStart = false;
	
	private static Handler mToastHandler;
    BroadcastReceiver mainReceiver = new BroadcastReceiver()
    {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(Intent.ACTION_SCREEN_OFF.equals(action))
			{				
				mScreenOff = true;
				GoodixLink.LOGI("<system-broadcast>screen off");
			}
			else if(Intent.ACTION_SCREEN_ON.equals(action))
			{
				GoodixLink.LOGI("<system-broadcast>screen on");
				mScreenOff = false;	
			}
			else if(intent.getAction().equals(GoodixLink.GOODIXLINK_SERVER_COMPLETE_ACTION))
			{
				GoodixLink.LOGD("<server-broadcast>runStatus:" + GoodixLink.runStatus);
//				LinkFloatWindowManager.removeSmallWindow(MagicService.this);
				if(mServer!=null&&serverConnect!=null)
				{
					unbindService(serverConnect);
					mServer = null;
				}			
				if(GoodixLink.serverOrClient != ServerOrClient.NONE)
				{
					shutdown();
				}
				//LinkShare.finishActivity();
				String msg = intent.getExtras().getString(GoodixLink.GOODIXLINK_MESSAGE_INFO);
				GoodixLink.ShowToast(context, msg);
			}
			else if(intent.getAction().equals(GoodixLink.GOODIXLINK_CILENT_COMPLETE_ACTION))
			{				
				GoodixLink.LOGD("<client-broadcast>runStatus:" + GoodixLink.runStatus);
//				if(GoodixJNI.readSetting(MagicService.this, MagicLink.mOpenFileState))
//				{
//					LinkFloatWindowManager.removeSmallWindow(MagicService.this);
//				}
				
				if(mClient!=null&&clientConnect!=null)
				{
					unbindService(clientConnect);
					mClient = null;
				}
				if(GoodixLink.serverOrClient != ServerOrClient.NONE)
				{
					shutdown();
				}
				
				String msg = intent.getExtras().getString(GoodixLink.GOODIXLINK_MESSAGE_INFO);
				if(!msg.contains("This is an bluetooth Mac Address"))
				{
					GoodixLink.ShowToast(context, msg);
				}
				
			}
			else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action))
            {
            	if(!GoodixLink.isBTA2dp)
            	{
            		return;
            	}
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            	switch(device.getBondState())
            	{
                case BluetoothDevice.BOND_BONDED: 
	            	 GoodixLink.LOGD("<system-broadcast>paired,connecting....");  
	            	 connect(device);
                    break;  
                case BluetoothDevice.BOND_BONDING:
                	GoodixLink.LOGD("<system-broadcast>pairing");  
	                 break; 
                case BluetoothDevice.BOND_NONE:
                	GoodixLink.LOGD("<system-broadcast>unpaired");  
	                 break; 
                default: 
                    break; 
            	}
            }
            else if(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action))
            {        	
            	BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            	if(dev.equals(mDevice))
            	{
            		int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
            		switch(state)
            		{
            		case BluetoothProfile.STATE_CONNECTED:
            			GoodixLink.LOGD("<system-broadcast>A2dp connected");
            			isA2dpConnected = true;
            			ClientService.isNeedCoseBT = true;
            			
                    	if(!GoodixLink.isBTA2dp)
                    	{
                    		GoodixLink.LOGD("Auto connected A2dp!");
                    		return;
                    	}
            			
            			GoodixLink.ShowToast(MagicService.this, "已连接"+dev.getName());
            			GoodixLink.isBTA2dp = false;
            			try {
            				PackageManager pm = context.getPackageManager();
            				Intent in = pm.getLaunchIntentForPackage("com.android.music");
            				context.startActivity(in);
            				Intent intent1 = new Intent("com.android.music.musicservicecommand");
            				intent1.putExtra("command", "play");
            				context.sendBroadcast(intent1);
            				int retry = 0;
            				while(!mA2dp.isA2dpPlaying(mDevice) && retry++ < 10)
            				{
//            					FileOperate.playAudio(getApplicationContext()); 
                				GoodixJNI.delay(200);
                				context.sendBroadcast(intent1);
            				}
            				
            				LinkLog.log.Add("retry time :"+retry);
						} catch (Exception e) {
							GoodixLink.LOGE("paly music throws exception");
						}
						
            			break;
            		case BluetoothProfile.STATE_DISCONNECTED:
            			isA2dpConnected = false; 
            			GoodixLink.LOGD("<system-broadcast>A2dp disconnected");
            			if(!GoodixLink.isBTA2dp || isHeadsetConnected)
                    	{
            				GoodixLink.LOGD("Auto disconnected A2dp!");
                    		return;
                    	}
//            			if(ClientService.isNeedCoseBT) {
//            				GoodixLink.LOGD("A2dp & Headset are all disconnected, disable BT!");
//            				mBtAdapter.disable();
//            			}
 
            			GoodixLink.ShowToast(MagicService.this, "已断开连接"+dev.getName()); 
            			GoodixLink.isBTA2dp = false;            			
            			break;
            		}           		
            	}
            	else
            	{
            		GoodixLink.LOGD("EXTRA_DEVICE:"+dev);
            	}
            }
            else if(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action))
            {
//            	if(!GoodixLink.isBTA2dp)
//            	{
//            		return;
//            	}
            	BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            	if(dev.equals(mDevice))
            	{
            		int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
            		switch(state)
            		{
            		case BluetoothProfile.STATE_CONNECTED:
            			GoodixLink.LOGD("<system-broadcast>Headset connected");
            			ClientService.isNeedCoseBT = true;
            			isHeadsetConnected = true;
            			break;
            		case BluetoothProfile.STATE_DISCONNECTED:
            			GoodixLink.LOGD("<system-broadcast>Headset disconnected");
            			isHeadsetConnected = false;
            			/*
            			if(!isA2dpConnected) {
            				GoodixLink.LOGD("Headset & A2dp are all disconnected, close BT!");
            				mBtAdapter.disable();
            			}
            			*/
            			break;
            		}
            	} 
            	else
            	{
            		GoodixLink.LOGD("EXTRA_DEVICE:"+dev);
            	}
            }
            else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action))
            {
            	int state = intent.getExtras().getInt(BluetoothAdapter.EXTRA_STATE);
            	int prestate = intent.getExtras().getInt(BluetoothAdapter.EXTRA_PREVIOUS_STATE);
        		String[] strState = new String[4];
        		strState[0] = "STATE_OFF";
        		strState[1] = "STATE_TURNING_ON";
        		strState[2] = "STATE_ON";
        		strState[3] = "STATE_TURNING_OFF";
            	GoodixLink.LOGD("BluetoothAdapter state:"+strState[prestate - 10]+
            			" -> "+strState[state - 10]);
            }
           
        }   	
    };
    
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
		if(GoodixJNI.readSetting(this, MagicLink.mSaveLogState)){
			if(!LinkLog.log.Init()){
				Toast.makeText(this, "LinkLog service start failed", Toast.LENGTH_SHORT).show();
			}
		}
		
		LinkLog.log.Add("MagicService init.");
				
		mVibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE); 
		mSoundPool = new SoundPool(3, AudioManager.STREAM_SYSTEM, 5);
		mMagicId = mSoundPool.load(this, R.raw.magic, 0);
		mFailedId = mSoundPool.load(this, R.raw.failed, 0);
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(GoodixLink.GOODIXLINK_SERVER_COMPLETE_ACTION);
		filter.addAction(GoodixLink.GOODIXLINK_CILENT_COMPLETE_ACTION);
		//蓝牙音箱使用到得广播
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
		filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		
		registerReceiver(mainReceiver,filter);
		//灭屏权限申请
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mPowerManager= (PowerManager) getSystemService(Context.POWER_SERVICE);
        mPowerScreenOn = mPowerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP|PowerManager.FULL_WAKE_LOCK, "goodixlink_bright");
        mPowerScreenOn.setReferenceCounted(false);
        mCPU = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "goodixlink_cpu");
        mCPU.acquire();
        
        mKeyguard = (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
        mKeylock = mKeyguard.newKeyguardLock("GoodixToolKg");
        mKeylock.disableKeyguard();
        
        mBtAdapter.getProfileProxy(MagicService.this, mConnectionCallback, BluetoothProfile.HEADSET);
		mBtAdapter.getProfileProxy(MagicService.this, mConnectionCallback, BluetoothProfile.A2DP);

        mLinkThreadRuning = true;
		mThread = new CheckPairedThread();
		mThread.start();
		
		mToastHandler = new Handler()
		{
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				GoodixLink.ShowToast(getApplicationContext(), msg.obj.toString());			
			}			
		};
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.unregisterReceiver(mainReceiver);
		mBtAdapter.closeProfileProxy(BluetoothProfile.HEADSET,mHeadset);
		mBtAdapter.closeProfileProxy(BluetoothProfile.A2DP,mA2dp);
		mPowerScreenOn.release();
		mCPU.release();
		mKeylock.reenableKeyguard();
		mLinkThreadRuning = false;
		mToastHandler = null;
		shutdown();
		
		GoodixJNI.exitSlaveMode();
		stopForeground(true);
		LinkLog.log.Add("MagicService stop.");
		LinkLog.log.Stop();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		if(!onStart)
		{
			onStart = true;
			Notification noti = new Notification();
			String msg = getString(R.string.app_name) + " service started";
	    	long[] vib = {0,100,0,0};	
	    	noti.vibrate = vib;
	    	noti.defaults |= Notification.DEFAULT_VIBRATE;
	    	noti.when = System.currentTimeMillis();
	    	noti.flags = Notification.FLAG_ONGOING_EVENT;
	    	noti.tickerText = msg;
	    	noti.icon = R.drawable.link_notification;
	    	//noti.largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.goodix);
	    	Intent notificationIntent = new Intent(this,MagicLink.class); //点击该通知后要跳转的Activity
	    	notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    	PendingIntent contentIntent = PendingIntent.getActivity(this,0,notificationIntent,0);
	    	noti.setLatestEventInfo(MagicService.this, msg, "Touch to set", contentIntent);
			startForeground(startId, noti);
		}    	
		return super.onStartCommand(intent, flags, startId);		
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		//GoodixJNI.saveServiceState(MagicService.this,false);
	}

	private class CheckPairedThread extends Thread
	{
		public void run()
		{	
			LinkLog.log.Add("CheckPairedThread# Clear paired buffer.");			
            GoodixJNI.clearPairBuf();
            
			while(mLinkThreadRuning)
			{		
				GoodixJNI.enterSlaveMode();								
				if(GoodixJNI.readPairState()){
					LinkLog.log.Add("Link paired successful.");
					break;
				}							
				GoodixJNI.delay(50);
			}
			
			if(!mLinkThreadRuning)
			{
				LinkLog.log.Add("Exit link thread.");
				return ;
			}
			
			LinkLog.log.Add("Clear pair buffer.");			
			GoodixJNI.clearPairBuf();
			GoodixLink.runStatus = GoodixLink.TRANSFER_ING;
			LinkLog.log.Add("Lock screen.");
			if(!GoodixLink.lockScreen(MagicService.this, mDevicePolicyManager, mPowerManager))
			{
		        GoodixLink.runStatus = GoodixLink.ERROR;
		        GoodixLink.ShowToast(MagicService.this, "No automatic lock screen permissions ");
				if(GoodixLink.runStatus != GoodixLink.NONE)
				{
					shutdown();
				}	
				return;
			}
			int i = 0;
			LinkLog.log.Add("downlinkCode start");
			i = GoodixJNI.downLinkCode();
			LinkLog.log.Add("downlinkCode result:"+i);
			i = 0;
			while(!mScreenOff)
			{
				GoodixJNI.delay(50);
				i++;
				if(i>40)
				{
					LinkLog.log.Add("Link wait screen off 2s timeout");
					break;
				}
			}
			
			if(!mLinkThreadRuning)
			{
				return;
			}
			LinkLog.log.Add("enter transfer mode");	
			if(!GoodixJNI.enterTransferMode()) {
				shutdown();
				FloatWindowService.refreshTips("Failed");
				return;
			}
			
			LinkLog.log.Add("exit master mode.");
			if(!GoodixJNI.exitMasterMode()) {
				shutdown();
				FloatWindowService.refreshTips("Failed");
				return;
			}
			
			Intent intent = new Intent();
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if(GoodixLink.serverOrClient == ServerOrClient.SERVER)
			{		
				//启动服务器发送数据	
				LinkLog.log.Add("**Link server enter transfer mode**");				
				intent.setClass(MagicService.this, ServerService.class);
				bindService(intent, serverConnect, Context.BIND_AUTO_CREATE);					
			}
			else 
			{
				//启动客户端接收数据
				LinkLog.log.Add("**Link client enter receive mode**");
				GoodixLink.serverOrClient = ServerOrClient.CLIENT;
				GoodixLink.receivedFilePath = null;
				Intent fwIntent = new Intent(MagicService.this, FloatWindowService.class);
				startService(fwIntent);
				intent.setClass(MagicService.this, ClientService.class);
				bindService(intent, clientConnect, Context.BIND_AUTO_CREATE);
			}				
			
		}
	}
	
	public static void connect(BluetoothDevice dev)
	{
//		while(mA2dp==null);
		ShowTips("正在连接...");
		mDevice = dev;
		GoodixLink.LOGD("connect to "+mDevice);

		try {
			//mHeadset.connect(mDevice);
			ClsUtils.invokeMethod(mHeadset, "connect", mDevice);
			GoodixJNI.delay(100);
			if(!isHeadsetConnected) {
				GoodixLink.LOGD("Retry connect Headset");
				//mHeadset.connect(mDevice);
				ClsUtils.invokeMethod(mHeadset, "connect", mDevice);
			}
		} catch (Exception e) {
			GoodixLink.LOGE(e.getMessage());
		}
		try {
			//mA2dp.connect(mDevice);
			ClsUtils.invokeMethod(mA2dp, "connect", mDevice);
			GoodixJNI.delay(100);
			if(!isA2dpConnected) {
				GoodixLink.LOGD("Retry connect A2dp");
				//mA2dp.connect(mDevice);
				ClsUtils.invokeMethod(mA2dp, "connect", mDevice);
			}
		} catch (Exception e) {
			GoodixLink.LOGE("error info:" + e.getMessage());
		}
	}
	
	public static void disconnect(BluetoothDevice dev)
	{
		if(mA2dp.getConnectionState(dev)!=BluetoothProfile.STATE_CONNECTED&&mHeadset.getConnectionState(dev)!=BluetoothProfile.STATE_CONNECTED)
		{	
			connect(dev);
		}
		else
		{
			ShowTips("正在断开连接...");
			mDevice = dev;
			try{
				if(mHeadset !=  null)
				{
					//mHeadset.disconnect(dev);
					ClsUtils.invokeMethod(mHeadset, "disconnect", dev);
//					while(isHeadsetConnected);
				}
				if(mA2dp !=  null)
				{
					//mA2dp.disconnect(dev);
					ClsUtils.invokeMethod(mA2dp, "disconnect", dev);
//					while(isA2dpConnected);
				}
			}
			catch(Exception e) {
				GoodixLink.LOGE(e.toString());
			}
		}
		
	}

	private void shutdown() {
		GoodixLink.LOGD("*******GoodixLink service shutdown*******"); 
		lightUpScreen();
 	    
		GoodixLink.isDebugMode = false;
 	    GoodixLink.serverOrClient = ServerOrClient.NONE;
 	    GoodixLink.runStatus = GoodixLink.NONE;
 	    GoodixLink.sendFilePath = null;	
 	   
 	    if(mThread != null)
	   	{   		
 	    	mThread.interrupt();
 	    	mThread = null;	    	
	   	}
 	    if(mLinkThreadRuning)
 	    {
 	    	mThread = new CheckPairedThread();
 	    	mThread.start();	    	
 	    } 	
	}
	
    private static void ShowTips(String msg)
    {
    	if(mToastHandler!=null)
    	{
    		Message msg1 = mToastHandler.obtainMessage();
    		msg1.obj = msg;
    		msg1.what = 0;
    		mToastHandler.sendMessage(msg1);
    	}
    }
    public static void lightUpScreen()
	{
    	GoodixLink.LOGD("exitTransferMode start");
		GoodixJNI.exitTransferMode() ;
		GoodixLink.LOGD("exitTransferMode end");
		GoodixJNI.exitMasterMode() ;	
		GoodixLink.unLockScreen(mPowerManager, mPowerScreenOn);
	}

    //震动提示
    public static void vibrateTips(boolean success)
    {
    	mVibrator.vibrate(200);
    	if(success)
    	{
    		mSoundPool.play(mMagicId, 2, 2, 0, 0, 1);
    	}
    	else
    	{
    		mSoundPool.play(mFailedId, 2, 2, 0, 0, 1);
    	}
    }
    
	private BluetoothProfile.ServiceListener mConnectionCallback = new BluetoothProfile.ServiceListener()
    {
		@Override
		public void onServiceConnected(int profile, BluetoothProfile proxy) {
			GoodixLink.LOGD("connected:"+profile);
			if(profile == BluetoothProfile.A2DP)
			{
				mA2dp = (BluetoothA2dp) proxy;			
			}
			else if(profile == BluetoothProfile.HEADSET)
			{
				mHeadset = (BluetoothHeadset) proxy;
			}			
		}

		@Override
		public void onServiceDisconnected(int profile) {
			GoodixLink.LOGD("disconnected:"+profile);
			if(profile == BluetoothProfile.A2DP)
			{
				mA2dp = null;			
			}
			else if(profile == BluetoothProfile.HEADSET)
			{
				mHeadset = null;
			}
		}    	
    };

    private ServiceConnection serverConnect = new ServiceConnection(){

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder service) {
			mServer = ((ServerService.ServerBinder)service).getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mServer = null;
		}
    	
    };
    
    private ServiceConnection clientConnect = new ServiceConnection(){

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder service) {
			mClient = ((ClientService.ClientBinder)service).getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mClient = null;
		}
    	
    };
}
