package com.rodiz.arch2.feature.login.presentation.googlesignin

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.rodiz.arch2.core.common.result.Try
import com.rodiz.arch2.feature.login.domain.model.AuthError

/**
 * Runs the Credential Manager / "Sign in with Google" flow and returns the Google
 * ID token on success. The caller hands the token to SignInWithGoogleUseCase.
 *
 * Why a top-level object (not Hilt-injected): Credential Manager needs an Activity
 * to render its bottom sheet, which we get from LocalContext at the Composable
 * layer — there's no value in putting this in the DI graph.
 */
object GoogleSignInLauncher {

    suspend fun launch(
        activity: Activity,
        serverClientId: String,
    ): Try<String, AuthError> = try {
        val manager = CredentialManager.create(activity)
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setServerClientId(serverClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(true)
                    .build(),
            )
            .build()
        val response = manager.getCredential(activity, request)
        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            Try.Success(googleIdToken)
        } else {
            Try.Failure(AuthError.GoogleSignInFailed)
        }
    } catch (e: GetCredentialCancellationException) {
        Try.Failure(AuthError.GoogleSignInCancelled)
    } catch (e: NoCredentialException) {
        Try.Failure(AuthError.GoogleSignInFailed)
    } catch (e: GetCredentialException) {
        Try.Failure(AuthError.GoogleSignInFailed)
    } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
        Try.Failure(AuthError.GoogleSignInFailed)
    }
}
