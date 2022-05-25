package com.jeanwest.mobile.manualRefill

data class ManualRefillProduct(
    var name: String,
    var KBarCode: String,
    var imageUrl: String,
    var primaryKey: Long,
    var productCode: String,
    var size: String,
    var color: String,
    var originalPrice: String,
    var salePrice: String,
    var rfidKey: Long,
    var wareHouseNumber: Int,
    var kName: String,
    var requestedNum : Int,
    var storeNumber : Int
)