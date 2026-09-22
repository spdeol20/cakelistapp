package com.example.cakelistapp.ui.cakes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.cakelistapp.R
import com.example.cakelistapp.di.AppContainer
import com.example.cakelistapp.domain.model.Cake
import com.example.cakelistapp.ui.animation.entranceAnimation
import com.example.cakelistapp.ui.animation.rememberEntranceAnimationState
import com.example.cakelistapp.ui.theme.CakelistappTheme

private val THUMBNAIL_SIZE = 56.dp

@Composable
fun CakeListRoute(
    modifier: Modifier = Modifier,
) {
    val loadErrorMessage = stringResource(R.string.cakes_load_error)
    val viewModel: CakeListViewModel = viewModel(
        factory = CakeListViewModel.factory(
            repository = AppContainer.cakeRepository,
            loadErrorMessage = loadErrorMessage,
        ),
    )
    CakeListScreen(viewModel = viewModel, modifier = modifier)
}

@Composable
fun CakeListScreen(
    viewModel: CakeListViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CakeListContent(
        state = state,
        onRetry = { viewModel.loadCakes() },
        onRefresh = { viewModel.loadCakes(isRefresh = true) },
        onCakeClick = viewModel::onCakeSelected,
        onDialogDismiss = viewModel::onDialogDismissed,
        onErrorShown = viewModel::errorMessageShown,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CakeListContent(
    state: CakeListUiState,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onCakeClick: (Cake) -> Unit,
    onDialogDismiss: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val entranceState = rememberEntranceAnimationState()

    LaunchedEffect(state.errorMessage, state.cakes.isNotEmpty()) {
        val message = state.errorMessage ?: return@LaunchedEffect
        if (state.cakes.isNotEmpty()) {
            snackbarHostState.showSnackbar(message)
            onErrorShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.cakes_title)) },
                actions = {
                    IconButton(
                        onClick = onRefresh,
                        // A load is already in flight, and re-entering would cancel and restart it.
                        enabled = !state.isInitialLoading && !state.isRefreshing,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.cakes_refresh),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val errorMessage = state.errorMessage
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.isInitialLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.cakes.isEmpty() && errorMessage != null -> {
                    ErrorState(
                        message = errorMessage,
                        onRetry = onRetry,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                else -> {
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (state.cakes.isEmpty()) {
                            Text(
                                text = stringResource(R.string.cakes_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                itemsIndexed(
                                    items = state.cakes,
                                    key = { _, cake -> cake.title },
                                ) { index, cake ->
                                    Column(
                                        modifier = Modifier.entranceAnimation(
                                            itemKey = cake.title,
                                            state = entranceState,
                                            index = index,
                                        ),
                                    ) {
                                        CakeRow(
                                            cake = cake,
                                            onClick = { onCakeClick(cake) },
                                        )
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // TODO: Promote the description popup to a detail screen with Navigation Compose once there
    //  is more than one field to show; the dialog is deliberate for a single paragraph.
    state.selectedCake?.let { cake ->
        AlertDialog(
            onDismissRequest = onDialogDismiss,
            title = { Text(text = cake.title) },
            text = { Text(text = cake.description) },
            confirmButton = {
                TextButton(onClick = onDialogDismiss) {
                    Text(text = stringResource(R.string.cakes_dismiss))
                }
            },
        )
    }
}

@Composable
private fun CakeRow(
    cake: Cake,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(
                text = cake.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            if (cake.description.isNotBlank()) {
                Text(
                    text = cake.description,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        leadingContent = { CakeThumbnail(cake = cake) },
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

@Composable
private fun CakeThumbnail(
    cake: Cake,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.cakes_image_content_description, cake.title)

    // Keyed on the URL so a recycled row does not inherit the previous cake's failure.
    var hasError by remember(cake.imageUrl) { mutableStateOf(false) }

    Box(
        modifier = modifier
            // AsyncImage derives its decode size from these constraints. Without a bounded size
            // Coil decodes at source resolution, and some of these images are 3000x2000, which is
            // a 24MB bitmap for a 56dp icon and evicts the whole memory cache.
            .size(THUMBNAIL_SIZE)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        // The broken image icon is drawn over the request rather than replacing it, so a failed
        // load is not torn out of composition and can still resolve if it later succeeds.
        AsyncImage(
            model = cake.imageUrl.ifBlank { null },
            contentDescription = description,
            contentScale = ContentScale.Crop,
            onSuccess = { hasError = false },
            onError = { hasError = true },
            modifier = Modifier.fillMaxSize(),
        )
        // TODO: Allow tapping a failed thumbnail to retry that single image request.
        if (hasError) {
            Icon(
                imageVector = Icons.Filled.BrokenImage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
        )
        TextButton(onClick = onRetry) {
            Text(text = stringResource(R.string.cakes_retry))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CakeListContentPreview() {
    CakelistappTheme {
        CakeListContent(
            state = CakeListUiState(
                cakes = listOf(
                    Cake(
                        title = "Victoria Sponge",
                        description = "sponge with jam",
                        imageUrl = "",
                    ),
                    Cake(
                        title = "Carrot Cake",
                        description = "Bugs bunnys favourite",
                        imageUrl = "",
                    ),
                ),
                isInitialLoading = false,
            ),
            onRetry = {},
            onRefresh = {},
            onCakeClick = {},
            onDialogDismiss = {},
            onErrorShown = {},
        )
    }
}
