package com.pareto.timetracker.ui.common

import androidx.compose.ui.graphics.Color
import com.pareto.timetracker.domain.CategoryPalette
import com.pareto.timetracker.domain.Tag

/** Yatırım/Tüketim göstergeleri her yerde sabit yeşil/kırmızı. */
fun tagColor(tag: Tag): Color = Color(CategoryPalette.color(tag, 1))
