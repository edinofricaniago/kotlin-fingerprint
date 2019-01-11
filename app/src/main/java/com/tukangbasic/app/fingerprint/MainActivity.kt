package com.tukangbasic.app.fingerprint

import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnShowDialog.click{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val fingerPrintManager = applicationContext.getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager
                if (!fingerPrintManager.isHardwareDetected || !fingerPrintManager.hasEnrolledFingerprints()) {
                    Toast.makeText(applicationContext,"Hardware not detected", Toast.LENGTH_SHORT).show()
                }else{
                    FingerPrintDialog(SettingActivity@ this)
                            .setCallback(
                                    onYesClicked = {},
                                    onNoClicked = {},
                                    onCancelClicked = {}
                            )
                            .build().show()
                }
            }else{
                Toast.makeText(applicationContext,"Need Android minimum version Marshmellow for this feature", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
