package svl.kadatha.filex;


/*
 * Copyright 2014 KC Ochibili
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 *  The "‚‗‚" character is not a comma, it is the SINGLE LOW-9 QUOTATION MARK unicode 201A
 *  and unicode 2017 that are used for separating the items in a list.
 */


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;

import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import timber.log.Timber;

/*
 * Copyright 2014 KC Ochibili
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 *  The "‚‗‚" character is not a comma, it is the SINGLE LOW-9 QUOTATION MARK unicode 201A
 *  and unicode 2017 that are used for separating the items in a list.
 */


public class TinyDB {

    private final SharedPreferences preferences;
    private String DEFAULT_APP_IMAGEDATA_DIRECTORY;
    private String lastImagePath = "";

    public TinyDB(Context appContext) {
        preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
    }

    /**
     * STORAGE UTILS
     **/

    public static boolean isExternalStorageWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    /**
     * IMAGE HELPERS
     **/

    public Bitmap getImage(String path) {
        Bitmap bitmapFromPath = null;
        try {
            bitmapFromPath = BitmapFactory.decodeFile(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmapFromPath;
    }

    public String getSavedImagePath() {
        return lastImagePath;
    }

    public String putImage(String theFolder, String theImageName, Bitmap theBitmap) {
        if (theFolder == null || theImageName == null || theBitmap == null) {
            return null;
        }
        this.DEFAULT_APP_IMAGEDATA_DIRECTORY = theFolder;
        String fullPath = setupFullPath(theImageName);
        if (!fullPath.isEmpty()) {
            lastImagePath = fullPath;
            saveBitmap(fullPath, theBitmap);
        }
        return fullPath;
    }

    public boolean putImageWithFullPath(String fullPath, Bitmap theBitmap) {
        return !(fullPath == null || theBitmap == null) && saveBitmap(fullPath, theBitmap);
    }

    private String setupFullPath(String imageName) {
        File folder = new File(Environment.getExternalStorageDirectory(), DEFAULT_APP_IMAGEDATA_DIRECTORY);
        if (isExternalStorageReadable() && isExternalStorageWritable() && !folder.exists()) {
            if (!folder.mkdirs()) {
                Timber.tag("ERROR").e("Failed to setup folder");
                return "";
            }
        }
        return folder.getPath() + '/' + imageName;
    }

    private boolean saveBitmap(String fullPath, Bitmap bitmap) {
        if (fullPath == null || bitmap == null) return false;

        boolean fileCreated = false;
        boolean bitmapCompressed;
        boolean streamClosed = false;

        File imageFile = new File(fullPath);
        if (imageFile.exists() && !imageFile.delete()) {
            return false;
        }

        try {
            fileCreated = imageFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(imageFile);
            bitmapCompressed = bitmap.compress(CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
            bitmapCompressed = false;
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                    streamClosed = true;
                } catch (IOException e) {
                    e.printStackTrace();
                    streamClosed = false;
                }
            }
        }
        return (fileCreated && bitmapCompressed && streamClosed);
    }

    /**
     * ================= SAFE GETTERS (with self-heal) =================
     **/

    // Self-heal writers
    private void selfHealWriteInt(String key, int defaultValue) {
        preferences.edit().putInt(key, defaultValue).apply();
    }

    private void selfHealWriteLong(String key, long defaultValue) {
        preferences.edit().putLong(key, defaultValue).apply();
    }

    private void selfHealWriteFloat(String key, float defaultValue) {
        preferences.edit().putFloat(key, defaultValue).apply();
    }

    private void selfHealWriteBoolean(String key, boolean defaultValue) {
        preferences.edit().putBoolean(key, defaultValue).apply();
    }

    private void selfHealWriteString(String key, String defaultValue) {
        preferences.edit().putString(key, defaultValue == null ? "" : defaultValue).apply();
    }

    /* ---- int ---- */
    public int getInt(String key) {
        return getInt(key, 0);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return preferences.getInt(key, defaultValue);
        } catch (ClassCastException e) {
            // wrong type stored; self-heal
            selfHealWriteInt(key, defaultValue);
            return defaultValue;
        }
    }

    /* ---- long ---- */
    public long getLong(String key) {
        return getLong(key, 0L);
    }

    public long getLong(String key, long defaultValue) {
        try {
            return preferences.getLong(key, defaultValue);
        } catch (ClassCastException e) {
            selfHealWriteLong(key, defaultValue);
            return defaultValue;
        }
    }

    /* ---- float ---- */
    public float getFloat(String key) {
        return getFloat(key, 0f);
    }

    public float getFloat(String key, float defaultValue) {
        try {
            return preferences.getFloat(key, defaultValue);
        } catch (ClassCastException e) {
            selfHealWriteFloat(key, defaultValue);
            return defaultValue;
        }
    }

    /* ---- boolean ---- */
    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            return preferences.getBoolean(key, defaultValue);
        } catch (ClassCastException e) {
            selfHealWriteBoolean(key, defaultValue);
            return defaultValue;
        }
    }

    /* ---- String ---- */
    public String getString(String key) {
        return getString(key, "");
    }

    public String getString(String key, String defaultValue) {
        try {
            String def = (defaultValue == null) ? "" : defaultValue;
            String val = preferences.getString(key, def);
            return (val == null) ? def : val;
        } catch (ClassCastException e) {
            String def = (defaultValue == null) ? "" : defaultValue;
            selfHealWriteString(key, def);
            return def;
        }
    }

    /* ---- double (stored as String) ---- */
    public double getDouble(String key, double defaultValue) {
        String s = getString(key, null);
        if (s == null || s.isEmpty()) return defaultValue;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            putString(key, String.valueOf(defaultValue)); // self-heal
            return defaultValue;
        }
    }

    /**
     * ================= LIST GETTERS (String-based) =================
     **/

    public ArrayList<Integer> getListInt(String key) {
        String[] myList = TextUtils.split(getString(key, ""), "‚‗‚");
        ArrayList<String> arrayToList = new ArrayList<>(Arrays.asList(myList));
        ArrayList<Integer> newList = new ArrayList<>();
        for (String item : arrayToList) {
            try {
                newList.add(Integer.parseInt(item));
            } catch (NumberFormatException ignore) { /* skip bad entries */ }
        }
        return newList;
    }

    public ArrayList<Double> getListDouble(String key) {
        String[] myList = TextUtils.split(getString(key, ""), "‚‗‚");
        ArrayList<String> arrayToList = new ArrayList<>(Arrays.asList(myList));
        ArrayList<Double> newList = new ArrayList<>();
        for (String item : arrayToList) {
            try {
                newList.add(Double.parseDouble(item));
            } catch (NumberFormatException ignore) { /* skip bad entries */ }
        }
        return newList;
    }

    public ArrayList<Long> getListLong(String key) {
        String[] myList = TextUtils.split(getString(key, ""), "‚‗‚");
        ArrayList<String> arrayToList = new ArrayList<>(Arrays.asList(myList));
        ArrayList<Long> newList = new ArrayList<>();
        for (String item : arrayToList) {
            try {
                newList.add(Long.parseLong(item));
            } catch (NumberFormatException ignore) { /* skip bad entries */ }
        }
        return newList;
    }

    public ArrayList<String> getListString(String key) {
        return new ArrayList<>(Arrays.asList(TextUtils.split(getString(key, ""), "‚‗‚")));
    }

    public ArrayList<Boolean> getListBoolean(String key) {
        ArrayList<String> myList = getListString(key);
        ArrayList<Boolean> newList = new ArrayList<>();
        for (String item : myList) {
            newList.add("true".equals(item));
        }
        return newList;
    }

    /**
     * ================= PUTTERS =================
     **/

    public void putInt(String key, int value) {
        checkForNullKey(key);
        preferences.edit().putInt(key, value).apply();
    }

    public void putListInt(String key, ArrayList<Integer> intList) {
        checkForNullKey(key);
        Integer[] myIntList = intList.toArray(new Integer[0]);
        preferences.edit().putString(key, TextUtils.join("‚‗‚", myIntList)).apply();
    }

    public void putLong(String key, long value) {
        checkForNullKey(key);
        preferences.edit().putLong(key, value).apply();
    }

    public void putListLong(String key, ArrayList<Long> longList) {
        checkForNullKey(key);
        Long[] myLongList = longList.toArray(new Long[0]);
        preferences.edit().putString(key, TextUtils.join("‚‗‚", myLongList)).apply();
    }

    public void putFloat(String key, float value) {
        checkForNullKey(key);
        preferences.edit().putFloat(key, value).apply();
    }

    public void putDouble(String key, double value) {
        checkForNullKey(key);
        putString(key, String.valueOf(value));
    }

    public void putListDouble(String key, ArrayList<Double> doubleList) {
        checkForNullKey(key);
        Double[] myDoubleList = doubleList.toArray(new Double[0]);
        preferences.edit().putString(key, TextUtils.join("‚‗‚", myDoubleList)).apply();
    }

    public void putString(String key, String value) {
        checkForNullKey(key);
        checkForNullValue(value);
        preferences.edit().putString(key, value).apply();
    }

    public void putListString(String key, ArrayList<String> stringList) {
        checkForNullKey(key);
        String[] myStringList = stringList.toArray(new String[0]);
        preferences.edit().putString(key, TextUtils.join("‚‗‚", myStringList)).apply();
    }

    public void putBoolean(String key, boolean value) {
        checkForNullKey(key);
        preferences.edit().putBoolean(key, value).apply();
    }

    public void putListBoolean(String key, ArrayList<Boolean> boolList) {
        checkForNullKey(key);
        ArrayList<String> newList = new ArrayList<>();
        for (Boolean item : boolList) {
            newList.add(item ? "true" : "false");
        }
        putListString(key, newList);
    }

    /**
     * ================= ADMIN =================
     **/

    public void remove(String key) {
        preferences.edit().remove(key).apply();
    }

    public boolean deleteImage(String path) {
        return new File(path).delete();
    }

    public void clear() {
        preferences.edit().clear().apply();
    }

    public Map<String, ?> getAll() {
        return preferences.getAll();
    }

    public void registerOnSharedPreferenceChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        preferences.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterOnSharedPreferenceChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        preferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public void checkForNullKey(String key) {
        if (key == null) throw new NullPointerException("Preference key is null");
    }

    public void checkForNullValue(String value) {
        if (value == null) throw new NullPointerException("Preference value is null");
    }
}
