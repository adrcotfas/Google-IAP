package com.limerse.iap

interface BillingServiceListener {

    /**
     * Callback will be triggered upon successful billing setup.
     * The user can safely attempt purchases.
     */
    fun onBillingSetupSuccess()

    /**
     * Callback will be triggered upon obtaining information about product prices
     *
     * @param iapKeyPrices - a map with available products
     */
    fun onPricesUpdated(iapKeyPrices: Map<String, String>)

    /**
     * Callback will be triggered upon successful acknowledgement of a purchase.
     * @param sku - the SKU of the product corresponding to the acknowledged purchase
     */
    fun onPurchaseAcknowledged(sku: String)

    /**
     * Callback will be triggered when a purchase for a product was not found.
     * @param sku - the SKU of the product which was not bought, was revoked or refunded
     */
    fun onPurchaseNotFound(sku: String)
}