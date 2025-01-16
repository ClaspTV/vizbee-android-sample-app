package tv.vizbee.demo.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class RemoteButtonIntegrationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    // Example 1: RemoteButton on TopAppBar
                    AppTopBar(
                        title = "TopAppBar Example",
                        onNavigationClick = { finish() }
                    )

                    // Example 2: RemoteButton on Custom Toolbar
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Custom Toolbar Example:",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    CustomToolbar(
                        title = "Custom Toolbar",
                        onBackPressed = { finish() }
                    )

                    // Example 3: RemoteButton in AndroidView
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "AndroidView Example:",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        VizbeeRemoteButton(
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }
    }
}