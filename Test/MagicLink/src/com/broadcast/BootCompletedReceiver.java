package com.broadcast;

import com.jni.GoodixJNI;
import com.magic.MagicLink;
import com.service.MagicService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompletedReceiver extends BroadcastReceiver{
	
	private static boolean linkServiceState=false;
	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED))
		{
			try  {
				linkServiceState = GoodixJNI.readSetting(context,MagicLink.mServerState);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if(linkServiceState) {
				Intent startIntent = new Intent(context,MagicService.class);
				context.startService(startIntent);
				return;
			}
		}
	}
}
