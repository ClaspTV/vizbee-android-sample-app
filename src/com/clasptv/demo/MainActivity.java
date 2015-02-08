package com.clasptv.demo;

import com.clasptv.demo.R;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.widget.ExpandableListView;
import android.widget.ImageView;

public class MainActivity extends Activity {
	
	public final static String LOG_TAG = "MainActivity";
	
	protected ExpandableListView mListView;
	protected DeviceListAdapter mDeviceListAdapter;
	
	public SSDPClient mSSDPClient;
	public Thread mSSDPThread;
	public SSDPHandler mHandler;
	public SSDPDeviceList mSSDPDeviceList;
	public String mVideo = "champ";
	public String mAd = "no";
	public ImageView mImage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle extras = getIntent().getExtras();
        if (extras != null) {
             mVideo = extras.getString("video");
             mAd = extras.getString("ad");
             
             SharedPreferences sharedPrefs = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
             String dd_enable = sharedPrefs.getString(getResources().getString(R.string.dd_enable),"false");
             
             Log.i(LOG_TAG, "dd_enable =" + dd_enable);

             if (dd_enable.equalsIgnoreCase("true")) {
             	
             	String dd_app_url = sharedPrefs.getString(getResources().getString(R.string.dd_app_url), "false");
             	if(!dd_app_url.equalsIgnoreCase("false")) {
             		 Log.i(LOG_TAG, "dd_app_url =" + dd_app_url);
             		DIALControl.launchVideoAndControl(this, dd_app_url, mVideo, mAd);
             	}
             }
             
        } else {
        	// hack!
        	
        	
        }
        
		
		setContentView(R.layout.activity_main);
		mImage = (ImageView) findViewById(R.id.header_image);

		// header image name is the same as video name
		int id = this.getResources().getIdentifier(mVideo, "drawable", this.getPackageName());
		mImage.setImageResource(id);
		
		mSSDPDeviceList = new SSDPDeviceList();
		mHandler = new SSDPHandler();
		
        mListView = (ExpandableListView) findViewById(R.id.device_list);
	    mDeviceListAdapter = new DeviceListAdapter(this, mSSDPDeviceList, mVideo, mAd);
	    mListView.setAdapter(mDeviceListAdapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		startSSDPDiscovery();
	}

	@Override
	protected void onStop() {
		if (null != mSSDPClient) {
			mSSDPClient.stop();
			mSSDPClient = null;
		}
		super.onStop();
	}
	
	private void startSSDPDiscovery() {
		
		try {
			mSSDPClient = new SSDPClient(mHandler);
			mSSDPThread = new Thread(mSSDPClient);
			mSSDPThread.start();
		} catch (RuntimeException e) {
			Log.e(LOG_TAG, "startSSDPDiscovery failed", e);
		}
	}
	
	private class SSDPHandler extends Handler {
		/** {inheritDoc} */
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == SSDPClient.SSDP_MULTICAST_UPDATE) {
				SSDPDevice s = (SSDPDevice) msg.obj;
				Log.d(LOG_TAG, "Received MCAST object = " + s.mDeviceInfo.mDeviceIPAddress.getHostAddress());
				int ret = mSSDPDeviceList.updateFromMulticastResponse(s);
				if (ret == SSDPDevice.MATCH_AND_UPDATE || ret == SSDPDevice.CREATED) {
					mDeviceListAdapter.notifyDataSetChanged();
					return;
				}
			}
			if (msg.what == SSDPClient.SSDP_HTTP_UPDATE) {
				SSDPDevice s = (SSDPDevice) msg.obj;
				Log.d(LOG_TAG, "Received HTTP object " + s.mDeviceInfo.mDeviceFriendlyName);
				int ret = mSSDPDeviceList.updateFromHTTPResponse(s);
				if (ret == SSDPDevice.MATCH_AND_UPDATE || ret == SSDPDevice.CREATED) {
					mDeviceListAdapter.notifyDataSetChanged();
					return;
				}
			}
		}
	}
}
