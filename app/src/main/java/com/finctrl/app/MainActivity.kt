package com.finctrl.app

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.finctrl.app.ui.AppNav
import com.finctrl.app.ui.theme.FinctrlTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = false
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false

        setContent { FinctrlTheme { AppNav() } }
    }
}