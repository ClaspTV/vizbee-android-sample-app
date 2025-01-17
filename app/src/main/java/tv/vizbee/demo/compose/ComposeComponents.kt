package tv.vizbee.demo.compose

// ComposeComponents.kt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import tv.vizbee.api.RemoteButton

@Composable
fun VizbeeRemoteButton(
    modifier: Modifier = Modifier,
    overrideClick: Boolean = false,
    onButtonClick: ((RemoteButton) -> Unit) = {}
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            RemoteButton(context).apply {
                // Only set custom click listener if overrideClick is true
                if (overrideClick) {
                    setOnClickListener { onButtonClick(this) }
                }
            }
        },
        update = { view ->
            // Only update click listener if overrideClick is true
            if (overrideClick) {
                view.setOnClickListener(null)
                view.setOnClickListener { onButtonClick(view) }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onNavigationClick: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                color = Color.White
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigationClick) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        },
        actions = {
            VizbeeRemoteButton(
                modifier = Modifier
                    .size(48.dp)
                    .padding(8.dp)
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Black,
            navigationIconContentColor = Color.White
        )
    )
}

@Composable
fun CustomToolbar(
    title: String,
    showBackButton: Boolean = true,
    onBackPressed: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color.Black)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBackButton) {
            IconButton(onClick = onBackPressed) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )

        VizbeeRemoteButton(
            modifier = Modifier.size(48.dp)
        )
    }
}