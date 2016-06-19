package com.aquivalabs.force.ant

import com.aquivalabs.force.ant.dsl.cobertura.Classes
import com.aquivalabs.force.ant.dsl.cobertura.CoberturaReportRoot
import com.aquivalabs.force.ant.dsl.html.BodyTag
import com.aquivalabs.force.ant.dsl.html.HtmlReportRoot
import com.aquivalabs.force.ant.dsl.junit.JUnitReportRoot
import com.sforce.soap.metadata.CodeCoverageResult
import com.sforce.soap.metadata.RunTestsResult
import java.io.File
import java.time.LocalDateTime
import java.util.*


class Reporter(val dateTimeProvider: () -> LocalDateTime) {

    fun createJUnitReport(
        runTestsResult: RunTestsResult,
        suiteName: String = "",
        properties: Map<String, String>? = null): JUnitReportRoot {

        val report = JUnitReportRoot()
        report.testSuite(
            name = suiteName,
            tests = runTestsResult.numSuccesses,
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
                    classname = it.qualifiedClassName,
                    name = it.methodName,
                    time = it.time / 1000)
            }

            runTestsResult.failures.forEach {
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

    fun createCoberturaReport(
        runTestsResult: RunTestsResult,
        projectRootPath: String? = null): CoberturaReportRoot {
        val coverageTypes = runTestsResult.codeCoverage.groupBy { it.type ?: "" }

        val report = CoberturaReportRoot()
        report.coverage {
            sources {
                source { +projectRootPath.orEmpty() }
            }
            packages {
                coverageTypes.forEach { coverageType ->
                    `package`(name = coverageType.key) {
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
        `class`(
            name = result.qualifiedClassName,
            filename = result.classFileName) {
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

    fun createHtmlCoverageReport(runTestsResult: RunTestsResult): HtmlReportRoot {
        val coverageResultsByType = runTestsResult.codeCoverage
            .sortedBy { it.qualifiedClassName }
            .groupBy { it.type ?: "" }

        val css = File(javaClass.classLoader.getResource("coverage-report.css").file).readText()

        val report = HtmlReportRoot()
        report.html {
            val title = "Code Coverage report for Apex code"

            head {
                title { +title }
                style {
                    type = "text/css"

                    +css
                }
            }

            body {
                div {
                    `class` = "wrapper"

                    div {
                        `class` = "pad1"
                        h1 { +title }
                        div {
                            `class` = "clearfix"
                            div {
                                `class` = "fl pad1y space-right2"
                                span {
                                    id = "averageCoveragePercentage"
                                    `class` = "strong"
                                    +"${runTestsResult.averageCoveragePercentage.format(2)}%"
                                }
                                span {
                                    `class` = "quiet"
                                    +"Lines"
                                }
                                span {
                                    id = "totalLinesCoverage"
                                    `class` = "fraction"
                                    +"${runTestsResult.totalNumLocationsCovered}/${runTestsResult.totalNumLocations}"
                                }
                            }
                        }
                    }

                    div { `class` = "status-line medium"; +"" }

                    div {
                        `class` = "pad1"
                        table {
                            `class` = "coverage-summary"
                            thead {
                                th { +"Type" }
                                th { +"File" }
                                th { `class` = "pic" }
                                th {
                                    `class` = "pict"
                                    +"Lines"
                                }
                            }

                            tbody {
                                coverageResultsByType.forEach {
                                    val (type, coverageResults) = it
                                    coverageResults.forEach {
                                        val coverage = it.coverage
                                        val coverageLevel = when {
                                            coverage < 0 -> ""
                                            0 <= coverage && coverage < 0.75 -> "low"
                                            else -> "high"
                                        }

                                        tr {
                                            td {
                                                `class` = coverageLevel
                                                +type
                                            }
                                            td {
                                                `class` = coverageLevel
                                                +it.qualifiedClassName
                                            }
                                            td {
                                                `class` = "pic $coverageLevel"
                                                createCoverageChart(it)
                                            }
                                            td {
                                                `class` = "pct $coverageLevel"
                                                +"${it.coveragePercentage.format(2)}%"
                                            }
                                            td {
                                                `class` = "abs $coverageLevel"
                                                +"${it.numLocationsCovered}/${it.numLocations}"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    div { `class` = "push"; +"" }
                }

                div {
                    `class` = "footer quiet pad2 space-top1 center small"
                    +"Code coverage generated by ForceAntTasks at ${dateTimeProvider()}"
                }
            }
        }
        return report
    }

    private fun BodyTag.createCoverageChart(result: CodeCoverageResult) {
        val coveragePercentage = result.coveragePercentage
        div {
            `class` = "chart"
            div {
                `class` = "cover-fill cover-full"
                style = "width: ${coveragePercentage.format(2)}%;"
                +""
            }
            div {
                `class` = "cover-empty"
                style = "left: ${coveragePercentage.format(2)}%; width: ${(100.0 - coveragePercentage).format(2)}%;"
                +""
            }
        }
    }
}

fun Double.format(digits: Int) = String.format(Locale.US, "%.${digits}f", this)