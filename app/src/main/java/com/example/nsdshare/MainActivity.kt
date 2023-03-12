package com.example.nsdshare

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.ethan.nsdshare.CustomLifeCycleObserver
import com.example.ftpm3.ui.theme.NsdShareTheme
import com.example.nsdshare.Navigation.SetupNavGraph

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NsdShareTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val contentResolver = contentResolver
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