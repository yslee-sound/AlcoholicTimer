package kr.sweetapps.alcoholictimer.core.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kr.sweetapps.alcoholictimer.ui.theme.AlcoholicTimerTheme
import kr.sweetapps.alcoholictimer.navigation.Screen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavHostController
import kr.sweetapps.alcoholictimer.core.ui.components.BottomNavBar
import kotlinx.coroutines.delay

/**
 * Common Scaffold: Top banner ad + center content + bottom navigation
 */
@Composable
fun BaseScaffold(
    navController: NavHostController,
    contentBackground: Color? = null,
    content: @Composable () -> Unit
) {
    // Subscribe to interstitial ad showing state
    val isInterstitialShowing = kr.sweetapps.alcoholictimer.ui.ad.AdController.isInterstitialShowingState()

    LaunchedEffect(isInterstitialShowing) {
        Log.d("BaseScaffold", "Ad isInterstitialShowing changed: $isInterstitialShowing")
    }

    // Short overlay delay after interstitial closes (for animation)
    var overlayHoldActive by remember { mutableStateOf(false) }
    LaunchedEffect(isInterstitialShowing) {
        if (!isInterstitialShowing) {
            overlayHoldActive = true
            delay(120)
            overlayHoldActive = false
        }
    }
    val overlayVisible = isInterstitialShowing || overlayHoldActive

    AlcoholicTimerTheme(darkTheme = false, applySystemBars = true) {
        // Observe nav back stack to allow per-route content background overrides (e.g., Run screen)
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        Box(modifier = Modifier.fillMaxSize()) {
            // Base UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // [REMOVED] Banner ad removed (2025-12-01)
                // Previous code:
                // AdmobBanner(modifier = Modifier.fillMaxWidth(), reserveSpaceWhenDisabled = true)
                // HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)

                // Center content
                val effectiveBg = when (currentRoute) {
                    Screen.Run.route -> Color(0xFFEEEDE9)
                    else -> contentBackground ?: Color.White
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(effectiveBg)
                ) {
                    content()
                }

                // Bottom divider
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)

                // Bottom Navigation (with padding to avoid overlap with system navigation buttons)
                Surface(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    color = Color.White,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp
                ) {
                    BottomNavBar(navController = navController)
                }
            }

            // Interstitial ad showing or just after close: black overlay (no animation)
            if (overlayVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
        }
    }
}
