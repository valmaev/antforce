package com.aquivalabs.force.ant.reporters

import com.aquivalabs.force.ant.*
import com.aquivalabs.force.ant.reporters.junit.*
import com.sforce.soap.metadata.RunTestFailure
import com.sforce.soap.metadata.RunTestSuccess
import com.sforce.soap.metadata.RunTestsResult
import org.hamcrest.MatcherAssert.*
import org.hamcrest.core.IsEqual.*
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.LocalDateTime


class JUnitReporterTestCase {
    val dateTimeProvider: () -> LocalDateTime = { LocalDateTime.MIN }

    @Test(dataProvider = "createReportTesSuiteData")
    fun createReport_always_shouldCreateProperTestSuite(
        input: RunTestsResult,
        expected: TestSuite,
        reason: String) {

        val sut = createSystemUnderTest(dateTimeProvider)
        val report = sut.createReport(input)
        val actual = report.children.filterIsInstance<TestSuite>().single()
        assertThat(reason, actual, equalTo(expected))
    }

    @DataProvider
    fun createReportTesSuiteData(): Array<Array<Any>> {
        return arrayOf(
            arrayOf(
                createRunTestsResult(),
                JUnitReportRoot().testSuite(timestamp = dateTimeProvider()),
                "Should create default testSuite when default RunTestsResult passed"),
            arrayOf(
                createRunTestsResult(numTestsRun = 10, numFailures = 2),
                JUnitReportRoot().testSuite(tests = 10 - 2, failures = 2, timestamp = dateTimeProvider()),
                "Should properly calculate tests number (numRunTests - numFailures)"),
            arrayOf(
                createRunTestsResult(totalTime = 1000.0),
                JUnitReportRoot().testSuite(time = 1000.0 / 1000.0, timestamp = dateTimeProvider()),
                "Should properly calculate time in ms (totalTime / 1000.0)"))
    }

    @Test(dataProvider = "createReportSuiteNameData")
    fun createReport_always_shouldUsePassedSuiteNameAsExpected(expected: String) {
        val sut = createSystemUnderTest(dateTimeProvider)
        sut.suiteName = expected
        val report = sut.createReport(createRunTestsResult())
        val actual = report.children.filterIsInstance<TestSuite>().single()
        assertThat(actual.name, equalTo(expected))
    }

    @DataProvider
    fun createReportSuiteNameData(): Array<Array<out Any>> =
        arrayOf(
            arrayOf<Any>(""),
            arrayOf<Any>("foo"))

    @Test(dataProvider = "createReportTestCaseData")
    fun createReport_forEachSuccess_shouldCreateTestCaseInsideTestSuite(
        successes: Array<RunTestSuccess>,
        expected: Array<TestCase>,
        reason: String) {

        val sut = createSystemUnderTest(dateTimeProvider)
        val input = createRunTestsResult(successes = successes)

        val report = sut.createReport(input)
        val actual = report.children.filterIsInstance<TestSuite>().single().testCases

        assertThat(actual.count(), equalTo(expected.size))
        expected.forEach { assertThat(reason, actual.contains(it)) }
    }

    @DataProvider
    fun createReportTestCaseData(): Array<Array<out Any>> {
        return arrayOf(
            arrayOf<Any>(
                arrayOf<RunTestSuccess>(),
                arrayOf<TestCase>(),
                "Should create test suite with 0 test cases for 0 successes"),
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
                    "(className = namespace.name, name = methodName, time = time / 1000.0)"),
            arrayOf(
                arrayOf(
                    createRunTestSuccess(
                        namespace = null,
                        name = "TestClass")),
                arrayOf(
                    createTestCase(
                        className = "TestClass")),
                "Should treat null namespace as empty string " +
                    "(className = name instead of .name or null.name)"))
    }

    @Test(dataProvider = "createReportTestCaseFailureData")
    fun createReport_forEachFailure_shouldCreateTestCaseWithFailureInsideTestSuite(
        failures: Array<RunTestFailure>,
        expected: Array<TestCase>,
        reason: String) {

        val sut = createSystemUnderTest(dateTimeProvider)
        val input = createRunTestsResult(failures = failures)

        val report = sut.createReport(input)
        val actual = report.children.filterIsInstance<TestSuite>().single().testCases

        assertThat(actual.count(), equalTo(expected.size))
        expected.forEach { assertThat(reason, actual.contains(it)) }
    }

    @DataProvider
    fun createReportTestCaseFailureData(): Array<Array<out Any>> {
        return arrayOf(
            arrayOf<Any>(
                arrayOf<RunTestFailure>(),
                arrayOf<TestCase>(),
                "Should create test suite with 0 test cases for 0 failures"),
            arrayOf(
                arrayOf(
                    createRunTestFailure(
                        message = "System.AssertionError",
                        type = "Class",
                        stackTrace = "foo.MyTestClass.testMethodName: line 9, column 1",
                        namespace = "foo",
                        name = "MyTestClass",
                        methodName = "testMethodName",
                        time = 1000.0),
                    createRunTestFailure(
                        message = "System.NullPointerException",
                        type = "Trigger",
                        stackTrace = "bar.OtherTestClass.someTestMethodName: line 21, column 1",
                        namespace = "bar",
                        name = "OtherTestClass",
                        methodName = "someTestMethodName",
                        time = 2000.0)),
                arrayOf(
                    TestSuite().testCase(
                        classname = "foo.MyTestClass",
                        name = "testMethodName",
                        time = 1000.0 / 1000.0) {

                        failure(
                            message = "System.AssertionError",
                            type = "Class") {
                            +"foo.MyTestClass.testMethodName: line 9, column 1"
                        }
                    },
                    TestSuite().testCase(
                        classname = "bar.OtherTestClass",
                        name = "someTestMethodName",
                        time = 2000.0 / 1000.0) {

                        failure(
                            message = "System.NullPointerException",
                            type = "Trigger") {
                            +"bar.OtherTestClass.someTestMethodName: line 21, column 1"
                        }
                    }),
                "Should properly create test case with nested failure for each failure " +
                    "(className = namespace.name, name = methodName, time = time / 1000.0 " +
                    "failure.message = message, failure.type = type, failure.CDATA = stackTrace)"),
            arrayOf(
                arrayOf(
                    createRunTestFailure(
                        namespace = null,
                        name = "MyTestClass")),
                arrayOf(
                    TestSuite().testCase(
                        classname = "MyTestClass") {
                        failure() { +"" }
                    }),
                "Should treat null namespace as empty string " +
                    "(className = name instead of .name or null.name)"))
    }

    @Test(dataProvider = "createReportTestCasePropertiesData")
    fun createReport_forEachProperty_shouldCreateCorrespondingPropertyElement(
        properties: Map<String, String>,
        expected: Array<Property>,
        reason: String) {

        val sut = createSystemUnderTest(dateTimeProvider)
        sut.properties = properties
        val report = sut.createReport(createRunTestsResult())
        val suite = report.children.filterIsInstance<TestSuite>().single()
        val actual = suite
            .children.filterIsInstance<Properties>().single()
            .children.filterIsInstance<Property>()

        assertThat(actual.count(), equalTo(expected.size))
        expected.forEach { assertThat(reason, actual.contains(it)) }
    }

    @DataProvider
    fun createReportTestCasePropertiesData(): Array<Array<out Any>> {
        return arrayOf(
            arrayOf(
                hashMapOf<String, String>(),
                arrayOf<Property>(),
                "Should create empty properties element"),
            arrayOf(
                hashMapOf(
                    "foo" to "bar",
                    "baz" to "qux"),
                arrayOf(
                    createProperty("foo", "bar"),
                    createProperty("baz", "qux")),
                "Should create property for each map entry"))
    }

    fun createSystemUnderTest(
        dateTimeProvider: () -> LocalDateTime = this.dateTimeProvider) =
        JUnitReporter(dateTimeProvider = dateTimeProvider)
}