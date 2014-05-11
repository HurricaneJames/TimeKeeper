package com.easytimelog.timekeeper.views;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;

public class TimerButton extends Button {
    public TimerButton(Context context) { super(context); }
    public TimerButton(Context context, AttributeSet attrs) { super(context, attrs); }
    public TimerButton(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); }

    private boolean mRunning = false;
    private long mBaseDuration = 0;
    private DateTime mStartAt = new DateTime();

    public boolean isRunning() { return mRunning; }
    public void start() {
        mRunning = true;
        getBackground().setColorFilter(0xff00ff00, PorterDuff.Mode.MULTIPLY);
    }
    public void stop() {
        mBaseDuration = getTotalDuration();
        mRunning = false;
        getBackground().setColorFilter(0xffff0000, PorterDuff.Mode.MULTIPLY);
    }

    public void updateTime()   { if(mRunning) { displayTime(); } }
    private void displayTime() { setText(DateFormatter.DEFAULT.print(new Period(getTotalDuration()))); }
    public void reset() {
        mRunning = false;
        mStartAt = new DateTime();
        mBaseDuration = 0;
        displayTime();
    }

    private Duration getDurationSinceStart() {
        return new Duration(mStartAt, new DateTime());
    }

    private long getTotalDuration() {
        if(mRunning) {
            return getDurationSinceStart().plus(mBaseDuration).getMillis();
        }else {
            return mBaseDuration;
        }
    }

    public void setBaseDuration(long durationMillis)   { mBaseDuration = durationMillis; displayTime(); }
    public void setBaseStartTime(String startTimeFrom) { mStartAt = new DateTime(startTimeFrom); displayTime(); }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(TimerButton.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(TimerButton.class.getName());
    }
}
