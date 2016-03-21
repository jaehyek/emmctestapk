package com.lge.emmctest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.os.SystemProperties;

public class eMMCTestActivity extends Activity implements
        OnCheckedChangeListener, OnSharedPreferenceChangeListener
{
    public final static boolean DEBUG = true;
    private final String TAG = "eMMCTestActivity";
    private String eMMCLifeTAG ;

    // 각 테스트 진행에 사용되는 action
    public static final String START_ACTION = "com.lge.emmctest.TEST_START_ACTION";
    public static final String RESTART_ACTION = "com.lge.emmctest.TEST_RESTART_ACTION";
    public static final String RESUME_ACTION = "com.lge.emmctest.TEST_RESUME_ACTION";
    private final String SLEEP_ACTION = "com.lge.emmctest.EMMC_SLEEP_ACTION";
    private final String WAKEUP_ACTION = "com.lge.emmctest.EMMC_WAKEUP_ACTION";
    private final String RANDOM_WRITE_ACTION = "com.lge.emmctest.EMMC_RANDOM_WRITE_ACTION";
    private final String STOP_RANDOM_WRITE_ACTION = "com.lge.emmctest.EMMC_STOP_RANDOM_WRITE_ACTION";
    private final String WRITE_ACTION = "com.lge.emmctest.EMMC_WRITE_ACTION";
    private final String SWRESET_ACTION = "com.lge.emmctest.EMMC_SWRESET_ACTION";

    // 기능 수행을 위한 시스템 메니저
    private PowerManager.WakeLock wakeLock;
    private PowerManager powerMgr;
    private AlarmManager alarmMgr;

    // 정보 저장 및 레포트 출력 객체
    private Preference preference;
    private eMMCTestReportWriter reportWriter;

    // UI 객체
    private ToggleButton tbSleepResume;
    private ToggleButton tbRandomWrite;
    private ToggleButton tbWrite;
    private ToggleButton tbSWReset;
    private ToggleButton tbStartStop;

    private ToggleButton tbAutorun;

    private EditText etRepeat;

    private TextView tRepeated;
    private ProgressBar pbProgress;

    private LinearLayout llSleepTime;
    private EditText etSleepTime;
    private RadioGroup rgSleepTime;

    private LinearLayout llWakeupTime;
    private EditText etWakeupTime;
    private RadioGroup rgWakeupTime;

    private LinearLayout llWriteSize;
    private EditText etWriteSize;
    private RadioGroup rgWriteSize;

    private LinearLayout llSWResetTime;
    private EditText etSWResetTime;
    private RadioGroup rgSWResetTime;

    // action 발생시 실행될 pending intent
    private PendingIntent sleepIntent;
    private PendingIntent wakeupIntent;
    private PendingIntent randomwriteIntent;
    private PendingIntent stoprandomwriteIntent;
    private PendingIntent writeIntent;
    private PendingIntent swresetIntent;

    private Handler mWriteFileHandler;

    private KeyguardManager.KeyguardLock keyguardLock;
    private KeyguardManager keyguardMgr;

    // 테스트 실행시 발생되는 broadcast 처리
    private BroadcastReceiver mTestControlReceiver = new BroadcastReceiver()
    {
        private boolean isRandomRunning = false;

        public void onReceive(Context paramContext, Intent paramIntent)
        {
            String str = paramIntent.getAction();

            if (DEBUG)
            {
                Log.d(TAG, "mTestControlReceiver : onReceive(), action = "
                        + str);
                if (str.equals(WAKEUP_ACTION))
                {
                    Log.d(TAG,"mTestControlReceiver : onReceive(), repeated = "
                                    + eMMCTestActivity.this.preference.getSleepWakeupRepeated());
                }
                else if (str.equals(RANDOM_WRITE_ACTION))
                {
                    Log.d(TAG,"mTestControlReceiver : onReceive(), repeated = "
                                    + eMMCTestActivity.this.preference.getRandomWriteRepeated());
                }
                else if (str.equals(WRITE_ACTION))
                {
                    Log.d(TAG,"mTestControlReceiver : onReceive(), repeated = "
                                    + eMMCTestActivity.this.preference.getWriteRepeated());
                }
                else if (str.equals(SWRESET_ACTION))
                {
                    Log.d(TAG,"mTestControlReceiver : onReceive(), repeated = "
                                    + eMMCTestActivity.this.preference.getSWResetRepeated());
                }
                
                Log.d(TAG, "mTestControlReceiver : onReceive(), repeat = "
                        + eMMCTestActivity.this.preference.getRepeat());
            }

            if (str.equals(SLEEP_ACTION))
            {
                eMMCTestActivity.this.startSleepResumeTest(true);
            }
            else if (str.equals(WAKEUP_ACTION))
            {
                eMMCTestActivity.this.acquireWakeLock();
                eMMCTestActivity.this.releaseLock();
                eMMCTestActivity.this.reportWriter
                        .writeLog("Event = Wakeup,  Count = "
                                + preference.getSleepWakeupRepeated());
                updateInformation();
                if (eMMCTestActivity.this.preference.getSleepWakeupRepeated() < eMMCTestActivity.this.preference
                        .getRepeat())
                {
                    if (eMMCTestActivity.this.preference.isRandomWriteChecked())
                    {
                        isRandomRunning = true;
                        eMMCTestActivity.this.alarmMgr.set(
                                AlarmManager.RTC_WAKEUP,
                                System.currentTimeMillis() + 1,
                                randomwriteIntent);
                        eMMCTestActivity.this.alarmMgr.set(
                                AlarmManager.RTC_WAKEUP,
                                System.currentTimeMillis()
                                        + (eMMCTestActivity.this.preference
                                                .getWakeupTime() - 1) * 1000,
                                eMMCTestActivity.this.stoprandomwriteIntent);
                    }
                    eMMCTestActivity.this.alarmMgr.set(
                            AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis()
                                    + eMMCTestActivity.this.preference
                                            .getWakeupTime() * 1000,
                            eMMCTestActivity.this.sleepIntent);
                }
                else
                {
                    
                    eMMCTestActivity.this.preference.setRunning(false);
                    eMMCTestActivity.this.preference.setStartStopChecked(false);
                    Log.d(TAG, eMMCLifeTAG + "WakeWrite" + "," + 
                            Long.toString(eMMCTestActivity.this.preference.getRepeat()) ) ;
                    eMMCTestActivity.this.reportWriter.endLogging(3);
                }

            }
            else if (str.equals(RANDOM_WRITE_ACTION))
            {
                if (eMMCTestActivity.this.preference.isSleepResumeChecked())
                {
                    eMMCTestActivity.this.preference.increaseRandomWriteRepeated();
                    writeRandomFile();
                    if (isRandomRunning)
                    {
                        eMMCTestActivity.this.alarmMgr.set(
                                AlarmManager.RTC_WAKEUP,
                                System.currentTimeMillis() + 1,
                                eMMCTestActivity.this.randomwriteIntent);
                    }
                }
                else
                {
                    eMMCTestActivity.this.acquireWakeLock();
                    eMMCTestActivity.this.releaseLock();
                    eMMCTestActivity.this.preference.increaseRandomWriteRepeated();
                    writeRandomFile();
                    updateInformation();

                    if (eMMCTestActivity.this.preference.getRandomWriteRepeated() < eMMCTestActivity.this.preference.getRepeat())
                    {
                        eMMCTestActivity.this.alarmMgr.set(
                                AlarmManager.RTC_WAKEUP,
                                System.currentTimeMillis() + 1,
                                eMMCTestActivity.this.randomwriteIntent);
                    }
                    else
                    {
                        
                        
                        eMMCTestActivity.this.preference.setRunning(false);
                        eMMCTestActivity.this.preference.setStartStopChecked(false);
                        Log.d(TAG, eMMCLifeTAG + "RandWrite" + "," + 
                                Long.toString(eMMCTestActivity.this.preference.getRepeat()) ) ;
                        eMMCTestActivity.this.reportWriter.endLogging(3);
                    }
                }
            }
            else if (str.equals(STOP_RANDOM_WRITE_ACTION))
            {
                eMMCTestActivity.this.reportWriter
                        .writeLog("Event = Stop Random Write, Count = "
                                + preference.getRandomWriteRepeated());
                isRandomRunning = false;
                alarmMgr.cancel(eMMCTestActivity.this.randomwriteIntent);
                alarmMgr.cancel(eMMCTestActivity.this.stoprandomwriteIntent);
            }
            else if (str.equals(WRITE_ACTION))
            {
             /*   mWriteFileHandler.post(new Runnable()
                {

                    @Override
                    public void run()
                    { */
                        eMMCTestActivity.this.acquireWakeLock();
                        eMMCTestActivity.this.releaseLock();
                        eMMCTestActivity.this.preference.increaseWriteRepeated();
                        writeFile(eMMCTestActivity.this.preference.getWriteSize());
                        updateInformation();

                        if (eMMCTestActivity.this.preference.getWriteRepeated() < eMMCTestActivity.this.preference.getRepeat())
                        {
                            eMMCTestActivity.this.alarmMgr.set(
                                    AlarmManager.RTC_WAKEUP,
                                    System.currentTimeMillis() + 1,
                                    eMMCTestActivity.this.writeIntent);
                        }
                        else
                        {
                            eMMCTestActivity.this.preference.setRunning(false);
                            eMMCTestActivity.this.preference.setStartStopChecked(false);
                            Log.d(TAG, eMMCLifeTAG + "SeqWrite" + "," + 
                                    Long.toString(eMMCTestActivity.this.preference.getRepeat()) + "," + 
                                    Long.toString(eMMCTestActivity.this.preference.getWriteSize())  ) ;
                            eMMCTestActivity.this.reportWriter.endLogging(3);
                        }
                   /* }
                }); */
            }
            else if (str.equals(SWRESET_ACTION))
            {
                /*
                 * eMMCTestActivity.this.acquireWakeLock();
                 * eMMCTestActivity.this.preference.increaseSWResetRepeated();
                 * updateInformation(); eMMCTestActivity.this.releaseLock();
                 */
                if (eMMCTestActivity.this.preference.getSWResetRepeated() < eMMCTestActivity.this.preference
                        .getRepeat())
                {
                    eMMCTestActivity.this.preference.increaseSWResetRepeated();
                    eMMCTestActivity.this.reportWriter
                            .writeLog("Event = SW Reset, Count = "
                                    + preference.getSWResetRepeated());
                    eMMCTestActivity.this.powerMgr.reboot("eMMCTest");
                }
                else
                {
                    
                    
                    eMMCTestActivity.this.preference.setRunning(false);
                    eMMCTestActivity.this.preference.setStartStopChecked(false);
                    Log.d(TAG, eMMCLifeTAG + "SWReset" + "," + 
                            Long.toString(eMMCTestActivity.this.preference.getRepeat()) ) ;
                    eMMCTestActivity.this.reportWriter.endLogging(3);
                }
            }
        }
    };

    // 외우 CLI를 통해 들어온 Broadcast 처리
    private void processBroadcast(Intent paramIntent)
    {
        String str = paramIntent.getAction();

        if (DEBUG)
            Log.d(TAG, "processBroadcast(), action = " + str + ", startstop = "
                    + eMMCTestActivity.this.preference.isStartStopChecked());

        if (!eMMCTestActivity.this.preference.isStartStopChecked()
                && str.equals(START_ACTION))
        {
            Log.d(TAG, "START_ACTION");
            initializeRepeated();
            updateInformation();
            preference.setStartStopChecked(true);
            Log.d(TAG, " setStartStopChecked who: processBroadcast:START_ACTION true");
        }
        else if (!eMMCTestActivity.this.preference.isStartStopChecked()
                && str.equals(RESTART_ACTION))
        {
            Log.d(TAG, "RESTART_ACTION");
            updateInformation();
            preference.setStartStopChecked(true);
            Log.d(TAG, " setStartStopChecked who: processBroadcast:RESTART_ACTION true");
        }
        else if (!eMMCTestActivity.this.preference.isStartStopChecked()
                && str.equals(RESUME_ACTION))
        {
            Log.d(TAG, "RESUME_ACTION");
            updateInformation();
            preference.setStartStopChecked(true);
            Log.d(TAG, " setStartStopChecked who: processBroadcast:RESUME_ACTION true");
        }
        else if (!eMMCTestActivity.this.preference.isStartStopChecked()
                && str.equals(Intent.ACTION_MAIN))
        {
            Log.d(TAG, "ACTION_MAIN");
            // updateInformation();
            // preference.setStartStopChecked(true);
        }
    }

    // 기본 객체 초기화
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.emmctestactivity);
        
        String model = Build.MODEL;
        final TelephonyManager tm =(TelephonyManager)getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
        String deviceid = tm.getDeviceId();
        StringBuilder finalStringb =new StringBuilder();
        finalStringb.append("eMMC,").append(model).append(",").append(deviceid).append(",") ;   
        eMMCLifeTAG = finalStringb.toString();

        preference = Preference.getInstance(this);
        preference.setStartStopChecked(false);
        Log.d(TAG, " setStartStopChecked who: onCreate false");
        preference.registerOnSharedPreferenceChangeListener(this);

        reportWriter = eMMCTestReportWriter.getInstance(this);

        powerMgr = ((PowerManager) getSystemService("power"));
        alarmMgr = ((AlarmManager) getSystemService("alarm"));

        this.keyguardMgr = ((KeyguardManager) getSystemService("keyguard"));
        this.keyguardLock = this.keyguardMgr.newKeyguardLock("keyguard");

        tbSleepResume = ((ToggleButton) findViewById(R.id.sleep_resume_test));
        tbSleepResume.setOnCheckedChangeListener(this);
        tbRandomWrite = ((ToggleButton) findViewById(R.id.randomwrite_test));
        tbRandomWrite.setOnCheckedChangeListener(this);
        tbWrite = ((ToggleButton) findViewById(R.id.write_test));
        tbWrite.setOnCheckedChangeListener(this);
        tbSWReset = ((ToggleButton) findViewById(R.id.swreset_test));
        tbSWReset.setOnCheckedChangeListener(this);
        tbStartStop = ((ToggleButton) findViewById(R.id.start_stop));
        // tbStartStop.setOnCheckedChangeListener(this);

        tbAutorun = ((ToggleButton) findViewById(R.id.autorun));
        tbAutorun.setOnCheckedChangeListener(this);

        tRepeated = (TextView) findViewById(R.id.repeated);
        pbProgress = (ProgressBar) findViewById(R.id.progress_bar);
        pbProgress.setMax(100);

        etRepeat = (EditText) findViewById(R.id.repeat);
        etRepeat.setImeOptions(EditorInfo.IME_ACTION_DONE
                | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        etRepeat.setOnEditorActionListener(new EditText.OnEditorActionListener()
        {

            @Override
            public boolean onEditorAction(TextView v, int actionId,
                    KeyEvent event)
            {

                eMMCTestActivity.this.preference.setRepeat(Long.valueOf(
                        v.getText().toString()).longValue());
                return false;
            }
        });

        llSleepTime = (LinearLayout) findViewById(R.id.sleep_time_layout);

        etSleepTime = (EditText) findViewById(R.id.sleep_time);

        etSleepTime
                .setOnEditorActionListener(new EditText.OnEditorActionListener()
                {

                    @Override
                    public boolean onEditorAction(TextView v, int actionId,
                            KeyEvent event)
                    {
                        if (actionId == EditorInfo.IME_ACTION_DONE)
                        {
                            if (Long.valueOf(v.getText().toString()) < 10)
                            {
                                Toast.makeText(eMMCTestActivity.this,
                                        "Minimum of Delay Time is 10 seconds",
                                        Toast.LENGTH_SHORT).show();
                                v.setText("10");
                            }
                            rgSleepTime.check(R.id.sleep_time_custom);
                        }
                        return false;
                    }
                });

        rgSleepTime = (RadioGroup) findViewById(R.id.sleep_time_radiogroup);

        rgSleepTime
                .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
                {

                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId)
                    {
                        Log.d(TAG, "onCheckedChanged checkId = " + checkedId);

                        if (group.getId() == R.id.sleep_time_radiogroup)
                        {
                            switch (checkedId)
                            {
                            case R.id.sleep_time_10_sec:
                                eMMCTestActivity.this.preference
                                        .setSleepTime(10);
                                break;
                            case R.id.sleep_time_20_sec:
                                eMMCTestActivity.this.preference
                                        .setSleepTime(20);
                                break;
                            case R.id.sleep_time_30_sec:
                                eMMCTestActivity.this.preference
                                        .setSleepTime(30);
                                break;
                            case R.id.sleep_time_custom:
                                eMMCTestActivity.this.preference
                                        .setSleepTime(Long.valueOf(
                                                etSleepTime.getText()
                                                        .toString())
                                                .longValue());
                                break;
                            }

                        }
                    }
                });

        llWakeupTime = (LinearLayout) findViewById(R.id.wakeup_time_layout);

        etWakeupTime = (EditText) findViewById(R.id.wakeup_time);

        etWakeupTime
                .setOnEditorActionListener(new EditText.OnEditorActionListener()
                {

                    @Override
                    public boolean onEditorAction(TextView v, int actionId,
                            KeyEvent event)
                    {
                        if (actionId == EditorInfo.IME_ACTION_DONE)
                        {
                            if (Long.valueOf(v.getText().toString()) < 1)
                            {
                                Toast.makeText(eMMCTestActivity.this,
                                        "Minimum of Delay Time is 10 seconds",
                                        Toast.LENGTH_SHORT).show();
                                v.setText("1");
                            }
                            rgWakeupTime.check(R.id.sleep_time_custom);
                        }
                        return false;
                    }
                });

        rgWakeupTime = (RadioGroup) findViewById(R.id.wakeup_time_radiogroup);

        rgWakeupTime
                .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
                {

                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId)
                    {
                        Log.d(TAG, "onCheckedChanged checkId = " + checkedId);

                        if (group.getId() == R.id.wakeup_time_radiogroup)
                        {
                            switch (checkedId)
                            {
                            case R.id.wakeup_time_1_sec:
                                eMMCTestActivity.this.preference
                                        .setWakeupTime(1);
                                break;
                            case R.id.wakeup_time_5_sec:
                                eMMCTestActivity.this.preference
                                        .setWakeupTime(5);
                                break;
                            case R.id.wakeup_time_10_sec:
                                eMMCTestActivity.this.preference
                                        .setWakeupTime(10);
                                break;
                            case R.id.wakeup_time_custom:
                                eMMCTestActivity.this.preference
                                        .setWakeupTime(Long.valueOf(
                                                etWakeupTime.getText()
                                                        .toString())
                                                .longValue());
                                break;
                            }

                        }
                    }
                });

        llWriteSize = (LinearLayout) findViewById(R.id.write_size_layout);

        etWriteSize = (EditText) findViewById(R.id.write_size);

        etWriteSize
                .setOnEditorActionListener(new EditText.OnEditorActionListener()
                {

                    @Override
                    public boolean onEditorAction(TextView v, int actionId,
                            KeyEvent event)
                    {
                        if (actionId == EditorInfo.IME_ACTION_DONE)
                        {
                            if (Long.valueOf(v.getText().toString()) < 1)
                            {
                                Toast.makeText(eMMCTestActivity.this,
                                        "Minimum of Delay Time is 10 seconds",
                                        Toast.LENGTH_SHORT).show();
                                v.setText("1");
                            }
                            rgWriteSize.check(R.id.write_size_custom);
                        }
                        return false;
                    }
                });

        rgWriteSize = (RadioGroup) findViewById(R.id.write_size_radiogroup);

        rgWriteSize
                .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
                {

                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId)
                    {
                        Log.d(TAG, "onCheckedChanged checkId = " + checkedId);

                        if (group.getId() == R.id.write_size_radiogroup)
                        {
                            switch (checkedId)
                            {
                            case R.id.write_size_1024mb:
                                eMMCTestActivity.this.preference
                                        .setWriteSize(1024);
                                break;
                            case R.id.write_size_2048mb:
                                eMMCTestActivity.this.preference
                                        .setWriteSize(2048);
                                break;
                            case R.id.write_size_custom:
                                eMMCTestActivity.this.preference
                                        .setWriteSize(Long.valueOf(
                                                etWriteSize.getText()
                                                        .toString())
                                                .longValue());
                                break;
                            }

                        }
                    }
                });

        llSWResetTime = (LinearLayout) findViewById(R.id.swreset_time_layout);

        etSWResetTime = (EditText) findViewById(R.id.swreset_time);

        etSWResetTime
                .setOnEditorActionListener(new EditText.OnEditorActionListener()
                {

                    @Override
                    public boolean onEditorAction(TextView v, int actionId,
                            KeyEvent event)
                    {
                        if (actionId == EditorInfo.IME_ACTION_DONE)
                        {
                            if (Long.valueOf(v.getText().toString()) < 1)
                            {
                                Toast.makeText(eMMCTestActivity.this,
                                        "Minimum of Delay Time is 1 minutes",
                                        Toast.LENGTH_SHORT).show();
                                v.setText("1");
                            }
                            rgSWResetTime.check(R.id.swreset_time_custom);
                        }
                        return false;
                    }
                });

        rgSWResetTime = (RadioGroup) findViewById(R.id.swreset_time_radiogroup);

        rgSWResetTime
                .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
                {

                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId)
                    {
                        Log.d(TAG, "onCheckedChanged checkId = " + checkedId);

                        if (group.getId() == R.id.swreset_time_radiogroup)
                        {
                            switch (checkedId)
                            {
                            case R.id.swreset_time_1min:
                                eMMCTestActivity.this.preference
                                        .setSWResetTime(1);
                                break;
                            case R.id.swreset_time_2min:
                                eMMCTestActivity.this.preference
                                        .setSWResetTime(2);
                                break;
                            case R.id.swreset_time_3min:
                                eMMCTestActivity.this.preference
                                        .setSWResetTime(3);
                                break;
                            case R.id.swreset_time_custom:
                                eMMCTestActivity.this.preference
                                        .setSWResetTime(Long.valueOf(
                                                etSWResetTime.getText()
                                                        .toString())
                                                .longValue());
                                break;
                            }

                        }
                    }
                });

        updateInformation();

        wakeupIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                WAKEUP_ACTION), PendingIntent.FLAG_CANCEL_CURRENT);
        sleepIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                SLEEP_ACTION), PendingIntent.FLAG_CANCEL_CURRENT);
        randomwriteIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                RANDOM_WRITE_ACTION), PendingIntent.FLAG_CANCEL_CURRENT);
        stoprandomwriteIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                STOP_RANDOM_WRITE_ACTION), PendingIntent.FLAG_CANCEL_CURRENT);
        writeIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                WRITE_ACTION), PendingIntent.FLAG_CANCEL_CURRENT);
        swresetIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                SWRESET_ACTION), PendingIntent.FLAG_CANCEL_CURRENT);

        mWriteFileHandler = new Handler();

        if (preference.isRunning())
            processBroadcast(getIntent());
    }

    @Override
    protected void onDestroy()
    {
        preference.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    protected void onPause()
    {
        if (DEBUG)
            Log.d(TAG, "onPause(), this = " + toString());

        super.onPause();
        this.keyguardLock.reenableKeyguard();
    }

    // Resume시 Screen Lock이 있다면 해제
    protected void onResume()
    {
        if (DEBUG)
            Log.d(TAG, "onResume(), this = " + toString());
        super.onResume();
        // Window win = getWindow();
        // win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
        // WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        this.keyguardLock.disableKeyguard();
        updateInformation();
    }

    // 화면 초기화 메뉴
    public boolean onCreateOptionsMenu(Menu paramMenu)
    {
        super.onCreateOptionsMenu(paramMenu);
        paramMenu.add(0, 1, 0, R.string.emmctest_restore_default_settings);
        return true;
    }

    // 화면 초기화 메뉴 실행
    public boolean onOptionsItemSelected(MenuItem paramMenuItem)
    {
        super.onOptionsItemSelected(paramMenuItem);
        switch (paramMenuItem.getItemId())
        {
        case 1:
            if (!preference.isRunning())
            {
                preference.loadDefaultSettings();
                updateInformation();
            }
            else
            {
                Toast.makeText(this, "Can not restore in testing",
                        Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return false;
    }

    // Sleep Wakeup 테스트 시작
    private void startSleepResumeTest(boolean paramBoolean)
    {
        if (DEBUG)
            Log.d(TAG, "startSleepResumeTest(), restart = " + paramBoolean);

        if (!paramBoolean)
        {
            IntentFilter localIntentFilter = new IntentFilter();
            localIntentFilter.addAction(SLEEP_ACTION);
            localIntentFilter.addAction(WAKEUP_ACTION);
            localIntentFilter.addAction(RANDOM_WRITE_ACTION);
            localIntentFilter.addAction(STOP_RANDOM_WRITE_ACTION);
            registerReceiver(mTestControlReceiver, localIntentFilter);
        }
        alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
                + preference.getSleepTime() * 1000, wakeupIntent);

        eMMCTestActivity.this.preference.increaseSleepWakeupRepeated();
        eMMCTestActivity.this.reportWriter
                .writeLog("Event = Goto Sleep, Count = "
                        + preference.getSleepWakeupRepeated());
        powerMgr.goToSleep(SystemClock.uptimeMillis());
    }

    // Sleep Wakeup 테스트 종료
    private void stopSleepResumeTest()
    {
        if (DEBUG)
            Log.d(TAG, "stopSleepResumeTest()");
        alarmMgr.cancel(this.sleepIntent);
        alarmMgr.cancel(this.wakeupIntent);
        alarmMgr.cancel(this.randomwriteIntent);
        alarmMgr.cancel(this.stoprandomwriteIntent);
        try
        {
            unregisterReceiver(mTestControlReceiver);
        }
        catch (IllegalArgumentException e)
        {

        }

    }

    // Wakeup시 시스템을 깨우기 위한 Wakelock
    private void acquireWakeLock()
    {
//        if (DEBUG)
//            Log.d(TAG, "acquireWakeLock()");
        if (this.wakeLock != null)
            releaseLock();
        wakeLock = powerMgr.newWakeLock/*
                                        * (PowerManager.PARTIAL_WAKE_LOCK,
                                        * "eMMCTest");
                                        */(
                PowerManager.ACQUIRE_CAUSES_WAKEUP
                        | PowerManager.FULL_WAKE_LOCK/*
                                                      * |PowerManager.
                                                      * ON_AFTER_RELEASE
                                                      */, "eMMCTest");
        wakeLock.setReferenceCounted(false);
        if (!wakeLock.isHeld())
            wakeLock.acquire();

        /*
         * getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
         * getWindow
         * ().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
         * getWindow
         * ().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
         * getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
         */
        /*
         * WindowManager.LayoutParams params = getWindow().getAttributes();
         * params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
         * params.flags |= WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
         * params.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
         * params.flags |= WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
         * params.screenBrightness = 0.9f; getWindow().setAttributes(params);
         */
    }

    // Wakelock 해제
    private void releaseLock()
    {
//        if (DEBUG)
//            Log.d(TAG, "releaseLock()");
        if (wakeLock != null)
        {
            if (wakeLock.isHeld())
            {
                wakeLock.release();
            }
            wakeLock = null;
            /*
             * getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
             * ); getWindow().clearFlags(WindowManager.LayoutParams.
             * FLAG_DISMISS_KEYGUARD);
             * getWindow().clearFlags(WindowManager.LayoutParams
             * .FLAG_SHOW_WHEN_LOCKED);
             * getWindow().clearFlags(WindowManager.LayoutParams
             * .FLAG_TURN_SCREEN_ON);
             */

            /*
             * WindowManager.LayoutParams params = getWindow().getAttributes();
             * params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
             * params.flags |= WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
             * params.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
             * params.flags |= WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
             * params.screenBrightness = 0.1f;
             * getWindow().setAttributes(params);
             */
        }
    }

    // RandomWrite 테스트 시작
    private void startRandomWriteTest()
    {
        if (DEBUG)
            Log.d(TAG, "startRandomWriteTest()");
        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction(RANDOM_WRITE_ACTION);
        registerReceiver(this.mTestControlReceiver, localIntentFilter);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1,
                randomwriteIntent);
    }

    // RandomWrite 테스트 종료
    private void stopRandomWriteTest()
    {
        if (DEBUG)
            Log.d(TAG, "stopRandomWriteTest()");
        alarmMgr.cancel(this.randomwriteIntent);

        try
        {
            unregisterReceiver(mTestControlReceiver);
        }
        catch (IllegalArgumentException e)
        {

        }
    }

    // Sequential Write 테스트 시작
    private void startWriteTest()
    {
        if (DEBUG)
            Log.d(TAG, "startWriteTest()");
        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction(WRITE_ACTION);
        registerReceiver(this.mTestControlReceiver, localIntentFilter);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1,
                writeIntent);
    }

    // Sequential Write 테스트 종료
    private void stopWriteTest()
    {
        if (DEBUG)
            Log.d(TAG, "stopWriteTest()");
        alarmMgr.cancel(this.writeIntent);

        try
        {
            unregisterReceiver(mTestControlReceiver);
        }
        catch (IllegalArgumentException e)
        {

        }
    }

    // SW Reset 테스트 시작
    private void startSWResetTest()
    {
        long delay = 1;
        if (DEBUG)
            Log.d(TAG, "startSWResetTest()");
        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction(SWRESET_ACTION);
        registerReceiver(this.mTestControlReceiver, localIntentFilter);
        if ((preference.getSWResetRepeated() > 0)
                && (preference.getSWResetRepeated() < preference.getRepeat()))
            delay = 60 * 1000 * preference.getSWResetTime();

        alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
                + delay, swresetIntent);
    }

    // SW Reset Write 테스트 종료
    private void stopSWResetTest()
    {
        if (DEBUG)
            Log.d(TAG, "stopSWResetTest()");

        alarmMgr.cancel(this.swresetIntent);

        try
        {
            unregisterReceiver(mTestControlReceiver);
        }
        catch (IllegalArgumentException e)
        {

        }
    }

    // 각 테스트 설정 버튼 이벤트 처리기
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        if (DEBUG)
            Log.d(TAG, "onCheckedChanged(): id = " + buttonView.getId()
                    + ", isChecked = " + isChecked);

        switch (buttonView.getId())
        {
        case R.id.start_stop:
            if (!eMMCTestActivity.this.preference.isWriteChecked()
                    && !eMMCTestActivity.this.preference.isSWResetChecked()
                    && (!eMMCTestActivity.this.preference
                            .isSleepResumeChecked() && !eMMCTestActivity.this.preference
                            .isRandomWriteChecked()))
            {
                Toast.makeText(this, "Should select any test type",
                        Toast.LENGTH_SHORT).show();
                this.tbStartStop.setChecked(false);
                return;
            }

            if (isChecked)
            {
                initializeRepeated();
                eMMCTestActivity.this.reportWriter.startLogging(1);
            }
            else
            {
                eMMCTestActivity.this.reportWriter.endLogging(1);
            }

            eMMCTestActivity.this.preference.setRunning(isChecked);
            eMMCTestActivity.this.preference.setStartStopChecked(isChecked);
            Log.d(TAG, " setStartStopChecked who: onCheckedChanged " + isChecked);
            // updateInformation();
            break;
        case R.id.sleep_resume_test:
            eMMCTestActivity.this.preference.setSleepResumeChecked(isChecked);
            updateInformation();
            break;
        case R.id.randomwrite_test:
            eMMCTestActivity.this.preference.setRandomWriteChecked(isChecked);
            break;
        case R.id.write_test:
            eMMCTestActivity.this.preference.setWriteChecked(isChecked);
            updateInformation();
            break;
        case R.id.swreset_test:
            eMMCTestActivity.this.preference.setSWResetChecked(isChecked);
            updateInformation();
            break;
        }
    }

    // 각 테스트 반복 횟수 초기화
    private void initializeRepeated()
    {
        if (DEBUG)
            Log.d(TAG, "initialPreference()");
        preference.setSleepWakeupRepeatedZero();
        preference.setRandomWriteRepeatedZero();
        preference.setWriteRepeatedZero();
        preference.setSWResetRepeatedZero();
    }

    // UI 업데이트 (이벤트 처리 방지)
    private void updateInformation()
    {
//        if (DEBUG)
//            Log.d(TAG, "updateInformation()");

        tbStartStop.setOnCheckedChangeListener(null);

        tbSleepResume.setChecked(preference.isSleepResumeChecked());
        tbRandomWrite.setChecked(preference.isRandomWriteChecked());
        tbWrite.setChecked(preference.isWriteChecked());
        tbSWReset.setChecked(preference.isSWResetChecked());

        if (preference.isStartStopChecked())
        {
            tbSleepResume.setEnabled(false);
            tbRandomWrite.setEnabled(false);
            tbWrite.setEnabled(false);
            tbSWReset.setEnabled(false);
        }
        else if (preference.isWriteChecked())
        {
            tbSleepResume.setEnabled(false);
            tbRandomWrite.setEnabled(false);
            tbWrite.setEnabled(true);
            tbSWReset.setEnabled(false);

            llSleepTime.setVisibility(LinearLayout.GONE);
            llWakeupTime.setVisibility(LinearLayout.GONE);
            llWriteSize.setVisibility(LinearLayout.VISIBLE);
            llSWResetTime.setVisibility(LinearLayout.GONE);
        }
        else if (preference.isSWResetChecked())
        {
            tbSleepResume.setEnabled(false);
            tbRandomWrite.setEnabled(false);
            tbWrite.setEnabled(false);
            tbSWReset.setEnabled(true);

            llSleepTime.setVisibility(LinearLayout.GONE);
            llWakeupTime.setVisibility(LinearLayout.GONE);
            llWriteSize.setVisibility(LinearLayout.GONE);
            llSWResetTime.setVisibility(LinearLayout.VISIBLE);
        }
        else
        {
            tbSleepResume.setEnabled(true);
            tbRandomWrite.setEnabled(true);
            tbWrite.setEnabled(true);
            tbSWReset.setEnabled(true);

            llSleepTime
                    .setVisibility(preference.isSleepResumeChecked() ? LinearLayout.VISIBLE
                            : LinearLayout.GONE);
            llWakeupTime
                    .setVisibility(preference.isSleepResumeChecked() ? LinearLayout.VISIBLE
                            : LinearLayout.GONE);
            llWriteSize.setVisibility(LinearLayout.GONE);
            llSWResetTime.setVisibility(LinearLayout.GONE);
        }

        etSleepTime.setText(String.valueOf(preference.getSleepTime()));

        if (preference.getSleepTime() == 10)
        {
            rgSleepTime.check(R.id.sleep_time_10_sec);
        }
        else if (preference.getSleepTime() == 20)
        {
            rgSleepTime.check(R.id.sleep_time_20_sec);
        }
        else if (preference.getSleepTime() == 30)
        {
            rgSleepTime.check(R.id.sleep_time_30_sec);
        }
        else
        {
            rgSleepTime.check(R.id.sleep_time_custom);
        }

        etWakeupTime.setText(String.valueOf(preference.getWakeupTime()));

        if (preference.getWakeupTime() == 1)
        {
            rgWakeupTime.check(R.id.wakeup_time_1_sec);
        }
        else if (preference.getWakeupTime() == 5)
        {
            rgWakeupTime.check(R.id.wakeup_time_5_sec);
        }
        else if (preference.getWakeupTime() == 10)
        {
            rgWakeupTime.check(R.id.wakeup_time_10_sec);
        }
        else
        {
            rgWakeupTime.check(R.id.wakeup_time_custom);
        }

        etWriteSize.setText(String.valueOf(preference.getWriteSize()));

        if (preference.getWriteSize() == 1024)
        {
            rgWriteSize.check(R.id.write_size_1024mb);
        }
        else if (preference.getWriteSize() == 2048)
        {
            rgWriteSize.check(R.id.write_size_2048mb);
        }
        else
        {
            rgWriteSize.check(R.id.write_size_custom);
        }

        etSWResetTime.setText(String.valueOf(preference.getSWResetTime()));

        if (preference.getSWResetTime() == 1)
        {
            rgSWResetTime.check(R.id.swreset_time_1min);
        }
        else if (preference.getSWResetTime() == 2)
        {
            rgSWResetTime.check(R.id.swreset_time_2min);
        }
        else if (preference.getSWResetTime() == 3)
        {
            rgSWResetTime.check(R.id.swreset_time_3min);
        }
        else
        {
            rgSWResetTime.check(R.id.write_size_custom);
        }

        etRepeat.setText(String.valueOf(preference.getRepeat()));
        if (preference.isWriteChecked())
        {
            tRepeated.setText(String.valueOf(preference.getWriteRepeated()));
            pbProgress.setProgress(Long.valueOf(
                    preference.getWriteRepeated() * 100
                            / preference.getRepeat()).intValue());
        }
        else if (preference.isSWResetChecked())
        {
            tRepeated.setText(String.valueOf(preference.getSWResetRepeated()));
            pbProgress.setProgress(Long.valueOf(
                    preference.getSWResetRepeated() * 100
                            / preference.getRepeat()).intValue());
        }
        else if (preference.isSleepResumeChecked())
        {
            tRepeated.setText(String.valueOf(preference
                    .getSleepWakeupRepeated()));
            pbProgress.setProgress(Long.valueOf(
                    preference.getSleepWakeupRepeated() * 100
                            / preference.getRepeat()).intValue());
        }
        else
        {
            tRepeated.setText(String.valueOf(preference
                    .getRandomWriteRepeated()));
            pbProgress.setProgress(Long.valueOf(
                    preference.getRandomWriteRepeated() * 100
                            / preference.getRepeat()).intValue());
        }

        tbStartStop.setChecked(preference.isStartStopChecked());

        tbStartStop.setOnCheckedChangeListener(this);
    }

    // RandomWrite file 쓰기
    void writeRandomFile()
    {
        if (DEBUG)
            Log.d(TAG, "writeRandomFile()");

        char data[] = new char[1024];

        String working = makeWorkingDir();
        // create the file  1000 times, and delete all those .
        try
        {
            int i ; 
            
            for( i =0 ; i < 1024 ; i++)
            {
                String filename = working + "/test" + String.format("%04d", i);
                
                File file = new File(filename);
                if (file.exists())
                    deleteWriteFile(filename);
            }
            
            for ( i = 0 ; i < 1024 ; i ++ )
            {
                String filename = working + "/test" + String.format("%04d", i);
                File file = new File(filename);
                FileWriter writer = new FileWriter(file, file.exists());
                writer.write(data, 0, 1024);
                writer.flush();
                writer.close();
            }
            

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        
        

        /*
        String filename = working + "/test-file";
        File file = new File(filename);

        // Ian: K ->M ->1G시 삭제
        // Ian: 20140224: 1G(1024*1024*1024) -> 100M로 변경
        if (file.exists() && file.length() >= 100 * 1024 * 1024)
        {
            eMMCTestActivity.this.reportWriter
                    .writeLog("Event = Delete Random Write File, size = "
                            + file.length() / 1024 + " KB");
            deleteWriteFile(filename);
        }

        try
        {
            FileWriter writer = new FileWriter(file, file.exists());
            writer.write(data, 0, 1024);
            writer.flush();
            writer.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        */

        eMMCTestActivity.this.reportWriter
                .writeLog("Event = Random Write, Count = "
                        + preference.getRandomWriteRepeated());
    }

    // Sequential Write file 쓰기
    void writeFile(long size)
    {
        if (DEBUG)
            Log.d(TAG, "writeFile()");

        char data[] = new char[1024 * 1024];

        String working = makeWorkingDir();

        String filename = working + "/test-file";
        File file = new File(filename);

        if (file.exists())
        {
            deleteWriteFile(filename);
        }

        try
        {
            FileWriter writer = new FileWriter(file, file.exists());
            BufferedWriter bufferedWriter = new BufferedWriter(writer,
                    1024 * 1024);
            for (long i = 0; i < size; i++)
            {
                bufferedWriter.write(data, 0, 1024 * 1024);
            }
            bufferedWriter.flush();
            bufferedWriter.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        eMMCTestActivity.this.reportWriter
                .writeLog("Event = Sequential Write, Count = "
                        + preference.getWriteRepeated() + ", Write Size = "
                        + preference.getWriteSize() + "MB");
    }

    // 테스트 시작
    void startTest()
    {
        if (DEBUG)
            Log.d(TAG,
                    "startTest(): sleep = " + preference.isSleepResumeChecked()
                            + ", random = " + preference.isRandomWriteChecked()
                            + ", write = " + preference.isWriteChecked()
                            + ", swreset = " + preference.isSWResetChecked());

        String filename = makeWorkingDir() + "/test-file";

        deleteWriteFile(filename);

        if (preference.isWriteChecked())
        {
            startWriteTest();
        }
        else if (preference.isSWResetChecked())
        {
            startSWResetTest();
        }
        else if (preference.isSleepResumeChecked())
        {
            startSleepResumeTest(false);
        }
        else if (preference.isRandomWriteChecked())
        {
            startRandomWriteTest();
        }
    }

    // 테스트 종료
    void stopTest()
    {
        if (DEBUG)
            Log.d(TAG,
                    "stopTest(): sleep = " + preference.isSleepResumeChecked()
                            + ", random = " + preference.isRandomWriteChecked()
                            + ", write = " + preference.isWriteChecked()
                            + ", swreset = " + preference.isSWResetChecked());

        if (preference.isWriteChecked())
        {
            stopWriteTest();
        }
        else if (preference.isSWResetChecked())
        {
            stopSWResetTest();
        }
        else if (preference.isSleepResumeChecked())
        {
            stopSleepResumeTest();
        }
        else if (preference.isRandomWriteChecked())
        {
            stopRandomWriteTest();
        }
        
        
    }

    // preference 변경 이벤트 처리기
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key)
    {

        if (DEBUG)
        {
//            Log.d(TAG,"startTest(): "+ SystemProperties.get("ro.product.manufacturer"));
            
            if (key.equals("running"))
                Log.d(TAG, "key value: " + key + " value:" + preference.isRunning()) ;
            else if (key.equals("start_stop"))
                Log.d(TAG, "key value: " + key + " value:" + preference.isStartStopChecked()) ;
            else 
                Log.d(TAG, "key value: " + key ) ;
        }

        if (SystemProperties.get("ro.product.manufacturer").indexOf("LG") < 0)
        {

            Toast.makeText(eMMCTestActivity.this,
                    "LGE Device Only Use This Tool", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ian: start_stop 버튼만 처리
        if (key.equals("start_stop"))
        {
            updateInformation();
            if (preference.isStartStopChecked())
            {
                startTest();
            }
            else
            {
                stopTest();
            }
        }

    }

    // 파일 삭제
    private void deleteWriteFile(String filename)
    {
        File file = new File(filename);

        if (file.exists())
        {
            file.delete();
        }
    }

    // working 디렉토리 생성
    private String makeWorkingDir()
    {
        String working = getFilesDir().getAbsolutePath() + "/working";
        File workingDir = new File(working);
        if (!workingDir.exists())
            workingDir.mkdir();

        return working;
    }
}
