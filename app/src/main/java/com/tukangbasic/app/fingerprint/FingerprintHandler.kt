package com.tukangbasic.app.fingerprint

import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.CancellationSignal
import android.support.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.M)
class FingerprintHandler(private val context: Context) : FingerprintManager.AuthenticationCallback() {
    private var callback: (message: String, isSuccess: Boolean) -> Unit = { _, _ -> }
    fun auth(fingerPrintManager: FingerprintManager, cryptoObject: FingerprintManager.CryptoObject?,callback: (message: String, isSuccess: Boolean) -> Unit = { _, _ -> }) {
        val cancellationSignal = CancellationSignal()
        fingerPrintManager.authenticate(cryptoObject, cancellationSignal, 0, this, null)
        this.callback = callback
    }

    override fun onAuthenticationFailed() {
        super.onAuthenticationFailed()
        callback("Fingerprint not recognized. Try again",false)
    }

    override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult?) {
        super.onAuthenticationSucceeded(result)
        callback("Fingerprint recognized",true)
    }
}