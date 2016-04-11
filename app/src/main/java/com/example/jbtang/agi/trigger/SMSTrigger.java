package com.example.jbtang.agi.trigger;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jbtang.agi.R;
import com.example.jbtang.agi.core.Global;
import com.example.jbtang.agi.core.Status;
import com.example.jbtang.agi.device.DeviceManager;
import com.example.jbtang.agi.device.MonitorDevice;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by jbtang on 12/6/2015.
 */
public class SMSTrigger implements Trigger {
    private static final String TAG = "smsTrigger";
    private static final String RAWSMS_MESSAGE_PREFIX = "sendSmsByRawPDU";
    private static final SMSTrigger instance = new SMSTrigger();

    private boolean start;

    private SMSTrigger() {
        start = false;
    }

    public static SMSTrigger getInstance() {
        return instance;
    }

    private Activity currentActivity;
    private TextView countTextView;
    private TextView failCountTextView;
    private int smsCount;
    private int smsFailCount;

    private Runnable task;
    private Future future;
    String finalText = "";
    @Override
    public void start(Activity activity, Status.Service service) {
        if (!start) {
            currentActivity = activity;
            switch (service) {
                case FINDSTMIS:
                    task = new FindSTMSITask();
                    future = Global.ThreadPool.scheduledThreadPool.scheduleAtFixedRate(task, 3, Global.Configuration.triggerInterval, TimeUnit.SECONDS);
                    break;
                case ORIENTATION:
                    task = new OrientationFindingTask();
                    future = Global.ThreadPool.scheduledThreadPool.scheduleAtFixedRate(task, 3, Global.Configuration.triggerInterval, TimeUnit.SECONDS);
                    break;
                case INTERFERENCE:
                    task = new InterferenceTask();
                    future = Global.ThreadPool.scheduledThreadPool.scheduleAtFixedRate(task, 1, 5, TimeUnit.SECONDS);
                    break;
                default:
                    throw new IllegalArgumentException("Illegal service: " + service.name());
            }

            start = true;
        }
    }

    @Override
    public void stop() {
        if (start) {
            for (MonitorDevice device : DeviceManager.getInstance().getDevices()) {
                device.stopMonitor();
            }
            if (task != null && future != null) {
                future.cancel(true);
            }
            task = null;
            future = null;
            start = false;
        }
    }

    class FindSTMSITask implements Runnable {
        private boolean startMonitor;
        public FindSTMSITask() {
            smsCount = 0;
            smsFailCount = 0;
            countTextView = (TextView) currentActivity.findViewById(R.id.find_stmsi_triggered_count);
            failCountTextView = (TextView) currentActivity.findViewById(R.id.find_stmsi_triggered_fail_count);
            startMonitor = false;
        }

        @Override
        public void run() {
            if (smsCount == Global.Configuration.triggerTotalCount) {
                future.cancel(true);
            }
            if(!startMonitor) {
                for (MonitorDevice device : DeviceManager.getInstance().getDevices()) {
                    device.startMonitor(Status.Service.FINDSTMIS);
                    device.setWorkingStatus(Status.DeviceWorkingStatus.ABNORMAL);
                }
                startMonitor = true;
            }
            if(Global.Configuration.smsType == Status.TriggerSMSType.INSIDE)
                send();
            smsCount++;
            freshSmsCount();
            Global.sendTime = new Date();
        }
    }

    class InterferenceTask implements Runnable{
        private boolean startMonitor;
        public InterferenceTask() {
            smsCount = 0;
            smsFailCount = 0;
            countTextView = (TextView) currentActivity.findViewById(R.id.interference_triggered_count);
            failCountTextView = (TextView) currentActivity.findViewById(R.id.interference_triggered_fail_count);
            startMonitor = false;
        }

        @Override
        public void run() {
            if (smsCount == Global.Configuration.triggerTotalCount) {
                future.cancel(true);
            }
            if(!startMonitor) {
                for (MonitorDevice device : DeviceManager.getInstance().getDevices()) {
                    device.startMonitor(Status.Service.FINDSTMIS);
                    device.setWorkingStatus(Status.DeviceWorkingStatus.ABNORMAL);
                }
                startMonitor = true;
            }
            if(Global.Configuration.smsType == Status.TriggerSMSType.INSIDE)
                send();
            smsCount++;
            freshSmsCount();
        }
    }

    class OrientationFindingTask implements Runnable {
        private boolean startMonitor;

        public OrientationFindingTask() {
            smsCount = 0;
            smsFailCount = 0;
            countTextView = (TextView) currentActivity.findViewById(R.id.orientation_triggered_count);
            failCountTextView = (TextView) currentActivity.findViewById(R.id.orientation_triggered_fail_count);
            startMonitor = false;
        }

        @Override
        public void run() {
            if (!startMonitor) {
                for (MonitorDevice device : DeviceManager.getInstance().getDevices()) {
                    device.startMonitor(Status.Service.ORIENTATION);
                    device.setWorkingStatus(Status.DeviceWorkingStatus.ABNORMAL);
                }
                startMonitor = true;
            }
            if(Global.Configuration.smsType == Status.TriggerSMSType.INSIDE)
                send();

            smsCount++;
            freshSmsCount();
        }
    }

    private void send() {
        String SENT = "sms_sent";
        String DELIVERED = "sms_delivered";
        PendingIntent sentPI = PendingIntent.getBroadcast(currentActivity, 0, new Intent(SENT), 0);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(currentActivity, 0, new Intent(DELIVERED), 0);

        if(smsCount == 0) {
            currentActivity.registerReceiver(new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (getResultCode()) {
                        case Activity.RESULT_OK:
                            Log.i("====>", "Activity.RESULT_OK");
                            //Toast.makeText(context,"发送成功",Toast.LENGTH_SHORT).show();
                            break;
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            Log.i("====>", "RESULT_ERROR_GENERIC_FAILURE");
                            break;
                        case SmsManager.RESULT_ERROR_NO_SERVICE:
                            Log.i("====>", "RESULT_ERROR_NO_SERVICE");
                            break;
                        case SmsManager.RESULT_ERROR_NULL_PDU:
                            Log.i("====>", "RESULT_ERROR_NULL_PDU");
                            break;
                        case SmsManager.RESULT_ERROR_RADIO_OFF:
                            Log.i("====>", "RESULT_ERROR_RADIO_OFF");
                            break;
                        default:
                            smsFailCount++;
                            //Toast.makeText(context,"发送失败",Toast.LENGTH_SHORT).show();
                            break;
                    }
                    freshSmsCount();
                }
            }, new IntentFilter(SENT));

            currentActivity.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (getResultCode()) {
                        case Activity.RESULT_OK:
                            //Toast.makeText(context,"接收成功",Toast.LENGTH_SHORT).show();
                            Log.i("====>", "RESULT_OK");
                            break;
                        case Activity.RESULT_CANCELED:
                            //Toast.makeText(context,"接收失败",Toast.LENGTH_SHORT).show();
                            Log.i("=====>", "RESULT_CANCELED");
                            break;
                    }
                }
            }, new IntentFilter(DELIVERED));
            String phone = Global.Configuration.targetPhoneNum;
            String smsCenter = Global.Configuration.smsCenter;
            String text = "hello";
            SMSHelper smsHelper = new SMSHelper();
            String DCSFormat = "英文";//Global.Configuration.insideSMSType == Status.InsideSMSType.NORMAL ? "英文" : "定位短信";//DCS
            String SendFormat = "";//PDU-Type
            String smsType = Global.Configuration.insideSMSType == Status.InsideSMSType.NORMAL ? "正常短信" : "定位短信";
            int PDUNums = 0;
            String SmsPDU = smsHelper.sms_Send_PDU_Encoder(phone, smsCenter,text, DCSFormat, SendFormat, smsType, PDUNums);
            finalText = RAWSMS_MESSAGE_PREFIX + SmsPDU;
        }

        Log.e("SMS", finalText);

        SmsManager smsm = SmsManager.getDefault();
        smsm.sendTextMessage(Global.Configuration.targetPhoneNum, null, finalText, sentPI, deliveredPI);

    }
    private void freshSmsCount(){
        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                countTextView.setText(String.valueOf(smsCount));
                failCountTextView.setText(String.valueOf(smsFailCount));
            }
        });
    }
}
