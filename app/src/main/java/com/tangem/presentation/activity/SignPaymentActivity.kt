package com.tangem.presentation.activityimport android.app.Activityimport android.content.Intentimport android.content.res.ColorStateListimport android.graphics.Colorimport android.nfc.NfcAdapterimport android.nfc.Tagimport android.nfc.tech.IsoDepimport android.os.Bundleimport android.support.v7.app.AppCompatActivityimport android.view.KeyEventimport android.view.Viewimport android.widget.ProgressBarimport android.widget.Toastimport com.tangem.data.nfc.SignPaymentTaskimport com.tangem.domain.cardReader.CardProtocolimport com.tangem.domain.cardReader.NfcManagerimport com.tangem.domain.wallet.CoinEngineimport com.tangem.domain.wallet.TangemCardimport com.tangem.domain.wallet.TangemContextimport com.tangem.presentation.dialog.NoExtendedLengthSupportDialogimport com.tangem.presentation.dialog.WaitSecurityDelayDialogimport com.tangem.util.Utilimport com.tangem.wallet.Rimport kotlinx.android.synthetic.main.activity_sign_payment.*import kotlinx.android.synthetic.main.notification_template_lines_media.*import java.io.IOExceptionclass SignPaymentActivity : AppCompatActivity(), NfcAdapter.ReaderCallback, CardProtocol.Notifications {    companion object {        val TAG: String = SignPaymentActivity::class.java.simpleName        const val EXTRA_AMOUNT = "Amount"        const val EXTRA_AMOUNT_CURRENCY = "AmountCurrency"        const val EXTRA_FEE = "Fee"        const val EXTRA_FEE_CURRENCY = "FeeCurrency"        const val EXTRA_FEE_INCLUDED = "FeeIncluded"        const val EXTRA_TARGET_ADDRESS = "TargetAddress"        const val REQUEST_CODE_SEND_PAYMENT = 1        const val RESULT_INVALID_PIN = Activity.RESULT_FIRST_USER    }    private var nfcManager: NfcManager? = null    private lateinit var ctx: TangemContext    private var signPaymentTask: SignPaymentTask? = null    private var amount: CoinEngine.Amount? = null    private var fee: CoinEngine.Amount? = null    private var isIncludeFee = true    private var outAddressStr: String? = null    private var lastReadSuccess = true    private var progressBar: ProgressBar? = null    override fun onCreate(savedInstanceState: Bundle?) {        super.onCreate(savedInstanceState)        setContentView(R.layout.activity_sign_payment)        MainActivity.commonInit(applicationContext)        nfcManager = NfcManager(this, this)        ctx=TangemContext.loadFromBundle(this, intent.extras)        amount =  CoinEngine.Amount(intent.getStringExtra(EXTRA_AMOUNT), intent.getStringExtra(EXTRA_AMOUNT_CURRENCY))        fee = CoinEngine.Amount(intent.getStringExtra(EXTRA_FEE), intent.getStringExtra(EXTRA_FEE_CURRENCY))        isIncludeFee = intent.getBooleanExtra(EXTRA_FEE_INCLUDED, true)        outAddressStr = intent.getStringExtra(EXTRA_TARGET_ADDRESS)        tvCardID.text = ctx.card!!.cidDescription        progressBar = findViewById(R.id.progressBar)        progressBar!!.progressTintList = ColorStateList.valueOf(Color.DKGRAY)        progressBar!!.visibility = View.INVISIBLE    }    public override fun onResume() {        super.onResume()        nfcManager!!.onResume()    }    public override fun onPause() {        nfcManager!!.onPause()        if (signPaymentTask != null)            signPaymentTask!!.cancel(true)        super.onPause()    }    public override fun onStop() {        // dismiss enable NFC dialog        nfcManager!!.onStop()        if (signPaymentTask != null)            signPaymentTask!!.cancel(true)        super.onStop()    }    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {        if (requestCode == REQUEST_CODE_SEND_PAYMENT) {            setResult(resultCode, data)            finish()            return        }        super.onActivityResult(requestCode, resultCode, data)    }    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {        when (keyCode) {            KeyEvent.KEYCODE_BACK -> {                val intent = Intent()                setResult(Activity.RESULT_CANCELED, intent)                finish()                return true            }        }        return super.onKeyDown(keyCode, event)    }    override fun onTagDiscovered(tag: Tag) {        try {            // get IsoDep handle and run cardReader thread            val isoDep = IsoDep.get(tag)                    ?: throw CardProtocol.TangemException(getString(R.string.wrong_tag_err))            val uid = tag.id            val sUID = Util.byteArrayToHexString(uid)//            Log.v(TAG, "UID: $sUID")            if (sUID == ctx.card!!.uid) {                if (lastReadSuccess) {                    isoDep.timeout = ctx.card!!.pauseBeforePIN2 + 5000                } else {                    isoDep.timeout = ctx.card!!.pauseBeforePIN2 + 65000                }                signPaymentTask = SignPaymentTask(this, ctx, nfcManager, isoDep, this, amount, fee, isIncludeFee, outAddressStr)                signPaymentTask!!.start()            } else {//                Log.d(TAG, "Mismatch card UID (" + sUID + " instead of " + card!!.uid + ")")                nfcManager!!.ignoreTag(isoDep.tag)            }        } catch (e: Exception) {            e.printStackTrace()        }    }    override fun onReadStart(cardProtocol: CardProtocol) {        progressBar!!.post {            progressBar!!.visibility = View.VISIBLE            progressBar!!.progress = 5        }    }    override fun onReadProgress(protocol: CardProtocol, progress: Int) {        progressBar!!.post { progressBar!!.progress = progress }    }    override fun onReadFinish(cardProtocol: CardProtocol?) {        signPaymentTask = null        if (cardProtocol != null) {            if (cardProtocol.error == null) {                progressBar!!.post {                    progressBar!!.progress = 100                    progressBar!!.progressTintList = ColorStateList.valueOf(Color.GREEN)                }            } else {                lastReadSuccess = false                if (cardProtocol.error.javaClass == CardProtocol.TangemException_InvalidPIN::class.java) {                    progressBar!!.post {                        progressBar!!.progress = 100                        progressBar!!.progressTintList = ColorStateList.valueOf(Color.RED)                    }                    progressBar!!.postDelayed({                        try {                            progressBar!!.progress = 0                            progressBar!!.progressTintList = ColorStateList.valueOf(Color.DKGRAY)                            progressBar!!.visibility = View.INVISIBLE                            val intent = Intent()                            intent.putExtra("message", getString(R.string.cannot_sign_transaction__make_sure_you_enter_correct_pin_2))                            intent.putExtra("UID", cardProtocol.card.uid)                            intent.putExtra("Card", cardProtocol.card.asBundle)                            setResult(RESULT_INVALID_PIN, intent)                            finish()                        } catch (e: Exception) {                            e.printStackTrace()                        }                    }, 500)                } else {                    if (cardProtocol.error is CardProtocol.TangemException_WrongAmount) {                        try {                            val intent = Intent()                            intent.putExtra("message", getString(R.string.cannot_sign_transaction_wrong_amount))                            intent.putExtra("UID", cardProtocol.card.uid)                            intent.putExtra("Card", cardProtocol.card.asBundle)                            setResult(Activity.RESULT_CANCELED, intent)                            finish()                        } catch (e: Exception) {                            e.printStackTrace()                        }                    }                    progressBar!!.post {                        if (cardProtocol.error is CardProtocol.TangemException_ExtendedLengthNotSupported) {                            if (!NoExtendedLengthSupportDialog.allReadyShowed) {                                NoExtendedLengthSupportDialog.message = getText(R.string.the_nfc_adapter_length_apdu).toString() + "\n" + getText(R.string.the_nfc_adapter_length_apdu_advice).toString()                                NoExtendedLengthSupportDialog().show(fragmentManager, NoExtendedLengthSupportDialog.TAG)                            }                        } else {                            Toast.makeText(baseContext, R.string.try_to_scan_again, Toast.LENGTH_LONG).show()                        }                        progressBar!!.progress = 100                        progressBar!!.progressTintList = ColorStateList.valueOf(Color.RED)                    }                }            }        }        progressBar!!.postDelayed({            try {                progressBar!!.progress = 0                progressBar!!.progressTintList = ColorStateList.valueOf(Color.DKGRAY)                progressBar!!.visibility = View.INVISIBLE            } catch (e: Exception) {                e.printStackTrace()            }        }, 500)    }    override fun onReadCancel() {        signPaymentTask = null        progressBar!!.postDelayed({            try {                progressBar!!.progress = 0                progressBar!!.progressTintList = ColorStateList.valueOf(Color.DKGRAY)                progressBar!!.visibility = View.INVISIBLE            } catch (e: Exception) {                e.printStackTrace()            }        }, 500)    }    override fun onReadWait(msec: Int) {        WaitSecurityDelayDialog.OnReadWait(this, msec)    }    override fun onReadBeforeRequest(timeout: Int) {        WaitSecurityDelayDialog.onReadBeforeRequest(this, timeout)    }    override fun onReadAfterRequest() {        WaitSecurityDelayDialog.onReadAfterRequest(this)    }}