package com.tangem.feature.wallet.presentation.wallet.state2.transformers

import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state2.model.BalancesAndLimitsBlockState
import com.tangem.feature.wallet.presentation.wallet.state2.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2

internal class SetBalancesAndLimitsTransformer(
    userWallet: UserWallet,
    private val clickIntents: WalletClickIntentsV2,
) : WalletStateTransformer(userWallet.walletId) {

    override fun transform(prevState: WalletState): WalletState {
        return prevState.transformWhenInState<WalletState.Visa.Content> { state ->
            state.copy(
                balancesAndLimitBlockState = state.balancesAndLimitBlockState.toLoadedState(),
            )
        }
    }

    // TODO: Implement in https://tangem.atlassian.net/browse/AND-5209
    @Suppress("UnusedReceiverParameter")
    private fun BalancesAndLimitsBlockState.toLoadedState(): BalancesAndLimitsBlockState {
        return BalancesAndLimitsBlockState.Content(
            availableBalance = "400.00",
            currencySymbol = "USDT",
            limitDays = 7,
            isEnabled = true,
            onClick = clickIntents::onBalancesAndLimitsClick,
        )
    }
}
