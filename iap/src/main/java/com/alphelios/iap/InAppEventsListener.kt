package com.alphelios.iap

/**
 * Establishes communication bridge between caller and [IapConnector].
 * [onProductsPurchased] provides recent purchases
 * [onPurchaseAcknowledged] provides a callback after a purchase is acknowledged
 * [onError] is used to notify caller about possible errors
 * [onNoOwnedProductsFound] is used to notify the caller that no purchased products were found
 * [onNotOwnedProductFound] is used to notify the caller that a specific product is not owned
 *                          and was probably refunded
 */
interface InAppEventsListener {
    fun onInAppProductsFetched(skuDetailsList: List<DataWrappers.SkuInfo>)
    fun onPurchaseAcknowledged(purchase: DataWrappers.PurchaseInfo)
    fun onProductsPurchased(purchases: List<DataWrappers.PurchaseInfo>)
    fun onError(inAppConnector: IapConnector, result: DataWrappers.BillingResponse? = null)
    fun onNoOwnedProductsFound()
    fun onNotOwnedProductFound(sku: String)
}
