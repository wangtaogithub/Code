package com.floatwindow;

import com.jni.GoodixJNI;
import com.tools.*;

public class LoadingFlash
{
	public static LoadingFlash mLoadingFlash = new LoadingFlash();
	private boolean mExitThread = false;
	private String mMsg = "";
	private int mCount = 0;
	private int mSleepTime = 200;
	private Thread mThread = null;
	public LoadingFlash(int sleepT)
	{
		mSleepTime = sleepT;		
	}
	
	public LoadingFlash()
	{

	}
	
	public void startFlash()
	{
		if(mThread == null ||mExitThread)
		{
			if(mExitThread)
			{
				GoodixJNI.delay(mSleepTime);
			}
			mExitThread = false;
			mMsg = "";
			mCount = 0;
			mThread = new refreshFloatWindowThread();
			mThread.start();
		}
		else {
			GoodixLink.LOGD("refreshFloatWindowThread is already exits.");
		}
	}
	
 	public void stopFlash()
	{
		mExitThread = true;
	}
	
	private class refreshFloatWindowThread extends Thread
	{
		@Override
		public void run() {
			// TODO Auto-generated method stub
			while(!mExitThread)
			{
				if(mCount++ >5)
				{
					mMsg = "";
					mCount = 0;
				}
				else
				{
					mMsg += ".";
				}
				FloatWindowService.refreshLoading(mMsg);
				try {
					Thread.sleep(mSleepTime);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}			
	}

}