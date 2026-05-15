package com.rodiz.arch2.feature.login.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rodiz.arch2.feature.login.presentation.R

@Composable
fun ForgotPasswordStubScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.forgot_stub_title))
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onBack) { Text(stringResource(R.string.common_back)) }
    }
}

@Composable
fun CreateAccountStubScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.create_account_stub_title))
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onBack) { Text(stringResource(R.string.common_back)) }
    }
}
