package com.wifihotspot;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.log.LinkLog;
import com.reflectUtils.ClsUtils;
import com.tools.GoodixLink;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.util.Log;


public abstract class WifiAdmin {
    
    private static final String TAG = "GOODIXWifiAdmin";
    
    private WifiManager mWifiManager;
    private WifiInfo mWifiInfo;
    // ɨ��������������б�
    private List<ScanResult> mWifiList;
    private List<WifiConfiguration> mWifiConfiguration;

    private WifiLock mWifiLock;
    
    private Context mContext = null;

    public WifiAdmin(Context context) {
        
        mContext = context;       
        // ȡ��WifiManager����
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        // ȡ��WifiInfo����
        mWifiInfo = mWifiManager.getConnectionInfo();
        
        //GoodixLink.LOGD("WifiAdmin consructor getIpAddress = " + mWifiInfo.getIpAddress());
        
    }

    // ��WIFI
    public void openWifi() {
        if (!mWifiManager.isWifiEnabled()) {
        	WifiApAdmin.closeWifiAp(mContext);
        	mWifiManager.setWifiEnabled(true);
        }
    }

    // �ر�WIFI
    public void closeWifi() {
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        }
    }

    public abstract Intent myRegisterReceiver(BroadcastReceiver receiver, IntentFilter filter);
    
    public abstract void myUnregisterReceiver(BroadcastReceiver receiver);
    
    public abstract void onNotifyWifiConnected();
    
    public abstract void onNotifyWifiConnectFailed();
    
    // ���һ�����粢����
    public void addNetwork(WifiConfiguration wcg) {
        
        //register();

		//����wifi������wifi�ȵ�ͬʱ����
    	setStaticIP(wcg);           //20130807
        WifiApAdmin.closeWifiAp(mContext);
        int wcgID = mWifiManager.addNetwork(wcg);
        mWifiManager.enableNetwork(wcgID, true);
    }
    
    public static final int TYPE_NO_PASSWD = 0x11;
    public static final int TYPE_WEP = 0x12;
    public static final int TYPE_WPA = 0x13;
    
    public void addNetwork(String ssid, String passwd, int type) {
        if (ssid == null || passwd == null || ssid.equals("")) {
            GoodixLink.LOGE("WifiAdmin addNetwork() ## nullpointer error!");
            return;
        }
        
        if (type != TYPE_NO_PASSWD && type != TYPE_WEP && type != TYPE_WPA) {
            GoodixLink.LOGE("WifiAdmin addNetwork() ## unknown type = " + type);
        }

//		WifiConfiguration tempConfig = this.IsExsits(ssid);  
//          
//        if(tempConfig != null)  
//        {  
//            mWifiManager.removeNetwork(tempConfig.networkId);  
//        } 
        List<WifiConfiguration> cfgs = mWifiManager.getConfiguredNetworks();
        if(cfgs.size() > 0)
        {
        	for(WifiConfiguration cf:cfgs)
        	{
        		mWifiManager.disableNetwork(cf.networkId);
        		mWifiManager.removeNetwork(cf.networkId);
        	}
        	GoodixLink.LOGD("remove all ConfiguredNetworks"+cfgs.size());
        }
        stopTimer();
        //unRegister();
        
        addNetwork(createWifiInfo(ssid, passwd, type));
    }
    
    /********************20130807************************/
    public static void setIpAssignment(String assign , WifiConfiguration wifiConf)
    throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException
    {
        setEnumField(wifiConf, assign, "ipAssignment");     
    }
 
    public static void setIpAddress(InetAddress addr, int prefixLength, WifiConfiguration wifiConf)
    throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException,
    NoSuchMethodException, ClassNotFoundException, InstantiationException, InvocationTargetException
    {
        Object linkProperties = getField(wifiConf, "linkProperties");
        if(linkProperties == null)return;
        Class laClass = Class.forName("android.net.LinkAddress");
        Constructor laConstructor = laClass.getConstructor(new Class[]{InetAddress.class, int.class});
        Object linkAddress = laConstructor.newInstance(addr, prefixLength);
 
        ArrayList mLinkAddresses = (ArrayList)getDeclaredField(linkProperties, "mLinkAddresses");
        mLinkAddresses.clear();
        mLinkAddresses.add(linkAddress);        
    }
 
    public static void setGateway(InetAddress gateway, WifiConfiguration wifiConf)
    throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException, 
    ClassNotFoundException, NoSuchMethodException, InstantiationException, InvocationTargetException
    {
        Object linkProperties = getField(wifiConf, "linkProperties");
        if(linkProperties == null)return;
        Class routeInfoClass = Class.forName("android.net.RouteInfo");
        Constructor routeInfoConstructor = routeInfoClass.getConstructor(new Class[]{InetAddress.class});
        Object routeInfo = routeInfoConstructor.newInstance(gateway);
 
        ArrayList mRoutes = (ArrayList)getDeclaredField(linkProperties, "mRoutes");
        mRoutes.clear();
        mRoutes.add(routeInfo);
    }
 
    public static void setDNS(InetAddress dns, WifiConfiguration wifiConf)
    throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException
    {
        Object linkProperties = getField(wifiConf, "linkProperties");
        if(linkProperties == null)return;
 
        ArrayList<InetAddress> mDnses = (ArrayList<InetAddress>)getDeclaredField(linkProperties, "mDnses");
        mDnses.clear(); //or add a new dns address , here I just want to replace DNS1
        mDnses.add(dns); 
    }
 
    public static Object getField(Object obj, String name)
    throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException
    {
        Field f = obj.getClass().getField(name);
        Object out = f.get(obj);
        return out;
    }
 
    public static Object getDeclaredField(Object obj, String name)
    throws SecurityException, NoSuchFieldException,IllegalArgumentException, IllegalAccessException 
    {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        Object out = f.get(obj);
        return out;
    }  
 
    public static void setEnumField(Object obj, String value, String name)
    throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException
    {
        Field f = obj.getClass().getField(name);
        f.set(obj, Enum.valueOf((Class<Enum>) f.getType(), value));
    }
 
    public void setStaticIP(WifiConfiguration wifiConf)
    {
     try{
            setIpAssignment("STATIC", wifiConf); //or "DHCP" for dynamic setting
            setIpAddress(InetAddress.getByName("192.168.43.24"), 24, wifiConf);
            setGateway(InetAddress.getByName("192.168.43.1"), wifiConf);
            setDNS(InetAddress.getByName("192.168.43.1"), wifiConf);
            mWifiManager.updateNetwork(wifiConf); //apply the setting
        }catch(Exception e){
            GoodixLink.LOGE("setStaticIP - "+e.getMessage());
        }
    }
    /********************20130807************************/

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.RSSI_CHANGED_ACTION)) {
                //GoodixLink.LOGD("WifiAdmin RSSI changed broadcasr received.");
                
                //�п��������ڻ�ȡ�������Ѿ���ȡ��
                GoodixLink.LOGD("WifiAdmin BR intent is " + WifiManager.RSSI_CHANGED_ACTION);
                
                int retry = 0;
                while(retry++ < 3)
                {
	                if (isWifiContected(mContext) == WIFI_CONNECTED) {
						GoodixLink.LOGD("WifiAdmin connect success broadcast received.");
	                    stopTimer();
	                    onNotifyWifiConnected();
	                    //unRegister();
	                    break;
	                } else if (isWifiContected(mContext) == WIFI_CONNECT_FAILED) {
	                	GoodixLink.LOGD("WifiAdmin connect failed broadcast received.");
	                    stopTimer();
	                    //closeWifi();
	                    onNotifyWifiConnectFailed();
	                    //unRegister();
	                    break;
	                } else if (isWifiContected(mContext) == WIFI_CONNECTING) {
	                    GoodixLink.LOGD("WifiAdmin connecting broadcast received.");
	                    break;
	                }
					else
					{
						GoodixLink.LOGE("WifiAdmin received unkown broadcast!");
					}
                }
            }
        }
    };
    
    private final int STATE_REGISTRING = 0x01;
    private final int STATE_REGISTERED = 0x02;
    private final int STATE_UNREGISTERING = 0x03;
    private final int STATE_UNREGISTERED = 0x04;
    
    private int mHaveRegister = STATE_UNREGISTERED;
    private synchronized void register() {
        Log.v(TAG, "register() ##mHaveRegister = " + mHaveRegister);

        if (mHaveRegister == STATE_REGISTRING 
                || mHaveRegister == STATE_REGISTERED) {
            return ;
        }
        
        mHaveRegister = STATE_REGISTRING;
        myRegisterReceiver(mBroadcastReceiver, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));
        mHaveRegister = STATE_REGISTERED;
        
        //startTimer();
    }
    
    private synchronized void unRegister() {
        GoodixLink.LOGD("WifiAdmin unRegister() ##mHaveRegister = " + mHaveRegister);
        
        if (mHaveRegister == STATE_UNREGISTERED 
                || mHaveRegister == STATE_UNREGISTERING) {
            return ;
        }
        
        mHaveRegister = STATE_UNREGISTERING;
        myUnregisterReceiver(mBroadcastReceiver);
        mHaveRegister = STATE_UNREGISTERED;
    }
    
    private Timer mTimer = null;
    private void startTimer() {
        if (mTimer != null) {
            stopTimer();
        }
        
        mTimer = new Timer(true);
        
        mTimer.schedule(mTimerTask, 60 * 1000);
    }
    
    private TimerTask mTimerTask  = new TimerTask() {
        
        @Override
        public void run() {
            GoodixLink.LOGE("WifiAdmin connect timer out!");
            onNotifyWifiConnectFailed();
            //unRegister();
        }
    };
    
    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }
    
    @Override
    protected void finalize() {
        try {
            super.finalize();
            //unRegister();
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public WifiConfiguration createWifiInfo(String SSID, String password, int type) {
        
        GoodixLink.LOGD("WifiAdmin createWifiInfo SSID = " + SSID + "## Password = " + password + "## Type = " + type);
        
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";
        //config.SSID = SSID;

        WifiConfiguration tempConfig = this.IsExsits(SSID);
        if (tempConfig != null) {
            mWifiManager.removeNetwork(tempConfig.networkId);
        }
        
        // ��Ϊ���������1û������2��wep����3��wpa����
        if (type == TYPE_NO_PASSWD) {// WIFICIPHER_NOPASS
            config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
            
        } else if (type == TYPE_WEP) {  //  WIFICIPHER_WEP 
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + password + "\"";
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (type == TYPE_WPA) {   // WIFICIPHER_WPA
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            // config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        } 
        
        return config;
    }
    
    public static final int WIFI_CONNECTED = 0x01;
    public static final int WIFI_CONNECT_FAILED = 0x02;
    public static final int WIFI_CONNECTING = 0x03;
    /**
     * �ж�wifi�Ƿ����ӳɹ�,����network
     * 
     * @param context
     * @return
     */
    public int isWifiContected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetworkInfo = connectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        
        //GoodixLink.LOGD("isConnectedOrConnecting = " + wifiNetworkInfo.isConnectedOrConnecting());
        //GoodixLink.LOGD("wifiNetworkInfo.getDetailedState() = " + wifiNetworkInfo.getDetailedState());
        if (wifiNetworkInfo.getDetailedState() == DetailedState.OBTAINING_IPADDR
                || wifiNetworkInfo.getDetailedState() == DetailedState.CONNECTING) {
            return WIFI_CONNECTING;
        } else if (wifiNetworkInfo.getDetailedState() == DetailedState.CONNECTED) {
        	//GoodixLink.LOGD("wifiNetworkInfo.getDetailedState() = " + wifiNetworkInfo.getDetailedState());
            return WIFI_CONNECTED;
        } else {
            //GoodixLink.LOGD("getDetailedState() == " + wifiNetworkInfo.getDetailedState());
            return WIFI_CONNECT_FAILED;
        }
    }
    
    private WifiConfiguration IsExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"") /*&& existingConfig.preSharedKey.equals("\"" + password + "\"")*/) {
                return existingConfig;
            }
        }
        return null;
    }
    


    // �Ͽ�ָ��ID������
    public void disconnectWifi(int netId) {
        mWifiManager.disableNetwork(netId);
        mWifiManager.disconnect();
        mWifiManager.removeNetwork(netId);        
    }
    
    // ��鵱ǰWIFI״̬
    public int checkState() {
        return mWifiManager.getWifiState();
    }

    // ����WifiLock
    public void acquireWifiLock() {
        mWifiLock.acquire();
    }

    // ����WifiLock
    public void releaseWifiLock() {
        // �ж�ʱ������
        if (mWifiLock.isHeld()) {
            mWifiLock.acquire();
        }
    }

    // ����һ��WifiLock
    public void creatWifiLock() {
        mWifiLock = mWifiManager.createWifiLock("GoodixLinkWifiLock");
    }

    // �õ����úõ�����
    public List<WifiConfiguration> getConfiguration() {
        return mWifiConfiguration;
    }

    // ָ�����úõ������������
    public void connectConfiguration(int index) {
        // �����������úõ�������������
        if (index > mWifiConfiguration.size()) {
            return;
        }
        // �������úõ�ָ��ID������
        mWifiManager.enableNetwork(mWifiConfiguration.get(index).networkId,
                true);
    }

    public void startScan() {
        mWifiManager.startScan();
        mWifiList = mWifiManager.getScanResults();
        mWifiConfiguration = mWifiManager.getConfiguredNetworks();
    }

    // �õ������б�
    public List<ScanResult> getWifiList() {
        return mWifiList;
    }

    // �鿴ɨ����
    public StringBuilder lookUpScan() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < mWifiList.size(); i++) {
            stringBuilder
                    .append("Index_" + new Integer(i + 1).toString() + ":");
            // ��ScanResult��Ϣת����һ���ַ�����
            // ���аѰ�����BSSID��SSID��capabilities��frequency��level
            stringBuilder.append((mWifiList.get(i)).toString());
            stringBuilder.append("/n");
        }
        return stringBuilder;
    }

    // �õ�MAC��ַ
    public String getMacAddress() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getMacAddress();
    }

    // �õ�������BSSID
    public String getBSSID() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getBSSID();
    }

    // �õ�IP��ַ
    public int getIPAddress() {
        return (mWifiInfo == null) ? 0 : mWifiInfo.getIpAddress();
    }

    // �õ����ӵ�ID
    public int getNetworkId() {
        return (mWifiInfo == null) ? 0 : mWifiInfo.getNetworkId();
    }

    // �õ�WifiInfo��������Ϣ��
    public String getWifiInfo() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.toString();
    }
}

