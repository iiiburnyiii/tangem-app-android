package com.tangem

import com.tangem.commands.Card
import com.tangem.commands.EllipticCurve
import com.tangem.common.extensions.calculateSha256
import com.tangem.crypto.CryptoUtils.generatePublicKey


/**
 * Contains data relating to a Tangem card. It is used in constructing all the commands,
 * and commands can return modified [SessionEnvironment].
 *
 * @property card  Current card, read by preflight [com.tangem.commands.ReadCommand].
 * @property terminalKeys generated terminal keys used in Linked Terminal feature.
 */
data class SessionEnvironment(
        var pin2: ByteArray = DEFAULT_PIN2.calculateSha256(),
        var card: Card? = null,
        var terminalKeys: KeyPair? = null,
        var encryptionMode: EncryptionMode = EncryptionMode.NONE,
        var encryptionKey: ByteArray? = null,
        var cvc: ByteArray? = null,
        var cardFilter: CardFilter = CardFilter(),
        val handleErrors: Boolean = true
) {

    var isDefaultPin1: Boolean = Companion.pin1.contentEquals(DEFAULT_PIN.calculateSha256())
    var isDefaultPin2: Boolean = pin2.contentEquals(DEFAULT_PIN2.calculateSha256())

    var pin1: ByteArray = SessionEnvironment.pin1
    private set
    get() = SessionEnvironment.pin1

    fun setPin1(pin1: String) {
        SessionEnvironment.pin1 = pin1.calculateSha256()
    }

    fun setPin2(pin2: String) {
        this.pin2 = pin2.calculateSha256()
    }

    companion object {
        const val DEFAULT_PIN = "000000"
        const val DEFAULT_PIN2 = "000"

        var pin1: ByteArray = DEFAULT_PIN.calculateSha256()
    }
}

/**
 * All possible encryption modes.
 */
enum class EncryptionMode(val code: Int) {
    NONE(0x0),
    FAST(0x1),
    STRONG(0x2)
}

class KeyPair(val publicKey: ByteArray, val privateKey: ByteArray) {

    constructor(privateKey: ByteArray, curve: EllipticCurve = EllipticCurve.Secp256k1) :
            this(generatePublicKey(privateKey, curve), privateKey)
}
