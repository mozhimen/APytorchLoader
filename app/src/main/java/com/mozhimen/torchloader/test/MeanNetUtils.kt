package com.mozhimen.torchloader.test

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import com.mozhimen.kotlin.utilk.android.content.UtilKIntentWrapper
import com.mozhimen.kotlin.utilk.android.graphics.UtilKBitmap

/**
 * @ClassName MeanNetUtils
 * @Description TODO
 * @Author Mozhimen / Kolin Zhao
 * @Date 2024/11/4 23:13
 * @Version 1.0
 */

object MeanNetUtils {
    /**
     * @param activity 活动
     * @param requestCode 请求码
     * @return 拍摄图像的路径
     */
    fun startCamera(activity: Activity, uri: Uri, requestCode: Int) {
        val intent = UtilKIntentWrapper.getMediaStoreImageCaptureOutput(uri)
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * @param activity 活动
     * @param requestCode 请求码
     */
    fun startAlbum(activity: Activity, requestCode: Int) {
        val intent = UtilKIntentWrapper.getPickImageAll()
        activity.startActivityForResult(intent, requestCode)
    }


    /**
     * @param filePath 文件路径
     * @return Bitmap对象
     */
    fun getScaleBitmapByPath(filePath: String): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        UtilKBitmap.get(filePath, options)
        val width = options.outWidth
        val height = options.outHeight

        val maxSize = 500
        options.inSampleSize = 1
        while (true) {
            if (width / options.inSampleSize < maxSize || height / options.inSampleSize < maxSize) {
                break
            }
            options.inSampleSize *= 2
        }
        options.inJustDecodeBounds = false

        // 返回解码后的图片
        return UtilKBitmap.get(filePath, options)
    }


    /**
     * @param origin 原始Bitmap
     * @param newWidth 缩放后的宽度
     * @param newHeight 缩放后的高度
     * @return 缩放后的Bitmap
     */
    fun getScaleBitmapByBitmap(origin: Bitmap?, newWidth: Int, newHeight: Int): Bitmap? {
        // 如果输入的Bitmap为空，则直接返回
        if (origin == null) {
            return null
        }
        // 原始Bitmap的长宽
        val height = origin.height
        val width = origin.width

        // 计算缩放后的图像比例
        val scaleWidthRatio = (newWidth.toFloat()) / width
        val scaleHeightRatio = (newHeight.toFloat()) / height

        val matrix = Matrix()
        matrix.postScale(scaleWidthRatio, scaleHeightRatio)

        // 创建新的Bitmap
        val scaledBitmap = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false)
        if (!origin.isRecycled) {
            origin.recycle()
        }
        return scaledBitmap
    }
}
