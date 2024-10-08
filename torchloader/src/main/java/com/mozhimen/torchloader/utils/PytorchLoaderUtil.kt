package com.mozhimen.torchloader.utils

import android.content.Context
import com.mozhimen.basick.utilk.kotlin.UtilKStrAsset
import java.io.File

/**
 * @ClassName PytorchLoaderUtil
 * @Description TODO
 * @Author Mozhimen / Kolin Zhao
 * @Date 2024/3/24 0:49
 * @Version 1.0
 */
object PytorchLoaderUtil {
    /**
     * Copies specified asset to the file in /files app directory and returns this file absolute path.
     *
     * @return absolute file path
     */
    fun strAssetModelName2file(context: Context, assetName: String): String? {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0)
            return file.absolutePath
        return UtilKStrAsset.strAssetName2file(assetName, file.absolutePath, bufferSize = 4 * 1024)?.absolutePath
    }
}