package svl.kadatha.filex;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout.LayoutParams;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import java.io.IOException;

public class PermissionsDialog extends DialogFragment
{
    private String file_path;
    private boolean owner_read,owner_write,owner_exe,grp_read,grp_write,grp_exe,other_read,other_write,other_exe;
	private int owner_permission_int, group_permission_int,other_permission_int;
	private Context context;
	private Bundle bundle;

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setCancelable(false);
		bundle=getArguments();
		if(bundle!=null)
		{
			file_path=bundle.getString("file_path");
            String file_permissions = bundle.getString("file_permissions");
            String symbolic_link = bundle.getString("symbolic_link");
            String[] file_permissions_split=file_permissions.split("");
            int size=file_permissions_split.length;

			for(int i=0;i<size;++i)
			{
				if(i==1 && file_permissions_split[i].matches("\\w"))
				{
					owner_read=true;
					owner_permission_int+=4;
				}
				else if(i==2 && file_permissions_split[i].matches("\\w"))
				{
					owner_write=true;
					owner_permission_int+=2;
				}
				else if(i==3 && file_permissions_split[i].matches("\\w"))
				{
					owner_exe=true;
					owner_permission_int+=1;
				}
				else if(i==4 && file_permissions_split[i].matches("\\w"))
				{
					grp_read=true;
					group_permission_int+=4;
				}
				else if(i==5 && file_permissions_split[i].matches("\\w"))
				{
					grp_write=true;
					group_permission_int+=2;
				}
				else if(i==6 && file_permissions_split[i].matches("\\w"))
				{
					grp_exe=true;
					group_permission_int+=1;
				}
				else if(i==7 && file_permissions_split[i].matches("\\w"))
				{
					other_read=true;
					other_permission_int+=4;
				}
				else if(i==8 && file_permissions_split[i].matches("\\w"))
				{
					other_write=true;
					other_permission_int+=2;
				}
				else if(i==9 && file_permissions_split[i].matches("\\w"))
				{
					other_exe=true;
					other_permission_int+=1;
					
				}
			
			}
		}
	
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
        View v = inflater.inflate(R.layout.fragment_permissions, container, false);
        CheckBox owner_read_chkbox = v.findViewById(R.id.fragment_permissions_owner_read_chkbox);
        CheckBox owner_write_chkbox = v.findViewById(R.id.fragment_permissions_owner_write_chkbox);
        CheckBox owner_exe_chkbox = v.findViewById(R.id.fragment_permissions_owner_execute_chkbox);
        CheckBox grp_read_chkbox = v.findViewById(R.id.fragment_permissions_group_read_chkbox);
        CheckBox grp_write_chkbox = v.findViewById(R.id.fragment_permissions_group_write_chkbox);
        CheckBox grp_exe_chkbox = v.findViewById(R.id.fragment_permissions_group_execute_chkbox);
        CheckBox other_read_chkbox = v.findViewById(R.id.fragment_permissions_other_read_chkbox);
        CheckBox other_write_chkbox = v.findViewById(R.id.fragment_permissions_other_write_chkbox);
        CheckBox other_exe_chkbox = v.findViewById(R.id.fragment_permissions_other_execute_chkbox);
        ViewGroup buttons_layout = v.findViewById(R.id.fragment_permissions_button_layout);
		buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button save_btn = buttons_layout.findViewById(R.id.first_button);
		save_btn.setText(R.string.save);
        Button cancel_btn = buttons_layout.findViewById(R.id.second_button);
		cancel_btn.setText(R.string.cancel);
		save_btn.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				String permission=owner_permission_int+""+group_permission_int+""+other_permission_int;
				try
				{
					java.lang.Process proc=Runtime.getRuntime().exec("chmod "+permission+" "+file_path);
					proc.waitFor();
					//print(proc.exitValue()+"");
					
				}
				catch(IOException | InterruptedException e){}

				getParentFragmentManager().setFragmentResult(PropertiesDialog.PROPERTIES_DIALOG_REQUEST_CODE,bundle);
				dismissAllowingStateLoss();
			}
		});
		cancel_btn.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				dismissAllowingStateLoss();
			}
			
		});
		
		owner_read_chkbox.setChecked(owner_read);
		owner_write_chkbox.setChecked(owner_write);
		owner_exe_chkbox.setChecked(owner_exe);
		
		grp_read_chkbox.setChecked(grp_read);
		grp_write_chkbox.setChecked(grp_write);
		grp_exe_chkbox.setChecked(grp_exe);
		
		other_read_chkbox.setChecked(other_read);
		other_write_chkbox.setChecked(other_write);
		other_exe_chkbox.setChecked(other_exe);
		
		
		owner_read_chkbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton btn, boolean b)
			{
				owner_read=b;
				if(b)
				{
					owner_permission_int+=4;
				}
				else
				{
					owner_permission_int-=4;
				}
			}
			
			
		});
		
		owner_write_chkbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener()
			{
				public void onCheckedChanged(CompoundButton btn, boolean b)
				{
					owner_write=b;
					if(b)
					{
						owner_permission_int+=2;
					}
					else
					{
						owner_permission_int-=2;
					}
				
				}
				
			});
			
		owner_exe_chkbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener()
			{
				public void onCheckedChanged(CompoundButton btn, boolean b)
				{
					owner_exe=b;
					if(b)
					{
						owner_permission_int+=1;
					}
					else
					{
						owner_permission_int-=1;
					}
				}
			});
			
		grp_read_chkbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener()
			{
				public void onCheckedChanged(CompoundButton btn, boolean b)
				{
					grp_read=b;
					if(b)
					{
						group_permission_int+=4;
					}
					else
					{
						group_permission_int-=4;
					}
				}
			});
			
		grp_write_chkbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener()
			{
				public void onCheckedChanged(CompoundButton btn, boolean b)
				{
					grp_write=b;
					if(b)
					{
						group_permission_int+=2;
					}
					else
					{
						group_permission_int-=2;
					}
				}
			});
			
		grp_exe_chkbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener()
			{
				public void onCheckedChanged(CompoundButton btn, boolean b)
				{
					grp_exe=b;
					if(b)
					{
						group_permission_int+=1;
					}
					else
					{
						group_permission_int-=1;
					}
				}
			});
			
		other_read_chkbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener()
			{
				public void onCheckedChanged(CompoundButton btn, boolean b)
				{
					other_read=b;
					if(b)
					{
						other_permission_int+=4;
					}
					else
					{
						other_permission_int-=4;
					}
				}
			});
			
		other_write_chkbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener()
			{
				public void onCheckedChanged(CompoundButton btn, boolean b)
				{
					other_write=b;
					if(b)
					{
						other_permission_int+=2;
					}
					else
					{
						other_permission_int-=2;
					}
				}
			});
			
		other_exe_chkbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener()
			{
				public void onCheckedChanged(CompoundButton btn, boolean b)
				{
					other_exe=b;
					if(b)
					{
						other_permission_int+=1;
					}
					else
					{
						other_permission_int-=1;
					}
				}
			});
		
		
		return v;
	}
	
	public static PermissionsDialog getInstance(String file_path,String file_permissions,String symbolic_link)
	{
		PermissionsDialog permissionsDialog=new PermissionsDialog();
		Bundle bundle=new Bundle();
		bundle.putString("file_path",file_path);
		bundle.putString("file_permissions",file_permissions);
		bundle.putString("symbolic_link",symbolic_link);
		permissionsDialog.setArguments(bundle);
		return permissionsDialog;
	}
	

	@Override
	public void onResume()
	{
		// TODO: Implement this method
		super.onResume();
		Window window=getDialog().getWindow();
		window.setLayout(Global.DIALOG_WIDTH,LayoutParams.WRAP_CONTENT);
		window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

	}

}
