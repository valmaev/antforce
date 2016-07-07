package com.aquivalabs.force.ant.dsl

import com.aquivalabs.force.ant.qualifiedClassName
import com.aquivalabs.force.ant.totalCoveragePercentage
import com.aquivalabs.force.ant.totalNumLocations
import com.aquivalabs.force.ant.totalNumLocationsCovered
import com.sforce.soap.metadata.RunTestsResult


class TeamCityReporter(
    val systemEnvironment: (String) -> String? = { System.getenv(it) },
    val log: (String) -> Unit = { println(it) }) : Reporter<Unit> {

    override fun createReport(result: RunTestsResult) {
        if (systemEnvironment("TEAMCITY_PROJECT_NAME") == null)
            return

        log("##teamcity[testSuiteStarted name='Apex']")
        result.successes.forEach {
            log("##teamcity[testStarted name='${it.qualifiedClassName}.${it.methodName}']")
            log("##teamcity[testFinished " +
                "name='${it.qualifiedClassName}.${it.methodName}' " +
                "duration='${it.time / 1000}']")
        }
        result.failures.forEach {
            log("##teamcity[testStarted name='${it.qualifiedClassName}.${it.methodName}']")
            log("##teamcity[testFailed " +
                "name='${it.qualifiedClassName}.${it.methodName}' " +
                "message='${it.message}' " +
                "details='${it.stackTrace}' " +
                "type='${it.type}']")
            log("##teamcity[testFinished " +
                "name='${it.qualifiedClassName}.${it.methodName}' " +
                "duration='${it.time / 1000}']")
        }
        log("##teamcity[testSuiteFinished name='Apex' duration='${result.totalTime / 1000}']")

        log("##teamcity[message text='Apex Code Coverage is ${result.totalCoveragePercentage}%']")
        log("##teamcity[blockOpened name='Apex Code Coverage Summary']")
        log("##teamcity[buildStatisticValue key='CodeCoverageAbsLCovered' " +
            "value='${result.totalNumLocationsCovered}']")
        log("##teamcity[buildStatisticValue key='CodeCoverageAbsLTotal' " +
            "value='${result.totalNumLocations}']")
        log("##teamcity[buildStatisticValue key='CodeCoverageL' " +
            "value='${result.totalCoveragePercentage}']")
        log("##teamcity[blockClosed name='Apex Code Coverage Summary']")
    }
}