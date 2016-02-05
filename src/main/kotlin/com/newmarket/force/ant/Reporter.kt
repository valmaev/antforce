package com.newmarket.force.ant

import com.newmarket.force.ant.dsl.Classes
import com.newmarket.force.ant.dsl.CoberturaReport
import com.newmarket.force.ant.dsl.JUnitReport
import com.sforce.soap.metadata.CodeCoverageResult
import com.sforce.soap.metadata.RunTestsResult
import java.time.LocalDateTime


public class Reporter(val dateTimeProvider: () -> LocalDateTime) {

    public fun createJUnitReport(
        runTestsResult: RunTestsResult,
        suiteName: String = "",
        properties: Map<String, String>? = null): JUnitReport {

        val report = JUnitReport()
        report.testSuite(
            name = suiteName,
            tests = runTestsResult.numTestsRun - runTestsResult.numFailures,
            failures = runTestsResult.numFailures,
            time = runTestsResult.totalTime / 1000,
            timestamp = dateTimeProvider()) {

            if (properties != null) {
                properties {
                    properties.forEach { property(name = it.key, value = it.value) }
                }
            }

            runTestsResult.successes.forEach {
                testCase(
                    className = if (it.namespace == null) it.name else "${it.namespace}.${it.name}",
                    name = it.methodName,
                    time = it.time / 1000)
            }

            runTestsResult.failures.forEach {
                testCase(
                    className = if (it.namespace == null) it.name else "${it.namespace}.${it.name}",
                    name = it.methodName,
                    time = it.time / 1000) {

                    failure(message = it.message, type = it.type) { +it.stackTrace }
                }
            }
        }
        return report
    }

    public fun createCoberturaReport(runTestsResult: RunTestsResult): CoberturaReport {
        val coverageTypes = runTestsResult.codeCoverage.groupBy { it.type ?: "" }

        val report = CoberturaReport()
        report.coverage {
            packages {
                coverageTypes.forEach { coverageType ->
                    packageTag(name = coverageType.key) {
                        classes {
                            coverageType.value.forEach { createClassTags(it) }
                        }
                    }
                }
            }
        }
        return report
    }

    private fun Classes.createClassTags(result: CodeCoverageResult) {
        classTag(
            name = if (result.namespace == null)
                result.name ?: ""
            else "${result.namespace}.${result.name ?: ""}") {
            lines {
                result.locationsNotCovered?.forEach {
                    line(number = it.line, hits = it.numExecutions)
                }
            }
        }
    }
}