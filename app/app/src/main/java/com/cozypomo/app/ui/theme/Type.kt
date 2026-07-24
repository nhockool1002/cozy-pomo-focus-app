package com.cozypomo.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cozypomo.app.R

/**
 * Baloo 2 (biến trục wght 400-800, hỗ trợ tiếng Việt đầy đủ — res/font/baloo2.ttf) — font bo
 * tròn, "mập mạp" hơn Nunito trước đó, hợp không khí game cute hơn cho toàn bộ ứng dụng.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun baloo2(weight: Int) = Font(
    resId = R.font.baloo2,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Baloo2Family = FontFamily(
    baloo2(400),
    baloo2(500),
    baloo2(600),
    baloo2(700),
    baloo2(800),
)

private fun style(weight: FontWeight, size: androidx.compose.ui.unit.TextUnit, lineHeight: androidx.compose.ui.unit.TextUnit? = null) =
    TextStyle(fontFamily = Baloo2Family, fontWeight = weight, fontSize = size, lineHeight = lineHeight ?: TextStyle.Default.lineHeight)

val CozyPomoTypography = Typography(
    displayLarge = style(FontWeight.ExtraBold, 48.sp, 56.sp),
    displayMedium = style(FontWeight.ExtraBold, 38.sp, 44.sp),
    displaySmall = style(FontWeight.Bold, 32.sp, 38.sp),
    headlineLarge = style(FontWeight.Bold, 30.sp, 36.sp),
    headlineMedium = style(FontWeight.Bold, 26.sp, 32.sp),
    headlineSmall = style(FontWeight.Bold, 22.sp, 28.sp),
    titleLarge = style(FontWeight.Bold, 20.sp, 26.sp),
    titleMedium = style(FontWeight.SemiBold, 17.sp, 22.sp),
    titleSmall = style(FontWeight.SemiBold, 14.sp, 20.sp),
    bodyLarge = style(FontWeight.Normal, 16.sp, 24.sp),
    bodyMedium = style(FontWeight.Normal, 14.sp, 20.sp),
    bodySmall = style(FontWeight.Normal, 12.sp, 16.sp),
    labelLarge = style(FontWeight.SemiBold, 14.sp, 20.sp),
    labelMedium = style(FontWeight.SemiBold, 12.sp, 16.sp),
    labelSmall = style(FontWeight.SemiBold, 11.sp, 14.sp),
)
