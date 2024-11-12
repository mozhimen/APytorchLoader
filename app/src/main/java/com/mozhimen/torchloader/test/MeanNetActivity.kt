package com.mozhimen.torchloader.test

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.util.Log
import com.mozhimen.torchloader.test.databinding.ActivityMeanNetBinding
import com.mozhimen.bindk.bases.viewbinding.activity.BaseActivityVB
import com.mozhimen.kotlin.utilk.android.graphics.UtilKBitmap
import com.mozhimen.kotlin.utilk.android.graphics.UtilKBitmapFactory
import com.mozhimen.kotlin.utilk.android.net.uri2strFilePathName
import com.mozhimen.kotlin.utilk.android.widget.showToast
import com.mozhimen.kotlin.utilk.kotlin.UtilKStrPath
import com.mozhimen.kotlin.utilk.kotlin.createFile
import com.mozhimen.kotlin.utilk.kotlin.strFilePath2uri
import com.mozhimen.torchloader.basic.PytorchLoaderUtil
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class MeanNetActivity : BaseActivityVB<ActivityMeanNetBinding>() {
    companion object {
        const val START_CAMERA_CODE: Int = 1111// 开启相机和调用相册的请求码
        const val START_ALBUM_CODE: Int = 1112
    }

    private var meanNet: Module? = null// 创建pytorch模型
    private var showImagePath: String? = null// 定义ImageView对象show_image展示的图片的路径
    private var cameraImagePath: String? = null// 定义相机拍摄图像的路径
    private var albumImagePath: String? = null// 定义从相册选择图像的路径

    /////////////////////////////////////////////////////////////////////////

    override fun initView(savedInstanceState: Bundle?) {
        vb.buttonStartCamera.setOnClickListener {
            // 开启相机，返回相机拍摄图像的路径，传入的请求码是START_CAMERA_CODE
            cameraImagePath = UtilKStrPath.Absolute.Internal.getCache() + "/XiaoMei/" + System.currentTimeMillis().toString() + ".jpg"
            cameraImagePath!!.createFile()
            val uri = cameraImagePath!!.strFilePath2uri()
            if (uri != null) {
                MeanNetUtils.startCamera(this@MeanNetActivity, uri, START_CAMERA_CODE)
            }
        }
        vb.buttonStartAlbum.setOnClickListener {
            // 调用相册，传入的请求码是START_ALBUM_CODE
            MeanNetUtils.startAlbum(this@MeanNetActivity, START_ALBUM_CODE)
        }
        vb.buttonSaveImage.setOnClickListener {

        }
        vb.buttonStartPredict.setOnClickListener {
            showImagePath?.let { predictMean(it) }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                START_ALBUM_CODE -> {
                    if (data == null) {
                        Log.w(TAG, "user photo data is null")
                        return
                    }

                    val albumImageUri = data.data
                    if (albumImageUri != null) {
                        albumImagePath = albumImageUri.uri2strFilePathName()?:return
                        showImagePath = albumImagePath
                        val bitmapAlbum: Bitmap = MeanNetUtils.getScaleBitmapByPath(albumImagePath!!)
                        vb.imageViewShowOriginal.setImageBitmap(MeanNetUtils.getScaleBitmapByBitmap(bitmapAlbum, 448, 448))
                        "album start".showToast()
                    }
                }

                START_CAMERA_CODE -> {
                    showImagePath = cameraImagePath?:return
                    val bitmapCamera: Bitmap = MeanNetUtils.getScaleBitmapByPath(cameraImagePath!!)
                    "camera start".showToast()
                    vb.imageViewShowOriginal.setImageBitmap(MeanNetUtils.getScaleBitmapByBitmap(bitmapCamera, 448, 448))
                }
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////

    private fun saveImage(bmp: Bitmap, quality: Int): String? {
        val appDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) ?: return null
        val fileName = System.currentTimeMillis().toString() + ".jpg"
        val file = File(appDir, fileName)
        var fos: FileOutputStream? = null

        try {
            fos = FileOutputStream(file)
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, fos)
            fos.flush()
            return file.absolutePath
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (fos != null) {
                try {
                    fos.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return null
    }

    private fun predictMean(imagePath: String) {
        // 设置输入尺寸，和python代码中的设定保持一致
        val inDims = intArrayOf(256, 256, 3)
        var bmp: Bitmap? = null
        var scaledBmp: Bitmap? = null
        var filePath = ""

        // 加载pytorch模型，加载失败则抛出异常
        try {
            filePath = PytorchLoaderUtil.strAssetModelName2file(this, "mean_net.pt") ?: return
            meanNet = Module.load(filePath)
        } catch (e: Exception) {
            Log.d(TAG, "can not load pt$filePath")
            return
        }
        // 获取输入图像，并进行resize操作，使它符合设定的输入图像尺寸
        try {
            val bis = BufferedInputStream(FileInputStream(imagePath))
            bmp = UtilKBitmap.get(bis)
            scaledBmp = Bitmap.createScaledBitmap(bmp, inDims[0], inDims[1], true)
            bis.close()
        } catch (e: Exception) {
            Log.d(TAG, "can not read bmp")
        }
        // 构建输入张量，预处理的均值和方差与python中代码保持一致
        val meanRGB = floatArrayOf(0.0f, 0.0f, 0.0f)
        val stdRGB = floatArrayOf(1.0f, 1.0f, 1.0f)
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            scaledBmp,
            meanRGB, stdRGB
        )

        try {
            // 前向推理
            val outputTensor = meanNet!!.forward(IValue.from(inputTensor)).toTensor()
            val outArray = outputTensor.dataAsFloatArray
            // 输出的第一个值，就是输入图像的平均值
            val mean = outArray[0] * 255

            // 将图像的平均值通过TextView显示到手机界面
            var result = ""
            result += "mean is $mean"
            vb.textViewStatistics.text = result
        } catch (e: Exception) {
            Log.e("Log", "fail to predict")
            e.printStackTrace()
        }
    }

}