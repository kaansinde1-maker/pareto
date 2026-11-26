package com.pareto.timetracker.domain

/**
 * Her Tag'in 4 tonluk kendi paleti vardır: Yatırım yeşil, Tüketim kırmızı.
 * Kategoride sadece ton sırası (colorIndex) saklanır; gerçek renk Tag + sıra ile çözülür.
 * Böylece Tag değişince renk aynı sıradaki tona otomatik eşlenir.
 */
object CategoryPalette {
    const val TONE_COUNT = 4

    private val investment = intArrayOf(
        0xFF81C784.toInt(), // açık yeşil
        0xFF43A047.toInt(), // yeşil
        0xFF2E7D32.toInt(), // koyu yeşil
        0xFF00695C.toInt(), // zümrüt
    )

    private val consumption = intArrayOf(
        0xFFEF9A9A.toInt(), // açık kırmızı
        0xFFE53935.toInt(), // kırmızı
        0xFFB71C1C.toInt(), // koyu kırmızı
        0xFF800020.toInt(), // bordo
    )

    fun colors(tag: Tag): List<Int> = when (tag) {
        Tag.INVESTMENT -> investment.toList()
        Tag.CONSUMPTION -> consumption.toList()
    }

    fun color(tag: Tag, index: Int): Int =
        colors(tag)[index.coerceIn(0, TONE_COUNT - 1)]
}
