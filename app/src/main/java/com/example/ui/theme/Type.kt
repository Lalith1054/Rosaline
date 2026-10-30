package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val NewsreaderMaxBoldFontFamily = FontFamily(
    Font(R.font.newsreader_800, FontWeight.Normal),
    Font(R.font.newsreader_800, FontWeight.Bold),
    Font(R.font.newsreader_800, FontWeight.ExtraBold),
    Font(R.font.newsreader_800, FontWeight.Black)
)

val NewsreaderFontFamily = FontFamily(
    Font(R.font.newsreader, FontWeight.Normal),
    Font(R.font.newsreader, FontWeight.Medium),
    Font(R.font.newsreader_700, FontWeight.Bold),
    Font(R.font.newsreader_800, FontWeight.ExtraBold)
)

val LibreFranklinFontFamily = FontFamily(
    Font(R.font.libre_franklin_500, FontWeight.Normal),
    Font(R.font.libre_franklin_600, FontWeight.Medium),
    Font(R.font.libre_franklin_600, FontWeight.SemiBold),
    Font(R.font.libre_franklin_700, FontWeight.Bold),
    Font(R.font.libre_franklin_800, FontWeight.ExtraBold),
    Font(R.font.libre_franklin_900, FontWeight.Black)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = NewsreaderMaxBoldFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.8).sp
    ),
    displayMedium = TextStyle(
        fontFamily = LibreFranklinFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = NewsreaderMaxBoldFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 54.sp,
        lineHeight = 58.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = LibreFranklinFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = LibreFranklinFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = LibreFranklinFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 23.sp
    ),
    titleSmall = TextStyle(
        fontFamily = LibreFranklinFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = LibreFranklinFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = LibreFranklinFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = LibreFranklinFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = LibreFranklinFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    )
)
