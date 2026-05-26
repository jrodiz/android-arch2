package com.rodiz.arch2.feature.notifications.presentation

import com.rodiz.arch2.core.common.logging.CrashReporter
import com.rodiz.arch2.core.firebase.FcmTokenSync
import com.rodiz.arch2.core.firebase.OwnerLocationWriter
import com.rodiz.arch2.core.testing.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class PermissionsOnboardingViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherExt = MainDispatcherExtension()

    private val testDispatcher get() = mainDispatcherExt.dispatcher

    @Test
    fun `initial state both rows NotRequested`() = runTest(testDispatcher) {
        val vm = newViewModel()
        val state = vm.uiState.value
        assertEquals(PermissionStatus.NotRequested, state.location)
        assertEquals(PermissionStatus.NotRequested, state.notifications)
    }

    @Test
    fun `onLocationWorking flips location to Working without touching notifications`() = runTest(testDispatcher) {
        val vm = newViewModel()
        vm.onLocationWorking()
        assertEquals(PermissionStatus.Working, vm.uiState.value.location)
        assertEquals(PermissionStatus.NotRequested, vm.uiState.value.notifications)
    }

    @Test
    fun `onLocationGranted with city writes to repo and surfaces Allowed`() = runTest(testDispatcher) {
        val writer = mockk<OwnerLocationWriter>(relaxed = true)
        val vm = newViewModel(writer = writer)
        vm.onLocationGranted(lat = 19.43, lng = -99.13, cityLabel = "Mexico City")
        advanceUntilIdle()
        assertEquals(PermissionStatus.Allowed("Mexico City"), vm.uiState.value.location)
        coVerify(exactly = 1) { writer.updateLocation(19.43, -99.13, "Mexico City") }
    }

    @Test
    fun `onLocationGranted with null city still writes and surfaces Allowed`() = runTest(testDispatcher) {
        val writer = mockk<OwnerLocationWriter>(relaxed = true)
        val vm = newViewModel(writer = writer)
        vm.onLocationGranted(lat = 40.71, lng = -74.0, cityLabel = null)
        advanceUntilIdle()
        assertEquals(PermissionStatus.Allowed(cityLabel = null), vm.uiState.value.location)
        coVerify(exactly = 1) { writer.updateLocation(40.71, -74.0, null) }
    }

    @Test
    fun `onLocationGranted records non-fatal when repo throws but keeps state Allowed`() = runTest(testDispatcher) {
        val writer = mockk<OwnerLocationWriter> {
            coEvery { updateLocation(any(), any(), any()) } throws RuntimeException("offline")
        }
        val crashReporter = mockk<CrashReporter>(relaxed = true)
        val vm = newViewModel(writer = writer, crashReporter = crashReporter)
        vm.onLocationGranted(lat = 0.0, lng = 0.0, cityLabel = "Null Island")
        advanceUntilIdle()
        assertEquals(PermissionStatus.Allowed("Null Island"), vm.uiState.value.location)
        coVerify(exactly = 1) {
            crashReporter.recordException(any(), match { it!!.contains("location write failed") })
        }
    }

    @Test
    fun `onLocationDenied does not call repo and flips to Denied`() = runTest(testDispatcher) {
        val writer = mockk<OwnerLocationWriter>(relaxed = true)
        val vm = newViewModel(writer = writer)
        vm.onLocationDenied()
        advanceUntilIdle()
        assertEquals(PermissionStatus.Denied, vm.uiState.value.location)
        coVerify(exactly = 0) { writer.updateLocation(any(), any(), any()) }
    }

    @Test
    fun `onLocationFetchFailed flips to Denied with no repo call`() = runTest(testDispatcher) {
        val writer = mockk<OwnerLocationWriter>(relaxed = true)
        val vm = newViewModel(writer = writer)
        vm.onLocationFetchFailed()
        advanceUntilIdle()
        assertEquals(PermissionStatus.Denied, vm.uiState.value.location)
        coVerify(exactly = 0) { writer.updateLocation(any(), any(), any()) }
    }

    @Test
    fun `onNotificationsGranted invokes FcmTokenSync and flips to Allowed`() = runTest(testDispatcher) {
        val sync = mockk<FcmTokenSync>(relaxed = true)
        val vm = newViewModel(sync = sync)
        vm.onNotificationsGranted()
        advanceUntilIdle()
        assertEquals(PermissionStatus.Allowed(), vm.uiState.value.notifications)
        coVerify(exactly = 1) { sync.syncForSignedInUser() }
    }

    @Test
    fun `onNotificationsGranted records non-fatal when sync throws but keeps Allowed`() = runTest(testDispatcher) {
        val sync = mockk<FcmTokenSync> {
            coEvery { syncForSignedInUser() } throws RuntimeException("fcm error")
        }
        val crashReporter = mockk<CrashReporter>(relaxed = true)
        val vm = newViewModel(sync = sync, crashReporter = crashReporter)
        vm.onNotificationsGranted()
        advanceUntilIdle()
        assertEquals(PermissionStatus.Allowed(), vm.uiState.value.notifications)
        coVerify(exactly = 1) {
            crashReporter.recordException(any(), match { it!!.contains("FCM token sync failed") })
        }
    }

    @Test
    fun `onNotificationsDenied does not call sync and flips to Denied`() = runTest(testDispatcher) {
        val sync = mockk<FcmTokenSync>(relaxed = true)
        val vm = newViewModel(sync = sync)
        vm.onNotificationsDenied()
        advanceUntilIdle()
        assertEquals(PermissionStatus.Denied, vm.uiState.value.notifications)
        coVerify(exactly = 0) { sync.syncForSignedInUser() }
    }

    // ---- helpers ----

    private fun newViewModel(
        writer: OwnerLocationWriter = mockk(relaxed = true),
        sync: FcmTokenSync = mockk(relaxed = true),
        crashReporter: CrashReporter = mockk(relaxed = true),
    ): PermissionsOnboardingViewModel = PermissionsOnboardingViewModel(
        locationWriter = writer,
        fcmTokenSync = sync,
        crashReporter = crashReporter,
    )
}
