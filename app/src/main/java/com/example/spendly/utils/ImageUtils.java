package com.example.spendly.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageUtils {
    private static final String TAG = "ImageUtils";
    private static final int MAX_IMAGE_SIZE = 800; // Maximum width/height in pixels
    private static final int COMPRESSION_QUALITY = 70; // JPEG compression quality

    /**
     * Convert image URI to Base64 string with compression
     */
    public static String uriToBase64(Context context, Uri imageUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for URI: " + imageUri);
                return null;
            }

            // Decode the image
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from URI: " + imageUri);
                return null;
            }

            // Resize bitmap to reduce size
            bitmap = resizeBitmap(bitmap, MAX_IMAGE_SIZE);

            // Convert to Base64
            return bitmapToBase64(bitmap);

        } catch (Exception e) {
            Log.e(TAG, "Error converting URI to Base64: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Convert Bitmap to Base64 string
     */
    public static String bitmapToBase64(Bitmap bitmap) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Error converting bitmap to Base64: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Convert Base64 string to Bitmap
     */
    public static Bitmap base64ToBitmap(String base64String) {
        try {
            if (base64String == null || base64String.isEmpty()) {
                return null;
            }
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Error converting Base64 to bitmap: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Resize bitmap to fit within max dimensions while maintaining aspect ratio
     */
    private static Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Calculate the scaling factor
        float scale = Math.min((float) maxSize / width, (float) maxSize / height);

        if (scale >= 1.0f) {
            // No need to resize
            return bitmap;
        }

        // Calculate new dimensions
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        // Create resized bitmap
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    /**
     * Check if string is a valid Base64 image
     */
    public static boolean isValidBase64Image(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            return false;
        }
        try {
            Bitmap bitmap = base64ToBitmap(base64String);
            return bitmap != null;
        } catch (Exception e) {
            return false;
        }
    }
}
