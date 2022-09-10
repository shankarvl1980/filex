package svl.kadatha.filex;
import android.view.View;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;


public class ListFiles
{
	static List<FilePOJO> List(String file_name,List<FilePOJO> list, boolean extract_icon, boolean archive_view)
	{
		String pattern_name,pattern_long;
		if(MainActivity.SHOW_HIDDEN_FILE)
		{
			
			pattern_name="-A";
			pattern_long="-lA";
			
		}
		else
		{
			pattern_name="";
			pattern_long="-l";
		}
		
		String [] command_line_name={"ls",pattern_name,file_name,"|","cat"};
		String [] command_line_long={"ls",pattern_long,file_name,"|","cat"};
		
		
		try
		{
			//java.lang.Process process_name=Runtime.getRuntime().exec(pattern_name+file_name + " | " +" cat ");
			java.lang.Process process_name=Runtime.getRuntime().exec(command_line_name);
			BufferedReader bf_name=new BufferedReader(new InputStreamReader(process_name.getInputStream()));
			String line_name;
			
			//java.lang.Process process_long=Runtime.getRuntime().exec(pattern_long+file_name + " | " +" cat ");
			java.lang.Process process_long=Runtime.getRuntime().exec(command_line_long);
			BufferedReader bf_long=new BufferedReader(new InputStreamReader(process_long.getInputStream()));
			String line_long;
			
			bf_long.readLine(); //consume first line as not required
			while((line_name=bf_name.readLine())!=null && (line_long=bf_long.readLine())!=null)
			{
			//(File f,String n,String p,String d,String s,Integer t,String ext)
			
				String [] split_line=line_long.split("\\s+");
				String permission=split_line[0];
				boolean isDirectory=false;
				//if(permission.startsWith("^[^d].+"))

				String p=Global.CONCATENATE_PARENT_CHILD_PATH(file_name,line_name);
				File f=new File(p);
				//String d=DetailFragment.getFileSize(f,archive_view);
				//DetailFragment.SizeClass sizeClass =DetailFragment.getFileSize(f,isDirectory,archive_view);
				long sizeLong=f.length();
				String si;
				int overlay_visible=View.INVISIBLE;
				int alfa=225;
				String file_ext="";
				String package_name = null;

				if(!isDirectory)
				{
					int idx=line_name.lastIndexOf(".");
					if(idx!=-1)
					{
						file_ext=line_name.substring(idx+1);
						package_name=FilePOJOUtil.EXTRACT_ICON(MainActivity.PM,p,file_ext);
						if(file_ext.matches(Global.VIDEO_REGEX))
						{
							overlay_visible=View.VISIBLE;
						}
					}
					si=FileUtil.humanReadableByteCount(sizeLong);
				}
				else
				{
					si="";
				}



				if(MainActivity.SHOW_HIDDEN_FILE && f.isHidden())
				{
					alfa=100;
				}
				int t=FilePOJOUtil.GET_FILE_TYPE(f.isDirectory(),file_ext);
				list.add(new FilePOJO(null, line_name,package_name,p,isDirectory,0L,si,sizeLong,si,t,file_ext,alfa,overlay_visible,0,0L,null,0,null));
				
			}
			
			process_name.waitFor();
			process_long.waitFor();
		
			
		}
		catch(Exception e){}
		
		/*
		try
		{
			//java.lang.Process process_name=Runtime.getRuntime().exec(pattern_name+file_name + " | " +" cat ");
			java.lang.Process process_name=Runtime.getRuntime().exec(command_line_name);
			BufferedReader bf_name=new BufferedReader(new InputStreamReader(process_name.getInputStream()));
			String line_name;

			//java.lang.Process process_long=Runtime.getRuntime().exec(pattern_long+file_name + " | " +" cat ");
			java.lang.Process process_long=Runtime.getRuntime().exec(command_line_long);
			BufferedReader bf_long=new BufferedReader(new InputStreamReader(process_long.getInputStream()));
			String line_long;

			bf_long.readLine();
			while((line_name=bf_name.readLine())!=null && (line_long=bf_long.readLine())!=null)
			{
				//(File f,String n,String p,String d,String s,Integer t,String ext)
				String [] split_line=line_long.split("\\s+");
				String permission=split_line[0];
				//if(permission.matches("^d.+"))
				if(permission.startsWith("d"))
				{
					continue;
				}
				String n=line_name;
				String p=file_name+File.separator+n;
				File f=new File(p);
				String d=MainActivity.getFileSize(f,archive_view);
				String s=MainActivity.getFileSize(f,archive_view);
				String ext="";
				int idx=n.lastIndexOf(".");
				if(idx!=-1)
				{
					ext=n.substring(idx+1);
				}
				int t=MainActivity.getFileType(f,ext);
				list.add(new FilePOJO(f,n,p,d,s,t,ext));

			}

			process_name.waitFor();
			process_long.waitFor();


		}
		catch(Exception e){}
		*/
		return list;
		
	}
}
