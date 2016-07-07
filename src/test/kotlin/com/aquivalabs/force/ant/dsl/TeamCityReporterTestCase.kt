package com.aquivalabs.force.ant.dsl

import com.aquivalabs.force.ant.*
import com.sforce.soap.metadata.*
import org.hamcrest.collection.IsEmptyCollection.*
import org.hamcrest.MatcherAssert.*
import org.testng.annotations.Test
import org.testng.Assert.assertTrue
import org.testng.annotations.DataProvider


class TeamCityReporterTestCase {

    @Test(dataProvider = "createReportWarningsTestData")
    fun createReport_ifTeamCityDetected_shouldLogCoverageMessages(
        codeCoverageWarnings: Array<CodeCoverageWarning>?) {
        // Arrange
        val env = hashMapOf("TEAMCITY_PROJECT_NAME" to "foo")
        val actual = mutableListOf<String>()
        val sut = createSystemUnderTest(
            systemEnvironment = { env[it] },
            log = { actual.add(it) })
        val runTestsResult = createRunTestsResult(codeCoverageWarnings = codeCoverageWarnings)

        // Act
        sut.createReport(runTestsResult)

        // Assert
        assertTrue(
            actual.contains("##teamcity[message text='Apex Code Coverage is ${runTestsResult.totalCoveragePercentage}%']"))
        assertTrue(
            actual.contains("##teamcity[buildStatisticValue key='CodeCoverageAbsLCovered' value='${runTestsResult.totalNumLocationsCovered}']"))
        assertTrue(
            actual.contains("##teamcity[buildStatisticValue key='CodeCoverageAbsLTotal' value='${runTestsResult.totalNumLocations}']"))
        assertTrue(
            actual.contains("##teamcity[buildStatisticValue key='CodeCoverageL' value='${runTestsResult.totalCoveragePercentage}']"))
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

    @Test
    fun createReport_ifTeamCityNotDetected_shouldNotLogAnything() {
        // Arrange
        val actual = mutableListOf<String>()
        val sut = createSystemUnderTest(
            systemEnvironment = { null },
            log = { actual.add(it) })
        val runTestsResult = createRunTestsResult()

        // Act
        sut.createReport(runTestsResult)

        // Assert
        assertThat(actual, empty())
    }

    @Test
    fun createReport_ifTeamCityDetected_shouldLogAboutTestSuite() {
        // Arrange
        val env = hashMapOf("TEAMCITY_PROJECT_NAME" to "foo")
        val actual = mutableListOf<String>()
        val sut = createSystemUnderTest(
            systemEnvironment = { env[it] },
            log = { actual.add(it) })
        val runTestsResult = createRunTestsResult(totalTime = 2000.0)

        // Act
        sut.createReport(runTestsResult)

        // Assert
        assertTrue(actual.contains("##teamcity[testSuiteStarted name='Apex']"))
        assertTrue(
            actual.contains("##teamcity[testSuiteFinished name='Apex' duration='${runTestsResult.totalTime / 1000}']"))
    }

    @Test
    fun createReport_ifTeamCityDetected_shouldLogAboutEachSuccess() {
        // Arrange
        val env = hashMapOf("TEAMCITY_PROJECT_NAME" to "foo")
        val actual = mutableListOf<String>()
        val sut = createSystemUnderTest(
            systemEnvironment = { env[it] },
            log = { actual.add(it) })
        val runTestsResult = createRunTestsResult(successes = arrayOf(
            createRunTestSuccess(namespace = "foo", name = "BookTestClass", methodName = "testMethod", time = 2500.0),
            createRunTestSuccess(namespace = "bar", name = "BarTestClass", methodName = "testAnotherMethod", time = 300.0),
            createRunTestSuccess(name = "BookTestClass", methodName = "testMethod", time = 100.0)))

        // Act
        sut.createReport(runTestsResult)

        // Assert
        runTestsResult.successes.forEach {
            actual.contains("##teamcity[testStarted name='${it.qualifiedClassName}.${it.methodName}']")
            actual.contains("##teamcity[testFinished " +
                "name='${it.qualifiedClassName}.${it.methodName}' " +
                "duration='${it.time / 1000}']")
        }
    }

    @Test
    fun createReport_ifTeamCityDetected_shouldLogAboutEachFailure() {
        // Arrange
        val env = hashMapOf("TEAMCITY_PROJECT_NAME" to "foo")
        val actual = mutableListOf<String>()
        val sut = createSystemUnderTest(
            systemEnvironment = { env[it] },
            log = { actual.add(it) })
        val runTestsResult = createRunTestsResult(failures = arrayOf(
            createRunTestFailure(
                namespace = "foo",
                name = "BookTestClass",
                methodName = "testMethod",
                time = 2500.0,
                type = "Error",
                message = "Exception was thrown",
                stackTrace = "long stacktrace"),
            createRunTestFailure(
                name = "barTestClass",
                methodName = "testAnotherMethod",
                time = 143.0,
                type = "Exception",
                message = "System.Exception was thrown",
                stackTrace = "very long stacktrace")))

        // Act
        sut.createReport(runTestsResult)

        // Assert
        runTestsResult.failures.forEach {
            actual.contains("##teamcity[testStarted name='${it.qualifiedClassName}.${it.methodName}']")
            actual.contains("##teamcity[testFailed " +
                "name='${it.qualifiedClassName}.${it.methodName}' " +
                "message='${it.message}' " +
                "details='${it.stackTrace}' " +
                "type='${it.type}']")
            actual.contains("##teamcity[testFinished " +
                "name='${it.qualifiedClassName}.${it.methodName}' " +
                "duration='${it.time / 1000}']")
        }
    }

    fun createSystemUnderTest(
        systemEnvironment: (String) -> String? = { null },
        log: (String) -> Unit = { println(it) }) =
        TeamCityReporter(systemEnvironment, log)
}