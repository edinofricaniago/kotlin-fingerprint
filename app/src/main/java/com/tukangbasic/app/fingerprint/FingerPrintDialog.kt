package com.tukangbasic.app.fingerprint

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Context.FINGERPRINT_SERVICE
import android.content.Context.KEYGUARD_SERVICE
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.annotation.TargetApi
import android.security.keystore.KeyGenParameterSpec
import android.widget.Button
import android.widget.Toast
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey


class FingerPrintDialog(private val context: Context) {
    private var onYesClicked: () -> Unit = {}
    private var onNoClicked: () -> Unit = {}
    private var onCancelClicked: () -> Unit = {}

    private lateinit var btnYes:Button
    private lateinit var btnNo:Button

    private lateinit var keyStore: KeyStore
    private lateinit var cipher: Cipher
    private var KEY_NAME = "AndroidKey"

    fun setCallback(onYesClicked: () -> Unit = {}, onNoClicked: () -> Unit = {}, onCancelClicked: () -> Unit = {}): FingerPrintDialog {
        this.onYesClicked = onYesClicked
        this.onNoClicked = onNoClicked
        this.onCancelClicked = onCancelClicked
        return this
    }

    fun build(): AlertDialog {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Fingerprint")
        builder.setMessage("Confirm fingerprint to continue")

        val layoutInflater = LayoutInflater.from(context).inflate(R.layout.dialog_setting_fingerprint, null)
        builder.setView(layoutInflater)

        builder.setPositiveButton("Yes") { dialog, which ->
            dialog.dismiss()
            onYesClicked()
        }
        builder.setNegativeButton("No") { dialog, which ->
            dialog.dismiss()
            onNoClicked()
        }
        builder.setNeutralButton("Cancel") { dialog, which ->
            dialog.dismiss()
            onCancelClicked
        }
        val ivFingerPrintIcon = layoutInflater.findViewById<ImageView>(R.id.ivFingerPrintIcon)
        val tvFingerPrintMessage = layoutInflater.findViewById<TextView>(R.id.tvFingerPrintMessage)

        val shape = ivFingerPrintIcon.background as GradientDrawable
        shape.setColor(ContextCompat.getColor(context, R.color.normal))
        ivFingerPrintIcon.background = shape
        val dialog = builder.create()

        dialog.setOnShowListener {
            btnYes = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btnNo = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            btnNo.isEnabled = false
            btnYes.isEnabled = false
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val fingerPrintManager = context.getSystemService(FINGERPRINT_SERVICE) as FingerprintManager
            val keyguardManager = context.getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            if (!fingerPrintManager.isHardwareDetected) {
                // hardware not detected
                dialog.dismiss()
                Toast.makeText(context, "Hardware not detected", Toast.LENGTH_SHORT).show()
            } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                // permission ungranted
                tvFingerPrintMessage.text = "Permission is not granted"
            } else if (!keyguardManager.isKeyguardSecure) {
                // add key guard to your phone
                tvFingerPrintMessage.text = "Please add your phone Key guard"
            } else if (!fingerPrintManager.hasEnrolledFingerprints()) {
                // you sould add atleast 1 fingerprint to use this feature
                tvFingerPrintMessage.text = "You should add atleast 1 fingerprint to use this feature"
            } else {
                // place your finger on scanner
                val fingerprintHandler = FingerprintHandler(context)
                fingerprintHandler.auth(fingerPrintManager, null) { message: String, isSuccess: Boolean ->
                    tvFingerPrintMessage.text = message
                    if (isSuccess) {
                        ivFingerPrintIcon.setImageResource(R.drawable.ic_check)
                        tvFingerPrintMessage.setTextColor(ContextCompat.getColor(context, R.color.success))
                        shape.setColor(ContextCompat.getColor(context, R.color.success))
                        btnNo.isEnabled = true
                        btnYes.isEnabled = true
                    } else {
                        ivFingerPrintIcon.setImageResource(R.drawable.ic_priority_high)
                        tvFingerPrintMessage.setTextColor(ContextCompat.getColor(context, R.color.failure))
                        shape.setColor(ContextCompat.getColor(context, R.color.failure))
                    }
                    ivFingerPrintIcon.background = shape

                }
            }
        }
        return dialog
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun generateKey() {

        try {

            keyStore = KeyStore.getInstance("AndroidKeyStore")
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")

            keyStore.load(null)
            keyGenerator.init(KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                            KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build())
            keyGenerator.generateKey()

        } catch (e: KeyStoreException) {

            e.printStackTrace()

        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: CertificateException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: InvalidAlgorithmParameterException) {
            e.printStackTrace()
        } catch (e: NoSuchProviderException) {
            e.printStackTrace()
        }

    }

    @TargetApi(Build.VERSION_CODES.M)
    fun cipherInit(): Boolean {
        try {
            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to get Cipher", e)
        } catch (e: NoSuchPaddingException) {
            throw RuntimeException("Failed to get Cipher", e)
        }


        try {

            keyStore.load(
                    null)

            val key = keyStore.getKey(KEY_NAME, null) as SecretKey

            cipher.init(Cipher.ENCRYPT_MODE, key)

            return true

        } catch (e: KeyPermanentlyInvalidatedException) {
            return false
        } catch (e: KeyStoreException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: CertificateException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: UnrecoverableKeyException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: IOException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: InvalidKeyException) {
            throw RuntimeException("Failed to init Cipher", e)
        }

    }
}