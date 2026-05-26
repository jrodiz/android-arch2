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
        // Two distinct paths surface here:
        //   1. The user dismissed the picker → genuine cancel.
        //   2. GMS aborted because there's no eligible Google account on the
        //      device (status code 28433); current Credential Manager versions
        //      route this through cancellation rather than NoCredentialException.
        // Disambiguate by sniffing the wrapped status code in the message — fragile
        // (the string isn't part of any public API), but the alternative is a
        // generic "cancelled" toast for a real configuration issue.
        if (e.isNoGoogleAccountSignal()) {
            Try.Failure(AuthError.GoogleNoAccount)
        } else {
            Try.Failure(AuthError.GoogleSignInCancelled)
        }
    } catch (e: NoCredentialException) {
        Try.Failure(AuthError.GoogleNoAccount)
    } catch (e: GetCredentialException) {
        Try.Failure(AuthError.GoogleSignInFailed)
    } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
        Try.Failure(AuthError.GoogleSignInFailed)
    }

    /**
     * Heuristic: GMS's "no eligible credential" outcome surfaces inside a
     * GetCredentialCancellationException whose message embeds the GoogleIdService
     * status code `28433`. Match the digits rather than the surrounding `chuk:[…]`
     * formatting so this still matches if Google reformats the message later.
     */
    private fun GetCredentialCancellationException.isNoGoogleAccountSignal(): Boolean {
        val haystack = (message.orEmpty() + " " + (cause?.message.orEmpty()))
        return "28433" in haystack
    }
}
