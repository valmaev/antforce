package com.aquivalabs.force.ant.reporters

import com.aquivalabs.force.ant.createRunTestsResult
import com.aquivalabs.force.ant.*
import com.sforce.soap.metadata.*
import kotlinx.html.dom.serialize
import org.hamcrest.core.IsNull.*
import org.hamcrest.core.IsEqual.*
import org.hamcrest.core.StringContains.*
import org.hamcrest.collection.IsIn.*
import org.hamcrest.MatcherAssert.*
import org.jsoup.Jsoup
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import org.testng.Assert.assertEquals
import java.io.File
import java.time.LocalDateTime


class HtmlCoverageReporterTestCase {

    val dateTimeProvider: () -> LocalDateTime = { LocalDateTime.MIN }

    @Test(dataProvider = "createSummaryReportTestData")
    fun createSummaryReport_always_shouldContainTotalLineCoveragePercentage(
        codeCoverage: Array<CodeCoverageResult>,
        codeCoverageWarnings: Array<CodeCoverageWarning>) {
        // Arrange
        val sut = createSystemUnderTest(dateTimeProvider)
        val runTestsResult = createRunTestsResult(
            codeCoverage = codeCoverage,
            codeCoverageWarnings = codeCoverageWarnings)

        // Act
        val report = sut.createSummaryReport(runTestsResult)

        // Assert
        val actual = report.getElementById("totalLineCoveragePercentage").textContent
        val expected = "${runTestsResult.totalCoveragePercentage.format(2)}%"
        assertThat(actual, equalTo(expected))
    }

    @Test(dataProvider = "createSummaryReportTestData")
    fun createSummaryReport_always_shouldContainTotalLineCoverage(
        codeCoverage: Array<CodeCoverageResult>,
        codeCoverageWarnings: Array<CodeCoverageWarning>) {
        // Arrange
        val sut = createSystemUnderTest(dateTimeProvider)
        val runTestsResult = createRunTestsResult(
            codeCoverage = codeCoverage,
            codeCoverageWarnings = codeCoverageWarnings)

        // Act
        val report = sut.createSummaryReport(runTestsResult)

        // Assert
        val actual = report.getElementById("totalLineCoverage").textContent
        val expected = "${runTestsResult.totalNumLocationsCovered}/${runTestsResult.totalNumLocations}"
        assertThat(actual, equalTo(expected))
    }

    @Test(dataProvider = "createSummaryReportTestData")
    fun createSummaryReport_always_shouldContainTotalClassCoveragePercentage(
        codeCoverage: Array<CodeCoverageResult>,
        codeCoverageWarnings: Array<CodeCoverageWarning>) {
        // Arrange
        val sut = createSystemUnderTest(dateTimeProvider)
        val runTestsResult = createRunTestsResult(
            codeCoverage = codeCoverage,
            codeCoverageWarnings = codeCoverageWarnings)

        // Act
        val report = sut.createSummaryReport(runTestsResult)

        // Assert
        val actual = report.getElementById("totalClassCoveragePercentage").textContent
        val expected = "${runTestsResult.classCoveragePercentage.format(2)}%"
        assertThat(actual, equalTo(expected))
    }

    @Test(dataProvider = "createSummaryReportTestData")
    fun createSummaryReport_always_shouldContainTotalClassCoverage(
        codeCoverage: Array<CodeCoverageResult>,
        codeCoverageWarnings: Array<CodeCoverageWarning>) {
        // Arrange
        val sut = createSystemUnderTest(dateTimeProvider)
        val runTestsResult = createRunTestsResult(
            codeCoverage = codeCoverage,
            codeCoverageWarnings = codeCoverageWarnings)

        // Act
        val report = sut.createSummaryReport(runTestsResult)

        // Assert
        val actual = report.getElementById("totalClassCoverage").textContent
        val expected = "${runTestsResult.numClassesCovered}/${runTestsResult.numClasses}"
        assertThat(actual, equalTo(expected))
    }

    @Test(dataProvider = "createSummaryReportTestData")
    fun createSummaryReport_always_shouldContainTotalTriggerCoveragePercentage(
        codeCoverage: Array<CodeCoverageResult>,
        codeCoverageWarnings: Array<CodeCoverageWarning>) {
        // Arrange
        val sut = createSystemUnderTest(dateTimeProvider)
        val runTestsResult = createRunTestsResult(
            codeCoverage = codeCoverage,
            codeCoverageWarnings = codeCoverageWarnings)

        // Act
        val report = sut.createSummaryReport(runTestsResult)

        // Assert
        val actual = report.getElementById("totalTriggerCoveragePercentage").textContent
        val expected = "${runTestsResult.triggerCoveragePercentage.format(2)}%"
        assertThat(actual, equalTo(expected))
    }

    @Test(dataProvider = "createSummaryReportTestData")
    fun createSummaryReport_always_shouldContainTotalTriggerCoverage(
        codeCoverage: Array<CodeCoverageResult>,
        codeCoverageWarnings: Array<CodeCoverageWarning>) {
        // Arrange
        val sut = createSystemUnderTest(dateTimeProvider)
        val runTestsResult = createRunTestsResult(
            codeCoverage = codeCoverage,
            codeCoverageWarnings = codeCoverageWarnings)

        // Act
        val report = sut.createSummaryReport(runTestsResult)

        // Assert
        val actual = report.getElementById("totalTriggerCoverage").textContent
        val expected = "${runTestsResult.numTriggersCovered}/${runTestsResult.numTriggers}"
        assertThat(actual, equalTo(expected))
    }

    @DataProvider
    fun createSummaryReportTestData(): Array<Array<Any?>> {
        return arrayOf(
            arrayOf<Any?>(
                arrayOf(
                    createCodeCoverageResult(
                        name = "Foo",
                        namespace = "nmspc",
                        type = "Trigger",
                        numLocations = 10,
                        numLocationsNotCovered = 0),
                    createCodeCoverageResult(
                        name = "Bar",
                        namespace = "nmspc",
                        type = "Trigger",
                        numLocations = 10,
                        numLocationsNotCovered = 10),
                    createCodeCoverageResult(
                        name = "Baz",
                        namespace = "nmspc",
                        type = "Class",
                        numLocations = 10,
                        numLocationsNotCovered = 0)),
                arrayOf(
                    createCodeCoverageWarning(
                        name = "Foo",
                        namespace = "qwe",
                        message = "Test coverage of selected Apex Trigger is 0%, at least 75% test coverage is required"),
                    createCodeCoverageWarning(
                        name = "Bar",
                        namespace = "qwe",
                        message = "Test coverage of selected Apex Trigger is 12%, at least 75% test coverage is required"),
                    createCodeCoverageWarning(
                        name = "Baz",
                        namespace = "qwe",
                        message = "Test coverage of selected Apex Class is 0%, at least 75% test coverage is required"),
                    createCodeCoverageWarning(
                        name = "Qux",
                        namespace = "qwe"))))
    }

    @Test(dataProvider = "createSummaryReportWarningsTestData")
    fun createSummaryReport_always_shouldContainTotalNumberOfCoverageWarnings(
        codeCoverageWarnings: Array<CodeCoverageWarning>?) {
        // Arrange
        val sut = createSystemUnderTest(dateTimeProvider)
        val runTestsResult = createRunTestsResult(codeCoverageWarnings = codeCoverageWarnings)

        // Act
        val report = sut.createSummaryReport(runTestsResult)

        // Assert
        val actual = report.getElementById("totalCoverageWarnings").textContent
        val expected = "${runTestsResult.codeCoverageWarnings.orEmpty().count()}"
        assertThat(actual, equalTo(expected))
    }

    @DataProvider
    fun createSummaryReportWarningsTestData(): Array<Array<Any?>> =
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
    fun createSummaryReport_ifCoverageWarningsExist_shouldContainAllOfThem(
        codeCoverageWarnings: Array<CodeCoverageWarning>) {
        // Arrange
        val sut = createSystemUnderTest(dateTimeProvider)
        val runTestsResult = createRunTestsResult(codeCoverageWarnings = codeCoverageWarnings)

        // Act
        val report = sut.createSummaryReport(runTestsResult)

        // Assert
        val html = Jsoup.parse(report.serialize())
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
    fun createSummaryReport_ifNoCoverageWarningsExist_shouldNotIncludeThem(
        codeCoverageWarnings: Array<CodeCoverageWarning>?) {
        // Arrange
        val sut = createSystemUnderTest(dateTimeProvider)
        val runTestsResult = createRunTestsResult(codeCoverageWarnings = codeCoverageWarnings)

        // Act
        val report = sut.createSummaryReport(runTestsResult)

        // Assert
        val actual = report.getElementById("coverageWarningsList")
        assertThat(actual, nullValue())
    }

    @Test(dataProvider = "createSummaryReportTestData")
    fun createSummaryReport_always_shouldContainTableWithAllCoverageResults(
        codeCoverage: Array<CodeCoverageResult>,
        codeCoverageWarnings: Array<CodeCoverageWarning>) {
        // Arrange
        val sut = createSystemUnderTest(dateTimeProvider)
        val runTestsResult = createRunTestsResult(
            codeCoverage = codeCoverage,
            codeCoverageWarnings = codeCoverageWarnings)

        // Act
        val report = sut.createSummaryReport(runTestsResult)

        // Assert
        val html = Jsoup.parse(report.serialize())
        val coverageRows = html
            .getElementsByClass("coverage-summary").single()
            .getElementsByTag("tbody").single()
            .getElementsByTag("tr")
        val actual = coverageRows.map {
            val cells = it.getElementsByTag("td")
            CoverageRow(
                type = cells[0].text(),
                className = cells[1].text(),
                linesPercent = it.getElementsByClass("pct").single().text(),
                lines = it.getElementsByClass("abs").single().text())
        }

        val expected = codeCoverage.map {
            CoverageRow(
                type = it.type,
                className = it.qualifiedName,
                linesPercent = "${it.coveragePercentage.format(2)}%",
                lines = "${it.numLocationsCovered}/${it.numLocations}")
        }

        expected.forEach {
            assert(actual.contains(it))
        }
    }

    data class CoverageRow(
        val type: String,
        val className: String,
        val linesPercent: String,
        val lines: String)

    @Test(dataProvider = "createSummaryReportHighlightingTestData")
    fun createSummaryReport_always_shouldProperlySetCssStyleToClassCoverageRow(
        codeCoverageResult: CodeCoverageResult,
        expected: String) {
        // Arrange
        val sut = createSystemUnderTest(dateTimeProvider)
        val runTestsResult = createRunTestsResult(codeCoverage = arrayOf(codeCoverageResult))

        // Act
        val report = sut.createSummaryReport(runTestsResult)

        // Assert
        val html = Jsoup.parse(report.serialize())
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
    fun createSummaryReportHighlightingTestData(): Array<Array<Any>> {
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
    fun createSummaryReport_always_shouldContainFooterWithCreationDate() {
        val expected = LocalDateTime.now()
        val sut = createSystemUnderTest(dateTimeProvider = { expected })

        val report = sut.createSummaryReport(createRunTestsResult())

        val html = Jsoup.parse(report.serialize())
        val actual = html.getElementsByClass("footer").single().ownText()
        assertThat(actual, containsString(expected.toString()))
    }

    @Test
    fun createSummaryReport_always_shouldEmbedCssFromResources() {
        val sut = createSystemUnderTest(dateTimeProvider = { LocalDateTime.now() })

        val report = sut.createSummaryReport(createRunTestsResult())

        val html = Jsoup.parse(report.serialize())
        val actual = html.getElementsByTag("style").first().data().trim()
        val expected = File(javaClass.classLoader.getResource("coverage-report.css").file)
            .readText().trim()
        assertEquals(actual, expected)
    }

    fun createSystemUnderTest(dateTimeProvider: () -> LocalDateTime = this.dateTimeProvider) =
        HtmlCoverageReporter(
            File("foo"),
            File("bar"),
            dateTimeProvider)
}