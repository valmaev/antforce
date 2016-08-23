package com.aquivalabs.force.ant.reporters

import com.aquivalabs.force.ant.*
import com.sforce.soap.metadata.CodeCoverageResult
import com.sforce.soap.metadata.RunTestsResult
import java.time.LocalDateTime
import java.util.*
import kotlinx.html.*
import kotlinx.html.dom.*
import org.w3c.dom.Document

internal fun Double.format(digits: Int) = String.format(Locale.US, "%.${digits}f", this)

class HtmlCoverageReporter(
    val dateTimeProvider: () -> LocalDateTime = { LocalDateTime.now() }) : Reporter<Document> {

    override fun createReport(result: RunTestsResult): Document {
        val css = javaClass.getResourceAsStream("/coverage-report.css").reader().use { it.readText() }
        return createHTMLDocument().html {
            val title = "Code Coverage for Apex code"
            head {
                title(title)
                style("text/css", css)
            }
            body {
                div("wrapper") {
                    div("pad1") {
                        h1 { +title }
                        coverageStatistics(result)
                    }
                    div("status-line medium")
                    div("pad1") {
                        coverageSummary(result)
                        coverageWarningsList(result)
                        coverageCalculationNotes()
                    }
                    div("push")
                }
                version()
            }
        }
    }

    private fun HtmlBlockTag.coverageStatistics(result: RunTestsResult) {
        div("clearfix") {
            div("fl pad1y space-right2") {
                span("strong") {
                    id = "totalCoveragePercentage"
                    +"${result.totalCoveragePercentage.format(2)}%"
                }
                span("quiet") { +"Lines" }
                span("fraction") {
                    id = "totalLinesCoverage"
                    +"${result.totalNumLocationsCovered}/${result.totalNumLocations}"
                }
            }
            div("fl pad1y space-right2") {
                span("strong") {
                    id = "totalCoverageWarnings"
                    +"${result.codeCoverageWarnings.orEmpty().count()}"
                }
                span("quiet") { +"Coverage Warnings" }
            }
        }
    }

    private fun HtmlBlockTag.coverageSummary(result: RunTestsResult) {
        table("coverage-summary") {
            thead {
                tr {
                    th { +"Type" }
                    th { +"File" }
                    th(classes = "pic")
                    th(classes = "pict") { +"Lines" }
                }
                tbody { coverageRows(result) }
            }
        }
    }

    private fun TBODY.coverageRows(result: RunTestsResult) {
        val coverageResultsByType = result.codeCoverage
            .sortedBy { it.qualifiedName }
            .groupBy { it.type ?: "" }

        coverageResultsByType.forEach {
            val (type, coverageResults) = it
            coverageResults.forEach {
                val coverageLevel = it.toTestLevel()
                tr {
                    td(coverageLevel) { +type }
                    td(coverageLevel) { +it.qualifiedName }
                    td("pic $coverageLevel") { coverageChart(it) }
                    td("pct $coverageLevel") { +"${it.coveragePercentage.format(2)}%" }
                    td("abs $coverageLevel") { +"${it.numLocationsCovered}/${it.numLocations}" }
                }
            }
        }
    }

    private fun CodeCoverageResult.toTestLevel() = when {
        coverage < 0 -> ""
        0 <= coverage && coverage < 0.75 -> "low"
        else -> "high"
    }

    private fun HtmlBlockTag.coverageChart(it: CodeCoverageResult) {
        val coveragePercentage = it.coveragePercentage
        div("chart") {
            div("cover-fill cover-full") {
                style = "width: ${coveragePercentage.format(2)}%;"
            }
            div("cover-empty") {
                style = "left: ${coveragePercentage.format(2)}%; " +
                    "width: ${(100.0 - coveragePercentage).format(2)}%;"
            }
        }
    }

    private fun HtmlBlockTag.coverageWarningsList(result: RunTestsResult) {
        if (result.codeCoverageWarnings.orEmpty().count() == 0)
            return

        div("pad1") {
            h3 { +"Coverage Warnings" }
            ol {
                id = "coverageWarningsList"
                result.codeCoverageWarnings.sortBy { it.qualifiedName }
                result.codeCoverageWarnings.forEach {
                    li("coverage-warning") {
                        span("coverage-warning-name") { +it.qualifiedName }
                        +": "
                        span("coverage-warning-message") { +"${it.message}" }
                    }
                }
            }
        }
    }

    private fun HtmlBlockTag.coverageCalculationNotes() {
        div("pad1") {
            h3 { +"Coverage Calculation Notes" }
            ul {
                li {
                    +"Since Force.com Metadata API don't provide total number "
                    +"of lines for zero-coverage classes, then total coverage "
                    +"will be inaccurate in this case"
                }
                li {
                    +"To force build failure in such cases use "
                    span("inline-code") { +"ignoreWarnings=\"false\"" }
                    +" attribute"
                }
                li {
                    +"To overcome the issue use "
                    span("inline-code") { +"enforceCoverageForAllClasses=\"true\"" }
                    +" attribute"
                }
            }
        }
    }

    private fun HtmlBlockTag.version() {
        div("footer quiet pad2 space-top1 center small") {
            val name = javaClass.`package`.implementationTitle
            val version = javaClass.`package`.implementationVersion
            +"Code coverage generated by $name $version at ${dateTimeProvider()}"
        }
    }
}