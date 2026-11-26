package com.pareto.timetracker.domain

object TimerRules {
    /** Bundan kısa oturumlar yanlış dokunuş sayılır ve kaydedilmez. */
    const val MIN_SESSION_MS = 10_000L

    /** Odak puanı çubuğu sadece bundan uzun, uygulama içinden durdurulan oturumlarda çıkar. */
    const val FOCUS_PROMPT_MIN_MS = 5 * 60_000L

    /** Odak puanı çubuğunun ekranda kalma süresi; dokunulmazsa puan boş kalır. */
    const val FOCUS_PROMPT_VISIBLE_MS = 8_000L
}
