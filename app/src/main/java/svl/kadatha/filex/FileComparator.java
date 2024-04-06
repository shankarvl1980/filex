package svl.kadatha.filex;

import java.io.File;
import java.util.Comparator;

import me.jahnen.libaums.core.fs.UsbFile;

public class FileComparator
{

	public static Comparator<File> FileComparate(String SORT)
	{
		switch(SORT)
		{
			case "d_name_desc":
				return new SORT_FILE_FOLDERFILE_NAME_DESC();
				
			case "d_date_asc":
				return new SORT_FILE_FOLDERFILE_TIME_ASC();
				
			case "d_date_desc":
				return new SORT_FILE_FOLDERFILE_TIME_DESC();
				
			case "f_name_asc":
				return new SORT_FILE_FILEFOLDER_NAME_ASC();

			case "f_name_desc":
				return new SORT_FILE_FILEFOLDER_NAME_DESC();

			case "f_date_asc":
				return new SORT_FILE_FILEFOLDER_TIME_ASC();

			case "f_date_desc":
				return new SORT_FILE_FILEFOLDER_TIME_DESC();
				
			default:
				return new SORT_FILE_FOLDERFILE_NAME_ASC();
		}
	}

	public static Comparator<UsbFile> UsbFileComparate(String SORT)
	{
		switch(SORT)
		{
			case "d_name_desc":
				return new SORT_USB_FOLDERFILE_NAME_DESC();

			case "d_date_asc":
				return new SORT_USB_FOLDERFILE_TIME_ASC();

			case "d_date_desc":
				return new SORT_USB_FOLDERFILE_TIME_DESC();

			case "f_name_asc":
				return new SORT_USB_FILEFOLDER_NAME_ASC();

			case "f_name_desc":
				return new SORT_USB_FILEFOLDER_NAME_DESC();

			case "f_date_asc":
				return new SORT_USB_FILEFOLDER_TIME_ASC();

			case "f_date_desc":
				return new SORT_USB_FILEFOLDER_TIME_DESC();

			default:
				return new SORT_USB_FOLDERFILE_NAME_ASC();
		}
	}


	public static Comparator<FilePOJO> FilePOJOComparate(String SORT,boolean compare_total_file_size)
	{
		switch(SORT)
		{
			case "d_name_desc":
				return new SORT_D_FILEPOJO_NAME_DESC();

			case "d_name_asc:":
				return new SORT_D_FILEPOJO_NAME_ASC();

			case "d_date_asc":
				return new SORT_D_FILEPOJO_TIME_ASC();

			case "d_date_desc":
				return new SORT_D_FILEPOJO_TIME_DESC();

			case "d_size_desc":
				if(compare_total_file_size) return new SORT_D_FILEPOJO_TOTAL_SIZE_DESC();
				else return new SORT_D_FILEPOJO_SIZE_DESC();

			case "d_size_asc":
				if(compare_total_file_size) return new SORT_D_FILEPOJO_TOTAL_SIZE_ASC();
				else return new SORT_D_FILEPOJO_SIZE_ASC();

			case "f_name_asc":
				return new SORT_F_FILEPOJO_NAME_ASC();

			case "f_name_desc":
				return new SORT_F_FILEPOJO_NAME_DESC();

			case "f_date_asc":
				return new SORT_F_FILEPOJO_TIME_ASC();

			case "f_date_desc":
				return new SORT_F_FILEPOJO_TIME_DESC();

			case "f_size_desc":
				if(compare_total_file_size) new SORT_F_FILEPOJO_TOTAL_SIZE_DESC();
				else return new SORT_F_FILEPOJO_SIZE_DESC();

			case "f_size_asc":
				if(compare_total_file_size) new SORT_F_FILEPOJO_TOTAL_SIZE_ASC();
				else return new SORT_F_FILEPOJO_SIZE_ASC();

			default:
				return new SORT_D_FILEPOJO_NAME_ASC();

		}
	}


	public static Comparator<AppManagerListFragment.AppPOJO> AppPOJOComparate(String SORT)
	{
		switch(SORT)
		{
			case "d_name_desc":
			case "f_name_desc":
				return new SORT_APPPOJO_NAME_DESC();

			case "d_name_asc:":
			case "f_name_asc":
				return new SORT_APPPOJO_NAME_ASC();

			case "d_date_asc":
			case "f_date_asc":
				return new SORT_APPPOJO_TIME_ASC();

			case "d_date_desc":
			case "f_date_desc":
				return new SORT_APPPOJO_TIME_DESC();

			case "d_size_desc":
			case "f_size_desc":
				return new SORT_APPPOJO_SIZE_DESC();

			case "d_size_asc":
			case "f_size_asc":
				return new SORT_APPPOJO_SIZE_ASC();

			default:
				return new SORT_APPPOJO_NAME_ASC();

		}
	}


	private static class SORT_D_FILEPOJO_NAME_ASC implements Comparator<FilePOJO>
	{
		@Override
		public int compare(FilePOJO f1, FilePOJO f2) 
		{
			if (f1.getIsDirectory() && !f2.getIsDirectory())
				return -1;

			else if(!f1.getIsDirectory() && f2.getIsDirectory())
				return 1;
			else
				return f1.getLowerName().compareTo(f2.getLowerName());
		}
	}

	private static class SORT_F_FILEPOJO_NAME_ASC implements Comparator<FilePOJO>
	{
		@Override
		public int compare(FilePOJO f1, FilePOJO f2) 
		{
			if (!f1.getIsDirectory() && f2.getIsDirectory())
				return -1;

			else if(f1.getIsDirectory() && !f2.getIsDirectory())
				return 1;
			else
				return f1.getLowerName().compareTo(f2.getLowerName());
		}
	}


	private static class SORT_D_FILEPOJO_NAME_DESC implements Comparator<FilePOJO>
	{
		@Override
		public int compare(FilePOJO f1, FilePOJO f2) 
		{
			if (f1.getIsDirectory() && !f2.getIsDirectory())
				return -1;

			else if(!f1.getIsDirectory() && f2.getIsDirectory())
				return 1;
			else
			{
				int i=f1.getLowerName().compareTo(f2.getLowerName());
				return -(i);
			}
		}
	}
	
	private static class SORT_F_FILEPOJO_NAME_DESC implements Comparator<FilePOJO>
	{
		@Override
		public int compare(FilePOJO f1, FilePOJO f2) 
		{
			if (!f1.getIsDirectory() && f2.getIsDirectory())
				return -1;

			else if(f1.getIsDirectory() && !f2.getIsDirectory())
				return 1;
			else
			{
				int i=f1.getLowerName().compareTo(f2.getLowerName());
				return -(i);
			}
		}
	}

	private static class SORT_D_FILEPOJO_TIME_ASC implements Comparator<FilePOJO>
	{

		@Override
		public int compare(FilePOJO f1, FilePOJO f2)
		{
			// TODO: Implement this method
			if (f1.getIsDirectory() && !f2.getIsDirectory())
				return -1;

			else if(!f1.getIsDirectory() && f2.getIsDirectory())
				return 1;
			else
				return Long.compare(f1.getDateLong(), f2.getDateLong());
		}
	}

	private static class SORT_F_FILEPOJO_TIME_ASC implements Comparator<FilePOJO>
	{
		@Override
		public int compare(FilePOJO f1, FilePOJO f2)
		{
			// TODO: Implement this method

			if (!f1.getIsDirectory() && f2.getIsDirectory())
				return -1;

			else if(f1.getIsDirectory() && !f2.getIsDirectory())
				return 1;
			else
				return Long.compare(f1.getDateLong(), f2.getDateLong());
		}
	}


	private static class SORT_D_FILEPOJO_TIME_DESC implements Comparator<FilePOJO>
	{
		@Override
		public int compare(FilePOJO f1, FilePOJO f2)
		{
			// TODO: Implement this method
			if (f1.getIsDirectory() && !f2.getIsDirectory())
				return -1;

			else if(!f1.getIsDirectory() && f2.getIsDirectory())
				return 1;
			else
			{
				int i= Long.compare(f1.getDateLong(), f2.getDateLong());
				return -(i);
			}
		}
	}

	private static class SORT_F_FILEPOJO_TIME_DESC implements Comparator<FilePOJO>
	{
		@Override
		public int compare(FilePOJO f1, FilePOJO f2)
		{
			// TODO: Implement this method
			if (!f1.getIsDirectory() && f2.getIsDirectory())
				return -1;
			else if(f1.getIsDirectory() && !f2.getIsDirectory())
				return 1;
			else
			{
				int i= Long.compare(f1.getDateLong(), f2.getDateLong());
				return -(i);
			}
		}
	}





	///
	private static class SORT_D_FILEPOJO_SIZE_ASC implements Comparator<FilePOJO>
	{
		@Override
		public int compare(FilePOJO f1, FilePOJO f2)
		{
			if (f1.getIsDirectory() && !f2.getIsDirectory())
				return -1;

			else if(!f1.getIsDirectory() && f2.getIsDirectory())
				return 1;
			else if(f1.getIsDirectory() && f2.getIsDirectory())
				return f1.getLowerName().compareTo(f2.getLowerName());
			else
			{
				return Long.compare(f1.getSizeLong(),f2.getSizeLong());
			}

		}
	}

	private static class SORT_F_FILEPOJO_SIZE_ASC implements Comparator<FilePOJO>
	{
		@Override
		public int compare(FilePOJO f1, FilePOJO f2)
		{
			if (!f1.getIsDirectory() && f2.getIsDirectory())
				return -1;

			else if(f1.getIsDirectory() && !f2.getIsDirectory())
				return 1;
			else if(f1.getIsDirectory() && f2.getIsDirectory())
				return f1.getLowerName().compareTo(f2.getLowerName());
			else
				return Long.compare(f1.getSizeLong(),f2.getSizeLong());
		}
	}

	private static class SORT_D_FILEPOJO_SIZE_DESC implements Comparator<FilePOJO>
	{
		@Override
		public int compare(FilePOJO f1, FilePOJO f2)
		{
			if (f1.getIsDirectory() && !f2.getIsDirectory())
				return -1;

			else if(!f1.getIsDirectory() && f2.getIsDirectory())
				return 1;
			else if(f1.getIsDirectory() && f2.getIsDirectory())
				return f1.getLowerName().compareTo(f2.getLowerName());
			else
			{
				int i=Long.compare(f1.getSizeLong(),f2.getSizeLong());
				return -(i);
			}
		}
	}

	private static class SORT_F_FILEPOJO_SIZE_DESC implements Comparator<FilePOJO>
	{
		@Override
		public int compare(FilePOJO f1, FilePOJO f2)
		{
			if (!f1.getIsDirectory() && f2.getIsDirectory())
				return -1;

			else if(f1.getIsDirectory() && !f2.getIsDirectory())
				return 1;
			else if(f1.getIsDirectory() && f2.getIsDirectory())
				return f1.getLowerName().compareTo(f2.getLowerName());
			else
			{
				int i=Long.compare(f1.getSizeLong(),f2.getSizeLong());
				return -(i);
			}
		}
	}


	private static class SORT_D_FILEPOJO_TOTAL_SIZE_ASC implements Comparator<FilePOJO>
	{
		@Override
		public int compare(FilePOJO f1, FilePOJO f2)
		{
			if (f1.getIsDirectory() && !f2.getIsDirectory())
				return -1;

			else if(!f1.getIsDirectory() && f2.getIsDirectory())
				return 1;
			else if(f1.getIsDirectory() && f2.getIsDirectory())
				return Long.compare(f1.getTotalSizeLong(),f2.getTotalSizeLong());
			else
			{
				return Long.compare(f1.getSizeLong(),f2.getSizeLong());
			}

		}
	}

	private static class SORT_F_FILEPOJO_TOTAL_SIZE_ASC implements Comparator<FilePOJO>
	{
		@Override
		public int compare(FilePOJO f1, FilePOJO f2)
		{
			if (!f1.getIsDirectory() && f2.getIsDirectory())
				return -1;

			else if(f1.getIsDirectory() && !f2.getIsDirectory())
				return 1;
			else if(f1.getIsDirectory() && f2.getIsDirectory())
				return Long.compare(f1.getTotalSizeLong(),f2.getTotalSizeLong());
			else
				return Long.compare(f1.getSizeLong(),f2.getSizeLong());
		}
	}

	private static class SORT_D_FILEPOJO_TOTAL_SIZE_DESC implements Comparator<FilePOJO>
	{
		@Override
		public int compare(FilePOJO f1, FilePOJO f2)
		{
			if (f1.getIsDirectory() && !f2.getIsDirectory())
				return -1;

			else if(!f1.getIsDirectory() && f2.getIsDirectory())
				return 1;
			else if(f1.getIsDirectory() && f2.getIsDirectory())
			{
				int i=Long.compare(f1.getTotalSizeLong(),f2.getTotalSizeLong());
				return -(i);
			}
			else
			{
				int i=Long.compare(f1.getSizeLong(),f2.getSizeLong());
				return -(i);
			}
		}
	}

	private static class SORT_F_FILEPOJO_TOTAL_SIZE_DESC implements Comparator<FilePOJO>
	{
		@Override
		public int compare(FilePOJO f1, FilePOJO f2)
		{
			if (!f1.getIsDirectory() && f2.getIsDirectory())
				return -1;

			else if(f1.getIsDirectory() && !f2.getIsDirectory())
				return 1;
			else if(f1.getIsDirectory() && f2.getIsDirectory())
			{
				int i=Long.compare(f1.getTotalSizeLong(),f2.getTotalSizeLong());
				return -(i);
			}
			else
			{
				int i=Long.compare(f1.getSizeLong(),f2.getSizeLong());
				return -(i);
			}
		}
	}




	///

	private static class SORT_FILE_FOLDERFILE_NAME_ASC implements Comparator<File>
	{
		@Override
		public int compare(File f1, File f2)
		{
			if (f1.isDirectory() && !f2.isDirectory())
				return -1;

			else if(!f1.isDirectory() && f2.isDirectory())
				return 1;
			else
				return f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
		}
	}

	private static class SORT_FILE_FILEFOLDER_NAME_ASC implements Comparator<File>
	{
		@Override
		public int compare(File f1, File f2)
		{
			if (!f1.isDirectory() && f2.isDirectory())
				return -1;

			else if(f1.isDirectory() && !f2.isDirectory())
				return 1;
			else
				return f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
		}
	}


	private static class SORT_FILE_FOLDERFILE_NAME_DESC implements Comparator<File>
	{
		@Override
		public int compare(File f1, File f2)
		{
			if (f1.isDirectory() && !f2.isDirectory())
				return -1;

			else if(!f1.isDirectory() && f2.isDirectory())
				return 1;
			else
			{
				int i=f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
				return -(i);
			}
		}
	}


	private static class SORT_FILE_FILEFOLDER_NAME_DESC implements Comparator<File>
	{
		@Override
		public int compare(File f1, File f2)
		{
			if (!f1.isDirectory() && f2.isDirectory())
				return -1;

			else if(f1.isDirectory() && !f2.isDirectory())
				return 1;
			else
			{
				int i=f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
				return -(i);
			}
		}
	}

	private static class SORT_FILE_FOLDERFILE_TIME_ASC implements Comparator<File>
	{

		@Override
		public int compare(File f1, File f2)
		{
			// TODO: Implement this method
			if (f1.isDirectory() && !f2.isDirectory())
				return -1;
			else if(!f1.isDirectory() && f2.isDirectory())
				return 1;
			else
				return Long.compare(f1.lastModified(), f2.lastModified());
		}
	}

	private static class SORT_FILE_FILEFOLDER_TIME_ASC implements Comparator<File>
	{
		@Override
		public int compare(File f1, File f2)
		{
			// TODO: Implement this method
			if (!f1.isDirectory() && f2.isDirectory())
				return -1;

			else if(f1.isDirectory() && !f2.isDirectory())
				return 1;
			else
				return Long.compare(f1.lastModified(), f2.lastModified());
		}
	}

	private static class SORT_FILE_FOLDERFILE_TIME_DESC implements Comparator<File>
	{
		@Override
		public int compare(File f1, File f2)
		{
			// TODO: Implement this method
			if (f1.isDirectory() && !f2.isDirectory())
				return -1;

			else if(!f1.isDirectory() && f2.isDirectory())
				return 1;
			else
			{
				int i= Long.compare(f1.lastModified(), f2.lastModified());
				return -(i);
			}
		}
	}

	private static class SORT_FILE_FILEFOLDER_TIME_DESC implements Comparator<File>
	{

		@Override
		public int compare(File f1, File f2)
		{
			// TODO: Implement this method
			if (!f1.isDirectory() && f2.isDirectory())
				return -1;

			else if(f1.isDirectory() && !f2.isDirectory())
				return 1;
			else
			{
				int i= Long.compare(f1.lastModified(), f2.lastModified());
				return -(i);
			}
		}
	}






	private static class SORT_USB_FOLDERFILE_NAME_ASC implements Comparator<UsbFile>
	{
		@Override
		public int compare(UsbFile f1, UsbFile f2)
		{
			if (f1.isDirectory() && !f2.isDirectory())
				return -1;

			else if(!f1.isDirectory() && f2.isDirectory())
				return 1;
			else
				return f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
		}
	}

	private static class SORT_USB_FILEFOLDER_NAME_ASC implements Comparator<UsbFile>
	{
		@Override
		public int compare(UsbFile f1, UsbFile f2)
		{
			if (!f1.isDirectory() && f2.isDirectory())
				return -1;

			else if(f1.isDirectory() && !f2.isDirectory())
				return 1;
			else
				return f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
		}
	}


	private static class SORT_USB_FOLDERFILE_NAME_DESC implements Comparator<UsbFile>
	{
		@Override
		public int compare(UsbFile f1, UsbFile f2)
		{
			if (f1.isDirectory() && !f2.isDirectory())
				return -1;

			else if(!f1.isDirectory() && f2.isDirectory())
				return 1;
			else
			{
				int i=f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
				return -(i);
			}
		}
	}


	private static class SORT_USB_FILEFOLDER_NAME_DESC implements Comparator<UsbFile>
	{
		@Override
		public int compare(UsbFile f1, UsbFile f2)
		{
			if (!f1.isDirectory() && f2.isDirectory())
				return -1;

			else if(f1.isDirectory() && !f2.isDirectory())
				return 1;
			else
			{
				int i=f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
				return -(i);
			}
		}
	}

	private static class SORT_USB_FOLDERFILE_TIME_ASC implements Comparator<UsbFile>
	{

		@Override
		public int compare(UsbFile f1, UsbFile f2)
		{
			// TODO: Implement this method
			if (f1.isDirectory() && !f2.isDirectory())
				return -1;
			else if(!f1.isDirectory() && f2.isDirectory())
				return 1;
			else
				return Long.compare(f1.lastModified(), f2.lastModified());
		}
	}

	private static class SORT_USB_FILEFOLDER_TIME_ASC implements Comparator<UsbFile>
	{
		@Override
		public int compare(UsbFile f1, UsbFile f2)
		{
			// TODO: Implement this method
			if (!f1.isDirectory() && f2.isDirectory())
				return -1;

			else if(f1.isDirectory() && !f2.isDirectory())
				return 1;
			else
				return Long.compare(f1.lastModified(), f2.lastModified());
		}
	}

	private static class SORT_USB_FOLDERFILE_TIME_DESC implements Comparator<UsbFile>
	{
		@Override
		public int compare(UsbFile f1, UsbFile f2)
		{
			// TODO: Implement this method
			if (f1.isDirectory() && !f2.isDirectory())
				return -1;

			else if(!f1.isDirectory() && f2.isDirectory())
				return 1;
			else
			{
				int i= Long.compare(f1.lastModified(), f2.lastModified());
				return -(i);
			}
		}
	}

	private static class SORT_USB_FILEFOLDER_TIME_DESC implements Comparator<UsbFile>
	{

		@Override
		public int compare(UsbFile f1, UsbFile f2)
		{
			// TODO: Implement this method
			if (!f1.isDirectory() && f2.isDirectory())
				return -1;

			else if(f1.isDirectory() && !f2.isDirectory())
				return 1;
			else
			{
				int i= Long.compare(f1.lastModified(), f2.lastModified());
				return -(i);
			}
		}
	}





	//

	private static class SORT_APPPOJO_NAME_ASC implements Comparator<AppManagerListFragment.AppPOJO>
	{
		@Override
		public int compare(AppManagerListFragment.AppPOJO f1, AppManagerListFragment.AppPOJO f2)
		{
			return f1.getLowerName().compareTo(f2.getLowerName());
		}
	}


	private static class SORT_APPPOJO_NAME_DESC implements Comparator<AppManagerListFragment.AppPOJO>
	{
		@Override
		public int compare(AppManagerListFragment.AppPOJO f1, AppManagerListFragment.AppPOJO f2)
		{
				int i=f1.getLowerName().compareTo(f2.getLowerName());
				return -(i);
		}
	}

	private static class SORT_APPPOJO_TIME_ASC implements Comparator<AppManagerListFragment.AppPOJO>
	{

		@Override
		public int compare(AppManagerListFragment.AppPOJO f1, AppManagerListFragment.AppPOJO f2)
		{
				return Long.compare(f1.getDateLong(), f2.getDateLong());
		}
	}


	private static class SORT_APPPOJO_TIME_DESC implements Comparator<AppManagerListFragment.AppPOJO>
	{
		@Override
		public int compare(AppManagerListFragment.AppPOJO f1, AppManagerListFragment.AppPOJO f2)
		{
			int i= Long.compare(f1.getDateLong(), f2.getDateLong());
			return -(i);
		}
	}


	private static class SORT_APPPOJO_SIZE_ASC implements Comparator<AppManagerListFragment.AppPOJO>
	{
		@Override
		public int compare(AppManagerListFragment.AppPOJO f1, AppManagerListFragment.AppPOJO f2)
		{
			return Long.compare(f1.getSizeLong(),f2.getSizeLong());
		}
	}

	private static class SORT_APPPOJO_SIZE_DESC implements Comparator<AppManagerListFragment.AppPOJO>
	{
		@Override
		public int compare(AppManagerListFragment.AppPOJO f1, AppManagerListFragment.AppPOJO f2)
		{
			int i=Long.compare(f1.getSizeLong(),f2.getSizeLong());
			return -(i);
		}
	}



}
	
	

