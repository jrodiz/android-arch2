package com.rodiz.arch2.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.core.designsystem.theme.TinPetTheme
import com.rodiz.arch2.core.ui.components.FilledPillTextField
import com.rodiz.arch2.core.ui.components.PrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeleteAccountSheet(
    state: DeleteAccountSheetUiState,
    onTypedChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancelDeletion: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("delete_account_sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
        ) {
            if (state.pendingDeletion == null) {
                ConfirmContent(
                    state = state,
                    onTypedChanged = onTypedChanged,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss,
                )
            } else {
                GraceContent(
                    state = state,
                    onCancelDeletion = onCancelDeletion,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

// ---------- Confirm variant ----------

@Composable
private fun ConfirmContent(
    state: DeleteAccountSheetUiState,
    onTypedChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    HeaderTile(icon = Icons.Outlined.Block)
    Spacer(Modifier.height(16.dp))
    Headline(text = stringResource(R.string.delete_sheet_headline))
    Spacer(Modifier.height(8.dp))
    BodyWithBoldSpan(
        format = stringResource(R.string.delete_sheet_body_format),
        bold = stringResource(R.string.delete_sheet_body_30_days),
    )
    Spacer(Modifier.height(20.dp))
    SummaryCard(petNames = state.petNames, matchCount = state.matchCount)
    Spacer(Modifier.height(20.dp))
    Eyebrow(text = stringResource(R.string.delete_sheet_confirm_eyebrow))
    Spacer(Modifier.height(8.dp))
    FilledPillTextField(
        value = state.typed,
        onValueChange = onTypedChanged,
        placeholder = stringResource(R.string.delete_sheet_placeholder),
        errorMessage = null,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            imeAction = ImeAction.Done,
            autoCorrectEnabled = false,
        ),
        keyboardActions = KeyboardActions(onDone = { if (state.canSubmit) onConfirm() }),
        containerColor = BrandColors.PeachWarmLight.copy(alpha = 0.45f),
        shadowElevation = 0.dp,
        fieldModifier = Modifier.testTag("delete_account_typed_field"),
    )
    Spacer(Modifier.height(16.dp))
    PrimaryButton(
        text = stringResource(R.string.delete_account_cta),
        loading = state.isSubmitting,
        enabled = state.canSubmit,
        onClick = onConfirm,
        testTag = "delete_account_confirm_cta",
        containerColor = BrandColors.DangerRed,
    )
    Spacer(Modifier.height(4.dp))
    TextButton(
        onClick = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("delete_account_keep_cta"),
    ) {
        Text(
            text = stringResource(R.string.delete_sheet_keep),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ---------- Grace variant ----------

@Composable
private fun GraceContent(
    state: DeleteAccountSheetUiState,
    onCancelDeletion: () -> Unit,
    onDismiss: () -> Unit,
) {
    HeaderTile(icon = Icons.Outlined.Schedule)
    Spacer(Modifier.height(16.dp))
    Headline(text = stringResource(R.string.delete_sheet_grace_headline))
    Spacer(Modifier.height(8.dp))
    BodyWithBoldSpan(
        format = stringResource(R.string.delete_sheet_grace_body_format),
        bold = stringResource(R.string.delete_sheet_grace_days_format, state.daysRemaining.toInt()),
    )
    Spacer(Modifier.height(24.dp))
    PrimaryButton(
        text = stringResource(R.string.delete_sheet_cancel_cta),
        loading = state.isSubmitting,
        enabled = !state.isSubmitting,
        onClick = onCancelDeletion,
        testTag = "delete_account_cancel_cta",
    )
    Spacer(Modifier.height(4.dp))
    TextButton(
        onClick = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("delete_account_close_cta"),
    ) {
        Text(
            text = stringResource(R.string.delete_sheet_close),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ---------- Shared building blocks ----------

@Composable
private fun HeaderTile(icon: ImageVector) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(BrandColors.CoralLight.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(R.string.delete_sheet_icon_cd),
                tint = BrandColors.CoralDeep,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun Headline(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() },
    )
}

@Composable
private fun BodyWithBoldSpan(format: String, bold: String) {
    val annotated = buildAnnotatedString {
        val placeholder = "%1\$s"
        val parts = format.split(placeholder)
        append(parts.getOrElse(0) { "" })
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
        if (parts.size > 1) append(parts.drop(1).joinToString(placeholder))
    }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Eyebrow(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SummaryCard(petNames: List<String>, matchCount: Int) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = BrandColors.PeachWarmLight.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.delete_sheet_will_remove).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                ),
                color = BrandColors.PeachWarmDeep,
            )
            Spacer(Modifier.height(2.dp))
            if (petNames.isNotEmpty()) {
                SummaryRow(
                    icon = Icons.Outlined.Pets,
                    text = pluralStringResource(
                        id = R.plurals.delete_summary_pets,
                        count = petNames.size,
                        petNames.size,
                        joinTruncated(petNames),
                    ),
                )
            }
            if (matchCount > 0) {
                SummaryRow(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    text = pluralStringResource(
                        id = R.plurals.delete_summary_matches,
                        count = matchCount,
                        matchCount,
                    ),
                )
            }
            SummaryRow(
                icon = Icons.Outlined.Block,
                text = stringResource(R.string.delete_sheet_profile_row),
            )
        }
    }
}

@Composable
private fun SummaryRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BrandColors.CoralDeep,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun joinTruncated(names: List<String>): String {
    val joined = names.joinToString(", ")
    return if (joined.length <= 60) joined else joined.take(57) + "…"
}

// ---------- Previews ----------

@Preview(name = "Delete sheet — confirm, empty counts", showBackground = true)
@Composable
private fun DeleteAccountSheetPreviewConfirmEmpty() {
    TinPetTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                ConfirmContent(
                    state = DeleteAccountSheetUiState(),
                    onTypedChanged = {},
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }
    }
}

@Preview(name = "Delete sheet — confirm, 2 pets + 14 matches", showBackground = true)
@Composable
private fun DeleteAccountSheetPreviewConfirmFilled() {
    TinPetTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                ConfirmContent(
                    state = DeleteAccountSheetUiState(
                        petNames = listOf("Biscuit", "Pearl"),
                        matchCount = 14,
                        typed = "DELETE",
                    ),
                    onTypedChanged = {},
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }
    }
}

@Preview(name = "Delete sheet — grace, 27 days", showBackground = true)
@Composable
private fun DeleteAccountSheetPreviewGrace() {
    TinPetTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                GraceContent(
                    state = DeleteAccountSheetUiState(
                        pendingDeletion = null, // For preview only — Headline ignores this
                        daysRemaining = 27,
                    ),
                    onCancelDeletion = {},
                    onDismiss = {},
                )
            }
        }
    }
}
