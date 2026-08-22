package zaaaam.siabsen.com

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import zaaaam.siabsen.com.ui.navigation.RootNavHost
import zaaaam.siabsen.com.ui.theme.SiabsenTheme

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SiabsenTheme {
                RootNavHost()
            }
        }
    }
}
