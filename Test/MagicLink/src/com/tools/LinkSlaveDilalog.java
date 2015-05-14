package com.tools;

import com.magic.R;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

public class LinkSlaveDilalog extends AlertDialog {
	 private View view;
	 private ImageButton btnCancel;
	 public LinkSlaveDilalog(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		LayoutInflater inflater = LayoutInflater.from(context);
		view = inflater.inflate(R.layout.link_share, null);
		btnCancel = (ImageButton)view.findViewById(R.id.btn_cancel);
	}
	 
	 @Override
	 protected void onCreate(Bundle savedInstanceState) {		 
		 //onProgressChanged();	
		 setView(view);
	     super.onCreate(savedInstanceState);
	     
			WindowManager.LayoutParams params = getWindow().getAttributes();
			params.gravity = Gravity.BOTTOM | Gravity.LEFT;
			params.height = 80;
			params.width = 200;
			getWindow().setAttributes(params);
	 }
	 
    public void setCancelButton(View.OnClickListener listener) {
    	btnCancel.setOnClickListener(listener);
    	btnCancel.setOnTouchListener(new View.OnTouchListener() {
			
    		@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction() == MotionEvent.ACTION_DOWN){
					btnCancel.setBackgroundColor(Color.DKGRAY);
					btnCancel.invalidate();
				}else {
					btnCancel.setBackgroundColor(R.color.transparent);
					btnCancel.invalidate();
				}
				return false;
			}
		});
    }
	
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		return;
		//super.onBackPressed();
	}
}
