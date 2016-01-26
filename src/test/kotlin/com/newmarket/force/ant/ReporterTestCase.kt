package com.newmarket.force.ant

import com.newmarket.force.ant.dsl.*
import com.sforce.soap.metadata.RunTestFailure
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

        assertThat(actual.collectionSizeOrNull(), equalTo(expected.size))
        expected.forEach { assertThat(reason, actual.contains(it)) }
    }

    @DataProvider
    fun createJUnitReportTestCaseData(): Array<Array<out Any>> =
        arrayOf(
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

    @Test(dataProvider = "createJUnitReportTestCaseFailureData")
    fun createJUnitReport_forEachFailure_shouldCreateTestCaseWithFailureInsideTestSuite(
        failures: Array<RunTestFailure>,
        expected: Array<TestCase>,
        reason: String) {

        val sut = Reporter(dateTimeProvider)
        val input = createRunTestsResult(failures = failures)

        val actual = sut.createJUnitReport(input).testCases

        assertThat(actual.collectionSizeOrNull(), equalTo(expected.size))
        expected.forEach { assertThat(reason, actual.contains(it)) }
    }

    @DataProvider
    fun createJUnitReportTestCaseFailureData(): Array<Array<out Any>> =
        arrayOf(
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
                        className = "foo.MyTestClass",
                        name = "testMethodName",
                        time = 1000.0 / 1000.0) {

                        failure(
                            message = "System.AssertionError",
                            type = "Class") {
                            +"foo.MyTestClass.testMethodName: line 9, column 1"
                        }
                    },
                    TestSuite().testCase(
                        className = "bar.OtherTestClass",
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
                        className = "MyTestClass") {
                        failure() { +"" }
                    }),
                "Should treat null namespace as empty string " +
                    "(className = name instead of .name or null.name)"))

    @Test(dataProvider = "createJUnitReportTestCasePropertiesData")
    fun createJUnitReport_forEachProperty_shouldCreateCorrespondingPropertyElement(
        properties: Map<String, String>,
        expected: Array<Property>,
        reason: String) {

        val sut = Reporter(dateTimeProvider)
        val suite = sut.createJUnitReport(createRunTestsResult(), properties = properties)
        val actual = suite
            .children.filterIsInstance<Properties>().single()
            .children.filterIsInstance<Property>()

        assertThat(actual.collectionSizeOrNull(), equalTo(expected.size))
        expected.forEach { assertThat(reason, actual.contains(it)) }
    }

    @DataProvider
    fun createJUnitReportTestCasePropertiesData(): Array<Array<out Any>> =
        arrayOf(
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

    fun createRunTestSuccess(
        namespace: String? = "",
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

    fun createRunTestFailure(
        namespace: String? = "",
        name: String = "",
        methodName: String = "",
        message: String = "",
        type: String = "",
        stackTrace: String = "",
        time: Double = 0.0): RunTestFailure {

        val failure = RunTestFailure()
        failure.namespace = namespace
        failure.name = name
        failure.methodName = methodName
        failure.message = message
        failure.type = type
        failure.stackTrace = stackTrace
        failure.time = time
        return failure
    }

    fun createTestCase(
        className: String = "",
        name: String = "",
        time: Double = 0.0): TestCase {

        val testCase = TestCase()
        testCase.className = className
        testCase.name = name
        testCase.time = time
        return testCase
    }

    fun createProperty(name: String = "", value: String = ""): Property {
        val property = Property()
        property.name = name
        property.value = value
        return property
    }
}