package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.RoseRed

data class CategoryMeta(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val color: Color
)

object CategoryData {
    val list = listOf(
        CategoryMeta("work", "عمل", Icons.Default.Work, Color(0xFF3B82F6)),
        CategoryMeta("health", "صحة ودواء", Icons.Default.MedicalServices, RoseRed),
        CategoryMeta("study", "دراسة", Icons.Default.Book, AmberAccent),
        CategoryMeta("personal", "شخصي", Icons.Default.Person, PurplePrimary),
        CategoryMeta("spiritual", "عبادة", Icons.Default.NightlightRound, EmeraldGreen),
        CategoryMeta("special", "مناسبة", Icons.Default.Event, CyanAccent),
        CategoryMeta("general", "عام", Icons.Default.FitnessCenter, Color(0xFF8B5CF6))
    )

    fun get(key: String): CategoryMeta {
        return list.find { it.key == key } ?: list.last()
    }
}

@Composable
fun CategoryBadge(
    categoryKey: String,
    modifier: Modifier = Modifier
) {
    val meta = CategoryData.get(categoryKey)
    Box(
        modifier = modifier
            .background(meta.color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = meta.icon,
                contentDescription = null,
                tint = meta.color,
                modifier = Modifier
                    .size(14.dp)
                    .padding(end = 4.dp)
            )
            Text(
                text = meta.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = meta.color
            )
        }
    }
}
