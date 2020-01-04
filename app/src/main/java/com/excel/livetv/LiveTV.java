package com.excel.livetv;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.excel.excelclasslibrary.UtilSQLite;
import com.excel.excelclasslibrary.UtilSharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class LiveTV extends Activity {

    private String path = "udp://@239.0.0.16:10000";

    private Uri uri;
    private VideoView mVideoView;

    private static final String TAG = "DTVPlayer";
    AnimatedGifImageView loading_channel;
    LinearLayout loading;
    TextView tv_tens, tv_units;

    //String tv_ch_url = "udp://@239.0.0.16:10000";
    String tv_ch_url = "";
    Context context = this;

    Stack<Integer> ch_no = new Stack<Integer>();
    LinearLayout ll_channel_nos;
    SharedPreferences spfs, spfs_last_channel;
    String current_ch_number;
    long tv_ch_timestamp;

    TextView tv_channel_name1, tv_channel_no1;
    ImageView iv_channel_icon1;
    LinearLayout ll_tv_channel_info;

    AppPreferences app_prefs;
    long ch_info_timestamp;
    SQLiteDatabase sqldb;
    int TV_CHANNEL_INFO_DISPLAY_INTERVAL_SECONDS = 10;

    boolean is_app_starting_from_beginning = true;

    String[] permissions = {
            // Manifest.permission.RECEIVE_BOOT_COMPLETED, // // Normal Permisison
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            // Manifest.permission.WRITE_SETTINGS, // // Special Permisison -> https://developer.android.com/reference/android/Manifest.permission.html#WRITE_SETTINGS
            "android.permission.DOWNLOAD_WITHOUT_NOTIFICATION",
    };

    @Override
    public void onCreate( Bundle icicle ) {
        super.onCreate( icicle );

        Log.d( TAG, "onCreate()" );

        spfs = (SharedPreferences) UtilSharedPreferences.createSharedPreference( this, Constants.PERMISSION_SPFS );

		/*if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
			if ( checkPermissions() ) {
				// permissions  granted.
				UtilSharedPreferences.editSharedPreference( spfs, Constants.IS_PERMISSION_GRANTED, Constants.PERMISSION_GRANTED_YES );
				finish();
			}
		}
		else{
			finish();
		}*/


        try{
            Bundle b = icicle;
            int r = b.getInt( "recreate" );
            is_app_starting_from_beginning = false;
        }
        catch( Exception e ){
            Log.d( TAG, "recreate : 0 - App started, ch 1 should be shown" );
            is_app_starting_from_beginning = true;
        }
        Log.d( TAG, "is_app_starting_from_beginning : "+Boolean.toString( is_app_starting_from_beginning ) );

        setContentView( R.layout.player );

        init();

        loadDatabase();
    }

    public void init(){
        //tv_ch_url = "";

        mVideoView = (VideoView) findViewById( R.id.videoView );
        loading = (LinearLayout) findViewById( R.id.loading );
        sqldb = UtilSQLite.makeDatabase( "tv_channels.db", context );

        // loading_channel = (AnimatedGifImageView) findViewById( R.id.loading_channel );
        // loading_channel.setAnimatedGif( R.raw.loading_4, TYPE.FIT_CENTER );

        tv_tens		= (TextView) findViewById( R.id.tv_tens );
        tv_units	= (TextView) findViewById( R.id.tv_units );

        ll_channel_nos = (LinearLayout) findViewById( R.id.ll_channel_nos );

        tv_channel_name1 = (TextView) findViewById( R.id.tv_channel_name1 );
        iv_channel_icon1 = (ImageView) findViewById( R.id.iv_channel_icon1 );
        ll_tv_channel_info = (LinearLayout) findViewById( R.id.ll_tv_channel_info );
        tv_channel_no1 = (TextView) findViewById( R.id.tv_channel_no1 );

        LocalBroadcastManager.getInstance( this ).registerReceiver( channelSwitchReceiver,
                new IntentFilter( "switch_channel" ) );

        app_prefs = new AppPreferences( context );

        playVideo();
    }

    private BroadcastReceiver channelSwitchReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive( Context context, Intent intent ) {
            // Get extra data included in the Intent
            tv_ch_url = intent.getStringExtra( "url" );
            current_ch_number = intent.getStringExtra( "channel_number" );

            Log.d( TAG, "Got message : " + current_ch_number );

            recreate();
        }

    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Log.d( TAG, "onSaveInstanceState" );
        outState.putInt( "recreate", 1 );

    }

    //boolean is_first_time = true;
    private void playVideo() {
        Log.d( TAG, "playVideo()" );

        try {

            if( ( tv_ch_url == null ) || tv_ch_url.equals( "" ) ){
                // Load Channel 1
                Log.d( TAG, "getLastAccessedChannelURLFromDB()" );
                getLastAccessedChannelURLFromDB();
            }

            Log.d( TAG, "playVideo() tv_ch_url : "+tv_ch_url );

            showChannelInformation();

            uri = Uri.parse( tv_ch_url );
            mVideoView.setVideoURI( uri );
            mVideoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                @Override
                public boolean onInfo(MediaPlayer mediaPlayer, int what, int i1) {
                    Log.d( TAG, "inside OnInfoListener" );
                    return false;
                }
            });
            mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int what, int i1) {
                    Log.d( TAG, "inside OnErrorListener" );
                    return false;
                }
            });
            mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    Log.d( TAG, "inside OnPreparedListener" );
                }
            });
            mVideoView.start();





        } catch ( Exception e ) {
            Log.e( TAG, "error: " + e.getMessage(), e );
        }
    }

    @Override
    public boolean onKeyUp( int keyCode, KeyEvent event ) {
        Log.i( TAG, "Key pressed : "+event.getKeyCode() );

        int code = event.getKeyCode();

        // if( ( code == 82 ) || ( code == 66 ) ){ // 66 -> OK Button, giec box
        if( ( code == 82 ) || ( code == 66 ) || ( code == 23 ) ){ // 23 -> OK Button and TV List Button
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
        if( ( code == 92 ) || ( code == 19 ) ){	// next Channel, up
            // switchNextPreviousChannel( TvList.getCurrentChannelNumber(), true );
            switchNextPreviousChannel( app_prefs.getCurrentChannelID(), true );
        }
        if( ( code == 93 ) || ( code == 20 ) ){   // previous channel, down
            // switchNextPreviousChannel( TvList.getCurrentChannelNumber(), false );
            switchNextPreviousChannel( app_prefs.getCurrentChannelID(), false );
        }
        if( ( code == KeyEvent.KEYCODE_DPAD_LEFT ) || ( code == 21 ) ){   // Volume down
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Instrumentation inst = new Instrumentation();
                    inst.sendKeyDownUpSync( KeyEvent.KEYCODE_VOLUME_DOWN );
                }
            }).start();
            return true;
        }
        if( ( code == KeyEvent.KEYCODE_DPAD_RIGHT ) || ( code == 22 ) ){   // Volume up
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Instrumentation inst = new Instrumentation();
                    inst.sendKeyDownUpSync( KeyEvent.KEYCODE_VOLUME_UP );
                }
            }).start();
            return true;
        }

        String key_name = KeyEvent.keyCodeToString( keyCode );
        shortCutKeyMonitor( key_name );
        if ( ( keyCode != 4 ) ||
                ( keyCode != KeyEvent.KEYCODE_BACK ) ) {
            return super.onKeyDown( keyCode, event );
        }

        if( keyCode == KeyEvent.KEYCODE_BACK ){
            return true;
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

                if( diff < 1 ){
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
        }, 1000 );

    }

    public boolean switchChannelUsingNumber(){
        String number = tv_tens.getText().toString().trim() + tv_units.getText().toString().trim();
        Log.d( TAG, "switchChannelUsingNumber() : "+number );

        return changeChannel( number );
    }

    public boolean switchNextPreviousChannel( String channel_number, boolean next ){
        String sql = "SELECT id FROM channels ORDER BY id DESC";
        Cursor c = UtilSQLite.executeQuery( sqldb, sql, false );

        if( c == null ){
            Toast.makeText( context, "Channel Unavailable !", Toast.LENGTH_LONG ).show();
            return false;
        }

        if( c.getCount() == 0 )
            return false;

        c.moveToNext();
        int number, n;
        number = c.getInt( c.getColumnIndex( "id" ) );
        // Log.d( TAG, "Last id : "+number );
        n = Integer.parseInt( channel_number );

        Log.d( TAG, String.format( "total channels %s, current channel : %s", number, n ) );
        if( next ){
            n = n + 1;
            if( n > number )
                n = 1;
        }
        else{
            n = n - 1;
            if( n < 1 )
                n = number;
        }
        Log.d( TAG, String.format( "total channels %s, current channel : %s", number, n ) );

        return changeChannel( String.valueOf( n ) );
    }

    public boolean changeChannel( String channel_number ){
        String sql = "SELECT b.sequence as category_sequence, b.id as category_id, b.category_name, a.id as channel_id, a.sequence as channel_sequence, " +
                "a.channel_name, a.channel_url, a.icon as channel_icon FROM channels a, categories b  WHERE a.id = "+ channel_number +" and (b.id=a.category_id)";

        Log.d( TAG, sql );

        // String sql = "SELECT channel_url FROM channels WHERE id="+channel_number;
        Cursor 	 c = UtilSQLite.executeQuery( sqldb, sql, false );

        if( c == null ){
            Toast.makeText( context, "Channel Unavailable !", Toast.LENGTH_LONG ).show();
            return false;
        }

        if( c.getCount() == 0 )
            return false;

        c.moveToNext();
        tv_ch_url = c.getString( c.getColumnIndex( "channel_url" ) );

        Intent intent = new Intent( "switch_channel" );
        intent.putExtra( "url", tv_ch_url );
        Log.d( TAG, String.format( "channel number set to : %s", channel_number ) );

        // TvList.setCurrentChannelNumber( channel_number );

        app_prefs.setAllPreferences( c.getString( c.getColumnIndex( "channel_id" ) ), c.getString( c.getColumnIndex( "channel_name" ) ), c.getString( c.getColumnIndex( "channel_sequence" ) ),
                c.getString( c.getColumnIndex( "channel_url" ) ), c.getString( c.getColumnIndex( "channel_icon" ) ),
                c.getString( c.getColumnIndex( "category_id" ) ), c.getString( c.getColumnIndex( "category_name" ) ),
                c.getString( c.getColumnIndex( "category_sequence" ) ) );

        LocalBroadcastManager.getInstance( context ).sendBroadcast( intent );


        return true;
    }

    public void loadDatabase(){

        sqldb = UtilSQLite.makeDatabase( "tv_channels.db", context );



        app_prefs.setisFirstTimeRunning( "0" );

    }

    public void showLoadingOfChannel(){
        ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat( loading, "alpha", 0.0f, 1.0f );
        alphaAnimation.setDuration( 500 );//set duration
        alphaAnimation.start();//start animation
    }

    public void hideLoadingOfChannel(){
        ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat( loading, "alpha", 1.0f, 0.0f );
        alphaAnimation.setDuration( 1 );//set duration
        alphaAnimation.start();//start animation
    }

    public void getFirstChannelURLFromDB(){
        // sqldb = UtilSQLite.makeDatabase( "tv_channels.db", context );
        Log.d( TAG, "getFirstChannelURLFromDB()" );
        String sql = "SELECT b.sequence as category_sequence, b.id as category_id, b.category_name, a.id as channel_id, a.sequence as channel_sequence, a.channel_name, a.channel_url, a.icon as channel_icon FROM channels a, categories b  WHERE a.id = 1 and (b.id=a.category_id)";
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
        // openTVList();
        c.moveToNext();
        tv_ch_url = c.getString( c.getColumnIndex( "channel_url" ) );
        // TvList.setCurrentChannelNumber( "1" );

        // UtilSharedPreferences.editSharedPreference( spfs_last_channel, "channel_id", "1" );
        // UtilSharedPreferences.editSharedPreference( spfs_last_channel, "channel_name", c.getString( c.getColumnIndex( "channel_name" ) ) );
        // UtilSharedPreferences.editSharedPreference( spfs_last_channel, "channel_url", tv_ch_url );

        app_prefs.setAllPreferences( "1", c.getString( c.getColumnIndex( "channel_name" ) ), c.getString( c.getColumnIndex( "channel_sequence" ) ),
                c.getString( c.getColumnIndex( "channel_url" ) ), c.getString( c.getColumnIndex( "channel_icon" ) ),
                c.getString( c.getColumnIndex( "category_id" ) ), c.getString( c.getColumnIndex( "category_name" ) ),
                c.getString( c.getColumnIndex( "category_sequence" ) ) );

        // Log.d( TAG, "channel_name : "+spfs_last_channel.getString( "channel_name" , "null" ) );
    }

    public void getLastAccessedChannelURLFromDB(){
        Log.d( TAG, "getLastAccessedChannelURLFromDB()" );

        tv_ch_url = UtilSharedPreferences.getSharedPreference( app_prefs.getSharedPreferences(), "channel_url", "" ).toString();
        if( tv_ch_url.trim().equals( "" ) ){
            getFirstChannelURLFromDB();
        }
        else if( is_app_starting_from_beginning ){
            Log.d( TAG, "is_app_starting_from_beginning" );
            getFirstChannelURLFromDB();
        }

    }

    public void openTVList(){
        //Intent intent =new Intent( context, TvList.class );
        Intent intent =new Intent( context, TvListSimple.class );
        startActivity( intent );
        this.overridePendingTransition( R.anim.show_tv_list_anim, R.anim.hide_tv_list_anim );
    }

    public void createTvChannelsDatabase(){
        SQLiteDatabase sqldb = UtilSQLite.makeExternalDatabase( Environment.getExternalStorageDirectory().getAbsolutePath(), "tv_channels.db", context );
        String sql1 = "CREATE TABLE categories (sequence TEXT, id INTEGER PRIMARY KEY, category_name TEXT)";
        String sql2 = "CREATE TABLE channels (icon TEXT, category_id NUMERIC, id INTEGER PRIMARY KEY, sequence NUMERIC, channel_name TEXT, channel_url TEXT)";
        // UtilSQLite.executeQuery( sqldb, sql1, false );
        // UtilSQLite.executeQuery( sqldb, sql2, false );
        sqldb.rawQuery( sql1, null );
        sqldb.rawQuery( sql2, null );
    }

    public void showChannelInformation(){
        Log.d( TAG, "showChannelInformation()" );

        tv_channel_name1.setText( app_prefs.getCurrentChannelName() );
        int resID = context.getResources().getIdentifier( app_prefs.getCurrentChannelIcon(), "drawable",  context.getPackageName() );
        iv_channel_icon1.setBackgroundResource( resID );
        tv_channel_no1.setText( app_prefs.getCurrentChannelID() );

        ll_tv_channel_info.setVisibility( View.VISIBLE );
        tv_channel_no1.setVisibility( View.VISIBLE );

        ch_info_timestamp = System.currentTimeMillis();
        startTvChannelInfoTimer();
    }

    public void hideChannelInformation(){
        Log.d( TAG, "hideChannelInformation()" );

        ll_tv_channel_info.setVisibility( View.GONE );
        tv_channel_no1.setVisibility( View.GONE );
    }

    public void startTvChannelInfoTimer(){
        new Handler().postDelayed( new Runnable() {

            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long secs = ( now - ch_info_timestamp )/1000;
                // Log.d( TAG, secs +"" );
                if( secs <= TV_CHANNEL_INFO_DISPLAY_INTERVAL_SECONDS ){
                    startTvChannelInfoTimer();
                }
                else{
                    hideChannelInformation();
                }
            }
        }, 1000 );
    }

    @Override
    protected void onPause() {

        /*if (isApplicationSentToBackground(this)){
            // Do what you want to do on detecting Home Key being Pressed
            Log.d( TAG, "App has been sent to background, kill it" );
            UtilShell.executeShellCommandWithOp( "am force-stop com.amlogic.DTVPlayer" );
        }*/

        super.onPause();
        Log.d( TAG, "onPause()" );

    }

   /* public boolean isApplicationSentToBackground(final Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;
            if (!topActivity.getPackageName().equals(context.getPackageName())) {
                return true;
            }
        }
        return false;
    }*/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d( TAG, "onDestroy()" );

    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.i( TAG, "onResume()" );

        // Set as on tv list
        if( AppPreferences.isBackFromTVList() ){

            AppPreferences.setBackFromTVList( false );
            // recreate();
            return;
        }


        try{
			/*Intent in = getIntent();
			if( in.getStringExtra( "url" ) != null ){
				tv_ch_url = in.getStringExtra( "url" );
			}
			Log.e( TAG, "onResume() : "+tv_ch_url );*/
        }
        catch( Exception e ){
            Log.e( TAG, "No intent !" );
            //getFirstChannelURLFromDB();
        }

    }



    @Override
    public void onRequestPermissionsResult( int requestCode, String permissions[], int[] grantResults ) {
        switch ( requestCode ) {
            case 10:
            {
                if( grantResults.length > 0 && grantResults[ 0 ] == PackageManager.PERMISSION_GRANTED ){
                    // permissions granted.
                    Log.d( TAG, grantResults.length + " Permissions granted : " );
                    UtilSharedPreferences.editSharedPreference( spfs, Constants.IS_PERMISSION_GRANTED, Constants.PERMISSION_GRANTED_YES );
                } else {
                    String permission = "";
                    for ( String per : permissions ) {
                        permission += "\n" + per;
                    }
                    // permissions list of don't granted permission
                    Log.d( TAG, "Permissions not granted : "+permission );
                    UtilSharedPreferences.editSharedPreference( spfs, Constants.IS_PERMISSION_GRANTED, Constants.PERMISSION_GRANTED_NO );
                }
                return;
            }
        }
    }

    public boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for ( String p:permissions ) {
            result = ContextCompat.checkSelfPermission( this, p );
            if ( result != PackageManager.PERMISSION_GRANTED ) {
                listPermissionsNeeded.add( p );
            }
        }
        if ( !listPermissionsNeeded.isEmpty() ) {
            ActivityCompat.requestPermissions( this, listPermissionsNeeded.toArray( new String[ listPermissionsNeeded.size() ] ), 10 );
            return false;
        }
        return true;
    }


    Stack<String> key_combination = new Stack<String>();
    static String Z = "KEYCODE_Z";
    static String K = "KEYCODE_K";
    static String X = "KEYCODE_X";
    static String P = "KEYCODE_P";
    static String O = "KEYCODE_O";
    static String ONE = "KEYCODE_1";
    static String THREE = "KEYCODE_3";
    static String NINE = "KEYCODE_9";
    static String SEVEN = "KEYCODE_7";
    static String DOT = "KEYCODE_PERIOD";
    String ALPHABET = "KEYCODE_";

    public void shortCutKeyMonitor( String key_name ){
        key_combination.push( key_name );

        if( key_combination.size() == 3 ){
            String key_3 = key_combination.pop();
            String key_2 = key_combination.pop();
            String key_1 = key_combination.pop();

            // Z-K-Z
            if( key_1.equals( Z ) && key_2.equals( K ) && key_3.equals( Z ) ){
                Intent in = new Intent( context, ShortcutsActivity.class );
                in.putExtra( "who", "zkz" );
                startActivity( in );
            }
            // X-K-X
            else if( key_1.equals( X ) && key_2.equals( K ) && key_3.equals( X ) ){
                Intent in = new Intent( context, ShortcutsActivity.class );
                in.putExtra( "who", "xkx" );
                startActivity( in );
            }


            key_combination.removeAllElements();
        }
    }


}
