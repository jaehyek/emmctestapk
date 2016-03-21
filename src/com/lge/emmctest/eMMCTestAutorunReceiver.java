package com.lge.emmctest;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

// 재부팅시 자동실행 이벤트 처리기
public class eMMCTestAutorunReceiver extends BroadcastReceiver
{
    private final String TAG = "eMMCTestAutorunReceiver";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String str = intent.getAction();
        Preference preference = Preference.getInstance(context);

        if (eMMCTestActivity.DEBUG)
            Log.d(TAG, "eMMCTestAutorunReceiver:onReceive(), action = " + str);

        if (str.equals("android.intent.action.BOOT_COMPLETED"))
        {
            if (preference.isRunning() && preference.isSWResetChecked())
            {
                KeyguardManager manager = (KeyguardManager) context
                        .getSystemService(Context.KEYGUARD_SERVICE);

                if (manager.inKeyguardRestrictedInputMode())
                {
                    KeyguardLock lock = manager
                            .newKeyguardLock(Context.KEYGUARD_SERVICE);
                    lock.disableKeyguard();
                    manager.exitKeyguardSecurely(null);
                }

                AlarmManager alarmMgr = ((AlarmManager) context
                        .getSystemService("alarm"));
                Intent broadcastIntent = new Intent(
                        eMMCTestActivity.RESTART_ACTION);
                broadcastIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                context.sendBroadcast(broadcastIntent);
                // PendingIntent restartIntent =
                // PendingIntent.getBroadcast(context, 0, broadcastIntent,
                // PendingIntent.FLAG_CANCEL_CURRENT);

                // alarmMgr.set(AlarmManager.RTC_WAKEUP,
                // System.currentTimeMillis() + 1, restartIntent);
            }
        }
    }

}
