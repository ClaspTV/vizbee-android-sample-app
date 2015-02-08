package com.clasptv.demo;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;

public class ControlsActivity extends Activity implements SeekBar.OnSeekBarChangeListener{
	
	private static final String LOG_TAG = "ControlsActivity";

	private TextView timeCodeText;
	private TextView title;
	private TextView skipMsg;
	private SeekBar seekBar;
	private ImageView playButton;
	private ImageView pauseButton;
	private ImageView image;
	private Animation animationFadeIn;
	private String mVideo = "NONE";
	private String mSecondScreenURL;
	private long adPosition;
	
	private String host;
	private String appUrl;
	private String ad;
	
	private boolean isPlayButtonVisible = false;
	private boolean isPauseButtonVisible = false;
	private boolean isSkipButtonVisible = false;
	private boolean canSkipAd = false;
	
	private boolean isPlaying = true;
	private boolean isSeeking = false;
	boolean isVisible = false;
	
    private Pubnub pubnub = new Pubnub("pub-c-1de7d026-c32b-409b-aa5a-519dca7781c0", "sub-c-23d1a802-b377-11e3-bec6-02ee2ddab7fe", "", false);
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(LOG_TAG, "OnCreate");
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	Log.i(LOG_TAG, "OnStart");
    	
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // getIntent() should always return the most recent
        setIntent(intent);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	Log.i(LOG_TAG, "OnResume");
    	
    	Bundle extras = getIntent().getExtras();
        if (extras != null) {
        	String video = extras.getString("EXTRA_VIDEO");
        	if (video.equals(mVideo)) {
        		// nothing to do - probably from a back button
        	} else {
        		// new intent - clean up 
	            host = extras.getString("EXTRA_HOST_URL");
	            mVideo = extras.getString("EXTRA_VIDEO");
	            ad = extras.getString("EXTRA_AD");
	            appUrl = extras.getString("EXTRA_APP_URL");
	            
	            newVideoSetup();
        	}
        }
    }
    
    
    @Override
    protected void onRestart() {
    	super.onRestart();
    	Log.i(LOG_TAG, "OnRestart");
    	// Toast.makeText(this, "Restarted", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	Log.i(LOG_TAG, "OnPause");
    	
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	Log.i(LOG_TAG, "OnStop");
    	// Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	Log.i(LOG_TAG, "OnDestroy");
    	// Toast.makeText(this, "Destroyed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    public void newVideoSetup() {
    	
    	setContentView(R.layout.control);
        
        isVisible = false;
        isPlayButtonVisible = false;
    	isPauseButtonVisible = false;
    	isSkipButtonVisible = false;
    	canSkipAd = false;
    	
    	isPlaying = true;
    	isSeeking = false;
        
        image = (ImageView)findViewById(R.id.image);
        image.setVisibility(View.GONE);
        
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	try {
                    Intent i = new Intent("android.intent.action.MAIN");
                    i.setComponent(ComponentName.unflattenFromString("com.android.chrome/com.android.chrome.Main"));
                    i.addCategory("android.intent.category.LAUNCHER");
                    i.setData(Uri.parse(mSecondScreenURL));
                    startActivity(i);
                }
                catch(ActivityNotFoundException e) {
                    // Chrome is probably not installed
                }
            }
         });
        
        animationFadeIn = AnimationUtils.loadAnimation(this, R.anim.fadein);
        
        timeCodeText = (TextView)findViewById(R.id.timeCodeView);
        timeCodeText.setVisibility(View.VISIBLE);
        
        title = (TextView)findViewById(R.id.title);
        
        
        skipMsg = (TextView)findViewById(R.id.skip_msg);
        skipMsg.setVisibility(View.GONE);
        
        skipMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	try {
            		if(canSkipAd) {
            			sendAdSkipMessage();
            		}
                }
                catch(ActivityNotFoundException e) {
                    // Chrome is probably not installed
                }
            }
         });
        
        seekBar = (SeekBar)findViewById(R.id.seekBar);
        seekBar.setVisibility(View.GONE);

        playButton = (ImageView)findViewById(R.id.playButton);
        playButton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
              sendPlayMessage();
           }
        });
        playButton.setVisibility(View.GONE);

        pauseButton = (ImageView)findViewById(R.id.pauseButton);
        pauseButton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
              sendPauseMessage();
           }
        });
        pauseButton.setVisibility(View.GONE);

        // Set up the seek bar state
        isSeeking = false;
        seekBar.setOnSeekBarChangeListener(this);	
        
        SharedPreferences sharedPrefs = this.getPreferences(Context.MODE_PRIVATE);
        String uuid = getResources().getString(R.string.pn_uuid);

        if (uuid == null || uuid.length() == 0) {
        	uuid = pubnub.uuid();
        	SharedPreferences.Editor editor = sharedPrefs.edit();
        	editor.putString(getString(R.string.pn_uuid), uuid);
        	editor.commit();
        }

        // set the uuid for the pubnub object

        pubnub.setUUID(uuid);
        
        try {
            pubnub.subscribe("my_channel", new Callback() {
                @Override
                public void connectCallback(String channel,
                Object message) {
                	Log.i("SUBSCRIBE", "CONNECT on channel:");
                	handleChannelMessage(message);
                	
                }
                @Override
                public void disconnectCallback(String channel,
                Object message) {
                	Log.i("SUBSCRIBE", "DISCONNECT on channel:");
                	handleChannelMessage(message);
                }

                @Override
                public void reconnectCallback(String channel,
                Object message) {
                	Log.i("SUBSCRIBE", "RECONNECT on channel:");
                	handleChannelMessage(message);
                }
                
                @Override
                public void successCallback(String channel,
                Object message) {
                	// Log.i("SUBSCRIBE", "SUCCESS on channel:");
                	handleChannelMessage(message);
                }
                
                @Override
                public void errorCallback(String channel,
                PubnubError error) {
                	Log.i("SUBSCRIBE", "Error on channel:");
                	Log.i("Error", error.toString());
                }
                    
            });
        } catch (Exception e) {
        }
    }
    
    
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
       // nothing to do
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
       isSeeking = true;  // flag that we are seeking so we don't update the seek bar from messages
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
       isSeeking = false;  // clear the seeking flag to allow messages to update the seek bar again

       int currentProgress = seekBar.getProgress();
       Log.d(LOG_TAG, "Seeking to " + currentProgress);
       sendSeekMessage(currentProgress);
    }
    
    public void sendPlayMessage() {
    	if (isPlaying) {
    		Log.i(LOG_TAG, "Clicked play while playing (ignored).");
    		return;
    	}
    	controlRokuPlayback("keypress/PLAY");
    	isPlaying = true;
    	
    	Log.i(LOG_TAG, "sendPlayMessage");
    }
    
    public void sendPauseMessage() {
    	if (!isPlaying) {
    		Log.i(LOG_TAG, "Clicked pause while paused (ignored).");
    		return;
    	}
    	controlRokuPlayback("keypress/PLAY");
    	isPlaying = false;
    	
    	Log.i(LOG_TAG, "sendPauseMessage");
    }
    
    public void sendSeekMessage(int currentProgress) {
    	Log.i(LOG_TAG, "sendSeekMessage");
    }
    
    public void sendAdSkipMessage() {
    	
    	Callback callback = new Callback() {
    		public void successCallback(String channel, Object response) {
    			System.out.println(response.toString());
    		}
    		public void errorCallback(String channel, PubnubError error) {
    			System.out.println(error.toString());
    		}
    	};
    	
    	JSONObject jso = new JSONObject();
    	
    	try {	
    		jso.put("type", "ad");
    		jso.put("video", mVideo);
    		jso.put("curr", adPosition);
    	} catch (Exception e) {
    		
    	}
    	
    	// not used currently
    	pubnub.publish("my_channel_sender", jso , callback);
    	
    	String request = "http://clasptvfnf.appspot.com/pubnub_sender/?type=video&video=" + mVideo + "&curr=" + adPosition; 
    	sendAdSkipToClasp(request);
  
    }
    
    public void sendAdSkipToClasp (String command) {

    	final String cmd = command;
		new Thread(new Runnable() {
			public void run() {
				try {
					
					HttpResponse response = new HttpRequestHelper().sendHttpGet(cmd);
			    	Log.i(LOG_TAG, "sent adskip Message");
				} catch (Exception e) {
					Log.e(LOG_TAG, "Send command error ", e);
				}
			}
		}).start();
    }
    
    private void updateTitle() {
        this.runOnUiThread(new Runnable() {
           @Override
           public void run() {
        	  String txt = "";
        	  
        	  if(mVideo.equalsIgnoreCase("subaru")) {
        		txt = "Advertisement: Country Life Butter";  
        	  }
        	  if(mVideo.equalsIgnoreCase("messi")) {
          		txt = "Lionel Messi's 21 Goals";  
          	  }
        	  if(mVideo.equalsIgnoreCase("champ")) {
            		txt = "Grand Champagne Cocktail";  
              }
        	  if(mVideo.equalsIgnoreCase("meta")) {
          		txt = "Metastasis Capitulo 35";  
            }
        	  
              title.setText(txt);
              
           }
        });
     }
    
    private void updateTimeCode(final String timeCode) {
        this.runOnUiThread(new Runnable() {
           @Override
           public void run() {
              timeCodeText.setText(timeCode);
              timeCodeText.setVisibility(View.VISIBLE);
           }
        });
     }

     private void updateSeekBar(final long currentSeconds, final long duration) {
        if (isSeeking) return; // ignore updates while the use is dragging the seek bar

        this.runOnUiThread(new Runnable() {
           @Override
           public void run() {
        	   
              seekBar.setMax((int)duration);
              seekBar.setProgress((int)currentSeconds);
              seekBar.setVisibility(View.VISIBLE);
           }
        });
     }
    
    public void updatePlaybackTime(long totalSeconds, long durationSeconds) {

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        String newTime = paddedNumber(hours) + " : " + paddedNumber(minutes) + " : " + paddedNumber(seconds);
        
        updateTitle();
        updateTimeCode(newTime);
        updateSeekBar(totalSeconds, durationSeconds);
        updateControls(totalSeconds);
        updateImage(totalSeconds, durationSeconds);
     }
    
    public void updateControls(long totalSeconds) {
    	
    	// should really check for video type = ad
    	if (mVideo.equalsIgnoreCase("Subaru")){
    		if (isPlayButtonVisible) {
    			playButton.setVisibility(View.GONE);
    			isPlayButtonVisible = false;
    		}
    		if (isPauseButtonVisible) {
    			pauseButton.setVisibility(View.GONE);
    			isPauseButtonVisible = false;
    		}
    		adPosition = totalSeconds;
    		canSkipAd = true;
    		String skipTxt = "Skip Ad";
    		skipMsg.setTextColor(0xff00ddff);
    		if (totalSeconds < 10) {
    			canSkipAd = false;
    			long skip = 10 - totalSeconds;
    			skipTxt = "Skip Ad in " + String.valueOf(skip);
    			skipMsg.setTextColor(0x44000000);
    		}
    		skipMsg.setText(skipTxt);
    		
    		if(!isSkipButtonVisible) {
    			skipMsg.setVisibility(View.VISIBLE);
    			isSkipButtonVisible = true;
    		}
    	} else {
    	 // if (mVideo.equalsIgnoreCase("messi") || mVideo.equalsIgnoreCase("champ")) {
    		if (!isPlayButtonVisible) {
    			playButton.setVisibility(View.VISIBLE);
    			isPlayButtonVisible = true;
    		}
    		if (!isPauseButtonVisible) {
    			pauseButton.setVisibility(View.VISIBLE);
    			isPauseButtonVisible = true;
    		}
    		if(isSkipButtonVisible) {
    			skipMsg.setVisibility(View.GONE);
    			isSkipButtonVisible = false;
    		}
    	}
    }
    
    public void updateImage(long totalSeconds, long durationSeconds) {
    	if (mVideo.equalsIgnoreCase("Subaru")){
    		if (!isVisible) {
				// transition
				int id = this.getResources().getIdentifier("butter4", "drawable", this.getPackageName());
				image.setImageResource(id);
				image.startAnimation(animationFadeIn);
				image.setVisibility(View.VISIBLE);
				mSecondScreenURL = "http://www.enjoycountrylife.co.uk/home.php";
				isVisible = true;
			}
    		
    	}
    	
    	if (mVideo.equalsIgnoreCase("champ")){
    		// 87 for the other video
    		if (totalSeconds > 40) {
    			if (!isVisible) {
    				// transition
    				int id = this.getResources().getIdentifier("champagne", "drawable", this.getPackageName());
    				image.setImageResource(id);
    				image.startAnimation(animationFadeIn);
    				image.setVisibility(View.VISIBLE);
    				mSecondScreenURL = "http://www.sherry-lehmann.com/wines/champagne-and-sparkling-wine";
    				isVisible = true;
    			}
    		} else {
    			if (isVisible) {
    				image.setVisibility(View.GONE);
    				isVisible = false;
    			}
    		}
    	} 
    	
    	if (mVideo.equalsIgnoreCase("messi")){
    		if (totalSeconds > 40) {
    			if (!isVisible) {
    				// transition
    				int id = this.getResources().getIdentifier("addidas_messi", "drawable", this.getPackageName());
    				image.setImageResource(id);
    				image.startAnimation(animationFadeIn);
    				image.setVisibility(View.VISIBLE);
    				mSecondScreenURL = "http://www.adidas.com/us/messi-soccer/_/N-1z128npZ1z13y8w";
    				isVisible = true;
    			}
    		} else {
    			if (isVisible) {
    				image.setVisibility(View.GONE);
    				isVisible = false;
    			}
    		}
    	} 
    }
    
    private static String paddedNumber(long number) {

        if (number == 0) {
           return "00";
        }
        if (number / 10 == 0) {
           return "0" + number;
        }
        return String.valueOf(number);
     }
    
    private void handleChannelMessage(Object message) {
    	
        try {
            if (message instanceof JSONObject) {
                final JSONObject obj = (JSONObject) message;
                this.runOnUiThread(new Runnable() {
                	public void run() {
                		if (obj.has("curr") && obj.has("max")) {
                		
							try {
								mVideo = obj.getString("video");
								String curr = obj.getString("curr");
								String max = obj.getString("max");
								// Log.i(LOG_TAG, "Got channel update " + curr + " " + max );
								
								updatePlaybackTime(Long.parseLong(curr), Long.parseLong(max));
						
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
                			
                		}
                	}
                });
               // Log.i("Received json msg : ", String.valueOf(obj));

            } else if (message instanceof String) {
                final String obj = (String) message;
                this.runOnUiThread(new Runnable() {
                    public void run() {
                       //  Toast.makeText(getApplicationContext(), obj,
                                     //  Toast.LENGTH_LONG).show();
                        Log.i("Received str msg : ", obj.toString());
                    }
                });

            } else if (message instanceof JSONArray) {
                final JSONArray obj = (JSONArray) message;
                this.runOnUiThread(new Runnable() {
                    public void run() {
                      //  Toast.makeText(getApplicationContext(), obj.toString(),
                              //         Toast.LENGTH_LONG).show();
                        Log.i("Received json array msg : ", obj.toString());
                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    
    public void controlRokuPlayback (String command) {

    	final String cmd = command;
		new Thread(new Runnable() {
			public void run() {
				try {
					
					DefaultHttpClient defaultHttpClient = HttpRequestHelper.createHttpClient();
					BasicHttpContext localContext = new BasicHttpContext();

					// control app with POST

					HttpPost httpPost = new HttpPost(host + cmd);
					// httpPost.setHeader("Content-type", "application/json");

					HttpResponse httpResponse = defaultHttpClient.execute(httpPost, localContext);
					if (httpResponse != null) {
					} else {
						Log.i(LOG_TAG, "no post response");
						return;
					}
					Log.i(LOG_TAG, "Sent comamnd: " + host + cmd);
				} catch (Exception e) {
					Log.e(LOG_TAG, "Send command error ", e);
				}
			}
		}).start();
    }
}
