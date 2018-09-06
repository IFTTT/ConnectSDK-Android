package com.ifttt.api.demo

import android.app.Activity
import android.view.Window
import com.google.android.material.snackbar.Snackbar

fun Activity.showSnackbar(charSequence: CharSequence) {
    Snackbar.make(findViewById(Window.ID_ANDROID_CONTENT), charSequence, Snackbar.LENGTH_LONG).show()
}
