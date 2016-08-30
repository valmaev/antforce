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
internal fun Any.getResourceAsString(name: String) = javaClass.getResourceAsStream(name).reader().use { it.readText() }

class HtmlCoverageReporter(
    val dateTimeProvider: () -> LocalDateTime = { LocalDateTime.now() }) : Reporter<Document> {

    override fun createReport(result: RunTestsResult): Document {
        return createHTMLDocument().html {
            val title = "Code Coverage for Apex code"
            head {
                title(title)
                style("text/css", getResourceAsString("/coverage-report.css"))
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
                script(type = ScriptType.textJavaScript) { +getResourceAsString("/sorter.js") }
            }
        }
    }

    private fun HtmlBlockTag.coverageStatistics(result: RunTestsResult) {
        div("clearfix") {
            coverageStatistic(
                "Class",
                "Classes",
                result.classCoveragePercentage,
                result.numClassesCovered,
                result.numClasses)
            coverageStatistic(
                "Trigger",
                "Triggers",
                result.triggerCoveragePercentage,
                result.numTriggersCovered,
                result.numTriggers)
            coverageStatistic(
                "Line",
                "Lines",
                result.totalCoveragePercentage,
                result.totalNumLocationsCovered,
                result.totalNumLocations)
            div("fl pad1y space-right2") {
                span("strong space-right") {
                    id = "totalCoverageWarnings"
                    +"${result.codeCoverageWarnings.orEmpty().count()}"
                }
                span("quiet") { +"Coverage Warnings" }
            }
        }
    }

    private fun HtmlBlockTag.coverageStatistic(
        singularName: String,
        pluralName: String,
        percentage: Double,
        numCovered: Int,
        numTotal: Int) {
        div("fl pad1y space-right2") {
            span("strong space-right") {
                id = "total${singularName}CoveragePercentage"
                +"${percentage.format(2)}%"
            }
            span("quiet space-right") { +pluralName }
            span("fraction") {
                id = "total${singularName}Coverage"
                +"$numCovered/$numTotal"
            }
        }
    }

    private fun HtmlBlockTag.coverageSummary(result: RunTestsResult) {
        table("coverage-summary") {
            thead {
                tr {
                    th {
                        +"Type"
                        attributes["data-col"] = "type"
                        attributes["data-fmt"] = "string"
                    }
                    th {
                        +"File"
                        attributes["data-col"] = "file"
                        attributes["data-fmt"] = "string"
                    }
                    th(classes = "pic") {
                        attributes["data-col"] = "pic"
                        attributes["data-type"] = "number"
                        attributes["data-fmt"] = "html"
                        attributes["data-html"] = "true"
                    }
                    th(classes = "pct") {
                        +"Lines"
                        attributes["data-col"] = "lines"
                        attributes["data-type"] = "number"
                        attributes["data-fmt"] = "pct"
                    }
                    th(classes = "abs") {
                        attributes["data-col"] = "lines_raw"
                        attributes["data-type"] = "number"
                        attributes["data-fmt"] = "html"
                    }
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
                    td(coverageLevel) {
                        +type
                        attributes["data-value"] = type
                    }
                    td(coverageLevel) {
                        +it.qualifiedName
                        attributes["data-value"] = it.qualifiedName
                    }
                    td("pic $coverageLevel") {
                        coverageChart(it)
                        attributes["data-value"] = it.coveragePercentage.format(2)
                    }
                    td("pct $coverageLevel") {
                        +"${it.coveragePercentage.format(2)}%"
                        attributes["data-value"] = it.coveragePercentage.format(2)
                    }
                    td("abs $coverageLevel") {
                        +"${it.numLocationsCovered}/${it.numLocations}"
                        attributes["data-value"] = it.numLocations.toString()
                    }
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