package com.floatwindow;

import java.lang.reflect.Field;
import java.util.Timer;
import java.util.TimerTask;

import com.jni.GoodixJNI;
import com.magic.R;
import com.tools.FileOperate;
import com.tools.GoodixLink;
import com.tools.GoodixLink.ServerOrClient;

import android.content.Context;
import android.content.Intent;
import android.os.FileObserver;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FloatWindowSmallView extends LinearLayout {

	/**
	 * ��¼С�������Ŀ��
	 */
	public static int viewWidth;

	/**
	 * ��¼С�������ĸ߶�
	 */
	public static int viewHeight;

	/**
	 * ��¼ϵͳ״̬���ĸ߶�
	 */
	 private static int statusBarHeight;

	/**
	 * ���ڸ���С��������λ��
	 */
	private WindowManager windowManager;

	/**
	 * С�������Ĳ���
	 */
	private WindowManager.LayoutParams mParams;

	/**
	 * ��¼��ǰ��ָλ������Ļ�ϵĺ�����ֵ
	 */
	private float xInScreen;

	/**
	 * ��¼��ǰ��ָλ������Ļ�ϵ�������ֵ
	 */
	private float yInScreen;

	/**
	 * ��¼��ָ����ʱ����Ļ�ϵĺ������ֵ
	 */
	private float xDownInScreen;

	/**
	 * ��¼��ָ����ʱ����Ļ�ϵ��������ֵ
	 */
	private float yDownInScreen;

	/**
	 * ��¼��ָ����ʱ��С��������View�ϵĺ������ֵ
	 */
	private float xInView;

	/**
	 * ��¼��ָ����ʱ��С��������View�ϵ��������ֵ
	 */
	private float yInView;
	
	private long lastTime = 0;
	
    final static int FW_CLICK_THRESHOLD = 30;

    private boolean mEnableMove = false;

	public FloatWindowSmallView(Context context) {
		super(context);
		windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		LayoutInflater.from(context).inflate(R.layout.float_window_small, this);
		View view = findViewById(R.id.small_window_layout);
		viewWidth = view.getLayoutParams().width;
		viewHeight = view.getLayoutParams().height;
		TextView percentView = (TextView) findViewById(R.id.percent);

		//percentView.setText(LinkFloatWindowManager.getUsedPercentValue(context));
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			// ��ָ����ʱ��¼��Ҫ����,�������ֵ����Ҫ��ȥ״̬���߶�
			mEnableMove = false;
			xInView = event.getX();
			yInView = event.getY();
			xDownInScreen = event.getRawX();
			yDownInScreen = event.getRawY() - getStatusBarHeight();
			xInScreen = event.getRawX();
			yInScreen = event.getRawY() - getStatusBarHeight();
//			GoodixLink.LOGD(" xInView" + xInView +
//							" yInView" + yInView +
//							" xDownInScreen" + xDownInScreen +
//							" yDownInScreen" + yDownInScreen);
			break;
		case MotionEvent.ACTION_MOVE:
			xInScreen = event.getRawX();
			yInScreen = event.getRawY() - getStatusBarHeight();
			// ��ָ�ƶ���ʱ�����С��������λ��
			if((Math.abs(xInScreen - xDownInScreen) > FW_CLICK_THRESHOLD)&&
			  (Math.abs(yInScreen - yDownInScreen) > FW_CLICK_THRESHOLD)
			  ) {
				mEnableMove = true;
			}
			if(mEnableMove)
			{
				updateViewPosition();
			}
			break;
		case MotionEvent.ACTION_UP:
			// �����ָ�뿪��Ļʱ��xDownInScreen��xInScreen��ȣ���yDownInScreen��yInScreen��ȣ�����Ϊ�����˵����¼���
			//if ((Math.abs(xDownInScreen - xInScreen) < FW_CLICK_THRESHOLD) && 
			//	(Math.abs(yDownInScreen-yInScreen ) < FW_CLICK_THRESHOLD)) {
			if(!mEnableMove){
				// link��������в������ж�
				if(!(  (GoodixLink.serverOrClient == ServerOrClient.NONE)||
						(GoodixLink.serverOrClient == ServerOrClient.SERVER && GoodixLink.runStatus == GoodixLink.TRANSFER_START)
					 ) )
				{
					break;
				}

				if(GoodixLink.receivedFilePath != null) {
					FileOperate.doOpenFile(GoodixLink.receivedFilePath, getContext());
					lastTime = System.currentTimeMillis();
				}
				
				if((System.currentTimeMillis() - lastTime)>5000)
				{
					GoodixLink.ShowToast(getContext().getApplicationContext(), "Click again, close the window.");
					lastTime = System.currentTimeMillis();
					break;
				}
				lastTime = 0;
				if(GoodixLink.serverOrClient == ServerOrClient.SERVER&&GoodixLink.runStatus == GoodixLink.TRANSFER_START)
				{
			    	int i=0;					
					while(i++<50)
					{
						if(GoodixJNI.exitMasterMode())
							break;
					}
					GoodixLink.serverOrClient = ServerOrClient.NONE;
					GoodixLink.runStatus = GoodixLink.NONE;
					GoodixLink.sendFilePath = null;
				}
			    LinkFloatWindowManager.removeSmallWindow(getContext().getApplicationContext());
			}
			break;
		default:
			break;
		}
		return true;
	}

	/**
	 * ��С�������Ĳ������룬���ڸ���С��������λ�á�
	 * 
	 * @param params
	 *            С�������Ĳ���
	 */
	public void setParams(WindowManager.LayoutParams params) {
		mParams = params;
	}

	/**
	 * ����С����������Ļ�е�λ�á�
	 */
	private void updateViewPosition() {
		mParams.x = (int) (xInScreen - xInView);
		mParams.y = (int) (yInScreen - yInView);
		windowManager.updateViewLayout(this, mParams);
	}

	/**
	 * �򿪴���������ͬʱ�ر�С��������
	 */
	/*
	private void openBigWindow() {
		LinkFloatWindowManager.createBigWindow(getContext());
		LinkFloatWindowManager.removeSmallWindow(getContext());
	}
	*/

	/**
	 * ���ڻ�ȡ״̬���ĸ߶ȡ�
	 * 
	 * @return ����״̬���߶ȵ�����ֵ��
	 */
	private int getStatusBarHeight() {
		if (statusBarHeight == 0) {
			try {
				Class<?> c = Class.forName("com.android.internal.R$dimen");
				Object o = c.newInstance();
				Field field = c.getField("status_bar_height");
				int x = (Integer) field.get(o);
				statusBarHeight = getResources().getDimensionPixelSize(x);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return statusBarHeight;
	}

}
