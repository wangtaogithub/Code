package com.magic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.floatwindow.FloatWindowService;
import com.floatwindow.LinkFloatWindowManager;
import com.jni.GoodixJNI;
import com.log.LinkLog;
import com.tools.GoodixLink;
import com.tools.GoodixLink.ServerOrClient;

import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;

public class LinkShare extends Activity {

	private Uri fileuri;
    private int mSendFileLen = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		if(GoodixLink.serverOrClient == ServerOrClient.NONE)
		{
			FloatWindowService.refreshTips("Link");
		}

		Intent intent = getIntent();
	    String action = intent.getAction();
	    if(Intent.ACTION_SEND.equals(action))
	    {
	    	GoodixLink.receivedFilePath = null;
	    	fileuri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
    		if(fileuri == null)
    		{
    			CharSequence extra_text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
    			if(extra_text != null)
    			{
    				fileuri = creatFileForSharedContent(this, extra_text);
    			}	    			
    		}
	    	if(!GoodixJNI.readSetting(getApplicationContext(),MagicLink.mServerState))
	    	{
	    		Intent intent1 = new Intent();
	    		intent1.setClass(LinkShare.this, MagicLink.class);
	    		LinkShare.this.startActivityForResult(intent1, 0);
	    	}
	    	else 
	    	{
	    		startTransfer();
	    		this.finish();
	    	}	    	
	    }	    
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	  
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		return;
		//super.onBackPressed();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode==KeyEvent.KEYCODE_BACK) {
            return true;
	    }else if(keyCode==KeyEvent.KEYCODE_HOME){
	            return true;
	    }
	    else if(keyCode==KeyEvent.KEYCODE_MENU)
	    {
	    	return true;
	    }
		return super.onKeyDown(keyCode, event);
	}
	

	@Override
	public void onConfigurationChanged(Configuration arg0) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(arg0);
	}

	private Uri creatFileForSharedContent(Context context, CharSequence shareContent){
		if (shareContent == null) {
            return null;
        }
		File file = new File(GoodixJNI.getStoragePath()+"/.##########");
		if(file.exists())
		{
			file.delete();			
		}
		try {
			file.createNewFile();
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(shareContent.toString().getBytes());
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Uri.fromFile(file);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);		
		if(GoodixJNI.readSetting(getApplicationContext(),MagicLink.mServerState))
		{
			startTransfer();
			this.finish();
		}
		else
		{
			GoodixLink.ShowToast(getApplicationContext(),"The link service is not started");
		}
	} 
	
	private void startTransfer()
	{
		byte[] buf = new byte[2];
		int times = 0;
		
    	if(GoodixLink.serverOrClient != ServerOrClient.NONE)
    	{
    		GoodixLink.ShowToast(getApplicationContext(),"The link service is transfering");
    		LinkShare.this.finish();
    		return ;
    	}
    	do
    	{
    		GoodixJNI.I2CReadRegister(buf, 2, 0x81B0);
    		if((buf[0]&0xFF)!=0x00&&buf[0]==buf[1])
        	{  		        		
    			break;
        	}   		
    		GoodixJNI.delay(200);
    	}while(times++<5);
    	
    	if(times >= 5)
    	{
    		GoodixLink.ShowToast(getApplicationContext(),"Not Support Link Function!");
    		LinkShare.this.finish();
    		return;
    	}
        if(fileuri == null)
        {
        	GoodixLink.ShowToast(getApplicationContext(),"Not support the file Type!");
        	LinkShare.this.finish();
        	return ;
        }
    	GoodixLink.sendFilePath = fileuri.getPath();
    	Cursor cursor = getContentResolver().query(fileuri, null,   
				null, null, null); 		 
		//第一行第二列保存路径strRingPath
		if(cursor!=null)
		{
			int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			GoodixLink.sendFilePath = cursor.getString(index);
			cursor.close();
		}	
		//judge file size
		File sendFile = new File(GoodixLink.sendFilePath);
		if(!sendFile.exists())
		{
			GoodixLink.LOGE("file is not exists.");
			GoodixLink.ShowToast(getApplicationContext(), "The selected file is not exist,Please select another one!");
			LinkShare.this.finish();
			return ;
		}
		mSendFileLen = (int) sendFile.length();
		if(mSendFileLen == 0)
		{				
			GoodixLink.ShowToast(getApplicationContext(), "\nThis is an empty file! Please select another file");
			LinkShare.this.finish();
			return ;
		}
		Intent fwIntent = new Intent(LinkShare.this, FloatWindowService.class);
        startService(fwIntent);
		GoodixLink.LOGD("Please approach your mobilephone...");
		
		enterMasterSend();		
	}
	   	
	private void enterMasterSend()
	{
		LinkLog.log.Add("Enter master mode.");
		
		if(!GoodixJNI.enterMasterMode()) {
			GoodixLink.LOGE("Link share enter master mode failed!");
			LinkFloatWindowManager.removeSmallWindow(getApplicationContext());
			return ;
		}
		GoodixLink.runStatus = GoodixLink.TRANSFER_START;
		GoodixLink.serverOrClient = ServerOrClient.SERVER;
		FloatWindowService.refreshTips("Ready");
	}
		
}
