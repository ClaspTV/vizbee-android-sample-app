package com.clasptv.demo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class DeviceListAdapter extends BaseExpandableListAdapter {
	
	public static String LOG_TAG = "DeviceListAdapter";
	
	private Context mContext;
    protected LayoutInflater mInflater;
    public SSDPDeviceList devices;
    public String mVideo;
    public String mAd;
	
	public DeviceListAdapter (Context context, SSDPDeviceList sdl, String video, String ad) {
		mContext = context;
	    mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		devices = sdl;
		mVideo = video;
		mAd = ad;
	}
	
	public int getGroupCount() {
		return devices.list.size();
	}
	
	public int getChildrenCount(int groupPosition) {
        return devices.list.get(groupPosition).mDeviceServices.size();
    }
	
	public long getGroupId(int groupPosition) {
        return groupPosition;
    }
	
	public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }
	
	public Object getGroup(int groupPosition) {
        return devices.list.get(groupPosition);
    }
	
	public Object getChild(int groupPosition, int childPosition) {
        return devices.list.get(groupPosition).mDeviceServices.get(childPosition);
    }
	
	public View getChildView(final int groupPosition, final int childPosition,
            boolean isLastChild, View convertView, ViewGroup parent) {
    	
    	View lView = convertView;
        if (lView == null) {
            lView = mInflater.inflate(R.layout.child_item, parent, false);
        }
        
        /*
        
        TextView l = (TextView) lView.findViewById(R.id.child_location_name);
        l.setText("LOCATION: " + devices.list.get(groupPosition).mDeviceServices.get(childPosition).mLocation);
        
        TextView s = (TextView) lView.findViewById(R.id.child_server_name);
        s.setText("SERVER: " + devices.list.get(groupPosition).mDeviceServices.get(childPosition).mServer);
        
        TextView st = (TextView) lView.findViewById(R.id.child_st_name);
        st.setText("ST: " + devices.list.get(groupPosition).mDeviceServices.get(childPosition).mST);
        
        TextView usn = (TextView) lView.findViewById(R.id.child_usn_name);
        usn.setText("USN: " + devices.list.get(groupPosition).mDeviceServices.get(childPosition).mUSN);
        
        TextView apps = (TextView) lView.findViewById(R.id.child_appsurl_name);
        apps.setText("APPSURL: " + devices.list.get(groupPosition).mDeviceServices.get(childPosition).mAppUrl);
        
        */
        
        return lView;
    }
	
	public View getGroupView(int groupPosition, boolean isExpanded,
            View convertView, ViewGroup parent) {
    	
    	View lView = convertView;
        if (lView == null) {
            lView = mInflater.inflate(R.layout.parent_item, parent, false);
        }
        
        ImageView di = (ImageView) lView.findViewById(R.id.device_icon);
        int id = mContext.getResources().getIdentifier(devices.list.get(groupPosition).mDeviceInfo.mDeviceIcon, "drawable", mContext.getPackageName());
        di.setImageResource(id);
        
        
        TextView mn = (TextView) lView.findViewById(R.id.parent_model_name);
        mn.setText(devices.list.get(groupPosition).mDeviceInfo.mDeviceModelName);
        
        TextView ip = (TextView) lView.findViewById(R.id.parent_ip);
        ip.setText(devices.list.get(groupPosition).mDeviceInfo.mDeviceIPAddress.getHostAddress());
        
        TextView man = (TextView) lView.findViewById(R.id.parent_manufacturer);
        man.setText(devices.list.get(groupPosition).mDeviceInfo.mDeviceManufacturer);
        
        TextView fn = (TextView) lView.findViewById(R.id.parent_friendly_name);
        fn.setText(devices.list.get(groupPosition).mDeviceInfo.mDeviceFriendlyName);
        
        ImageView ci = (ImageView) lView.findViewById(R.id.check);
        
        if(devices.list.get(groupPosition).mDefault) {
        	ci.setVisibility(View.VISIBLE);
        } else {
        	ci.setVisibility(View.INVISIBLE);
        }
        
        // TextView udn = (TextView) lView.findViewById(R.id.parent_udn);
        // udn.setText(devices.list.get(groupPosition).mDeviceInfo.mDeviceUDN);
        
        final SwipeDetector swipeDetector = new SwipeDetector();
        lView.setOnTouchListener(swipeDetector);
        
       lView.setOnClickListener(new videoLauncher(devices.list.get(groupPosition), devices.list.get(groupPosition).mDeviceServices.get(0).mAppUrl, mVideo, mAd, swipeDetector));
        
        
        
        return lView;
    }
    
    public boolean hasStableIds() {
        return true;
    }
 
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }   
    
    
    
    public class videoLauncher implements OnClickListener {
    	SSDPDevice sd;
    	String appUrl;
    	String video;
    	String ad;
    	SwipeDetector swipeDetector;
    	
    	public videoLauncher(SSDPDevice d, String url, String v, String a, SwipeDetector sd) {
    		this.sd = d;
    		this.appUrl = url;
    		this.video = v;
    		this.ad = a;
    		this.swipeDetector = sd;
    	}

    	public void onClick(View v) {
    		
    		if (swipeDetector.swipeDetected()) {
    			ImageView ci = (ImageView) v.findViewById(R.id.check);
    			
    			Activity a = (Activity) ci.getContext();
    			SharedPreferences sharedPrefs = a.getSharedPreferences(a.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = sharedPrefs.edit();
    			
    			if (sd.mDefault) {
    				ci.setVisibility(View.INVISIBLE);
    				sd.mDefault = false;
    				
    				editor.putString(a.getString(R.string.dd_enable), "false");
    				editor.commit();
    				
    			} else {
    				ci.setVisibility(View.VISIBLE);
    				sd.mDefault = true;
    				
    				editor.putString(a.getString(R.string.dd_enable), "true");
    				editor.putString(a.getString(R.string.dd_app_url), appUrl);
    				editor.commit();
    				
    				Log.i(LOG_TAG, "APPURL = " + appUrl);
    			}
    			
    		} else {
    			
    			DIALControl.launchVideoAndControl(mContext, appUrl, video, ad);
    		}
    		
    	}
    	
    }
}

















