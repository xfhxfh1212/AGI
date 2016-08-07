package com.example.jbtang.agi.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jbtang.agi.R;
import com.example.jbtang.agi.core.Global;
import com.example.jbtang.agi.core.Status;
import com.example.jbtang.agi.dao.configuration.ConfigurationDAO;
import com.example.jbtang.agi.dao.configuration.ConfigurationDBManager;
import com.example.jbtang.agi.dao.devices.DeviceDAO;
import com.example.jbtang.agi.dao.devices.DeviceDBManager;
import com.example.jbtang.agi.device.MonitorDevice;
import com.example.jbtang.agi.external.MonitorApplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by xiang on 2016/1/22.
 */
public class ConfigurationActivity extends AppCompatActivity {
    private static final String IP_SPLITTER = ".";

    private List<MonitorDevice> devices;
    private DeviceDBManager dmgr;

    private RadioButton triggerSMS;
    private RadioButton triggerPhone;
    private RadioButton SMSInside;
    private RadioButton SMSOutside;
    private RadioButton insideNormal;
    private RadioButton insideSilent;
    private RadioGroup silentSMSType;
    private EditText triggerInterval;
    private EditText filterInterval;
    private EditText silenceTimer;
    private EditText totalTriggerCount;
    private EditText SMSCenter;
    private ConfigurationDBManager cmgr;

    private static final int DEFAULT_TRIGGER_INTERVAL_SMS_MIN = 5;
    private static final int DEFAULT_TRIGGER_INTERVAL_SMS_MAX = 30;
    private static final int DEFAULT_TRIGGER_INTERVAL_PHONE_MIN = 2;
    private static final int DEFAULT_TRIGGER_INTERVAL_PHONE_MAX = 10;
    private static final int DEFAULT_SMS_FILTER_INTERVAL_MIN = 5;
    private static final int DEFAULT_SMS_FILTER_INTERVAL_MAX = 15;
    private static final int DEFAULT_SILENCECHECKTIME = 80;
    private static final int DEFAULT_RECEIVINGANTENNANUM = 2;
    private static final int DEFAULT_TOTAL_TRIGGER_COUNT = 30;
    private static final Pattern PHONE_NUMBER = Pattern.compile("^((13[0-9])|(15[^4,\\D])|(18[0-9]))\\d{8}$");
    public static final String ADD_DEVICE_FLAG = "addDeviceFlag";
    public static final String DELETE_DEVICE_FLAG = "deleteDeviceFlag";
    public static final String CHANGE_DEVICE_FLAG = "changeDeviceFlag";
    public static final String REBOOT_DEVICE_FLAG = "rebootDeivceFlag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_configuration);

        initDefaultValue();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_device_configuration, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.menu_device_configuration_add_device:
                showAddDeviceDialog();
                return true;
            case R.id.menu_device_configuration_save:
                saveToNextStep();
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        for(MonitorDevice device : devices){
            device.release();
        }
        super.onDestroy();
        cmgr.closeDB();
        dmgr.closeDB();
    }

    /**
     * for ListView
     */
    public class MyAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        public MyAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return devices.size();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return 0;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.device_configuration_list_item, null);
                holder = new ViewHolder();
                holder.title = (TextView) convertView.findViewById(R.id.device_configuration_item_title);
                holder.detailBtn = (Button) convertView.findViewById(R.id.device_configuration_detail_btn);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            String temDevice = devices.get(position).getName();

            Log.d("device","device: "+temDevice+" position: " + position);
            holder.title.setText(temDevice);
            MyListener detailListener;
            detailListener = new MyListener(position);
            holder.detailBtn.setTag(position);
            holder.detailBtn.setOnClickListener(detailListener);

            return convertView;
        }
    }


    private class MyListener implements View.OnClickListener {
        int mPosition;

        public MyListener(int inPosition) {
            this.mPosition = inPosition;
        }

        @Override
        public void onClick(View v) {
                    Toast.makeText(ConfigurationActivity.this, "Detail", Toast.LENGTH_SHORT).show();
                    showDetailInfo(mPosition);

        }
    }

    private void showDetailInfo(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        final View view = inflater.inflate(R.layout.device_configuration_show_detail, null);
        builder.setTitle(R.string.title_device_configuration_show_detail)
                .setView(view)
                .setPositiveButton(R.string.page_device_configure_show_detail_ok, null)
                .setNeutralButton(R.string.page_device_configure_show_detail_reboot, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        confirmReboot(position);
                    }
                })
                .setNegativeButton(R.string.page_device_configure_show_detail_delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        confirmDelete(position);
                    }
                })

                .show();
        initialShowDetailInfoDialog(view, devices.get(position));
    }

    private void initialShowDetailInfoDialog(View view, MonitorDevice device) {
        TextView name = (TextView) view.findViewById(R.id.device_configuration_show_name);
        TextView ip = (TextView) view.findViewById(R.id.device_configuration_show_ip);
        TextView dataPort = (TextView) view.findViewById(R.id.device_configuration_show_dataPort);
        TextView messagePort = (TextView) view.findViewById(R.id.device_configuration_show_messagePort);
        TextView type = (TextView) view.findViewById(R.id.device_configuration_show_type);
        name.setText("设备名称 : " + device.getName());
        ip.setText("IP地址 : " + device.getIP());
        dataPort.setText("数据端口 : " + String.valueOf(device.getDataPort()));
        messagePort.setText("消息端口 : " + String.valueOf(device.getMessagePort()));
        type.setText("板卡类型 : " + device.getType().name());
    }

    private void deleteDevice(int position) {
        dmgr.deleteByName(devices.get(position).getName());
        MonitorDevice device = devices.remove(position);
        device.release();
        ListView listView = (ListView) findViewById(R.id.device_configuration_listView);
        ((MyAdapter) listView.getAdapter()).notifyDataSetChanged();

        sendMyBroadcast(DELETE_DEVICE_FLAG,device);
    }

    private void confirmDelete(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_device_configuration_confirm_delete)
                .setNegativeButton(R.string.page_device_configure_confirm_delete_cancel, null)
                .setPositiveButton(R.string.page_device_configure_confirm_delete_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        deleteDevice(position);
                    }
                })
                .show();
    }
    private void rebootDevice(int position) {
        MonitorDevice device = devices.get(position);
        sendMyBroadcast(REBOOT_DEVICE_FLAG, device);
    }
    private void confirmReboot(final int position) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_device_congituration_confirm_reboot)
                .setNegativeButton(R.string.page_device_configure_confirm_reboot_cancel,null)
                .setPositiveButton(R.string.page_device_configure_confirm_reboot_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        rebootDevice(position);
                    }
                })
                .show();
    }

    public final class ViewHolder {
        public TextView title;
        public Button detailBtn;
    }



    private void showAddDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        final View view = inflater.inflate(R.layout.device_configuration_add_device, null);
        final EditText ip = (EditText) view.findViewById(R.id.device_configure_ip);
        final RadioButton fdd = (RadioButton) view.findViewById(R.id.device_configure_hint_board_type_fdd);

        builder.setTitle(R.string.menu_device_configuration).setIcon(android.R.drawable.ic_dialog_info)
                .setView(view)
                .setNegativeButton(R.string.menu_device_configuration_dialog_cancel, null)
                .setPositiveButton(R.string.menu_device_configuration_dialog_add, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        addDevice(ip, fdd);
                    }
                })
                .show();
    }

    private void addDevice(EditText ip, RadioButton fdd) {
        if (validateInput(ip.getText().toString())) {
            Status.BoardType type;
            if (fdd.isChecked()) {
                type = Status.BoardType.FDD;
            } else {
                type = Status.BoardType.TDD;
            }
            String iptext = ip.getText().toString();
            String name = MonitorDevice.DEVICE_NAME_PREFIX + iptext.substring(iptext.lastIndexOf(IP_SPLITTER) + 1);
            MonitorDevice device = new MonitorDevice(name, ip.getText().toString(), type);
            device.release();
            addDeviceToDB(device);
            addDeviceToListView(device);


        }
    }

    private void addDeviceToDB(MonitorDevice device) {
        DeviceDAO deviceDAO = new DeviceDAO(
                device.getName(),
                device.getIP(),
                device.getDataPort(),
                device.getMessagePort(),
                device.getType());
        dmgr.add(Collections.singletonList(deviceDAO));
    }

    private void addDeviceToListView(MonitorDevice device) {
        for(int i=0;i<devices.size();i++){
            if(devices.get(i).getName().equals(device.getName())){
                devices.set(i,device);
                ListView listView = (ListView) findViewById(R.id.device_configuration_listView);
                ((MyAdapter) listView.getAdapter()).notifyDataSetChanged();
                sendMyBroadcast(CHANGE_DEVICE_FLAG,device);
                return;
            }
        }
        devices.add(device);
        ListView listView = (ListView) findViewById(R.id.device_configuration_listView);
        ((MyAdapter) listView.getAdapter()).notifyDataSetChanged();
        sendMyBroadcast(ADD_DEVICE_FLAG,device);
    }

    private boolean validateInput(String ip) {
        try {
            validateIP(ip);
        } catch (IllegalArgumentException e) {
            new AlertDialog.Builder(this)
                    .setTitle("非法输入")
                    .setMessage(e.getMessage())
                    .setPositiveButton("确定", null)
                    .show();
            return false;
        }
        return true;
    }

    private void validateIP(String ip) throws IllegalArgumentException {
        String regex = "((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)$";
        if (!ip.matches(regex)) {
            throw new IllegalArgumentException("IP地址格式错误！");
        }
        if (dmgr.isExistsByIP(ip)) {
            throw new IllegalArgumentException("该IP地址已有板卡使用！");
        }
    }

    private void saveToNextStep() {
        if (validate()) {
            saveToCache();
            saveToDAO();
            Intent intent = new Intent(this, MainMenuActivity.class);
            startActivity(intent);
        }
    }


    private void initDefaultValue() {
        dmgr = new DeviceDBManager(this);
        devices = getDevices();
        ListView listView = (ListView) findViewById(R.id.device_configuration_listView);
        MyAdapter adapter = new MyAdapter(this);
        listView.setAdapter(adapter);
        triggerSMS = (RadioButton) findViewById(R.id.system_configure_trigger_sms);
        triggerPhone = (RadioButton) findViewById(R.id.system_configure_trigger_phone);
        SMSInside = (RadioButton) findViewById(R.id.system_configure_trigger_sms_inside);
        SMSOutside = (RadioButton) findViewById(R.id.system_configure_trigger_sms_outside);
        insideNormal = (RadioButton) findViewById(R.id.system_configure_trigger_sms_inside_normal);
        insideSilent = (RadioButton) findViewById(R.id.system_configure_trigger_sms_inside_silent);
        silentSMSType = (RadioGroup) findViewById(R.id.system_configure_trigger_sms_silent_type);
        triggerInterval = (EditText) findViewById(R.id.system_configure_trigger_interval);
        filterInterval = (EditText) findViewById(R.id.system_configure_filter_threshold);
        silenceTimer = (EditText) findViewById(R.id.system_configure_silence_timer);
        totalTriggerCount = (EditText) findViewById(R.id.system_configure_trigger_max);
        SMSCenter = (EditText) findViewById(R.id.system_configure_SMS_center);
        cmgr = new ConfigurationDBManager(this);

        final ConfigurationDAO dao = cmgr.getConfiguration(Global.UserInfo.user_name);

        triggerSMS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (dao != null) {
                        triggerInterval.setText(String.valueOf(dao.triggerInterval));
                    } else {
                        String text = String.format("%d~%d", DEFAULT_TRIGGER_INTERVAL_SMS_MIN, DEFAULT_TRIGGER_INTERVAL_SMS_MAX);
                        triggerInterval.setText("");
                        triggerInterval.setHint(text);
                    }
                }
            }
        });

        triggerPhone.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (dao != null) {
                        triggerInterval.setText(String.valueOf(dao.triggerInterval));
                    } else {
                        String text = String.format("%d~%d", DEFAULT_TRIGGER_INTERVAL_PHONE_MIN, DEFAULT_TRIGGER_INTERVAL_PHONE_MAX);
                        triggerInterval.setText("");
                        triggerInterval.setHint(text);
                    }
                }
            }
        });


        if (dao != null) {
            switch (dao.type) {
                case SMS:
                    triggerSMS.setChecked(true);
                    break;
                case PHONE:
                    triggerPhone.setChecked(true);
            }
            switch (dao.smsType) {
                case INSIDE:
                    SMSInside.setChecked(true);
                    break;
                case OUTSIDE:
                    SMSOutside.setChecked(true);
            }
            switch (dao.insideSMSType) {
                case NORMAL:
                    insideNormal.setChecked(true);
                    break;
                case SILENT:
                    insideSilent.setChecked(true);
            }
            switch (dao.silentSMSType) {
                case TYPE_ONE: silentSMSType.check(R.id.system_configure_trigger_sms_silent_type_one);break;
                case TYPE_TWO: silentSMSType.check(R.id.system_configure_trigger_sms_silent_type_two);break;
                case TYPE_THREE: silentSMSType.check(R.id.system_configure_trigger_sms_silent_type_three);break;
                case TYPE_FOUR: silentSMSType.check(R.id.system_configure_trigger_sms_silent_type_four);break;
            }
        }

        String text = String.format("%d~%d", DEFAULT_SMS_FILTER_INTERVAL_MIN, DEFAULT_SMS_FILTER_INTERVAL_MAX);
        if (dao == null) {
            filterInterval.setHint(text);
        } else {
            filterInterval.setText(String.valueOf(dao.filterInterval));
        }

        silenceTimer.setText(String.valueOf(dao == null ? DEFAULT_SILENCECHECKTIME : dao.silenceCheckTimer));
        totalTriggerCount.setText(String.valueOf(dao == null ? DEFAULT_TOTAL_TRIGGER_COUNT : dao.totalTriggerCount));
        SMSCenter.setText(dao == null ? "" : dao.smsCenter);
    }

    private boolean validate() {
        try {
            validateNull();
            validateTriggerInterval();
            validateFilterInterval();
        } catch (IllegalArgumentException e) {
            new AlertDialog.Builder(this)
                    .setTitle("非法输入")
                    .setMessage(e.getMessage())
                    .setPositiveButton("确定", null)
                    .show();
            return false;
        }
        return true;
    }

    private void validateTriggerInterval() throws IllegalArgumentException {
        Integer interval = Integer.parseInt(triggerInterval.getText().toString());
        if (triggerSMS.isChecked() && !(interval >= DEFAULT_TRIGGER_INTERVAL_SMS_MIN && interval <= DEFAULT_TRIGGER_INTERVAL_SMS_MAX)) {
            throw new IllegalArgumentException(String.format("触发间隔配置错误,需在%d~%d中(含)",
                    DEFAULT_TRIGGER_INTERVAL_SMS_MIN, DEFAULT_TRIGGER_INTERVAL_SMS_MAX));
        }
        if (triggerPhone.isChecked() && !(interval >= DEFAULT_TRIGGER_INTERVAL_PHONE_MIN && interval <= DEFAULT_TRIGGER_INTERVAL_PHONE_MAX)) {
            throw new IllegalArgumentException(String.format("触发间隔配置错误,需在%d~%d中(含)",
                    DEFAULT_TRIGGER_INTERVAL_PHONE_MIN, DEFAULT_TRIGGER_INTERVAL_PHONE_MAX));
        }
    }

    private void validateNull() throws IllegalArgumentException {
        if (triggerInterval.getText().toString().isEmpty()
                || !(triggerSMS.isChecked() || triggerPhone.isChecked())
                || !(SMSInside.isChecked() || SMSOutside.isChecked())
                || !((insideSilent.isChecked() && (silentSMSType.getCheckedRadioButtonId() != -1)) || insideNormal.isChecked())
                || filterInterval.getText().toString().isEmpty()
                || silenceTimer.getText().toString().isEmpty()
                || totalTriggerCount.getText().toString().isEmpty()) {
            throw new IllegalArgumentException("参数不可为空!");
        }
    }

    private void validateFilterInterval() throws IllegalArgumentException {
        Integer interval = Integer.parseInt(filterInterval.getText().toString());
        if (!(interval >= DEFAULT_SMS_FILTER_INTERVAL_MIN && interval <= DEFAULT_SMS_FILTER_INTERVAL_MAX)) {
            throw new IllegalArgumentException(String.format("过滤门限配置错误,需在%d~%d中(含)",
                    DEFAULT_SMS_FILTER_INTERVAL_MIN, DEFAULT_SMS_FILTER_INTERVAL_MAX));
        }
    }

    private List<MonitorDevice> getDevices() {
        List<MonitorDevice> devices = new ArrayList<>();
        List<DeviceDAO> deviceDAOs = dmgr.listDB();
        for (DeviceDAO dao : deviceDAOs) {
            devices.add(new MonitorDevice(dao.name, dao.ip, dao.type));
        }
        return devices;
    }
    private void saveToCache() {
        Global.Configuration.name = Global.UserInfo.user_name;
        Global.Configuration.type = triggerSMS.isChecked() ? Status.TriggerType.SMS : Status.TriggerType.PHONE;
        Global.Configuration.smsType = SMSInside.isChecked() ? Status.TriggerSMSType.INSIDE : Status.TriggerSMSType.OUTSIDE;
        Global.Configuration.insideSMSType = insideNormal.isChecked() ? Status.InsideSMSType.NORMAL : Status.InsideSMSType.SILENT;
        switch (silentSMSType.getCheckedRadioButtonId()){
            case R.id.system_configure_trigger_sms_silent_type_one : Global.Configuration.silentSMSType = Status.SilentSMSType.TYPE_ONE;break;
            case R.id.system_configure_trigger_sms_silent_type_two : Global.Configuration.silentSMSType = Status.SilentSMSType.TYPE_TWO;break;
            case R.id.system_configure_trigger_sms_silent_type_three : Global.Configuration.silentSMSType = Status.SilentSMSType.TYPE_THREE;break;
            case R.id.system_configure_trigger_sms_silent_type_four : Global.Configuration.silentSMSType = Status.SilentSMSType.TYPE_FOUR;break;
        }
        Global.Configuration.triggerInterval = Integer.parseInt(triggerInterval.getText().toString());
        Global.Configuration.filterInterval = Integer.parseInt(filterInterval.getText().toString());
        Global.Configuration.silenceCheckTimer = Integer.parseInt(silenceTimer.getText().toString());
        Global.Configuration.receivingAntennaNum = DEFAULT_RECEIVINGANTENNANUM;
        Global.Configuration.triggerTotalCount = Integer.parseInt(totalTriggerCount.getText().toString());
        Global.Configuration.smsCenter = SMSCenter.getText().toString();
    }

    private void saveToDAO() {
        ConfigurationDAO dao = new ConfigurationDAO(Global.Configuration.name, Global.Configuration.type,Global.Configuration.smsType ,
                Global.Configuration.insideSMSType, Global.Configuration.silentSMSType, Global.Configuration.triggerInterval,
                Global.Configuration.filterInterval, Global.Configuration.silenceCheckTimer, Global.Configuration.receivingAntennaNum,
                Global.Configuration.triggerTotalCount, Global.Configuration.targetPhoneNum,Global.Configuration.smsCenter);
        cmgr.insertOrUpdate(dao);
    }
    private void sendMyBroadcast(String flag,MonitorDevice device){
        Intent intent = new Intent();  //Itent就是我们要发送的内容
        intent.putExtra("flag", flag);
        if (flag.equals(ADD_DEVICE_FLAG)) {
            intent.putExtra("name", device.getName());
            intent.putExtra("ip", device.getIP());
            intent.putExtra("type", device.getType());
        } else if (flag.equals(DELETE_DEVICE_FLAG)) {
            intent.putExtra("name", device.getName());
            Log.d("changeDevice", "send deleteDevice,deviceName: " + device.getName());
        } else if(flag.equals(CHANGE_DEVICE_FLAG)){
            intent.putExtra("name", device.getName());
            intent.putExtra("ip", device.getIP());
            intent.putExtra("type", device.getType());
        }
        else if (flag.equals(REBOOT_DEVICE_FLAG)) {
            intent.putExtra("name", device.getName());
        }
        intent.setAction(MonitorApplication.BROAD_FROM_CONFIGURATION_ACTIVITY);   //设置你这个广播的action，只有和这个action一样的接受者才能接受者才能接收广播
        sendBroadcast(intent);   //发送广播
    }
}
