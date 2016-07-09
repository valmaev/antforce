package com.aquivalabs.force.ant.reporters

import com.aquivalabs.force.ant.*
import com.aquivalabs.force.ant.reporters.html.BodyTag
import com.aquivalabs.force.ant.reporters.html.HtmlReportRoot
import com.sforce.soap.metadata.CodeCoverageResult
import com.sforce.soap.metadata.RunTestsResult
import java.time.LocalDateTime


class HtmlCoverageReporter(
    val dateTimeProvider: () -> LocalDateTime = { LocalDateTime.now() }) : Reporter<HtmlReportRoot> {

    override fun createReport(result: RunTestsResult): HtmlReportRoot {
        val coverageResultsByType = result.codeCoverage
            .sortedBy { it.qualifiedName }
            .groupBy { it.type ?: "" }

        var css = ""
        javaClass.getResourceAsStream("/coverage-report.css").reader().use {
            css = it.readText()
        }

        val report = HtmlReportRoot()
        report.html {
            val title = "Code Coverage for Apex code"

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
                                    +"${result.totalCoveragePercentage.format(2)}%"
                                }
                                span {
                                    `class` = "quiet"
                                    +"Lines"
                                }
                                span {
                                    id = "totalLinesCoverage"
                                    `class` = "fraction"
                                    +"${result.totalNumLocationsCovered}/${result.totalNumLocations}"
                                }
                            }
                            div {
                                `class` = "fl pad1y space-right2"
                                span {
                                    id = "totalCoverageWarnings"
                                    `class` = "strong"
                                    +"${result.codeCoverageWarnings.orEmpty().count()}"
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

                    createCoverageWarningsList(result)
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
}