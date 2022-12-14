package com.ajitmaurya.imagepicker;

import android.content.Context;
import android.media.ExifInterface;
import android.net.Uri;

import com.ajitmaurya.imagepicker.config.PictureMimeType;
import com.ajitmaurya.imagepicker.tools.PictureFileUtils;
import com.ajitmaurya.imagepicker.tools.SdkVersionUtils;

import java.io.InputStream;

/**
 * @author：luck
 * @date：2020-04-12 13:13
 * @describe：PictureSelector对外提供的一些方法
 */
public class PictureSelectorExternalUtils {
    /**
     * 获取ExifInterface
     *
     * @param context
     * @param url
     * @return
     */
    public static ExifInterface getExifInterface(Context context, String url) {
        ExifInterface exifInterface = null;
        InputStream inputStream = null;
        try {
            if (SdkVersionUtils.checkedAndroid_Q() && PictureMimeType.isContent(url)) {
                inputStream = context.getContentResolver().openInputStream(Uri.parse(url));
                if (inputStream != null) {
                    exifInterface = new ExifInterface(inputStream);
                }
            } else {
                exifInterface = new ExifInterface(url);
            }
            return exifInterface;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            PictureFileUtils.close(inputStream);
        }
        return null;
    }
}
