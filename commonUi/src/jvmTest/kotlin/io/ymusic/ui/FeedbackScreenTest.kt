package io.ymusic.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import io.ymusic.dynamicengine.DynamicEngineManager
import io.ymusic.dynamicengine.embeddedEngineJsSource
import io.ymusic.settings.InMemorySettingsStore
import io.ymusic.settings.put
import kotlinx.coroutines.runBlocking
import java.awt.datatransfer.DataFlavor
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Feedback stays local until the user copies details or opens a GitHub issue. */
@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class FeedbackScreenTest {

    private fun engines() =
        DynamicEngineManager(CannedHttp { _, _ -> throw UnsupportedOperationException() }, appVersion = 1, jsSource = embeddedEngineJsSource())

    private class RecordingClipboard : Clipboard {
        var text: String? = null
        override val nativeClipboard: NativeClipboard get() = throw UnsupportedOperationException()
        override suspend fun getClipEntry(): ClipEntry? = null
        override suspend fun setClipEntry(clipEntry: ClipEntry?) {
            text = clipEntry?.nativeClipEntry?.let { (it as java.awt.datatransfer.Transferable).getTransferData(DataFlavor.stringFlavor) as String }
        }
    }

    private class RecordingUriHandler : UriHandler {
        val opened = mutableListOf<String>()
        override fun openUri(uri: String) {
            opened += uri
        }
    }

    @Test
    fun feedback_is_manual_and_uses_the_fork_repository() {
        Locale.setDefault(Locale.ENGLISH)
        val graph = AppGraph(settings = InMemorySettingsStore(), engines = engines(), player = StubPlayer(), playerSurface = {})
        runBlocking {
            graph.settings.put(io.ymusic.settings.BoolSetting("telemetry.crashReports", false), true)
            graph.settings.put(io.ymusic.settings.BoolSetting("telemetry.stats", false), true)
        }
        val clipboard = RecordingClipboard()
        val uriHandler = RecordingUriHandler()
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalClipboard provides clipboard, LocalUriHandler provides uriHandler) {
                    FeedbackScreen(graph, onDismiss = {})
                }
            }
            waitUntilAtLeastOneExists(hasText("Copy"), 5_000)
            waitForIdle()
            assertEquals(null, clipboard.text)
            assertTrue(uriHandler.opened.isEmpty())
            onNodeWithText("Copy").performClick()
            waitUntil(timeoutMillis = 5_000) { clipboard.text != null }
            assertTrue("Device ID: " in clipboard.text!!)
            onNodeWithText("Open an issue on GitHub").performClick()
            waitUntil(timeoutMillis = 5_000) { uriHandler.opened.size == 1 }
            assertTrue(uriHandler.opened.single().startsWith("https://github.com/peixotorms/app-ymusic/issues/new?"))
        }
    }

    @Test
    fun project_links_cannot_be_redirected_back_to_upstream() {
        val links = AppLinks(null)
        assertEquals("https://github.com/peixotorms/app-ymusic", links.sourceCode)
        assertEquals(links.sourceCode, links.website)
        assertTrue(links.feedbackEmail.isBlank())
        assertTrue(links.changelog.startsWith(links.sourceCode + "/"))
    }
}
