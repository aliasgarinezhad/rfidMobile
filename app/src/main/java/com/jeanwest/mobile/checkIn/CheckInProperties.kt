package com.jeanwest.mobile.checkIn

data class CheckInProperties(
    val number : Long,
    val date : String,
    val numberOfItems : Int,
    val source : Int,
    val destination : Int
)
