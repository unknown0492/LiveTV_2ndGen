package com.excel.livetv;

import static com.excel.livetv.Constants.SPFS_LAST_CHANNEL_HISTORY;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.excel.excelclasslibrary.UtilSQLite;
import com.excel.excelclasslibrary.UtilSharedPreferences;

public class CopyOfTvList extends Activity {
	
	public static final String TAG = "TvList";
	
	ListView list_main;
	ListView list_sub;
	Context context = this;
	
	public static int focus = 0;
	public static int FOCUSSED_LEFT = 0;
	public static int FOCUSSED_RIGHT = 1;
	
	SQLiteDatabase sqldb;
	SharedPreferences spfs;
	
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
		
		MainListAdapter mla = new MainListAdapter( context, main_list_items, R.layout.list_item );
		// sla  = new SubListAdapter( context, sub_list_items, R.layout.list_item_subitem );
		// ArrayAdapter<String> aap1 = new ArrayAdapter<String>( TvList.this, android.R.layout.simple_list_item_1, sub_list_items );
		list_main.setAdapter( mla ); 
		list_main.setBackgroundResource( R.drawable.list_background_blue );
		list_main.getBackground().setAlpha( 75 );  
		
		list_sub.setAdapter( sla );
		list_sub.setBackgroundResource( R.drawable.list_background_transparent );
		list_sub.getBackground().setAlpha( 0 ); 
		
		setOnMainListClickListener();     
		setOnSubListClickListener();     
		
		setOnMainListFocusChangedListener();
		setOnSubListFocusChangedListener();
		 
		
		list_main.setOnItemSelectedListener( new OnItemSelectedListener() {

			@Override
			public void onItemSelected( AdapterView<?> parent, View view, int position, long id ) {
				Log.d( TAG, "changed" );
				showSubList();
				list_sub.setBackgroundResource( R.drawable.list_background_transparent );
				list_sub.getBackground().setAlpha( 0 );
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
				
			}
			
		});
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
	
	public int index_main_list = 0;
	
	private void setOnMainListFocusChangedListener(){
		list_main.setSelection( -1 );
		list_main.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange( View v, boolean hasFocus ) {
				ListView lv = (ListView) v;
				
				if( hasFocus ){
					Log.i( TAG, "Focus gained on main List, main list position from backup : "+index_main_list );        
					// list_main.setSelection( index_main_list );
					switchFocus( FOCUSSED_LEFT );
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
					Log.i( TAG, "Focus lost from main List" );        
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
		
		/*list_main.setOnItemClickListener( new OnItemClickListener() { 

			@Override
			public void onItemClick( AdapterView<?> parent, View view, int position, long id ) {
				Log.i( TAG, "position : "+position );        
				if( position == 0 ){
					sla  = new SubListAdapter( context, s1, R.layout.list_item_subitem );
				}
				else if( position == 1 ){
					sla  = new SubListAdapter( context, s2, R.layout.list_item_subitem );
				}
				else if( position == 3 ){
					sla  = new SubListAdapter( context, s3, R.layout.list_item_subitem );
				}
				else{
					sla  = new SubListAdapter( context, sub_list_items, R.layout.list_item_subitem );
				}
				list_sub.setAdapter( sla );
				list_sub.setVisibility( View.VISIBLE ); 
				// UtilShell.executeShellCommandWithOp( "input keyevent 22" );
				
				list_sub.setSelected( true );
				list_main.setSelected( false );
			}
			
		});*/
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
				LocalBroadcastManager.getInstance( CopyOfTvList.this ).sendBroadcast( intent );
				
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
	
	
	public static final int CATEGORY_ID   = 0;
	public static final int CATEGORY_NAME = 1;
	
	public static final int CHANNEL_ID	  		= 0;
	public static final int CHANNEL_SEQUENCE  	= 1;
	public static final int CHANNEL_NAME		= 2;
	public static final int CHANNEL_URL			= 3;
	
	
	private void initializeSQLite(){
		sqldb = UtilSQLite.makeDatabase( "tv_channels.db", context );
		
		String sql = "SELECT count(*) FROM categories as count";
		Cursor 	 c = UtilSQLite.executeQuery( sqldb, sql, false );
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
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.i( TAG, "Key pressed : "+event.getKeyCode() );
		
		int code = event.getKeyCode();
		
		if( ( code == 21 ) && ( focus == FOCUSSED_RIGHT ) ){ // Left Pressed && FOCUSSED_RIGHT
			// list_main.setSelectionFromTop( index_main_list, 0 ); 
			// Hide the sub list
			list_sub.setVisibility( View.INVISIBLE );
		}
		else if( ( code == 22 ) && ( focus == FOCUSSED_LEFT ) ){ // RIGHT Pressed && FOCUSSED_LEFT
			// list_main.setSelectionFromTop( index_main_list, 0 ); 
			// Show the sub list
			showSubList();
			
			// Animate Sub-List In
			Animation anim_in = AnimationUtils.loadAnimation( context, R.anim.sub_list_in );
			list_sub.startAnimation( anim_in );
			
			list_main.setBackgroundResource( R.drawable.list_background_transparent );
			list_main.getBackground().setAlpha( 0 );
			
			list_sub.setBackgroundResource( R.drawable.list_background_blue );
			list_sub.getBackground().setAlpha( 100 );
		}
		else if( ( code == 20 ) && ( focus == FOCUSSED_RIGHT ) ){ // Down pressed && FOCUSSED_RIGHT -> Last element in the list has been reached
			list_sub.setSelected( true ); 
			list_sub.setSelection( 0 );
			
			
			
			return true;
		}  
		else if( code == 4 ){ // Back pressed
			Intent in = new Intent( context, Player.class );
			startActivity( in ); 
			finish();
			return true;
		}
		
		
		return super.onKeyDown(keyCode, event);
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
		list_sub.setVisibility( View.VISIBLE );
		String position = String.valueOf( list_main.getSelectedItemPosition() );
		
		String sql = "Select * FROM channels WHERE category_id ="+main_list_items[ Integer.parseInt( position ) ][ CATEGORY_ID ]+" ORDER BY sequence";
		Log.i( TAG, "sql : "+sql );
		Cursor c = UtilSQLite.executeQuery( sqldb, sql, false );
		
		String[][] temp = new String[ c.getCount() ][ 4 ];
		for( int i = 0 ; i < c.getCount() ; i++ ){
			c.moveToNext();
			temp[ i ][ CHANNEL_ID ] = c.getString( c.getColumnIndex( "id" ) );
			temp[ i ][ CHANNEL_SEQUENCE ] = c.getString( c.getColumnIndex( "sequence" ) );
			temp[ i ][ CHANNEL_NAME ] = c.getString( c.getColumnIndex( "channel_name" ) );
			temp[ i ][ CHANNEL_URL ] = c.getString( c.getColumnIndex( "channel_url" ) );
		}
		
		sla  = new SubListAdapter( context, temp, R.layout.list_item_subitem );
		
		list_sub.setAdapter( sla );
	}
}
