package sk.jaja.callblocker

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import sk.jaja.callblocker.ui.theme.JajaBlockerTheme // <--- Správny import novej témy

class MainActivity : ComponentActivity() {

    private val startForRoleResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Jaja Blocker bol úspešne aktivovaný!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Pre fungovanie aplikácie je potrebné schváliť rolu.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JajaBlockerTheme { // <--- Správny názov vašej vygenerovanej témy
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Jaja Blocker beží na pozadí.")
                    }
                }

                // Spustenie požiadavky na rolu musí byť vnútri Composable stromu
                LaunchedEffect(Unit) {
                    requestCallScreeningRole()
                }
            }
        }
    }

    private fun requestCallScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                startForRoleResult.launch(intent)
            }
        }
    }
}