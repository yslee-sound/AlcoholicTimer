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
import kr.sweetapps.alcoholictimer.core.ui.theme.AlcoholicTimerTheme
import kr.sweetapps.alcoholictimer.navigation.Screen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavHostController
import kr.sweetapps.alcoholictimer.core.ui.components.BottomNavBar
import kotlinx.coroutines.delay

/**
 * ê³µí†µ ?¤ìº?´ë“œ: ?ë‹¨ ë°°ë„ˆ ê´‘ê³  + ì¤‘ì•™ ì½˜í…ì¸?+ ?˜ë‹¨ Bottom Navigation
 */
@Composable
fun BaseScaffold(
    navController: NavHostController,
    contentBackground: Color? = null,
    content: @Composable () -> Unit
) {
    // ?„ë©´ê´‘ê³  ?œì‹œ ?íƒœ êµ¬ë…
    val isInterstitialShowing = kr.sweetapps.alcoholictimer.ui.ad.AdController.isInterstitialShowingState()

    LaunchedEffect(isInterstitialShowing) {
        Log.d("BaseScaffold", "?¬ isInterstitialShowing changed: $isInterstitialShowing")
    }

    // ?„ë©´ê´‘ê³  ì¢…ë£Œ ??ì§§ì? ?€?œë¡œ ?œê°???¬ë¼?´ë“œë¥??„ì¶© (? ë‹ˆë©”ì´???œê±°)
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
            // ê¸°ë³¸ UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // [REMOVED] ë°°ë„ˆ ê´‘ê³  ?œê±° (2025-12-01)
                // ê¸°ì¡´ ì½”ë“œ:
                // AdmobBanner(modifier = Modifier.fillMaxWidth(), reserveSpaceWhenDisabled = true)
                // HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)

                // ì¤‘ì•™ ì½˜í…ì¸?
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

                // ?˜ë‹¨ êµ¬ë¶„??
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)

                // Bottom Navigation (?œìŠ¤ì²?3ë²„íŠ¼ê³?ê²¹ì¹˜ì§€ ?Šë„ë¡??¨ë”©)
                Surface(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    color = Color.White,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp
                ) {
                    BottomNavBar(navController = navController)
                }
            }

            // ?„ë©´ê´‘ê³  ?œì‹œ ì¤?ì¢…ë£Œ ì§í›„: ê²€?€ ?¤ë²„?ˆì´ë¡?ì¦‰ì‹œ ??¸° (? ë‹ˆë©”ì´???†ìŒ)
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
