package com.drumpads.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.drumpads.app.ui.screens.MainAppContainer
import com.drumpads.app.ui.pads.DrumPadsViewModel
import com.drumpads.app.ui.theme.DrumPadsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: DrumPadsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DrumPadsTheme {
                MainAppContainer(viewModel = viewModel)
            }
        }
    }
}
