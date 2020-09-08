package com.tangem.tap.features.send.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.core.view.postDelayed
import androidx.core.widget.addTextChangedListener
import com.tangem.tap.common.KeyboardObserver
import com.tangem.tap.common.entities.TapCurrency
import com.tangem.tap.common.extensions.getFromClipboard
import com.tangem.tap.common.qrCodeScan.ScanQrCodeActivity
import com.tangem.tap.common.snackBar.MaxAmountSnackbar
import com.tangem.tap.common.text.truncateMiddleWith
import com.tangem.tap.features.send.BaseStoreFragment
import com.tangem.tap.features.send.redux.AddressPayIdActionUi.*
import com.tangem.tap.features.send.redux.AmountActionUi.*
import com.tangem.tap.features.send.redux.FeeActionUi.*
import com.tangem.tap.features.send.redux.FeeType
import com.tangem.tap.features.send.redux.MainCurrencyType
import com.tangem.tap.features.send.redux.ReleaseSendState
import com.tangem.tap.features.send.ui.stateSubscribers.SendStateSubscriber
import com.tangem.tap.mainScope
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.btn_paste.*
import kotlinx.android.synthetic.main.btn_qr_code.*
import kotlinx.android.synthetic.main.layout_send_address_payid.*
import kotlinx.android.synthetic.main.layout_send_amount.*
import kotlinx.android.synthetic.main.layout_send_network_fee.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

/**
 * Created by Anton Zhilenkov on 31/08/2020.
 */
class SendFragment : BaseStoreFragment(R.layout.fragment_send) {

    private val sendSubscriber = SendStateSubscriber(this)
    private lateinit var keyboardObserver: KeyboardObserver

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAddressOrPayIdLayout()
        setupAmountLayout()
        setupFeeLayout()
    }

    private fun setupAddressOrPayIdLayout() {
        store.dispatch(SetTruncateHandler { etAddressOrPayId.truncateMiddleWith(it, " *** ") })

        etAddressOrPayId.setOnFocusChangeListener { v, hasFocus ->
            store.dispatch(TruncateOrRestore(!hasFocus))
        }
        etAddressOrPayId.inputedTextAsFlow()
                .debounce(400)
                .filter { store.state.sendState.addressPayIdState.etFieldValue != it }
                .onEach { store.dispatch(ChangeAddressOrPayId(it)) }
                .launchIn(mainScope)

        imvPaste.setOnClickListener {
            store.dispatch(ChangeAddressOrPayId(requireContext().getFromClipboard()?.toString() ?: ""))
            store.dispatch(TruncateOrRestore(!etAddressOrPayId.isFocused))
        }
        imvQrCode.setOnClickListener {
            startActivityForResult(
                    Intent(requireContext(), ScanQrCodeActivity::class.java),
                    ScanQrCodeActivity.SCAN_QR_REQUEST_CODE
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != ScanQrCodeActivity.SCAN_QR_REQUEST_CODE) return

        val scannedCode = data?.getStringExtra(ScanQrCodeActivity.SCAN_RESULT) ?: ""
        store.dispatch(ChangeAddressOrPayId(scannedCode))
        store.dispatch(TruncateOrRestore(!etAddressOrPayId.isFocused))
    }

    private fun setupAmountLayout() {
        store.dispatch(SetMainCurrency(restoreMainCurrency()))
        tvAmountCurrency.setOnClickListener { store.dispatch(ToggleMainCurrency) }

        val maxAmountSnackbar = MaxAmountSnackbar.make(etAmountToSend) { store.dispatch(SetMaxAmount) }
        var snackbarControlledByChangingFocus = false
        keyboardObserver = KeyboardObserver(requireActivity())
        keyboardObserver.registerListener { isShow ->
            if (snackbarControlledByChangingFocus) return@registerListener

            if (isShow) {
                if (etAmountToSend.isFocused && !maxAmountSnackbar.isShown) maxAmountSnackbar.show()
            } else {
                if (maxAmountSnackbar.isShown) maxAmountSnackbar.dismiss()
            }
        }

        etAmountToSend.setOnFocusChangeListener { v, hasFocus ->
            snackbarControlledByChangingFocus = true
            if (hasFocus) {
                etAmountToSend.postDelayed(200) {
                    maxAmountSnackbar.show()
                    snackbarControlledByChangingFocus = false

                }
            } else {
                etAmountToSend.postDelayed(350) {
                    maxAmountSnackbar.dismiss()
                    snackbarControlledByChangingFocus = false
                }
            }
        }

        val prevFocusChangeListener = etAmountToSend.onFocusChangeListener
        etAmountToSend.setOnFocusChangeListener { v, hasFocus ->
            prevFocusChangeListener.onFocusChange(v, hasFocus)
            if (hasFocus && etAmountToSend.text?.toString() == "0") etAmountToSend.setText("")
            if (!hasFocus && etAmountToSend.text?.toString() == "") etAmountToSend.setText("0")
        }

        etAmountToSend.inputedTextAsFlow()
                .debounce(400)
                .filter { store.state.sendState.amountState.etAmountFieldValue != it }
                .onEach { store.dispatch(ChangeAmountToSend(it)) }
                .launchIn(mainScope)
    }

    private fun setupFeeLayout() {
        flExpandCollapse.setOnClickListener {
            store.dispatch(ToggleFeeLayoutVisibility)
        }
        chipGroup.setOnCheckedChangeListener { group, checkedId ->
            store.dispatch(ChangeSelectedFee(FeeUiHelper.idToFee(checkedId)))
        }
        swIncludeFee.setOnCheckedChangeListener { btn, isChecked ->
            store.dispatch(ChangeIncludeFee(isChecked))
        }
    }

    override fun subscribeToStore() {
        store.subscribe(sendSubscriber) { appState ->
            appState.skipRepeats { oldState, newState -> oldState == newState }.select { it.sendState }
        }
        storeSubscribersList.add(sendSubscriber)
    }

    fun restoreMainCurrency(): MainCurrencyType {
        val sp = requireContext().getSharedPreferences("SendScreen", Context.MODE_PRIVATE)
        val mainCurrency = sp.getString("mainCurrency", TapCurrency.main)
        val foundType = MainCurrencyType.values()
                .firstOrNull { it.name.toLowerCase() == mainCurrency!!.toLowerCase() } ?: MainCurrencyType.FIAT
        return foundType
    }

    fun saveMainCurrency(type: MainCurrencyType) {
        val sp = requireContext().getSharedPreferences("SendScreen", Context.MODE_PRIVATE)
        sp.edit().putString("mainCurrency", type.name).apply()
    }

    override fun onDestroy() {
        keyboardObserver.unregisterListener()
        store.dispatch(ReleaseSendState)
        super.onDestroy()
    }
}

@ExperimentalCoroutinesApi
fun EditText.inputedTextAsFlow(): Flow<String> = callbackFlow {
    val watcher = addTextChangedListener { editable -> offer(editable?.toString() ?: "") }
    awaitClose { removeTextChangedListener(watcher) }
}

class FeeUiHelper {
    companion object {
        fun feeToId(fee: FeeType): Int {
            return when (fee) {
                FeeType.LOW -> R.id.chipLow
                FeeType.NORMAL -> R.id.chipNormal
                FeeType.PRIORITY -> R.id.chipPriority
            }
        }

        fun idToFee(id: Int): FeeType {
            return when (id) {
                R.id.chipLow -> FeeType.LOW
                R.id.chipNormal -> FeeType.NORMAL
                R.id.chipPriority -> FeeType.PRIORITY
                else -> FeeType.NORMAL
            }
        }
    }
}




