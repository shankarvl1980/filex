package svl.kadatha.filex;

import android.content.Intent;
import android.os.Bundle;

public class ExternalIntentActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Process the intent
        // Get the original intent
        Intent intent = getIntent();

        // Set the component to MainActivity
        intent.setClass(this, MainActivity.class);

        // Add flags to bring MainActivity to the foreground if it's already running
        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // Start MainActivity with the modified intent
        startActivity(intent);

        // Finish ExternalIntentActivity
        finish();
    }
}
