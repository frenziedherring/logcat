package logcat

import kotlinx.coroutines.Runnable
import logcat.LogPriority.INFO
import kotlin.native.concurrent.AtomicReference
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogcatTest {

    @AfterTest
    fun tearDown() {
        LogcatLogger.uninstall()
    }

    @Test fun when_no_logger_set_calling_logcat_does_not_crash() {
        logcat { "Yo" }
    }

    @Test fun when_no_logger_set_the_message_lambda_is_not_invoked() {
        var count = 0

        logcat {
            "Yo${++count}"
        }

        assertEquals(0, count)
    }

    @Test fun logcat_logs_message_from_lambda() {
        val logger = NativeTestLogcatLogger().apply { LogcatLogger.install(this); latestLog = null }

        logcat { "Hi" }

        assertEquals("Hi", logger.latestLog!!.message)
    }

    @Test fun logcat_captures_tag_from_outer_context_class_name() {
        val logger = NativeTestLogcatLogger().apply { LogcatLogger.install(this); latestLog = null }

        logcat { "Hi" }

        assertEquals(LogcatTest::class.simpleName, logger.latestLog!!.tag)
    }

    @Test fun logcat_tag_overriding_passes_tag_to_logger() {
        val logger = NativeTestLogcatLogger().apply { LogcatLogger.install(this); latestLog = null }

        logcat(tag = "Bonjour") { "Hi" }

        assertEquals("Bonjour", logger.latestLog!!.tag)
    }

    @Test fun logcat_passes_priority_to_logger() {
        val logger = NativeTestLogcatLogger().apply { LogcatLogger.install(this); latestLog = null }

        logcat(INFO) { "Hi" }

        assertEquals(INFO, logger.latestLog!!.priority)
    }

    @Test fun logcat_passes_priority_to_isLoggable_check() {
        val receivedPriority: AtomicReference<LogPriority?> = AtomicReference(null)
        NativeTestLogcatLogger(isLoggable = { receivedPriority.value = it; true })
            .apply { LogcatLogger.install(this); latestLog = null }

        logcat(INFO) { "Hi" }

        assertEquals(INFO, receivedPriority.value)
    }

    @Test fun when_not_loggable_the_message_lambda_is_not_invoked() {
        NativeTestLogcatLogger(isLoggable = { false }).apply { LogcatLogger.install(this); latestLog = null }
        var count = 0

        logcat { "Yo${++count}" }

        assertEquals(0, count)
    }

    @Test fun Throwable_asLogMessage_has_stacktrace_logged() {
        val logger = NativeTestLogcatLogger().apply { LogcatLogger.install(this); latestLog = null }
        val exception = RuntimeException("damn")

        logcat { exception.asLog() }

        val stackTraceStr = """
      |kotlin.RuntimeException: damn
      |    at 0
      """.trimMargin()
        assertTrue(
            logger.latestLog!!.message.contains(stackTraceStr)
        )
    }

    @Test fun standalone_function_can_log_with_tag() {
        val logger = NativeTestLogcatLogger().apply { LogcatLogger.install(this); latestLog = null }

        standaloneFunctionLog(tag = "Bonjour", message = { "Hi" })

        with(logger.latestLog!!) {
            assertEquals("Bonjour", tag)
            assertEquals("Hi", message)
        }
    }

    @Test fun logcat_captures_outer_this_tag_from_lambda() {
        val logger = NativeTestLogcatLogger().apply { LogcatLogger.install(this); latestLog = null }

        val lambda = {
            logcat { "Hi" }
        }
        lambda()

        assertEquals(LogcatTest::class.simpleName, logger.latestLog!!.tag)
    }

    @Test fun logcat_captures_outer_this_tag_from_nested_lambda() {
        val logger = NativeTestLogcatLogger().apply { LogcatLogger.install(this); latestLog = null }

        val lambda = {
            val lambda = {
                logcat { "Hi" }
            }
            lambda()
        }
        lambda()

        assertEquals(LogcatTest::class.simpleName, logger.latestLog!!.tag)
    }

    @Test fun logcat_captures_outer_this_tag_from_anonymous_object() {
        val logger = NativeTestLogcatLogger().apply { LogcatLogger.install(this); latestLog = null }

        val anonymousRunnable = object : Runnable {
            override fun run() {
                logcat { "Hi" }
            }
        }
        anonymousRunnable.run()

        assertEquals(LogcatTest::class.simpleName, logger.latestLog!!.tag)
    }

    @Test fun logcat_captures_tag_from_companion_function() {
        val logger = NativeTestLogcatLogger().apply { LogcatLogger.install(this); latestLog = null }

        companionFunctionLog { "Hi" }

        assertEquals(LogcatTest::class.simpleName, logger.latestLog!!.tag)
    }

    @Test fun logcat_captures_tag_from_nested_companion_function() {
        val logger = NativeTestLogcatLogger().apply { LogcatLogger.install(this); latestLog = null }

        nestedCompanionFunctionLog { "Hi" }

        assertEquals(LogcatTest::class.simpleName, logger.latestLog!!.tag)
    }

    companion object {
        fun companionFunctionLog(
            message: () -> String
        ) {
            logcat(message = message)
        }
        fun nestedCompanionFunctionLog(
            message: () -> String
        ) {
            fun anotherNestedFunctionLog(
                message: () -> String
            ) {
                logcat(message = message)
            }
            anotherNestedFunctionLog(message)
        }
    }

}

fun standaloneFunctionLog(
    tag: String,
    message: () -> String
) {
    logcat(tag, message = message)
}
