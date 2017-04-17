package com.outlay.view.dialog;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.widget.DatePicker;

import com.byagowi.persiancalendar.R;
import com.outlay.utils.DeviceUtils;

import java.util.Calendar;

/**
 * Created by Bogdan Melnychuk on 1/29/16.
 */
public class DatePickerFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener {

    private DatePickerDialog.OnDateSetListener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //current date as the default date in the picker
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(getActivity(), R.style.DatePicker, this, year, month, day);
        if (DeviceUtils.supportV5()) {
            dialog.getDatePicker().setFirstDayOfWeek(Calendar.MONDAY);
        } else {
            //dialog.getDatePicker().getCalendarView().setFirstDayOfWeek(Calendar.MONDAY);
        }
        return dialog;
    }

    public void setOnDateSetListener(DatePickerDialog.OnDateSetListener listener) {
        this.listener = listener;
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        if (listener != null) {
            listener.onDateSet(view, year, monthOfYear, dayOfMonth);
        }
    }

}
