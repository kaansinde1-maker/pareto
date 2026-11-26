package com.pareto.timetracker.domain

enum class GoalPeriod { DAILY, WEEKLY, MONTHLY }

enum class GoalType { MIN, MAX }

fun GoalPeriod.toTimeFilter(): TimeFilter = when (this) {
    GoalPeriod.DAILY -> TimeFilter.DAILY
    GoalPeriod.WEEKLY -> TimeFilter.WEEKLY
    GoalPeriod.MONTHLY -> TimeFilter.MONTHLY
}
