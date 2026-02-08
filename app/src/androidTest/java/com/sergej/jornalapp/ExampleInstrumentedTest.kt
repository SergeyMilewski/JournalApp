package com.sergej.jornalapp

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExampleInstrumentedTest {
    @Test
    fun `given application context when accessed then package name matches`() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.sergej.jornalapp", appContext.packageName)
    }
}
