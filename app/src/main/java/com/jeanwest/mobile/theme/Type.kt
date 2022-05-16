package com.jeanwest.mobile.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jeanwest.mobile.R

// Set of Material typography styles to start with
val Typography = Typography(
    body1 = TextStyle(
        fontFamily = FontFamily(Font(R.font.sans_regular)),
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    body2 = TextStyle(
        fontFamily = FontFamily(Font(R.font.sans_regular)),
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp
    ),
    h2 = TextStyle(
        fontFamily = FontFamily(Font(R.font.sans_regular)),
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp
    ),
    /* Other default text styles to override*/
    button = TextStyle(
        fontFamily = FontFamily(Font(R.font.sans_regular)),
        fontWeight = FontWeight.W500,
        fontSize = 14.sp
    ),
    caption = TextStyle(
        fontFamily = FontFamily(Font(R.font.sans_regular)),
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    defaultFontFamily = FontFamily(Font(R.font.sans_regular)),
    h1 = TextStyle(
        fontFamily = FontFamily(Font(R.font.sans_bold)),
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    h3 = TextStyle(
        fontFamily = FontFamily(Font(R.font.sans_regular)),
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    )
)