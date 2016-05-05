package com.newmarket.force.ant

import com.newmarket.force.ant.dsl.Classes
import com.newmarket.force.ant.dsl.CoberturaReport
import com.newmarket.force.ant.dsl.JUnitReport
import com.sforce.soap.metadata.CodeCoverageResult
import com.sforce.soap.metadata.RunTestsResult
import java.io.File
import java.time.LocalDateTime


class Reporter(val dateTimeProvider: () -> LocalDateTime) {

    fun createJUnitReport(
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

    fun createCoberturaReport(
        runTestsResult: RunTestsResult,
        projectRootPath: String? = null): CoberturaReport {
        val coverageTypes = runTestsResult.codeCoverage.groupBy { it.type ?: "" }

        val report = CoberturaReport()
        report.coverage {
            sources {
                source { +projectRootPath.orEmpty() }
            }
            packages {
                coverageTypes.forEach { coverageType ->
                    packageTag(name = coverageType.key) {
                        classes {
                            coverageType.value.forEach {
                                createClassTags(it)
                            }
                        }
                    }
                }
            }
        }
        return report
    }

    private fun Classes.createClassTags(
        result: CodeCoverageResult) {
        classTag(
            name = getClassName(result),
            fileName = getClassFileName(result)) {
            lines {
                val notCoveredLines = result.locationsNotCovered.orEmpty().associateBy { it.line }
                for (currentLine in 1..result.numLocations) {
                    val hits =
                        if (notCoveredLines.contains(currentLine))
                            notCoveredLines[currentLine]!!.numExecutions
                        else 1
                    line(number = currentLine, hits = hits)
                }
            }
        }
    }

    private fun getClassName(result: CodeCoverageResult) =
        if (result.namespace == null)
            result.name ?: ""
        else "${result.namespace}.${result.name ?: ""}"

    private fun getClassFileName(
        result: CodeCoverageResult): String =
        if (result.name.isNullOrEmpty())
            ""
        else if (!result.namespace.isNullOrEmpty())
            ""
        else when (result.type) {
            "Class" -> "classes${File.separator}${result.name}.cls"
            "Trigger" -> "triggers${File.separator}${result.name}.cls"
            else -> ""
        }
}