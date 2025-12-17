package io.github.ravenliao.plugin

import java.util.concurrent.atomic.AtomicBoolean
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

internal abstract class OpenReportOnceService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    private val opened = AtomicBoolean(false)

    fun tryAcquire(): Boolean = opened.compareAndSet(false, true)

    override fun close() {
    }
}
