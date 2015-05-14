package com.magic;

import java.util.Random;

import com.floatwindow.LinkFloatWindowManager;
import com.jni.GoodixJNI;
import com.service.MagicService;
import com.tools.GoodixLink;
import com.tools.LockScreenAdmin;
import android.os.Bundle;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

public class MagicLink extends Activity {

	private Switch mOpenCloseSwitch;
	private Switch mAutoOpenSwitch;
	private Switch mSaveLogSwitch;
	private DevicePolicyManager devicePolicyManager;
	private Context mContext;
	public static String mServerState = "LinkServiceState";
	public static String mOpenFileState = "AutoOpenFile";
	public static String mSaveLogState = "SaveLog";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.magic_link);
		mContext = getApplicationContext();
		mOpenCloseSwitch = (Switch) findViewById(R.id.linkState);
		mAutoOpenSwitch = (Switch) findViewById(R.id.openFileSwitch);
		mSaveLogSwitch = (Switch) findViewById(R.id.saveLogSwitch);
		mOpenCloseSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if(isChecked)
				{
			    	SharedPreferences settings = getSharedPreferences("settings", Context.MODE_PRIVATE); 
					GoodixLink.suffixNum = settings.getInt("wifiAp_ssid_suffixNum", 0);	
					while(GoodixLink.suffixNum == 0)
					{
						Random random=new Random();
						GoodixLink.suffixNum = random.nextInt();
						
						SharedPreferences.Editor editor = settings.edit();
						editor.putInt("wifiAp_ssid_suffixNum", GoodixLink.suffixNum);
						editor.commit();
					}
					GoodixLink.LOGD("wifiAp_ssid_suffixNum:"+GoodixLink.suffixNum);
//					GoodixJNI.downLinkCode();
					
					if(GoodixJNI.CheckAuthorization());
					{
						Log.d("GOODIX", "check Authorization success!");
						//getPermission();
					}
//					else
//					{
//						//GoodixLink.ShowToast(MagicLink.this, "CheckAuthorization Failed!");
//						
//					}
				}
				else
				{
					removePermission();
					GoodixJNI.saveSetting(mContext, mServerState, false);
					Intent linkIntent = new Intent(MagicLink.this,MagicService.class);
					stopService(linkIntent);					
					LinkFloatWindowManager.removeSmallWindow(MagicLink.this.getApplicationContext());					
				}
				
				mSaveLogSwitch.setEnabled(!isChecked);
			}		
		});

		mAutoOpenSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				GoodixJNI.saveSetting(mContext, mOpenFileState, isChecked);				
			}		
		});
	
		mSaveLogSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				GoodixJNI.saveSetting(mContext, mSaveLogState, isChecked);
			}		
		});
		
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		GoodixJNI.saveSetting(mContext, mServerState, mOpenCloseSwitch.isChecked());
		GoodixJNI.saveSetting(mContext, mOpenFileState, mAutoOpenSwitch.isChecked());
		GoodixJNI.saveSetting(mContext, mSaveLogState, mSaveLogSwitch.isChecked());
	}
	


	@Override
	protected void onResume() {
		super.onResume();
		mOpenCloseSwitch.setChecked(GoodixJNI.readSetting(mContext, mServerState));
		mAutoOpenSwitch.setChecked(GoodixJNI.readSetting(mContext, mOpenFileState,true));
		boolean serverState = GoodixJNI.readSetting(mContext, mServerState);
		mSaveLogSwitch.setEnabled(!serverState);
		mSaveLogSwitch.setChecked(GoodixJNI.readSetting(mContext, mSaveLogState));
	}
	

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK)
		{
			MagicLink.this.finish();
		}
		return super.onKeyDown(keyCode, event);
	}	
	
	@Override
	protected void onActivityResult(int arg0, int arg1, Intent arg2) {
		super.onActivityResult(arg0, arg1, arg2);
		devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
	    ComponentName componentName = new ComponentName(this, LockScreenAdmin.class);
        boolean isAdminActive = devicePolicyManager.isAdminActive(componentName);
        if(isAdminActive){   
        	GoodixJNI.saveSetting(mContext, mServerState, true);
        	Intent linkIntent = new Intent(MagicLink.this,MagicService.class);
			startService(linkIntent);	
						
			mOpenCloseSwitch.setChecked(true);
        }
        else
        {
        	mOpenCloseSwitch.setChecked(false);
        }
	}

	private void getPermission()
	{
		//申请自动关屏权限
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
	    ComponentName componentName = new ComponentName(this, LockScreenAdmin.class);
        boolean isAdminActive = devicePolicyManager.isAdminActive(componentName);
        if(!isAdminActive){       	          
            Intent intent = new Intent();
            intent.setAction(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);  // 指定动作名称           
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName); // 指定给哪个组件授权
            startActivityForResult(intent,0);
        }
        else
        {				
        	GoodixJNI.saveSetting(mContext,mServerState, true);
        	Intent linkIntent = new Intent(MagicLink.this,MagicService.class);				//start link service
			startService(linkIntent);				
			mOpenCloseSwitch.setChecked(true);
        }
	}
	
	private void removePermission()
	{
		//申请自动关屏权限
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
	    ComponentName componentName = new ComponentName(this, LockScreenAdmin.class);
        boolean isAdminActive = devicePolicyManager.isAdminActive(componentName);
        if(isAdminActive){       	          
        	devicePolicyManager.removeActiveAdmin(componentName);
        }
	}
}
