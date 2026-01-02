package svl.kadatha.filex.pdfthumbnail;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;

import java.io.File;
import java.io.FileOutputStream;

import svl.kadatha.filex.Global;

/**
 * 1st step
 * class contains the main logic for thumbnail generate
 * implements {@link ModelLoader} interface
 * {@link String} is the input and {@link Bitmap} is the output of the class
 */
public class ThumbnailBuilder implements ModelLoader<String, Bitmap> {

    private final Context mContext;

    public ThumbnailBuilder(Context mContext) {
        this.mContext = mContext;
    }

    @Nullable
    @Override
    public LoadData<Bitmap> buildLoadData(@NonNull String input, int width, int height, @NonNull Options options) {
        return new LoadData<>(new ObjectKey(input), new ThumbnailCreator(mContext, input));
    }

    @Override
    public boolean handles(@NonNull String input) {
        // handles only pdf file
        String p = input.toLowerCase();
        return p.endsWith(".pdf");
    }


    private static class ThumbnailCreator implements DataFetcher<Bitmap> {
        private final String input;

        public ThumbnailCreator(Context mContext, String input) {
            this.input = input;
        }

        @Override
        public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super Bitmap> callback) {
            try {
                File thumbnail = new File(Global.PDF_CACHE_DIR, Uri.parse(input).getLastPathSegment() + ".png");
                // check if file is already exist then there is no need to re create it
                if (!thumbnail.exists()) {
                    File file = new File(input);
                    try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)) {
                        try (PdfRenderer renderer = new PdfRenderer(pfd)) {
                            if (renderer.getPageCount() <= 0) {
                                callback.onLoadFailed(new IllegalStateException("Empty PDF"));
                                return;
                            }
                            PdfRenderer.Page page = renderer.openPage(0);
                            Bitmap output = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(output);
                            canvas.drawColor(Color.WHITE);
                            page.render(output, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                            page.close();

                            callback.onDataReady(output);

                            // Save thumbnail
                            try (FileOutputStream fos = new FileOutputStream(thumbnail)) {
                                output.compress(Bitmap.CompressFormat.PNG, 80, fos);
                            }
                        }
                    } catch (Exception e) {
                        callback.onLoadFailed(e);
                    }
                } else {
                    callback.onDataReady(BitmapFactory.decodeFile(thumbnail.getAbsolutePath()));
                }

            } catch (Exception e) {
                callback.onLoadFailed(e);
            }
        }

        @Override
        public void cleanup() {
            // empty
        }

        @Override
        public void cancel() {
            // empty
        }

        @NonNull
        @Override
        public Class<Bitmap> getDataClass() {
            return Bitmap.class;
        }

        @NonNull
        @Override
        public DataSource getDataSource() {
            // data source local or network base
            return DataSource.LOCAL;
        }
    }
}