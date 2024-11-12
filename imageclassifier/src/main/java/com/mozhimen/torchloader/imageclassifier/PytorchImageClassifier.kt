package com.mozhimen.torchloader.imageclassifier

import android.graphics.Bitmap
import com.mozhimen.basick.utilk.android.util.UtilKLogWrapper
import com.mozhimen.basick.utilk.bases.BaseUtilK
import com.mozhimen.proguardk.torchloader.basic.PytorchLoaderUtil
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.MemoryFormat
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils

/**
 * @ClassName PytorchImageClassifier
 * @Description TODO
 * @Author Mozhimen / Kolin Zhao
 * @Date 2024/3/24 0:52
 * @Version 1.0
 */
class PytorchImageClassifier(modelPath: String) : BaseUtilK() {
    companion object {
        @JvmStatic
        fun create(modelPath: String): PytorchImageClassifier =
            PytorchImageClassifier(modelPath)
    }

    ////////////////////////////////////////////////////////////////////

    private var _module: Module? = null

    ////////////////////////////////////////////////////////////////////

    init {
        _module = LiteModuleLoader.load(com.mozhimen.proguardk.torchloader.basic.PytorchLoaderUtil.strAssetModelName2file(_context, modelPath /*"model.pt"*/))
    }

    ////////////////////////////////////////////////////////////////////

    fun classify(bitmap: Bitmap): FloatArray {
        // preparing input tensor
        val inputTensor =
            TensorImageUtils.bitmapToFloat32Tensor(bitmap, TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB, MemoryFormat.CHANNELS_LAST)

        // running the model
        val outputTensor = _module?.forward(IValue.from(inputTensor))?.toTensor() ?: return floatArrayOf()

        // getting tensor content as java array of floats
        val scores = outputTensor.dataAsFloatArray

        UtilKLogWrapper.d(TAG, "classify: scores ${scores.map { it }}")

        return scores
    }
}