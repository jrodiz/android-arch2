package com.rodiz.arch2.core.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun EmailFieldPill(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    errorMessage: String?,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    shadowElevation: Dp = 0.dp,
) {
    FilledPillTextField(
        value = value,
        onValueChange = onValueChange,
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
        fieldModifier = Modifier.testTag("email_field_pill"),
        containerColor = containerColor,
        shadowElevation = shadowElevation,
    )
}
