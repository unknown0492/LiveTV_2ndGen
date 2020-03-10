package com.excel.livetv;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.excel.configuration.ConfigurationReader;
import com.excel.customitems.CustomItems;
import com.excel.excelclasslibrary.UtilFile;
import com.excel.excelclasslibrary.UtilNetwork;
import com.excel.excelclasslibrary.UtilSQLite;
import com.excel.excelclasslibrary.UtilSharedPreferences;
import com.excel.excelclasslibrary.UtilShell;
import com.excel.excelclasslibrary.UtilURL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;


public class SplashLoading extends Activity {

    Context context = this;
    AnimatedGifImageView iv_loading;
    final static String TAG = "SplashLoading";
    ConfigurationReader configurationReader;
    String appstv_live_channels_key;

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

    SharedPreferences spfs;
    BroadcastReceiver receiver;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_splash_loading );

        registerAllBroadcasts();

        spfs = (SharedPreferences) UtilSharedPreferences.createSharedPreference( this, Constants.PERMISSION_SPFS );

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
            if ( checkPermissions() ) {
                // permissions  granted.
                UtilSharedPreferences.editSharedPreference( spfs, Constants.IS_PERMISSION_GRANTED, Constants.PERMISSION_GRANTED_YES );
                //finish();
            }
        }

        configurationReader = ConfigurationReader.getInstance();

        iv_loading = (AnimatedGifImageView) findViewById( R.id.iv_loading );
        iv_loading.setMaxHeight( 20 );
        iv_loading.setMaxWidth( 20 );
        iv_loading.setAnimatedGif( context.getResources().getIdentifier( "drawable/loading1" , null, context.getPackageName() ), AnimatedGifImageView.TYPE.AS_IS );

        createTvChannelsDatabase();

        //getAccessKey();
        AsyncLiveTVData asyncLiveTVData = new AsyncLiveTVData();
        asyncLiveTVData.execute();
    }

    Vector<IntentFilter> intentFilterVector;

    private void registerAllBroadcasts(){
        receiver = new Receiver();
        intentFilterVector = new Vector<IntentFilter>();
        intentFilterVector.add( new IntentFilter( "connectivity_change" ) );
        intentFilterVector.add( new IntentFilter( "download_iptv_channels" ) );


        Iterator<IntentFilter> iterator = intentFilterVector.iterator();
        while( iterator.hasNext() ){
            registerReceiver( receiver, iterator.next() );
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver( receiver );
    }

    public void createTvChannelsDatabase(){
        SQLiteDatabase sqldb = UtilSQLite.makeDatabase( "tv_channels.db", context );
        String sql1 = "CREATE TABLE categories (sequence TEXT, id INTEGER PRIMARY KEY, category_name TEXT)";
        String sql2 = "CREATE TABLE channels (icon TEXT, category_id NUMERIC, id INTEGER PRIMARY KEY, channel_id INTEGER, sequence NUMERIC, channel_name TEXT, channel_url TEXT)";
        // UtilSQLite.executeQuery( sqldb, sql1, false );
        // UtilSQLite.executeQuery( sqldb, sql2, false );
        try {
            sqldb.execSQL(sql1);
        }
        catch ( Exception e ){
            e.printStackTrace();
        }
        try {
            sqldb.execSQL( sql2 );
        }
        catch ( Exception e ){
            e.printStackTrace();
        }

    }


    class AsyncLiveTVData extends AsyncTask< String, Integer, String > {

        @Override
        protected String doInBackground( String... params ) {
            String url = UtilURL.getWebserviceURL(  );
            Log.d( TAG, "Webservice path : " + url );
            String response = UtilNetwork.makeRequestForData( url, "POST",
                    UtilURL.getURLParamsFromPairs( new String[][]{ { "what_do_you_want", "get_iptv_channels" },
                            { "mac_address", UtilNetwork.getMacAddress( context ) } } ) );

            return response;
        }

        @Override
        protected void onPostExecute( String result ) {
            super.onPostExecute( result );

            Log.i( TAG,  "inside onPostExecute()" );

            if( result != null ){

                //Log.i( TAG,  result );
                try {
                    JSONArray jsonArray = new JSONArray( result );
                    JSONObject jsonObject1 = jsonArray.getJSONObject( 0 );

                    String type = jsonObject1.getString( "type" );
                    if( type.equals( "success" ) ){

                        JSONObject jsonObject = jsonObject1.getJSONObject( "info" );

                        /*File tempFile = new File( ( getFilesDir() + File.separator + "json.data" ) );
                        UtilFile.saveDataToFile( tempFile, responsesArray.toString() );*/

                        JSONArray categories = jsonObject.getJSONArray( "categories" );
                        JSONArray channels = jsonObject.getJSONArray( "channels" );

                        // Get Database Instance
                        SQLiteDatabase sqldb = UtilSQLite.makeDatabase( "tv_channels.db", context );
                        // Truncate the tables
                        UtilSQLite.executeQuery( sqldb, "DELETE FROM categories", true );
                        UtilSQLite.executeQuery( sqldb, "DELETE FROM channels", true );

                        String sql1 = "INSERT INTO categories( id, sequence, category_name ) VALUES";
                        for( int i = 0 ; i < categories.length() ; i++ ){
                            jsonObject = categories.getJSONObject( i );
                            sql1 += String.format( "( %s, %s, '%s' ),", jsonObject.getString( "id" ), jsonObject.getString( "sequence" ), jsonObject.getString( "category_name" ) );
                        }
                        sql1 = sql1.substring( 0, sql1.length() - 1 );
                        Log.d( TAG, sql1 );

                        String sql2 = "INSERT INTO channels( channel_id, category_id, sequence, icon, channel_name, channel_url ) VALUES";
                        for( int i = 0 ; i < channels.length() ; i++ ){
                            jsonObject = channels.getJSONObject( i );
                            sql2 += String.format( "( %s, %s, %s, '%s', '%s', '%s' ),", jsonObject.getString( "channel_id" ), jsonObject.getString( "category_id" ), jsonObject.getString( "sequence" ), jsonObject.getString( "icon" ), jsonObject.getString( "channel_name" ), jsonObject.getString( "channel_url" ) );
                        }
                        sql2 = sql2.substring( 0, sql2.length() - 1 );
                        Log.d( TAG, sql2 );

                        // Insert new values in the database
                        UtilSQLite.executeQuery( sqldb, sql1, true );
                        UtilSQLite.executeQuery( sqldb, sql2, true );

                        UtilShell.executeShellCommandWithOp( "setprop is_iptv_channels_synced 1" );

                        /*Intent in = new Intent( context, Player.class );
                        startActivity( in );
                        //SplashLoading.this.overridePendingTransition( R.anim.show_tv_list_anim, R.anim.hide_tv_list_anim );
                        finish();*/
                    }
                    else if( type.equals( "error" ) ){
                        String info = jsonObject1.getString( "info" );
                        Log.e( TAG, info );
                        CustomItems.showCustomToast( context, type, info, Toast.LENGTH_LONG );
                        new Handler().postDelayed( new Runnable() {
                            @Override
                            public void run() {
                                finish();

                            }
                        }, 3000 );
                    }

                }
                catch ( JSONException e ) {
                    e.printStackTrace();

                }
            }
            else{
                Log.e( TAG, "Null was returned " );
                Toast.makeText( context, "Cannot sync new TV channels !", Toast.LENGTH_LONG ).show();
            }

            Intent in = new Intent( context, LiveTV.class );
            startActivity( in );
            //SplashLoading.this.overridePendingTransition( R.anim.show_tv_list_anim, R.anim.hide_tv_list_anim );
            finish();

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
}
