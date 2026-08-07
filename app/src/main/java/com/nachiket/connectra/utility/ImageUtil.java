package com.nachiket.connectra.utility;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Converts images to base64 data URIs so they can be stored directly in the
 * Realtime Database (Firebase Storage is no longer used). Glide can load
 * "data:image/jpeg;base64,..." strings natively, so the same RTDB fields keep
 * working everywhere in the app.
 */
public class ImageUtil {

    // Keep encoded images comfortably below RTDB/Worker limits
    private static final int MAX_DIMENSION = 720;
    private static final int MAX_BYTES = 400 * 1024; // ~400KB binary -> ~540KB base64

    public static String uriToDataUri(Context context, Uri uri) throws Exception {
        // First pass: read bounds only
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        InputStream in = context.getContentResolver().openInputStream(uri);
        BitmapFactory.decodeStream(in, null, bounds);
        if (in != null) in.close();

        // Compute sample size to get close to MAX_DIMENSION
        int sample = 1;
        int largest = Math.max(bounds.outWidth, bounds.outHeight);
        while (largest / (sample * 2) >= MAX_DIMENSION) {
            sample *= 2;
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        in = context.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(in, null, opts);
        if (in != null) in.close();
        if (bitmap == null) throw new Exception("Could not decode image");

        // Exact resize if still larger than MAX_DIMENSION
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int max = Math.max(w, h);
        if (max > MAX_DIMENSION) {
            float scale = (float) MAX_DIMENSION / max;
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap,
                    Math.round(w * scale), Math.round(h * scale), true);
            if (scaled != bitmap) bitmap.recycle();
            bitmap = scaled;
        }

        // Compress as JPEG, lowering quality until under the size cap
        int quality = 85;
        byte[] bytes;
        while (true) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
            bytes = out.toByteArray();
            if (bytes.length <= MAX_BYTES || quality <= 40) break;
            quality -= 10;
        }
        bitmap.recycle();

        return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
    }
}
