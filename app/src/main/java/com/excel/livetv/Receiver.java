package com.excel.livetv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.excel.excelclasslibrary.UtilSharedPreferences;

public class Receiver extends BroadcastReceiver {
	
	public static final String TAG = "Receiver";
	SharedPreferences spfs;
	
	@Override
	public void onReceive( Context context, Intent intent ) {
		String action = intent.getAction();
		Log.i( TAG, "action : "+action );

		spfs = UtilSharedPreferences.createSharedPreference( context, "md5" );

		if( action.equals( "connectivity_change" ) ){

			//Intent in = new Intent( context, TVChannelDownloaderService.class );
			//context.startService( in );

		}
		else if( action.equals( "download_iptv_channels" ) ){
			//Intent in = new Intent( context, TVChannelDownloaderService.class );
			//context.startService( in );
			
		}

	}


}
