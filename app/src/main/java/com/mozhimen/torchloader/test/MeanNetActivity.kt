package com.mozhimen.torchloader.test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mozhimen.bindk.bases.viewbinding.activity.BaseActivityVB
import com.mozhimen.torchloader.test.databinding.ActivityMeanNetBinding

class MeanNetActivity : BaseActivityVB<ActivityMeanNetBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mean_net)
    }
}