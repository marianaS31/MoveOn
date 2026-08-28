package pt.ipp.estg.moveon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import pt.ipp.estg.moveon.ui.screens.ActivityDetailScreen
import pt.ipp.estg.moveon.ui.screens.Login
import pt.ipp.estg.moveon.ui.screens.MainScreen
import pt.ipp.estg.moveon.ui.theme.MoveOnTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        val startDestination = if (user != null) "main" else "login"

        enableEdgeToEdge()
        setContent {
            MoveOnTheme {
                AppNavigation(startDestination = startDestination)
            }
        }
    }
}

@Composable
fun AppNavigation(startDestination: String="login") {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {

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
                },
                onHistoryItemClick = { activityId ->
                    // Quando clicam num item do histórico, vamos para os detalhes
                    navController.navigate("detail_screen/$activityId")
                }
            )
        }

        composable(
            route = "detail_screen/{activityId}",
            arguments = listOf(navArgument("activityId") { type = NavType.LongType })
        ) { backStackEntry ->

            val id = backStackEntry.arguments?.getLong("activityId") ?: -1L

            ActivityDetailScreen(
                activityId = id,
                onBack = { navController.popBackStack() } // Botão voltar
            )
        }

    }
}