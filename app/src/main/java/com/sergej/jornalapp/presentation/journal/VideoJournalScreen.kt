package com.sergej.jornalapp.presentation.journal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.videoFrameMillis
import com.sergej.jornalapp.domain.model.VideoJournalEntry
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoJournalScreen(
    viewModel: VideoJournalViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showRecordDialog by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteEntry by remember { mutableStateOf<VideoJournalEntry?>(null) }

    val captureVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo(),
    ) { success ->
        viewModel.onCaptureCompleted(success)
    }

    val requestCaptureWithPermissions = rememberCapturePermissionRequester(
        onPermissionsGranted = viewModel::onRecordRequested,
        onPermissionDenied = { message ->
            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
        },
    )

    val pagerState = rememberPagerState(pageCount = { uiState.entries.size })
    val exoPlayer = rememberSharedPlayer()
    var isCurrentVideoPlaying by rememberSaveable { mutableStateOf(false) }
    val canRecord = !uiState.isSaving && !uiState.isDeleting && uiState.pendingCaptureUri == null

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is VideoJournalEvent.LaunchCapture -> captureVideoLauncher.launch(event.uri.toUri())
                is VideoJournalEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(pagerState.currentPage, uiState.entries) {
        val currentEntry = uiState.entries.getOrNull(pagerState.currentPage)
        if (currentEntry == null) {
            exoPlayer.stop()
            return@LaunchedEffect
        }

        val mediaUri = Uri.fromFile(File(currentEntry.filePath))
        val loadedUri = exoPlayer.currentMediaItem?.localConfiguration?.uri
        if (loadedUri != mediaUri) {
            exoPlayer.setMediaItem(MediaItem.fromUri(mediaUri))
            exoPlayer.prepare()
        }

        isCurrentVideoPlaying = false
        exoPlayer.playWhenReady = false
    }

    LaunchedEffect(isCurrentVideoPlaying) {
        exoPlayer.playWhenReady = isCurrentVideoPlaying
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Mini Video Journal",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (canRecord) {
                        showRecordDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .alpha(if (canRecord) 1f else 0.6f),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Record clip",
                )
            }
        },
    ) { innerPadding ->
        if (uiState.entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No clips yet. Tap + to record your first journal entry.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) { page ->
                val entry = uiState.entries[page]
                val isActivePage = page == pagerState.currentPage

                VideoJournalPagerPage(
                    entry = entry,
                    isActivePage = isActivePage,
                    isPlaying = isCurrentVideoPlaying,
                    player = exoPlayer,
                    onTogglePlayback = { isCurrentVideoPlaying = !isCurrentVideoPlaying },
                    onDeleteRequested = { pendingDeleteEntry = entry },
                    isDeleteEnabled = !uiState.isDeleting,
                )
            }
        }
    }

    if (showRecordDialog) {
        RecordDescriptionDialog(
            description = uiState.descriptionDraft,
            onDescriptionChanged = viewModel::onDescriptionChanged,
            onDismiss = { showRecordDialog = false },
            onConfirm = {
                showRecordDialog = false
                requestCaptureWithPermissions()
            },
        )
    }

    if (pendingDeleteEntry != null) {
        DeleteEntryDialog(
            onDismiss = { pendingDeleteEntry = null },
            onConfirm = {
                pendingDeleteEntry?.let(viewModel::onDeleteRequested)
                pendingDeleteEntry = null
                isCurrentVideoPlaying = false
            },
        )
    }
}

@Composable
private fun VideoJournalPagerPage(
    entry: VideoJournalEntry,
    isActivePage: Boolean,
    isPlaying: Boolean,
    player: ExoPlayer,
    onTogglePlayback: () -> Unit,
    onDeleteRequested: () -> Unit,
    isDeleteEnabled: Boolean,
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(enabled = isActivePage, onClick = onTogglePlayback),
        ) {
            if (isActivePage) {
                SharedInlineVideoPlayer(
                    player = player,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                VideoThumbnail(filePath = entry.filePath)
            }

            if (isActivePage) {
                IconButton(
                    onClick = onTogglePlayback,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(
                            color = Color.Black.copy(alpha = 0.32f),
                            shape = MaterialTheme.shapes.extraLarge,
                        )
                        .padding(8.dp),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                    )
                }

                IconButton(
                    onClick = onDeleteRequested,
                    enabled = isDeleteEnabled,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.32f),
                            shape = MaterialTheme.shapes.extraLarge,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete entry",
                        tint = Color.White,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(12.dp),
            ) {
                VideoMeta(entry = entry)
            }
        }
    }
}

@Composable
private fun VideoMeta(entry: VideoJournalEntry) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (!entry.description.isNullOrBlank()) {
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = formatTimestamp(entry.createdAtEpochMs),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun SharedInlineVideoPlayer(
    player: ExoPlayer,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { playerContext ->
            PlayerView(playerContext).apply {
                this.player = player
                useController = false
            }
        },
        update = { playerView ->
            playerView.player = player
        },
    )
}

@Composable
private fun VideoThumbnail(filePath: String) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(File(filePath))
                .videoFrameMillis(500)
                .build(),
            contentDescription = "Video thumbnail",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.22f)),
        )

        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Play video",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .height(56.dp),
        )
    }
}

@Composable
private fun RecordDescriptionDialog(
    description: String,
    onDescriptionChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record New Clip") },
        text = {
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Description (optional)") },
                maxLines = 4,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun DeleteEntryDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Entry") },
        text = { Text("This clip will be removed from your journal and deleted from device storage.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun rememberSharedPlayer(): ExoPlayer {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    return exoPlayer
}

@Composable
private fun rememberCapturePermissionRequester(
    onPermissionsGranted: () -> Unit,
    onPermissionDenied: (String) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val requiredPermissions = remember { requiredCapturePermissions() }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val deniedPermissions = requiredPermissions.filter { permission ->
            val wasGrantedByLauncher = results[permission] == true
            val isGrantedNow = ContextCompat.checkSelfPermission(
                context,
                permission,
            ) == PackageManager.PERMISSION_GRANTED
            !wasGrantedByLauncher && !isGrantedNow
        }

        if (deniedPermissions.isEmpty()) {
            onPermissionsGranted()
            return@rememberLauncherForActivityResult
        }

        val permanentlyDenied = activity != null && deniedPermissions.any { permission ->
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }

        val message = if (permanentlyDenied) {
            "Permission denied permanently. Enable camera access in Settings."
        } else {
            "Camera permission is required to record videos."
        }
        onPermissionDenied(message)
    }

    return {
        val granted = hasCapturePermissions(context, requiredPermissions)
        if (granted) {
            onPermissionsGranted()
        } else {
            permissionsLauncher.launch(requiredPermissions.toTypedArray())
        }
    }
}

private fun requiredCapturePermissions(): List<String> {
    val permissions = mutableListOf(Manifest.permission.CAMERA)
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        permissions += Manifest.permission.WRITE_EXTERNAL_STORAGE
    }
    return permissions
}

private fun hasCapturePermissions(context: Context, permissions: List<String>): Boolean {
    return permissions.all { permission ->
        ContextCompat.checkSelfPermission(
            context,
            permission,
        ) == PackageManager.PERMISSION_GRANTED
    }
}

private fun formatTimestamp(epochMs: Long): String {
    return DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM,
        DateFormat.SHORT,
    ).format(Date(epochMs))
}
