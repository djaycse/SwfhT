package net.qs.swfht

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import net.qs.swfht.ui.theme.SWFHTTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SWFHTTheme(dynamicColor = false) {
                SWFHTApp()
            }
        }
    }
}
