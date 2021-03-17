package com.alphelios.iap

class DataWrappers {

    data class SkuInfo(
        val sku: String,
    )

    data class PurchaseInfo(
        val sku: String,
        val purchaseState: Int,
        val isAcknowledged: Boolean,
        val purchaseToken: String
    )

    data class BillingResponse(
        val message: String,
        val responseCode: Int = 99
    )
}