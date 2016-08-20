package com.example.jbtang.agi.ui;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jbtang.agi.R;
import com.example.jbtang.agi.core.Global;
import com.example.jbtang.agi.core.Status;
import com.example.jbtang.agi.dao.FindSTMSIInfos.FindeSTMSIInfoDAO;
import com.example.jbtang.agi.dao.FindSTMSIInfos.FindSTMSIInfoManager;
import com.example.jbtang.agi.device.DeviceManager;
import com.example.jbtang.agi.device.MonitorDevice;
import com.example.jbtang.agi.external.MonitorApplication;
import com.example.jbtang.agi.external.MonitorHelper;
import com.example.jbtang.agi.service.FindSTMSI;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import io.fmaster.LTEServCellMessage;

/**
 * Created by jbtang on 11/1/2015.
 */
public class FindSTMSIActivity extends AppCompatActivity {
    private static final int STMSICountMaxValuePerMinute = 400;
    private boolean startToFind;

    private List<FindSTMSI.CountSortedInfo> countSortedInfoList;

    private Button startButton;
    private Button stopButton;
    private TextView targetPhone;
    private ListView count;
    private TextView currentPCi;
    private EditText targetSTMSI;
    private myHandler handler;
    private TextView cellConfirmColorOne;
    private TextView cellConfirmColorTwo;
    private TextView cellConfirmColorThree;
    private TextView cellConfirmColorFour;
    private TextView cellRsrpOne;
    private TextView cellRsrpTwo;
    private TextView cellRsrpThree;
    private TextView cellRsrpFour;
    private TextView pciNumOne;
    private TextView pciNumTwo;
    private TextView pciNumThree;
    private TextView pciNumFour;
    private CheckBox filterCheckBox;
    private MonitorHelper monitorHelper;

    private FindSTMSIInfoManager findSTMSIInfoManager;
    private SharedPreferences sharedPreferences;
    private static final String CONFIG_DERECTORY = "findstmsi";
    private static final String FILTERCHECKBOX = "filterCheckBox";
    private static final String TARGETSTMSI = "targetSTMSI";
    private TextView sumCountText;
    private TextView nullCountText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        setContentView(R.layout.activity_find_stmsi);

        init();

    }
    @Override
    protected void onDestroy() {
        findSTMSIInfoManager.clear();
        findSTMSIInfoManager.add(countSortedInfoList);
        findSTMSIInfoManager.closeDB();

        if(startToFind) {
            FindSTMSI.getInstance().stop();
            startToFind = false;
        }
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (filterCheckBox.isChecked()) {
            editor.putString(FILTERCHECKBOX, "true");
        } else {
            editor.putString(FILTERCHECKBOX, "false");
        }
        editor.putString(TARGETSTMSI,targetSTMSI.getText().toString());
        editor.commit();
        unregisterReceiver(receiver);
        monitorHelper.unbindservice(FindSTMSIActivity.this);
        super.onDestroy();
    }

    @Override
    protected void onStop() {

        super.onStop();
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
        LayoutInflater inflater = getLayoutInflater();
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.find_stmsi_cell_status_bar);
        LinearLayout cellStatusBar = (LinearLayout) inflater.inflate(R.layout.cell_status_bar,null).findViewById(R.id.cell_status_bar_linearlayout);
        linearLayout.addView(cellStatusBar);

        findSTMSIInfoManager = new FindSTMSIInfoManager(this);
        countSortedInfoList = getStmsiInfoList();
        startToFind = false;

        startButton = (Button) findViewById(R.id.find_stmsi_start_button);
        stopButton = (Button) findViewById(R.id.find_stmsi_stop_button);
        targetPhone = (TextView) findViewById(R.id.find_stmsi_target_phone_num);
        currentPCi = (TextView) findViewById(R.id.find_stmsi_current_pci);
        targetSTMSI = (EditText) findViewById(R.id.find_stmsi_target_stmsi);
        //deviceStatusColor = (TextView) findViewById(R.id.find_stmsi_device_background_one);
        cellConfirmColorOne = (TextView)findViewById(R.id.cell_status_bar_confirm_background_one);
        cellConfirmColorTwo = (TextView)findViewById(R.id.cell_status_bar_confirm_background_two);
        cellConfirmColorThree = (TextView)findViewById(R.id.cell_status_bar_confirm_background_three);
        cellConfirmColorFour = (TextView)findViewById(R.id.cell_status_bar_confirm_background_four);
        cellRsrpOne = (TextView)findViewById(R.id.cell_status_bar_rsrp_one);
        cellRsrpTwo = (TextView)findViewById(R.id.cell_status_bar_rsrp_two);
        cellRsrpThree = (TextView)findViewById(R.id.cell_status_bar_rsrp_three);
        cellRsrpFour = (TextView)findViewById(R.id.cell_status_bar_rsrp_four);
        pciNumOne = (TextView)findViewById(R.id.cell_status_bar_pci_num_one);
        pciNumTwo = (TextView)findViewById(R.id.cell_status_bar_pci_num_two);
        pciNumThree = (TextView)findViewById(R.id.cell_status_bar_pci_num_three);
        pciNumFour = (TextView)findViewById(R.id.cell_status_bar_pci_num_four);

        targetPhone.setText(Global.Configuration.targetPhoneNum);


        sumCountText = (TextView) findViewById(R.id.find_stmsi_sum_count_text);
        nullCountText = (TextView) findViewById(R.id.find_stmsi_null_count_text);
        filterCheckBox = (CheckBox) findViewById(R.id.find_stmsi_filter_checkbox);
        count = (ListView) findViewById(R.id.find_stmsi_count_listView);

        sharedPreferences =  getSharedPreferences(CONFIG_DERECTORY, Context.MODE_PRIVATE);
        if(sharedPreferences.getString(FILTERCHECKBOX,"false").equals("true")){
            filterCheckBox.setChecked(true);
        } else {
            filterCheckBox.setChecked(false);
        }
        if(Global.TARGET_STMSI != null){
            targetSTMSI.setText(Global.TARGET_STMSI);
        } else {
            targetSTMSI.setText(sharedPreferences.getString(TARGETSTMSI,""));
        }
        CountAdapter countAdapter = new CountAdapter(this);
        count.setAdapter(countAdapter);
        ((CountAdapter) count.getAdapter()).notifyDataSetChanged();
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
                if (DeviceManager.getInstance().getDevices().size() == 0) {
                    Toast.makeText(FindSTMSIActivity.this,"没有可用设备！",Toast.LENGTH_LONG).show();
                    return;
                }
                if(Global.Configuration.smsType == Status.TriggerSMSType.INSIDE && Global.Configuration.targetPhoneNum.isEmpty()){
                    Toast.makeText(FindSTMSIActivity.this,"目标号码为空！",Toast.LENGTH_LONG).show();
                    return;
                }
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
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        FindSTMSIActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                startButton.setEnabled(true);
                            }
                        });
                    }
                }, 2000);
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
        MyRunnable myRunnable = new MyRunnable();
        Global.ThreadPool.scheduledThreadPool.scheduleAtFixedRate(myRunnable, 120, 120, TimeUnit.SECONDS);

        IntentFilter filter = new IntentFilter(MonitorApplication.BROAD_TO_MAIN_ACTIVITY);
        filter.addAction(MonitorApplication.BROAD_FROM_MAIN_MENU_DEVICE);
        registerReceiver(receiver, filter);
        monitorHelper = new MonitorHelper();
        monitorHelper.bindService(FindSTMSIActivity.this);


    }
    private List<FindSTMSI.CountSortedInfo> getStmsiInfoList(){
        List<FindeSTMSIInfoDAO> findeSTMSIInfoDAOList = findSTMSIInfoManager.listDB();
        List<FindSTMSI.CountSortedInfo> stmsiInfoDBList = new ArrayList<>();
        for(FindeSTMSIInfoDAO dao : findeSTMSIInfoDAOList) {
            FindSTMSI.CountSortedInfo countSortedInfo = new FindSTMSI.CountSortedInfo();
            countSortedInfo.stmsi = dao.stmsi;
            countSortedInfo.count = dao.count;
            countSortedInfo.time = dao.time;
            countSortedInfo.pci = dao.pci;
            countSortedInfo.earfcn = dao.earfcn;
            stmsiInfoDBList.add(countSortedInfo);
        }
        return stmsiInfoDBList;
    }
    private void refresh() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                countSortedInfoList.clear();
                countSortedInfoList.addAll(FindSTMSI.getInstance().getCountSortedInfoList());
                ((CountAdapter) count.getAdapter()).notifyDataSetChanged();
                sumCountText.setText(String.valueOf(FindSTMSI.getInstance().sumCount));
                nullCountText.setText(String.valueOf(FindSTMSI.getInstance().nullCount));
                int position = 0;
                for (MonitorDevice device : DeviceManager.getInstance().getAllDevices()) {
                    if(device.getStatus() != Status.DeviceStatus.DISCONNECTED ) {
                        if(device.getIsReadyToMonitor()) {
                            if (device.getWorkingStatus() == Status.DeviceWorkingStatus.NORMAL) {
                                String rsrp = String.format("%.2f", device.getCellInfo().rsrp);
                                setCellStatusBar(position, Color.GREEN, rsrp, device.getCellInfo().pci + "");
                            } else if(device.getWorkingStatus() != Status.DeviceWorkingStatus.NORMAL){
                                setCellStatusBar(position, Color.YELLOW, "N/A", device.getCellInfo().pci + "");
                            }
                        } else {
                            setCellStatusBar(position, Color.RED, "", "");
                        }
                    } else {
                        setCellStatusBar(position, getResources().getColor(R.color.default_color), "", "");
                    }
                    position++;
                }
            }
        });
    }
    private void setCellStatusBar(int position,int color,String text,String pci){
        switch (position){
            case 0:{
                cellConfirmColorOne.setBackgroundColor(color);
                cellRsrpOne.setText(text);
                pciNumOne.setText(pci);
                break;
            }
            case 1:{
                cellConfirmColorTwo.setBackgroundColor(color);
                cellRsrpTwo.setText(text);
                pciNumTwo.setText(pci);
                break;
            }
            case 2:{
                cellConfirmColorThree.setBackgroundColor(color);
                cellRsrpThree.setText(text);
                pciNumThree.setText(pci);
                break;
            }
            case 3:{
                cellConfirmColorFour.setBackgroundColor(color);
                cellRsrpFour.setText(text);
                pciNumFour.setText(pci);
                break;
            }
            default:break;
        }
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
                //refreshDeviceStatus(intent);
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
        Bundle bundle = intent.getExtras();
        int colorOne = bundle.getInt("colorOne");
        int colorTwo = bundle.getInt("colorTwo");
        int colorThree = bundle.getInt("colorThree");
        int colorFour = bundle.getInt("colorFour");
        cellConfirmColorOne.setBackgroundColor(Color.RED);
        cellConfirmColorTwo.setBackgroundColor(Color.RED);
        cellConfirmColorThree.setBackgroundColor(Color.RED);
        cellConfirmColorFour.setBackgroundColor(Color.RED);
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
    class MyRunnable implements Runnable {
        @Override
        public void run() {
            if(startToFind) {
                if (FindSTMSI.getInstance().stmsiCount < STMSICountMaxValuePerMinute) {
                    FindSTMSI.getInstance().stmsiCount = 0;
                } else {
                    //FindSTMSI.getInstance().stop();
                    //startToFind = false;
                    FindSTMSIActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //startButton.setEnabled(true);
                            new AlertDialog.Builder(FindSTMSIActivity.this)
                                    .setTitle("注意")
                                    .setMessage("该处STMSI过多，不适合工作!")
                                    .setPositiveButton("确定", null)
                                    .show();
                        }
                    });

                }
            }
        }
    }

}
