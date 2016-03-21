package com.lge.emmctest;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.util.Log;

// 테스트 정보 저장 Preference
public class Preference
{
    private static Preference mInstance = null;
    private SharedPreferences mDefPref = null;
    private SharedPreferences.Editor mDefPrefEditor = null;
    
    private final String TAG = "eMMCTestActivity";

    private Preference(Context paramContext)
    {
        this.mDefPref = PreferenceManager
                .getDefaultSharedPreferences(paramContext);
        this.mDefPrefEditor = this.mDefPref.edit();
    }

    public static Preference getInstance(Context paramContext)
    {
        if (mInstance == null)
            mInstance = new Preference(paramContext);
        return mInstance;
    }

    public void loadDefaultSettings()
    {
        setStartStopChecked(false);
        Log.d(TAG, " setStartStopChecked who: loadDefaultSettings false");
        setSleepResumeChecked(true);
        setRandomWriteChecked(true);
        setWriteChecked(false);
        setSWResetChecked(false);

        setSleepTime(10);
        setWakeupTime(10);
        setWriteSize(1024);
        setSWResetTime(1);
        setRepeat(1000);

        setSleepWakeupRepeatedZero();
        setRandomWriteRepeatedZero();
        setWriteRepeatedZero();
        setSWResetRepeatedZero();
    }

    public boolean isSWResetChecked()
    {
        return this.mDefPref.getBoolean("swreset", false);
    }

    public boolean isWriteChecked()
    {
        return this.mDefPref.getBoolean("write", false);
    }

    public boolean isRandomWriteChecked()
    {
        return this.mDefPref.getBoolean("random_write", true);
    }

    public boolean isSleepResumeChecked()
    {
        return this.mDefPref.getBoolean("sleep_resume", true);
    }

    public boolean isStartStopChecked()
    {
        return this.mDefPref.getBoolean("start_stop", false);
    }

    public void setSWResetChecked(boolean paramBoolean)
    {
        this.mDefPrefEditor.putBoolean("swreset", paramBoolean);
        this.mDefPrefEditor.commit();
    }

    public void setWriteChecked(boolean paramBoolean)
    {
        this.mDefPrefEditor.putBoolean("write", paramBoolean);
        this.mDefPrefEditor.commit();
    }

    public void setRandomWriteChecked(boolean paramBoolean)
    {
        this.mDefPrefEditor.putBoolean("random_write", paramBoolean);
        this.mDefPrefEditor.commit();
    }

    public void setSleepResumeChecked(boolean paramBoolean)
    {
        this.mDefPrefEditor.putBoolean("sleep_resume", paramBoolean);
        this.mDefPrefEditor.commit();
    }

    public void setStartStopChecked(boolean paramBoolean)
    {
        this.mDefPrefEditor.putBoolean("start_stop", paramBoolean);
        this.mDefPrefEditor.commit();
    }

    public long getSleepTime()
    {
        return this.mDefPref.getLong("sleep_time", 10);
    }

    public void setSleepTime(long paramLong)
    {
        this.mDefPrefEditor.putLong("sleep_time", paramLong);
        this.mDefPrefEditor.commit();
    }

    public long getWakeupTime()
    {
        return this.mDefPref.getLong("wakeup_time", getSleepTime());
    }

    public void setWakeupTime(long paramLong)
    {
        this.mDefPrefEditor.putLong("wakeup_time", paramLong);
        this.mDefPrefEditor.commit();
    }

    public long getWriteSize()
    {
        return this.mDefPref.getLong("write_size", 1024);
    }

    public void setWriteSize(long paramLong)
    {
        this.mDefPrefEditor.putLong("write_size", paramLong);
        this.mDefPrefEditor.commit();
    }

    public long getSWResetTime()
    {
        return this.mDefPref.getLong("swreset_time", 1);
    }

    public void setSWResetTime(long paramLong)
    {
        this.mDefPrefEditor.putLong("swreset_time", paramLong);
        this.mDefPrefEditor.commit();
    }

    public long getRepeat()
    {
        return this.mDefPref.getLong("repeat", 1000);
    }

    public void setRepeat(long paramLong)
    {
        this.mDefPrefEditor.putLong("repeat", paramLong);
        this.mDefPrefEditor.commit();
    }

    public long getSleepWakeupRepeated()
    {
        return this.mDefPref.getLong("sleepwakeup_repeated", 0);
    }

    public void increaseSleepWakeupRepeated()
    {
        this.mDefPrefEditor.putLong("sleepwakeup_repeated",
                1 + getSleepWakeupRepeated());
        this.mDefPrefEditor.commit();
    }

    public void setSleepWakeupRepeatedZero()
    {
        this.mDefPrefEditor.putLong("sleepwakeup_repeated", 0);
        this.mDefPrefEditor.commit();
    }

    public long getRandomWriteRepeated()
    {
        return this.mDefPref.getLong("randomwrite_repeated", 0);
    }

    public void increaseRandomWriteRepeated()
    {
        this.mDefPrefEditor.putLong("randomwrite_repeated",
                1 + getRandomWriteRepeated());
        this.mDefPrefEditor.commit();
    }

    public void setRandomWriteRepeatedZero()
    {
        this.mDefPrefEditor.putLong("randomwrite_repeated", 0);
        this.mDefPrefEditor.commit();
    }

    public long getWriteRepeated()
    {
        return this.mDefPref.getLong("write_repeated", 0);
    }

    public void increaseWriteRepeated()
    {
        this.mDefPrefEditor.putLong("write_repeated", 1 + getWriteRepeated());
        this.mDefPrefEditor.commit();
    }

    public void setWriteRepeatedZero()
    {
        this.mDefPrefEditor.putLong("write_repeated", 0);
        this.mDefPrefEditor.commit();
    }

    public long getSWResetRepeated()
    {
        return this.mDefPref.getLong("swreset_repeated", 0);
    }

    public void increaseSWResetRepeated()
    {
        this.mDefPrefEditor.putLong("swreset_repeated",
                1 + getSWResetRepeated());
        this.mDefPrefEditor.commit();
    }

    public void setSWResetRepeatedZero()
    {
        this.mDefPrefEditor.putLong("swreset_repeated", 0);
        this.mDefPrefEditor.commit();
    }

    public boolean isRunning()
    {
        return this.mDefPref.getBoolean("running", false);
    }

    public void setRunning(boolean paramBoolean)
    {
        this.mDefPrefEditor.putBoolean("running", paramBoolean);
        this.mDefPrefEditor.commit();
    }

    public void registerOnSharedPreferenceChangeListener(
            OnSharedPreferenceChangeListener listener)
    {
        this.mDefPref.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterOnSharedPreferenceChangeListener(
            OnSharedPreferenceChangeListener listener)
    {
        this.mDefPref.unregisterOnSharedPreferenceChangeListener(listener);
    }
}