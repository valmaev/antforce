package com.aquivalabs.force.ant

import com.aquivalabs.force.ant.dsl.cobertura.*
import com.aquivalabs.force.ant.dsl.junit.*
import com.sforce.soap.metadata.*
import org.hamcrest.core.IsNull.*
import org.hamcrest.core.IsEqual.*
import org.hamcrest.core.StringContains.*
import org.hamcrest.collection.IsIterableContainingInAnyOrder.*
import org.hamcrest.collection.IsIn.*
import org.hamcrest.MatcherAssert.*
import org.jsoup.Jsoup
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import org.testng.Assert.assertEquals
import java.io.File
import java.time.LocalDateTime


class ReporterTestCase {

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
    fun createJUnitReportTesSuiteData(): Array<Array<Any>> {
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

        assertThat(actual.count(), equalTo(expected.size))
        expected.forEach { assertThat(reason, actual.contains(it)) }
    }

    @DataProvider
    fun createJUnitReportTestCaseData(): Array<Array<out Any>> {
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

    @Test(dataProvider = "createJUnitReportTestCaseFailureData")
    fun createJUnitReport_forEachFailure_shouldCreateTestCaseWithFailureInsideTestSuite(
        failures: Array<RunTestFailure>,
        expected: Array<TestCase>,
        reason: String) {

        val sut = Reporter(dateTimeProvider)
        val input = createRunTestsResult(failures = failures)

        val report = sut.createJUnitReport(input)
        val actual = report.children.filterIsInstance<TestSuite>().single().testCases

        assertThat(actual.count(), equalTo(expected.size))
        expected.forEach { assertThat(reason, actual.contains(it)) }
    }

    @DataProvider
    fun createJUnitReportTestCaseFailureData(): Array<Array<out Any>> {
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

        assertThat(actual.count(), equalTo(expected.size))
        expected.forEach { assertThat(reason, actual.contains(it)) }
    }

    @DataProvider
    fun createJUnitReportTestCasePropertiesData(): Array<Array<out Any>> {
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
    fun createCoberturaReportPackagesTestData(): Array<Array<out Any>> {
        return arrayOf(
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
                    `package`("Class") {
                        classes {
                            `class`() {
                                lines()
                            }
                        }
                    }
                    `package`("Trigger") {
                        classes {
                            `class`() {
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
                    `package`(name = "") {
                        classes {
                            `class`() {
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
                    `package`("Class") {
                        classes {
                            `class`(
                                name = "Book",
                                filename = "classes/Book.cls") {
                                lines()
                            }
                            `class`(
                                name = "foo.BookBuilder",
                                filename = "") {
                                lines()
                            }
                        }
                    }
                    `package`("Trigger") {
                        classes {
                            `class`(
                                name = "AccountTrigger",
                                filename = "triggers/AccountTrigger.trigger") {
                                lines()
                            }
                            `class`(
                                name = "bar.BookTrigger",
                                filename = "") {
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
                        numLocations = 10,
                        locationsNotCovered = arrayOf(
                            createCodeLocation(line = 1, numExecutions = 0),
                            createCodeLocation(line = 2, numExecutions = 0),
                            createCodeLocation(line = 4, numExecutions = 3)))),
                "",
                Coverage().packages {
                    `package`("Class") {
                        classes {
                            `class`(
                                name = "BookBuilder",
                                filename = "classes/BookBuilder.cls") {
                                lines {
                                    line(number = 1, hits = 0)
                                    line(number = 2, hits = 0)
                                    line(number = 3, hits = 1)
                                    line(number = 4, hits = 3)
                                    line(number = 5, hits = 1)
                                    line(number = 6, hits = 1)
                                    line(number = 7, hits = 1)
                                    line(number = 8, hits = 1)
                                    line(number = 9, hits = 1)
                                    line(number = 10, hits = 1)
                                }
                            }
                        }
                    }
                },
                "Should create line for each not covered location in CodeCoverageResult and for each covered location"),
            arrayOf(
                arrayOf(
                    createCodeCoverageResult(
                        name = "BookBuilder",
                        type = "Class")),
                "/foo/bar/myDirectory",
                Coverage().packages {
                    `package`("Class") {
                        classes {
                            `class`(
                                filename = "classes/BookBuilder.cls",
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
                    `package`("Trigger") {
                        classes {
                            `class`(
                                filename = "triggers/BookTrigger.trigger",
                                name = "BookTrigger") { lines() }
                        }
                    }
                },
                "Should properly construct file names for Triggers – {projectRootPath}/triggers/{name}.trigger"),
            arrayOf(
                arrayOf(
                    createCodeCoverageResult(
                        name = "BookBuilder",
                        namespace = "foo",
                        type = "Class")),
                "/foo/bar/myDirectory",
                Coverage().packages {
                    `package`("Class") {
                        classes {
                            `class`(
                                filename = "",
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
                    `package`("Class") {
                        classes {
                            `class`(
                                filename = "",
                                name = "") { lines() }
                            `class`(
                                filename = "",
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
                    `package`("Class") {
                        classes {
                            `class`(
                                name = "Book",
                                filename = "classes/Book.cls") { lines() }
                        }
                    }
                },
                "Should properly handle trailing slash in projectRootPath"))
    }

    @Test(dataProvider = "createHtmlCoverageReportTestData")
    fun createHtmlCoverageReport_always_shouldContainTotalCoveragePercentage(
        codeCoverage: Array<CodeCoverageResult>) {
        // Arrange
        val sut = Reporter(dateTimeProvider)
        val runTestsResult = createRunTestsResult(codeCoverage = codeCoverage)

        // Act
        val report = sut.createHtmlCoverageReport(runTestsResult)

        // Assert
        val html = Jsoup.parse(report.toString())
        val actual = html.getElementById("totalCoveragePercentage").ownText()
        val expected = "${runTestsResult.totalCoveragePercentage.format(2)}%"
        assertThat(actual, equalTo(expected));
    }

    @Test(dataProvider = "createHtmlCoverageReportTestData")
    fun createHtmlCoverageReport_always_shouldContainTotalLinesCoverage(
        codeCoverage: Array<CodeCoverageResult>) {
        // Arrange
        val sut = Reporter(dateTimeProvider)
        val runTestsResult = createRunTestsResult(codeCoverage = codeCoverage)

        // Act
        val report = sut.createHtmlCoverageReport(runTestsResult)

        // Assert
        val html = Jsoup.parse(report.toString())
        val actual = html.getElementById("totalLinesCoverage").ownText()
        val expected =
            "${runTestsResult.totalNumLocationsCovered}/${runTestsResult.totalNumLocations}"
        assertThat(actual, equalTo(expected));
    }

    @DataProvider
    fun createHtmlCoverageReportTestData(): Array<Array<Any>> {
        return arrayOf(
            arrayOf<Any>(
                arrayOf(createCodeCoverageResult(
                    name = "ControllerCls",
                    type = "Class",
                    numLocations = 100))))
    }

    @Test(dataProvider = "createHtmlCoverageReportWarningsTestData")
    fun createHtmlCoverageReport_always_shouldContainTotalNumberOfCoverageWarnings(
        codeCoverageWarnings: Array<CodeCoverageWarning>?) {
        // Arrange
        val sut = Reporter(dateTimeProvider)
        val runTestsResult = createRunTestsResult(codeCoverageWarnings = codeCoverageWarnings)

        // Act
        val report = sut.createHtmlCoverageReport(runTestsResult)

        // Assert
        val html = Jsoup.parse(report.toString())
        val actual = html.getElementById("totalCoverageWarnings").ownText()
        val expected = "${runTestsResult.codeCoverageWarnings.orEmpty().count()}"
        assertThat(actual, equalTo(expected))
    }

    @DataProvider
    fun createHtmlCoverageReportWarningsTestData(): Array<Array<Any?>> =
        emptyCoverageWarningTestData()
            .plus(nonEmptyCoverageWarningsTestData())
            .plus(arrayOf(arrayOf<Any?>(
                arrayOf(
                    createCodeCoverageWarning()))))

    @DataProvider
    fun emptyCoverageWarningTestData(): Array<Array<Any?>> {
        return arrayOf(
            arrayOf<Any?>(null),
            arrayOf<Any?>(
                arrayOf<CodeCoverageWarning>()))
    }

    @DataProvider
    fun nonEmptyCoverageWarningsTestData(): Array<Array<Any?>> {
        return arrayOf(
            arrayOf<Any?>(
                arrayOf(
                    createCodeCoverageWarning(
                        name = "Book",
                        namespace = "fdc",
                        message = "Test coverage of selected Apex Class is 0%, at least 75% test coverage is required"),
                    createCodeCoverageWarning(
                        name = "bar",
                        namespace = "",
                        message = "Test coverage of selected Apex Class is 20%, at least 75% test coverage is required"),
                    createCodeCoverageWarning(
                        name = "baz",
                        namespace = "fdc",
                        message = "Test coverage of selected Apex Class is 66%, at least 75% test coverage is required"))))
    }

    @Test(dataProvider = "nonEmptyCoverageWarningsTestData")
    fun createHtmlCoverageReport_ifCoverageWarningsExist_shouldContainAllOfThem(
        codeCoverageWarnings: Array<CodeCoverageWarning>) {
        // Arrange
        val sut = Reporter(dateTimeProvider)
        val runTestsResult = createRunTestsResult(codeCoverageWarnings = codeCoverageWarnings)

        // Act
        val report = sut.createHtmlCoverageReport(runTestsResult)

        // Assert
        val html = Jsoup.parse(report.toString())
        val coverageWarningListItems = html
            .getElementById("coverageWarningsList")
            .getElementsByTag("li")
        val actual = coverageWarningListItems.map {
            CoverageWarning(
                qualifiedName = it.getElementsByClass("coverage-warning-name").single().ownText(),
                message = it.getElementsByClass("coverage-warning-message").single().ownText())
        }

        val expected = codeCoverageWarnings.map {
            CoverageWarning(
                qualifiedName = it.qualifiedName,
                message = it.message)
        }

        assertThat(actual, equalTo(expected))
    }

    data class CoverageWarning(val qualifiedName: String?, val message: String?)

    @Test(dataProvider = "emptyCoverageWarningTestData")
    fun createHtmlCoverageReport_ifNoCoverageWarningsExist_shouldNotIncludeThem(
        codeCoverageWarnings: Array<CodeCoverageWarning>?) {
        // Arrange
        val sut = Reporter(dateTimeProvider)
        val runTestsResult = createRunTestsResult(codeCoverageWarnings = codeCoverageWarnings)

        // Act
        val report = sut.createHtmlCoverageReport(runTestsResult)

        // Assert
        val html = Jsoup.parse(report.toString())
        val actual = html.getElementById("coverageWarningsList")
        assertThat(actual, nullValue())
    }

    @Test(dataProvider = "createHtmlCoverageReportTestData")
    fun createHtmlCoverageReport_always_shouldContainTableWithAllCoverageResults(
        codeCoverage: Array<CodeCoverageResult>) {
        // Arrange
        val sut = Reporter(dateTimeProvider)
        val runTestsResult = createRunTestsResult(codeCoverage = codeCoverage)

        // Act
        val report = sut.createHtmlCoverageReport(runTestsResult)

        // Assert
        val html = Jsoup.parse(report.toString())
        val coverageRows = html
            .getElementsByClass("coverage-summary").single()
            .getElementsByTag("tbody").single()
            .getElementsByTag("tr")
        val actual = coverageRows.map {
            val cells = it.getElementsByTag("td")
            CoverageRow(
                type = cells[0].ownText(),
                className = cells[1].ownText(),
                linesPercent = it.getElementsByClass("pct").single().ownText(),
                lines = it.getElementsByClass("abs").single().ownText())
        }

        val expected = codeCoverage.map {
            CoverageRow(
                type = it.type,
                className = it.qualifiedName,
                linesPercent = "${it.coveragePercentage.format(2)}%",
                lines = "${it.numLocationsCovered}/${it.numLocations}")
        }

        expected.forEach { assertThat(actual, containsInAnyOrder(it)) }
    }

    data class CoverageRow(
        val type: String,
        val className: String,
        val linesPercent: String,
        val lines: String)

    @Test(dataProvider = "createHtmlCoverageReportHighlightingTestData")
    fun createHtmlCoverageReport_always_shouldProperlySetCssStyleToClassCoverageRow(
        codeCoverageResult: CodeCoverageResult,
        expected: String) {
        // Arrange
        val sut = Reporter(dateTimeProvider)
        val runTestsResult = createRunTestsResult(codeCoverage = arrayOf(codeCoverageResult))

        // Act
        val report = sut.createHtmlCoverageReport(runTestsResult)

        // Assert
        val html = Jsoup.parse(report.toString())
        val coverageCells = html
            .getElementsByClass("coverage-summary").single()
            .getElementsByTag("tbody").single()
            .getElementsByTag("tr").single()
            .getElementsByTag("td")
        coverageCells.forEach {
            assertThat(expected, isIn(it.classNames()))
        }
    }

    @DataProvider
    fun createHtmlCoverageReportHighlightingTestData(): Array<Array<Any>> {
        return arrayOf(
            arrayOf(
                createCodeCoverageResult(
                    numLocationsNotCovered = 0,
                    numLocations = 0),
                "high"),
            arrayOf(
                createCodeCoverageResult(
                    numLocationsNotCovered = 25,
                    numLocations = 100),
                "high"),
            arrayOf(
                createCodeCoverageResult(
                    numLocationsNotCovered = 26,
                    numLocations = 100),
                "low"),
            arrayOf(
                createCodeCoverageResult(
                    numLocationsNotCovered = 100,
                    numLocations = 100),
                "low"))
    }

    @Test
    fun createHtmlCoverageReport_always_shouldContainFooterWithCreationDate() {
        val expected = LocalDateTime.now()
        val sut = Reporter { expected }

        val report = sut.createHtmlCoverageReport(createRunTestsResult())

        val html = Jsoup.parse(report.toString())
        val actual = html.getElementsByClass("footer").single().ownText()
        assertThat(actual, containsString(expected.toString()))
    }

    @Test
    fun createHtmlCoverageReport_always_shouldEmbedCssFromResources() {
        val sut = Reporter { LocalDateTime.now() }

        val report = sut.createHtmlCoverageReport(createRunTestsResult())

        val html = Jsoup.parse(report.toString())
        val actual = html.getElementsByTag("style").first().data().trim()
        val expected = File(javaClass.classLoader.getResource("coverage-report.css").file)
            .readText().trim()
        assertEquals(actual, expected)
    }
}