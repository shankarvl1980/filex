package svl.kadatha.filex;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

public class FtpDetailsDialog extends DialogFragment {

    private Context context;
    private Toolbar bottom_toolbar;
    private FragmentManager fragmentManager;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        fragmentManager=((AppCompatActivity)context).getSupportFragmentManager();

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO: Implement this method
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v=inflater.inflate(R.layout.fragment_ftp_list,container,false);
        Button add_btn=v.findViewById(R.id.fragment_ftp_add_btn);
        add_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FtpDetailsInputDialog ftpDetailsInputDialog=new FtpDetailsInputDialog();
                ftpDetailsInputDialog.show(fragmentManager,"");
            }
        });

        EquallyDistributedButtonsWithTextLayout tb_layout =new EquallyDistributedButtonsWithTextLayout(context,3,Global.SCREEN_WIDTH,Global.SCREEN_HEIGHT);
        int[] bottom_drawables ={R.drawable.delete_icon,R.drawable.rename_icon,R.drawable.edit_icon};
        String [] titles=new String[]{getString(R.string.delete),getString(R.string.rename),getString(R.string.edit)};
        tb_layout.setResourceImageDrawables(bottom_drawables,titles);
        bottom_toolbar=v.findViewById(R.id.fragment_ftp_toolbar);
        bottom_toolbar.addView(tb_layout);
        return v;
    }

    @Override
    public void onResume()
    {
        // TODO: Implement this method
        super.onResume();
        Window window=getDialog().getWindow();
        window.setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
        window.setBackgroundDrawable(new ColorDrawable(R.attr.dialog_recyclerview_background));

    }

    @Override
    public void onDestroy() {
        if(getDialog()!=null && getRetainInstance())
        {
            getDialog().setDismissMessage(null);
        }
        super.onDestroy();

    }

    public static class FtpPOJO
    {
        final String server,mode,user_name,password,encoding,display;
        final int port;
        final boolean anonymous;

        FtpPOJO(String server,int port,String mode, String user_name,String password,boolean anonymous,String encoding, String display)
        {
            this.server=server;
            this.port=port;
            this.mode=mode;
            this.user_name=user_name;
            this.password=password;
            this.anonymous=anonymous;
            this.encoding=encoding;
            this.display=display;
        }
    }

    private void print(String msg)
    {
        Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
    }
}