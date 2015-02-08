package com.clasptv.demo;

import java.net.InetAddress;
import java.util.ArrayList;

public class SSDPDeviceList {
	public static final String LOG_TAG = "SSDPDeviceList";
	
	public ArrayList<SSDPDevice> list;
	
	public SSDPDeviceList() {
		list = new ArrayList<SSDPDevice>();
	}
	
	public int updateFromMulticastResponse (SSDPDevice s) {
		int ret = SSDPDevice.NO_MATCH;
		for (int i=0; i< list.size(); i++) {
			ret = list.get(i).updateFromMulticastResponse(s);
			if (SSDPDevice.NO_MATCH == ret) {
				continue;
			}
			return ret;
		}
		// add new device
		list.add(s);
		
		return SSDPDevice.CREATED;
	}
	
	public int updateFromHTTPResponse (SSDPDevice s) {
		int ret = SSDPDevice.NO_MATCH;
		for (int i=0; i< list.size(); i++) {
			ret = list.get(i).updateFromHTTPResponse(s);
			if (SSDPDevice.NO_MATCH == ret) {
				continue;
			}
			return ret;
		}
		// add new device
		list.add(s);
		
		return SSDPDevice.CREATED;
	}
}
