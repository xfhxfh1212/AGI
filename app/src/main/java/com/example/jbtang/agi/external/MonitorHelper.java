package com.example.jbtang.agi.external;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.example.jbtang.agi.external.service.MonitorService;

/**
 * Created by xiang on 2016/3/9.
 */
public class MonitorHelper {
    private static Intent startIntent;
    private static MonitorService mBoundService;
    private static ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {

            mBoundService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBoundService = ((MonitorService.LocalBinder) service).getService();
            MonitorApplication.MonitorService = mBoundService;
        }
    };
    public static void startService(Context context) {
        MonitorApplication.IMEI = getIMEI(context);
        startIntent = new Intent(context, MonitorService.class);
        context.startService(startIntent);
        context.bindService(startIntent, connection, 0);
        Log.e("test", "startService");
    }
    public static String getIMEI(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        String imei = telephonyManager.getDeviceId();
        Log.e("test", "get IMEI:"+imei);
        return imei != null ? imei : "";
    }
    public static void stopService(Context context){
        context.unbindService(connection);
        context.stopService(startIntent);
        Log.e("test", "stopService");
    }
}
