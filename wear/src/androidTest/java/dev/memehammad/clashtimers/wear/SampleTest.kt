package dev.memehammad.clashtimers.wear

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

/** Loads a sample village so the "wear renders" workflow can take screenshots. */
@RunWith(AndroidJUnit4::class)
class SampleTest {
    @Test
    fun loadSample() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val now = System.currentTimeMillis()
        fun at(min: Long) = now + min * 60_000
        val json = """{"tag":"#DEMO","name":"Main","exported":${now - 120_000},
          "slots":[{"key":"BUILDER","label":"Builders","total":6,"ends":[${at(154)},${at(866)},${at(2000)},${at(2860)},${at(4320)}]},
                   {"key":"LAB","label":"Lab","total":1,"ends":[${at(349)}]},
                   {"key":"PET","label":"Pets","total":1,"ends":[${at(1066)}]},
                   {"key":"BB_BUILDER","label":"Builder Base","total":3,"ends":[]}],
          "jobs":[{"name":"Inferno Tower","detail":"Level 10 → 11","end":${at(154)},"section":"BUILDERS"},
                  {"name":"Golem","detail":"Level 12 → 13","end":${at(349)},"section":"LAB"},
                  {"name":"Wizard Tower","detail":"Level 15 → 16","end":${at(866)},"section":"BUILDERS"},
                  {"name":"Electro Owl","detail":"Level 9 → 10","end":${at(1066)},"section":"PETS"},
                  {"name":"Archer Queen","detail":"Level 94 → 95","end":${at(2000)},"section":"BUILDERS"}]}"""
        Summary.save(ctx, json)
        DataListener.refresh(ctx)
    }
}
