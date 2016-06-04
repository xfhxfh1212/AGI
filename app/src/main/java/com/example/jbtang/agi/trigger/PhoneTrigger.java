package com.example.jbtang.agi.trigger;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.ITelephony;
import com.example.jbtang.agi.core.Global;
import com.example.jbtang.agi.core.Status;
import java.lang.reflect.Method;
/**
 * Created by jbtang on 12/4/2015.
 */
public class PhoneTrigger implements Trigger {

    private static final PhoneTrigger instance = new PhoneTrigger();

    private PhoneTrigger() {
    }

    public static PhoneTrigger getInstance() {
        return instance;
    }


    @Override
    public void start(Activity activity, Status.Service service) {
        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Global.Configuration.targetPhoneNum));
        activity.startActivity(intent);
        final TelephonyManager tm=(TelephonyManager)activity.getSystemService(Context.TELEPHONY_SERVICE);
        ITelephony iPhoney=getITelephony(activity);//获取电话实例

    }

    @Override
    public void stop() {

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
        ITelephony iTelephony=null;
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
