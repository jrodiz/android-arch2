package com.rodiz.arch2.feature.login.presentation.screen

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodiz.arch2.feature.login.presentation.R
import com.rodiz.arch2.feature.login.presentation.state.SignUpAction
import com.rodiz.arch2.feature.login.presentation.state.SignUpEvent
import com.rodiz.arch2.feature.login.presentation.state.SoftWarning
import com.rodiz.arch2.feature.login.presentation.viewmodel.SignUpViewModel
import java.io.File

@Composable
fun SignUpRoute(
    onNavigateHome: () -> Unit,
    onNavigateBack: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val pendingCameraUri = remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = PickVisualMedia(),
    ) { uri ->
        if (uri != null) viewModel.onAction(SignUpAction.AvatarSelected(uri.toString()))
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCameraUri.value
        if (success && uri != null) {
            viewModel.onAction(SignUpAction.AvatarSelected(uri.toString()))
        }
        pendingCameraUri.value = null
    }

    val avatarUploadFailedMsg = stringResource(R.string.signup_warning_avatar_upload_failed)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SignUpEvent.NavigateHome -> onNavigateHome()
                SignUpEvent.NavigateBack -> onNavigateBack()
                SignUpEvent.LaunchGalleryPicker ->
                    galleryLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                SignUpEvent.LaunchCamera -> {
                    val target = createTempAvatarUri(context)
                    pendingCameraUri.value = target
                    cameraLauncher.launch(target)
                }
                is SignUpEvent.ShowSoftWarning -> when (event.warning) {
                    SoftWarning.AvatarUploadFailed ->
                        Toast.makeText(context, avatarUploadFailedMsg, Toast.LENGTH_LONG).show()
                }
                is SignUpEvent.ShowComingSoon ->
                    snackbarHostState.showSnackbar(context.getString(event.resId))
                SignUpEvent.OpenTerms -> onOpenTerms()
                SignUpEvent.OpenPrivacyPolicy -> onOpenPrivacyPolicy()
            }
        }
    }

    SignUpScreen(
        state = state,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
    )
}

private fun createTempAvatarUri(context: Context): Uri {
    val dir = File(context.cacheDir, "avatars").apply { mkdirs() }
    val file = File(dir, "cap-${System.currentTimeMillis()}.jpg")
    file.createNewFile()
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
}
