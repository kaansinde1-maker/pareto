package com.pareto.timetracker.timer

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sadece titreşim, ses yok. Güç düşük-orta aralığındadır (genlik 90–110 / 255);
 * genlik kontrolü olmayan cihazlarda süreler aynı kalır, güç cihaz varsayılanı olur.
 *
 * Titreşim "alarm" kullanım türüyle çalınır: sessiz modda da çalar ve
 * sistemin titreşim ayarları (kapalıysa) geçerli olur.
 */
@Singleton
class TimerHaptics @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    /** 5 titreşim: 4 kısa (80 ms) + 1 uzun (220 ms), aralar 70 ms, genlik hafifçe yükselir. */
    fun playStart() = play(START_TIMINGS, START_AMPLITUDES)

    /** Tek, 1 saniyelik titreşim. */
    fun playStop() = play(STOP_TIMINGS, STOP_AMPLITUDES)

    private fun play(timings: LongArray, amplitudes: IntArray) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val effect = if (v.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(timings, amplitudes, NO_REPEAT)
        } else {
            VibrationEffect.createWaveform(timings, NO_REPEAT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(
                effect,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
    }

    private companion object {
        const val NO_REPEAT = -1

        // [bekleme, titre, bekle, titre, ...]
        val START_TIMINGS = longArrayOf(0, 80, 70, 80, 70, 80, 70, 80, 70, 220)
        val START_AMPLITUDES = intArrayOf(0, 90, 0, 95, 0, 100, 0, 105, 0, 110)

        val STOP_TIMINGS = longArrayOf(0, 1000)
        val STOP_AMPLITUDES = intArrayOf(0, 100)
    }
}
