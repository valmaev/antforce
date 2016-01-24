package com.newmarket.force.ant

import com.newmarket.force.ant.dsl.JUnitReport
import com.newmarket.force.ant.dsl.TestSuite
import com.sforce.soap.metadata.RunTestsResult
import java.time.LocalDateTime

public class Reporter(val dateTimeProvider: () -> LocalDateTime) {

    fun createJUnitReport(
        runTestsResult: RunTestsResult,
        suiteName: String = ""): TestSuite {
        val suite = JUnitReport.testSuite(
            name = suiteName,
            tests = runTestsResult.numTestsRun - runTestsResult.numFailures,
            failures = runTestsResult.numFailures,
            time = runTestsResult.totalTime / 1000,
            timestamp = dateTimeProvider())

        return suite
    }
}