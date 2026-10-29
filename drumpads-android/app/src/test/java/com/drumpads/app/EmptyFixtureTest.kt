package com.drumpads.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class EmptyFixtureTest {

    @Test
    fun testParseEmptyFixtureWithoutError() {
        // Find fixture relative to repo root or test path
        val candidatePaths = listOf(
            "../shared-specs/fixtures/test-vectors/sequencer-test-vectors.empty.json",
            "../../shared-specs/fixtures/test-vectors/sequencer-test-vectors.empty.json",
            "shared-specs/fixtures/test-vectors/sequencer-test-vectors.empty.json"
        )

        var file: File? = null
        for (path in candidatePaths) {
            val f = File(path)
            if (f.exists()) {
                file = f
                break
            }
        }

        assertNotNull("Fixture file sequencer-test-vectors.empty.json must exist on disk", file)
        val jsonContent = file!!.readText()
        val json = JSONObject(jsonContent)

        assertEquals("Schema version must be 1", 1, json.getInt("schemaVersion"))
        assertEquals("PPQ must be 960", 960, json.getInt("ppq"))
        assertTrue("Fixture must contain testCases array", json.has("testCases"))

        val testCases = json.getJSONArray("testCases")
        assertEquals("M1 empty fixture must have 0 test cases", 0, testCases.length())
    }
}
