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

package com.omninotes;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.StrictMode;
import android.support.multidex.MultiDex;
import android.text.TextUtils;
import android.util.Log;

import com.byagowi.persiancalendar.BuildConfig;
import com.byagowi.persiancalendar.R;


import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender.Method;
import org.acra.sender.HttpSender.Type;

import java.util.Locale;

import com.omninotes.helpers.AnalyticsHelper;
import com.omninotes.utils.Constants;


@ReportsCrashes(httpMethod = Method.POST, reportType = Type.FORM,
		formUri = "http://vmagnify.com/crashes",
		mode = ReportingInteractionMode.TOAST,
		forceCloseDialogAfterToast = false,
		resToastText = R.string.crash_toast)
public class OmniNotes extends com.outlay.App {

	private static Context mContext;
	private final static String PREF_LANG = "settings_language";
	static SharedPreferences prefs;

	protected void attachBaseContext(Context base)
	{
		super.attachBaseContext(base);
		MultiDex.install(this);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = getApplicationContext();
		prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_MULTI_PROCESS);


		if (BuildConfig.BUILD_TYPE.equals("debug")) {
			StrictMode.enableDefaults();
		}

		initAcra(this);

		// Checks selected locale or default one
		updateLanguage(this, null);

		Log.e("runFirst","");
		// Analytics initialization
		AnalyticsHelper.init(this);
	}


	private void initAcra(Application application) {
		ACRA.init(application);
		String isDebugBuild = BuildConfig.BUILD_TYPE.equals("debug") ? "1" : "0";
		ACRA.getErrorReporter().putCustomData("TRACEPOT_DEVELOP_MODE", isDebugBuild);
	}


	@Override
	// Used to restore user selected locale when configuration changes
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		String language = prefs.getString(PREF_LANG, "");
		super.onConfigurationChanged(newConfig);
		updateLanguage(this, language);
	}

	public static Context getAppContext() {
		return OmniNotes.mContext;
	}


	/**
	 * Updates default language with forced one
	 */
	public static void updateLanguage(Context ctx, String lang) {
		Configuration cfg = new Configuration();
		String language = prefs.getString(PREF_LANG, "");

		if (TextUtils.isEmpty(language) && lang == null) {
			cfg.locale = Locale.getDefault();
			prefs.edit().putString(PREF_LANG, Locale.getDefault().toString()).commit();

		} else if (lang != null) {
			// Checks country
			if (lang.contains("_")) {
				cfg.locale = new Locale(lang.split("_")[0], lang.split("_")[1]);
			} else {
				cfg.locale = new Locale(lang);
			}
			prefs.edit().putString(PREF_LANG, lang).commit();

		} else if (!TextUtils.isEmpty(language)) {
			// Checks country
			if (language.contains("_")) {
				cfg.locale = new Locale(language.split("_")[0], language.split("_")[1]);
			} else {
				cfg.locale = new Locale(language);
			}
		}

		ctx.getResources().updateConfiguration(cfg, null);
	}




}
