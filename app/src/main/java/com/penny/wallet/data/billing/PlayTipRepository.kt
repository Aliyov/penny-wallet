package com.penny.wallet.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/** Google Play Billing, connected only while the Support screen is open. */
class PlayTipRepository(context: Context) : TipRepository {
    private val _state = MutableStateFlow<TipsState>(TipsState.Loading)
    override val state: StateFlow<TipsState> = _state
    private val _events = Channel<TipEvent>(Channel.BUFFERED)
    override val events: Flow<TipEvent> = _events.receiveAsFlow()
    private val details = mutableMapOf<TipKind, ProductDetails>()

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener { result, purchases -> onPurchases(result, purchases) }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    init {
        runCatching {
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingResponseCode.OK) {
                        loadProducts()
                        consumeLeftovers()
                    } else {
                        _state.value = TipsState.Unavailable
                    }
                }

                override fun onBillingServiceDisconnected() {
                    if (_state.value is TipsState.Loading) _state.value = TipsState.Unavailable
                }
            })
        }.onFailure { _state.value = TipsState.Unavailable }
    }

    private fun loadProducts() {
        val products = TipKind.entries.map {
            QueryProductDetailsParams.Product.newBuilder().setProductId(it.productId).setProductType(ProductType.INAPP).build()
        }
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(products).build()) { result, found ->
            val list = found.productDetailsList
            if (result.responseCode != BillingResponseCode.OK || list.isEmpty()) {
                _state.value = TipsState.Unavailable
                return@queryProductDetailsAsync
            }
            val tips = TipKind.entries.mapNotNull { kind ->
                val d = list.firstOrNull { it.productId == kind.productId } ?: return@mapNotNull null
                details[kind] = d
                d.oneTimePurchaseOfferDetails?.formattedPrice?.let { Tip(kind, it) }
            }
            _state.value = if (tips.isEmpty()) TipsState.Unavailable else TipsState.Ready(tips)
        }
    }

    /** A tip paid while the app was closed (or killed before consuming) is consumed now and thanked for. */
    private fun consumeLeftovers() {
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()) { result, purchases ->
            if (result.responseCode == BillingResponseCode.OK) purchases.forEach(::handle)
        }
    }

    override fun buy(activity: Activity, kind: TipKind) {
        val d = details[kind] ?: return run { _events.trySend(TipEvent.FAILED) }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build()))
            .build()
        val r = client.launchBillingFlow(activity, params)
        if (r.responseCode != BillingResponseCode.OK && r.responseCode != BillingResponseCode.USER_CANCELED) _events.trySend(TipEvent.FAILED)
    }

    private fun onPurchases(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> purchases.orEmpty().forEach(::handle)
            BillingResponseCode.USER_CANCELED -> Unit
            BillingResponseCode.ITEM_ALREADY_OWNED -> consumeLeftovers()
            else -> _events.trySend(TipEvent.FAILED)
        }
    }

    private fun handle(p: Purchase) {
        when (p.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> client.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) { r, _ ->
                if (r.responseCode == BillingResponseCode.OK) _events.trySend(TipEvent.THANKS)
            }
            Purchase.PurchaseState.PENDING -> _events.trySend(TipEvent.PENDING)
            else -> Unit
        }
    }

    override fun close() {
        runCatching { client.endConnection() }
    }
}
