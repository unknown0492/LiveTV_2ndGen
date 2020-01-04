package com.excel.livetv;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.excel.customitems.CustomItems;
import com.excel.excelclasslibrary.UtilMisc;
import com.excel.excelclasslibrary.UtilNetwork;
import com.excel.excelclasslibrary.UtilShell;

public class ShortcutsActivity extends Activity {
	
	TextView tv_mac_address, tv_firmware_version, tv_ip_address;
	Button  bt_reboot, bt_root_browser, bt_mbox, bt_settings,
			bt_reboot_recovery;
	Context context = this;
	final static String TAG = "ShortcutsActivity";
	LinearLayout ll_left_remaining, ll_right_remaining;
	
	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_shortcuts );
		
		init();

	}

	public void init(){
		initViews();
		
		try {
			// Set Mac Address
			setMacAddress();
		}
		catch( Exception e ){
			e.printStackTrace();
		}

		// Set Firmware Version
		setFirmwareVersion();

		// Set IP Address
		setIPAddress();

		// Reboot Box
		rebootBoxButtonClick();

		// Root Browser Click
		rootBrowserClick();
		
		// MBox Settings Click
		mboxSettingsClick();
		
		// Android Settings Click
		androidSettingsClick();

		// Reboot To Recovery
		rebootToRecovery();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	public void initViews(){
		tv_firmware_version = (TextView) findViewById( R.id.tv_firmware_name );
		tv_mac_address = (TextView) findViewById( R.id.tv_mac_address );
		tv_ip_address = (TextView) findViewById( R.id.tv_ip_address );
		bt_reboot = (Button) findViewById( R.id.bt_reboot_box );
		bt_root_browser = (Button) findViewById( R.id.bt_root_browser );
		bt_mbox = (Button) findViewById( R.id.bt_mbox );
		bt_settings = (Button) findViewById( R.id.bt_settings );
		bt_reboot_recovery = (Button) findViewById( R.id.bt_reboot_recovery );
	}
	
	public void setMacAddress(){
		String mac_address = UtilNetwork.getMacAddress( context );
		if( mac_address == null )
			mac_address = "Network Disconnected";
		tv_mac_address.setText( mac_address );
	}
	
	public void setFirmwareVersion(){
		String firmwareName = UtilShell.executeShellCommandWithOp( "getprop ro.build.display.id" );
		firmwareName = firmwareName.trim();
		tv_firmware_version.setText( firmwareName );
	}

	public void setIPAddress(){
		tv_ip_address.setText( UtilNetwork.getLocalIpAddressIPv4( context ) );
	}
	
	public void rebootBoxButtonClick(){
		bt_reboot.setOnClickListener( new OnClickListener() {
			
			@Override
			public void onClick( View v ) {
				UtilShell.executeShellCommandWithOp( "reboot" );
			}
		});
	}

	public void rootBrowserClick(){
		bt_root_browser.setOnClickListener( new OnClickListener() {
			
			@Override
			public void onClick( View v ) {
				if( !UtilMisc.startApplicationUsingPackageName( context, "com.jrummy.root.browserfree" ) ){
					CustomItems.showCustomToast( context, "error", "Root Borwser Free version is not installed", 5000 );
					
					if( !UtilMisc.startApplicationUsingPackageName( context, "com.jrummy.root.browser" ) ){
						CustomItems.showCustomToast( context, "error", "Root Borwser Full version is not installed", 5000 );
					}
				}
			}
		});
	}

	public void mboxSettingsClick(){
		bt_mbox.setOnClickListener( new OnClickListener() {
			
			@Override
			public void onClick( View v ) {
                if( Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT ) {
                    if (!UtilMisc.startApplicationUsingPackageName(context, "com.mbx.settingsmbox") ) {
                        //if( !UtilMisc.startApplicationUsingPackageName( context, "com.mbx.settingsmbox" ) ){
                        CustomItems.showCustomToast(context, "error", "GIEC Settings not found", 5000);
                    }
                }
                else {
                    if (!UtilMisc.startApplicationUsingPackageName(context, "com.sdmc.settings")) {
                        //if( !UtilMisc.startApplicationUsingPackageName( context, "com.mbx.settingsmbox" ) ){
                        CustomItems.showCustomToast(context, "error", "SDMC Settings not found", 5000);
                    }
                }
			}
		});
	}
	
	public void androidSettingsClick(){
		bt_settings.setOnClickListener( new OnClickListener() {
			
			@Override
			public void onClick( View v ) {
				Log.d( TAG, "Open Settings" );

				if( Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT ) {
					UtilShell.executeShellCommandWithOp( "monkey -p com.android.settings -c android.intent.category.LAUNCHER 1" );
				}
				else {
					UtilShell.executeShellCommandWithOp( "am start -a android.intent.action.MAIN -n com.android.settings/.Settings" );
				}
			}
		});
	}
	
	public void rebootToRecovery(){
		bt_reboot_recovery.setOnClickListener( new OnClickListener() {
			
			@Override
			public void onClick( View v ) {
				UtilShell.executeShellCommandWithOp( "reboot recovery" );
			}
		});
	}

	@Override
	protected void onPause() {
		super.onPause();

		Log.i( TAG, "onPause()" );

		finish();
	}
}
