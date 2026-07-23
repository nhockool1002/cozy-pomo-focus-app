package com.cozypomo.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cozypomo.app.ui.login.LoginScreen
import com.cozypomo.app.ui.onboarding.OnboardingScreen
import com.cozypomo.app.ui.splash.SplashDestination
import com.cozypomo.app.ui.splash.SplashScreen

private object RootRoute {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val MAIN = "main"
}

/**
 * Graph cấp cao nhất của app: Splash (S-00) quyết định vào Onboarding (S-00b, lần đầu),
 * Login (T-041, chưa có JWT) hay thẳng Main (4 tab bottom nav, [CozyPomoNavHost]).
 */
@Composable
fun RootNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = RootRoute.SPLASH) {
        composable(RootRoute.SPLASH) {
            SplashScreen(
                onNavigate = { destination ->
                    val route = when (destination) {
                        SplashDestination.Onboarding -> RootRoute.ONBOARDING
                        SplashDestination.Login -> RootRoute.LOGIN
                        SplashDestination.Main -> RootRoute.MAIN
                    }
                    navController.navigate(route) {
                        popUpTo(RootRoute.SPLASH) { inclusive = true }
                    }
                },
            )
        }
        composable(RootRoute.ONBOARDING) {
            OnboardingScreen(
                onDone = {
                    navController.navigate(RootRoute.LOGIN) {
                        popUpTo(RootRoute.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(RootRoute.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(RootRoute.MAIN) {
                        popUpTo(RootRoute.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(RootRoute.MAIN) {
            CozyPomoNavHost(
                onLogout = {
                    navController.navigate(RootRoute.LOGIN) {
                        popUpTo(RootRoute.MAIN) { inclusive = true }
                    }
                },
            )
        }
    }
}
