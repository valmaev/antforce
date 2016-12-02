package com.aquivalabs.force.ant.reporters

import com.aquivalabs.force.ant.*
import com.sforce.soap.metadata.RunTestFailure
import com.sforce.soap.metadata.RunTestSuccess
import com.sforce.soap.metadata.RunTestsResult
import org.hamcrest.MatcherAssert.*
import org.hamcrest.CoreMatchers.*
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import org.xmlunit.diff.*
import org.xmlunit.diff.ElementSelectors.*
import org.xmlunit.matchers.CompareMatcher.*
import org.xmlunit.matchers.EvaluateXPathMatcher.*
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime


abstract class JUnitReporterTestCase<out T> where T : JUnitReporter {

    val dateTimeProvider: () -> LocalDateTime = { LocalDateTime.MIN }

    @Test(dataProvider = "createReportTestSuiteData")
    fun createReport_always_shouldCreateProperTestSuite(
        input: RunTestsResult,
        expected: String,
        reason: String) = withTestDirectory {

        val sut = createSystemUnderTest(outputDir = it)
        val reports = sut.createReport(deployResult(input)).getReportContents()

        reports.forEach {
            assertThat(reason, it.value, isSimilarTo(expected)
                .withNodeFilter { n -> n.nodeName == "testsuite" }
                .withAttributeFilter { p -> p.name != "name" })
        }
    }

    @DataProvider
    fun createReportTestSuiteData(): Array<Array<Any>> {
        return arrayOf(
            arrayOf(
                runTestsResult(),
                """<testsuite name="" tests="0" failures="0" time="0.0" errors="0" timestamp="${dateTimeProvider()}" />""",
                "Should create default testSuite when default RunTestsResult passed"),
            arrayOf(
                runTestsResult(
                    successes = Array(8) { runTestSuccess() },
                    failures = Array(2) { runTestFailure() }),
                """<testsuite name="" tests="8" failures="2" time="0.0" errors="0" timestamp="${dateTimeProvider()}" />""",
                "Should properly calculate tests number (successes.size and failures.size)"),
            arrayOf(
                runTestsResult(
                    successes = Array(3) { runTestSuccess(time = 2.0) },
                    failures = Array(4) { runTestFailure(time = 1.0) }),
                """<testsuite name="" tests="3" failures="4" time="${(3 * 2.0 + 4 * 1.0) / 1000.0}" errors="0" timestamp="${dateTimeProvider()}" />""",
                "Should properly calculate time in ms (sum of times in ms for all successes and failures)"))
    }

    @Test(dataProvider = "createReportSuccessfulTestCaseData")
    fun createReport_forEachSuccess_shouldCreateTestCaseInsideTestSuite(
        successes: Array<RunTestSuccess>,
        expected: String,
        reason: String) = withTestDirectory {

        val sut = createSystemUnderTest(outputDir = it)
        val input = runTestsResult(successes = successes)

        val reports = sut.createReport(deployResult(input)).getReportContents()
        reports.forEach {
            assertThat(reason, it.value, isSimilarTo(expected)
                .ignoreWhitespace()
                .withAttributeFilter { p -> p.ownerElement.tagName != "testsuite" }
                .withNodeMatcher(DefaultNodeMatcher(
                    selectorForElementNamed("testsuite", byName),
                    selectorForElementNamed("testcase", byNameAndAttributes("classname", "name")))))
        }
    }

    @DataProvider
    fun createReportSuccessfulTestCaseData(): Array<Array<out Any>> {
        return arrayOf(
            arrayOf(
                arrayOf(
                    runTestSuccess(
                        namespace = "foo",
                        name = "MyTestClass",
                        methodName = "testMethodName",
                        time = 1000.0),
                    runTestSuccess(
                        namespace = "foo",
                        name = "MyTestClass",
                        methodName = "otherTestMethodName",
                        time = 2000.0)),
                """<testsuite>
                     <testcase classname="foo.MyTestClass" name="testMethodName" time="1.0" />
                     <testcase classname="foo.MyTestClass" name="otherTestMethodName" time="2.0" />
                   </testsuite>""",
                "Should properly create test case for each success " +
                    "(className = namespace.name, name = methodName, time = time / 1000.0)"),
            arrayOf(
                arrayOf(
                    runTestSuccess(
                        namespace = null,
                        name = "TestClass")),
                """<testsuite>
                     <testcase classname="TestClass" name="" time="0.0" />
                   </testsuite>""",
                "Should treat null namespace as empty string " +
                    "(className = name instead of .name or null.name)")
        )
    }

    @Test(dataProvider = "createReportFailedTestCaseData")
    fun createReport_forEachFailure_shouldCreateTestCaseWithFailureInsideTestSuite(
        failures: Array<RunTestFailure>,
        expected: String,
        reason: String) = withTestDirectory {

        val sut = createSystemUnderTest(outputDir = it)
        val input = runTestsResult(failures = failures)

        val reports = sut.createReport(deployResult(input)).getReportContents()

        reports.forEach {
            assertThat(reason, it.value, isSimilarTo(expected)
                .ignoreWhitespace()
                .withAttributeFilter { p -> p.ownerElement.tagName != "testsuite" }
                .withNodeMatcher(DefaultNodeMatcher(
                    selectorForElementNamed("testsuite", byName),
                    selectorForElementNamed("testcase", byNameAndAttributes("classname", "name")),
                    selectorForElementNamed("failure", byNameAndAttributes("message", "type")))))
        }
    }

    @DataProvider
    fun createReportFailedTestCaseData(): Array<Array<out Any>> {
        return arrayOf(
            arrayOf(
                arrayOf(
                    runTestFailure(
                        message = "System.AssertionError",
                        type = "Class",
                        stackTrace = "foo.MyTestClass.testMethodName: line 9, column 1",
                        namespace = "foo",
                        name = "MyTestClass",
                        methodName = "testMethodName",
                        time = 1000.0),
                    runTestFailure(
                        message = "System.NullPointerException",
                        type = "Class",
                        stackTrace = "foo.MyTestClass.someTestMethodName: line 21, column 1",
                        namespace = "foo",
                        name = "MyTestClass",
                        methodName = "someTestMethodName",
                        time = 2000.0)),
                """<testsuite>
                    <testcase classname="foo.MyTestClass" name="testMethodName" time="1.0">
                      <failure message="System.AssertionError" type="Class">
                        <![CDATA[foo.MyTestClass.testMethodName: line 9, column 1]]>
                      </failure>
                    </testcase>
                    <testcase classname="foo.MyTestClass" name="someTestMethodName" time="2.0">
                      <failure message="System.NullPointerException" type="Class">
                        <![CDATA[foo.MyTestClass.someTestMethodName: line 21, column 1]]>
                      </failure>
                    </testcase>
                   </testsuite>""",
                "Should properly create test case with nested failure for each failure " +
                    "(className = namespace.name, name = methodName, time = time / 1000.0 " +
                    "failure.message = message, failure.type = type, failure.CDATA = stackTrace)"),
            arrayOf(
                arrayOf(
                    runTestFailure(
                        namespace = null,
                        name = "MyTestClass")),
                """<testsuite>
                    <testcase classname="MyTestClass" name="" time="0.0">
                      <failure message="" type="">
                        <![CDATA[]]>
                      </failure>
                    </testcase>
                   </testsuite>""",
                "Should treat null namespace as empty string " +
                    "(className = name instead of .name or null.name)"))
    }

    @Test(dataProvider = "createReportTestCasePropertiesData")
    fun createReport_forEachProperty_shouldCreateCorrespondingPropertyElement(
        properties: Map<String, String>,
        expected: String,
        reason: String) = withTestDirectory {

        val sut = createSystemUnderTest(outputDir = it)
        sut.properties = properties

        val reports = sut.createReport(deployResult()).getReportContents()

        reports.forEach {
            assertThat(reason, it.value, isSimilarTo(expected)
                .ignoreWhitespace()
                .withAttributeFilter { n -> n.ownerElement.tagName != "testsuite" })
        }
    }

    @DataProvider
    fun createReportTestCasePropertiesData(): Array<Array<out Any>> {
        return arrayOf(
            arrayOf(
                hashMapOf<String, String>(),
                """<testsuite>
                    <properties />
                   </testsuite>""",
                "Should create empty properties element"),
            arrayOf(
                hashMapOf(
                    "foo" to "bar",
                    "baz" to "qux"),
                """<testsuite>
                    <properties>
                      <property name="baz" value="qux" />
                      <property name="foo" value="bar" />
                    </properties>
                  </testsuite>""",
                "Should create property for each map entry"))
    }

    abstract fun createSystemUnderTest(
        outputDir: File,
        dateTimeProvider: () -> LocalDateTime = this.dateTimeProvider): T

    open fun File.getReportContents(): Map<String, String> =
        Files.newDirectoryStream(this.toPath(), "TEST-*.xml").use {
            return it.map { path -> Pair(path.fileName.toString(), path.toFile().readText()) }.toMap()
        }

}

class SingleSuiteJUnitReporterTestCase : JUnitReporterTestCase<SingleSuiteJUnitReporter>() {
    @Test(dataProvider = "createReportSuiteNameData")
    fun createReport_always_shouldUsePassedSuiteNameAsExpected(expected: String) = withTestDirectory {
        val sut = createSystemUnderTest(outputDir = it)
        sut.suiteName = expected

        val reports = sut.createReport(deployResult()).getReportContents()
        reports.forEach {
            assertThat(it.value, hasXPath("/testsuite/@name", equalTo(expected)))
        }
    }

    @DataProvider
    fun createReportSuiteNameData(): Array<Array<out Any>> =
        arrayOf(
            arrayOf<Any>(""),
            arrayOf<Any>("foo"))

    override fun createSystemUnderTest(outputDir: File, dateTimeProvider: () -> LocalDateTime) =
        SingleSuiteJUnitReporter(outputDir = outputDir, dateTimeProvider = dateTimeProvider, suiteName = "")
}

class SuitePerTestClassJUnitReporterTests : JUnitReporterTestCase<SuitePerTestClassJUnitReporter>() {

    override fun createSystemUnderTest(outputDir: File, dateTimeProvider: () -> LocalDateTime) =
        SuitePerTestClassJUnitReporter(outputDir = outputDir, dateTimeProvider = dateTimeProvider)
}