package com.ascend.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ascend.app.ui.navigation.Routes
import com.ascend.app.ui.theme.AscendTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AscendTheme {
                AscendNavHost()
            }
        }
    }
}

@Composable
fun AscendNavHost(){
    val navController= rememberNavController()
    NavHost(
        navController=navController,
        startDestination = Routes.LOGIN
    ){
        composable(Routes.LOGIN){
            Text("Login Screen")
        }
        composable(Routes.DASHBOARD){
            Text("Dashboard Screen")
        }
        composable(Routes.HABIT_LIST){
            Text("Habit List Screen")
        }
    }
}