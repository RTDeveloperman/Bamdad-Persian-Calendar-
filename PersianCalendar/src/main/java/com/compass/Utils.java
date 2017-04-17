package com.compass;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;

import com.compass.compass.Main;
import com.compass.settings.Prefs;

import org.joda.time.LocalDate;

import java.util.Locale;

public class Utils {
    private static final String[] ASSETS = {"/dinigunler/hicriyil.html", "/dinigunler/asure.html", "/dinigunler/mevlid.html", "/dinigunler/3aylar.html", "/dinigunler/regaib.html", "/dinigunler/mirac.html", "/dinigunler/berat.html", "/dinigunler/ramazan.html", "/dinigunler/kadir.html", "/dinigunler/arefe.html", "/dinigunler/ramazanbay.html", "/dinigunler/ramazanbay.html", "/dinigunler/ramazanbay.html", "/dinigunler/arefe.html", "/dinigunler/kurban.html", "/dinigunler/kurban.html", "/dinigunler/kurban.html", "/dinigunler/kurban.html"};
    private static String[] sGMonths;
    private static String[] sHMonths;
    private static String[] sHolydays;
    private static String[] sWeekdays;
    private static String[] sShortWeekdays;

    public static CharSequence fixTimeForHTML(String time) {
        time = fixTime(time);
        if (!Prefs.use12H()) {
            return time;
        }
        int d = time.indexOf(" ");
        if (d < 0) return time;
        time = time.replace(" ", "");

        int s = time.length();
        Spannable span = new SpannableString(time);
        span.setSpan(new SuperscriptSpan(), d, s, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new RelativeSizeSpan(0.5f), d, s, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return span;
    }


    public static String fixTime(String time) {
        if (Prefs.use12H() && time.contains(":")) {
            try {
                String fix = time.substring(0, time.indexOf(":"));
                String suffix = time.substring(time.indexOf(":"));


                int hour = Integer.parseInt(fix);
                if (hour == 0) {
                    return "00" + suffix + " AM";
                } else if (hour < 12) {
                    return az(hour) + suffix + " AM";
                } else if (hour == 12) {
                    return "12" + suffix + " PM";
                } else {
                    return az(hour - 12) + suffix + " PM";
                }
            } catch (Exception e) {
                return time;
            }
        }


        return toArabicNrs(time);

    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static void init() {
        String newLang = Prefs.getLanguage();

        Context c = Main.getContext();


        int year = LocalDate.now().getYear();

        Prefs.setLastCalSync(year);

        if (newLang == null) {
            return;
        }
        Locale locale = new Locale(newLang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        ((ContextWrapper) Main.getContext()).getBaseContext().getResources().updateConfiguration(config, ((ContextWrapper) Main.getContext()).getBaseContext().getResources().getDisplayMetrics());

    }

    public static void changeLanguage(String language) {
        String newLang = language;
        Context c = Main.getContext();

        Prefs.setLanguage(language);

        sGMonths = null;
        sHMonths = null;
        sHolydays = null;
        sWeekdays = null;
        sShortWeekdays = null;

    }



    public static String[] getAllHolydays() {
        return sHolydays;
    }

    public static boolean askLang(final Activity act) {
        if (Prefs.getLanguage() != null) {
            return false;
        }
        return true;
    }

    public static String az(int i) {
        if (i < 10) {
            return "0" + i;
        }
        return i + "";
    }

    private static String getDateFormat(boolean hicri) {
        return hicri ? Prefs.getHDF() : Prefs.getDF();
    }
    public static String format(LocalDate date) {
        String format = getDateFormat(true);
        format = format.replace("DD", az(date.getDayOfMonth(), 2));

        format = format.replace("MM", az(date.getMonthOfYear(), 2));
        format = format.replace("YYYY", az(date.getYear(), 4));
        format = format.replace("YY", az(date.getYear(), 2));
        return toArabicNrs(format);
    }

    private static String az(int Int, int num) {
        String ret = Int + "";
        if (ret.length() < num) {
            for (int i = ret.length(); i < num; i++) {
                ret = "0" + ret;
            }
        } else if (ret.length() > num) {
            ret = ret.substring(ret.length() - num, ret.length());
        }

        return ret;
    }

    public static String getAssetForHolyday(int pos) {
        return Prefs.getLanguage() + ASSETS[pos - 1];
    }


    public static String toArabicNrs(String str) {
        if (str == null) return null;
        if (Prefs.getDigits().equals("normal")) return str;
        char[] arabicChars = {'٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'};
        if (Prefs.getDigits().equals("farsi")) {
            arabicChars[4] = '۴';
            arabicChars[5] = '۵';
            arabicChars[6] = '۶';
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            if (Character.isDigit(str.charAt(i))) {
                builder.append(arabicChars[(int) (str.charAt(i)) - 48]);
            } else {
                builder.append(str.charAt(i));
            }
        }
        return builder.toString();
    }

    public static String toArabicNrs(int nr) {
        return toArabicNrs(nr + "");
    }
}
