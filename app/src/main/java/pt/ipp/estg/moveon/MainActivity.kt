package pt.ipp.estg.moveon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pt.ipp.estg.moveon.ui.screens.Login
import pt.ipp.estg.moveon.ui.screens.MainScreen
import pt.ipp.estg.moveon.ui.theme.MoveOnTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoveOnTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {

        // Rota 1: Login
        composable("login") {
            Login(
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // Rota 2: Ecrã Principal
        composable("main") {
            MainScreen(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0)
                    }
                }
            )
        }
    }
}