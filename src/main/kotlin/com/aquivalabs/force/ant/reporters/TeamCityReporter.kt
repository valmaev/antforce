package com.aquivalabs.force.ant.reporters

import com.aquivalabs.force.ant.*
import com.sforce.soap.metadata.DeployResult


class TeamCityReporter(
    val systemEnvironment: (String) -> String? = System::getenv,
    val log: (String) -> Unit = ::println) : Reporter<Unit> {

    override fun createReport(deployResult: DeployResult) {
        if (systemEnvironment("TEAMCITY_PROJECT_NAME") == null)
            return

        val testResult = deployResult.details.runTestResult
        log("##teamcity[testSuiteStarted name='Apex']")
        testResult.successes.forEach {
            log("##teamcity[testStarted name='${it.qualifiedClassName.escape()}.${it.methodName.escape()}']")
            log("##teamcity[testFinished " +
                "name='${it.qualifiedClassName.escape()}.${it.methodName.escape()}' " +
                "duration='${it.time / 1000}']")
        }
        testResult.failures.forEach {
            log("##teamcity[testStarted name='${it.qualifiedClassName.escape()}.${it.methodName.escape()}']")
            log("##teamcity[testFailed " +
                "name='${it.qualifiedClassName.escape()}.${it.methodName.escape()}' " +
                "message='${it.message.escape()}' " +
                "details='${it.stackTrace.escape()}' " +
                "type='${it.type.escape()}']")
            log("##teamcity[testFinished " +
                "name='${it.qualifiedClassName.escape()}.${it.methodName.escape()}' " +
                "duration='${it.time / 1000}']")
        }
        log("##teamcity[testSuiteFinished name='Apex' duration='${testResult.totalTime / 1000}']")

        log("##teamcity[message text='Apex Code Coverage is ${testResult.totalCoveragePercentage}%']")
        log("##teamcity[blockOpened name='Apex Code Coverage Summary']")

        CoverageType.CLASS.logCoverageStatistic(
            testResult.numClassesCovered,
            testResult.numClasses,
            testResult.classCoveragePercentage)

        CoverageType.TRIGGER.logCoverageStatistic(
            testResult.numTriggersCovered,
            testResult.numTriggers,
            testResult.triggerCoveragePercentage)

        CoverageType.LINE.logCoverageStatistic(
            testResult.totalNumLocationsCovered,
            testResult.totalNumLocations,
            testResult.totalCoveragePercentage)

        logBuildStatisticValue("CodeCoverageWarningCount", testResult.codeCoverageWarnings.orEmpty().size)

        log("##teamcity[blockClosed name='Apex Code Coverage Summary']")
    }

    private enum class CoverageType { CLASS, TRIGGER, LINE }

    private fun CoverageType.logCoverageStatistic(covered: Int, total: Int, totalPercentage: Double) {
        val type = this.name[0]
        logBuildStatisticValue("CodeCoverageAbs${type}Covered", covered)
        logBuildStatisticValue("CodeCoverageAbs${type}Total", total)
        logBuildStatisticValue("CodeCoverage$type", totalPercentage)
    }

    private fun logBuildStatisticValue(key: String, value: Any) =
        log("##teamcity[buildStatisticValue key='$key' value='$value']")

    private fun String.escape(): String {
        val sb = StringBuilder(this.length * 2)
        this.forEach {
            when (it) {
                '|' -> sb.append("||")
                '\n' -> sb.append("|n")
                '\r' -> sb.append("|r")
                '\'' -> sb.append("|'")
                '[' -> sb.append("|[")
                ']' -> sb.append("|]")
                '\u0085' -> sb.append("|x")
                '\u2028' -> sb.append("|l")
                '\u2029' -> sb.append("|p")
                else -> sb.append(it)
            }
        }
        return sb.toString()
    }
}