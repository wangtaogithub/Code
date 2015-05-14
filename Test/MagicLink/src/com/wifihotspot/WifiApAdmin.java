package com.wifihotspot;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import com.jni.GoodixJNI;
import com.reflectUtils.ClsUtils;
//import com.magic.LinkShare;
import com.tools.GoodixLink;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

/**
 * 创建热点
 *
 */
public abstract class WifiApAdmin {
    public static final String TAG = "GOODIXWifiApAdmin";
    private String mMsg;
//    private static boolean mWifiState = true;
    private static WifiCheckTimer mTimerCheck;
    public static  void closeWifiAp(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE); 
        closeWifiAp(wifiManager);
    }
    
    private static WifiManager mWifiManager = null;
    
    private Context mContext = null;
	private static String wifiApIpAddr = null;
    public WifiApAdmin(Context context) {
        mContext = context;
        
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);  
        
//        closeWifiAp(mWifiManager);
    }
    
    private String mSSID = "";
    private String mPasswd = "";
    public void startWifiAp(String ssid, String passwd) {
        mSSID = ssid;
        mPasswd = passwd;
        
//        if (mWifiManager.isWifiEnabled()) 
        {
            mWifiManager.setWifiEnabled(false);           
        } 
        
        if(isWifiApEnabled(mWifiManager))
        {
        	//WifiConfiguration cfg = mWifiManager.getWifiApConfiguration();
        	WifiConfiguration cfg;
			try {
				cfg = (WifiConfiguration)ClsUtils.invokeMethod(mWifiManager, "getWifiApConfiguration");
	        	if(!ssid.equals(cfg.SSID))
	        	{
	        		GoodixLink.LOGD("Turn off wifihotspot"); 
	        		closeWifiAp(mWifiManager);
	        	}
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
        
        stratWifiAp();
        
        mTimerCheck = new WifiCheckTimer() {
            
            @Override
            public void doTimerCheckWork() {

                if (isWifiApEnabled(mWifiManager)) {
                	onNotifyWifiApCreated();
                	mMsg = "Create wifi hotspot successfully!";
                	GoodixLink.LOGD(mMsg);
					int retry = 0;
					while((wifiApIpAddr==null)&&(retry++ < 2000)) {
						wifiApIpAddr = getWifiApIpAddress();
						GoodixJNI.delay(10);
						}
					GoodixLink.LOGD("Get wifiap IP:" + wifiApIpAddr + ",times:" + retry);
                    this.exit();
                } else {
//                    GoodixLink.LOGD("Create Wifi hotspot failed!Please try again!"); 
                }
            }

            @Override
            public void doTimeOutWork() {
            	mMsg = "Wifi check timeout!";
                GoodixLink.LOGE(mMsg);			
                this.exit();
                onNotifyWifiApCreateFailed();
            }
        };
        mTimerCheck.start(150, 100);
        
    }

    public void stratWifiAp() {
        Method method1 = null;
        try {
            method1 = mWifiManager.getClass().getMethod("setWifiApEnabled",
                    WifiConfiguration.class, boolean.class);
            WifiConfiguration netConfig = new WifiConfiguration();

            netConfig.SSID = mSSID;
            netConfig.preSharedKey = mPasswd;

            netConfig.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.OPEN);
            netConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            netConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            netConfig.allowedKeyManagement
                    .set(WifiConfiguration.KeyMgmt.WPA_PSK);
            netConfig.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.CCMP);
            netConfig.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.TKIP);
            netConfig.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.CCMP);
            netConfig.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.TKIP);

            method1.invoke(mWifiManager, netConfig, true);

        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static  void closeWifiAp(WifiManager wifiManager) {
    	if(mTimerCheck!=null)
    	{
    		mTimerCheck.exit();
    	}
        if (isWifiApEnabled(wifiManager)) {
            try {
                Method method = wifiManager.getClass().getMethod("getWifiApConfiguration");
                method.setAccessible(true);

                WifiConfiguration config = (WifiConfiguration) method.invoke(wifiManager);

                Method method2 = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                method2.invoke(wifiManager, config, false);
            } catch (NoSuchMethodException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static boolean isWifiApEnabled(WifiManager wifiManager) {
        try {
			//android的热点功能不可见，用了反射的技术搞定
            Method method = wifiManager.getClass().getMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifiManager);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

	public static String getHostIpAddr() {
		return wifiApIpAddr;
	}
	
	private String getWifiApIpAddress() {
    try {
        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
                .hasMoreElements();) {
            NetworkInterface intf = en.nextElement();
			//GoodixLink.LOGD("NetworkIf:" + intf.getName());
            if (intf.getName().contains("wlan") || intf.getName().contains("ap")) {
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
                        .hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()
                            && (inetAddress.getAddress().length == 4)) {
                        GoodixLink.LOGD(inetAddress.getHostAddress());
                        return inetAddress.getHostAddress();
                    }
                }
            }
        }
    } catch (SocketException ex) {
        GoodixLink.LOGE(ex.toString());
    }
    return null;
}

    public abstract void onNotifyWifiApCreated();
    
    public abstract void onNotifyWifiApCreateFailed();
}

