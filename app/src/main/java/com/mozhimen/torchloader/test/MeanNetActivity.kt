package com.mozhimen.torchloader.test

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mozhimen.bindk.bases.viewbinding.activity.BaseActivityVB
import com.mozhimen.torchloader.test.databinding.ActivityMeanNetBinding
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
    companion object{
        // 开启相机和调用相册的请求码
        const val START_CAMERA_CODE: Int = 1111
        const val START_ALBUM_CODE: Int = 1112
        // 请求权限的请求码
        const val REQUIRE_PERMISSION_CODE: Int = 111
    }

    // 创建pytorch模型
    private var meanNet: Module? = null
    // 定义ImageView对象show_image展示的图片的路径
    private var showImagePath: String? = null
    // 定义相机拍摄图像的路径
    private var cameraImagePath: String? = null
    // 定义从相册选择图像的路径
    private var albumImagePath: String? = null


    // 创建用于开启相机的按钮
    private var btnStartCamera: Button? = null
    // 创建用于调用系统相册的按钮
    private var btnStartAlbum: Button? = null
    // 创建用于保存ImageView内容的按钮
    private var btnSaveImage: Button? = null
    // 创建用于启动pytorch前向推理的按钮
    private var btnStartPredict: Button? = null

    // 创建用于显示原始图像的ImageView对象
    private var showOriginalImage: ImageView? = null
    // 创建用于显示统计结果的TextView对象
    private var textViewStatistics: TextView? = null

    override fun initView(savedInstanceState: Bundle?) {

        // 绑定开启相机的按钮
        btnStartCamera = findViewById<View>(R.id.button_start_camera) as Button
        btnStartCamera!!.setOnClickListener(StartCameraOnClickListener())

        // 绑定调用相册的按钮
        btnStartAlbum = findViewById<View>(R.id.button_start_album) as Button
        btnStartAlbum!!.setOnClickListener(StartAlbumOnClickListener())

        // 绑定保存图片的按钮
        btnSaveImage = findViewById<View>(R.id.button_save_image) as Button
        btnSaveImage!!.setOnClickListener(SaveImageOnClickListener())

        btnStartPredict = findViewById<View>(R.id.button_start_predict) as Button
        btnStartPredict!!.setOnClickListener(StartPredictOnClickListener())

        //  绑定我们在activity_main.xml中定义的ImageView
        showOriginalImage = findViewById<View>(R.id.image_view_show_original) as ImageView

        //  绑定我们在activity_main.xml中定义的TextView
        textViewStatistics = findViewById<View>(R.id.text_view_statistics) as TextView

    }

    inner class StartCameraOnClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            // 开启相机，返回相机拍摄图像的路径，传入的请求码是START_CAMERA_CODE
            cameraImagePath = Utils.startCamera(this, START_CAMERA_CODE)
        }
    }

    inner class StartAlbumOnClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            // 调用相册，传入的请求码是START_ALBUM_CODE
            Utils.startAlbum(this, START_ALBUM_CODE)
        }
    }

    inner class SaveImageOnClickListener : View.OnClickListener {
        override fun onClick(v: View) {
        }
    }

    inner class StartPredictOnClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            showImagePath?.let { predictMean(it) }
        }
    }

    private fun saveImage(bmp: Bitmap?, quality: Int): String? {
        if (bmp == null) {
            return null
        }

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

    // 请求本案例需要的三种权限
    private fun requestPermissions() {
        // 定义容器，存储我们需要申请的权限
        val permissionList: MutableList<String> = ArrayList()
        // 检测应用是否具有CAMERA的权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.CAMERA)
        }
        // 检测应用是否具有READ_EXTERNAL_STORAGE权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        // 检测应用是否具有WRITE_EXTERNAL_STORAGE权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        // 如果permissionList不为空，则说明前面检测的三种权限中至少有一个是应用不具备的
        // 则需要向用户申请使用permissionList中的权限
        if (!permissionList.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionList.toTypedArray<String>(), com.example.aa175.as_demo.MainActivity.REQUIRE_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // 判断请求码
        when (requestCode) {
            com.example.aa175.as_demo.MainActivity.REQUIRE_PERMISSION_CODE -> if (grantResults.size > 0) {
                var i = 0
                while (i < grantResults.size) {
                    // 如果请求被拒绝，则弹出下面的Toast
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        Toast.makeText(this, permissions[i] + " was denied", Toast.LENGTH_SHORT).show()
                    }
                    i++
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                com.example.aa175.as_demo.MainActivity.START_ALBUM_CODE -> {
                    if (data == null) {
                        Log.w("LOG", "user photo data is null")
                        return
                    }

                    val albumImageUri = data.data
                    albumImagePath = Utils.getPathFromUri(this@MainActivity, albumImageUri)
                    showImagePath = albumImagePath
                    val bitmapAlbum: Bitmap = Utils.getScaleBitmapByPath(albumImagePath)
                    showOriginalImage!!.setImageBitmap(Utils.getScaleBitmapByBitmap(bitmapAlbum, 448, 448))
                    Toast.makeText(this@MainActivity, "album start", Toast.LENGTH_LONG).show()
                }

                com.example.aa175.as_demo.MainActivity.START_CAMERA_CODE -> {
                    showImagePath = cameraImagePath
                    val bitmapCamera: Bitmap = Utils.getScaleBitmapByPath(cameraImagePath)
                    Toast.makeText(this@MainActivity, "camera start", Toast.LENGTH_LONG).show()
                    showOriginalImage!!.setImageBitmap(Utils.getScaleBitmapByBitmap(bitmapCamera, 448, 448))
                }
            }
        }
    }

    private fun predictMean(imagePath: String) {
        // 设置输入尺寸，和python代码中的设定保持一致
        val inDims = intArrayOf(256, 256, 3)
        var bmp: Bitmap? = null
        var scaledBmp: Bitmap? = null
        var filePath = ""

        // 加载pytorch模型，加载失败则抛出异常
        try {
            filePath = com.example.aa175.as_demo.MainActivity.assetFilePath(this, "mean_net.pt")
            meanNet = Module.load(filePath)
        } catch (e: Exception) {
            Log.d("LOG", "can not load pt$filePath")
        }
        // 获取输入图像，并进行resize操作，使它符合设定的输入图像尺寸
        try {
            val bis = BufferedInputStream(FileInputStream(imagePath))
            bmp = BitmapFactory.decodeStream(bis)
            scaledBmp = Bitmap.createScaledBitmap(bmp, inDims[0], inDims[1], true)
            bis.close()
        } catch (e: Exception) {
            Log.d("LOG", "can not read bmp")
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
            textViewStatistics!!.text = result
        } catch (e: Exception) {
            Log.e("Log", "fail to predict")
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    fun assetFilePath(context: Context, assetName: String?): String {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }

        context.assets.open(assetName!!).use { `is` ->
            FileOutputStream(file).use { os ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while ((`is`.read(buffer).also { read = it }) != -1) {
                    os.write(buffer, 0, read)
                }
                os.flush()
            }
            return file.absolutePath
        }
    }
}