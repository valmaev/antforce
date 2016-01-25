package com.newmarket.force.ant

import com.newmarket.force.ant.dsl.JUnitReport
import com.newmarket.force.ant.dsl.TestCase
import com.newmarket.force.ant.dsl.TestSuite
import com.sforce.soap.metadata.RunTestSuccess
import com.sforce.soap.metadata.RunTestsResult
import org.hamcrest.core.IsEqual.*
import org.hamcrest.MatcherAssert.*
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
        assertThat(reason, actual, equalTo(expected))
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

    @Test(dataProvider = "createJUnitReportSuiteNameData")
    fun createJUnitReport_always_shouldUsePassedSuiteNameAsExpected(expected: String) {
        val sut = Reporter(dateTimeProvider)
        val actual = sut.createJUnitReport(createRunTestsResult(), suiteName = expected)
        assertThat(actual.name, equalTo(expected))
    }

    @DataProvider
    fun createJUnitReportSuiteNameData(): Array<Array<out Any>> =
        arrayOf(
            arrayOf<Any>(""),
            arrayOf<Any>("foo"))

    @Test(dataProvider = "createJUnitReportTestCaseData")
    fun createJUnitReport_forEachSuccess_shouldCreateTestCaseInsideTestSuite(
        successes: Array<RunTestSuccess>,
        expected: Array<TestCase>,
        reason: String) {

        val sut = Reporter(dateTimeProvider)
        val input = createRunTestsResult(successes = successes)

        val actual = sut.createJUnitReport(input).testCases

        expected.forEach { assertThat(reason, actual.contains(it)) }
    }

    @DataProvider
    fun createJUnitReportTestCaseData(): Array<Array<out Any>> =
        arrayOf(
            arrayOf<Any>(
                arrayOf<RunTestSuccess>(),
                arrayOf<TestCase>(),
                "Should test suite with 0 test cases for 0 successes"),
            arrayOf(
                arrayOf(
                    createRunTestSuccess(
                        namespace = "foo",
                        name = "MyTestClass",
                        methodName = "testMethodName",
                        time = 1000.0),
                    createRunTestSuccess(
                        namespace = "bar",
                        name = "MyOtherTestClass",
                        methodName = "otherTestMethodName",
                        time = 2000.0)),
                arrayOf(
                    createTestCase(
                        className = "foo.MyTestClass",
                        name = "testMethodName",
                        time = 1000.0 / 1000.0),
                    createTestCase(
                        className = "bar.MyOtherTestClass",
                        name = "otherTestMethodName",
                        time = 2000.0 / 1000.0)),
                "Should properly create test case for each success " +
                    "(className = namespace.name, name = methodName, time = time / 1000.0)"))

    fun createRunTestsResult(
        numTestsRun: Int = 0,
        numFailures: Int = 0,
        totalTime: Double = 0.0,
        successes: Array<RunTestSuccess> = arrayOf()): RunTestsResult {

        val result = RunTestsResult()
        result.numTestsRun = numTestsRun
        result.numFailures = numFailures
        result.totalTime = totalTime
        result.successes = successes
        return result
    }

    fun createRunTestSuccess(
        namespace: String = "",
        name: String = "",
        methodName: String = "",
        time: Double = 0.0): RunTestSuccess {

        val success = RunTestSuccess()
        success.namespace = namespace
        success.name = name
        success.methodName = methodName
        success.time = time
        return success
    }

    fun createTestCase(
        className: String = "",
        name: String = "", time:
        Double = 0.0): TestCase {

        val testCase = TestCase()
        testCase.className = className
        testCase.name = name
        testCase.time = time
        return testCase
    }
}