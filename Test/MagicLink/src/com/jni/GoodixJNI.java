package com.jni;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.tools.GoodixLink;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

public class GoodixJNI {
	
	static{
		try {
			System.loadLibrary("GoodixTools");
		} catch (UnsatisfiedLinkError e) {
			GoodixLink.LOGE("Can't find file of .so."+e.toString());
		}
	}

	public native static int I2CReadRegister(byte[] buf, int i, int j) ;

	public native static int I2CWriteRegister(byte[] buf, int i, int j) ;
		
	public native static void delay(int i) ;

	public native static int GuitarReset() ;
	
	public native static int downLinkCode();

	public native static byte getCheckSm(byte[] buf, int length) ;

	public native static boolean checkCRC(byte[] tempbuf, int i, byte rcvlength) ;
	
	public native static boolean readPairState();
	
	public native static boolean clearPairBuf();
	
	public native static boolean enterSlaveMode();
	
	public native static boolean enterMasterMode();
	
	public native static boolean enterTransferMode();
	
	public native static boolean exitSlaveMode();
	
	public native static boolean exitMasterMode();
	
	public native static boolean exitTransferMode();
	
	public native static boolean sendData(byte[] buf, int length);
	
	public native static int receiveData(byte[] buf);
	
	public native static  boolean CheckAuthorization();

	public static String getStoragePath() {
		String rootPath;
		if(!Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
		{
			rootPath="/";			
		}
		else
		{
			rootPath = Environment.getExternalStorageDirectory().getPath();
		}
		
		return rootPath;
	}

	public static boolean readSetting(Context context,String key)
    {	
    	SharedPreferences settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE); 
		boolean LinkServiceState = settings.getBoolean(key, false);	 
		return LinkServiceState;
    }
	
	public static boolean readSetting(Context context,String key,boolean defval)
    {	
    	SharedPreferences settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE); 
		boolean LinkServiceState = settings.getBoolean(key, defval);	 
		return LinkServiceState;
    }
	
	public static void saveSetting(Context context ,String key, boolean ServiceState)
	{
		SharedPreferences settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE); 
		SharedPreferences.Editor editor = settings.edit(); 
		editor.putBoolean(key, ServiceState);
		editor.commit();  
	}

	public static String IntToHex(byte data)
	{
		String result = "";
		char ch  ;
		int temp = data;
		
		if((temp&0xF0)>0x90)
		{
			ch = (char) (((temp>>4)&0x0F)+'A'-10);
		}
		else 
		{
			ch = (char) (((temp>>4)&0x0F)+'0');
		}
		result += ch;
		if((temp&0x0F)>0x09)
		{
			ch = (char) ((temp&0x0F)+'A'-10);
		}
		else 
		{
			ch = (char) ((temp&0x0F)+'0');
		}
		
		result += ch;
		return result;
	}

	public static void ConserveFile(String path, byte[] buf, int i) {
		// TODO Auto-generated method stub
		File file=new File(path);
		FileOutputStream out;
		
		try {
			if(!file.exists()||file.length()>GoodixLink.M)
			{
				file.createNewFile();
			}
			out=new FileOutputStream(file,true);
			out.write(buf,0,i);
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
}
