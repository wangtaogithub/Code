package com.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.jni.GoodixJNI;
import com.magic.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

public class FileOperate {
	/**寻找适合的程序打开文件*/
	public static void doOpenFile(String file,Context context){
		
		/**
		ZipUtils.zip(file, file + "2"); 
		/**/
		/***/
		if(GoodixLink.receivedFilePath.equals(file))
		{
			GoodixLink.receivedFilePath = null;
		}
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addCategory("android.intent.category.DEFAULT");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri = Uri.parse("file://" + file); 
		String type = null;
		type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
						MimeTypeMap.getFileExtensionFromUrl(file));
		if (type == null) {
			String tmp = file.toLowerCase();
			if(tmp.endsWith("txt")||tmp.endsWith("ini")||tmp.endsWith("cfg")||tmp.endsWith("bin"))
			{
				type = "text/html";
			}
			else if(tmp.endsWith("jpg")||tmp.endsWith("png")||tmp.endsWith("bmp"))
			{
				type = "image/*";
			}
			else if(tmp.endsWith("mp4")||tmp.endsWith("avi")||tmp.endsWith("3gp"))
			{
				type = "video/*";
			}
			else if(tmp.endsWith("apk"))
			{
				type = "application/vnd.android.package-archive";
			}
			else if(tmp.endsWith("doc"))
			{
				type = "application/msword";
			}
			else if(tmp.endsWith("docx"))
			{
				type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
			}else if(tmp.endsWith("pdf"))
			{
				type = "application/pdf";
			}else if(tmp.endsWith("xls"))
			{
				type = "application/vnd.ms-excel";
			}else if(tmp.endsWith("zip"))
			{
				type = "application/x-zip-compressed";
			}else if(tmp.endsWith("tar"))
			{
				type = "application/x-tar";
			}
			else if (tmp.endsWith("mp3") || tmp.endsWith("wav") || tmp.endsWith("wma"))
			{
				type = "audio/*";
			}

		}
		try
		{
			if(type != null){
				//intent.setType(type);
				intent.setDataAndType(uri, type);
				try {
					context.startActivity(intent);
				} catch (ActivityNotFoundException e){
					Toast.makeText(context, 
							R.string.can_not_open_file,
							Toast.LENGTH_SHORT).show();
				}
			} else {
				Toast.makeText(context, 
						R.string.can_not_find_a_suitable_program_to_open_this_file, 
						Toast.LENGTH_SHORT).show();
			}		
			/** */
		}catch(Exception e)
		{
			GoodixLink.LOGE("Open file fail!"+e.getMessage());		
		}		
	}
	
	/*用户选择适合的程序打开文件*/
	public static void doOpenInOtherManner(final String path,final Context context)
	{
		new AlertDialog.Builder(context)
		.setTitle("选择程序")
				.setItems((R.array.fileType),new AlertDialog.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.addCategory("android.intent.category.DEFAULT");
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						Uri uri = Uri.parse("file://" + path);
						switch (which){
						case 0:
							intent.setDataAndType(uri, "text/*");		//文本
							break;
						case 1:
							intent.setDataAndType(uri, "image/*");		//图片
							break;
						case 2:
							intent.setDataAndType(uri, "video/*");		//视频
							break;
						case 3:
							intent.setDataAndType(uri, "audio/*");		//音乐
							break;
						default:
								return;
						}
						try {
							((Activity) context).startActivityForResult(intent, 2);
						} catch (ActivityNotFoundException e){
							Toast.makeText(context, 
									R.string.can_not_open_file, 
									Toast.LENGTH_SHORT).show();
						}
					}
				}).setPositiveButton(R.string.ok, null)
				.setNegativeButton(R.string.cancel, new AlertDialog.OnClickListener() {					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).show();
	}

	public static boolean deleteFile(String path)
	{
		if(path == null)
		{
			return false;
		}
		File file = new File(path);
		if(file.exists())
		{
			if(file.delete())
			{
				GoodixLink.LOGD("Delete the error file "+path+" failed");
				return true;
			}
		}
		
		return false;
	}
	
	public static String createFile(String fileName) throws IOException
	{
		String path = GoodixJNI.getStoragePath()+"/link/";
		String saveFilePath = path+fileName;
		File file = new File(saveFilePath);
		File dirs = new File(file.getParent());
		int index = 1;
		GoodixLink.LOGD("receive saveFilePath is "+saveFilePath);
		if(!dirs.exists())
		{
			dirs.mkdirs();
		}
		while(file.exists())
		{
			if(fileName.contains(".##########"))
			{
				break;
			}
			if(fileName.contains("(")&&fileName.contains(")"))
			{
				fileName = fileName.substring(0, fileName.lastIndexOf("(")+1)
				+index+fileName.substring(fileName.lastIndexOf(")"));
			}
			else
			{
				if(fileName.contains("."))
				{
					fileName = fileName.substring(0, fileName.lastIndexOf("."))
					+"("+index+")"+fileName.substring(fileName.lastIndexOf("."));
				}
				else
				{
					fileName += "("+index+")";
				}
			}
        	file = new File(path+fileName);
        	index++;
		}
		
        file.createNewFile();
        saveFilePath = path+fileName;
        return saveFilePath;
	}

	//将assets目录下数据输出到/sdcard/目录下
	public static void deepFile(Context context, String path) 
	{         
		String rootpath = "/sdcard/"+path;
		try 
		{            
			String str[] = context.getAssets().list(path);
			if (str.length > 0) 
			{//如果是目录               
				File file = new File(rootpath); 
				file.mkdirs();               
				for (String string : str) 
				{                  
					path = path + "/" + string;                                       
					deepFile(context, path);                   
					path = path.substring(0, path.lastIndexOf('/')); 
				}            				
			} 
			else 
			{//如果是文件                
				InputStream is = context.getAssets().open(path); 
				FileOutputStream fos = new FileOutputStream(new File(rootpath)); 
				byte[] buffer = new byte[1024];       
				int count = 0;               
				while (true) {           
					count++;            
					int len = is.read(buffer);  
					if (len == -1)
					{     
						break;     
					}            
					fos.write(buffer, 0, len);
				}             
				is.close();   
				fos.close();   
			}      
		} 
		catch (IOException e) 
		{            // TODO Auto-generated catch block            
			e.printStackTrace(); 
		} 
	} 

	public static void playAudio(Context context) throws Exception
	{	
		PackageManager pm = context.getPackageManager();
		Intent in = pm.getLaunchIntentForPackage("com.android.music");
		context.startActivity(in);
		GoodixJNI.delay(200);
		Intent intent = new Intent("com.android.music.musicservicecommand");
		intent.putExtra("command", "play");
		context.sendBroadcast(intent);
//		Intent intent = new Intent();  
//		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        intent.setAction(android.content.Intent.ACTION_VIEW);
//        
//        intent.setComponent(new ComponentName("com.android.music","com.android.music.MediaPlaybackActivity"));
//        context.startActivity(intent);
	}
}
