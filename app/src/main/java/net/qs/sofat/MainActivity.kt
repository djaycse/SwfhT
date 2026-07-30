package net.qs.sofat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import net.qs.sofat.ui.theme.SOFATTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SOFATTheme(dynamicColor = false) {
                SOFATApp()
            }
        }
    }
}
