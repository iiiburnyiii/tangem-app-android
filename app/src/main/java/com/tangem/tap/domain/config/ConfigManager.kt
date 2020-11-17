package com.tangem.tap.domain.config

import com.tangem.commands.Card
import com.tangem.tangem_sdk_new.ui.animation.VoidCallback
import java.util.*

/**
 * Created by Anton Zhilenkov on 12/11/2020.
 */
data class ConfigState(
        val coinMarketCapKey: String = "f6622117-c043-47a0-8975-9d673ce484de",
        val moonPayApiKey: String = "pk_test_kc90oYTANy7UQdBavDKGfL4K9l6VEPE",
        val moonPayApiSecretKey: String = "sk_test_V8w4M19LbDjjYOt170s0tGuvXAgyEb1C",
        val payIdIsEnabled: Boolean = true,
        val usePayId: Boolean = true,
        val useTopUp: Boolean = true,
        val isStart2Coin: Boolean = false
)

class ConfigManager(
        private val localLoader: ConfigLoader,
        private val remoteLoader: ConfigLoader
) {
    private val usePayId = "usePayId"
    private val payIdIsEnabled = "payIdIsEnabled"
    private val useTopUp = "useTopUp"
    private val isStart2Coin = "isStart2Coin"

    var config: ConfigState = ConfigState()
        private set

    fun load(onComplete: VoidCallback? = null) {
        localLoader.loadConfig {
            it.features?.forEach { feature -> updateFeature(feature.name, feature.value) }
        }
        remoteLoader.loadConfig {
            it.features?.forEach { feature -> updateFeature(feature.name, feature.value) }
            onComplete?.invoke()
        }
    }

    fun onCardScanned(card: Card) {
        updateFeature(isStart2Coin, card.cardData?.issuerName?.toLowerCase(Locale.US) == "start2coin")
    }

    private fun updateFeature(name: String, value: Boolean?) {
        val newValue = value ?: return

        when (name) {
            usePayId -> config = config.copy(usePayId = newValue)
            payIdIsEnabled -> config = config.copy(payIdIsEnabled = newValue)
            useTopUp -> config = config.copy(useTopUp = newValue)
            isStart2Coin -> config = config.copy(isStart2Coin = newValue)
        }
    }
}