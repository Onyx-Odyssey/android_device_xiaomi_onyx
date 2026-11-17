package org.lineageos.settings

import android.content.Context
import androidx.activity.ComponentActivity

// Compose needs the credential-protected context; the app's device-protected default lacks ClipboardManager.
abstract class PartsComposeActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.createCredentialProtectedStorageContext())
    }
}
