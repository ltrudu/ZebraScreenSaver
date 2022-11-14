package com.zebra.screensaver;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class Constants {
    public static final String TAG  ="ScreenSaverService";

    // Shared preference keys
    public static final String SHARED_PREFERENCES_NAME = "ScreenSaverService";
    public static final String SHARED_PREFERENCES_START_SERVICE_ON_BOOT = "startonboot";
    public static final String SHARED_PREFERENCES_START_SERVICE_ON_CHARGING = "startoncharging";
    public static final String SHARED_PREFERENCES_TIMEOUT = "timeout";

    public static final String EXTRA_CONFIGURATION_START_ON_BOOT = "startonboot";
    public static final String EXTRA_CONFIGURATION_START_ON_CHARGING = "startoncharging";






    private String TARGET_APP_PACKAGE = "target_app_package"; 
    private String DATA_NAME = "data_name"; 
    private String DATA_VALUE = "data_value"; 
    private String DATA_INPUT_FORM = "data_input_form"; 
    private String DATA_OUTPUT_FORM = "data_output_form"; 
    private String DATA_PERSIST_REQUIRED = "data_persist_required"; 
    private String MULTI_INSTANCE_REQUIRED = "multi_instance_required"; 

             

    private String AUTHORITY = "content://com.zebra.securestoragemanager.securecontentprovider/data"; 

    private void InsertSnippet(Context context)
    {
        Uri cpUri = Uri.parse(AUTHORITY); 
        ContentValues values = new ContentValues();
        // TARGET_APP_PACKAGE gives both package name info and signature info in single entry. This can be either single target app or even multiple target apps.
        values.put(TARGET_APP_PACKAGE,
                "{\"pkgs_sigs\": [{\"pkg\":\"com.ztestapp.clientapplication\",\"sig\":\"ABSFFSDF… WREWED\"}]}");
        //Dummy sig is placed here. use SigTool to get this base64 String.

        values.put(DATA_NAME, "unique name to identify data in UTF-8 encoded format");
        values.put(DATA_VALUE,"any string data/json data");
        values.put(DATA_INPUT_FORM, "1"); //plaintext =1, encrypted=2

         values.put(DATA_OUTPUT_FORM, "1"); //plaintext=1, encrypted=2
        values.put(DATA_PERSIST_REQUIRED, "false");
        values.put(MULTI_INSTANCE_REQUIRED, "false");


        Uri createdRow = context.getContentResolver().insert(cpUri, values);

        Log.d(TAG, "Created row: " + createdRow.toString());
    }

    
    private void Query(Context context, String packageName)
    {
        Uri cpUriQuery = Uri.parse(AUTHORITY + "/[com.ztestapp.clientapplication]");
        String selection = "target_app_package = '" + packageName + "'" +"AND "+ "data_persist_required = '" + "false" + "'" +  
        "AND "+"multi_instance_required = '"+ "true" + "'"; 
        Cursor cursor = null;
        try {  
            cursor = context.getContentResolver().query(cpUriQuery, null, selection, null, null);
        } catch (Exception e) {
            Log.d(TAG, "Error: "+ e.getMessage());
        }
        //Then traverse the cursor object to get the required field values in standard android way. Not shown here.
    }
    
}
