package tv.vizbee.demo.compose

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class ComposeExamplesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    AppTopBar(
                        title = "Compose Examples",
                        onNavigationClick = { finish() }
                    )

                    Button(
                        onClick = {
                            startActivity(Intent(this@ComposeExamplesActivity,
                                RemoteButtonIntegrationActivity::class.java))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("RemoteButton Integration")
                    }

                    // Add a spacer with weight to push CastBarComposable to bottom
                    Spacer(modifier = Modifier.weight(1f))

                    CastBarComposable(
                        modifier = Modifier
                            .fillMaxWidth().height(56.dp),
                        fragmentManager= supportFragmentManager
                    )
                }
            }
        }
    }
}