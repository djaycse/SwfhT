package net.qs.inoffice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import net.qs.inoffice.ui.theme.InOfficeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            InOfficeTheme(dynamicColor = false) {
                InOfficeApp()
            }
        }
    }
}
