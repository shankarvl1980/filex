package svl.kadatha.filex;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;

public class Preferences 
{
    public static void setPrefs(String key, String value, Context context)
	{
        SharedPreferences sharedpreferences = context.getSharedPreferences("explorer", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String getPrefs(String key, Context context)
	{
        SharedPreferences sharedpreferences = context.getSharedPreferences("explorer", Context.MODE_PRIVATE);
        return sharedpreferences.getString(key, "notfound");
    }

	public static void removePrefs(String key,Context context)
	{
		SharedPreferences sharedpreferences = context.getSharedPreferences("explorer", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor=sharedpreferences.edit();
		editor.remove(key);
		editor.apply();
	}
    public static void setArrayPrefs(String preference_file,String arrayName, ArrayList<String> array, Context mContext) 
	{
        SharedPreferences prefs = mContext.getSharedPreferences(preference_file, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(arrayName +"_size", array.size());
        for(int i=0;i<array.size();++i)
		{
			editor.putString(arrayName + "_" + i, array.get(i));
		}
            
        editor.apply();
    }

    public static ArrayList<String> getArrayPrefs(String preference_file,String arrayName, Context mContext) 
	{
        SharedPreferences prefs = mContext.getSharedPreferences(preference_file, 0);
        int size = prefs.getInt(arrayName + "_size", 0);
        ArrayList<String> array = new ArrayList<>(size);
        for(int i=0;i<size;++i)
		{
			array.add(prefs.getString(arrayName + "_" + i, null));
		}
            
        return array;
    }
	
	public static void removeArrayPrefs(String preference_file,String arrayName,Context mContext)
	{
		SharedPreferences prefs = mContext.getSharedPreferences(preference_file, 0);
		SharedPreferences.Editor editor = prefs.edit();
        int size = prefs.getInt(arrayName + "_size", 0);
		editor.remove(arrayName +"_size");
		for(int i=0;i<size;++i)
		{
			editor.remove(arrayName + "_" + i);
		}

        editor.apply();
	}
	
}
