package svl.kadatha.filex;

import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

@GlideModule
public final class MyAppGlideModule extends AppGlideModule
{

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
/*
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        // Change bitmap format to ARGB_8888
        builder.setDecodeFormat(DecodeFormat.PREFER_ARGB_8888);
    }

    @Override
    public void registerComponents(Context context, Glide glide) {
        // register ModelLoaders here.
    }

 */
}
