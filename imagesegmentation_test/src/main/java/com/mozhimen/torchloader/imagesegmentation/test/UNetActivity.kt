package com.mozhimen.torchloader.imagesegmentation.test

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import com.mozhimen.bindk.bases.viewbinding.activity.BaseActivityVB
import com.mozhimen.kotlin.elemk.commons.I_Listener
import com.mozhimen.kotlin.lintk.optins.permission.OPermission_MANAGE_EXTERNAL_STORAGE
import com.mozhimen.kotlin.lintk.optins.permission.OPermission_READ_EXTERNAL_STORAGE
import com.mozhimen.kotlin.lintk.optins.permission.OPermission_WRITE_EXTERNAL_STORAGE
import com.mozhimen.kotlin.utilk.android.graphics.applyBitmapAnyRotate
import com.mozhimen.kotlin.utilk.android.net.uri2strFilePathName
import com.mozhimen.kotlin.utilk.android.util.UtilKLogWrapper
import com.mozhimen.kotlin.utilk.android.widget.showToast
import com.mozhimen.kotlin.utilk.kotlin.UtilKStrPath
import com.mozhimen.kotlin.utilk.kotlin.createFile
import com.mozhimen.kotlin.utilk.kotlin.strFilePath2bitmapAny_use_ofInputStream
import com.mozhimen.kotlin.utilk.kotlin.strFilePath2uri
import com.mozhimen.manifestk.xxpermissions.XXPermissionsCheckUtil
import com.mozhimen.manifestk.xxpermissions.XXPermissionsRequestUtil
import com.mozhimen.torchloader.basic.PytorchLoaderUtil
import com.mozhimen.torchloader.imagesegmentation.test.databinding.ActivityUnetBinding
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils

class UNetActivity : BaseActivityVB<ActivityUnetBinding>() {
    companion object {
        const val START_CAMERA_CODE: Int = 1111// 开启相机和调用相册的请求码
        const val START_ALBUM_CODE: Int = 1112
    }


    private var segModule: Module? = null// 创建pytorch模型

    /////////////////////////////////////////////////////////////////////////

    private var showImagePath: String? = null// 定义ImageView对象show_image展示的图片的路径
    private var cameraImagePath: String? = null// 定义相机拍摄图像的路径
    private var albumImagePath: String? = null// 定义从相册选择图像的路径

    override fun initData(savedInstanceState: Bundle?) {
        requestPermissions {
            super.initData(savedInstanceState)
        }
    }

    @OptIn(OPermission_READ_EXTERNAL_STORAGE::class, OPermission_WRITE_EXTERNAL_STORAGE::class, OPermission_MANAGE_EXTERNAL_STORAGE::class)
    @SuppressLint("MissingPermission")
    private fun requestPermissions(onGranted: I_Listener) {
        if (XXPermissionsCheckUtil.hasReadWritePermission(this)) {
            onGranted.invoke()
        } else {
            XXPermissionsRequestUtil.requestReadWritePermission(this, onGranted)
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        // 绑定开启相机的按钮
        vb.buttonStartCamera.setOnClickListener {
            // 开启相机，返回相机拍摄图像的路径，传入的请求码是START_CAMERA_CODE
            cameraImagePath = UtilKStrPath.Absolute.Internal.getCache() + "/XiaoMei/" + System.currentTimeMillis().toString() + ".jpg"
            cameraImagePath!!.createFile()
            val uri = cameraImagePath!!.strFilePath2uri()
            if (uri != null) {
                UNetUtils.startCamera(this@UNetActivity, uri, START_CAMERA_CODE)
            }
        }
        // 绑定调用相册的按钮
        vb.buttonStartAlbum.setOnClickListener {
            // 调用相册，传入的请求码是START_ALBUM_CODE
            UNetUtils.startAlbum(this@UNetActivity, START_ALBUM_CODE)
        }
        // 绑定保存图片的按钮
        vb.buttonSaveImage.setOnClickListener {
//            // 获取show_image展示的图片
//            val bitmap = (vb.imageViewShowBody.getDrawable() as? BitmapDrawable)?.bitmap ?: return@setOnClickListener
//            // 通过saveImage函数生成图像的保存路径
//            val imageSavePath: String? = saveImage(bitmap, 100)
//            // 提示用户图片已经被保存
//            if (imageSavePath != null) {
//                "save to: $imageSavePath".showToast()
//            }
        }

        vb.buttonStartPredict.setOnClickListener {
            showImagePath?.let { segmentationForMask(it) }
        }
    }

    @Deprecated("Deprecated in Java")
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
                        albumImagePath = albumImageUri.uri2strFilePathName() ?: return
                        showImagePath = albumImagePath
                        UtilKLogWrapper.d(TAG, "onActivityResult: albumImagePath $albumImagePath")
                        val bitmapAlbum: Bitmap = UNetUtils.getScaleBitmapByPath(albumImagePath!!)?.applyBitmapAnyRotate(-90f)?:return
                        vb.imageViewShowOriginal.setImageBitmap(UNetUtils.getScaleBitmapByBitmap(bitmapAlbum, 448, 448))
                        "album start".showToast()
                    }
                }

                START_CAMERA_CODE -> {
                    showImagePath = cameraImagePath ?: return
                    val bitmap_camera: Bitmap = UNetUtils.getScaleBitmapByPath(cameraImagePath!!)?:return
                    "camera start".showToast()
                    vb.imageViewShowOriginal.setImageBitmap(UNetUtils.getScaleBitmapByBitmap(bitmap_camera, 448, 448)?.applyBitmapAnyRotate(-90f))
                }
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////

//    private fun saveImage(bmp: Bitmap?, quality: Int): String? {
//        if (bmp == null) {
//            return null
//        }
//
//        val appDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) ?: return null
//
//        val fileName = System.currentTimeMillis().toString() + ".jpg"
//        val file = File(appDir, fileName)
//        var fos: FileOutputStream? = null
//
//        try {
//            fos = FileOutputStream(file)
//            bmp.compress(Bitmap.CompressFormat.JPEG, quality, fos)
//            fos.flush()
//            return file.absolutePath
//        } catch (e: FileNotFoundException) {
//            e.printStackTrace()
//        } catch (e: IOException) {
//            e.printStackTrace()
//        } finally {
//            if (fos != null) {
//                try {
//                    fos.close()
//                } catch (e: IOException) {
//                    e.printStackTrace()
//                }
//            }
//        }
//        return null
//    }

    private fun segmentationForMask(imagePath: String) {
        UtilKLogWrapper.d(TAG, "segmentationForMask: ")
        // 创建3通道的输出，虽然输出的mask是单通道的，这里将mask看作是3通道的
        val inDims = intArrayOf(256, 256, 3)
        val outDims = intArrayOf(256, 256, 3)

        val mask_channels = 1
        var bmp: Bitmap? = null
        var scaledBmp: Bitmap? = null
        var filePath = ""
        try {
            filePath = PytorchLoaderUtil.strAssetModelName2file(this, "20_unet.pt") ?: return
            segModule = Module.load(filePath)
        } catch (e: Exception) {
            Log.d(TAG, "can not load pt $filePath")
        }
        try {
            bmp = imagePath.strFilePath2bitmapAny_use_ofInputStream()?.applyBitmapAnyRotate(-90f) ?: return//UtilKBitmap.get(bis)
            scaledBmp = Bitmap.createScaledBitmap(bmp, inDims[0], inDims[1], true)
        } catch (e: Exception) {
            Log.d(TAG, "can not read human image bitmap")
        }

        val meanRGB = floatArrayOf(0.0f, 0.0f, 0.0f)
        val stdRGB = floatArrayOf(1.0f, 1.0f, 1.0f)
        val humanImageTensor = TensorImageUtils.bitmapToFloat32Tensor(
            scaledBmp,
            meanRGB, stdRGB
        )

        try {
            val maskTensor = segModule!!.forward(IValue.from(humanImageTensor)).toTensor()
            val maskArray = maskTensor.dataAsFloatArray

            var index = 0
            // mask_image用于存放分割掩码，它是3通道的
            val maskImage = Array(outDims[0]) { Array(outDims[1]) { FloatArray(3) } }
            for (j in 0 until mask_channels) {
                for (k in 0 until outDims[0]) {
                    for (m in 0 until outDims[1]) {
                        // 对3个通道进行赋值，遍历的时候j只遍历1个通道值，因为输出的outArr是1通道的
                        if (maskArray[index] > 0.5) {
                            maskImage[k][m][j] = 255.0f
                            maskImage[k][m][j + 1] = 255.0f
                            maskImage[k][m][j + 2] = 255.0f
                        } else {
                            maskImage[k][m][j] = 0.0f
                            maskImage[k][m][j + 1] = 0.0f
                            maskImage[k][m][j + 2] = 0.0f
                        }
                        index++
                    }
                }
            }

            // 将分割掩码展示到ImageView上
            val maskBitmap: Bitmap = Utils.getBitmap(maskImage, outDims)
            vb.imageViewShowPredictMask.setImageBitmap(maskBitmap)

            // foregroundImage用于展示经分割掩码处理后的前景图像
            val foregroundImage = Array(inDims[0]) { Array(inDims[1]) { FloatArray(inDims[2]) } }
            index = 0

            for (k in 0 until outDims[0]) {
                for (m in 0 until outDims[1]) {
                    // 获取原图中的像素值
                    val pixel = scaledBmp!!.getPixel(m, k)
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    // 对3个通道进行赋值，遍历的时候j只遍历1个通道值
                    if (maskArray[index] > 0.5) {
                        foregroundImage[k][m][0] = r.toFloat()
                        foregroundImage[k][m][1] = g.toFloat()
                        foregroundImage[k][m][2] = b.toFloat()
                    } else {
                        foregroundImage[k][m][0] = 0f
                        foregroundImage[k][m][1] = 0f
                        foregroundImage[k][m][2] = 0f
                    }

                    index++
                }
            }


            val foregroundBitmap: Bitmap = Utils.getBitmap(foregroundImage, outDims)
            vb.imageViewShowBody.setImageBitmap(foregroundBitmap)
        } catch (e: Exception) {
            Log.e("Log", "fail to preform segmentation")
            e.printStackTrace()
        }
    }
}