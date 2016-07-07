package com.aquivalabs.force.ant.reporters

import com.aquivalabs.force.ant.reporters.junit.JUnitReportRoot
import com.aquivalabs.force.ant.*
import com.sforce.soap.metadata.RunTestsResult
import java.time.LocalDateTime


class JUnitReporter(
    var suiteName: String = "",
    var properties: Map<String, String>? = null,
    val dateTimeProvider: () -> LocalDateTime = { LocalDateTime.now() }) : Reporter<JUnitReportRoot> {


    override fun createReport(result: RunTestsResult): JUnitReportRoot {

        val report = JUnitReportRoot()
        report.testSuite(
            name = suiteName,
            tests = result.numSuccesses,
            failures = result.numFailures,
            time = result.totalTime / 1000,
            timestamp = dateTimeProvider()) {

            if (properties != null) {
                properties {
                    properties?.forEach { property(name = it.key, value = it.value) }
                }
            }

            result.successes.forEach {
                testCase(
                    classname = it.qualifiedClassName,
                    name = it.methodName,
                    time = it.time / 1000)
            }

            result.failures.forEach {
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