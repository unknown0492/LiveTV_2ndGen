package com.excel.livetv;

import static com.excel.livetv.Constants.SPFS_LAST_CHANNEL_HISTORY;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.excel.excelclasslibrary.UtilSQLite;
import com.excel.excelclasslibrary.UtilSharedPreferences;

public class TvList extends Activity {
	
	public static final String TAG = "TvList";
	
	ListView list_main;
	ListView list_sub;
	Context context = this;
	
	public static int focus = 0;
	public static int FOCUSSED_LEFT = 0;
	public static int FOCUSSED_RIGHT = 1;
	
	SQLiteDatabase sqldb; 
	SharedPreferences spfs;
	
	ImageView iv_left_arrow;
	TextView tv_category_name;
	

	public static final int CATEGORY_ID   = 0;
	public static final int CATEGORY_NAME = 1;
	
	public static final int CHANNEL_ID	  		= 0;
	public static final int CHANNEL_SEQUENCE  	= 1;
	public static final int CHANNEL_NAME		= 2;
	public static final int CHANNEL_URL			= 3;
	public static final int CHANNEL_ICON		= 4;
	
	
	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_tvlist );
		
		init();
		
	}

	String[][] main_list_items;
	String[] sub_list_items;
	
	String[] s1;
	String[] s2;
	String[] s3;
	
	SubListAdapter sla;
	MainListAdapter mla;
	final Handler tvlist_idle_timeout = new Handler();
	long current_timestamp;
	
	@SuppressLint("ResourceAsColor") 
	private void init(){
		list_main = (ListView) findViewById( R.id.list_main );
		list_sub  = (ListView) findViewById( R.id.list_sub ); 
		
		//main_list_items = new String[]{ "Sports", "Kids", "Action", "Genre", "Movies", "Daily Soaps", "Education", "Wildlife", "Nature", "Two", "Three", "Four", "One", "Two", "Three", "Four", "One", "Two", "Three", "Four", "One", "Two", "Three", "Four" };
		sub_list_items = new String[]{ "One", "Two", "Three", "Four", "One", "Two", "Three", "Four", "One", "Two", "Three", "Four", "One", "Two", "Three", "Four", "One", "Two", "Three", "Four", "One", "Two", "Three", "Four" };
		
		initializeSQLite();
		initializeSharedPreferences();
		
		s1 = new String[]{ "aaa", "bbb", "ccc" };
		s2 = new String[]{ "ddd", "eee", "fff" };
		s3 = new String[]{ "ggg", "hhh", "zzz" }; 
		
		mla = new MainListAdapter( context, main_list_items, R.layout.list_item );
		// sla  = new SubListAdapter( context, sub_list_items, R.layout.list_item_subitem );
		// ArrayAdapter<String> aap1 = new ArrayAdapter<String>( TvList.this, android.R.layout.simple_list_item_1, sub_list_items );
		list_main.setAdapter( mla ); 
		list_main.setBackgroundResource( R.drawable.list_background_blue );
		list_main.getBackground().setAlpha( 75 );  
		
		list_sub.setAdapter( sla );
		
		iv_left_arrow = (ImageView) findViewById( R.id.iv_left_arrow );
		tv_category_name = (TextView) findViewById( R.id.tv_category_name );
		//list_sub.setBackgroundResource( R.drawable.list_background_transparent );
		//list_sub.getBackground().setAlpha( 0 ); 
		
		setOnMainListClickListener();     
		setOnSubListClickListener();     
		 
		setOnMainListFocusChangedListener();
		setOnSubListFocusChangedListener();
		  
		  
		list_main.setOnItemSelectedListener( new OnItemSelectedListener() {

			@Override
			public void onItemSelected( AdapterView<?> parent, View view, int position, long id ) {
				Log.d( TAG, "changed" );
				//index_main_list = list_main.getSelectedItemPosition();
				showSubList();
				//list_sub.setBackgroundResource( R.drawable.list_background_transparent );
				//list_sub.getBackground().setAlpha( 0 );
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		// Current Time in Milliseconds when activity is loaded
		current_timestamp = System.currentTimeMillis();
		idleCheckTimer();
	}
	
	public void idleCheckTimer(){
		// Start 10 second timer
		tvlist_idle_timeout.postDelayed( new Runnable() {
			@Override
			public void run() {
				long now = System.currentTimeMillis();
				long difference = ( now - current_timestamp )/1000;
				
				Log.d( TAG, "idleCheckTimer() run after 10 seconds, now : "+now+", current_timestamp : "+current_timestamp+", difference : "+difference );
				
				if( difference < 20 ){
					idleCheckTimer();
				}
				else{
					// Close the TV List
					finish();  
					//TvList.this.overridePendingTransition( R.anim.show_tv_list_anim, R.anim.hide_tv_list_anim );
				
				}
			}
		}, 5000 );
		
	}
	
	
	@Override
	public void onUserInteraction() {
		super.onUserInteraction();
		Log.d( TAG, "onUserInteraction()" );
		
		current_timestamp = System.currentTimeMillis();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d( TAG, "inside onResume()" );
		
		int category_position = Integer.parseInt( (String)UtilSharedPreferences.getSharedPreference( spfs, "category_position", "-1" ) );
		int channel_position = Integer.parseInt( (String)UtilSharedPreferences.getSharedPreference( spfs, "channel_position", "-1" ) );
		
		/*if( channel_position != -1 ){
			list_main.setSelection( category_position );
			dispatchKeyEvent( new KeyEvent( KeyEvent.ACTION_DOWN, 22 ) );
			// list_sub.setVisibility( View.VISIBLE );
			list_sub.setSelection( channel_position );
			list_sub.requestFocus();
		}  */
		Log.d( TAG, "channel_position : "+channel_position ); 
		 
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		
		Log.d( TAG, "onBackPressed()" );
		finish();  
		this.overridePendingTransition( R.anim.show_tv_list_anim, R.anim.hide_tv_list_anim );
	
	}

	public int index_main_list = 0;
	
	private void setOnMainListFocusChangedListener(){
		// list_main.setSelection( -1 );
		list_main.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange( View v, boolean hasFocus ) {
				ListView lv = (ListView) v;
				
				if( hasFocus ){
					Log.i( TAG, "Focus gained on main List, main list position from backup : "+index_main_list );        
					list_main.setSelection( index_main_list );
					switchFocus( FOCUSSED_LEFT );
					animateSubListOut();
					showSubList();	
					// int position = lv.getSelectedItemPosition();
					// Log.i( TAG, "Position of focussed item on main list : "+position );
				}
				else{
					index_main_list = list_main.getSelectedItemPosition();
					Log.i( TAG, "Focus lost from main List, main list position : "+index_main_list );   
					switchFocus( FOCUSSED_RIGHT );
				}
			}
		});
	}
	
	private void setOnSubListFocusChangedListener(){
		list_sub.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange( View v, boolean hasFocus ) {
				if( hasFocus ){
					Log.i( TAG, "Focus gained on sub List" );        
					switchFocus( FOCUSSED_RIGHT );
					// list_main.setSelection( index_main_list );
					// int position = lv.getSelectedItemPosition();
					// Log.i( TAG, "Position of focussed item on main list : "+position );
				}
				else{
					switchFocus( FOCUSSED_LEFT );
					
					// index_main_list = list_main.getSelectedItemPosition();
					Log.i( TAG, "Focus lost from Sub List" );        
				}
			}
		});
	}
	
	private void setOnMainListClickListener(){
		list_main.setOnItemSelectedListener( new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				Log.i( TAG, "setOnItemSelectedListener called" );
				//view.animate().setDuration( 1000 ).alpha( 0.5f );
				/*ObjectAnimator anim = ObjectAnimator.ofFloat( view, View.ALPHA, 0.5f );
				anim.setDuration( 1000 );
				anim.start();*/
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
				
			}
			
		});
	}
	
	private void setOnSubListClickListener(){
		list_sub.setOnItemClickListener( new OnItemClickListener() { 

			@Override
			public void onItemClick( AdapterView<?> parent, View view, int position, long id ) {
				// Log.i( TAG, "position : "+position );        
				LinearLayout ll = (LinearLayout) view;
				TextView tv_channel_url  = (TextView) ll.findViewById( R.id.tv_channel_url );
				TextView tv_channel_id   = (TextView) ll.findViewById( R.id.tv_channel_id );
				TextView tv_channel_name = (TextView) ll.findViewById( R.id.tv_channel_name );
				
				String tv_ch_url = tv_channel_url.getText().toString();
				Log.i( TAG, "position : "+position+", Url : "+tv_ch_url );        
				 
				Intent intent = new Intent( "switch_channel" );
				intent.putExtra( "url", tv_ch_url );
				LocalBroadcastManager.getInstance( TvList.this ).sendBroadcast( intent );
				
				// Store the data into spfs
				UtilSharedPreferences.editSharedPreference( spfs, "channel_id", tv_channel_id.getText().toString() );
				UtilSharedPreferences.editSharedPreference( spfs, "channel_url", tv_ch_url );
				UtilSharedPreferences.editSharedPreference( spfs, "channel_name", tv_channel_name.getText().toString() );
				UtilSharedPreferences.editSharedPreference( spfs, "channel_position", String.valueOf( position ) );
				UtilSharedPreferences.editSharedPreference( spfs, "category_position", String.valueOf( list_main.getSelectedItemPosition() ) );
				
				/*Intent in = new Intent( TvList.this, Player.class );
				in.putExtra( "url", tv_ch_url );
				startActivity( in );*/
			}
			
		});
	}
	
	
	
	
	private void initializeSQLite(){
		sqldb = UtilSQLite.makeDatabase( "tv_channels.db", context ); 
		
		String sql = "SELECT count(*) FROM categories as count";
		Cursor 	 c = UtilSQLite.executeQuery( sqldb, sql, false );
		
		if( c == null ){
			Toast.makeText( context, "TV Channels have not yet synchronized !", Toast.LENGTH_LONG ).show();
			finish();
			//return;
		}
		
		int no_of_categories = c.getCount();
		
		// sql = "SELECT * FROM categories a, channels b WHERE a.id = b.category_id";
		sql = "SELECT * FROM categories ORDER BY sequence";
		c	= UtilSQLite.executeQuery( sqldb, sql, false );
		
		main_list_items = new String[ c.getCount() ][ 2 ];
		for( int i = 0 ; i < c.getCount() ; i++ ){
			c.moveToNext();
			main_list_items[ i ][ CATEGORY_NAME ] = c.getString( c.getColumnIndex( "category_name" ) );
			main_list_items[ i ][ CATEGORY_ID ] = c.getString( c.getColumnIndex( "id" ) );
		}
		
	}
	
	private void initializeSharedPreferences(){
		spfs = UtilSharedPreferences.createSharedPreference( context, SPFS_LAST_CHANNEL_HISTORY );
	}
	
	int LAST_MAIN_LIST_VALUE = 0;
	Parcelable state;
	@Override
	public boolean onKeyDown( int keyCode, KeyEvent event ) {
		Log.i( TAG, "Key pressed : "+event.getKeyCode() );
		
		int code = event.getKeyCode();
		
		if( ( code == 21 ) && ( focus == FOCUSSED_RIGHT ) ){ // Left Pressed && FOCUSSED_RIGHT
			// list_main.setSelectionFromTop( index_main_list, 0 ); 
			// Hide the sub list
			//list_sub.setVisibility( View.INVISIBLE );
			animateSubListOut();
			showSubList();	
			// list_main.setSelection( LAST_MAIN_LIST_VALUE );
			// Log.d( TAG, "LAST_MAIN_LIST_VALUE : "+LAST_MAIN_LIST_VALUE );
			// Log.d( TAG, "index_main_list : "+index_main_list );
			// showSubList();	
			
			// Restore previous state (including selected item index and scroll position)
		    if(state != null) {
		        //Log.d(TAG, "trying to restore listview state..");
		        list_main.onRestoreInstanceState(state);
		        list_main.requestFocus();
		    }
		    return true;
		}
		else if( ( code == 22 ) && ( focus == FOCUSSED_LEFT ) ){ // RIGHT Pressed && FOCUSSED_LEFT
			// list_main.setSelectionFromTop( index_main_list, 0 ); 
			// Show the sub list
			showSubList();
			animateSubListIn();
			state = list_main.onSaveInstanceState();
			
		}
		else if( ( code == 20 ) && ( focus == FOCUSSED_RIGHT ) ){ // Down pressed && FOCUSSED_RIGHT -> Last element in the list has been reached
			list_sub.setSelected( true ); 
			list_sub.setSelection( 0 );
			
			return true;
		}
		/*else if( ( code == 20 ) && ( focus == FOCUSSED_LEFT ) ){ // Up pressed && FOCUSSED_RIGHT
			
		}*/
		/*else if( code == 4 ){ // Back pressed
			Intent in = new Intent( context, Player.class );
			startActivity( in ); 
			finish();
			return true;
		}*/
		
		
		return super.onKeyDown( keyCode, event );
	}

	@Override
	public boolean onKeyUp( int keyCode, KeyEvent event ) {
		Log.i( TAG, "Key pressed : "+event.getKeyCode() );
		
		int code = event.getKeyCode();
		
		return super.onKeyUp(keyCode, event);
	}
	
	public void switchFocus( int switch_to ){
		if( switch_to == FOCUSSED_LEFT ){
			focus = 0;
			// list_sub.setVisibility( View.INVISIBLE );
		}
		else if( switch_to == FOCUSSED_RIGHT ){
			focus = 1;
			// list_sub.setVisibility( View.VISIBLE );
		}
		else
			focus = 0;
	}
	
	public void showSubList(){
		//list_sub.setVisibility( View.VISIBLE );
		String position = String.valueOf( list_main.getSelectedItemPosition() );
		Log.e(TAG, "pos : "+position );
		if( position.equals( "-1" ) )
			position = "0";
		
		tv_category_name.setText( main_list_items[ Integer.parseInt( position ) ][ CATEGORY_NAME ] );
		
		String sql = "";
		if( main_list_items[ Integer.parseInt( position ) ][ CATEGORY_ID ].equals( "0" ) )
			sql = "Select * FROM channels GROUP BY channel_id ORDER BY id ";
		else
			sql = "Select * FROM channels WHERE category_id ="+main_list_items[ Integer.parseInt( position ) ][ CATEGORY_ID ]+" ORDER BY sequence";
		
		Log.i( TAG, "sql : "+sql );
		Cursor c = UtilSQLite.executeQuery( sqldb, sql, false );
		
		String[][] temp = new String[ c.getCount() ][ c.getColumnCount() ];
		for( int i = 0 ; i < c.getCount() ; i++ ){
			c.moveToNext();
			temp[ i ][ CHANNEL_ID ] = c.getString( c.getColumnIndex( "id" ) );
			temp[ i ][ CHANNEL_SEQUENCE ] = c.getString( c.getColumnIndex( "sequence" ) );
			temp[ i ][ CHANNEL_NAME ] = c.getString( c.getColumnIndex( "channel_name" ) );
			temp[ i ][ CHANNEL_URL ] = c.getString( c.getColumnIndex( "channel_url" ) );
			temp[ i ][ CHANNEL_ICON ] = c.getString( c.getColumnIndex( "icon" ) );
		}
		
		sla  = new SubListAdapter( context, temp, R.layout.list_item_subitem );
		
		list_sub.setAdapter( sla );
	}
	
	
	
	public void animateSubListIn(){
		// Animate Sub-List In
		ObjectAnimator transAnimation = ObjectAnimator.ofFloat( list_sub, "translationX", -200 );
		transAnimation.setDuration( 500 );//set duration
		transAnimation.start();//start animation
		
		ObjectAnimator transAnimation1 = ObjectAnimator.ofFloat( tv_category_name, "translationX", -200 );
		transAnimation1.setDuration( 500 );//set duration
		transAnimation1.start();//start animation
		
		// Animation anim_in = AnimationUtils.loadAnimation( context, R.anim.sub_list_in );
		// list_sub.startAnimation( anim_in );
		
		// Make Sub-list Blue background
		list_sub.setBackgroundResource( R.drawable.list_background_blue );
		list_sub.getBackground().setAlpha( 75 );
		
		// Make Main-list Alpha 0
		ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat( list_main, "alpha", 1.0f, 0.0f );
		alphaAnimation.setDuration( 500 );//set duration
		alphaAnimation.start();//start animation
		
		// list_main.setLayoutParams( new LayoutParams( 100, list_main.getHeight() ) );
		ObjectAnimator arrowAnimation = ObjectAnimator.ofFloat( iv_left_arrow, "alpha", 0.0f, 1.0f );
		arrowAnimation.setDuration( 500 );//set duration
		arrowAnimation.start();//start animation
	}
	
	public void animateSubListOut(){
		// Animate Sub-List In
		ObjectAnimator transAnimation = ObjectAnimator.ofFloat( list_sub, "translationX", 0 );
		transAnimation.setDuration( 600 );//set duration
		transAnimation.start();//start animation
		
		ObjectAnimator transAnimation1 = ObjectAnimator.ofFloat( tv_category_name, "translationX", 0 );
		transAnimation1.setDuration( 600 );//set duration
		transAnimation1.start();//start animation
		
		// Animation anim_in = AnimationUtils.loadAnimation( context, R.anim.sub_list_in );
		// list_sub.startAnimation( anim_in );
		
		// Make Sub-list Transparent background
		list_sub.setBackgroundResource( R.drawable.sub_list_background );
		list_sub.getBackground().setAlpha( 0 );
		
		// Make Main-list Alpha 1
		ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat( list_main, "alpha", 0.0f, 1.0f );
		alphaAnimation.setDuration( 500 );//set duration
		alphaAnimation.start();//start animation
		
		// Make Main List Blue Background
		list_main.setBackgroundResource( R.drawable.list_background_blue );
		list_main.getBackground().setAlpha( 75 );
		
		
		ObjectAnimator arrowAnimation = ObjectAnimator.ofFloat( iv_left_arrow, "alpha", 1.0f, 0.0f );
		arrowAnimation.setDuration( 500 );//set duration
		arrowAnimation.start();//start animation
	}
}
