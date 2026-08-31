package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.PriorityHighColor
import com.example.ui.theme.PriorityLowColor
import com.example.ui.theme.PriorityMediumColor

enum class Priority(val titleRu: String, val level: Int) {
    LOW("Низкий", 1),
    MEDIUM("Средний", 2),
    HIGH("Высокий", 3);

    fun getColor(): Color = when (this) {
        HIGH -> PriorityHighColor
        MEDIUM -> PriorityMediumColor
        LOW -> PriorityLowColor
    }
}
