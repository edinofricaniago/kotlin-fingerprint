package com.tukangbasic.app.fingerprint

import android.view.View


fun View.click(listener: (View) -> Unit) {
    setOnClickListener(listener)
}
