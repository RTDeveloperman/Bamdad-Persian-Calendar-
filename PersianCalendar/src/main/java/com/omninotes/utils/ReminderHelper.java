/*
 * Copyright (C) 2015 Federico Iosue (federico.iosue@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.omninotes.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.widget.Toast;

import java.util.Calendar;

import com.byagowi.persiancalendar.R;
import com.byagowi.persiancalendar.view.activity.MainActivity;
import com.omninotes.OmniNotes;
import com.omninotes.models.Note;
import com.omninotes.receiver.AlarmReceiver;
import com.omninotes.utils.date.DateHelper;


public class ReminderHelper {

	public static void addReminder(Context context, Note note) {
		if (note.getAlarm() != null) {
			addReminder(context, note, Long.parseLong(note.getAlarm()));
		}
	}


	public static void addReminder(Context context, Note note, long reminder) {
		Intent intent = new Intent(context, AlarmReceiver.class);
		intent.putExtra(Constants.INTENT_NOTE, (android.os.Parcelable) note);
		PendingIntent sender = PendingIntent.getBroadcast(context, note.getCreation().intValue(), intent,
				PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			am.setExact(AlarmManager.RTC_WAKEUP, reminder, sender);
		} else {
			am.set(AlarmManager.RTC_WAKEUP, reminder, sender);
		}
	}


	public static void removeReminder(Context context, Note note) {
		if (!TextUtils.isEmpty(note.getAlarm())) {
			AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			Intent intent = new Intent(context, AlarmReceiver.class);
			PendingIntent p = PendingIntent.getBroadcast(context, note.getCreation().intValue(), intent, 0);
			am.cancel(p);
			p.cancel();
		}
	}


	public static void showReminderMessage(String reminderString) {
		if (reminderString != null) {
			long reminder = Long.parseLong(reminderString);
			if (reminder > Calendar.getInstance().getTimeInMillis()) {
				new Handler(OmniNotes.getAppContext().getMainLooper()).post(() -> Toast.makeText(OmniNotes.getAppContext(),
						OmniNotes.getAppContext().getString(R.string.alarm_set_on) + " " + DateHelper.getDateTimeShort
								(OmniNotes.getAppContext(), reminder), Toast.LENGTH_LONG).show());
			}
		}
	}
}
