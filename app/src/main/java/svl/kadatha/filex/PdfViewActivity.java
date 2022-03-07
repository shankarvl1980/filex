package svl.kadatha.filex;

import androidx.fragment.app.FragmentManager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class PdfViewActivity extends BaseActivity {

    private Context context;
    public FragmentManager fm;
    public Uri data;
    public static final String ACTIVITY_NAME="PDF_VIEW_ACTIVITY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context=this;
        setContentView(R.layout.activity_blank_view);
        fm=getSupportFragmentManager();
        TinyDB tinyDB = new TinyDB(context);

        Intent intent=getIntent();
        if(savedInstanceState==null)
        {
           on_intent(intent);
        }
    }

    private void on_intent(Intent intent)
    {
        data=intent.getData();
        boolean fromArchiveView = intent.getBooleanExtra(FileIntentDispatch.EXTRA_FROM_ARCHIVE, false);
        FileObjectType fileObjectType = Global.GET_FILE_OBJECT_TYPE(intent.getStringExtra(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE));
        String file_path = intent.getStringExtra(FileIntentDispatch.EXTRA_FILE_PATH);
        if(file_path ==null) file_path =PathUtil.getPath(context,data);
        fm.beginTransaction().replace(R.id.activity_blank_view_container,PdfViewFragment_view_pager.getNewInstance(file_path, fromArchiveView, fileObjectType),"pdf_view_fragment").commit();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        on_intent(intent);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // TODO: Implement this method
        super.onActivityResult(requestCode, resultCode, data);
    }


    private void print(String msg)
    {
        Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
    }

}