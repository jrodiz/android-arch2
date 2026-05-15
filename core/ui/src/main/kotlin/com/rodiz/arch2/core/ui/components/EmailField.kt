package com.rodiz.arch2.core.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun EmailField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    BrandTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        leadingIcon = Icons.Outlined.MailOutline,
        errorMessage = errorMessage,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
        ),
        modifier = modifier,
        fieldModifier = Modifier.testTag("email_field"),
    )
}
