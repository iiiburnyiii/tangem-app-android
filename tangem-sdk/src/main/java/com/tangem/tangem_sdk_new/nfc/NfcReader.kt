package com.tangem.tangem_sdk_new.nfc

import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcV
import com.tangem.CardReader
import com.tangem.Log
import com.tangem.TangemSdkError
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.ResponseApdu

/**
 * Provides NFC communication between an Android application and Tangem card.
 */
class NfcReader : CardReader {

    var readingActive = false
        private set
    var nfcEnabled = false
    var manager: NfcManager? = null
    private var isoDep: IsoDep? = null
        set(value) {
            // don't reassign when there's an active tag already
            if (field == null) {
                field = value
                // if tag is received, call connect first before transceiving data
                if (value != null) connect()
            }
            if (value == null) field = value
        }

    var readingCancelled = false
        set(value) {
            field = value
            if (value) {
                // Stops reading and sends failure callback to a task
                // if reading is cancelled (when user closes nfc bottom sheet dialog).
                closeSession()
                callback?.invoke(CompletionResult.Failure(TangemSdkError.UserCancelled()))
            }
        }

    private var data: ByteArray? = null
    private var callback: ((response: CompletionResult<ResponseApdu>) -> Unit)? = null

    override fun openSession() {
        Log.i(this::class.simpleName!!, "NFC reader is starting NFC session")
        readingActive = true
        readingCancelled = false
        manager?.disableReaderMode()
        manager?.enableReaderMode()
    }

    override fun closeSession() {
        isoDep = null
        readingActive = false
    }

    override fun transceiveApdu(apdu: CommandApdu, callback: (response: CompletionResult<ResponseApdu>) -> Unit) {
        data = apdu.apduData
        this.callback = callback
        if (isoDep != null) {
            transceiveData()
        }
    }

    private fun transceiveData() {
        if (readingCancelled) {
            callback?.invoke(CompletionResult.Failure(TangemSdkError.UserCancelled()))
            return
        }
        if (data == null) return

        val rawResponse: ByteArray?
        try {
            Log.i(this::class.simpleName!!, "Sending data to the card")
            rawResponse = isoDep?.transceive(data)
        } catch (exception: TagLostException) {
            callback?.invoke(CompletionResult.Failure(TangemSdkError.TagLost()))
            isoDep = null
            return
        } catch (exception: Exception) {
            Log.i(this::class.simpleName!!, exception.localizedMessage ?: "Error tranceiving data")
            isoDep = null
            return
        }
        if (rawResponse != null) {
            Log.i(this::class.simpleName!!, "Data from the card was received")
            data = null
        }
        rawResponse?.let { callback?.invoke(CompletionResult.Success(ResponseApdu(it))) }
    }

    fun onTagDiscovered(tag: Tag?) {
        NfcV.get(tag)?.let { onNfcVDiscovered(it) }
        isoDep = IsoDep.get(tag)
        transceiveData()
    }


    private fun connect() {
        isoDep?.connect()
        isoDep?.close()
        isoDep?.connect()
        isoDep?.timeout = 240000
        Log.i(this::class.simpleName!!, "NFC tag is connected")
    }

    private fun onNfcVDiscovered(nfcV: NfcV) {
        val response = SlixTagReader().transceive(nfcV)
        when (response) {
            is SlixReadResult.Failure -> {
                Log.e(this::class.simpleName!!, "${response.exception.message}")
                callback?.invoke(CompletionResult.Failure(TangemSdkError.ErrorProcessingCommand()))
            }
            is SlixReadResult.Success -> {
                 callback?.invoke(CompletionResult.Success(ResponseApdu(response.data)))
            }
        }
    }
}