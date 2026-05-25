package com.rodiz.arch2.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Wrappers around [LegalDocumentScreen] for the three static legal pages.
 * Content is a placeholder draft — copy intentionally signals "we'll firm
 * this up before launch" so legal review can land later without blocking the
 * UI from being reachable today.
 */
@Composable
internal fun PrivacyPolicyRoute(onBack: () -> Unit) {
    LegalDocumentScreen(
        title = stringResource(R.string.legal_privacy_policy_title),
        sections = listOf(
            LegalSection(
                heading = stringResource(R.string.legal_privacy_what_we_collect_title),
                body = stringResource(R.string.legal_privacy_what_we_collect_body),
            ),
            LegalSection(
                heading = stringResource(R.string.legal_privacy_how_we_use_title),
                body = stringResource(R.string.legal_privacy_how_we_use_body),
            ),
            LegalSection(
                heading = stringResource(R.string.legal_privacy_sharing_title),
                body = stringResource(R.string.legal_privacy_sharing_body),
            ),
            LegalSection(
                heading = stringResource(R.string.legal_privacy_rights_title),
                body = stringResource(R.string.legal_privacy_rights_body),
            ),
            LegalSection(
                heading = stringResource(R.string.legal_draft_notice_title),
                body = stringResource(R.string.legal_draft_notice_body),
            ),
        ),
        onBack = onBack,
    )
}

@Composable
internal fun TermsRoute(onBack: () -> Unit) {
    LegalDocumentScreen(
        title = stringResource(R.string.legal_terms_title),
        sections = listOf(
            LegalSection(
                heading = stringResource(R.string.legal_terms_eligibility_title),
                body = stringResource(R.string.legal_terms_eligibility_body),
            ),
            LegalSection(
                heading = stringResource(R.string.legal_terms_conduct_title),
                body = stringResource(R.string.legal_terms_conduct_body),
            ),
            LegalSection(
                heading = stringResource(R.string.legal_terms_content_title),
                body = stringResource(R.string.legal_terms_content_body),
            ),
            LegalSection(
                heading = stringResource(R.string.legal_terms_termination_title),
                body = stringResource(R.string.legal_terms_termination_body),
            ),
            LegalSection(
                heading = stringResource(R.string.legal_draft_notice_title),
                body = stringResource(R.string.legal_draft_notice_body),
            ),
        ),
        onBack = onBack,
    )
}

@Composable
internal fun HelpSafetyRoute(onBack: () -> Unit) {
    LegalDocumentScreen(
        title = stringResource(R.string.legal_help_safety_title),
        sections = listOf(
            LegalSection(
                heading = stringResource(R.string.legal_help_blocking_title),
                body = stringResource(R.string.legal_help_blocking_body),
            ),
            LegalSection(
                heading = stringResource(R.string.legal_help_reporting_title),
                body = stringResource(R.string.legal_help_reporting_body),
            ),
            LegalSection(
                heading = stringResource(R.string.legal_help_in_person_title),
                body = stringResource(R.string.legal_help_in_person_body),
            ),
            LegalSection(
                heading = stringResource(R.string.legal_help_account_title),
                body = stringResource(R.string.legal_help_account_body),
            ),
            LegalSection(
                heading = stringResource(R.string.legal_help_contact_title),
                body = stringResource(R.string.legal_help_contact_body),
            ),
        ),
        onBack = onBack,
    )
}
