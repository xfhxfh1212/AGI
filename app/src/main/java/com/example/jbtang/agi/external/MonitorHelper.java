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
    private Intent startIntent;
    private MonitorService mBoundService;
    private ServiceConnection connection = new ServiceConnection() {

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
    public void startService(Context context) {
        MonitorApplication.IMEI = getIMEI(context);
        startIntent = new Intent(context, MonitorService.class);
        context.startService(startIntent);

        Log.d("test", "startService");
    }
    public void bindService(Context context){
        if(startIntent == null)
            startIntent = new Intent(context,MonitorService.class);
        context.bindService(startIntent, connection, 0);
    }
    public void unbindservice(Context context){
        context.unbindService(connection);
    }
    public String getIMEI(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        String imei = telephonyManager.getDeviceId();
        Log.d("test", "get IMEI:"+imei);
        return imei != null ? imei : "";
    }
    public void stopService(Context context){

        context.stopService(startIntent);
        Log.d("test", "stopService");
    }
}
