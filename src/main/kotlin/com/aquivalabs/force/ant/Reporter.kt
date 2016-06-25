package com.aquivalabs.force.ant

import com.aquivalabs.force.ant.dsl.cobertura.Classes
import com.aquivalabs.force.ant.dsl.cobertura.CoberturaReportRoot
import com.aquivalabs.force.ant.dsl.html.BodyTag
import com.aquivalabs.force.ant.dsl.html.HtmlReportRoot
import com.aquivalabs.force.ant.dsl.junit.JUnitReportRoot
import com.sforce.soap.metadata.CodeCoverageResult
import com.sforce.soap.metadata.RunTestsResult
import java.time.LocalDateTime
import java.util.*


class Reporter(
    val dateTimeProvider: () -> LocalDateTime = { LocalDateTime.now() },
    val systemEnvironment: (String) -> String? = { System.getenv(it) }) {

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
            name = result.qualifiedName,
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
            .sortedBy { it.qualifiedName }
            .groupBy { it.type ?: "" }

        var css = ""
        javaClass.getResourceAsStream("/coverage-report.css").reader().use {
            css = it.readText()
        }

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
                                    id = "totalCoveragePercentage"
                                    `class` = "strong"
                                    +"${runTestsResult.totalCoveragePercentage.format(2)}%"
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
                            div {
                                `class` = "fl pad1y space-right2"
                                span {
                                    id = "totalCoverageWarnings"
                                    `class` = "strong"
                                    +"${runTestsResult.codeCoverageWarnings.orEmpty().count()}"
                                }
                                span {
                                    `class` = "quiet"
                                    +"Coverage Warnings"
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
                                                +it.qualifiedName
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

                    createCoverageWarningsList(runTestsResult)
                    createCoverageCalculationNotes()

                    div { `class` = "push"; +"" }
                }

                div {
                    `class` = "footer quiet pad2 space-top1 center small"
                    +"Code coverage generated by AntForce at ${dateTimeProvider()}"
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

    private fun BodyTag.createCoverageWarningsList(result: RunTestsResult) {
        if (result.codeCoverageWarnings.orEmpty().count() == 0)
            return

        div {
            `class` = "pad1"
            h3 { +"Coverage Warnings" }
            ol {
                id = "coverageWarningsList"
                result.codeCoverageWarnings.sortBy { it.qualifiedName }
                result.codeCoverageWarnings.forEach {
                    li {
                        `class` = "coverage-warning"
                        span {
                            `class` = "coverage-warning-name"
                            +it.qualifiedName
                        }
                        +": "
                        span {
                            `class` = "coverage-warning-message"
                            +"${it.message}"
                        }
                    }
                }
            }
        }
    }

    private fun BodyTag.createCoverageCalculationNotes() {
        div {
            `class` = "pad1"
            h3 { +"Coverage Calculation Notes" }
            ul {
                li {
                    +"Since Force.com Metadata API don't provide total number "
                    +"of lines for zero-coverage classes, then total coverage "
                    +"will be inaccurate in this case"
                }
                li {
                    +"To force build failure in such cases use "
                    span {
                        `class` = "inline-code"
                        +"ignoreWarnings=\"false\""
                    }
                    +" attribute"
                }
            }
        }
    }

    fun reportToTeamCity(runTestsResult: RunTestsResult, log: (String) -> Unit) {
        if (systemEnvironment("TEAMCITY_PROJECT_NAME") == null)
            return

        log("##teamcity[message text='Apex Code Coverage is ${runTestsResult.totalCoveragePercentage}%']")
        log("##teamcity[blockOpened name='Apex Code Coverage Summary']")
        log("##teamcity[buildStatisticValue key='CodeCoverageAbsLCovered' value='${runTestsResult.totalNumLocationsCovered}']")
        log("##teamcity[buildStatisticValue key='CodeCoverageAbsLTotal' value='${runTestsResult.totalNumLocations}']")
        log("##teamcity[buildStatisticValue key='CodeCoverageL' value='${runTestsResult.totalCoveragePercentage}']")
        log("##teamcity[blockClosed name='Apex Code Coverage Summary']")
    }
}

fun Double.format(digits: Int) = String.format(Locale.US, "%.${digits}f", this)