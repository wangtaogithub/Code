package com.magic;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class LinkShowText extends Activity {

	private TextView mInfo;
	private String mFilePath;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.link_show_text);
		
		mInfo = (TextView) findViewById(R.id.info);
		mInfo.setMovementMethod(ScrollingMovementMethod.getInstance());
		mInfo.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
//				LinkShowText.this.finish();
			}
			
		});
		
		Intent intent= getIntent();
		mFilePath = intent.getExtras().getString("link.filepath");

	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
//		mFile.delete();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		try {

			mInfo.setText("");
			BufferedReader in = new BufferedReader(new FileReader(mFilePath));
		    String line;
		    while((line = in.readLine())!=null) 
		    {
		     mInfo.append(line);
		    }
		    in.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}
