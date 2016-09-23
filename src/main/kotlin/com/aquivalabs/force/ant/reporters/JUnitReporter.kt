package com.aquivalabs.force.ant.reporters

import com.aquivalabs.force.ant.reporters.junit.JUnitReportRoot
import com.aquivalabs.force.ant.*
import com.sforce.soap.metadata.DeployResult
import java.io.File
import java.time.LocalDateTime


class JUnitReporter(
    var outputFile: File,
    var suiteName: String = "",
    var properties: Map<String, String>? = null,
    val dateTimeProvider: () -> LocalDateTime = { LocalDateTime.now() }) : Reporter<File> {

    override fun createReport(deployResult: DeployResult) : File {
        val report = createJUnitReport(deployResult)
        outputFile.writeText(report.toString())
        return outputFile
    }

    internal fun createJUnitReport(deployResult: DeployResult): JUnitReportRoot {
        val testResult = deployResult.details.runTestResult
        val report = JUnitReportRoot()
        report.testSuite(
            name = suiteName,
            tests = testResult.numSuccesses,
            failures = testResult.numFailures,
            time = testResult.totalTime / 1000,
            timestamp = dateTimeProvider()) {

            if (properties != null) {
                properties {
                    properties?.forEach { property(name = it.key, value = it.value) }
                }
            }

            testResult.successes.forEach {
                testCase(
                    classname = it.qualifiedClassName,
                    name = it.methodName,
                    time = it.time / 1000)
            }

            testResult.failures.forEach {
                testCase(
                    classname = it.qualifiedClassName,
                    name = it.methodName,
                    time = it.time / 1000) {

                    failure(message = it.message, type = it.type) { +it.stackTrace }
                }
            }
        }
        return report
    }
}