package tv.vizbee.demo.compose

// ComposeComponents.kt

import android.view.View
import androidx.appcompat.app.AppCompatActivity
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
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import tv.vizbee.api.CastBarFragment
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

/**
 * CastBarComposable: A composable wrapper for the CastBarFragment
 *
 * Use this wrapper to integrate the Cast Bar into any Compose layout.
 * It uses `FragmentContainerView` to host the `CastBarFragment`.
 *
 * @param modifier Modifier for customizing the layout, e.g., width and height.
 * @param fragmentManager An optional instance of FragmentManager to manage the fragment transaction.
 *                        If not provided, it will attempt to derive it from the provided context.
 */
@Composable
fun CastBarComposable(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight(),
    fragmentManager: FragmentManager? = null
) {
    AndroidView(
        factory = { context ->
            FragmentContainerView(context).apply {
                id = View.generateViewId()

                // Obtain FragmentManager from the provided context or parameter
                val fm = fragmentManager ?: (context as? AppCompatActivity)?.supportFragmentManager
                fm?.beginTransaction()
                    ?.replace(this.id, CastBarFragment())
                    ?.commit()
            }
        },
        modifier = modifier
    )
}