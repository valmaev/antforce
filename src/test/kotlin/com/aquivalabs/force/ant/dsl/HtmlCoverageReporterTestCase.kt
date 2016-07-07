package com.aquivalabs.force.ant.dsl

import com.aquivalabs.force.ant.createRunTestsResult
import com.aquivalabs.force.ant.*
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


class HtmlCoverageReporterTestCase {

    val dateTimeProvider: () -> LocalDateTime = { LocalDateTime.MIN }

    @Test(dataProvider = "createReportTestData")
    fun createReport_always_shouldContainTotalCoveragePercentage(
        codeCoverage: Array<CodeCoverageResult>) {
        // Arrange
        val sut = createSystemUnderTest(dateTimeProvider)
        val runTestsResult = createRunTestsResult(codeCoverage = codeCoverage)

        // Act
        val report = sut.createReport(runTestsResult)

        // Assert
        val html = Jsoup.parse(report.toString())
        val actual = html.getElementById("totalCoveragePercentage").ownText()
        val expected = "${runTestsResult.totalCoveragePercentage.format(2)}%"
        assertThat(actual, equalTo(expected))
    }

    @Test(dataProvider = "createReportTestData")
    fun createReport_always_shouldContainTotalLinesCoverage(
        codeCoverage: Array<CodeCoverageResult>) {
        // Arrange
        val sut = createSystemUnderTest(dateTimeProvider)
        val runTestsResult = createRunTestsResult(codeCoverage = codeCoverage)

        // Act
        val report = sut.createReport(runTestsResult)

        // Assert
        val html = Jsoup.parse(report.toString())
        val actual = html.getElementById("totalLinesCoverage").ownText()
        val expected =
            "${runTestsResult.totalNumLocationsCovered}/${runTestsResult.totalNumLocations}"
        assertThat(actual, equalTo(expected))
    }

    @DataProvider
    fun createReportTestData(): Array<Array<Any>> {
        return arrayOf(
            arrayOf<Any>(
                arrayOf(createCodeCoverageResult(
                    name = "ControllerCls",
                    type = "Class",
                    numLocations = 100))))
    }

    @Test(dataProvider = "createReportWarningsTestData")
    fun createReport_always_shouldContainTotalNumberOfCoverageWarnings(
        codeCoverageWarnings: Array<CodeCoverageWarning>?) {
        // Arrange
        val sut = createSystemUnderTest(dateTimeProvider)
        val runTestsResult = createRunTestsResult(codeCoverageWarnings = codeCoverageWarnings)

        // Act
        val report = sut.createReport(runTestsResult)

        // Assert
        val html = Jsoup.parse(report.toString())
        val actual = html.getElementById("totalCoverageWarnings").ownText()
        val expected = "${runTestsResult.codeCoverageWarnings.orEmpty().count()}"
        assertThat(actual, equalTo(expected))
    }

    @DataProvider
    fun createReportWarningsTestData(): Array<Array<Any?>> =
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
    fun createReport_ifCoverageWarningsExist_shouldContainAllOfThem(
        codeCoverageWarnings: Array<CodeCoverageWarning>) {
        // Arrange
        val sut = createSystemUnderTest(dateTimeProvider)
        val runTestsResult = createRunTestsResult(codeCoverageWarnings = codeCoverageWarnings)

        // Act
        val report = sut.createReport(runTestsResult)

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
    fun createReport_ifNoCoverageWarningsExist_shouldNotIncludeThem(
        codeCoverageWarnings: Array<CodeCoverageWarning>?) {
        // Arrange
        val sut = createSystemUnderTest(dateTimeProvider)
        val runTestsResult = createRunTestsResult(codeCoverageWarnings = codeCoverageWarnings)

        // Act
        val report = sut.createReport(runTestsResult)

        // Assert
        val html = Jsoup.parse(report.toString())
        val actual = html.getElementById("coverageWarningsList")
        assertThat(actual, nullValue())
    }

    @Test(dataProvider = "createReportTestData")
    fun createReport_always_shouldContainTableWithAllCoverageResults(
        codeCoverage: Array<CodeCoverageResult>) {
        // Arrange
        val sut = createSystemUnderTest(dateTimeProvider)
        val runTestsResult = createRunTestsResult(codeCoverage = codeCoverage)

        // Act
        val report = sut.createReport(runTestsResult)

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

    @Test(dataProvider = "createReportHighlightingTestData")
    fun createReport_always_shouldProperlySetCssStyleToClassCoverageRow(
        codeCoverageResult: CodeCoverageResult,
        expected: String) {
        // Arrange
        val sut = createSystemUnderTest(dateTimeProvider)
        val runTestsResult = createRunTestsResult(codeCoverage = arrayOf(codeCoverageResult))

        // Act
        val report = sut.createReport(runTestsResult)

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
    fun createReportHighlightingTestData(): Array<Array<Any>> {
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
    fun createReport_always_shouldContainFooterWithCreationDate() {
        val expected = LocalDateTime.now()
        val sut = createSystemUnderTest(dateTimeProvider = { expected })

        val report = sut.createReport(createRunTestsResult())

        val html = Jsoup.parse(report.toString())
        val actual = html.getElementsByClass("footer").single().ownText()
        assertThat(actual, containsString(expected.toString()))
    }

    @Test
    fun createReport_always_shouldEmbedCssFromResources() {
        val sut = createSystemUnderTest(dateTimeProvider = { LocalDateTime.now() })

        val report = sut.createReport(createRunTestsResult())

        val html = Jsoup.parse(report.toString())
        val actual = html.getElementsByTag("style").first().data().trim()
        val expected = File(javaClass.classLoader.getResource("coverage-report.css").file)
            .readText().trim()
        assertEquals(actual, expected)
    }

    fun createSystemUnderTest(dateTimeProvider: () -> LocalDateTime = this.dateTimeProvider) =
        HtmlCoverageReporter(dateTimeProvider)
}