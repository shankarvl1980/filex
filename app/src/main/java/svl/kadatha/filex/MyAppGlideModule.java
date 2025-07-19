package svl.kadatha.filex;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

import svl.kadatha.filex.pdfthumbnail.ThumbnailBuilderFactory;

@GlideModule
public final class MyAppGlideModule extends AppGlideModule {

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }

    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        super.applyOptions(context, builder);
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        //super.registerComponents(context, glide, registry);
        registry.prepend(String.class, Bitmap.class, new ThumbnailBuilderFactory(context));
    }
}
