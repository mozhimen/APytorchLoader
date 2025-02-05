package com.mozhimen.torchloader.imagesegmentation.test;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;

public class Utils {

    /**
     * @param activity 活动
     * @param requestCode 请求码
     * @return 拍摄图像的路径
     */
    public static String startCamera(Activity activity, int requestCode) {
        Uri imageUri;
        // 将拍摄图像保存到指定路径,图片的名字由系统使用时间生成,这样就不会重名
        File outputImage = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/XiaoMei/", System.currentTimeMillis() + ".jpg");
        Log.d("outputImage", outputImage.getAbsolutePath());
        try {
            if (outputImage.exists()) {
                outputImage.delete();
            }
            File outPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/XiaoMei/");
            if (!outPath.exists()) {
                outPath.mkdirs();
            }
            outputImage.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT >= 23) {
            // 兼容性提升
            imageUri = FileProvider.getUriForFile(activity,
                    "com.example.aa175.fcn", outputImage);
        } else {
            imageUri = Uri.fromFile(outputImage);
        }
        // 开启相机的Intent
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        // 指定图片的保存路径
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        // 设置图像的质量
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        activity.startActivityForResult(intent, requestCode);
        // 返回图像的保存路径
        return outputImage.getAbsolutePath();
    }

    /**
     * @param activity 活动
     * @param requestCode 请求码
     */
    public static void startAlbum(Activity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * @param context 上下文
     * @param uri 资源标识
     * @return 路径值
     */
    public static String getPathFromUri(Context context, Uri uri) {

        String result;
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            result = uri.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    /**
     * @param image_array 输入的四维数组
     * @param dim_info 未读信息
     * @return
     */
    public static Bitmap getBitmap(float[][][] image_array, int[] dim_info) {
        int count = 0;
        int[] color_info = new int[dim_info[0] * dim_info[1]];
        // 遍历图像,获取颜色信息
        for (int i = 0; i < dim_info[0]; i++) {
            for (int j = 0; j < dim_info[1]; j++) {
                float[] arr = image_array[i][j];
                int alpha = 255;
                int red = (int) arr[0];
                int green = (int) arr[1];
                int blue = (int) arr[2];
                int tempARGB = (alpha << 24) | (red << 16) | (green << 8) | blue;
                color_info[count++] = tempARGB;
            }
        }
        // 创建bitmap对象
        return Bitmap.createBitmap(color_info, dim_info[0], dim_info[1], Bitmap.Config.ARGB_8888);
    }

    /**
     * @param filePath 文件路径
     * @return Bitmap对象
     */
    public static Bitmap getScaleBitmapByPath(String filePath) {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        int width = options.outWidth;
        int height = options.outHeight;

        int maxSize = 500;
        options.inSampleSize = 1;
        while (true) {
            if (width / options.inSampleSize < maxSize || height / options.inSampleSize < maxSize) {
                break;
            }
            options.inSampleSize *= 2;
        }
        options.inJustDecodeBounds = false;

        // 返回解码后的图片
        return BitmapFactory.decodeFile(filePath, options);
    }


    /**
     * @param origin 原始Bitmap
     * @param newWidth 缩放后的宽度
     * @param newHeight 缩放后的高度
     * @return 缩放后的Bitmap
     */
    public static Bitmap getScaleBitmapByBitmap(Bitmap origin, int newWidth, int newHeight) {
        // 如果输入的Bitmap为空，则直接返回
        if (origin == null) {
            return null;
        }
        // 原始Bitmap的长宽
        int height = origin.getHeight();
        int width = origin.getWidth();

        // 计算缩放后的图像比例
        float scaleWidthRatio= ((float) newWidth) / width;
        float scaleHeightRatio = ((float) newHeight) / height;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidthRatio, scaleHeightRatio);

        // 创建新的Bitmap
        Bitmap scaledBitmap = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (!origin.isRecycled()) {
            origin.recycle();
        }
        return scaledBitmap;
    }
}