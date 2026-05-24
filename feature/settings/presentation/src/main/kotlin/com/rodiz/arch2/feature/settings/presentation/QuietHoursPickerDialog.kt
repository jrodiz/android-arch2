package com.rodiz.arch2.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rodiz.arch2.core.designsystem.theme.BrandColors

/**
 * Two-pane time picker for the quiet-hours window — Start + End stacked, each
 * using M3's compact [TimeInput] (number-pad style) so the dialog fits in one
 * screen on small devices. The start>end case (e.g. 22:00 → 08:00) is valid
 * domain shape — the VM doesn't reorder — so we let the user pick any combination.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuietHoursPickerDialog(
    initialStartMinutes: Int,
    initialEndMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (startMinutes: Int, endMinutes: Int) -> Unit,
) {
    val startState = rememberTimePickerState(
        initialHour = initialStartMinutes / 60,
        initialMinute = initialStartMinutes % 60,
        is24Hour = false,
    )
    val endState = rememberTimePickerState(
        initialHour = initialEndMinutes / 60,
        initialMinute = initialEndMinutes % 60,
        is24Hour = false,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("quiet_hours_dialog"),
        title = {
            Text(
                text = stringResource(R.string.notifications_quiet_hours_picker_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LabeledTimeInput(
                    label = stringResource(R.string.notifications_quiet_hours_picker_start),
                    state = startState,
                )
                LabeledTimeInput(
                    label = stringResource(R.string.notifications_quiet_hours_picker_end),
                    state = endState,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        startState.hour * 60 + startState.minute,
                        endState.hour * 60 + endState.minute,
                    )
                },
                modifier = Modifier.testTag("quiet_hours_dialog_save"),
            ) {
                Text(
                    text = stringResource(R.string.notifications_quiet_hours_picker_save),
                    color = BrandColors.CoralDeep,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.notifications_quiet_hours_picker_cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabeledTimeInput(
    label: String,
    state: androidx.compose.material3.TimePickerState,
) {
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
            ) {
                TimeInput(state = state)
            }
        }
        Spacer(Modifier.height(0.dp))
    }
}
