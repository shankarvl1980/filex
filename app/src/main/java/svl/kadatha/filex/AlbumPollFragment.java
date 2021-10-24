package svl.kadatha.filex;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlbumPollFragment extends Fragment
{

    private final IndexedLinkedHashMap<FilePOJO,Integer> video_list=new IndexedLinkedHashMap<>();
    public String selected_file_path,regex,source_folder;
    public boolean fromArchiveView,fromThirdPartyApp;
    AsyncTaskStatus asyncTaskStatus;
    public FilePOJO currently_shown_file;
    public int file_selected_idx;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        asyncTaskStatus= AsyncTaskStatus.NOT_YET_STARTED;
        FilenameFilter file_name_filter = new FilenameFilter() {
            public boolean accept(File fi, String na) {
                if (MainActivity.SHOW_HIDDEN_FILE) {
                    return true;
                } else {
                    return !na.startsWith(".");
                }
            }
        };

        Bundle bundle=getArguments();
        if(bundle!=null)
        {
            selected_file_path=bundle.getString("selected_file_path");
            regex=bundle.getString("regex");
            FileObjectType fileObjectType = (FileObjectType) bundle.getSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE);
            fromArchiveView=bundle.getBoolean(FileIntentDispatch.EXTRA_FROM_ARCHIVE);

            if(fileObjectType ==null || fileObjectType ==FileObjectType.SEARCH_LIBRARY_TYPE)
            {
                fromThirdPartyApp=true;
                fileObjectType =FileObjectType.FILE_TYPE;
            }

            source_folder=new File(selected_file_path).getParent();
            if(fileObjectType ==FileObjectType.USB_TYPE)
            {
                if(MainActivity.usbFileRoot!=null)
                {
                    try {
                        currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(MainActivity.usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(selected_file_path)),false);
                    } catch (IOException e) {

                    }
                }
            }
            else
            {
                currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(new File(selected_file_path),false,false,FileObjectType.FILE_TYPE);
            }
            new AlbumPollingAsyncTask(regex,source_folder, fileObjectType,fromArchiveView).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    static AlbumPollFragment getInstance(String selected_file_path,FileObjectType fileObjectType,String regex,boolean fromArchiveView)
    {
        AlbumPollFragment albumPollFragment=new AlbumPollFragment();
        Bundle bundle=new Bundle();
        bundle.putString("selected_file_path",selected_file_path);
        bundle.putSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE,fileObjectType);
        bundle.putString("regex",regex);
        bundle.putBoolean(FileIntentDispatch.EXTRA_FROM_ARCHIVE,fromArchiveView);
        albumPollFragment.setArguments(bundle);
        return albumPollFragment;
    }

    private class AlbumPollingAsyncTask extends svl.kadatha.filex.AsyncTask<Void,Void,Void>
    {
        final FileObjectType fileObjectType;
        final String source_folder;
        final boolean fromArchiveView;
        final String regex;

        AlbumPollingAsyncTask(String regex ,String source_folder ,FileObjectType fileObjectType,boolean fromArchiveView)
        {
            this.regex=regex;
            this.source_folder=source_folder;
            this.fileObjectType=fileObjectType;
            this.fromArchiveView=fromArchiveView;
        }
        @Override
        protected void onPreExecute() {
            asyncTaskStatus=AsyncTaskStatus.STARTED;
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            List<FilePOJO> filePOJOS=new ArrayList<>(), filePOJOS_filtered=new ArrayList<>();
            if (!Global.HASHMAP_FILE_POJO.containsKey(fileObjectType+source_folder))
            {
                FilePOJOUtil.FILL_FILEPOJO(filePOJOS,filePOJOS_filtered,fileObjectType,source_folder,null,false);
            }
            else
            {
                if(MainActivity.SHOW_HIDDEN_FILE)
                {
                    filePOJOS=Global.HASHMAP_FILE_POJO.get(fileObjectType+source_folder) ;
                }
                else
                {
                    filePOJOS=Global.HASHMAP_FILE_POJO_FILTERED.get(fileObjectType+source_folder);
                }
            }

            // limiting to the selected only, in case of file selected from usb storage by adding condition below
            if(fromArchiveView || fromThirdPartyApp || fileObjectType==FileObjectType.USB_TYPE)
            {
                video_list.put(currently_shown_file,0);
            }
            else
            {
                Collections.sort(filePOJOS,FileComparator.FilePOJOComparate(Global.SORT,false));
                int size=filePOJOS.size();
                int count=0;
                for(int i=0; i<size;++i)
                {
                    FilePOJO filePOJO=filePOJOS.get(i);
                    if(!filePOJO.getIsDirectory())
                    {
                        String file_ext;
                        int idx=filePOJO.getName().lastIndexOf(".");
                        if(idx!=-1)
                        {
                            file_ext=filePOJO.getName().substring(idx+1);
                            if(file_ext.matches(Global.VIDEO_REGEX))
                            {

                                video_list.put(filePOJO,0);
                                if(filePOJO.getName().equals(currently_shown_file.getName())) file_selected_idx=count;
                                count++;

                            }
                            else if(filePOJO.getName().equals(currently_shown_file.getName()))
                            {
                                video_list.put(currently_shown_file,0);
                                file_selected_idx=count;
                                count++;
                            }

                        }
                        else if(filePOJO.getName().equals(currently_shown_file.getName()))
                        {
                            video_list.put(currently_shown_file,0);
                            file_selected_idx=count;
                            count++;
                        }

                    }
                }

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            asyncTaskStatus=AsyncTaskStatus.COMPLETED;
        }

    }

    public IndexedLinkedHashMap<FilePOJO,Integer> getVideoPollingResult()
    {
        if(asyncTaskStatus==AsyncTaskStatus.COMPLETED)
        {
            return this.video_list;
        }
        else
        {
            return null;
        }
    }

}

