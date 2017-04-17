package com.recurrence.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.NumberPicker;


import com.recurrence.receivers.SnoozeReceiver;
import com.recurrence.utils.AlarmUtil;
import com.recurrence.utils.NotificationUtil;
import com.byagowi.persiancalendar.R;

import java.util.Calendar;

public class SnoozeDialogActivity extends AppCompatActivity {

    private NumberPicker mHourPicker;
    private NumberPicker mMinutePicker;
    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final int reminderId = getIntent().getIntExtra("NOTIFICATION_ID", 0);

        View view = getLayoutInflater().inflate(R.layout.number_picker_recurence, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Dialog);
        builder.setTitle(R.string.snooze_length);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mHourPicker = (NumberPicker) view.findViewById(R.id.picker1);
        mMinutePicker = (NumberPicker) view.findViewById(R.id.picker2);

        setUpHourPicker();
        setUpMinutePicker();

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mHourPicker.getValue() != 0 || mMinutePicker.getValue() != 0) {
                    NotificationUtil.cancelNotification(getApplicationContext(), reminderId);

                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.MINUTE, mMinutePicker.getValue());
                    calendar.add(Calendar.HOUR, mHourPicker.getValue());
                    Intent alarmIntent = new Intent(getApplicationContext(), SnoozeReceiver.class);
                    AlarmUtil.setAlarm(getApplicationContext(), alarmIntent, reminderId, calendar);

                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putInt("snoozeHours", mHourPicker.getValue());
                    editor.putInt("snoozeMinutes", mMinutePicker.getValue());
                    editor.apply();
                }
                finish();
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                finish();
            }
        });

        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });

        builder.setView(view).create().show();
    }

    public void setUpHourPicker() {
        mHourPicker.setMinValue(0);
        mHourPicker.setMaxValue(24);
        mHourPicker.setValue(mSharedPreferences.getInt("snoozeHours", getResources().getInteger(R.integer.default_snooze_hours)));

        String[] hourValues = new String[25];
        for (int i = 0; i < hourValues.length; i++) {
            hourValues[i] = String.format(getResources().getQuantityString(R.plurals.time_hour, i), i);
        }
        mHourPicker.setDisplayedValues(hourValues);
    }

    public void setUpMinutePicker() {
        mMinutePicker.setMinValue(0);
        mMinutePicker.setMaxValue(60);
        mMinutePicker.setValue(mSharedPreferences.getInt("snoozeMinutes", getResources().getInteger(R.integer.default_snooze_minutes)));

        String[] minuteValues = new String[61];
        for (int i = 0; i < minuteValues.length; i++) {
            minuteValues[i] = String.format(getResources().getQuantityString(R.plurals.time_minute, i), i);
        }
        mMinutePicker.setDisplayedValues(minuteValues);
    }
}