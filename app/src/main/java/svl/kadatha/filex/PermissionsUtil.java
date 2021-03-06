package svl.kadatha.filex;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionsUtil
{
	static final int STORAGE_PERMISSIONS_REQUEST_CODE=657;
	static final int READ_PHONE_STATE_PERMISSION_REQUEST_CODE=693;
	private final Context context;
	private final Activity activity;
	private final List<String>permissions_list_storage=new ArrayList<>();
	private final List<String>permissions_not_granted_list=new ArrayList<>();


	
	PermissionsUtil(Context context, AppCompatActivity activity)
	{
		this.context=context;
		this.activity=activity;
		

		permissions_list_storage.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
		
	}
	
	public boolean check_storage_permission()
	{
		
		if(Build.VERSION.SDK_INT>=23)
		{
			for(String permission:permissions_list_storage)
			{
				//List<String>permissions_not_granted_list1=new ArrayList<>();
				int i=ContextCompat.checkSelfPermission(context,android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
				//permissions_not_granted_list.add(permission);
				if(i!=PackageManager.PERMISSION_GRANTED)
				{
					permissions_not_granted_list.add(permission);
				}
			}
			if(!permissions_not_granted_list.isEmpty())
			{
				
				activity.requestPermissions(permissions_not_granted_list.toArray(new String[0]),STORAGE_PERMISSIONS_REQUEST_CODE);
				return false;
			}
			else
			{
				return true;
			}
		
		}
		return true;
	}
	

	public boolean check_read_phone_state() {
		if (Build.VERSION.SDK_INT >= 23) {

			int i = activity.checkSelfPermission(Manifest.permission.READ_PHONE_STATE);
			if (i != PackageManager.PERMISSION_GRANTED) {
				activity.requestPermissions(new String[]{"android.Manifest.permission.READ_PHONE_STATE"}, READ_PHONE_STATE_PERMISSION_REQUEST_CODE);
				return false;


			} else {
				return true;
			}
		} else {
			return true;
		}
	}
	
}
