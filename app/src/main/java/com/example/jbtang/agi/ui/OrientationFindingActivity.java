package com.example.jbtang.agi.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jbtang.agi.R;
import com.example.jbtang.agi.core.Global;
import com.example.jbtang.agi.core.Status;
import com.example.jbtang.agi.dao.OrientationInfos.OrientationInfoDAO;
import com.example.jbtang.agi.dao.OrientationInfos.OrientationInfoManager;
import com.example.jbtang.agi.device.DeviceManager;
import com.example.jbtang.agi.device.MonitorDevice;
import com.example.jbtang.agi.external.MonitorApplication;
import com.example.jbtang.agi.external.MonitorHelper;
import com.example.jbtang.agi.service.OrientationFinding;
import com.example.jbtang.agi.utils.BarChartView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import io.fmaster.LTEServCellMessage;

/**
 * Created by jbtang on 11/7/2015.
 */
public class OrientationFindingActivity extends AppCompatActivity {
    private static final String TAG = "OrientationActivity";
    private static final int RSRP_LIST_MAX_SIZE = 4;
    private boolean startToFind;

    private TextView currentPCi;
    private TextView targetPhoneNum;
    private TextView targetStmsiTextView;
    private RadioGroup triggerTypeRG;
    private int temSMSInterval;
    private int temSilenceTimer;
    private Button startButton;
    private Button stopButton;
    private ListView resultListView;
    private LinearLayout resultGraphLayout;
    private myHandler handler;
    private List<OrientationFinding.OrientationInfo> orientationInfoList;
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
    private MonitorHelper monitorHelper;
    public static List<String> options = Arrays.asList("", "", "", "", "", "");
    private BarChartView view;
    private TextToSpeech textToSpeech;
    private OrientationInfoManager orientationInfoManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orientation_finding);

        startToFind = false;
        OrientationFinding.getInstance().targetStmsi = Global.TARGET_STMSI;
        orientationInfoList = new ArrayList<>();
        init();

    }

    @Override
    protected void onDestroy() {
        orientationInfoManager.clear();
        orientationInfoManager.add(orientationInfoList);
        orientationInfoManager.closeDB();
        if (startToFind) {
            OrientationFinding.getInstance().stop();
            startToFind = false;
        }
        Global.Configuration.triggerInterval = temSMSInterval;
        Global.Configuration.silenceCheckTimer = temSilenceTimer;
        unregisterReceiver(receiver);
        monitorHelper.unbindservice(OrientationFindingActivity.this);
        textToSpeech.shutdown();
        Log.e("Orientation", "orientation is onDestory");
        super.onDestroy();
    }

    @Override
    protected void onStop() {

        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_orientation_find, menu);
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
        }

        return super.onOptionsItemSelected(item);
    }

    private void init() {
        LayoutInflater inflater = getLayoutInflater();
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.orientation_find_layout_cell_status_bar);
        LinearLayout cellStatusBar = (LinearLayout) inflater.inflate(R.layout.cell_status_bar, null).findViewById(R.id.cell_status_bar_linearlayout);
        linearLayout.addView(cellStatusBar);

        currentPCi = (TextView) findViewById(R.id.orientation_current_pci);
        targetPhoneNum = (TextView) findViewById(R.id.orientation_target_phone_num);
        targetStmsiTextView = (TextView) findViewById(R.id.orientation_find_target_stmsi);
        triggerTypeRG = (RadioGroup) findViewById(R.id.orientation_find_trigger_type);
        startButton = (Button) findViewById(R.id.orientation_find_start);
        stopButton = (Button) findViewById(R.id.orientation_find_stop);
        cellConfirmColorOne = (TextView) findViewById(R.id.cell_status_bar_confirm_background_one);
        cellConfirmColorTwo = (TextView) findViewById(R.id.cell_status_bar_confirm_background_two);
        cellConfirmColorThree = (TextView) findViewById(R.id.cell_status_bar_confirm_background_three);
        cellConfirmColorFour = (TextView) findViewById(R.id.cell_status_bar_confirm_background_four);
        cellRsrpOne = (TextView) findViewById(R.id.cell_status_bar_rsrp_one);
        cellRsrpTwo = (TextView) findViewById(R.id.cell_status_bar_rsrp_two);
        cellRsrpThree = (TextView) findViewById(R.id.cell_status_bar_rsrp_three);
        cellRsrpFour = (TextView) findViewById(R.id.cell_status_bar_rsrp_four);
        pciNumOne = (TextView) findViewById(R.id.cell_status_bar_pci_num_one);
        pciNumTwo = (TextView) findViewById(R.id.cell_status_bar_pci_num_two);
        pciNumThree = (TextView) findViewById(R.id.cell_status_bar_pci_num_three);
        pciNumFour = (TextView) findViewById(R.id.cell_status_bar_pci_num_four);

        orientationInfoManager = new OrientationInfoManager(this);
        orientationInfoList = getOrientationInfoList();

        resultListView = (ListView) findViewById(R.id.orientation_find_result_list);
        resultListView.setAdapter(new MyAdapter(this));
        ((MyAdapter) resultListView.getAdapter()).notifyDataSetChanged();

        resultGraphLayout = (LinearLayout) findViewById(R.id.orientation_find_layout_result_graph);
        refreshBarChart();

        targetPhoneNum.setText(Global.Configuration.targetPhoneNum);
        targetStmsiTextView.setText(OrientationFinding.getInstance().targetStmsi);

        temSMSInterval = Global.Configuration.triggerInterval;
        temSilenceTimer = Global.Configuration.silenceCheckTimer;

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (textToSpeech != null && !textToSpeech.isSpeaking()) {
                    textToSpeech.speak("开始侧向", TextToSpeech.QUEUE_FLUSH, null);
                }
                if (startToFind || DeviceManager.getInstance().getDevices().size() == 0 || Global.TARGET_STMSI == null)
                    return;
                if (triggerTypeRG.getCheckedRadioButtonId() == R.id.orientation_find_trigger_continue) {
                    Global.Configuration.triggerInterval = 4;//连续触发间隔
                    Global.Configuration.silenceCheckTimer = 0;
                } else {
                    Global.Configuration.triggerInterval = temSMSInterval;
                    Global.Configuration.silenceCheckTimer = temSilenceTimer;
                }
                OrientationFinding.getInstance().start(OrientationFindingActivity.this);
                startToFind = true;
                orientationInfoList.clear();
                for (int i = 0; i < RSRP_LIST_MAX_SIZE + 1; i++) {
                    options.set(i, "");
                }
                refreshBarChart();
                startButton.setEnabled(false);
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OrientationFinding.getInstance().stop();
                if (startToFind) {
                    startToFind = false;
                    //Toast.makeText(OrientationFindingActivity.this,"设备停止中，请稍后！",Toast.LENGTH_LONG).show();
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            OrientationFindingActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    startButton.setEnabled(true);
                                }
                            });
                        }
                    }, 2000);
                }
            }
        });

        triggerTypeRG.check(R.id.orientation_find_trigger_single);
        temSMSInterval = Global.Configuration.triggerInterval;
        temSilenceTimer = Global.Configuration.silenceCheckTimer;

        IntentFilter filter = new IntentFilter();
        filter.addAction(MonitorApplication.BROAD_TO_MAIN_ACTIVITY);
        filter.addAction(MonitorApplication.BROAD_FROM_MAIN_MENU_DEVICE);
        registerReceiver(receiver, filter);
        handler = new myHandler(this);
        Global.ThreadPool.scheduledThreadPool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (startToFind) {
                    Message msg = new Message();
                    msg.what = 2;
                    handler.sendMessage(msg);
                }
            }
        }, 1, 3, TimeUnit.SECONDS);
        OrientationFinding.getInstance().setOutHandler(handler);
        monitorHelper = new MonitorHelper();
        monitorHelper.bindService(OrientationFindingActivity.this);

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = textToSpeech.setLanguage(Locale.CHINA);
                    if (result == TextToSpeech.LANG_NOT_SUPPORTED
                            || result == TextToSpeech.LANG_MISSING_DATA) {
                        Toast.makeText(OrientationFindingActivity.this, "数据丢失或不支持", Toast.LENGTH_LONG).show();
                    } else {
                        textToSpeech.setPitch(1.5f);
                    }
                }
            }
        });
    }

    private List<OrientationFinding.OrientationInfo> getOrientationInfoList() {
        List<OrientationInfoDAO> orientationInfoDAOList = orientationInfoManager.listDB();
        List<OrientationFinding.OrientationInfo> orientationInfoDBList = new ArrayList<>();
        for (OrientationInfoDAO dao : orientationInfoDAOList) {
            OrientationFinding.OrientationInfo orientationInfo = new OrientationFinding.OrientationInfo();
            //orientationInfo.PUSCHRsrp = dao.pusch.equals(Double.NaN)? Double.NaN:Double.parseDouble(dao.pusch);
            orientationInfo.PUSCHRsrp = Double.parseDouble(dao.pusch);
            orientationInfo.pci = dao.pci;
            orientationInfo.earfcn = dao.earfcn;
            orientationInfo.timeStamp = dao.time;
            orientationInfoDBList.add(orientationInfo);
        }
        return orientationInfoDBList;
    }

    private void refresh(String type) {
        final String temtype = type;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (temtype.equals("all")) {
                    ((MyAdapter) resultListView.getAdapter()).notifyDataSetChanged();
                    resultListView.setSelection(orientationInfoList.size() - 1);
                    refreshBarChart();
                }
                refreshCellStatusBar();
            }
        });
    }

    private void refreshBarChart() {
        resultGraphLayout.removeAllViews();
        view = new BarChartView(OrientationFindingActivity.this);

        int from = orientationInfoList.size() < RSRP_LIST_MAX_SIZE ? 0 : orientationInfoList.size() - RSRP_LIST_MAX_SIZE;
        int to = orientationInfoList.size();
//        int[] pucchList = new int[RSRP_LIST_MAX_SIZE];
        int[] puschList = new int[RSRP_LIST_MAX_SIZE];

        int rsrpIndex = RSRP_LIST_MAX_SIZE - 1;
        for (; to > from; to--, rsrpIndex--) {
//            pucchList[rsrpIndex] = orientationInfoList.get(to - 1).getStandardPucch();
            int pusch = orientationInfoList.get(to - 1).getStandardPusch();
            puschList[rsrpIndex] = pusch;
            options.set(rsrpIndex + 1, orientationInfoList.get(to - 1).timeStamp);
            if (pusch > 25 && !Global.Configuration.targetPhoneNum.equals(Global.LogInfo.phone)) {
                Global.LogInfo.phone = Global.Configuration.targetPhoneNum;
                Global.LogInfo.findStartTime = new Date().toString();
            } else if (pusch > 25) {
                Global.LogInfo.targetSTMSI = Global.TARGET_STMSI;
                Global.LogInfo.findEndTime = new Date().toString();
            }
            Log.e(TAG, "LogInfo.phone" + Global.LogInfo.phone + ",LogInfo.targetSTMSI" + Global.LogInfo.targetSTMSI + ",LogInfo.findStartTime" + Global.LogInfo.findStartTime + ",LogInfo.findEndTime" + Global.LogInfo.findEndTime);
        }
        for (; to > from; to--, rsrpIndex--) {
//            pucchList[rsrpIndex] = 0;
            puschList[rsrpIndex] = 0;
        }
        view.initData(puschList, options, "功率图");
        resultGraphLayout.addView(view.getBarChartView());
        if (textToSpeech != null && !textToSpeech.isSpeaking()) {
            textToSpeech.speak(String.valueOf(puschList[RSRP_LIST_MAX_SIZE - 1]), TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private void refreshCellStatusBar() {
        int position = 0;
        for (MonitorDevice device : DeviceManager.getInstance().getAllDevices()) {
            if (device.getStatus() != Status.DeviceStatus.DISCONNECTED) {
                if (device.getIsReadyToMonitor()) {
                    if (device.getWorkingStatus() == Status.DeviceWorkingStatus.NORMAL) {
                        String rsrp = String.format("%.2f", device.getCellInfo().rsrp);
                        setCellStatusBar(position, Color.GREEN, rsrp, device.getCellInfo().pci + "");
                    } else if (device.getWorkingStatus() != Status.DeviceWorkingStatus.NORMAL) {
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

    private void setCellStatusBar(int position, int color, String text, String pci) {
        switch (position) {
            case 0: {
                cellConfirmColorOne.setBackgroundColor(color);
                cellRsrpOne.setText(text);
                pciNumOne.setText(pci);
                break;
            }
            case 1: {
                cellConfirmColorTwo.setBackgroundColor(color);
                cellRsrpTwo.setText(text);
                pciNumTwo.setText(pci);
                break;
            }
            case 2: {
                cellConfirmColorThree.setBackgroundColor(color);
                cellRsrpThree.setText(text);
                pciNumThree.setText(pci);
                break;
            }
            case 3: {
                cellConfirmColorFour.setBackgroundColor(color);
                cellRsrpFour.setText(text);
                pciNumFour.setText(pci);
                break;
            }
            default:
                break;
        }
    }

    static class myHandler extends Handler {
        private final WeakReference<OrientationFindingActivity> mOuter;

        public myHandler(OrientationFindingActivity activity) {
            mOuter = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1 && msg.obj != null) {
                OrientationFinding.OrientationInfo info = (OrientationFinding.OrientationInfo) msg.obj;
                //if(!Double.isNaN(info.PUSCHRsrp)) {
                mOuter.get().orientationInfoList.add(info);
                mOuter.get().refresh("all");
                //}
            } else if (msg.what == 2) {
                mOuter.get().refresh("");
            }

        }
    }

    /**
     * for ListView
     */
    private final class ViewHolder {
        public TextView num;
        public TextView pusch;
        public TextView pci;
        public TextView earfcn;
        public TextView time;
    }

    private class MyAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        public MyAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return orientationInfoList.size();
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
            final ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.orientation_finding_list_item, null);
                holder = new ViewHolder();
                holder.num = (TextView) convertView.findViewById(R.id.orientation_find_list_item_num);
                holder.pusch = (TextView) convertView.findViewById(R.id.orientation_find_list_item_pusch);
                holder.pci = (TextView) convertView.findViewById(R.id.orientation_find_list_item_pci);
                holder.earfcn = (TextView) convertView.findViewById(R.id.orientation_find_list_item_earfcn);
                holder.time = (TextView) convertView.findViewById(R.id.orientation_find_list_item_time);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.num.setText(String.valueOf(position + 1));
            holder.pusch.setText(String.format("%.2f", orientationInfoList.get(position).PUSCHRsrp));
            holder.pci.setText(orientationInfoList.get(position).pci);
            holder.earfcn.setText(orientationInfoList.get(position).earfcn);
            holder.time.setText(orientationInfoList.get(position).timeStamp);
            return convertView;
        }
    }


    private MyBroadcastReceiver receiver = new MyBroadcastReceiver();

    class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals("")) {
                return;
            }
            if (intent.getAction().equals(MonitorApplication.BROAD_TO_MAIN_ACTIVITY)) {
                refreshServerCell(intent);
            } else if (intent.getAction().equals(MonitorApplication.BROAD_FROM_MAIN_MENU_DEVICE)) {
                //refreshDeviceStatus(intent);
            }
        }
    }

    private void refreshServerCell(Intent intent) {
        int flag = intent.getFlags();
        Bundle bundle = intent.getExtras();
        switch (flag) {
            case MonitorApplication.SERVER_CELL_FLAG:
                LTEServCellMessage myServCellMessage = bundle.getParcelable("msg");
                if (myServCellMessage != null) {
                    currentPCi.setText(String.valueOf(myServCellMessage.getPCI()));
                }
                break;
            default:
                break;
        }
    }

    private void refreshDeviceStatus(Intent intent) {
        Bundle bundle = intent.getExtras();
        int colorOne = bundle.getInt("colorOne");
        int colorTwo = bundle.getInt("colorTwo");
        int colorThree = bundle.getInt("colorThree");
        int colorFour = bundle.getInt("colorFour");
        if (colorOne == Color.RED) {
            cellConfirmColorOne.setBackgroundColor(Color.RED);
        }
        if (colorTwo == Color.RED) {
            cellConfirmColorTwo.setBackgroundColor(Color.RED);
        }
        if (colorThree == Color.RED) {
            cellConfirmColorThree.setBackgroundColor(Color.RED);
        }
        if (colorFour == Color.RED) {
            cellConfirmColorFour.setBackgroundColor(Color.RED);
        }
    }

//    private void refreshView(Intent intent) {
//
//        int flag = intent.getFlags();
//        Bundle bundle = intent.getExtras();
//        switch (flag) {
//            case MonitorApplication.STMSI:
//                String stmsi = bundle.getString("msg");
//                myStmsiTextView.setText(stmsi);
//                break;
//        }
//    }
}
