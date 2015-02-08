package com.clasptv.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class SSDPClient implements Runnable {
	
	private static final String LOG_TAG = "SSDPClient";
	
	// SSDP multicast response fields 
	public static final String HEADER_LOCATION = "LOCATION";
	public static final String HEADER_ST = "ST";
	public static final String HEADER_SERVER = "SERVER";
	public static final String HEADER_USN = "USN";
		
	// SSDP HTTP response fields
	public static final String HEADER_APPLICATION_URL = "Application-URL";
	
	public static final int SSDP_MULTICAST_UPDATE = 100;
	public static final int SSDP_HTTP_UPDATE = 101;
	
	private static final String SEARCH_TARGET_ALL = "ssdp:all";
	private static final String SEARCH_TARGET_UPNP = "upnp:rootdevice";
	private static final String SEARCH_TARGET_ECP = "roku:ecp";
	private static final String SEARCH_TARGET_DIAL = "urn:dial-multiscreen-org:service:dial:1";
	// private static final String SEARCH_TARGET_LG_UDAP = 
	// private static final String SEARCH_TARGET_SAMSUNG_MS =
	
	private static final String SEARCH_TARGET_INTERNET_GATEWAY_DEVICE = "urn:schemas-upnp-org:device:InternetGatewayDevice:1";

	private static final String SSDP_ADDR = "239.255.255.250";
	private static final int SSDP_PORT = 1900;

	/*
	 * Frequency of probe messages -
	 * 
	 * With exponential (doubling) delay between successive tries and
	 * a max timeout of 64000, a maximum of 5 tries will be made.
	 */
	private static final int SSDP_REQUEST_INTERVAL_MS = 4000;
	private static final int SSDP_REQUEST_TIME_MAX = 64000;

	private final InetAddress mSSDPAddress;

	private final Thread mSSDPSendThread;
	private boolean mSending = true;
	private boolean mReceiving = true;
	private int mSSDPRequestInterval;
	
	private Handler mHandler;
	private DatagramSocket mSocket;

	public SSDPClient(Handler handler) {
		
		mHandler = handler;
		
		try {
			mSSDPAddress = InetAddress.getByName(SSDP_ADDR);
		} catch (UnknownHostException e) {
			Log.e(LOG_TAG, "Could not resolve " + SSDP_ADDR, e);
			throw new RuntimeException();
		}
		mReceiving = true;

		try {
			mSocket = new DatagramSocket();
			mSocket.setBroadcast(true);
		} catch (SocketException e) {
			Log.e(LOG_TAG, "Could not create SSDP client socket.", e);
			throw new RuntimeException();
		}
		
		Log.i(LOG_TAG,"Socket port " + mSocket.getLocalPort());
		
	    mSSDPRequestInterval = SSDP_REQUEST_INTERVAL_MS;
		mSSDPSendThread = new Thread(new Runnable() {

			@Override
			public void run() {
				mSending = true;
				Log.i(LOG_TAG, "SSDP send thread starting");
				
				while (mSending) {
					try {
						//sendSSDPRequest(SEARCH_TARGET_ALL);
						//sendSSDPRequest(SEARCH_TARGET_ECP);
						sendSSDPRequest(SEARCH_TARGET_DIAL);
						
						try {
							Thread.sleep(mSSDPRequestInterval);
						} catch (InterruptedException e) {
							Log.e(LOG_TAG, "Couldn't sleep", e);
						}
						mSSDPRequestInterval = mSSDPRequestInterval * 2;
						if (mSSDPRequestInterval > SSDP_REQUEST_TIME_MAX) {
							mSending = false;
							mReceiving = false;
						}
					} catch (Throwable e) {
						Log.e(LOG_TAG, "Couldn't send SSDP request", e);
					}
				}
				Log.i(LOG_TAG, "SSDP send thread exiting");
			}
			
		});
		
	}
	
	public void run() {
		
		mSSDPSendThread.start();
		
		Log.i(LOG_TAG, "SSDP receive thread starting.");
		byte[] buffer = new byte[4096];

		while (mReceiving) {
			try {
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				mSocket.receive(packet);
				handleSSDPResponse(packet);
			} catch (InterruptedIOException e) {
				// timeout
			} catch (IOException e) {
				// SocketException - stop() was called
				break;
			} catch (IllegalArgumentException e) {
				break;
			}
		}
		Log.i(LOG_TAG, "SSDP receive thred exiting.");
		mSending = false;
		mSSDPSendThread.interrupt();
	}
	
	public void stop() {
		if (mSocket != null) {
			mSocket.close();
		}
		mReceiving = false;
	}
	
	private void sendSSDPRequest(String searchTargetType) {
		try {
			String ssdpMessage = "M-SEARCH * HTTP/1.1\r\n" + 
								"HOST: 239.255.255.250:1900\r\n" + 
								"MAN: \"ssdp:discover\"\r\n" + 
								"MX: 1\r\n" + 
								"ST: " + searchTargetType + "\r\n\r\n";
			
			byte[] buf = ssdpMessage.getBytes();
			DatagramPacket packet = new DatagramPacket(buf, buf.length, mSSDPAddress, SSDP_PORT);
			Log.i(LOG_TAG,"Socket port " + mSocket.getLocalPort());
			mSocket.send(packet);
		} catch (Throwable e) {
			Log.e(LOG_TAG, "Could not send SSDP request", e);
			return;
		}
	}

	private void handleSSDPResponse(DatagramPacket packet) {

		try {

			String strPacket = new String(packet.getData(), 0, packet.getLength());

			Log.d(LOG_TAG, "Received SSDP multicast response =" + strPacket);
			Log.d(LOG_TAG, "Packet length =" + packet.getLength());

			String tokens[] = strPacket.trim().split("\\n");
			String location = "NONE";
			String st = "NONE";
			String server = "NONE";
			String usn = "NONE";

			for (int i = 0; i < tokens.length; i++) {
				String token = tokens[i].trim();

				if (token.toUpperCase().startsWith(HEADER_LOCATION)) {
					location = token.substring(10).trim();
					Log.i(LOG_TAG, "LOCATION = " + location);

				} else if (token.toUpperCase().startsWith(HEADER_ST)) {
					st = token.substring(4).trim();
					Log.i(LOG_TAG, "ST = " + st);

				} else if (token.toUpperCase().startsWith(HEADER_SERVER)) {
					server = token.substring(8).trim(); 
					Log.i(LOG_TAG, "SERVER = " + server);

				} else if (token.toUpperCase().startsWith(HEADER_USN)) {
					usn = token.substring(5).trim(); 
					Log.i(LOG_TAG, "USN = " + usn);

				}
			}

			Uri uri = Uri.parse(location);
			InetAddress ip = null;
			try {
				ip = InetAddress.getByName(uri.getHost());
			} catch (Exception e) {
				Log.d(LOG_TAG, "Could not get IP address from " + uri.getHost(), e);
			}

			// int ret = mSSDPDeviceList.updateFromMulticastResponse(ip, location, st, server, usn);
			
			SSDPDevice update = new SSDPDevice();
			update.createFromMulticastResponse(ip, location, st, server, usn);
		
			Message message = mHandler.obtainMessage(SSDP_MULTICAST_UPDATE, update);
			mHandler.sendMessage(message);
			
			doHTTPRequestResponse(ip, location, st, server, usn);
			
		} catch (Exception e) {
			Log.e(LOG_TAG, "handleSSDPResponse", e);
		}
	}
	
	public void doHTTPRequestResponse (InetAddress ipAddress, String location, String searchTarget, String ser, String serverUsn) {

		final String l = location; 
		final String st = searchTarget;
		final String server = ser;
		final String usn = serverUsn;
		final InetAddress ip = ipAddress;
		
		if (location != null) {
			new Thread(new Runnable() {
				public void run() {
					String friendlyName = null;
					String manufacturer = null;
					String modelName = null;
					String udn = null;
					
					HttpResponse response = new HttpRequestHelper().sendHttpGet(l);
					boolean shouldParse = false;
					if (response != null) {
						if (st.compareToIgnoreCase(SEARCH_TARGET_DIAL) == 0) {
							shouldParse = true;
							String appsUrl = null;
							Header header = response.getLastHeader(HEADER_APPLICATION_URL);
							if (header != null) {
								appsUrl = header.getValue();
								Log.d(LOG_TAG, "appsUrl = " + appsUrl);
							}
							
							SSDPDevice update = new SSDPDevice();
							update.createFromMulticastResponse(ip, l, st, server, usn);
							update.mDeviceServices.get(0).mAppUrl = appsUrl;
						
							Message message = mHandler.obtainMessage(SSDP_MULTICAST_UPDATE, update);
							mHandler.sendMessage(message);
						}
						if (st.compareToIgnoreCase(SEARCH_TARGET_INTERNET_GATEWAY_DEVICE) == 0) {
							shouldParse = true;
						}
						if (shouldParse) {
							
							try {
								// HttpEntity entity = response.getEntity();
					            // String content = EntityUtils.toString(entity);
					            // Log.d(LOG_TAG, "HTTP Response = " + content);
					            
								InputStream inputStream = response.getEntity().getContent();
								BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
								XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
								factory.setNamespaceAware(true);
								XmlPullParser parser = factory.newPullParser();
								parser.setInput(reader);
								int eventType = parser.getEventType();
								String lastTagName = null;
								while (eventType != XmlPullParser.END_DOCUMENT) {
									switch (eventType) {
									case XmlPullParser.START_DOCUMENT:
										break;
									case XmlPullParser.START_TAG:
										String tagName = parser.getName();
										lastTagName = tagName;
										break;
									case XmlPullParser.TEXT:
										// capture first entry
										if (lastTagName != null) {
											if ("friendlyName".equals(lastTagName) && friendlyName == null) {
												friendlyName = parser.getText();
											} else if ("UDN".equals(lastTagName) && udn == null) {
												udn = parser.getText();
											} else if ("manufacturer".equals(lastTagName) && manufacturer == null) {
												manufacturer = parser.getText();
											} else if ("modelName".equals(lastTagName) && modelName == null) {
												modelName = parser.getText();
											}
										}
										break;
									case XmlPullParser.END_TAG:
										tagName = parser.getName();
										lastTagName = null;
										break;
									}
									eventType = parser.next();
								}
								inputStream.close();
							} catch (Exception e) {
								Log.e(LOG_TAG, "parse device description", e);
							}
							Log.d(LOG_TAG, "friendlyName="+friendlyName);

							// int ret = mSSDPDeviceList.updateFromHTTPResponse(ip, friendlyName, modelName, manufacturer, udn);
							
							SSDPDevice update = new SSDPDevice();
							update.createFromHTTPResponse(ip, friendlyName, modelName, manufacturer, udn);
						
							Message message = mHandler.obtainMessage(SSDP_HTTP_UPDATE, update);
							mHandler.sendMessage(message);
	
						}
					}
				}
			}).start();
		}
	}
}