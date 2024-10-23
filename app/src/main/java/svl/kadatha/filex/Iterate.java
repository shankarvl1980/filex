package svl.kadatha.filex;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Stack;

import me.jahnen.libaums.core.fs.UsbFile;
import svl.kadatha.filex.filemodel.FileModel;

public class Iterate {

    private static final List<Integer> FOLDERWISE_NO_OF_FILES = new ArrayList<>();
    private static final List<Long> FOLDERWISE_SIZE_OF_FILES = new ArrayList<>();
    private static int TOTAL_NO_OF_FILES;
    private static long TOTAL_SIZE_OF_FILES;

    public static List<FileModel> populate(FileModel[] source_file_model_list, List<FileModel> target_file_model_list, boolean include_folder) {
        Stack<FileModel[]> stack = new Stack<>();
        stack.push(source_file_model_list);

        while (!stack.isEmpty()) {
            FileModel[] current_file_model_list = stack.pop();
            int size = current_file_model_list.length;

            for (int i = 0; i < size; ++i) {
                int no_of_files = 0;
                long size_of_files = 0L;
                FileModel fileModel = current_file_model_list[i];

                if (fileModel.isDirectory()) {
                    FileModel[] sub_file_model_array = fileModel.list();
                    if (sub_file_model_array != null) {
                        if (sub_file_model_array.length != 0) {
                            stack.push(sub_file_model_array);
                        }
                        target_file_model_list.add(fileModel);
                        if (include_folder) {
                            no_of_files++;
                        }
                    }
                } else {
                    target_file_model_list.add(fileModel);
                    no_of_files++;
                    size_of_files += fileModel.getLength();
                }

                FOLDERWISE_NO_OF_FILES.add(no_of_files);
                FOLDERWISE_SIZE_OF_FILES.add(size_of_files);

                TOTAL_NO_OF_FILES += no_of_files;
                TOTAL_SIZE_OF_FILES += size_of_files;
            }
        }

        return target_file_model_list;
    }


    public static List<File> populate(File[] source_list_files, List<File> target_list_files, boolean include_folder) {
        Stack<File[]> stack = new Stack<>();
        stack.push(source_list_files);

        while (!stack.isEmpty()) {
            File[] current_list_files = stack.pop();
            int size = current_list_files.length;

            for (int i = 0; i < size; ++i) {
                int no_of_files = 0;
                long size_of_files = 0L;
                File f = current_list_files[i];

                if (f.isDirectory()) {
                    File[] files_array = f.listFiles();
                    if (files_array != null) {
                        if (files_array.length != 0) {
                            stack.push(files_array);
                        }
                        target_list_files.add(f);
                        if (include_folder) {
                            no_of_files++;
                        }
                    }
                } else {
                    target_list_files.add(f);
                    no_of_files++;
                    size_of_files += f.length();
                }

                FOLDERWISE_NO_OF_FILES.add(no_of_files);
                FOLDERWISE_SIZE_OF_FILES.add(size_of_files);

                TOTAL_NO_OF_FILES += no_of_files;
                TOTAL_SIZE_OF_FILES += size_of_files;
            }
        }

        return target_list_files;
    }


    public static List<File> populate(File f, List<File> target_list_files, boolean include_folder) {
        Stack<File> stack = new Stack<>();
        stack.push(f);

        while (!stack.isEmpty()) {
            File currentFile = stack.pop();
            int no_of_files = 0;
            long size_of_files = 0L;

            if (currentFile.isDirectory()) {
                File[] files_array = currentFile.listFiles();
                if (files_array != null) {
                    for (File subFile : files_array) {
                        stack.push(subFile);
                    }
                    target_list_files.add(currentFile);
                    if (include_folder) {
                        no_of_files++;
                    }
                }
            } else {
                target_list_files.add(currentFile);
                no_of_files++;
                size_of_files += currentFile.length();
            }

            FOLDERWISE_NO_OF_FILES.add(no_of_files);
            FOLDERWISE_SIZE_OF_FILES.add(size_of_files);

            TOTAL_NO_OF_FILES += no_of_files;
            TOTAL_SIZE_OF_FILES += size_of_files;
        }

        return target_list_files;
    }

    public static List<UsbFile> populate(UsbFile[] source_list_files, List<UsbFile> target_list_files, boolean include_folder) {
        Stack<UsbFile[]> stack = new Stack<>();
        stack.push(source_list_files);

        while (!stack.isEmpty()) {
            UsbFile[] current_list_files = stack.pop();
            int size = current_list_files.length;

            for (int i = 0; i < size; ++i) {
                int no_of_files = 0;
                long size_of_files = 0L;
                UsbFile f = current_list_files[i];

                if (f.isDirectory()) {
                    try {
                        UsbFile[] files_array = f.listFiles();
                        if (files_array.length != 0) {
                            stack.push(files_array);
                        }
                        target_list_files.add(f);
                        if (include_folder) {
                            no_of_files++;
                        }
                    } catch (IOException | ConcurrentModificationException e) {
                        // Handle exception if needed
                    }
                } else {
                    target_list_files.add(f);
                    no_of_files++;
                    size_of_files += f.getLength();
                }

                FOLDERWISE_NO_OF_FILES.add(no_of_files);
                FOLDERWISE_SIZE_OF_FILES.add(size_of_files);

                TOTAL_NO_OF_FILES += no_of_files;
                TOTAL_SIZE_OF_FILES += size_of_files;
            }
        }

        return target_list_files;
    }


//	public static List<FileModel> populate(FileModel[] source_file_model_list, List<FileModel> target_file_model_list, boolean include_folder)
//	{
//		int size=source_file_model_list.length;
//		for(int i=0;i<size;++i)
//		{
//			int no_of_files=0;
//			long size_of_files=0L;
//			FileModel fileModel=source_file_model_list[i];
//			if(fileModel.isDirectory())
//			{
//				FileModel[] sub_file_model_array=fileModel.list();
//				if(sub_file_model_array!=null)
//				{
//					if (sub_file_model_array.length != 0) {
//						populate(sub_file_model_array, target_file_model_list, include_folder);
//					}
//					target_file_model_list.add(fileModel);
//					if(include_folder)
//					{
//						no_of_files++;
//					}
//				}
//
//			}
//			else
//			{
//				target_file_model_list.add(fileModel);
//				no_of_files++;
//				size_of_files+=fileModel.getLength();
//			}
//			FOLDERWISE_NO_OF_FILES.add(no_of_files);
//			FOLDERWISE_SIZE_OF_FILES.add(size_of_files);
//
//			TOTAL_NO_OF_FILES+=no_of_files;
//			TOTAL_SIZE_OF_FILES+=size_of_files;
//		}
//
//		return target_file_model_list;
//	}

//	public static List<File> populate(File[] source_list_files,List<File> target_list_files, boolean include_folder)
//	{
//		int size=source_list_files.length;
//		for(int i=0;i<size;++i)
//		{
//			int no_of_files=0;
//			long size_of_files=0L;
//			File f=source_list_files[i];
//			if(f.isDirectory())
//			{
//				File[] files_array=f.listFiles();
//				if(files_array!=null)
//				{
//					if (files_array.length != 0) {
//						populate(files_array, target_list_files, include_folder);
//					}
//					target_list_files.add(f);
//					if(include_folder)
//					{
//						no_of_files++;
//					}
//				}
//
//			}
//			else
//			{
//				target_list_files.add(f);
//				no_of_files++;
//				size_of_files+=f.length();
//			}
//			FOLDERWISE_NO_OF_FILES.add(no_of_files);
//			FOLDERWISE_SIZE_OF_FILES.add(size_of_files);
//
//			TOTAL_NO_OF_FILES+=no_of_files;
//			TOTAL_SIZE_OF_FILES+=size_of_files;
//		}
//
//		return target_list_files;
//	}

//	public static List<File> populate(File f,List<File> target_list_files, boolean include_folder)
//	{
//		int no_of_files=0;
//		long size_of_files=0L;
//		if(f.isDirectory())
//		{
//			File[] files_array=f.listFiles();
//			if(files_array!=null)
//			{
//				if (files_array!=null && files_array.length != 0) {
//					populate(files_array, target_list_files, include_folder);
//				}
//				target_list_files.add(f);
//				if(include_folder)
//				{
//					no_of_files++;
//				}
//			}
//		}
//		else
//		{
//			target_list_files.add(f);
//			no_of_files++;
//			size_of_files+=f.length();
//		}
//		FOLDERWISE_NO_OF_FILES.add(no_of_files);
//		FOLDERWISE_SIZE_OF_FILES.add(size_of_files);
//
//		TOTAL_NO_OF_FILES+=no_of_files;
//		TOTAL_SIZE_OF_FILES+=size_of_files;
//		return target_list_files;
//	}


//	public static List<UsbFile> populate(UsbFile[] source_list_files, List<UsbFile> target_list_files, boolean include_folder)
//	{
//		int size=source_list_files.length;
//		for(int i=0;i<size;++i)
//		{
//
//			int no_of_files=0;
//			long size_of_files=0L;
//			UsbFile f=source_list_files[i];
//			if(f.isDirectory())
//			{
//
//				try
//				{
//					UsbFile[] files_array=f.listFiles();
//					if (files_array.length != 0) {
//						populate(files_array, target_list_files, include_folder);
//					}
//					target_list_files.add(f);
//					if(include_folder)
//					{
//						no_of_files++;
//					}
//				}
//				catch (IOException | ConcurrentModificationException e)
//				{
//
//				}
//			}
//			else
//			{
//				target_list_files.add(f);
//				no_of_files++;
//				size_of_files+=f.getLength();
//			}
//			FOLDERWISE_NO_OF_FILES.add(no_of_files);
//			FOLDERWISE_SIZE_OF_FILES.add(size_of_files);
//
//			TOTAL_NO_OF_FILES+=no_of_files;
//			TOTAL_SIZE_OF_FILES+=size_of_files;
//
//		}
//
//		return target_list_files;
//	}


}
