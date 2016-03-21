package com.lge.emmctest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.util.Log;

//각종 로그 출력
public class eMMCTestReportWriter
{

    private static eMMCTestReportWriter mInstance = null;
    private Context mContext;
    private String mLogFileName;
    private Date start, end;
    private int mTestType;

    public eMMCTestReportWriter(Context context)
    {
        mContext = context;
        mLogFileName = null;
        mTestType = 0;
    }

    public static eMMCTestReportWriter getInstance(Context paramContext)
    {
        if (mInstance == null)
            mInstance = new eMMCTestReportWriter(paramContext);
        return mInstance;
    }

    public boolean startLogging(int byApp)
    {
        String by = null;
        mTestType = byApp;

        switch (byApp)
        {
        case 1:
            by = " Start by App";
            break;
        case 2:
            by = " Start by Broadcast";
            break;
        case 3:
            by = " Resume by Autorun";
            break;
        case 4:
            by = " Resume by Broadcast";
            break;
        default:
            by = " Start by Error";
            break;
        }

        return (getTestType() == null) ? false : writeLog(getTestType() + by);
    }

    public boolean endLogging(int byApp)
    {
        boolean ret = false;
        String by = null;

        switch (byApp)
        {
        case 1:
            by = "by App";
            break;
        case 2:
            by = "by Broadcast";
            break;
        case 3:
            by = "by Repeat";
            break;
        default:
            by = "by Error";
            break;
        }

        ret = writeLog(getTestType() + " End " + by);
        end = new Date();
        printSummary();
        mLogFileName = null;

        return ret;
    }

    public void makeFileName()
    {
        File file;
        start = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HH_mm_ss");
        String currentTime = dateFormat.format(start);

        mLogFileName = mContext.getFilesDir().getAbsolutePath() + "/log/"
                + currentTime + ".log";

        do
        {
            file = new File(mLogFileName);
        }
        while (file.exists());
    }

    public void createLogDir()
    {
        String log = mContext.getFilesDir().getAbsolutePath() + "/log";
        File logDir = new File(log);
        if (!logDir.exists())
            logDir.mkdir();
    }

    public boolean checkLogFileOverSize(long size)
    {
        File file = new File(mLogFileName);

        if (file.length() > size)
            return true;
        return false;
    }

    public boolean writeLog(String message)
    {
        File file = null;

        if (message == null)
            return false;

        createLogDir();

        if (mLogFileName == null)
        {
            makeFileName();
        }
        else
        {
            file = new File(mLogFileName);
            if ((mTestType == 2) && checkLogFileOverSize(100 * 1024 * 1024))
            {
                file.delete();
                makeFileName();
            }
        }

        file = new File(mLogFileName);

        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss.SSS");
        String currentTime = dateFormat.format(new Date());

        try
        {
            FileWriter writer = new FileWriter(file, file.exists());
            writer.write("[");
            writer.write(currentTime);
            writer.write("]");
            writer.write(" ");
            writer.write(message);
            writer.write("\n");
            writer.flush();
            writer.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }

        Log.i("eMMCTest", message);

        return true;
    }

    private void printSummary()
    {
        Preference preference = Preference.getInstance(mContext);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss");

        String startString = dateFormat.format(start);
        String endString = dateFormat.format(end);
        long duration = end.getTime() - start.getTime();
        String testTime = String.format("%02d:%02d:%02d",
                (duration / (1000 * 60 * 60)), (duration / (1000 * 60)) % 60,
                (duration / 1000) % 60);

        File file = new File(mLogFileName);

        try
        {
            FileWriter writer = new FileWriter(file, file.exists());

            writer.write("##################### Summary #####################\n");
            writer.write("Start Time : " + startString + "\n");
            writer.write("End Time : " + endString + "\n");
            writer.write("Test Time : " + testTime + "\n");
            writer.write("Test Type : " + getTestType() + "\n");

            if (preference.isWriteChecked())
            {
                writer.write("Sequential Write File Size (MB) : "
                        + preference.getWriteSize() + "\n");
                writer.write("Sequential Write Repeated Count : "
                        + preference.getWriteRepeated());
                writer.write("/" + preference.getRepeat() + "\n");
            }
            else if (preference.isSWResetChecked())
            {
                writer.write("SW Reset Interval Time (Min) : "
                        + preference.getSWResetTime() + "\n");
                writer.write("SW Reset Repeated Count : "
                        + preference.getSWResetRepeated());
                writer.write("/" + preference.getRepeat() + "\n");
            }
            else if (preference.isSleepResumeChecked())
            {
                writer.write("Sleep Delay Time (s) : "
                        + preference.getSleepTime() + "\n");
                writer.write("Wakeup Delay Time (s) : "
                        + preference.getWakeupTime() + "\n");
                writer.write("Sleep and Wakeup Repeated Count : "
                        + preference.getSleepWakeupRepeated());
                writer.write("/" + preference.getRepeat() + "\n");
                if (preference.isRandomWriteChecked())
                {
                    writer.write("Random Write Repeated Count : "
                            + preference.getRandomWriteRepeated() + "\n");
                }
            }
            else if (preference.isRandomWriteChecked())
            {
                writer.write("Random Write Repeated Count : "
                        + preference.getRandomWriteRepeated());
                writer.write("/" + preference.getRepeat() + "\n");
            }

            writer.write("###################################################\n");
            writer.flush();
            writer.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private String getTestType()
    {
        Preference preference = Preference.getInstance(mContext);

        if (preference.isWriteChecked())
        {
            return "Sequential Write Test";
        }
        else if (preference.isSWResetChecked())
        {
            return "SW Reset Test";
        }
        else if (preference.isSleepResumeChecked()
                && preference.isRandomWriteChecked())
        {
            return "Sleep & Resume with Random Write Test";
        }
        else if (preference.isSleepResumeChecked())
        {
            return "Sleep & Resume Test";
        }
        else if (preference.isRandomWriteChecked())
        {
            return "Random Write Test";
        }

        return null;
    }
}
