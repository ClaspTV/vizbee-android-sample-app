package tv.vizbee.demo.compose

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import tv.vizbee.demo.model.Video
import tv.vizbee.demo.model.VideoItem

class RemoteButtonIntegrationActivity : AppCompatActivity() {
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

                    // Example 4: RemoteButton in AndroidView with click override
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "AndroidView Example with RemoteButton click override:",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val startPosition = 0
                        VizbeeRemoteButton(
                            modifier = Modifier.size(48.dp),
                            overrideClick = true,
                            onButtonClick = { remoteButton ->
                                remoteButton.click(VideoItem(), startPosition.toLong())
                            }
                        )
                    }

                    // Add a spacer with weight to push CastBarComposable to bottom
                    Spacer(modifier = Modifier.weight(1f))

                    CastBarComposable(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        supportFragmentManager
                    )
                }
            }
        }
    }
}