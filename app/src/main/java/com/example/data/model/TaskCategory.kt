package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

enum class TaskCategory(val titleRu: String, val iconName: String) {
    WORK("Работа", "Work"),
    PERSONAL("Личное", "Person"),
    STUDY("Учёба", "School"),
    SHOPPING("Покупки", "ShoppingCart"),
    HEALTH("Здоровье", "Favorite"),
    IDEAS("Идеи", "Lightbulb"),
    OTHER("Разное", "Assignment");

    fun getIcon(): ImageVector = when (this) {
        WORK -> Icons.Default.Work
        PERSONAL -> Icons.Default.Person
        STUDY -> Icons.Default.School
        SHOPPING -> Icons.Default.ShoppingCart
        HEALTH -> Icons.Default.Favorite
        IDEAS -> Icons.Default.Lightbulb
        OTHER -> Icons.Default.Assignment
    }
}
