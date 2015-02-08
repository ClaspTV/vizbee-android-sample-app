package com.clasptv.demo;

public class SSDPDeviceService {
	public static final String LOG_TAG = "SSDPDeviceServices";
	
	public String mLocation;
	public String mST;
	public String mServer;
	public String mUSN;
	public String mAppUrl;
	
	public SSDPDeviceService() {
		mLocation = null;
		mST = null;
		mServer = null;
		mUSN = null;
		mAppUrl = "NONE";
	}

	public SSDPDeviceService(String l, String st, String s, String usn) {
		mLocation = l;
		mST = st;
		mServer = s;
		mUSN = usn;
		mAppUrl = "NONE";
	}
	
	public int checkAndUpdateInfo(SSDPDeviceService s) {
		
		// Assumption - rest of the information is not conflicting
		
		if (mLocation.compareToIgnoreCase(s.mLocation) != 0)  {
			return SSDPDevice.NO_MATCH;
		} 
		
		if (mST.compareToIgnoreCase(s.mST) != 0)  {
			return SSDPDevice.NO_MATCH;
		} 
		
		if (mServer.compareToIgnoreCase(s.mServer) != 0)  {
			return SSDPDevice.NO_MATCH;
		} 
		
		if (mUSN.compareToIgnoreCase(s.mUSN) != 0)  {
			return SSDPDevice.NO_MATCH;
		} 
		
		if (s.mAppUrl != null) {
			if (s.mAppUrl.compareToIgnoreCase(mAppUrl) != 0) {
				mAppUrl = s.mAppUrl;
				return SSDPDevice.MATCH_AND_UPDATE;
			}
		}
		
		return SSDPDevice.MATCH_AND_NO_UPDATE;
	}
}
