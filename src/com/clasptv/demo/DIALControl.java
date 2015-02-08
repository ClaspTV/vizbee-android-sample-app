package com.clasptv.demo;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DIALControl {
	
	public static String LOG_TAG = "DIALControl";
	
	public static void launchVideoAndControl (Context context, String appUrl, String video, String ad) {
		
		try {
			launchVideo(appUrl, video, ad);
			
			// hard-coding for roku
			// get host and port
			
			String host = appUrl.replace("dial/", "");
			host = appUrl.replace("dial", "");
			Log.i(LOG_TAG, "host url " + host);
			
			// send the string in the intent to controls activity
			
			Intent i = new Intent(context, ControlsActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			// i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
			i.putExtra("EXTRA_HOST_URL", host);
			i.putExtra("EXTRA_VIDEO", video);
			i.putExtra("EXTRA_APP_URL", appUrl);
			i.putExtra("EXTRA_AD", ad);
			context.startActivity(i); 
			
		} catch (Exception e) {}

	}

	public static void launchVideo (String appUrl, String v, String a) {

		final String url = appUrl;
		final String video = v;
		final String ad = a;

		new Thread(new Runnable() {
			public void run() {
				try {

					String slash = null;
					if(url.endsWith("/")) {
						slash = "";
					} else {
						slash = "/";
					}

					String app = "ClaspTV";
					String video_param = "url=" + video + "&ad=" + ad;

					DefaultHttpClient defaultHttpClient = HttpRequestHelper.createHttpClient();
					BasicHttpContext localContext = new BasicHttpContext();
					
					// stop any running versions
					Log.i(LOG_TAG, "Deleting app =" + url + slash + app + "/run");
					
					HttpDelete httpDelete = new HttpDelete(url + slash + app + "/run");
					Log.i(LOG_TAG, url + slash + app + "/run");
					HttpResponse deleteResponse = defaultHttpClient.execute(httpDelete, localContext);
					if (deleteResponse != null) {
						Log.i(LOG_TAG, "Delete response =" + deleteResponse.getStatusLine().getStatusCode());
					}
					
					// try 3 times
					// deleteResponse = defaultHttpClient.execute(httpDelete, localContext);
					// deleteResponse = defaultHttpClient.execute(httpDelete, localContext);
					// deleteResponse = defaultHttpClient.execute(httpDelete, localContext);
					
					Thread.sleep(500);

					// start the app with POST
					
					Log.i(LOG_TAG, "Starting app =" + url + slash + app);

					HttpPost httpPost = new HttpPost(url + slash + app);
					httpPost.setHeader("Content-type", "text/plain");
					httpPost.setEntity(new StringEntity(video_param)); 

					HttpResponse httpResponse = defaultHttpClient.execute(httpPost, localContext);
					if (httpResponse != null) {
						Log.i(LOG_TAG, "post response code=" + httpResponse.getStatusLine().getStatusCode());
						String response = EntityUtils.toString(httpResponse.getEntity());
						Log.i(LOG_TAG, "post response=" + response);
						Header[] headers = httpResponse.getHeaders("LOCATION");
						if (headers.length > 0) {
							String location = headers[0].getValue();
							Log.i(LOG_TAG, "post response location=" + location);
						}

						headers = httpResponse.getAllHeaders();
						for (int i = 0; i < headers.length; i++) {
							Log.i(LOG_TAG, headers[i].getName() + "=" + headers[i].getValue());
						}
					} else {
						Log.i(LOG_TAG, "no post response");
						return;
					}
				} catch (Exception e) {
					Log.e(LOG_TAG, "run", e);
				}
			}
		}).start();
	}
}
	