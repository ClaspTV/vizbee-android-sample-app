package com.clasptv.demo;

import java.net.InetAddress;

public class SSDPDeviceInfo {
	public static final String LOG_TAG = "SSDPDeviceInfo";
	
	public InetAddress mDeviceIPAddress;
	public String mDeviceFriendlyName;
	public String mDeviceManufacturer;
	public String mDeviceModelName;
	public String mDeviceModelNumber;
	public String mDeviceModelDescription;
	public String mDeviceSerialNumber;
	public String mDeviceUDN;
	public String mDeviceIcon;

	public SSDPDeviceInfo() {
		mDeviceIPAddress = null;
		mDeviceFriendlyName = null;
		mDeviceManufacturer = null;
		mDeviceModelName = null;
		mDeviceModelNumber = null;
		mDeviceModelDescription = null;
		mDeviceSerialNumber = null;
		mDeviceUDN = null;
		mDeviceIcon = "unkown_device";
	}

	public SSDPDeviceInfo(InetAddress ip) {
		mDeviceIPAddress = ip;
		mDeviceFriendlyName = null;
		mDeviceManufacturer = null;
		mDeviceModelName = null;
		mDeviceModelNumber = null;
		mDeviceModelDescription = null;
		mDeviceSerialNumber = null;
		mDeviceUDN = null;
		mDeviceIcon = "unkown_device";
	}
	
	private void updateIcon() {
		if (mDeviceModelName != null) {
			
			if(mDeviceModelName.contains("Eureka Dongle")) {
				mDeviceIcon = "chromecast_device";
				return;
			}
			if(mDeviceModelName.toLowerCase().contains("roku")) {
				mDeviceIcon = "roku_device";
				return;
			}
			if(mDeviceModelName.toLowerCase().contains("linksys")) {
				mDeviceIcon = "linksys_device";
				return;
			}
			if(mDeviceModelName.toLowerCase().contains("un46es8000")) {
				mDeviceIcon = "samsung_tv_un46es8000";
			}
			if(mDeviceModelName.toLowerCase().contains("xbox 360")) {
				mDeviceIcon = "xbox_360";
			}
			if(mDeviceModelName.toLowerCase().contains("panasonic viera")) {
				mDeviceIcon = "panasonic_viera";
			}
			if(mDeviceModelName.toLowerCase().contains("lg tv") || 
					mDeviceModelName.toLowerCase().contains("47lm8600-uc")) {
				mDeviceIcon = "lg_tv";
			}
			if(mDeviceModelName.toLowerCase().contains("wd tv live")) {
				mDeviceIcon = "wd_tv_live";
			}
			if(mDeviceModelName.toLowerCase().contains("firetv")) {
				mDeviceIcon = "fire_tv";
			}
		}
	}
	
	public SSDPDeviceInfo(InetAddress ip, String fn, String mn, String man, String udn) {
		mDeviceIPAddress = ip;
		mDeviceFriendlyName = fn;
		mDeviceManufacturer = man;
		mDeviceModelName = mn;
		mDeviceModelNumber = null;
		mDeviceModelDescription = null;
		mDeviceSerialNumber = null;
		mDeviceUDN = udn;
		updateIcon();
	}
	
	public int checkAndUpdateDeviceIP(InetAddress ip) {
		
		if (mDeviceIPAddress == null) {
			mDeviceIPAddress = ip;
			return SSDPDevice.CREATED;
		}
		
		if (mDeviceIPAddress.getHostAddress().compareToIgnoreCase(ip.getHostAddress()) != 0) {
			return SSDPDevice.NO_MATCH;
		} else {
			return SSDPDevice.MATCH_AND_NO_UPDATE;
		}
	}
	
	public int createWithBasicInfo(InetAddress ip, String fn, String mn, String man, String udn) {
		
		int update = 0;
		
		int ipRet = checkAndUpdateDeviceIP(ip);
		if (SSDPDevice.NO_MATCH == ipRet) {
			return ipRet;
		}
		
		// Assumption - rest of the information is not conflicting
		
		if (null == mDeviceFriendlyName) {
			mDeviceFriendlyName = fn;
			update++;
		} 
		if (null == mDeviceModelName) {
			mDeviceModelName = mn;
			update++;
		}
		if (null == mDeviceUDN) {
			mDeviceUDN = udn;
			update++;
		}
		if (null == mDeviceManufacturer) {
			mDeviceManufacturer = man;
			update++;
		}
		
		updateIcon();
		
		if (update > 0) {
			return SSDPDevice.MATCH_AND_UPDATE;
		} else {
			return SSDPDevice.MATCH_AND_NO_UPDATE;
		}
	}
	
	public int checkAndUpdateBasicInfo(SSDPDeviceInfo i) {

		int update = 0;

		int ipRet = checkAndUpdateDeviceIP(i.mDeviceIPAddress);
		if (SSDPDevice.NO_MATCH == ipRet) {
			return ipRet;
		}

		// Assumption - rest of the information is not conflicting

		if (null == mDeviceFriendlyName) {
			mDeviceFriendlyName = i.mDeviceFriendlyName;
			update++;
		} 
		if (null == mDeviceModelName) {
			mDeviceModelName = i.mDeviceModelName;
			update++;
		}
		if (null == mDeviceUDN) {
			mDeviceUDN = i.mDeviceUDN;
			update++;
		}
		if (null == mDeviceManufacturer) {
			mDeviceManufacturer = i.mDeviceManufacturer;
			update++;
		}
		
		updateIcon();

		if (update > 0) {
			return SSDPDevice.MATCH_AND_UPDATE;
		} else {
			return SSDPDevice.MATCH_AND_NO_UPDATE;
		}
	}
}
