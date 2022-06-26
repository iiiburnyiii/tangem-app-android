package com.tangem.tap.features.wallet.redux.reducers

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Wallet
import com.tangem.common.extensions.isZero
import com.tangem.tap.common.extensions.toFiatString
import com.tangem.tap.common.extensions.toFiatValue
import com.tangem.tap.common.extensions.toFormattedCurrencyString
import com.tangem.tap.common.extensions.toFormattedFiatValue
import com.tangem.tap.domain.getFirstToken
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.filterByToken
import com.tangem.tap.features.wallet.models.getPendingTransactions
import com.tangem.tap.features.wallet.models.removeUnknownTransactions
import com.tangem.tap.features.wallet.redux.*
import com.tangem.tap.features.wallet.redux.WalletState.Companion.UNKNOWN_AMOUNT_SIGN
import com.tangem.tap.features.wallet.ui.BalanceStatus
import com.tangem.tap.features.wallet.ui.BalanceWidgetData
import com.tangem.tap.features.wallet.ui.TokenData
import com.tangem.tap.store

class OnWalletLoadedReducer {

    fun reduce(wallet: Wallet, blockchainNetwork: BlockchainNetwork, walletState: WalletState): WalletState {
        return if (!walletState.isMultiwalletAllowed) {
            onSingleWalletLoaded(wallet, walletState)
        } else {
            onMultiWalletLoaded(wallet, blockchainNetwork, walletState)
        }
    }

    private fun onMultiWalletLoaded(
        wallet: Wallet,
        blockchainNetwork: BlockchainNetwork,
        walletState: WalletState
    ): WalletState {
        val walletData = walletState.getWalletData(blockchainNetwork) ?: return walletState

        val fiatCurrency = store.state.globalState.appCurrency
        val exchangeManager = store.state.globalState.currencyExchangeManager

        val coinAmountValue = wallet.amounts[AmountType.Coin]?.value
        val formattedAmount = coinAmountValue?.toFormattedCurrencyString(
            wallet.blockchain.decimals(),
            wallet.blockchain.currency
        )

        val pendingTransactions = wallet.getPendingTransactions()
        val isCoinSendButtonEnabled = coinAmountValue?.isZero() == false && pendingTransactions.isEmpty()
        val balanceStatus = if (pendingTransactions.isNotEmpty()) {
            BalanceStatus.TransactionInProgress
        } else {
            BalanceStatus.VerifiedOnline
        }

        val fiatAmount = walletData.fiatRate?.let { coinAmountValue?.toFiatValue(it) }
        val fiatAmountFormatted = fiatAmount?.toFormattedFiatValue(fiatCurrency.symbol) ?: UNKNOWN_AMOUNT_SIGN

        val newWalletData = walletData.copy(
            currencyData = walletData.currencyData.copy(
                status = balanceStatus,
                currency = wallet.blockchain.fullName,
                currencySymbol = wallet.blockchain.currency,
                blockchainAmount = coinAmountValue,
                amount = coinAmountValue,
                amountFormatted = formattedAmount,
                fiatAmount = fiatAmount,
                fiatAmountFormatted = fiatAmountFormatted,
            ),
            pendingTransactions = pendingTransactions.removeUnknownTransactions(),
            mainButton = WalletMainButton.SendButton(isCoinSendButtonEnabled),
            currency = Currency.fromBlockchainNetwork(blockchainNetwork),
            tradeCryptoState = TradeCryptoState.from(exchangeManager, walletData),
        )

        val tokens = wallet.getTokens().mapNotNull { token ->
            val currency = Currency.fromBlockchainNetwork(blockchainNetwork, token)
            val tokenWalletData = walletState.getWalletData(currency)
            val tokenPendingTransactions = pendingTransactions.filterByToken(token)
            val tokenBalanceStatus = when {
                tokenPendingTransactions.isNotEmpty() -> BalanceStatus.TransactionInProgress
                pendingTransactions.isNotEmpty() -> BalanceStatus.SameCurrencyTransactionInProgress
                else -> BalanceStatus.VerifiedOnline
            }
            val tokenAmountValue = wallet.getTokenAmount(token)?.value
            val tokenFiatAmount = tokenWalletData?.fiatRate?.let { tokenAmountValue?.toFiatValue(it) }
            val tokenFiatAmountFormatted = tokenFiatAmount?.toFormattedFiatValue(fiatCurrency.symbol)
                ?: UNKNOWN_AMOUNT_SIGN

            val isTokenSendButtonEnabled = newWalletData.shouldEnableTokenSendButton()
                && pendingTransactions.isEmpty()
            tokenWalletData?.copy(
                currencyData = tokenWalletData.currencyData.copy(
                    status = tokenBalanceStatus,
                    blockchainAmount = coinAmountValue,
                    amount = tokenAmountValue,
                    amountFormatted = tokenAmountValue?.toFormattedCurrencyString(
                        token.decimals,
                        token.symbol
                    ),
                    fiatAmount = tokenFiatAmount,
                    fiatAmountFormatted = tokenFiatAmountFormatted,
                ),
                pendingTransactions = tokenPendingTransactions.removeUnknownTransactions(),
                mainButton = WalletMainButton.SendButton(isTokenSendButtonEnabled),
                tradeCryptoState = TradeCryptoState.from(exchangeManager, tokenWalletData),
            )
        }
        val newWallets = tokens + newWalletData
        val wallets = walletState.replaceSomeWallets((newWallets))

        return walletState
            .updateWalletsData(wallets)
    }

    private fun onSingleWalletLoaded(wallet: Wallet, walletState: WalletState): WalletState {
        if (wallet.blockchain != walletState.primaryBlockchain) return walletState

        val fiatCurrencyName = store.state.globalState.appCurrency.code
        val exchangeManager = store.state.globalState.currencyExchangeManager

        val token = wallet.getFirstToken()
        val tokenData = if (token != null) {
            val tokenAmount = wallet.getTokenAmount(token)
            if (tokenAmount != null) {
                val tokenFiatRate = walletState.primaryWallet?.currencyData?.token?.fiatRate
                val tokenFiatAmount =
                    tokenFiatRate?.let { tokenAmount.value?.toFiatString(it, fiatCurrencyName) }
                TokenData(
                    tokenAmount.value?.toFormattedCurrencyString(
                        token.decimals, token.symbol
                    ) ?: "",
                    tokenAmount.currencySymbol, tokenFiatAmount
                )
            } else {
                null
            }
        } else {
            null
        }
        val amount = wallet.amounts[AmountType.Coin]?.value
        val formattedAmount = amount?.toFormattedCurrencyString(
            wallet.blockchain.decimals(),
            wallet.blockchain.currency
        )
        val fiatAmount = walletState.primaryWallet?.fiatRate?.let { amount?.toFiatValue(it) }
        val fiatAmountFormatted = fiatAmount?.toFormattedFiatValue(fiatCurrencyName) ?: UNKNOWN_AMOUNT_SIGN

        val pendingTransactions = wallet.getPendingTransactions()
        val sendButtonEnabled = amount?.isZero() == false && pendingTransactions.isEmpty()
        val balanceStatus = if (pendingTransactions.isNotEmpty()) {
            BalanceStatus.TransactionInProgress
        } else {
            BalanceStatus.VerifiedOnline
        }
        val walletData = walletState.primaryWallet?.copy(
            currencyData = BalanceWidgetData(
                balanceStatus, wallet.blockchain.fullName,
                currencySymbol = wallet.blockchain.currency,
                token = tokenData,
                blockchainAmount = amount,
                amount = amount,
                amountFormatted = formattedAmount,
                fiatAmount = fiatAmount,
                fiatAmountFormatted = fiatAmountFormatted
            ),
            pendingTransactions = pendingTransactions.removeUnknownTransactions(),
            mainButton = WalletMainButton.SendButton(sendButtonEnabled),
            tradeCryptoState = TradeCryptoState.from(exchangeManager, walletState.primaryWallet),
        )
        val wallets = listOfNotNull(walletData)
        val updatedStore = walletState.getWalletStore(walletData?.currency)?.updateWallets(wallets)

        return walletState.updateWalletStore(updatedStore).copy(
            state = ProgressState.Done, error = null
        )
    }
}
