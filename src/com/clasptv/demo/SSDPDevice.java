package com.clasptv.demo;

import java.net.InetAddress;
import java.util.ArrayList;

import android.util.Log;

public class SSDPDevice {
	public static final String LOG_TAG = "SSDPDevice";
	
	// Update types
	public static final int NO_MATCH = 100;
	public static final int MATCH_AND_NO_UPDATE = 101;
	public static final int CREATED = 102;
	public static final int MATCH_AND_UPDATE = 103;

	public String mUpdateID;
	public SSDPDeviceInfo mDeviceInfo;
	public ArrayList<SSDPDeviceService> mDeviceServices;
	public boolean mDefault = false;

	public SSDPDevice() {
		mDeviceInfo = new SSDPDeviceInfo();
		mDeviceServices = new ArrayList<SSDPDeviceService>();
	}
	
	public void createFromMulticastResponse(InetAddress ip, String location, String st, String server, String usn) {
		
		mDeviceInfo.checkAndUpdateDeviceIP(ip);
		SSDPDeviceService service = new SSDPDeviceService(location, st, server, usn);
		mDeviceServices.add(service);
		mUpdateID = "MCAST" + Long.toString(Math.round(10000*Math.random()));
		Log.d(LOG_TAG, "Created updateID " + mUpdateID);
	}
	
	public void createFromHTTPResponse(InetAddress ip, String fn, String mn, String man, String udn) {
		mUpdateID = "HTTP" + Long.toString(Math.round(10000*Math.random()));
		Log.d(LOG_TAG, "Created updateID " + mUpdateID);
		mDeviceInfo.createWithBasicInfo(ip, fn, mn, man, udn);
	}

	public int updateFromMulticastResponse(SSDPDevice s) {
		
		Log.d(LOG_TAG, "Received updateID " + s.mUpdateID);
		
		int retDeviceInfo = NO_MATCH;
		retDeviceInfo = mDeviceInfo.checkAndUpdateDeviceIP(s.mDeviceInfo.mDeviceIPAddress);
		if (retDeviceInfo == NO_MATCH) {
			return NO_MATCH;
		}
		
		if(s.mDeviceServices.size() == 0) {
			Log.d(LOG_TAG, "Should not be 0! " + s.mDeviceInfo.mDeviceIPAddress.getHostAddress());
			return retDeviceInfo;
		}
		
		// service
		SSDPDeviceService service = s.mDeviceServices.get(0);
		
		// Check if service exists
		int retDeviceService = NO_MATCH;
		for (int i=0; i < mDeviceServices.size(); i++) {
			retDeviceService = mDeviceServices.get(i).checkAndUpdateInfo(service);
			if (NO_MATCH == retDeviceService) {
				continue;
			} 
			return retDeviceService;
		}
		
		// Add service
		mDeviceServices.add(service);
		return CREATED;
	}
	
	public int updateFromHTTPResponse(SSDPDevice s) {
		
		Log.d(LOG_TAG, "Received updateID " + s.mUpdateID);

		int retDeviceInfo = NO_MATCH;
		retDeviceInfo = mDeviceInfo.checkAndUpdateBasicInfo(s.mDeviceInfo);
		return retDeviceInfo;
	}
}
