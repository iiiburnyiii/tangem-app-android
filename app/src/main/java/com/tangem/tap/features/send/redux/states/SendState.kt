package com.tangem.tap.features.send.redux.states

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.extensions.isZero
import com.tangem.tap.common.CurrencyConverter
import com.tangem.tap.common.entities.TapCurrency
import com.tangem.tap.domain.TapError
import com.tangem.tap.store
import org.rekotlin.StateType
import java.math.BigDecimal

/**
 * Created by Anton Zhilenkov on 31/08/2020.
 */

interface IdStateHolder {
    val stateId: StateId
}

enum class StateId {
    SEND_SCREEN, ADDRESS_PAY_ID, AMOUNT, FEE, RECEIPT
}

interface SendScreenState : StateType, IdStateHolder

data class SendState(
        val walletManager: WalletManager? = null,
        val coinConverter: CurrencyConverter = CurrencyConverter(BigDecimal.ONE, 2),
        val tokenConverter: CurrencyConverter = CurrencyConverter(BigDecimal.ONE, 2),
        val lastChangedStates: LinkedHashSet<StateId> = linkedSetOf(),
        val addressPayIdState: AddressPayIdState = AddressPayIdState(),
        val amountState: AmountState = AmountState(),
        val feeState: FeeState = FeeState(),
        val receiptState: ReceiptState = ReceiptState(),
        val sendButtonState: SendButtonState = SendButtonState.DISABLED,
        val hasInitializationError: Boolean = false
) : SendScreenState {

    override val stateId: StateId = StateId.SEND_SCREEN

    fun isReadyToSend(): Boolean {
        val sendState = store.state.sendState
        return addressPayIdIsReady() && sendState.amountState.isReady() && sendState.feeState.isReady()
    }

    fun addressPayIdIsReady(): Boolean = store.state.sendState.addressPayIdState.isReady()

    fun getDecimals(type: MainCurrencyType): Int = when (type) {
        MainCurrencyType.FIAT -> 2
        MainCurrencyType.CRYPTO -> amountState.walletAmount?.decimals ?: 0
    }

    fun getConverter(): CurrencyConverter {
        return if (amountState.typeOfAmount == AmountType.Coin) coinConverter
        else tokenConverter
    }

    fun convertToCrypto(value: BigDecimal): BigDecimal {
        return getConverter().toCrypto(value)
    }

    fun convertToFiat(value: BigDecimal, scaleWithPrecision: Boolean = false): BigDecimal {
        val converter = getConverter()
        return if (!scaleWithPrecision) converter.toFiat(value) else converter.toFiatWithPrecision(value)
    }

    fun getButtonState(): SendButtonState = if (isReadyToSend()) SendButtonState.ENABLED else SendButtonState.DISABLED

    fun getTotalAmountToSend(value: BigDecimal = amountState.amountToSendCrypto): BigDecimal {
        val needToExtractFee = amountState.isCoinAmount() && feeState.feeIsIncluded
        return if (needToExtractFee) value.minus(feeState.getCurrentFee()) else value
    }
}

enum class SendButtonState {
    ENABLED, DISABLED, PROGRESS
}

data class AmountState(
        val walletAmount: Amount? = null,
        val typeOfAmount: AmountType = AmountType.Coin,
        val viewAmountValue: InputViewValue = InputViewValue(BigDecimal.ZERO.toPlainString()),
        val viewBalanceValue: String = BigDecimal.ZERO.toPlainString(),
        val mainCurrency: MainCurrency = MainCurrency(MainCurrencyType.FIAT, TapCurrency.DEFAULT_FIAT_CURRENCY),
        val amountToSendCrypto: BigDecimal = BigDecimal.ZERO,
        val balanceCrypto: BigDecimal = BigDecimal.ZERO,
        val cursorAtTheSamePosition: Boolean = true,
        val maxLengthOfAmount: Int = 2,
        val error: TapError? = null
) : SendScreenState {

    override val stateId: StateId = StateId.AMOUNT

    fun isReady(): Boolean = error == null && !amountToSendCrypto.isZero()

    fun isCoinAmount(): Boolean = typeOfAmount == AmountType.Coin

    fun createMainCurrency(type: MainCurrencyType): MainCurrency {
        return when (type) {
            MainCurrencyType.FIAT -> MainCurrency(type, store.state.globalState.appCurrency)
            MainCurrencyType.CRYPTO -> MainCurrency(type, walletAmount?.currencySymbol ?: "NONE")
        }
    }
}

data class InputViewValue(val value: String, val isFromUserInput: Boolean = false)
enum class MainCurrencyType {
    FIAT, CRYPTO
}

data class MainCurrency(
        val type: MainCurrencyType,
        val currencySymbol: String
)