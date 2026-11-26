package com.pareto.timetracker.data.pool

import com.pareto.timetracker.data.db.CategoryEntity
import com.pareto.timetracker.data.db.GoalEntity
import org.json.JSONObject

fun jsonOf(vararg pairs: Pair<String, Any?>): JSONObject = JSONObject().apply {
    pairs.forEach { (key, value) -> put(key, value ?: JSONObject.NULL) }
}

fun CategoryEntity.toEventJson(): JSONObject = jsonOf(
    "name" to name,
    "tag" to tag.name,
    "color_index" to colorIndex,
)

fun GoalEntity.toEventJson(): JSONObject = jsonOf(
    "scope_key" to scopeKey,
    "category_id" to categoryId,
    "tag" to tag?.name,
    "period" to period.name,
    "goal_type" to goalType.name,
    "target_seconds" to targetSeconds,
)
