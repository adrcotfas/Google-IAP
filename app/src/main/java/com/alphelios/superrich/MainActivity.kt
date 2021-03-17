package com.alphelios.superrich

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.alphelios.iap.DataWrappers
import com.alphelios.iap.IapConnector
import com.alphelios.iap.InAppEventsListener
import com.alphelios.superrich.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val tag: String = "IAP"
    private var fetchedSkuDetailsList = mutableListOf<DataWrappers.SkuInfo>()
    private lateinit var iapConnector: IapConnector
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        iapConnector = IapConnector(
            this, "key" // License Key
        )
            .setInAppProductIds(listOf("base", "moderate", "plenty", "quite"))
            .autoAcknowledge()
            .connect()

        iapConnector.setOnInAppEventsListener(object : InAppEventsListener {
            override fun onInAppProductsFetched(skuDetailsList: List<DataWrappers.SkuInfo>) {
                fetchedSkuDetailsList.addAll(skuDetailsList)
                Log.d(tag, "Retrieved SKU details list : $skuDetailsList")
            }

            override fun onPurchaseAcknowledged(purchase: DataWrappers.PurchaseInfo) {
                Log.d(tag, "onPurchaseAcknowledged")
            }

            override fun onProductsPurchased(purchases: List<DataWrappers.PurchaseInfo>) {
                purchases.forEach {
                    when (it.sku) {
                        "base" -> {

                        }
                        "moderate" -> {

                        }
                        "quite" -> {

                        }
                        "plenty" -> {

                        }
                        "subscribe" -> {

                        }
                        "yearly" -> {

                        }
                    }
                }
            }

            override fun onError(
                inAppConnector: IapConnector,
                result: DataWrappers.BillingResponse?
            ) {
                Log.d(tag, "Error : ${result?.message}")
            }

            override fun onNoOwnedProductsFound() {
                Log.d(tag, "onNoOwnedProductsFound")
            }

            override fun onNotOwnedProductFound(sku: String) {
                Log.d(tag, "onNotOwnedProductFound: + $sku")
            }
        })

        binding.btPurchaseCons.setOnClickListener {
            if (fetchedSkuDetailsList.find { it.sku == "base" } != null) {
                iapConnector.makePurchase(this,"base")
            }
        }
        binding.btnMonthly.setOnClickListener {
            if (fetchedSkuDetailsList.find { it.sku == "subscribe" } != null) {
                iapConnector.makePurchase(this, "subscribe")
            }
        }

        binding.btnYearly.setOnClickListener {
            if (fetchedSkuDetailsList.find { it.sku == "yearly" } != null) {
                iapConnector.makePurchase(this,"yearly")
            }
        }
        binding.btnQuite.setOnClickListener {
            if (fetchedSkuDetailsList.find { it.sku == "quite" } != null) {
                iapConnector.makePurchase(this,"quite")
            }
        }
        binding.btnModerate.setOnClickListener {
            if (fetchedSkuDetailsList.find { it.sku == "moderate" } != null) {
                iapConnector.makePurchase(this,"moderate")
            }
        }

        binding.btnUltimate.setOnClickListener {
            if (fetchedSkuDetailsList.find { it.sku == "plenty" } != null) {
                iapConnector.makePurchase(this,"plenty")
            }
        }
    }
}