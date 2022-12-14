package com.ajitmaurya.imagepicker;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.ajitmaurya.imagepicker.broadcast.BroadcastAction;
import com.ajitmaurya.imagepicker.broadcast.BroadcastManager;
import com.ajitmaurya.imagepicker.config.PictureConfig;
import com.ajitmaurya.imagepicker.config.PictureMimeType;
import com.ajitmaurya.imagepicker.config.PictureSelectionConfig;
import com.ajitmaurya.imagepicker.dialog.PictureCustomDialog;
import com.ajitmaurya.imagepicker.entity.LocalMedia;
import com.ajitmaurya.imagepicker.listener.OnImageCompleteCallback;
import com.ajitmaurya.imagepicker.permissions.PermissionChecker;
import com.ajitmaurya.imagepicker.photoview.PhotoView;
import com.ajitmaurya.imagepicker.thread.PictureThreadUtils;
import com.ajitmaurya.imagepicker.tools.AttrsUtils;
import com.ajitmaurya.imagepicker.tools.DateUtils;
import com.ajitmaurya.imagepicker.tools.JumpUtils;
import com.ajitmaurya.imagepicker.tools.MediaUtils;
import com.ajitmaurya.imagepicker.tools.PictureFileUtils;
import com.ajitmaurya.imagepicker.tools.SdkVersionUtils;
import com.ajitmaurya.imagepicker.tools.ToastUtils;
import com.ajitmaurya.imagepicker.tools.ValueOf;
import com.ajitmaurya.imagepicker.widget.PreviewViewPager;
import com.ajitmaurya.imagepicker.widget.longimage.ImageSource;
import com.ajitmaurya.imagepicker.widget.longimage.ImageViewState;
import com.ajitmaurya.imagepicker.widget.longimage.SubsamplingScaleImageView;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okio.BufferedSource;
import okio.Okio;

/**
 * @author???luck
 * @data???2017/01/18 ??????1:00
 * @??????: ????????????
 */
public class PictureExternalPreviewActivity extends PictureBaseActivity implements View.OnClickListener {

    private ImageButton ibLeftBack;
    private TextView tvTitle;
    private PreviewViewPager viewPager;
    private List<LocalMedia> images = new ArrayList<>();
    private int position = 0;
    private SimpleFragmentAdapter adapter;
    private String downloadPath;
    private String mMimeType;
    private ImageButton ibDelete;
    private View titleViewBg;

    @Override
    public int getResourceId() {
        return R.layout.picture_activity_external_preview;
    }

    @Override
    protected void initWidgets() {
        super.initWidgets();
        titleViewBg = findViewById(R.id.titleViewBg);
        tvTitle = findViewById(R.id.picture_title);
        ibLeftBack = findViewById(R.id.left_back);
        ibDelete = findViewById(R.id.ib_delete);
        viewPager = findViewById(R.id.preview_pager);
        position = getIntent().getIntExtra(PictureConfig.EXTRA_POSITION, 0);
        images = (List<LocalMedia>) getIntent().getSerializableExtra(PictureConfig.EXTRA_PREVIEW_SELECT_LIST);
        ibLeftBack.setOnClickListener(this);
        ibDelete.setOnClickListener(this);
        ibDelete.setVisibility(config.style != null ? config.style.pictureExternalPreviewGonePreviewDelete
                ? View.VISIBLE : View.GONE : View.GONE);
        initViewPageAdapterData();
    }

    /**
     * ????????????
     */
    @Override
    public void initPictureSelectorStyle() {
        if (config.style != null) {
            if (config.style.pictureTitleTextColor != 0) {
                tvTitle.setTextColor(config.style.pictureTitleTextColor);
            }
            if (config.style.pictureTitleTextSize != 0) {
                tvTitle.setTextSize(config.style.pictureTitleTextSize);
            }
            if (config.style.pictureLeftBackIcon != 0) {
                ibLeftBack.setImageResource(config.style.pictureLeftBackIcon);
            }
            if (config.style.pictureExternalPreviewDeleteStyle != 0) {
                ibDelete.setImageResource(config.style.pictureExternalPreviewDeleteStyle);
            }
            if (config.style.pictureTitleBarBackgroundColor != 0) {
                titleViewBg.setBackgroundColor(colorPrimary);
            }
        } else {
            int previewBgColor = AttrsUtils.getTypeValueColor(getContext(), R.attr.picture_ac_preview_title_bg);
            if (previewBgColor != 0) {
                titleViewBg.setBackgroundColor(previewBgColor);
            } else {
                titleViewBg.setBackgroundColor(colorPrimary);
            }
        }
    }

    private void initViewPageAdapterData() {
        tvTitle.setText(getString(R.string.picture_preview_image_num,
                position + 1, images.size()));
        adapter = new SimpleFragmentAdapter();
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(position);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int index) {
                tvTitle.setText(getString(R.string.picture_preview_image_num,
                        index + 1, images.size()));
                position = index;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.left_back) {
            finish();
            exitAnimation();
        } else if (id == R.id.ib_delete) {
            if (images != null && images.size() > 0) {
                int currentItem = viewPager.getCurrentItem();
                images.remove(currentItem);
                adapter.removeCacheView(currentItem);
                // ????????????????????????
                Bundle bundle = new Bundle();
                bundle.putInt(PictureConfig.EXTRA_PREVIEW_DELETE_POSITION, currentItem);
                BroadcastManager.getInstance(getContext())
                        .action(BroadcastAction.ACTION_DELETE_PREVIEW_POSITION)
                        .extras(bundle).broadcast();
                if (images.size() == 0) {
                    onBackPressed();
                    return;
                }
                tvTitle.setText(getString(R.string.picture_preview_image_num,
                        position + 1, images.size()));
                position = currentItem;
                adapter.notifyDataSetChanged();
            }
        }
    }

    public class SimpleFragmentAdapter extends PagerAdapter {

        /**
         * ????????????????????????
         */
        private static final int MAX_CACHE_SIZE = 20;
        /**
         * ??????view
         */
        private SparseArray<View> mCacheView;

        private void clear() {
            if (null != mCacheView) {
                mCacheView.clear();
                mCacheView = null;
            }
        }

        public void removeCacheView(int position) {
            if (mCacheView != null && position < mCacheView.size()) {
                mCacheView.removeAt(position);
            }
        }

        public SimpleFragmentAdapter() {
            super();
            this.mCacheView = new SparseArray<>();
        }

        @Override
        public int getCount() {
            return images != null ? images.size() : 0;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            (container).removeView((View) object);
            if (mCacheView.size() > MAX_CACHE_SIZE) {
                mCacheView.remove(position);
            }
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View contentView = mCacheView.get(position);
            if (contentView == null) {
                contentView = LayoutInflater.from(container.getContext())
                        .inflate(R.layout.picture_image_preview, container, false);
                mCacheView.put(position, contentView);
            }
            // ???????????????
            final PhotoView imageView = contentView.findViewById(R.id.preview_image);
            // ????????????
            final SubsamplingScaleImageView longImageView = contentView.findViewById(R.id.longImg);
            // ??????????????????
            ImageView ivPlay = contentView.findViewById(R.id.iv_play);
            LocalMedia media = images.get(position);
            if (media != null) {
                final String path;
                if (media.isCut() && !media.isCompressed()) {
                    // ?????????
                    path = media.getCutPath();
                } else if (media.isCompressed() || (media.isCut() && media.isCompressed())) {
                    // ?????????,???????????????????????????,??????????????????????????????
                    path = media.getCompressPath();
                } else if (!TextUtils.isEmpty(media.getAndroidQToPath())) {
                    // AndroidQ??????path
                    path = media.getAndroidQToPath();
                } else {
                    // ??????
                    path = media.getPath();
                }
                boolean isHttp = PictureMimeType.isHasHttp(path);
                String mimeType = isHttp ? PictureMimeType.getImageMimeType(media.getPath()) : media.getMimeType();
                boolean isHasVideo = PictureMimeType.isHasVideo(mimeType);
                ivPlay.setVisibility(isHasVideo ? View.VISIBLE : View.GONE);
                boolean isGif = PictureMimeType.isGif(mimeType);
                boolean eqLongImg = MediaUtils.isLongImg(media);
                imageView.setVisibility(eqLongImg && !isGif ? View.GONE : View.VISIBLE);
                longImageView.setVisibility(eqLongImg && !isGif ? View.VISIBLE : View.GONE);
                // ????????????gif?????????gif???
                if (isGif && !media.isCompressed()) {
                    if (config != null && PictureSelectionConfig.imageEngine != null) {
                        PictureSelectionConfig.imageEngine.loadAsGifImage
                                (getContext(), path, imageView);
                    }
                } else {
                    if (config != null && PictureSelectionConfig.imageEngine != null) {
                        if (isHttp) {
                            // ????????????
                            PictureSelectionConfig.imageEngine.loadImage(contentView.getContext(), path,
                                    imageView, longImageView, new OnImageCompleteCallback() {
                                        @Override
                                        public void onShowLoading() {
                                            showPleaseDialog();
                                        }

                                        @Override
                                        public void onHideLoading() {
                                            dismissDialog();
                                        }
                                    });
                        } else {
                            if (eqLongImg) {
                                displayLongPic(PictureMimeType.isContent(path)
                                        ? Uri.parse(path) : Uri.fromFile(new File(path)), longImageView);
                            } else {
                                PictureSelectionConfig.imageEngine.loadImage(contentView.getContext(), path, imageView);
                            }
                        }
                    }
                }
                imageView.setOnViewTapListener((view, x, y) -> {
                    finish();
                    exitAnimation();
                });
                longImageView.setOnClickListener(v -> {
                    finish();
                    exitAnimation();
                });
                if (!isHasVideo) {
                    longImageView.setOnLongClickListener(v -> {
                        if (config.isNotPreviewDownload) {
                            if (PermissionChecker.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                downloadPath = path;
                                String currentMimeType = PictureMimeType.isHasHttp(path) ? PictureMimeType.getImageMimeType(media.getPath()) : media.getMimeType();
                                mMimeType = PictureMimeType.isJPG(currentMimeType) ? PictureMimeType.MIME_TYPE_JPEG : currentMimeType;
                                showDownLoadDialog();
                            } else {
                                PermissionChecker.requestPermissions(PictureExternalPreviewActivity.this,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE);
                            }
                        }
                        return true;
                    });
                }
                if (!isHasVideo) {
                    imageView.setOnLongClickListener(v -> {
                        if (config.isNotPreviewDownload) {
                            if (PermissionChecker.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                downloadPath = path;
                                String currentMimeType = PictureMimeType.isHasHttp(path) ? PictureMimeType.getImageMimeType(media.getPath()) : media.getMimeType();
                                mMimeType = PictureMimeType.isJPG(currentMimeType) ? PictureMimeType.MIME_TYPE_JPEG : currentMimeType;
                                showDownLoadDialog();
                            } else {
                                PermissionChecker.requestPermissions(PictureExternalPreviewActivity.this,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE);
                            }
                        }
                        return true;
                    });
                }
                ivPlay.setOnClickListener(v -> {
                    if (PictureSelectionConfig.customVideoPlayCallback != null) {
                        PictureSelectionConfig.customVideoPlayCallback.startPlayVideo(media);
                    } else {
                        Intent intent = new Intent();
                        Bundle bundle = new Bundle();
                        bundle.putString(PictureConfig.EXTRA_VIDEO_PATH, path);
                        intent.putExtras(bundle);
                        JumpUtils.startPictureVideoPlayActivity(container.getContext(), bundle, PictureConfig.PREVIEW_VIDEO_CODE);
                    }
                });
            }
            (container).addView(contentView, 0);
            return contentView;
        }
    }

    /**
     * ????????????
     *
     * @param uri
     * @param longImg
     */
    private void displayLongPic(Uri uri, SubsamplingScaleImageView longImg) {
        longImg.setQuickScaleEnabled(true);
        longImg.setZoomEnabled(true);
        longImg.setPanEnabled(true);
        longImg.setDoubleTapZoomDuration(100);
        longImg.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP);
        longImg.setDoubleTapZoomDpi(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER);
        longImg.setImage(ImageSource.uri(uri), new ImageViewState(0, new PointF(0, 0), 0));
    }

    /**
     * ??????????????????
     */
    private void showDownLoadDialog() {
        if (!isFinishing() && !TextUtils.isEmpty(downloadPath)) {
            final PictureCustomDialog dialog =
                    new PictureCustomDialog(getContext(), R.layout.picture_wind_base_dialog);
            Button btn_cancel = dialog.findViewById(R.id.btn_cancel);
            Button btn_commit = dialog.findViewById(R.id.btn_commit);
            TextView tvTitle = dialog.findViewById(R.id.tvTitle);
            TextView tv_content = dialog.findViewById(R.id.tv_content);
            tvTitle.setText(getString(R.string.picture_prompt));
            tv_content.setText(getString(R.string.picture_prompt_content));
            btn_cancel.setOnClickListener(v -> {
                if (!isFinishing()) {
                    dialog.dismiss();
                }
            });
            btn_commit.setOnClickListener(view -> {
                boolean isHttp = PictureMimeType.isHasHttp(downloadPath);
                showPleaseDialog();
                if (isHttp) {
                    PictureThreadUtils.executeByIo(new PictureThreadUtils.SimpleTask<String>() {
                        @Override
                        public String doInBackground() {
                            return showLoadingImage(downloadPath);
                        }

                        @Override
                        public void onSuccess(String result) {
                            onSuccessful(result);
                        }
                    });
                } else {
                    // ?????????????????????
                    try {
                        if (PictureMimeType.isContent(downloadPath)) {
                            savePictureAlbumAndroidQ(PictureMimeType.isContent(downloadPath) ? Uri.parse(downloadPath) : Uri.fromFile(new File(downloadPath)));
                        } else {
                            // ??????????????????????????????
                            savePictureAlbum();
                        }
                    } catch (Exception e) {
                        ToastUtils.s(getContext(), getString(R.string.picture_save_error) + "\n" + e.getMessage());
                        dismissDialog();
                        e.printStackTrace();
                    }
                }
                if (!isFinishing()) {
                    dialog.dismiss();
                }
            });
            dialog.show();
        }
    }

    /**
     * ???????????????????????????
     *
     * @throws Exception
     */
    private void savePictureAlbum() throws Exception {
        String suffix = PictureMimeType.getLastImgSuffix(mMimeType);
        String state = Environment.getExternalStorageState();
        File rootDir = state.equals(Environment.MEDIA_MOUNTED)
                ? Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                : getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (rootDir != null && !rootDir.exists() && rootDir.mkdirs()) {
        }
        File folderDir = new File(SdkVersionUtils.checkedAndroid_Q() || !state.equals(Environment.MEDIA_MOUNTED)
                ? rootDir.getAbsolutePath() : rootDir.getAbsolutePath() + File.separator + PictureMimeType.CAMERA + File.separator);
        if (folderDir != null && !folderDir.exists() && folderDir.mkdirs()) {
        }
        String fileName = DateUtils.getCreateFileName("IMG_") + suffix;
        File file = new File(folderDir, fileName);
        PictureFileUtils.copyFile(downloadPath, file.getAbsolutePath());
        onSuccessful(file.getAbsolutePath());
    }

    /**
     * ??????????????????
     *
     * @param result
     */
    private void onSuccessful(String result) {
        dismissDialog();
        if (!TextUtils.isEmpty(result)) {
            try {
                if (!SdkVersionUtils.checkedAndroid_Q()) {
                    File file = new File(result);
                    MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), null);
                    new PictureMediaScannerConnection(getContext(), file.getAbsolutePath(), () -> {
                    });
                }
                ToastUtils.s(getContext(), getString(R.string.picture_save_success) + "\n" + result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            ToastUtils.s(getContext(), getString(R.string.picture_save_error));
        }
    }

    /**
     * ???????????????picture ?????????Android Q???????????????????????????????????????????????????????????????SAF??????
     *
     * @param inputUri
     */
    private void savePictureAlbumAndroidQ(Uri inputUri) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, DateUtils.getCreateFileName("IMG_"));
        contentValues.put(MediaStore.Images.Media.DATE_TAKEN, ValueOf.toString(System.currentTimeMillis()));
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, mMimeType);
        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, PictureMimeType.DCIM);
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        if (uri == null) {
            ToastUtils.s(getContext(), getString(R.string.picture_save_error));
            return;
        }
        PictureThreadUtils.executeByIo(new PictureThreadUtils.SimpleTask<String>() {

            @Override
            public String doInBackground() {
                BufferedSource buffer = null;
                try {
                    buffer = Okio.buffer(Okio.source(Objects.requireNonNull(getContentResolver().openInputStream(inputUri))));
                    OutputStream outputStream = getContentResolver().openOutputStream(uri);
                    boolean bufferCopy = PictureFileUtils.bufferCopy(buffer, outputStream);
                    if (bufferCopy) {
                        return PictureFileUtils.getPath(getContext(), uri);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (buffer != null && buffer.isOpen()) {
                        PictureFileUtils.close(buffer);
                    }
                }
                return "";
            }

            @Override
            public void onSuccess(String result) {
                PictureThreadUtils.cancel(PictureThreadUtils.getIoPool());
                onSuccessful(result);
            }
        });
    }


    /**
     * ??????Q????????????uri
     *
     * @return
     */
    private Uri createOutImageUri() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, DateUtils.getCreateFileName("IMG_"));
        contentValues.put(MediaStore.Images.Media.DATE_TAKEN, ValueOf.toString(System.currentTimeMillis()));
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, mMimeType);
        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, PictureMimeType.DCIM);

        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
    }

    // ???????????????????????????
    public String showLoadingImage(String urlPath) {
        Uri outImageUri = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        BufferedSource inBuffer = null;
        try {
            if (SdkVersionUtils.checkedAndroid_Q()) {
                outImageUri = createOutImageUri();
            } else {
                String suffix = PictureMimeType.getLastImgSuffix(mMimeType);
                String state = Environment.getExternalStorageState();
                File rootDir =
                        state.equals(Environment.MEDIA_MOUNTED)
                                ? Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                                : getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                if (rootDir != null) {
                    if (!rootDir.exists()) {
                        rootDir.mkdirs();
                    }
                    File folderDir = new File(!state.equals(Environment.MEDIA_MOUNTED)
                            ? rootDir.getAbsolutePath() : rootDir.getAbsolutePath() + File.separator + PictureMimeType.CAMERA + File.separator);
                    if (!folderDir.exists() && folderDir.mkdirs()) {
                    }
                    String fileName = DateUtils.getCreateFileName("IMG_") + suffix;
                    File file = new File(folderDir, fileName);
                    outImageUri = Uri.fromFile(file);
                }
            }
            if (outImageUri != null) {
                outputStream = Objects.requireNonNull(getContentResolver().openOutputStream(outImageUri));
                URL u = new URL(urlPath);
                inputStream = u.openStream();
                inBuffer = Okio.buffer(Okio.source(inputStream));
                boolean bufferCopy = PictureFileUtils.bufferCopy(inBuffer, outputStream);
                if (bufferCopy) {
                    return PictureFileUtils.getPath(this, outImageUri);
                }
            }
        } catch (Exception e) {
            if (outImageUri != null && SdkVersionUtils.checkedAndroid_Q()) {
                getContentResolver().delete(outImageUri, null, null);
            }
        } finally {
            PictureFileUtils.close(inputStream);
            PictureFileUtils.close(outputStream);
            PictureFileUtils.close(inBuffer);
        }
        return null;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        exitAnimation();
    }

    private void exitAnimation() {
        overridePendingTransition(R.anim.picture_anim_fade_in, config.windowAnimationStyle != null
                && config.windowAnimationStyle.activityPreviewExitAnimation != 0
                ? config.windowAnimationStyle.activityPreviewExitAnimation : R.anim.picture_anim_exit);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) {
            adapter.clear();
        }
        if (PictureSelectionConfig.customVideoPlayCallback != null) {
            PictureSelectionConfig.customVideoPlayCallback = null;
        }
        if (PictureSelectionConfig.onCustomCameraInterfaceListener != null) {
            PictureSelectionConfig.onCustomCameraInterfaceListener = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE:
                // ????????????
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        showDownLoadDialog();
                    } else {
                        ToastUtils.s(getContext(), "Error 4");
                    }
                }
                break;
        }
    }
}
