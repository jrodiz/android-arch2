package com.rodiz.arch2.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rodiz.arch2.core.designsystem.theme.BrandColors

/**
 * Filled pill-shaped text field used on the login redesign. Coral-derived container
 * (works in both light and dark mode), no indicator line, optional leading icon,
 * optional trailing icon, placeholder-only (no floating label — caller renders an
 * external label if needed).
 *
 * Supports multi-line via [singleLine] = false + [minLines]; when used multi-line,
 * pass a non-circular [shape] (`RoundedCornerShape(24.dp)` works well) so wrapped
 * text isn't clipped by the pill.
 */
@Composable
fun FilledPillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    errorMessage: String?,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    fieldModifier: Modifier = Modifier,
    /**
     * Overrides the default coral-peach container. Pass `Color.White` for the SignUp
     * redesign, leave null to keep Login's tinted look.
     */
    containerColor: Color? = null,
    /** Adds a subtle drop shadow under the pill — used by SignUp's white fields. */
    shadowElevation: Dp = 0.dp,
    /** Set to false for multi-line inputs (bio, paragraph). */
    singleLine: Boolean = true,
    /** Minimum visible lines when [singleLine] = false. */
    minLines: Int = 1,
    /**
     * Container shape. Defaults to [CircleShape] for the established pill look.
     * Override with `RoundedCornerShape(...)` when [singleLine] = false so wrapped
     * text doesn't get clipped by circular ends.
     */
    shape: Shape = CircleShape,
) {
    val container = containerColor ?: BrandColors.Coral.copy(alpha = 0.10f)
    val sizingModifier = if (singleLine) {
        Modifier.fillMaxWidth().height(56.dp)
    } else {
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        val field: @Composable () -> Unit = {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = placeholder,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                },
                leadingIcon = leadingIcon?.let { icon ->
                    {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                trailingIcon = trailingIcon,
                isError = errorMessage != null,
                singleLine = singleLine,
                minLines = minLines,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                shape = shape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = container,
                    unfocusedContainerColor = container,
                    disabledContainerColor = container,
                    errorContainerColor = container,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    errorCursorColor = MaterialTheme.colorScheme.error,
                ),
                modifier = fieldModifier.then(sizingModifier),
            )
        }
        if (shadowElevation > 0.dp) {
            // Wrap in a Surface so the pill casts a soft shadow without painting
            // its own background (the TextField container still does that).
            Surface(
                shape = shape,
                color = Color.Transparent,
                shadowElevation = shadowElevation,
                modifier = Modifier.fillMaxWidth(),
            ) { field() }
        } else {
            field()
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp, start = 16.dp),
            )
        }
    }
}
