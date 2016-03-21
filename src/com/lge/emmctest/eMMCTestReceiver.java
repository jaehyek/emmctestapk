package com.lge.emmctest;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

// CLI로 Broadcast 전송시 처리
public class eMMCTestReceiver extends BroadcastReceiver
{
    private final String TAG = "eMMCTestActivity";

    public static final String END_ACTION = "com.lge.emmctest.TEST_END_ACTION";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String str = intent.getAction();
        Preference preference = Preference.getInstance(context);
        eMMCTestReportWriter reportWriter = eMMCTestReportWriter
                .getInstance(context);

        if (eMMCTestActivity.DEBUG)
            Log.d(TAG, "eMMCTestReceiver:onReceive(), action = " + str);

        if (str.equals(eMMCTestActivity.START_ACTION))
        {
            reportWriter.startLogging(2);
            if (intent.getBooleanExtra("sleepwakeup", false)
                    && intent.getLongExtra("sleep_time", 10) < 10)
            {
                reportWriter.writeLog("Delay time is over 10 seconds ("
                        + intent.getLongExtra("sleep_time", 10) + ")");
                reportWriter.endLogging(4);
                return;
            }

            KeyguardManager manager = (KeyguardManager) context
                    .getSystemService(Context.KEYGUARD_SERVICE);

            if (manager.inKeyguardRestrictedInputMode())
            {
                KeyguardLock lock = manager
                        .newKeyguardLock(Context.KEYGUARD_SERVICE);
                lock.disableKeyguard();
                manager.exitKeyguardSecurely(null);
            }

            preference.setRunning(true);
            preference.setSleepResumeChecked(intent.getBooleanExtra(
                    "sleepwakeup", preference.isSleepResumeChecked()));
            preference.setRandomWriteChecked(intent.getBooleanExtra(
                    "randomwrite", preference.isRandomWriteChecked()));
            preference.setWriteChecked(intent.getBooleanExtra(
                    "sequentialwrite", preference.isWriteChecked()));
            preference.setSWResetChecked(intent.getBooleanExtra("swreset",
                    preference.isSWResetChecked()));

            preference.setSleepTime(intent.getLongExtra("sleep_time",
                    preference.getSleepTime()));
            preference.setWakeupTime(intent.getLongExtra("wakeup_time",
                    preference.getWakeupTime()));
            preference.setRepeat(intent.getLongExtra("repeat",
                    preference.getRepeat()));
            preference.setWriteSize(intent.getLongExtra("file_size",
                    preference.getWriteSize()));
            preference.setSWResetTime(intent.getLongExtra("swreset_time",
                    preference.getSWResetTime()));

            Intent newIntent = new Intent(eMMCTestActivity.START_ACTION);
            newIntent.setComponent(new ComponentName(context,
                    eMMCTestActivity.class));
            newIntent.addCategory("android.intent.category.DEFAULT");
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(newIntent);
        }
        else if (str.equals(eMMCTestActivity.RESTART_ACTION))
        {
            reportWriter.startLogging(3);
            intent.setComponent(new ComponentName(context,
                    eMMCTestActivity.class));
            intent.addCategory("android.intent.category.DEFAULT");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        }
        else if (str.equals(eMMCTestActivity.RESUME_ACTION))
        {
            reportWriter.startLogging(4);
            intent.setComponent(new ComponentName(context,
                    eMMCTestActivity.class));
            intent.addCategory("android.intent.category.DEFAULT");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        }
        else if (str.equals(END_ACTION))
        {
            preference.setRunning(false);
            preference.setStartStopChecked(false);
            Log.d(TAG, " setStartStopChecked who: Receiver:onReceive false");
            reportWriter.endLogging(2);
        }
    }

}
