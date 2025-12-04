package kr.sweetapps.alcoholictimer.ui.tab_05.screens.debug

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kr.sweetapps.alcoholictimer.data.source.remote.AdVerifier

@Composable
fun DebugAdsScreen() {
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    var results by remember { mutableStateOf<List<kr.sweetapps.alcoholictimer.data.source.remote.AdVerifier.Result>>(emptyList()) }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Ad system verifier (debug)", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = {
            AdVerifier.runChecks(activity) { list -> results = list
            }
        }) { Text("Run verification") }
        Spacer(modifier = Modifier.height(16.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            for (r in results) {
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = r.name + if (r.ok) " ✓" else " ✗", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = r.message, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}


