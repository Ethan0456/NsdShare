package com.example.nsdshare

import android.content.ContentResolver
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.ethan.nsdshare.CustomLifeCycleObserver
import com.example.nsdshare.Navigation.SetupNavGraph
import com.example.nsdshare.ui.theme.NsdShareTheme
import java.util.jar.Manifest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NsdShareTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val contentResolver = getContentResolver()
                    val nsdShareViewModel = NsdShareViewModel(this, contentResolver)
                    val navHostController = rememberNavController()
                    lifecycle.addObserver(CustomLifeCycleObserver(this, nsdShareViewModel))

                    SetupNavGraph(nsdShareViewModel = nsdShareViewModel, navHostController = navHostController)
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    NsdShareTheme {
        Greeting("Android")
    }
}