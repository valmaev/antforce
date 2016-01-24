package com.newmarket.force.ant

import com.newmarket.force.ant.dsl.JUnitReport
import com.newmarket.force.ant.dsl.TestSuite
import com.sforce.soap.metadata.RunTestsResult
import org.hamcrest.MatcherAssert.*
import org.hamcrest.beans.SamePropertyValuesAs.*
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.LocalDateTime

public class ReporterTestCase {

    final val dateTimeProvider: () -> LocalDateTime = { LocalDateTime.MIN }

    @Test(dataProvider = "createJUnitReportTesSuiteData")
    fun createJUnitReport_always_shouldCreateProperTestSuite(
        input: RunTestsResult,
        expected: TestSuite,
        reason: String) {

        val sut = Reporter(dateTimeProvider)
        val actual = sut.createJUnitReport(input)
        assertThat(reason, actual, samePropertyValuesAs(expected))
    }

    @DataProvider
    fun createJUnitReportTesSuiteData(): Array<Array<Any>> =
        arrayOf(
            arrayOf(
                createRunTestsResult(),
                JUnitReport.testSuite(timestamp = dateTimeProvider()),
                "Should create default testSuite when default RunTestsResult passed"),
            arrayOf(
                createRunTestsResult(numTestsRun = 10, numFailures = 2),
                JUnitReport.testSuite(tests = 10 - 2, failures = 2, timestamp = dateTimeProvider()),
                "Should properly calculate tests number (numRunTests - numFailures)"),
            arrayOf(
                createRunTestsResult(totalTime = 1000.0),
                JUnitReport.testSuite(time = 1000.0 / 1000.0, timestamp = dateTimeProvider()),
                "Should properly calculate time in ms (totalTime / 1000.0)"))

    fun createRunTestsResult(
        numTestsRun: Int = 0,
        numFailures: Int = 0,
        totalTime: Double = 0.0): RunTestsResult {
        val result = RunTestsResult()
        result.numTestsRun = numTestsRun
        result.numFailures = numFailures
        result.totalTime = totalTime
        return result
    }
}