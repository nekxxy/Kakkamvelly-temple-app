package page.kakkamvellytemple.app.presentation.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun NetworkImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
)
