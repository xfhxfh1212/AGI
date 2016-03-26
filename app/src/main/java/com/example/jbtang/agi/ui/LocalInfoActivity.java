package com.example.jbtang.agi.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;

import com.example.jbtang.agi.R;
import com.example.jbtang.agi.core.CellInfo;
import com.example.jbtang.agi.core.Global;
import com.example.jbtang.agi.external.MonitorApplication;
import com.example.jbtang.agi.external.MonitorHelper;
import com.example.jbtang.agi.external.service.MonitorService;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.fmaster.LTEPwrInfoMessage;
import io.fmaster.LTEServCellMessage;

public class LocalInfoActivity extends AppCompatActivity {
    private TextView myStmsiTextView;
    private TextView myErfcnTextView;
    private TextView myPciTextView;
    private TextView myTaiTextView;
    private TextView myRsrpTextView;
    private TextView mySinrTextView;
    private MonitorHelper monitorHelper;
    //private Intent startIntent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_info);
        init();
        //startService();

    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
//        unbindService(connection);
//        stopService(startIntent);
        monitorHelper.unbindservice(LocalInfoActivity.this);
        Log.e("Test", "LocalInfoActivity onDestroy");
        super.onDestroy();
    }
    private void init(){
        myStmsiTextView = (TextView) findViewById(R.id.local_info_my_stmsi);
        myErfcnTextView = (TextView) findViewById(R.id.local_info_my_earfcn);
        myPciTextView = (TextView) findViewById(R.id.local_info_my_pci);
        myTaiTextView = (TextView) findViewById(R.id.local_info_my_tai);
        myRsrpTextView = (TextView) findViewById(R.id.local_info_my_rsrp);
        mySinrTextView = (TextView) findViewById(R.id.local_info_my_sinr);
        IntentFilter filter = new IntentFilter(MonitorApplication.BROAD_TO_LOCAL_INFO_ACTIVITY);
        filter.addAction(MonitorApplication.BROAD_TO_MAIN_ACTIVITY);
        registerReceiver(receiver, filter);
        monitorHelper = new MonitorHelper();
        monitorHelper.bindService(LocalInfoActivity.this);
    }
    private MonitorService mBoundService;
//    private void startService() {
//        MonitorApplication.IMEI = getIMEI(this);
//        startIntent = new Intent(this, MonitorService.class);
//        startService(startIntent);
//        //Intent intent=new Intent(this,MonitorService.class);
//        bindService(startIntent, connection, 0);
//    }
//    private ServiceConnection connection = new ServiceConnection() {
//
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//
//            mBoundService = null;
//        }
//
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            mBoundService = ((MonitorService.LocalBinder) service).getService();
//            MonitorApplication.MonitorService = mBoundService;
//        }
//    };
//    public static String getIMEI(Context context) {
//        TelephonyManager telephonyManager = (TelephonyManager) context
//                .getSystemService(Context.TELEPHONY_SERVICE);
//        String imei = telephonyManager.getDeviceId();
//        return imei != null ? imei : "";
//    }

    private MyBroadcastReceiver receiver = new MyBroadcastReceiver();

    class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals("")) {
                return;
            }
            refreshView(intent);
        }
    }

    private void refreshView(Intent intent) {

        int flag = intent.getFlags();
        Bundle bundle = intent.getExtras();
        switch (flag) {
            case MonitorApplication.STMSI:
                String stmsi = bundle.getString("msg");
                myStmsiTextView.setText(stmsi);
                break;
            case MonitorApplication.SERVER_CELL_FLAG:
                LTEServCellMessage mServCellMessage = bundle.getParcelable("msg");
                myErfcnTextView.setText(String.valueOf(mServCellMessage.getEARFCN()));
                myPciTextView.setText(String.valueOf(mServCellMessage.getPCI()));
                myTaiTextView.setText(String.valueOf(mServCellMessage.getTAC()));
                break;
            case MonitorApplication.POWER_INFO_FLAG:
                LTEPwrInfoMessage mPwrInfoMessage = bundle.getParcelable("msg");

                myRsrpTextView.setText(String.format("%.2f",mPwrInfoMessage.getRSRP()));
                mySinrTextView.setText(String.format("%.2f",mPwrInfoMessage.getSINR()));
                break;
            default:
                break;
        }
    }

}
