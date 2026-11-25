package com.walletq.feature.activity

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

fun categoryIconFor(identifier: String): ImageVector {
    return when (identifier) {
        "restaurant" -> Icons.Outlined.Restaurant
        "shopping_cart", "local_grocery_store" -> Icons.Outlined.ShoppingCart
        "directions_car" -> Icons.Outlined.DirectionsCar
        "home" -> Icons.Outlined.Home
        "bolt" -> Icons.Outlined.Bolt
        "medical_services" -> Icons.Outlined.MedicalServices
        "movie" -> Icons.Outlined.Movie
        "shopping_bag" -> Icons.Outlined.ShoppingBag
        "spa" -> Icons.Outlined.Spa
        "school" -> Icons.Outlined.School
        "account_balance" -> Icons.Outlined.AccountBalance
        "more_horiz" -> Icons.Outlined.MoreHoriz
        "work" -> Icons.Outlined.Work
        "business_center" -> Icons.Outlined.BusinessCenter
        "trending_up" -> Icons.Outlined.TrendingUp
        "card_giftcard" -> Icons.Outlined.CardGiftcard
        "savings" -> Icons.Outlined.Savings
        else -> Icons.Outlined.Category
    }
}

fun parseCategoryColor(hex: String, fallback: Color): Color {
    return try {
        val cleaned = hex.removePrefix("#")
        val value = cleaned.toLong(16)
        when (cleaned.length) {
            6 -> Color(0xFF000000L or value)
            8 -> Color(value)
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
