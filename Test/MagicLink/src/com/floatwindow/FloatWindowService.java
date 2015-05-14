package com.floatwindow;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.tools.GoodixLink;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

public class FloatWindowService extends Service {

//	/**
//	 * �������߳��д������Ƴ���������
//	 */
	private Handler handler = new Handler();
//
//	/**
//	 * ��ʱ������ʱ���м�⵱ǰӦ�ô��������Ƴ���������
//	 */
//	private static Timer timer;
//	
	public static String floatMsg = "Link";
	private static boolean isShow = true;
	private static Handler mHandler;
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		GoodixLink.LOGD("FloatWindowService onCreate");		
		if(mHandler==null)
		{
			mHandler = new Handler()
			{
				@Override
				public void handleMessage(Message msg) {
					// TODO Auto-generated method stub
					super.handleMessage(msg);
					if(LinkFloatWindowManager.isWindowShowing())
					{
						LinkFloatWindowManager.updateUsedPercent(msg.obj.toString());				
					}			
				}				
			};
		}
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// ������ʱ����ÿ��0.5��ˢ��һ��
		GoodixLink.LOGD("FloatWindowService onStartCommand");
//		if (timer == null) {
//			timer = new Timer();
//			timer.scheduleAtFixedRate(new RefreshTask(), 0, 500);
//		}
		if(!LinkFloatWindowManager.isWindowShowing())
		{
			LinkFloatWindowManager.createSmallWindow(getApplicationContext());
			if(!isShow)
			{
				LinkFloatWindowManager.updateUsedPercent(floatMsg);
				isShow = true;
			}			
		}
		else
		{
			LinkFloatWindowManager.updateUsedPercent("Link");
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Service����ֹ��ͬʱҲֹͣ��ʱ����������
//		if(timer != null) {
//			timer.cancel();
//			timer = null;
//		}
		if(mHandler != null)
		{
			mHandler = null;
		}
	}
	
//	public static void stopShowSmallWindow() {
//		if(timer != null) {
//			timer.cancel();
//			timer = null;
//		}
//		floatMsg = "Link";
//	}
	
	public static void refreshTips(String msg) {
//		if(timer == null) {
//			GoodixLink.LOGE("refresh timer is null!");
//		}		
		LoadingFlash.mLoadingFlash.stopFlash();
		if(mHandler!=null)
    	{
    		Message msg1 = mHandler.obtainMessage();
    		msg1.obj = msg;
    		msg1.what = 0;
    		mHandler.sendMessage(msg1);   		
    	}
		else
		{
			isShow = false;
			floatMsg = msg;
			GoodixLink.LOGE("refresh handler is null!");
		}
	}

	public static void refreshLoading(String msg)
	{
//		floatMsg = msg;
		if(mHandler!=null&&!mHandler.hasMessages(0,msg))
    	{
    		Message msg1 = mHandler.obtainMessage();
    		msg1.obj = msg;
    		msg1.what = 0;
    		mHandler.sendMessage(msg1);
    	}
	}
	
	class RefreshTask extends TimerTask {

		@Override
		public void run() {
			// ��ǰ���������棬��û����������ʾ���򴴽���������
			if (isHome() && !LinkFloatWindowManager.isWindowShowing()) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						LinkFloatWindowManager.createSmallWindow(getApplicationContext());
					}
				});
			}
			// ��ǰ���治�����棬������������ʾ�����Ƴ���������
			/*
			else if (!isHome() && LinkFloatWindowManager.isWindowShowing()) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						LinkFloatWindowManager.removeSmallWindow(getApplicationContext());
						//LinkFloatWindowManager.removeBigWindow(getApplicationContext());
					}
				});
			}
			*/
			// ��ǰ���������棬������������ʾ��������ڴ����ݡ�
			else if (isHome() && LinkFloatWindowManager.isWindowShowing()) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						LinkFloatWindowManager.updateUsedPercent(floatMsg);
					}
				});
			}
		}

	}

	/**
	 * �жϵ�ǰ�����Ƿ�������
	 */
	private boolean isHome() {
//		ActivityManager mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//		List<RunningTaskInfo> rti = mActivityManager.getRunningTasks(1);
//		return getHomes().contains(rti.get(0).topActivity.getPackageName());
		return true;
	}

	/**
	 * ������������Ӧ�õ�Ӧ�ð�����
	 * 
	 * @return ���ذ������а������ַ����б�
	 */
	private List<String> getHomes() {
		List<String> names = new ArrayList<String>();
		PackageManager packageManager = this.getPackageManager();
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(intent,
				PackageManager.MATCH_DEFAULT_ONLY);
		for (ResolveInfo ri : resolveInfo) {
			names.add(ri.activityInfo.packageName);
		}
		return names;
	}
}