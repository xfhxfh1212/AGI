package com.example.jbtang.agi.ui;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.graphics.Color;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jbtang.agi.R;
import com.example.jbtang.agi.core.Global;
import com.example.jbtang.agi.core.Status;
import com.example.jbtang.agi.dao.configuration.ConfigurationDAO;
import com.example.jbtang.agi.dao.configuration.ConfigurationDBManager;
import com.example.jbtang.agi.dao.devices.DeviceDAO;
import com.example.jbtang.agi.dao.devices.DeviceDBManager;
import com.example.jbtang.agi.device.DeviceManager;
import com.example.jbtang.agi.device.MonitorDevice;
import com.example.jbtang.agi.external.MonitorApplication;
import com.example.jbtang.agi.external.MonitorHelper;
import com.example.jbtang.agi.external.service.MonitorService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Created by xiang on 2016/1/11.
 */
public class MainMenuActivity extends AppCompatActivity {
    private static final int DEFAULT_TRIGGER_INTERVAL_SMS_MAX = 30;
    private static final int DEFAULT_SMS_FILTER_INTERVAL_MAX = 10;
    private static final int DEFAULT_SILENCECHECKTIME = 80;
    private static final int DEFAULT_RECEIVINGANTENNANUM = 2;
    private static final int DEFAULT_TOTAL_TRIGGER_COUNT = 30;
    private static final String SAVE_SUCCEED = "保存成功";
    private static final String WRONG_PHONE_NUM = "号码输入有误";
    private static final Pattern PHONE_NUMBER = Pattern.compile("^((13[0-9])|(15[^4,\\D])|(18[0-9]))\\d{8}$");
    private GridView gridView;
    private ArrayList<HashMap<String,Object>> itemList;
    private SimpleAdapter simpleAdapter;
    private String texts[];
    private int images[];
    private long exitTime = 0;
    private EditText phoneNum;
    private Button saveBtn;
    private Button rebootBtn;
    private Button connectBtn;
    private Button disconnectBtn;
    private TextView deviceStatusText;
    private TextView deviceColorOne;
    private TextView deviceColorTwo;
    private TextView deviceColorThree;
    private TextView deviceColorFour;
    private DeviceDBManager dmgr;
    private ConfigurationDBManager cmgr;
    private ConfigurationDAO dao;
    private myBroadcastReceiver broadcastReceiver;
    private MonitorHelper monitorHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        setContentView(R.layout.activity_main_menu);
        init();
    }
    public void init(){
        images = new int[]{R.drawable.cell_monitor,
                R.drawable.find_stmsi,
                R.drawable.orientation,
                R.drawable.interference,
                R.drawable.configuration,
                R.drawable.local_info};
        texts = new String[]{this.getString(R.string.title_main_menu_cell_monitor),
                this.getString(R.string.title_main_menu_find_STMSI),
                this.getString(R.string.title_main_menu_orientation),
                this.getString(R.string.title_main_menu_interference_detection),
                this.getString(R.string.title_main_menu_configuration),
                this.getString(R.string.title_main_menu_cellPhone_info)};
        itemList = new ArrayList<HashMap<String, Object>>();
        for(int i = 0; i < 6; i++){
            HashMap<String,Object> map = new HashMap<String,Object>();
            map.put("itemImage",images[i]);
            map.put("itemText",texts[i]);
            itemList.add(map);
        }
        simpleAdapter = new SimpleAdapter(this,
                itemList,
                R.layout.main_menu_item,
                new String[]{"itemImage","itemText"},
                new int[]{R.id.main_menu_item_image,R.id.main_menu_item_name});
        gridView = (GridView) findViewById(R.id.main_menu_gridView);
        gridView.setAdapter(simpleAdapter);
        gridView.setOnItemClickListener(new ItemClickListener());

        phoneNum = (EditText) findViewById(R.id.main_menu_target_phone_num);
        saveBtn = (Button) findViewById(R.id.main_menu_target_phone_num_save);

        rebootBtn = (Button) findViewById(R.id.main_menu_device_reboot);
        connectBtn = (Button) findViewById(R.id.main_menu_device_connnect);
        disconnectBtn = (Button) findViewById(R.id.main_menu_device_disconnect);
        rebootBtn.setOnClickListener(new rebootBtnOnClickListener());
        connectBtn.setOnClickListener(new conBtnOnClickListener());
        disconnectBtn.setOnClickListener(new disconBtnOnClickListener());

        deviceColorOne = (TextView) findViewById(R.id.main_menu_device_background_one);
        deviceColorTwo = (TextView) findViewById(R.id.main_menu_device_background_two);
        deviceColorThree = (TextView) findViewById(R.id.main_menu_device_background_three);
        deviceColorFour = (TextView) findViewById(R.id.main_menu_device_background_four);
        deviceStatusText = (TextView) findViewById(R.id.main_menu_device_status_text);

        dmgr = new DeviceDBManager(this);
        getDevices();

        cmgr = new ConfigurationDBManager(this);
        dao = cmgr.getConfiguration(Global.UserInfo.user_name);
        saveToCache();
        saveToDAO();
        phoneNum.setText(Global.Configuration.targetPhoneNum);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PHONE_NUMBER.matcher(phoneNum.getText().toString()).matches()) {
                    Global.Configuration.targetPhoneNum = phoneNum.getText().toString();
                    saveToDAO();
                    Toast.makeText(getApplicationContext(), SAVE_SUCCEED, Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getApplicationContext(), WRONG_PHONE_NUM, Toast.LENGTH_SHORT).show();
                }
            }
        });
        try {
            broadcastReceiver = new myBroadcastReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(MonitorApplication.BROAD_FROM_MAIN_MENU_DEVICE);
            filter.addAction(MonitorApplication.BROAD_FROM_CONFIGURATION_ACTIVITY);
            registerReceiver(broadcastReceiver, filter);
        }catch (Exception e){
            e.printStackTrace();
        }
        Global.ThreadPool.scheduledThreadPool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                refreshDeviceStatusBar();
            }
        }, 1, 1, TimeUnit.SECONDS);
        monitorHelper = new MonitorHelper();
        monitorHelper.startService(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    private static final String TAG = "DeviceConfiguration";
    private static final String IP_SPLITTER = ".";

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void exit() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Toast.makeText(this, "再按一次退出程序",
                    Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            dmgr.closeDB();
            cmgr.closeDB();
            finish();
            System.exit(0);
        }
    }
    @Override
    public void onDestroy() {
        monitorHelper.stopService(this);
        for(MonitorDevice device:DeviceManager.getInstance().getAllDevices()){
            try {
                device.disconnect();
                DeviceManager.getInstance().remove(device.getName());
            } catch (Exception e) {
            }
        }
        super.onDestroy();
    }
    @Override
    protected void onResume() {
        for(MonitorDevice device:DeviceManager.getInstance().getAllDevices()) {
            if (device.isConnected())
                DeviceManager.getInstance().add(device);
        }
        Log.e("test", "onResume");
        super.onResume();
    }
//    @Override
//    public void onPause() {
//        Log.e("Test", "MainMenuActivity onPause");
//        super.onPause();
//    }
//
//    @Override
//    public void onStop() {
//        Log.e("Test", "MainMenuActivity onStop");
//        super.onStop();
//    }

    class ItemClickListener implements AdapterView.OnItemClickListener{

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            HashMap<String,Object> item = (HashMap<String, Object>) parent.getItemAtPosition(position);
            String itemText = (String) item.get("itemText");
            Object object = item.get("itemImage");
            //Toast.makeText(MainMenuActivity.this,itemText,Toast.LENGTH_LONG).show();

            switch (images[position]){
                case R.drawable.cell_monitor:
                    startActivity(new Intent(MainMenuActivity.this, CellMonitorActivity.class));
                    break;
                case R.drawable.find_stmsi:
                    startActivity(new Intent(MainMenuActivity.this, FindSTMSIActivity.class));
                    break;
                case R.drawable.orientation:
                    startActivity(new Intent(MainMenuActivity.this, OrientationFindingActivity.class));
                    break;
                case R.drawable.interference:
                    startActivity(new Intent(MainMenuActivity.this,InterferenceActivity.class));
                    break;
                case R.drawable.configuration:
                    startActivity(new Intent(MainMenuActivity.this, ConfigurationActivity.class));
                    break;
                case R.drawable.local_info:
                    startActivity(new Intent(MainMenuActivity.this, LocalInfoActivity.class));
                    break;
            }
        }
    }
    class rebootBtnOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            new AlertDialog.Builder(MainMenuActivity.this)
                    .setTitle(R.string.title_main_menu_device_reboot_confirm)
                    .setPositiveButton(R.string.page_main_menu_device_reboot_confirm_cancel,null)
                    .setNegativeButton(R.string.page_main_menu_device_reboot_confrim_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            rebootConfirm();
                        }
                    }).show();

        }
    }
    private void rebootConfirm() {
        for(MonitorDevice device : DeviceManager.getInstance().getDevices()) {
            device.reboot();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainMenuActivity.this, "设备重启中!", Toast.LENGTH_SHORT).show();
            }
        });
    }
    class conBtnOnClickListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            for(MonitorDevice device:DeviceManager.getInstance().getAllDevices()){
                try {
                    device.connect();
                    //Thread.sleep(1000);
                    if(device.isConnected()) {
                        DeviceManager.getInstance().add(device);
                        Log.e("Test","连接设备：" + device.getName());
                    }
                } catch (Exception e) {
                }
            }
            Log.e("Test","连接设备数："+String.valueOf(DeviceManager.getInstance().getDevices().size()));
        }
    }
    class disconBtnOnClickListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            for(MonitorDevice device:DeviceManager.getInstance().getAllDevices()){
                try {
                    device.disconnect();
                    DeviceManager.getInstance().remove(device.getName());
                } catch (Exception e) {
                }
            }
            Log.e("Test","连接设备数："+String.valueOf(DeviceManager.getInstance().getDevices().size()));
        }
    }

    private void getDevices() {
        List<DeviceDAO> deviceDAOs = dmgr.listDB();

        for (DeviceDAO dao : deviceDAOs) {
            MonitorDevice device = new MonitorDevice(dao.name, dao.ip, dao.type);
            if(DeviceManager.getInstance().getFromAll(device.getName()) == null) {
                DeviceManager.getInstance().addToAll(device);
            }
            Log.e(TAG, "deviceAdd" + dao.ip);
        }
    }

     private class myBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            if(intent.getAction().equals(MonitorApplication.BROAD_FROM_MAIN_MENU_DEVICE)) {
                Bundle bundle = intent.getExtras();
                final int colorOne = bundle.getInt("colorOne");
                final int colorTwo = bundle.getInt("colorTwo");
                final int colorThree = bundle.getInt("colorThree");
                final int colorFour = bundle.getInt("colorFour");
                final String statusText = bundle.getString("statusText");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        deviceColorOne.setBackgroundColor(colorOne);
                        deviceColorTwo.setBackgroundColor(colorTwo);
                        deviceColorThree.setBackgroundColor(colorThree);
                        deviceColorFour.setBackgroundColor(colorFour);
                        deviceStatusText.setText(statusText);
                    }
                });
            }
            else if(intent.getAction().equals(MonitorApplication.BROAD_FROM_CONFIGURATION_ACTIVITY)){
                String flag = intent.getStringExtra("flag");
                switch (flag)
                {
                    case ConfigurationActivity.ADD_DEVICE_FLAG: {
                        String name = intent.getStringExtra("name");
                        String ip = intent.getStringExtra("ip");
                        Status.BoardType type = (Status.BoardType) intent.getSerializableExtra("type");
                        DeviceManager.getInstance().addToAll(new MonitorDevice(name, ip, type));
                        Log.e(TAG,"AllDevices Size" + DeviceManager.getInstance().getAllDevices().size());
                        break;
                    }
                    case ConfigurationActivity.DELETE_DEVICE_FLAG: {
                        String name = intent.getStringExtra("name");
                        MonitorDevice device = DeviceManager.getInstance().getDevice(name);
                        if (device != null) {
                            device.release();
                            DeviceManager.getInstance().remove(name);
                        }
                        DeviceManager.getInstance().removeFromAll(name);
                        Log.e(TAG,"AllDevices Size" + DeviceManager.getInstance().getAllDevices().size());
                        break;
                    }
                    case ConfigurationActivity.REBOOT_DEVICE_FLAG: {
                        String name = intent.getStringExtra("name");
                        MonitorDevice device = DeviceManager.getInstance().getDevice(name);
                        if (device != null) {
                            device.reboot();
                            //DeviceManager.getInstance().remove(name);
                            Toast.makeText(MainMenuActivity.this, "设备重启中!", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
        }
    }

    private void refreshDeviceStatusBar(){
        int colorOne = getResources().getColor(R.color.default_color);
        int colorTwo = getResources().getColor(R.color.default_color);
        int colorThree = getResources().getColor(R.color.default_color);
        int colorFour = getResources().getColor(R.color.default_color);
        String statusText = "未就绪";
        int i = 0;
        for(MonitorDevice device:DeviceManager.getInstance().getAllDevices()) {
            i++;
            if(device.isConnected()){
                if(i == 1)
                    colorOne = Color.GREEN;
                else if(i == 2)
                    colorTwo = Color.GREEN;
                else if (i ==3 )
                    colorThree = Color.GREEN;
                else if(i == 4)
                    colorFour = Color.GREEN;
                statusText = "已连接";

            }
            else {
                if (device.getPingStatus() == Status.PingResult.SUCCEED) {
                    if (i == 1)
                        colorOne = Color.YELLOW;
                    else if (i == 2)
                        colorTwo = Color.YELLOW;
                    else if (i ==3 )
                        colorThree = Color.YELLOW;
                    else if(i == 4)
                        colorFour = Color.YELLOW;
                    if (statusText != "已连接")
                        statusText = "已就绪";
                } else {
                    if (i == 1)
                        colorOne = Color.RED;
                    else if (i == 2)
                        colorTwo = Color.RED;
                    else if (i ==3 )
                        colorThree = Color.RED;
                    else if(i == 4)
                        colorFour = Color.RED;
                }
                try {
                    //device.connect();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            //Log.d("changeDevice","devicename:"+device.getName()+" hashcode:"+device.hashCode());
        }
        if(DeviceManager.getInstance().getAllDevices().size() == 0){
            statusText = "无可用设备";
        }
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putInt("colorOne",colorOne);
        bundle.putInt("colorTwo",colorTwo);
        bundle.putInt("colorThree",colorThree);
        bundle.putInt("colorFour",colorFour);
        bundle.putString("statusText",statusText);
        intent.putExtras(bundle);
        intent.setAction(MonitorApplication.BROAD_FROM_MAIN_MENU_DEVICE);
        sendBroadcast(intent);
    }
    private void saveToCache() {
        Global.Configuration.name = Global.UserInfo.user_name;
        Global.Configuration.type = (dao == null ? Status.TriggerType.SMS : dao.type);
        Global.Configuration.smsType = (dao == null ? Status.TriggerSMSType.INSIDE : dao.smsType);
        Global.Configuration.insideSMSType = (dao == null ? Status.InsideSMSType.NORMAL : dao.insideSMSType);
        Global.Configuration.silentSMSType = (dao == null ? Status.SilentSMSType.TYPE_ONE : dao.silentSMSType);
        Global.Configuration.triggerInterval = (dao == null ? DEFAULT_TRIGGER_INTERVAL_SMS_MAX : dao.triggerInterval);
        Global.Configuration.filterInterval = (dao == null ? DEFAULT_SMS_FILTER_INTERVAL_MAX : dao.filterInterval);
        Global.Configuration.silenceCheckTimer = (dao == null ?DEFAULT_SILENCECHECKTIME : dao.silenceCheckTimer);
        Global.Configuration.receivingAntennaNum = (dao == null ? DEFAULT_RECEIVINGANTENNANUM : dao.receivingAntennaNum);
        Global.Configuration.triggerTotalCount = (dao == null ? DEFAULT_TOTAL_TRIGGER_COUNT : dao.totalTriggerCount);
        Global.Configuration.targetPhoneNum = (dao == null ? "" : dao.targetPhoneNum);
        Global.Configuration.smsCenter = (dao == null ? "" : dao.smsCenter);

    }

    private void saveToDAO() {
        ConfigurationDAO dao = new ConfigurationDAO(Global.Configuration.name, Global.Configuration.type,Global.Configuration.smsType ,
                Global.Configuration.insideSMSType, Global.Configuration.silentSMSType, Global.Configuration.triggerInterval,
                Global.Configuration.filterInterval, Global.Configuration.silenceCheckTimer, Global.Configuration.receivingAntennaNum,
                Global.Configuration.triggerTotalCount, Global.Configuration.targetPhoneNum,Global.Configuration.smsCenter);
        cmgr.insertOrUpdate(dao);
    }
}

