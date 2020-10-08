package com.aquivalabs.force.ant.reporters

import com.aquivalabs.force.ant.*
import com.sforce.soap.metadata.*
import org.hamcrest.collection.IsEmptyCollection.*
import org.hamcrest.MatcherAssert.*
import org.testng.annotations.Test
import org.testng.Assert.assertTrue
import org.testng.annotations.DataProvider


class TeamCityReporterTestCase {

    @Test(dataProvider = "createReportTestData")
    fun createReport_ifTeamCityDetected_shouldLogCoverageMessages(
        codeCoverage: Array<CodeCoverageResult>?,
        codeCoverageWarnings: Array<CodeCoverageWarning>?) {
        // Arrange
        val env = hashMapOf("TEAMCITY_PROJECT_NAME" to "foo")
        val actual = mutableListOf<String>()
        val sut = createSystemUnderTest(
            systemEnvironment = { env[it] },
            log = { actual.add(it) })
        val runTestsResult = runTestsResult(
            codeCoverage = codeCoverage,
            codeCoverageWarnings = codeCoverageWarnings)

        // Act
        sut.createReport(deployResult(runTestsResult))

        // Assert
        assertTrue(
            actual.contains("##teamcity[message text='Apex Code Coverage is ${runTestsResult.totalCoveragePercentage}%']"))

        assertTrue(
            actual.contains("##teamcity[buildStatisticValue key='CodeCoverageAbsLCovered' value='${runTestsResult.totalNumLocationsCovered}']"))
        assertTrue(
            actual.contains("##teamcity[buildStatisticValue key='CodeCoverageAbsLTotal' value='${runTestsResult.totalNumLocations}']"))
        assertTrue(
            actual.contains("##teamcity[buildStatisticValue key='CodeCoverageL' value='${runTestsResult.totalCoveragePercentage}']"))

        assertTrue(
            actual.contains("##teamcity[buildStatisticValue key='CodeCoverageAbsCCovered' value='${runTestsResult.numClassesCovered}']"))
        assertTrue(
            actual.contains("##teamcity[buildStatisticValue key='CodeCoverageAbsCTotal' value='${runTestsResult.numClasses}']"))
        assertTrue(
            actual.contains("##teamcity[buildStatisticValue key='CodeCoverageC' value='${runTestsResult.classCoveragePercentage}']"))

        assertTrue(
            actual.contains("##teamcity[buildStatisticValue key='CodeCoverageAbsTCovered' value='${runTestsResult.numTriggersCovered}']"))
        assertTrue(
            actual.contains("##teamcity[buildStatisticValue key='CodeCoverageAbsTTotal' value='${runTestsResult.numTriggers}']"))
        assertTrue(
            actual.contains("##teamcity[buildStatisticValue key='CodeCoverageT' value='${runTestsResult.triggerCoveragePercentage}']"))

        assertTrue(
            actual.contains("##teamcity[buildStatisticValue key='CodeCoverageWarningCount' value='${runTestsResult.codeCoverageWarnings.orEmpty().size}']"))
    }

    @DataProvider
    fun createReportTestData(): Array<Array<Any?>> {
        return arrayOf(
            arrayOf(
                arrayOf<CodeCoverageResult>(),
                arrayOf<CodeCoverageWarning>()),
            arrayOf(
                arrayOf(
                    codeCoverageResult(
                        name = "Foo",
                        namespace = "nmspc",
                        type = "Trigger",
                        numLocations = 10,
                        numLocationsNotCovered = 0),
                    codeCoverageResult(
                        name = "Bar",
                        namespace = "nmspc",
                        type = "Trigger",
                        numLocations = 10,
                        numLocationsNotCovered = 10),
                    codeCoverageResult(
                        name = "Baz",
                        namespace = "nmspc",
                        type = "Class",
                        numLocations = 10,
                        numLocationsNotCovered = 0)),
                arrayOf(
                    codeCoverageWarning(
                        name = "Foo",
                        namespace = "qwe",
                        message = "Test coverage of selected Apex Trigger is 0%, at least 75% test coverage is required"),
                    codeCoverageWarning(
                        name = "Bar",
                        namespace = "qwe",
                        message = "Test coverage of selected Apex Trigger is 12%, at least 75% test coverage is required"),
                    codeCoverageWarning(
                        name = "Baz",
                        namespace = "qwe",
                        message = "Test coverage of selected Apex Class is 0%, at least 75% test coverage is required"),
                    codeCoverageWarning(
                        name = "Qux",
                        namespace = "qwe"))))
    }

    @Test
    fun createReport_ifTeamCityNotDetected_shouldNotLogAnything() {
        // Arrange
        val actual = mutableListOf<String>()
        val sut = createSystemUnderTest(
            systemEnvironment = { null },
            log = { actual.add(it) })
        val runTestsResult = runTestsResult()

        // Act
        sut.createReport(deployResult(runTestsResult))

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
        val runTestsResult = runTestsResult(totalTime = 2000.0)

        // Act
        sut.createReport(deployResult(runTestsResult))

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
        val runTestsResult = runTestsResult(successes = arrayOf(
            runTestSuccess(
                namespace = "foo",
                name = "BookTestClass",
                methodName = "testMethod",
                time = 2500.0),
            runTestSuccess(
                namespace = "bar",
                name = "BarTestClass",
                methodName = "testAnotherMethod",
                time = 300.0),
            runTestSuccess(
                name = "BookTestClass",
                methodName = "testMethod",
                time = 100.0)))

        // Act
        sut.createReport(deployResult(runTestsResult))

        // Assert
        runTestsResult.successes.forEach {
            assertTrue(actual.contains(
                "##teamcity[testStarted " +
                    "name='${it.qualifiedClassName}.${it.methodName}']"))
            assertTrue(actual.contains(
                "##teamcity[testFinished " +
                    "name='${it.qualifiedClassName}.${it.methodName}' " +
                    "duration='${it.time / 1000}']"))
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
        val runTestsResult = runTestsResult(failures = arrayOf(
            runTestFailure(
                namespace = "foo",
                name = "BookTestClass",
                methodName = "testMethod",
                time = 2500.0,
                type = "Error",
                message = "Exception was thrown",
                stackTrace = "long stacktrace"),
            runTestFailure(
                name = "barTestClass",
                methodName = "testAnotherMethod",
                time = 143.0,
                type = "Exception",
                message = "System.Exception was thrown",
                stackTrace = "Class.TestPortalController.constructor_always_shouldSetContactAndAccount: line 16, column 1")))

        // Act
        sut.createReport(deployResult(runTestsResult))

        // Assert
        runTestsResult.failures.forEach {
            assertTrue(actual.contains(
                "##teamcity[testStarted " +
                    "name='${it.qualifiedClassName}.${it.methodName}']"))
            assertTrue(actual.contains(
                "##teamcity[testFailed " +
                    "name='${it.qualifiedClassName}.${it.methodName}' " +
                    "message='${it.message}' " +
                    "details='${it.stackTrace}' " +
                    "type='${it.type}']"))
            assertTrue(actual.contains(
                "##teamcity[testFinished " +
                    "name='${it.qualifiedClassName}.${it.methodName}' " +
                    "duration='${it.time / 1000}']"))
        }
    }

    @Test
    fun createReport_ifTeamCityDetected_shouldEscapeFailureMessages() {
        // See documentation: https://confluence.jetbrains.com/display/TCD10/Build+Script+Interaction+with+TeamCity

        // Arrange
        val env = hashMapOf("TEAMCITY_PROJECT_NAME" to "foo")
        val actual = mutableListOf<String>()
        val sut = createSystemUnderTest(
            systemEnvironment = { env[it] },
            log = { actual.add(it) })
        val beforeEscape = "|\n\r\'[]\u0085\u2028\u2029foobar"
        val afterEscape = "|||n|r|'|[|]|x|l|pfoobar"
        val failure = runTestFailure(
            namespace = beforeEscape,
            name = beforeEscape,
            methodName = beforeEscape,
            type = beforeEscape,
            message = beforeEscape,
            stackTrace = beforeEscape)
        val runTestsResult = runTestsResult(failures = arrayOf(failure))

        // Act
        sut.createReport(deployResult(runTestsResult))

        // Assert
        assertTrue(actual.contains(
            "##teamcity[testStarted name='$afterEscape.$afterEscape.$afterEscape']"))
        assertTrue(actual.contains("##teamcity[testFailed " +
            "name='$afterEscape.$afterEscape.$afterEscape' message='$afterEscape' " +
            "details='$afterEscape' type='$afterEscape']"))
        assertTrue(actual.contains("##teamcity[testFinished " +
            "name='$afterEscape.$afterEscape.$afterEscape' duration='${failure.time / 1000}']"))
    }

    @Test
    fun createReport_ifTeamCityDetected_shouldEscapeSuccessMessages() {
        // See documentation: https://confluence.jetbrains.com/display/TCD10/Build+Script+Interaction+with+TeamCity

        // Arrange
        val env = hashMapOf("TEAMCITY_PROJECT_NAME" to "foo")
        val actual = mutableListOf<String>()
        val sut = createSystemUnderTest(
            systemEnvironment = { env[it] },
            log = { actual.add(it) })
        val beforeEscape = "|\n\r\'[]\u0085\u2028\u2029foobar"
        val afterEscape = "|||n|r|'|[|]|x|l|pfoobar"

        val success = runTestSuccess(
            namespace = beforeEscape,
            name = beforeEscape,
            methodName = beforeEscape)
        val runTestsResult = runTestsResult(successes = arrayOf(success))

        // Act
        sut.createReport(deployResult(runTestsResult))

        // Assert
        assertTrue(actual.contains(
            "##teamcity[testStarted name='$afterEscape.$afterEscape.$afterEscape']"))
        assertTrue(actual.contains("##teamcity[testFinished " +
            "name='$afterEscape.$afterEscape.$afterEscape' duration='${success.time / 1000}']"))
    }

    private fun createSystemUnderTest(
        systemEnvironment: (String) -> String? = { null },
        log: (String) -> Unit = ::println) =
        TeamCityReporter(systemEnvironment, log)
}