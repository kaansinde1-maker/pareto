package com.pareto.timetracker.timer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.pareto.timetracker.ui.theme.ParetoTheme
import com.pareto.timetracker.ui.timer.DiscardTimerDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Bildirimdeki "İptal" butonu bunu açar: şeffaf ekran üzerinde sadece onay diyaloğu görünür. */
@AndroidEntryPoint
class DiscardTimerActivity : ComponentActivity() {

    @Inject lateinit var controller: TimerController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ParetoTheme {
                DiscardTimerDialog(
                    onConfirm = {
                        controller.cancel()
                        finish()
                    },
                    onDismiss = { finish() },
                )
            }
        }
    }
}
