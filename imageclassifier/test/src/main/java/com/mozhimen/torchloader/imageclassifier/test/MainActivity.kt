package com.mozhimen.torchloader.imageclassifier.test

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.mozhimen.basick.elemk.android.media.cons.CMediaFormat
import com.mozhimen.basick.elemk.androidx.appcompat.bases.viewbinding.BaseActivityVB
import com.mozhimen.basick.utilk.android.graphics.applyBitmapAnyScale
import com.mozhimen.basick.utilk.android.net.uri2bitmap
import com.mozhimen.torchloader.imageclassifier.PytorchImageClassifier
import com.mozhimen.torchloader.imageclassifier.test.databinding.ActivityMainBinding

/**
 * @ClassName MainActivity
 * @Description TODO
 * @Author Mozhimen / Kolin Zhao
 * @Date 2024/3/24 0:41
 * @Version 1.0
 */

class MainActivity : BaseActivityVB<ActivityMainBinding>() {

    private lateinit var _pytorchImageClassifier: PytorchImageClassifier

    private val getContentImage = registerForActivityResult(ActivityResultContracts.GetContent()) {
        if (it != null) {
            processBitmap(it.uri2bitmap())
        }
    }

    private val takePicturePreview = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) {
        if (it != null) {
            processBitmap(it)
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        initPytorch()

        vb.buttonAlbum.setOnClickListener {
            getContentImage.launch(CMediaFormat.MIMETYPE_IMAGE_ALL)
        }
        vb.buttonCamera.setOnClickListener {
            takePicturePreview.launch(null)
        }
    }

    private fun initPytorch() {
        _pytorchImageClassifier = PytorchImageClassifier("optimized_model.pt")
    }

    private var _newBitmap: Bitmap? = null

    @SuppressLint("SetTextI18n")
    private fun processBitmap(bitmap: Bitmap) {
        _newBitmap=bitmap.applyBitmapAnyScale(256f, 256f)
        // showing image on UI
        vb.mainImg.setImageBitmap(_newBitmap)

        val scores = _pytorchImageClassifier.classify(_newBitmap!!)

        // searching for the index with maximum score
        var maxScore = -Float.MAX_VALUE
        var maxScoreIdx = -1
        for (i in scores.indices) {
            if (scores[i] > maxScore) {
                maxScore = scores[i]
                maxScoreIdx = i
            }
        }
//        val className = ImageNetClasses.IMAGENET_CLASSES[maxScoreIdx]

        // showing className on UI
        vb.textView.text = "$maxScoreIdx $maxScore"
    }
}

