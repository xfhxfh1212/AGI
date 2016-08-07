package com.example.jbtang.agi.trigger;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;
import com.example.jbtang.agi.R;
import com.example.jbtang.agi.core.Global;
import com.example.jbtang.agi.core.Status;
import com.example.jbtang.agi.device.DeviceManager;
import com.example.jbtang.agi.device.MonitorDevice;
import com.example.jbtang.agi.service.OrientationFinding;
import com.example.jbtang.agi.ui.FindSTMSIActivity;
import com.example.jbtang.agi.ui.InterferenceActivity;
import com.example.jbtang.agi.ui.OrientationFindingActivity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by jbtang on 12/6/2015.
 */
public class SMSTrigger implements Trigger {
    private static final String TAG = "smsTrigger";
    private static final String RAWSMS_MESSAGE_PREFIX = "sendSmsByRawPDU";
    private static final SMSTrigger instance = new SMSTrigger();

    private static final int RECEIVE_TAG = 15000;//等待短信发送回执时间,用来判断目标是否关机
    private static final int FIRST_SEND_DELAY = 6000;//第一次短信发送延迟时间
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
    private Timer timer;
    private boolean send;
    private boolean deliver;
    private int orientationType;

    @Override
    public void start(Activity activity, Status.Service service) {
        if (!start) {
            currentActivity = activity;
            switch (service) {
                case FINDSTMIS:
                    task = new FindSTMSITask();
                    future = Global.ThreadPool.scheduledThreadPool.scheduleAtFixedRate(task, 1, Global.Configuration.triggerInterval, TimeUnit.SECONDS);
                    break;
                case ORIENTATION:
                    task = new OrientationFindingTask();
                    future = Global.ThreadPool.scheduledThreadPool.scheduleAtFixedRate(task, 1, Global.Configuration.triggerInterval, TimeUnit.SECONDS);
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
            if (timer != null)
                timer.cancel();
            if (Global.Configuration.type == Status.TriggerType.SMS && Global.Configuration.smsType == Status.TriggerSMSType.INSIDE) {
                currentActivity.unregisterReceiver(sendReceive);
                currentActivity.unregisterReceiver(deliverReceive);
            }
            task = null;
            future = null;
            timer = null;
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
//            if (smsCount == Global.Configuration.triggerTotalCount) {
//                future.cancel(true);
//            }
            if (!startMonitor) {
                for (MonitorDevice device : DeviceManager.getInstance().getDevices()) {
                    device.startMonitor(Status.Service.FINDSTMIS);
                    device.setWorkingStatus(Status.DeviceWorkingStatus.ABNORMAL);
                }
                if (Global.Configuration.type == Status.TriggerType.SMS && Global.Configuration.smsType == Status.TriggerSMSType.INSIDE) {
                    String SENT = "sms_sent";
                    String DELIVERED = "sms_delivered";
                    currentActivity.registerReceiver(sendReceive, new IntentFilter(SENT));
                    currentActivity.registerReceiver(deliverReceive, new IntentFilter(DELIVERED));
                }
                startMonitor = true;
            }
//            for (MonitorDevice device : DeviceManager.getInstance().getDevices()) {
//                if (device.isConnected()) {
//
//                }
//            }
            if (Global.Configuration.type == Status.TriggerType.SMS && Global.Configuration.smsType == Status.TriggerSMSType.INSIDE) {
                send();
            } else if (Global.Configuration.type == Status.TriggerType.PHONE && Global.Configuration.smsType == Status.TriggerSMSType.INSIDE) {
                call();
            }
            smsCount++;
            Global.sendTime = new Date();
            freshSmsCount();
        }
    }

    class InterferenceTask implements Runnable {
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
//            if (smsCount == Global.Configuration.triggerTotalCount) {
//                future.cancel(true);
//            }
            if (!startMonitor) {
                Log.e(TAG, DeviceManager.getInstance().getDevices().size() + "");
                for (MonitorDevice device : DeviceManager.getInstance().getDevices()) {
                    Log.e(TAG, device.getName());
                    device.startMonitor(Status.Service.FINDSTMIS);
                    device.setWorkingStatus(Status.DeviceWorkingStatus.ABNORMAL);
                }
                if (Global.Configuration.type == Status.TriggerType.SMS && Global.Configuration.smsType == Status.TriggerSMSType.INSIDE) {
                    String SENT = "sms_sent";
                    String DELIVERED = "sms_delivered";
                    currentActivity.registerReceiver(sendReceive, new IntentFilter(SENT));
                    currentActivity.registerReceiver(deliverReceive, new IntentFilter(DELIVERED));
                }
                startMonitor = true;
            }
            if (Global.Configuration.type == Status.TriggerType.SMS && Global.Configuration.smsType == Status.TriggerSMSType.INSIDE) {
                send();
            } else if (Global.Configuration.type == Status.TriggerType.PHONE && Global.Configuration.smsType == Status.TriggerSMSType.INSIDE) {
                call();
            }
            smsCount++;
            freshSmsCount();
        }
    }

    class OrientationFindingTask implements Runnable {
        private boolean startMonitor;

        public OrientationFindingTask() {
            orientationType = ((RadioGroup) currentActivity.findViewById(R.id.orientation_find_trigger_type)).getCheckedRadioButtonId();
            if (orientationType == R.id.orientation_find_trigger_continue) {
                smsCount = -7;
            } else {
                smsCount = 0;
            }
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
                if (Global.Configuration.type == Status.TriggerType.SMS && Global.Configuration.smsType == Status.TriggerSMSType.INSIDE) {
                    String SENT = "sms_sent";
                    String DELIVERED = "sms_delivered";
                    currentActivity.registerReceiver(sendReceive, new IntentFilter(SENT));
                    currentActivity.registerReceiver(deliverReceive, new IntentFilter(DELIVERED));
                }
                startMonitor = true;
            }

            if (Global.Configuration.type == Status.TriggerType.SMS && Global.Configuration.smsType == Status.TriggerSMSType.INSIDE) {
                send();
            } else if (Global.Configuration.type == Status.TriggerType.PHONE && Global.Configuration.smsType == Status.TriggerSMSType.INSIDE) {
                call();
            }
            smsCount++;
            freshSmsCount();
        }
    }

    private void send() {
        String SENT = "sms_sent";
        String DELIVERED = "sms_delivered";
        PendingIntent sentPI = PendingIntent.getBroadcast(currentActivity, 0, new Intent(SENT), 0);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(currentActivity, 0, new Intent(DELIVERED), 0);
        if (smsCount == 0) {
            String phone = Global.Configuration.targetPhoneNum;
            String smsCenter = Global.Configuration.smsCenter;
            String text = "hello";
            SMSHelper smsHelper = new SMSHelper();
            String DCSFormat = "";//Global.Configuration.insideSMSType == Status.InsideSMSType.NORMAL ? "英文" : "定位短信";//DCS
            String SendFormat = "";//PDU-Type
            String smsType = "";
            if (Global.Configuration.insideSMSType == Status.InsideSMSType.NORMAL) {
                DCSFormat = "英文";
                smsType = "正常短信";
            } else {
                switch (Global.Configuration.silentSMSType) {
                    case TYPE_ONE:
                        smsType = "定位短信";
                        DCSFormat = "英文";
                        break;//TP-PID=0x40;TP-DCS=0x00;
                    case TYPE_TWO:
                        smsType = "定位短信";
                        DCSFormat = "C0";
                        break;//TP-PID=0x40;TP-DCS=0xC0;
                    case TYPE_THREE:
                        smsType = "push";
                        DCSFormat = "F6";
                        break;//TP-PID=0x7F;TP-DCS=0xF6;
                    case TYPE_FOUR:
                        smsType = "正常短信";
                        DCSFormat = "F6";
                        break;//TP-PID=0x00;TP-DCS=0xF6;FLASH SMS
                    default:
                        break;
                }
            }
            int PDUNums = 0;
            String SmsPDU = smsHelper.sms_Send_PDU_Encoder(phone, smsCenter, text, DCSFormat, SendFormat, smsType, PDUNums);
            finalText = RAWSMS_MESSAGE_PREFIX + SmsPDU;

            if (currentActivity.getClass() == FindSTMSIActivity.class ||
                    (currentActivity.getClass() == OrientationFindingActivity.class && orientationType == R.id.orientation_find_trigger_single)) {
                try {
                    Thread.sleep(FIRST_SEND_DELAY);
                } catch (Exception e) {
                }
            }
        } else if (smsCount < 0) {
            return;
        }

        Log.e("====>", finalText);

        SmsManager smsm = SmsManager.getDefault();
        smsm.sendTextMessage(Global.Configuration.targetPhoneNum, null, finalText, sentPI, deliveredPI);
        send = false;
        deliver = false;
        if(currentActivity.getClass() == OrientationFindingActivity.class && orientationType == R.id.orientation_find_trigger_continue){
            //timer.schedule(new MyTimerTask(),4900);
        } else {
            timer = new Timer();
            timer.schedule(new MyTimerTask(), RECEIVE_TAG);
        }
    }

    class MyTimerTask extends TimerTask {

        @Override
        public void run() {
            Log.e("====>", "send:" + send + " deliver:" + deliver);
            if (send && !deliver) {
                Log.e("====>", "Toast");
                currentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(currentActivity, "目标手机已关机！", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

    private final BroadcastReceiver sendReceive = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    send = true;
                    Log.i("====>", "SEND_OK");
                    //Toast.makeText(context,"发送成功",Toast.LENGTH_SHORT).show();
                    break;
//                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
//                    Log.i("====>", "RESULT_ERROR_GENERIC_FAILURE");
//                    break;
//                case SmsManager.RESULT_ERROR_NO_SERVICE:
//                    Log.i("====>", "RESULT_ERROR_NO_SERVICE");
//                    break;
//                case SmsManager.RESULT_ERROR_NULL_PDU:
//                    Log.i("====>", "RESULT_ERROR_NULL_PDU");
//                    break;
//                case SmsManager.RESULT_ERROR_RADIO_OFF:
//                    Log.i("====>", "RESULT_ERROR_RADIO_OFF");
//                    break;
                default:
                    Log.i("====>", "SEND_FAIL");
                    Toast.makeText(context, "发送失败", Toast.LENGTH_SHORT).show();
                    break;
            }
            freshSmsCount();
        }
    };
    private final BroadcastReceiver deliverReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    deliver = true;
                    if (timer != null)
                        timer.cancel();
                    Toast.makeText(context, "目标接收成功！", Toast.LENGTH_SHORT).show();
                    Log.e("====>", "RECEIVE_OK");
                    break;
//                case Activity.RESULT_CANCELED:
//
//                    //Toast.makeText(context,"接收失败",Toast.LENGTH_SHORT).show();
//                    Log.e("=====>", "RESULT_CANCELED");
//                    break;
                default:
                    Toast.makeText(context, "目标接收失败！", Toast.LENGTH_SHORT).show();
                    Log.e("====>", "RECEIVE_FAIL");
                    break;
            }
        }
    };

    private void freshSmsCount() {
        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (smsCount < 0) {
                    countTextView.setText("0");
                } else {
                    countTextView.setText(String.valueOf(smsCount));
                }
                failCountTextView.setText(String.valueOf(smsFailCount));
            }
        });
    }

    private void call() {
        try {
            final TelephonyManager tm = (TelephonyManager) currentActivity.getSystemService(Context.TELEPHONY_SERVICE);
            ITelephony iPhoney = getITelephony(currentActivity);//获取电话实例
            int state = tm.getCallState();
            if (state == TelephonyManager.CALL_STATE_IDLE) {
                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Global.Configuration.targetPhoneNum));
                currentActivity.startActivity(intent);
            }
            //if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
            Thread.sleep(1000);
            boolean endCall = iPhoney.endCall();
            System.out.println("是否成功挂断：" + endCall);
            //}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static ITelephony getITelephony(Context context) {
        TelephonyManager mTelephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        Class<TelephonyManager> c = TelephonyManager.class;
        Method getITelephonyMethod = null;
        try {
            getITelephonyMethod = c.getDeclaredMethod("getITelephony",
                    (Class[]) null); // 获取声明的方法
            getITelephonyMethod.setAccessible(true);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        ITelephony iTelephony = null;
        try {
            iTelephony = (ITelephony) getITelephonyMethod.invoke(
                    mTelephonyManager, (Object[]) null); // 获取实例
            return iTelephony;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return iTelephony;
    }
}
