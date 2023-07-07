package com.tangem.feature.wallet.presentation.common.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

/** Token item state */
@Immutable
internal sealed interface TokenItemState {

    /** Loading token state */
    object Loading : TokenItemState

    /**
     * Content token state
     *
     * @property tokenIconUrl        token icon url
     * @property tokenIconResId      token icon resource id
     * @property networkIconResId    network icon resource id, may be null if it is a coin
     * @property name                token name
     * @property amount              amount of token
     * @property hasPending          pending tx in blockchain
     * @property tokenOptions        state for token options
     */
    data class Content(
        val id: String,
        val tokenIconUrl: String?,
        @DrawableRes val tokenIconResId: Int,
        @DrawableRes val networkIconResId: Int?,
        val name: String,
        val amount: String,
        val hasPending: Boolean,
        val tokenOptions: TokenOptionsState,
    ) : TokenItemState

    /**
     * Draggable token state
     *
     * @property tokenIconUrl        token icon url
     * @property tokenIconResId      token icon resource id
     * @property networkIconResId    network icon resource id, may be null if it is a coin
     * @property name                token name
     */
    data class Draggable(
        val id: String,
        val tokenIconUrl: String?,
        @DrawableRes val tokenIconResId: Int,
        @DrawableRes val networkIconResId: Int?,
        val name: String,
        val fiatAmount: String,
    ) : TokenItemState

    /**
     * Unreachable token state
     *
     * @property id                  token id
     * @property tokenIconUrl        token icon url
     * @property tokenIconResId      token icon resource id
     * @property networkIconResId    network icon resource id, may be null if it is a coin
     * @property name                token name
     */
    data class Unreachable(
        val id: String,
        val tokenIconUrl: String?,
        @DrawableRes val tokenIconResId: Int,
        @DrawableRes val networkIconResId: Int?,
        val name: String,
    ) : TokenItemState

    /** Token options state */
    sealed interface TokenOptionsState {

        /**
         * Visible token options state
         *
         * @property fiatAmount fiat amount of token
         * @property priceChange value of price changing
         */
        data class Visible(val fiatAmount: String, val priceChange: PriceChangeConfig) : TokenOptionsState

        /**
         * Hidden token options state
         *
         * @property priceChange value of price changing
         */
        data class Hidden(val priceChange: PriceChangeConfig) : TokenOptionsState
    }
}
