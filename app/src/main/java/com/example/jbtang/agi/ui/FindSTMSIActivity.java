package com.example.jbtang.agi.ui;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.example.jbtang.agi.R;
import com.example.jbtang.agi.core.Global;
import com.example.jbtang.agi.core.Status;
import com.example.jbtang.agi.device.DeviceManager;
import com.example.jbtang.agi.external.MonitorApplication;
import com.example.jbtang.agi.external.MonitorHelper;
import com.example.jbtang.agi.service.FindSTMSI;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.fmaster.LTEServCellMessage;

/**
 * Created by jbtang on 11/1/2015.
 */
public class FindSTMSIActivity extends AppCompatActivity {
    private static final int STMSICountMaxValuePerMinute = 200;
    private boolean startToFind;

    private List<FindSTMSI.CountSortedInfo> countSortedInfoList;

    private Button startButton;
    private Button stopButton;
    private TextView targetPhone;
    private ListView count;
    private TextView currentPCi;
    private EditText targetSTMSI;
    private myHandler handler;
    private TextView deviceStatusColor;
    private TextView cellConfirmColor;
    private TextView cellRsrp;
    private TextView pciNum;
    private MonitorHelper monitorHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        setContentView(R.layout.activity_find_stmsi);

        init();

    }
    @Override
    protected void onDestroy() {
        if(startToFind) {
            FindSTMSI.getInstance().stop();
            startToFind = false;
        }
        unregisterReceiver(receiver);
        monitorHelper.unbindservice(FindSTMSIActivity.this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_find_stmsi, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.menu_find_stmsi_save) {
            saveToNext();
        }

        return super.onOptionsItemSelected(item);
    }


    private void init() {
        countSortedInfoList = new ArrayList<>();
        startToFind = false;

        startButton = (Button) findViewById(R.id.find_stmsi_start_button);
        stopButton = (Button) findViewById(R.id.find_stmsi_stop_button);
        targetPhone = (TextView) findViewById(R.id.find_stmsi_target_phone_num);
        currentPCi = (TextView) findViewById(R.id.find_stmsi_current_pci);
        targetSTMSI = (EditText) findViewById(R.id.find_stmsi_target_stmsi);
        deviceStatusColor = (TextView) findViewById(R.id.find_stmsi_device_background);
        cellConfirmColor = (TextView)findViewById(R.id.find_stmsi_confirm_background);
        cellRsrp = (TextView)findViewById(R.id.find_stmsi_rsrp);
        pciNum = (TextView)findViewById(R.id.find_stmsi_pci_num);

        targetPhone.setText(Global.Configuration.targetPhoneNum);

        count = (ListView) findViewById(R.id.find_stmsi_count_listView);
        CountAdapter countAdapter = new CountAdapter(this);
        count.setAdapter(countAdapter);

        count.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                targetSTMSI.setText(countSortedInfoList.get(position).stmsi);
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(startToFind)
                    return;
                if (DeviceManager.getInstance().getDevices().size() == 0)
                    return;
                Global.sendTime = new Date();
                FindSTMSI.getInstance().start(FindSTMSIActivity.this);
                startToFind = true;
                startButton.setEnabled(false);
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FindSTMSI.getInstance().stop();
                startToFind = false;
                startButton.setEnabled(true);
            }
        });

        handler = new myHandler(this);
        Global.ThreadPool.scheduledThreadPool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (startToFind) {
                    handler.sendMessage(new Message());
                }
            }
        }, 1, 3, TimeUnit.SECONDS);
        MyRunable myRunable = new MyRunable();
        Global.ThreadPool.scheduledThreadPool.scheduleAtFixedRate(myRunable, 60, 60, TimeUnit.SECONDS);

        IntentFilter filter = new IntentFilter(MonitorApplication.BROAD_TO_MAIN_ACTIVITY);
        filter.addAction(MonitorApplication.BROAD_FROM_MAIN_MENU_DEVICE);
        registerReceiver(receiver, filter);
        monitorHelper = new MonitorHelper();
        monitorHelper.bindService(FindSTMSIActivity.this);
    }

    private void refresh() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                countSortedInfoList.clear();
                countSortedInfoList.addAll(FindSTMSI.getInstance().getCountSortedInfoList());
                ((CountAdapter) count.getAdapter()).notifyDataSetChanged();
                if(DeviceManager.getInstance().getDevices().size() == 0)
                    return;
                if (DeviceManager.getInstance().getDevices().get(0).getWorkingStatus() == Status.DeviceWorkingStatus.NORMAL) {
                    String rsrp = String.format("%.2f",DeviceManager.getInstance().getDevices().get(0).getCellInfo().rsrp);
                    cellConfirmColor.setBackgroundColor(Color.GREEN);
                    cellRsrp.setText(rsrp);
                } else {
                    cellConfirmColor.setBackgroundColor(Color.RED);
                    cellRsrp.setText("N/A");
                }
                pciNum.setText(String.valueOf(DeviceManager.getInstance().getDevices().get(0).getCellInfo().pci));
            }
        });
    }
    static class myHandler extends Handler {
        private final WeakReference<FindSTMSIActivity> mOuter;

        public myHandler(FindSTMSIActivity activity) {
            mOuter = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {

            mOuter.get().refresh();
        }

    }

    /**
     * for count ListView
     */
    private final class CountViewHolder {
        public TextView stmsi;
        public TextView count;
        public TextView time;
        public TextView pci;
        public TextView earfcn;
    }

    private class CountAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        public CountAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return countSortedInfoList.size();
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
            final CountViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.find_stmsi_count_list_item, null);
                holder = new CountViewHolder();
                holder.stmsi = (TextView) convertView.findViewById(R.id.find_stmsi_count_list_item_stmsi);
                holder.count = (TextView) convertView.findViewById(R.id.find_stmsi_count_list_item_count);
                holder.time = (TextView) convertView.findViewById(R.id.find_stmsi_count_list_item_time);
                holder.pci = (TextView) convertView.findViewById(R.id.find_stmsi_count_list_item_pci);
                holder.earfcn = (TextView) convertView.findViewById(R.id.find_stmsi_count_list_item_earfcn);
                convertView.setTag(holder);
            } else {
                holder = (CountViewHolder) convertView.getTag();
            }

            holder.stmsi.setText(countSortedInfoList.get(position).stmsi);
            holder.count.setText(countSortedInfoList.get(position).count);
            holder.time.setText(countSortedInfoList.get(position).time);
            holder.pci.setText(countSortedInfoList.get(position).pci);
            holder.earfcn.setText(countSortedInfoList.get(position).earfcn);
            return convertView;
        }
    }

    private final MyBroadcastReceiver receiver = new MyBroadcastReceiver();
    class MyBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("")){
                return;
            }
            if(intent.getAction().equals(MonitorApplication.BROAD_TO_MAIN_ACTIVITY)){
                refreshServerCell(intent);
            }
            else if(intent.getAction().equals(MonitorApplication.BROAD_FROM_MAIN_MENU_DEVICE)){
                refreshDeviceStatus(intent);
            }
        }
    }
    private void refreshServerCell(Intent intent){
        int flag = intent.getFlags();
        Bundle bundle = intent.getExtras();
        switch (flag){
            case MonitorApplication.SERVER_CELL_FLAG:
                LTEServCellMessage myServCellMessage = bundle.getParcelable("msg");
                if(myServCellMessage != null){
                    currentPCi.setText(String.valueOf(myServCellMessage.getPCI()));
                }
                break;
            default:
                break;
        }
    }
    private void refreshDeviceStatus(Intent intent){
        int color = intent.getIntExtra("colorOne", Color.RED);
        deviceStatusColor.setBackgroundColor(color);
    }
    /**
     * for count ListView
     */

    private void saveToNext() {
        String stmsi = targetSTMSI.getText().toString();
        if (validateSTMSI(stmsi)) {
            Intent intent = new Intent(this, MainMenuActivity.class);
            Global.TARGET_STMSI = stmsi;
            intent.putExtra(Global.TARGET_STMSI, stmsi);
            startActivity(intent);
        }
    }

    private boolean validateSTMSI(String stmsi) {
        String regex = "[a-zA-Z\\d]{10}$";
        if (!stmsi.matches(regex)) {
            new AlertDialog.Builder(this)
                    .setTitle("非法STMSI")
                    .setMessage("STMSI需为10位字母数字组合!")
                    .setPositiveButton("确定", null)
                    .show();
            return false;
        }
        return true;
    }
    class MyRunable implements Runnable {
        @Override
        public void run() {
            if(startToFind) {
                if (FindSTMSI.getInstance().stmsiCount < STMSICountMaxValuePerMinute) {
                    FindSTMSI.getInstance().stmsiCount = 0;
                } else {
                    FindSTMSI.getInstance().stop();
                    startToFind = false;
                    new AlertDialog.Builder(FindSTMSIActivity.this)
                            .setTitle("注意")
                            .setMessage("该处STMSI过多，设备已停止!")
                            .setPositiveButton("确定", null)
                            .show();
                }
            }
        }
    }

}
