package com.newmarket.force.ant

import com.newmarket.force.ant.dsl.*
import com.sforce.soap.metadata.*
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
        val report = sut.createJUnitReport(input)
        val actual = report.children.filterIsInstance<TestSuite>().single()
        assertThat(reason, actual, equalTo(expected))
    }

    @DataProvider
    fun createJUnitReportTesSuiteData(): Array<Array<Any>> =
        arrayOf(
            arrayOf(
                createRunTestsResult(),
                JUnitReport().testSuite(timestamp = dateTimeProvider()),
                "Should create default testSuite when default RunTestsResult passed"),
            arrayOf(
                createRunTestsResult(numTestsRun = 10, numFailures = 2),
                JUnitReport().testSuite(tests = 10 - 2, failures = 2, timestamp = dateTimeProvider()),
                "Should properly calculate tests number (numRunTests - numFailures)"),
            arrayOf(
                createRunTestsResult(totalTime = 1000.0),
                JUnitReport().testSuite(time = 1000.0 / 1000.0, timestamp = dateTimeProvider()),
                "Should properly calculate time in ms (totalTime / 1000.0)"))

    @Test(dataProvider = "createJUnitReportSuiteNameData")
    fun createJUnitReport_always_shouldUsePassedSuiteNameAsExpected(expected: String) {
        val sut = Reporter(dateTimeProvider)
        val report = sut.createJUnitReport(createRunTestsResult(), suiteName = expected)
        val actual = report.children.filterIsInstance<TestSuite>().single()
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

        val report = sut.createJUnitReport(input)
        val actual = report.children.filterIsInstance<TestSuite>().single().testCases

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

        val report = sut.createJUnitReport(input)
        val actual = report.children.filterIsInstance<TestSuite>().single().testCases

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
        val report = sut.createJUnitReport(createRunTestsResult(), properties = properties)
        val suite = report.children.filterIsInstance<TestSuite>().single()
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

    @Test(dataProvider = "createCoberturaReportPackagesTestData")
    fun createCoberturaReport_forEachCodeCoverageType_shouldCreatePackage(
        codeCoverage: Array<CodeCoverageResult>,
        projectRootPath: String,
        expected: Packages,
        reason: String) {

        val sut = Reporter(dateTimeProvider)
        val testResult = createRunTestsResult(codeCoverage = codeCoverage)
        val report = sut.createCoberturaReport(testResult, projectRootPath)

        val actual = report
            .children.filterIsInstance<Coverage>().single()
            .children.filterIsInstance<Packages>().single()

        assertThat(reason, actual, equalTo(expected))
    }

    @DataProvider
    fun createCoberturaReportPackagesTestData(): Array<Array<out Any>> =
        arrayOf(
            arrayOf(
                arrayOf<CodeCoverageResult>(),
                "",
                Packages(),
                "Should create empty packages element for empty array of CodeCoverageResult"),
            arrayOf(
                arrayOf(
                    createCodeCoverageResult(type = "Class"),
                    createCodeCoverageResult(type = "Trigger")),
                "",
                Coverage().packages {
                    packageTag("Class") {
                        classes {
                            classTag() {
                                lines()
                            }
                        }
                    }
                    packageTag("Trigger") {
                        classes {
                            classTag() {
                                lines()
                            }
                        }
                    }
                },
                "Should create two packages for two types of CodeCoverageResult"),
            arrayOf(
                arrayOf(
                    createCodeCoverageResult(type = null)),
                "",
                Coverage().packages {
                    packageTag(name = "") {
                        classes {
                            classTag() {
                                lines()
                            }
                        }
                    }
                },
                "Should create package with empty name if type of CodeCoverageResult is null"),
            arrayOf(
                arrayOf(
                    createCodeCoverageResult(
                        type = "Class",
                        name = "Book"),
                    createCodeCoverageResult(
                        type = "Class",
                        name = "BookBuilder",
                        namespace = "foo"),
                    createCodeCoverageResult(
                        type = "Trigger",
                        name = "AccountTrigger"),
                    createCodeCoverageResult(
                        type = "Trigger",
                        name = "BookTrigger",
                        namespace = "bar")),
                "",
                Coverage().packages {
                    packageTag("Class") {
                        classes {
                            classTag(
                                name = "Book",
                                fileName = "/classes/Book.cls") {
                                lines()
                            }
                            classTag(
                                name = "foo.BookBuilder",
                                fileName = "") {
                                lines()
                            }
                        }
                    }
                    packageTag("Trigger") {
                        classes {
                            classTag(
                                name = "AccountTrigger",
                                fileName = "/triggers/AccountTrigger.cls") {
                                lines()
                            }
                            classTag(
                                name = "bar.BookTrigger",
                                fileName = "") {
                                lines()
                            }
                        }
                    }
                },
                "Should create class tag using name and namespace of CodeCoverageResult"),
            arrayOf(
                arrayOf(
                    createCodeCoverageResult(
                        name = "BookBuilder",
                        type = "Class",
                        locationsNotCovered = arrayOf(
                            createCodeLocation(line = 1, numExecutions = 0),
                            createCodeLocation(line = 2, numExecutions = 0),
                            createCodeLocation(line = 245, numExecutions = 3)))),
                "",
                Coverage().packages {
                    packageTag("Class") {
                        classes {
                            classTag(
                                name = "BookBuilder",
                                fileName = "/classes/BookBuilder.cls") {
                                lines {
                                    line(number = 1, hits = 0)
                                    line(number = 2, hits = 0)
                                    line(number = 245, hits = 3)
                                }
                            }
                        }
                    }
                },
                "Should create line for each not covered location in CodeCoverageResult"),
            arrayOf(
                arrayOf(
                    createCodeCoverageResult(
                        name = "BookBuilder",
                        type = "Class")),
                "/foo/bar/myDirectory",
                Coverage().packages {
                    packageTag("Class") {
                        classes {
                            classTag(
                                fileName = "/foo/bar/myDirectory/classes/BookBuilder.cls",
                                name = "BookBuilder") { lines() }
                        }
                    }
                },
                "Should properly construct file names for Classes – {projectRootPath}/classes/{name}.cls"),
            arrayOf(
                arrayOf(
                    createCodeCoverageResult(
                        name = "BookTrigger",
                        type = "Trigger")),
                "/foo/bar/myDirectory",
                Coverage().packages {
                    packageTag("Trigger") {
                        classes {
                            classTag(
                                fileName = "/foo/bar/myDirectory/triggers/BookTrigger.cls",
                                name = "BookTrigger") { lines() }
                        }
                    }
                },
                "Should properly construct file names for Triggers – {projectRootPath}/triggers/{name}.cls"),
            arrayOf(
                arrayOf(
                    createCodeCoverageResult(
                        name = "BookBuilder",
                        namespace = "foo",
                        type = "Class")),
                "/foo/bar/myDirectory",
                Coverage().packages {
                    packageTag("Class") {
                        classes {
                            classTag(
                                fileName = "",
                                name = "foo.BookBuilder") { lines() }
                        }
                    }
                },
                "Should not generate fileName for coverage results with non-empty namespace"),
            arrayOf(
                arrayOf(
                    createCodeCoverageResult(
                        name = null,
                        type = "Class"),
                    createCodeCoverageResult(
                        name = "",
                        type = "Class")),
                "/foo/bar/myDirectory",
                Coverage().packages {
                    packageTag("Class") {
                        classes {
                            classTag(
                                fileName = "",
                                name = "") { lines() }
                            classTag(
                                fileName = "",
                                name = "") { lines() }
                        }
                    }
                },
                "Should not generate fileName for coverage results with null or empty name"),
            arrayOf(
                arrayOf(
                    createCodeCoverageResult(
                        name = "Book",
                        type = "Class")),
                "/foo/bar/myDirectory/",
                Coverage().packages {
                    packageTag("Class") {
                        classes {
                            classTag(
                                name = "Book",
                                fileName = "/foo/bar/myDirectory/classes/Book.cls") { lines() }
                        }
                    }
                },
                "Should properly handle trailing slash in projectRootPath"))

    fun createCodeCoverageResult(
        name: String? = null,
        namespace: String? = null,
        type: String? = null,
        locationsNotCovered: Array<CodeLocation>? = null): CodeCoverageResult {
        val result = CodeCoverageResult()
        result.name = name
        result.namespace = namespace
        result.type = type
        result.locationsNotCovered = locationsNotCovered
        return result
    }

    fun createCodeLocation(
        line: Int = 0,
        numExecutions: Int = 0): CodeLocation {
        val location = CodeLocation()
        location.line = line
        location.numExecutions = numExecutions
        return location
    }

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