package com.alphelios.iap

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode.*
import com.android.billingclient.api.BillingClient.SkuType.INAPP
import org.json.JSONException
import org.json.JSONObject

/**
 * Wrapper class for Google In-App purchases.
 * Handles vital processes while dealing with IAP.
 * Works around a listener [InAppEventsListener] for delivering events back to the caller.
 */
class IapConnector(context: Context, private val base64Key: String) {
    private var shouldAutoAcknowledge: Boolean = false
    private var fetchedSkuDetailsList = mutableListOf<SkuDetails>()
    private val tag = "InAppLog"
    private var inAppEventsListener: InAppEventsListener? = null

    private var inAppIds: List<String>? = null

    private lateinit var iapClient: BillingClient

    init {
        init(context)
    }

    /**
     * To set INAPP product IDs.
     */
    fun setInAppProductIds(inAppIds: List<String>): IapConnector {
        this.inAppIds = inAppIds
        return this
    }

    /**
     * Iap will auto acknowledge the purchase
     */
    fun autoAcknowledge(): IapConnector {
        shouldAutoAcknowledge = true
        return this
    }

    /**
     * Called to purchase an item.
     * Its result is received in [PurchasesUpdatedListener] which further is handled
     * by [processPurchases].
     */
    fun makePurchase(activity: Activity, sku: String) {
        if (fetchedSkuDetailsList.isEmpty())
            inAppEventsListener?.onError(this, DataWrappers.BillingResponse("Products not fetched"))
        else
            iapClient.launchBillingFlow(
                    activity,
                    BillingFlowParams.newBuilder()
                            .setSkuDetails(fetchedSkuDetailsList.find { it.sku == sku }!!).build()
            )
    }

    /**
     * To attach an event listener to establish a bridge with the caller.
     */
    fun setOnInAppEventsListener(inAppEventsListener: InAppEventsListener) {
        this.inAppEventsListener = inAppEventsListener
    }

    /**
     * To initialise IapConnector.
     */
    private fun init(context: Context) {
        iapClient = BillingClient.newBuilder(context)
                .enablePendingPurchases()
                .setListener { billingResult, purchases ->
                    /**
                     * Only recent purchases are received here
                     */
                    when (billingResult.responseCode) {
                        OK -> {
                            purchases?.let { processPurchases(purchases) }
                        }
                        ITEM_ALREADY_OWNED -> inAppEventsListener?.onError(
                                this,
                                billingResult.run {
                                    DataWrappers.BillingResponse(
                                            debugMessage,
                                            responseCode
                                    )
                                }
                        )
                        SERVICE_DISCONNECTED -> connect()
                        else -> Log.i(tag, "Purchase update : ${billingResult.debugMessage}")
                    }
                }.build()
    }

    /**
     * Connects billing client with Play console to start working with IAP.
     */
    fun connect(): IapConnector {
        Log.d(tag, "Billing service : Connecting...")
        if (!iapClient.isReady) {
            iapClient.startConnection(object : BillingClientStateListener {
                override fun onBillingServiceDisconnected() {
                    inAppEventsListener?.onError(
                            this@IapConnector,
                            DataWrappers.BillingResponse("Billing service : Disconnected")
                    )
                }

                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    when (billingResult.responseCode) {
                        OK -> {
                            Log.d(tag, "Billing service : Connected")
                            inAppIds?.let { querySku(it) }
                        }
                        BILLING_UNAVAILABLE -> Log.d(tag, "Billing service : Unavailable")
                        else -> Log.d(tag, "Billing service : Setup error")
                    }
                }
            })
        }
        return this
    }

    /**
     * Fires a query in Play console to get [SkuDetails] for provided type and IDs.
     */
    private fun querySku(ids: List<String>) {
        iapClient.querySkuDetailsAsync(
                SkuDetailsParams.newBuilder()
                        .setSkusList(ids).setType(INAPP).build()
        ) { billingResult, skuDetailsList ->
            when (billingResult.responseCode) {
                OK -> {
                    if (skuDetailsList!!.isEmpty()) {
                        Log.d(tag, "Query SKU : Data not found (List empty)")
                        inAppEventsListener?.onError(
                                this,
                                billingResult.run {
                                    DataWrappers.BillingResponse(
                                            debugMessage,
                                            responseCode
                                    )
                                })
                    } else {
                        Log.d(tag, "Query SKU : Data found")

                        fetchedSkuDetailsList.addAll(skuDetailsList)

                        val fetchedSkuInfo = skuDetailsList.map {
                            getSkuInfo(it)
                        }

                        inAppEventsListener?.onInAppProductsFetched(fetchedSkuInfo)
                    }
                }
                else -> {
                    Log.d(tag, "Query SKU : Failed")
                    inAppEventsListener?.onError(
                            this,
                            billingResult.run {
                                DataWrappers.BillingResponse(
                                        debugMessage,
                                        responseCode
                                )
                            }
                    )
                }
            }
        }
    }

    private fun getSkuInfo(skuDetails: SkuDetails): DataWrappers.SkuInfo {
        return DataWrappers.SkuInfo(skuDetails.sku)
    }

    /**
     * Returns all the **non-consumable** purchases of the user.
     */
    fun getAllPurchases() {
        if (iapClient.isReady) {
            val allPurchases = mutableListOf<Purchase>()
            allPurchases.addAll(iapClient.queryPurchases(INAPP).purchasesList!!)
            processPurchases(allPurchases)
        } else {
            inAppEventsListener?.onError(
                    this,
                    DataWrappers.BillingResponse("Client not initialized yet.")
            )
        }
    }

    /**
     * Checks purchase signature for more security.
     */
    private fun processPurchases(allPurchases: List<Purchase>) {
        if (allPurchases.isNotEmpty()) {
            val validPurchases = allPurchases.filter {
                isPurchaseSignatureValid(it)
            }.map { purchase ->
                DataWrappers.PurchaseInfo(
                        purchase.sku,
                        getUnmaskedPurchaseState(purchase),
                        purchase.isAcknowledged,
                        purchase.purchaseToken
                )
            }

            inAppEventsListener?.onProductsPurchased(validPurchases)

            if (shouldAutoAcknowledge)
                validPurchases.forEach {
                    acknowledgePurchase(it)
                }
        } else {
            inAppEventsListener?.onNoOwnedProductsFound()
        }
    }

    /**
     * Purchase of non-consumable products must be acknowledged to Play console.
     * This will avoid refunding for these products to users by Google.
     *
     * Consumable products might be brought/consumed by users multiple times (for eg. diamonds, coins).
     * Hence, it is necessary to notify Play console about such products.
     */
    private fun acknowledgePurchase(purchase: DataWrappers.PurchaseInfo) {
        purchase.run {
            iapClient.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseToken).build()) { billingResult ->
                when (billingResult.responseCode) {
                    OK -> inAppEventsListener?.onPurchaseAcknowledged(this)
                    DEVELOPER_ERROR -> {
                        if (billingResult.debugMessage == "Item is not owned by the user.") {
                            inAppEventsListener?.onNotOwnedProductFound(purchase.sku)
                        }
                    }
                    else -> {
                        Log.d(tag, "Handling non consumables : Error -> ${billingResult.debugMessage}")
                        inAppEventsListener?.onError(
                                this@IapConnector,
                                billingResult.run {
                                    DataWrappers.BillingResponse(
                                            debugMessage,
                                            responseCode
                                    )
                                }
                        )
                    }
                }
            }
        }
    }

    /**
     * Checks purchase signature validity
     */
    private fun isPurchaseSignatureValid(purchase: Purchase): Boolean {
        return Security.verifyPurchase(
                base64Key, purchase.originalJson, purchase.signature
        )
    }

    companion object {
        fun getUnmaskedPurchaseState(purchase: Purchase): Int {
            var purchaseState = 0
            try {
                purchaseState = JSONObject(purchase.originalJson).optInt("purchaseState", 0)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            return purchaseState
        }
    }
}