package com.tangem.tap.network.exchangeServices

import com.tangem.Message
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.models.scan.CardDTO
import com.tangem.tap.common.extensions.safeUpdate
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.TangemSigner
import com.tangem.tap.domain.model.Currency
import com.tangem.tap.features.demo.isDemoCard
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
import java.math.BigDecimal

/**
 * Created by Anton Zhilenkov on 02/03/2022.
 */
class CurrencyExchangeManager(
    private val buyService: ExchangeService,
    private val sellService: ExchangeService,
    private val primaryRules: ExchangeRules,
) : ExchangeService {

    override fun featureIsSwitchedOn(): Boolean = primaryRules.featureIsSwitchedOn()

    override suspend fun update() {
        buyService.update()
        sellService.update()
    }

    override fun isBuyAllowed(): Boolean = primaryRules.isBuyAllowed() && buyService.isBuyAllowed()
    override fun isSellAllowed(): Boolean = primaryRules.isSellAllowed() && sellService.isSellAllowed()

    override fun availableForBuy(currency: Currency): Boolean {
        return primaryRules.availableForBuy(currency) && buyService.availableForBuy(currency)
    }

    override fun availableForSell(currency: Currency): Boolean {
        return primaryRules.availableForSell(currency) && sellService.availableForSell(currency)
    }

    override fun getUrl(
        action: Action,
        blockchain: Blockchain,
        cryptoCurrencyName: CryptoCurrencyName,
        fiatCurrencyName: String,
        walletAddress: String,
        isDarkTheme: Boolean,
    ): String? {
        if (blockchain.isTestnet()) return blockchain.getTestnetTopUpUrl()

        val urlBuilder = getExchangeUrlBuilder(action)
        return urlBuilder.getUrl(
            action,
            blockchain,
            cryptoCurrencyName,
            fiatCurrencyName,
            walletAddress,
            isDarkTheme,
        )
    }

    override fun getSellCryptoReceiptUrl(action: Action, transactionId: String): String? {
        val urlBuilder = getExchangeUrlBuilder(action)
        return urlBuilder.getSellCryptoReceiptUrl(action, transactionId)
    }

    private fun getExchangeUrlBuilder(action: Action): ExchangeUrlBuilder {
        return when (action) {
            Action.Buy -> buyService
            Action.Sell -> sellService
        }
    }

    enum class Action { Buy, Sell }

    companion object {
        fun dummy(): CurrencyExchangeManager = CurrencyExchangeManager(
            buyService = ExchangeService.dummy(),
            sellService = ExchangeService.dummy(),
            primaryRules = ExchangeRules.dummy(),
        )
    }
}

suspend fun buyErc20TestnetTokens(card: CardDTO, walletManager: EthereumWalletManager, destinationAddress: String) {
    walletManager.safeUpdate(card.isDemoCard())

    val amountToSend = Amount(walletManager.wallet.blockchain)

    val feeResult = walletManager.getFee(amountToSend, destinationAddress) as? Result.Success ?: return
    val fee = when (val feeForTx = feeResult.data) {
        is TransactionFee.Choosable -> feeForTx.minimum
        is TransactionFee.Single -> feeForTx.normal
    }

    val coinValue = walletManager.wallet.amounts[AmountType.Coin]?.value ?: BigDecimal.ZERO
    if (coinValue < fee.amount.value) return

    val signer = TangemSigner(
        card = card,
        tangemSdk = store.state.daggerGraphState.get(DaggerGraphState::cardSdkConfigRepository).sdk,
        initialMessage = Message(),
    ) { signResponse ->
        store.dispatch(
            GlobalAction.UpdateWalletSignedHashes(
                walletSignedHashes = signResponse.totalSignedHashes,
                walletPublicKey = walletManager.wallet.publicKey.seedKey,
                remainingSignatures = signResponse.remainingSignatures,
            ),
        )
    }

    walletManager.send(
        transactionData = walletManager.createTransaction(
            amount = amountToSend,
            fee = fee,
            destination = destinationAddress,
        ),
        signer = signer,
    )
}
