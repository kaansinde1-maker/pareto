package com.pareto.timetracker.domain

import androidx.annotation.StringRes
import com.pareto.timetracker.R

enum class Tag(@StringRes val labelRes: Int) {
    INVESTMENT(R.string.tag_investment),
    CONSUMPTION(R.string.tag_consumption),
}
