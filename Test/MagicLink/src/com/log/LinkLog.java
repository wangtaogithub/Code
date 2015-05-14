package com.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.text.SimpleDateFormat;
import android.util.Log;
import java.util.concurrent.*;

import com.tools.FileOperate;

public class LinkLog {
	
	public static LinkLog log = new LinkLog();
	
	private String TAG = "LinkLog";
	private LogThread mLogThread = null;
	private boolean mExitThread = false;
	private BufferedWriter mWriter = null;
	private BlockingQueue<String> logQueue = new LinkedBlockingQueue<String>();
	//private long count = 0;
	private boolean enable = false;
	private Date mLastMsgTime = new Date();
	private SimpleDateFormat mLogTimeFormat = new SimpleDateFormat("HH:mm:ss:SSS");
	private logcatThread mlogcatThread;
	public LinkLog(){
		
	}
	
	private String GetTimeStamp(){
		Date now = new Date();
		long delt = now.getTime() - mLastMsgTime.getTime();
		mLastMsgTime = now;
		return "[" +  mLogTimeFormat.format(now) + String.format(" %5d]", delt);
	}
	
	public void Add(String msg){
		if(enable){			
			//count++;
			//Log.d(TAG,"Add msg:   [" + count + "]" + msg);		
			String timeStamp = GetTimeStamp();
			Log.d("GOODIX", timeStamp + msg);
//			if(!logQueue.offer(timeStamp + msg + "\r\n")){
//				Log.e(TAG, "LogQueue is full!");				
//			}
		}
	}
	
	public boolean Init1(String path){		
		String LOG_FILE_NAME = path + "/link/LinkLog/LinkLog_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".txt";
		Log.d(TAG,LOG_FILE_NAME);
		File mLogFile = new File(LOG_FILE_NAME);
		File mLogDir = new File(mLogFile.getParent());
		if(!(mLogDir.mkdirs()||mLogDir.exists()))
		{
			Log.e(TAG,"can not create log directory: "+ mLogDir.getName());
			return false;
		}
		
		try {
			if(!mLogFile.createNewFile()) {
				Log.e(TAG,"can not create file: " + LOG_FILE_NAME);
				return false;
			}
			
			Log.d(TAG, "create file: " + LOG_FILE_NAME);
			
			mWriter = new BufferedWriter(new FileWriter(mLogFile));		
			
		} catch (IOException e) {
			Log.e(TAG, e.toString());
			return false;
		}		
		
		logQueue.clear();
		mExitThread = false;
		mLogThread = new LogThread();
		mLogThread.start();
		enable = true;
		//count = 0;
		Log.d(TAG, "Init complete.");

		return true;
	}
	
	public boolean Init()
	{
		mExitThread = false;
		enable = true;
		mlogcatThread = new logcatThread();
		mlogcatThread.start();
		return true;
	}
	
	public void Stop(){
		enable = false;
		mExitThread = true;
		if(logQueue!=null){
			logQueue.offer("Stop log.");
		}

		if(mLogThread!=null){
			mLogThread = null;
		}
		
		if(mlogcatThread != null)
		{
			mlogcatThread = null;
		}
	}
	
	private class LogThread extends Thread{
		
		@Override
		public void run(){
			
			Log.d(TAG, "Thread starts.");
			
			//long count = 0;
			try {
				while(!mExitThread){
				
					String msg = logQueue.take();
					mWriter.write(msg);
					mWriter.flush();
					//count++;
					//Log.d(TAG,"Write msg: [" + count + "]" + msg);							
				}				
				mWriter.close();
				
				Log.d(TAG, "Thread exits.");
				
			} catch (Exception e) {
				Log.e(TAG, e.toString());
			}
		}
	}
	
	private class logcatThread extends Thread
	{
		Process logcatProc;
		BufferedReader mReader = null;
		String cmds = null;
		FileOutputStream out = null; 
		String filepath;
		public void run() {
		     try {
		    	 filepath = FileOperate.createFile("log/LinkLog_"+ new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".txt");
		    	 cmds = "logcat GOODIX:v *:s";    // | grep \"(" + mPID +")\"
		    	 logcatProc = Runtime.getRuntime().exec(cmds);
		         mReader = new BufferedReader(new InputStreamReader( logcatProc.getInputStream()), 1024);
		         out = new FileOutputStream(new File(filepath));
		         String line = null;
		         while (!mExitThread && (line = mReader.readLine()) != null) {
		        	 if (mExitThread) 
		        	 { 
		        		 break; 
		        	 }
		        	 if (line.length() == 0) 
		        	 {
		        		 continue;
		        	 }

		        	 if (out != null && line.contains("GOODIX")) 
		        	 {
		        		 out.write((line + "\n").getBytes());
		        	 }
		        } 

		     } catch (IOException e) {
		        e.printStackTrace();
		     } finally {
		        if (logcatProc != null) {
		          logcatProc.destroy();
		          logcatProc = null;
		        }
		        if (mReader != null) {
		          try {
		            mReader.close();
		             mReader = null;
		          } catch (IOException e) {
		             e.printStackTrace();
		          }
		        }
		        if (out != null) {
		          try {
		             out.close();
		          } catch (IOException e) {
		             e.printStackTrace();

		          }
		          out = null;
		        } 
		     }  
		}
	}
}
