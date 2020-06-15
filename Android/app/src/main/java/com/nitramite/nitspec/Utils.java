package com.nitramite.nitspec;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

class Utils {


    /**
     * Set double to shared preferences suitable form
     * @param value double input
     * @return Returns long
     */
    static Long sharedPrefsDoubleToLong(final double value) {
        return Double.doubleToRawLongBits(value);
    }


    /**
     * Get long and convert back to double on shared prefs
     * @param prefs Preferences
     * @param key Key
     * @param defaultValue default value
     * @return double
     */
    static Double sharedPrefsLongToDouble(final SharedPreferences prefs, final String key, final double defaultValue) {
        return Double.longBitsToDouble(prefs.getLong(key, Double.doubleToLongBits(defaultValue)));
    }


    /**
     * Get app version code
     * @return versionCode string
     */
    static String getAppVersionCode(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return String.valueOf(pInfo.versionCode);
            // appVersionName = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "-";
        }
    }



} // End of class