/*
 * Copyright (C) 2013 yixia.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.excel.livetv;



import java.util.Stack;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.excel.excelclasslibrary.UtilSQLite;
import com.excel.excelclasslibrary.UtilSharedPreferences;
import com.excel.excelclasslibrary.UtilShell;
import com.excel.livetv.AnimatedGifImageView.TYPE;

public class Player extends Activity implements OnBufferingUpdateListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnVideoSizeChangedListener, SurfaceHolder.Callback {

	private static final String TAG = "Player";
	private int mVideoWidth;
	private int mVideoHeight;
	private MediaPlayer mMediaPlayer;
	private SurfaceView mPreview;
	private SurfaceHolder holder;
	private String path;
	private boolean mIsVideoSizeKnown = false;
	private boolean mIsVideoReadyToBePlayed = false;
	
	AnimatedGifImageView loading_channel;
	LinearLayout loading;
	TextView tv_tens, tv_units;
	
	String tv_ch_url = "";
	Context context = Player.this;
	
	Stack<Integer> ch_no = new Stack<Integer>();
	LinearLayout ll_channel_nos;
	SharedPreferences spfs;
	
	/**
	 * 
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		//Vitamio.isInitialized(getApplicationContext());
		
		setContentView(R.layout.player);
		
		init();
		
		
		tv_ch_url = "";
		
		mPreview = (SurfaceView) findViewById(R.id.surface);
		holder = mPreview.getHolder();
		holder.addCallback(this);
		holder.setFormat(PixelFormat.RGBA_8888); 
		
		
		LocalBroadcastManager.getInstance( this ).registerReceiver( channelSwitchReceiver,
			      new IntentFilter( "switch_channel" ) );
		
		loadDatabase();
		
		// Open TV List By Default
		// openTVList();
	}
	
	WifiManager wifi;
	MulticastLock multicastLock;
	
	private void init(){   
		loading = (LinearLayout) findViewById( R.id.loading );
		
		loading_channel = (AnimatedGifImageView) findViewById( R.id.loading_channel );
		loading_channel.setAnimatedGif( R.drawable.loading1, TYPE.FIT_CENTER );
		
		tv_tens		= (TextView) findViewById( R.id.tv_tens );
		tv_units	= (TextView) findViewById( R.id.tv_units );
		
		ll_channel_nos = (LinearLayout) findViewById( R.id.ll_channel_nos );
		
		// Get multicast Lock
		wifi = (WifiManager) getApplicationContext().getSystemService( Context.WIFI_SERVICE );
		multicastLock = wifi.createMulticastLock( "multicastLock" );
		multicastLock.setReferenceCounted( true );
		multicastLock.acquire();
		
	}
	
	private BroadcastReceiver channelSwitchReceiver = new BroadcastReceiver() {
		  @Override
		  public void onReceive(Context context, Intent intent) {
		    // Get extra data included in the Intent
		    tv_ch_url = intent.getStringExtra( "url" );
		    Log.d( TAG, "Got message: " + tv_ch_url );
		   
		    releaseMediaPlayer();
		    playVideo();
		  }
		};
	
	
	
	long tv_ch_timestamp;
	@Override
	public boolean onKeyUp( int keyCode, KeyEvent event ) {
		Log.i( TAG, "Key pressed : "+event.getKeyCode() );
		  
		int code = event.getKeyCode();  
		
		if( ( code == 82 ) || ( code == KeyEvent.KEYCODE_DPAD_CENTER ) || ( code == 23 ) ){ // 66 -> OK Button
			openTVList();
			
			// finish();
		}
		if( ( code >= 7 ) && ( code <= 16 ) ){ // These are numbers from 0 to 9
			char pressedKey = (char) event.getUnicodeChar();
			
			
			if( ch_no.size() == 2 ){ // Already 2 digits in stack and 3rd wants to go in, then empty the stack
				ch_no.clear();
				
				// hide the channel number
				// Animate to opactiy 1.0
				ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat( ll_channel_nos, "alpha", 1.0f, 0.0f );
				alphaAnimation.setDuration( 500 );//set duration
				alphaAnimation.start();//start animation 
			}
			if( ch_no.size() == 0 ){ // When the stack is empty, // Show Units-Tens TextView
				// Reset the Units and Tens
				tv_units.setText( "-" );
				tv_tens.setText( "" );
				
				// Animate to opactiy 1.0
				ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat( ll_channel_nos, "alpha", 0.0f, 1.0f );
				alphaAnimation.setDuration( 500 );//set duration
				alphaAnimation.start();//start animation 
				
				// Make Units Text View = Number Pressed
				tv_units.setText( pressedKey + "" );
				
				// Start the Timer
				tv_ch_timestamp = System.currentTimeMillis();
				startTVChannelTimer();
			}
			if( ch_no.size() == 1 ){ // When the stack has units element
				// Make Tens Text View = Units Text View value
				tv_tens.setText( tv_units.getText().toString() );
				tv_units.setText( pressedKey + "" );
				
				// Start the Timer
				tv_ch_timestamp = System.currentTimeMillis();
				// startTVChannelTimer();
			}
			
			
			// Add this digit into the Stack
			ch_no.push( Integer.parseInt( String.valueOf( pressedKey ) ) );
		}
		  
		return super.onKeyUp(keyCode, event);
	}
	
	public void startTVChannelTimer(){
		Log.d( TAG, "startTVChannelTimer()" );
		
		Handler tv_channel_timer = new Handler();
		tv_channel_timer.postDelayed( new Runnable() {
			
			@Override
			public void run() {
				Log.d( TAG, "tv_channel_timer started for 3 seconds" );
				long right_now = System.currentTimeMillis();
				long diff = (right_now - tv_ch_timestamp)/1000;
				
				if( diff < 3 ){
					startTVChannelTimer();
				}
				else{
					// Switch the Channel
					if( ! switchChannelUsingNumber() ){
						Toast.makeText( context, "No such channel !", Toast.LENGTH_LONG ).show();
					}
					ch_no.clear();
					
					// Animate to opactiy 1.0
					ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat( ll_channel_nos, "alpha", 1.0f, 0.0f );
					alphaAnimation.setDuration( 500 );//set duration
					alphaAnimation.start();//start animation 
					
				}
			}
		}, 3000 ); 
				
	}
	
	public boolean switchChannelUsingNumber(){
		String number = tv_tens.getText().toString().trim() + tv_units.getText().toString().trim();
		Log.d( TAG, "switchChannelUsingNumber() : "+number );
		
		String sql = "SELECT channel_url FROM channels WHERE id="+number;
		Cursor 	 c = UtilSQLite.executeQuery( sqldb, sql, false );
		if( c.getCount() == 0 )
			return false;
		
		c.moveToNext();
		tv_ch_url = c.getString( c.getColumnIndex( "channel_url" ) );
		
		Intent intent = new Intent( "switch_channel" );
		intent.putExtra( "url", tv_ch_url );
		LocalBroadcastManager.getInstance( Player.this ).sendBroadcast( intent );
		
		return true;
	}
	
	private void playVideo() {
		Log.d( TAG, "playVideo()" );
		//loading.setVisibility( View.VISIBLE );
		doCleanUp(); 
		
		try {
 			// path = "rtsp://192.168.1.11:8554/t";
			if( ( tv_ch_url == null ) || tv_ch_url.equals( "" ) ){
				// Load Channel 1
				//tv_ch_url = "udp://@239.2.2.2:1032";
				getFirstChannelURLFromDB();
			}
			// tv_ch_url
			//path = "http://www.listenlive.eu/bbcradio1.m3u";   
			
			

			// Create a new media player and set the listeners   
			mMediaPlayer = new MediaPlayer();
			mMediaPlayer.setDataSource( tv_ch_url );
			mMediaPlayer.setDisplay(holder); 
			// mMediaPlayer.setBufferSize( 4096 );
			//mMediaPlayer.setDeinterlace( true );
			// mMediaPlayer.setUseCache( true );
			//mMediaPlayer.setAdaptiveStream( true );
			// mMediaPlayer.
			//mMediaPlayer.
			mMediaPlayer.prepareAsync(); 
			mMediaPlayer.setOnBufferingUpdateListener(this);
			mMediaPlayer.setOnCompletionListener(this); 
			mMediaPlayer.setOnPreparedListener(this);
			mMediaPlayer.setOnVideoSizeChangedListener(this);
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
			
			mMediaPlayer.setOnInfoListener( new MediaPlayer.OnInfoListener() {
				
				@Override
				public boolean onInfo(MediaPlayer mp, int what, int extra) {

					Log.d( TAG, "what : " +what );
					mp.start();
					/*long buffersize = mp.audioTrackInit();
					mp.audioInitedOk(buffersize);*/

					/*switch (what) {
			            case MediaPlayer.MEDIA_INFO_FILE_OPEN_OK:
			            long buffersize = mp.audioTrackInit();
			            mp.audioInitedOk(buffersize);
			            break;
		            }*/
		            return false;
				}
			});

		} catch (Exception e) {
			Log.e(TAG, "error: " + e.getMessage(), e);
		}
	}

	public void onBufferingUpdate(MediaPlayer arg0, int percent) {
		// Log.d(TAG, "onBufferingUpdate percent:" + percent);
		Log.d( TAG, "onBufferingUpdate()" );
		// loading.setVisibility( View.INVISIBLE );
	}

	public void onCompletion(MediaPlayer arg0) {
		Log.d(TAG, "onCompletion called");
		// Toast.makeText( this, "onCompletion called", Toast.LENGTH_LONG ).show();
	}

	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
		Log.v(TAG, "onVideoSizeChanged called");
		if (width == 0 || height == 0) {
			Log.e(TAG, "invalid video width(" + width + ") or height(" + height + ")");
			return;
		}
		mIsVideoSizeKnown = true;
		mVideoWidth = width;
		mVideoHeight = height;
		if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
			startVideoPlayback();
		}
	}

	public void onPrepared(MediaPlayer mediaplayer) {
		Log.d(TAG, "onPrepared called");
		mIsVideoReadyToBePlayed = true;
		if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
			startVideoPlayback();
		}
		//loading.setVisibility( View.INVISIBLE );
	}

	public void surfaceChanged(SurfaceHolder surfaceholder, int i, int j, int k) {
		Log.d(TAG, "surfaceChanged called");
		//loading.setVisibility( View.INVISIBLE );
	}

	public void surfaceDestroyed(SurfaceHolder surfaceholder) {
		Log.d(TAG, "surfaceDestroyed called");
	}

	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surfaceCreated called");
		playVideo();

	}
	
	private void startVideoPlayback() {
		Log.v(TAG, "startVideoPlayback()");
		// holder.setSizeFromLayout();
		// holder.setFixedSize(mVideoWidth, mVideoHeight);
		holder.setFixedSize( 1280, 720 );
		mMediaPlayer.start();
		hideLoadingOfChannel();
		//loading.setVisibility( View.INVISIBLE );
		// loading.setVisibility( View.VISIBLE );
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d( TAG, "onPause()" );
		
		//releaseMediaPlayer();
		//doCleanUp();
	}
 
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d( TAG, "onDestroy()" );
		releaseMediaPlayer();
		doCleanUp();
		   
		// Once your finish using it, release multicast lock
		if (multicastLock != null) {
		    multicastLock.release();
		    multicastLock = null;
		} 
	}    

	private void releaseMediaPlayer() {
		Log.d( TAG, "releaseMediaPlayer()" );
		//loading.setVisibility( View.VISIBLE );
		showLoadingOfChannel();
		
		if (mMediaPlayer != null) {
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
	}

	private void doCleanUp() {
		Log.d( TAG, "doCleanUp()" );
		mVideoWidth = 0;
		mVideoHeight = 0;
		mIsVideoReadyToBePlayed = false;
		mIsVideoSizeKnown = false;
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		Log.i( TAG, "onResume()" );
		
		try{
			Intent in = getIntent();
			tv_ch_url = in.getStringExtra( "url" );
			
		}
		catch( Exception e ){
			Log.e( TAG, "No intent !" );
			getFirstChannelURLFromDB();
		}
		
	}
	
	SQLiteDatabase sqldb;
	
	public void loadDatabase(){
		/*Log.d( TAG, "Inside loadDatabase()" );
		
		InputStream fis;
		FileOutputStream fos;
		
		try{ 
			// AssetFileDescriptor assetFileDescriptor = getAssets().openFd( "tv_channels.db" );  
			// FileDescriptor fileDescriptor = assetFileDescriptor.getFileDescriptor();  
			
			fis = getResources().openRawResource( R.raw.tv_channels1 );
			
			// SQLiteDatabase sqldb = SQLiteDatabase.openDatabase( "tv_channels.db", null, SQLiteDatabase.OPEN_READWRITE );
			sqldb = openOrCreateDatabase( "tv_channels.db", Context.MODE_PRIVATE, null );	
			// sqldb.rawQuery( "Create table abc(`id`) TEXT", null );
			
			fos = new FileOutputStream( "/data/data/com.excel.livetv/databases/tv_channels.db" );
			byte b[] = new byte[ 1024 ];
			while( fis.read( b, 0, b.length ) != -1 ){
				fos.write( b );
			}
			fis.close();
			fos.close();
			
		}
		catch( Exception e ){
			e.printStackTrace();
			Log.e( TAG, e.getMessage() );
		}*/
		
		
		sqldb = UtilSQLite.makeDatabase( "tv_channels.db", context );
		spfs = UtilSharedPreferences.createSharedPreference( context, "md5" );
		
		// Check if tv channels were downloaded in this session
		/*String is_iptv_channels_synced = UtilShell.executeShellCommandWithOp( "getprop is_iptv_channels_synced" );
		if( is_iptv_channels_synced.equals( "1" ) )
			return;
		
		
		String md5 = (String) UtilSharedPreferences.getSharedPreference( spfs, "md5", "" );
		if( md5.equals( "" ) ){
			Log.d( TAG,  "Its first time, hence creating empty database !" );
			
			// if it is first time, then create the tables in the database
			createTvChannelsDatabase();
			
			// get the tv channels from the internet
			Intent in = new Intent( "download_iptv_channels" );
			sendBroadcast( in );
		}*/
		
		
		
		
		
	}
	
	public void showLoadingOfChannel(){
		ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat( loading, "alpha", 0.0f, 1.0f );
		alphaAnimation.setDuration( 500 );//set duration
		alphaAnimation.start();//start animation
	}
	
	public void hideLoadingOfChannel(){
		ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat( loading, "alpha", 1.0f, 0.0f );
		alphaAnimation.setDuration( 500 );//set duration
		alphaAnimation.start();//start animation
	}
	
	public void getFirstChannelURLFromDB(){
		// sqldb = UtilSQLite.makeDatabase( "tv_channels.db", context );
		String sql = "SELECT * FROM channels WHERE id = 1";
		Cursor 	 c = UtilSQLite.executeQuery( sqldb, sql, false );
		if( c == null ){
			Toast.makeText( context, "TV Channels have not yet synchronized !", Toast.LENGTH_LONG ).show();
			tv_ch_url = "";
			return;
		}
		if( c.getCount() == 0 ){
			Toast.makeText( context, "Channel 1 does not exist !", Toast.LENGTH_LONG ).show();
			return;
		} 
		//openTVList();
		c.moveToNext();
		tv_ch_url = c.getString( c.getColumnIndex( "channel_url" ) );
	}
	
	public void openTVList(){
		Intent intent =new Intent( Player.this, TvList.class );
		startActivity( intent );
		this.overridePendingTransition( R.anim.show_tv_list_anim, R.anim.hide_tv_list_anim );
	}
	
	public void createTvChannelsDatabase(){
		SQLiteDatabase sqldb = UtilSQLite.makeDatabase( "tv_channels.db", context );
		String sql1 = "CREATE TABLE categories (sequence TEXT, id INTEGER PRIMARY KEY, category_name TEXT)";
		String sql2 = "CREATE TABLE channels (icon TEXT, category_id NUMERIC, id INTEGER PRIMARY KEY, sequence NUMERIC, channel_name TEXT, channel_url TEXT)";
		// UtilSQLite.executeQuery( sqldb, sql1, false );
		// UtilSQLite.executeQuery( sqldb, sql2, false );
		sqldb.rawQuery( sql1, null );
		sqldb.rawQuery( sql2, null );
	}
}
