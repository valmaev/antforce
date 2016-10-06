package com.aquivalabs.force.ant.reporters

import com.aquivalabs.force.ant.*
import com.sforce.soap.metadata.CodeCoverageResult
import com.sforce.soap.metadata.DeployResult
import com.sforce.soap.metadata.RunTestsResult
import java.time.LocalDateTime
import java.util.*
import kotlinx.html.*
import kotlinx.html.dom.*
import org.w3c.dom.Document
import java.io.File
import java.io.Reader
import java.util.zip.ZipFile


internal fun Double.format(digits: Int) = String.format(Locale.US, "%.${digits}f", this)
internal fun Class<*>.getResourceAsString(name: String) = getResourceAsStream(name).reader().use { it.readText() }

open class HtmlCoverageReporter(
    var sourceDir: File?,
    var outputDir: File,
    var codeHighlighting: Boolean = false,
    val dateTimeProvider: () -> LocalDateTime = LocalDateTime::now) : Reporter<File> {

    private val reportTitle = "Code Coverage for Apex code"

    override fun createReport(deployResult: DeployResult): File {
        if (!outputDir.exists())
            outputDir.mkdirs()

        val testResult = deployResult.details.runTestResult
        val rootReport = createSummaryReport(testResult)
        rootReport.saveToFile("index.html")

        if (sourceDir != null && codeHighlighting)
            createClassCoverageReports(testResult)
        return outputDir
    }

    protected open fun createClassCoverageReports(testResult: RunTestsResult) {
        if (sourceDir?.isDirectory != true)
            return
        testResult.codeCoverage.forEach {
            val file = File(sourceDir, it.classFileName)
            if (file.exists() && file.isFile) {
                file.bufferedReader().use { reader ->
                    val report = createClassCoverageReport(it, reader)
                    report.saveToFile("${it.classFileName}.html")
                }
            }
        }
    }

    private fun createSummaryReport(result: RunTestsResult): Document {
        return htmlDocument(
            statusCssClass = result.totalCoveragePercentage.toTestLevel(),
            summary = {
                h1 { +reportTitle }
                coverageStatistics(result)
            },
            body = {
                div("pad1") {
                    coverageSummary(result)
                    coverageWarningsList(result)
                    coverageCalculationNotes()
                }
            })
    }

    internal fun createClassCoverageReport(
        coverageResult: CodeCoverageResult,
        sourceFileReader: Reader): Document {

        val notCoveredLines = coverageResult.locationsNotCovered.orEmpty().map { it.line }
        return htmlDocument(
            statusCssClass = coverageResult.coveragePercentage.toTestLevel(),
            summary = {
                h1 {
                    a("../index.html") { +"all files" }
                    +" / ${coverageResult.classFileName.replace(File.separator, " / ")}"
                }
                div("clearfix") {
                    coverageStatistic(
                        "Line",
                        "Lines",
                        coverageResult.coveragePercentage,
                        coverageResult.numLocationsCovered,
                        coverageResult.numLocations)
                }
            },
            body = {
                pre {
                    table("coverage") {
                        tbody {
                            tr {
                                td("text") {
                                    pre("prettyprint linenums") {
                                        var index = 0
                                        sourceFileReader.forEachLine {
                                            if (notCoveredLines.contains(++index))
                                                span(classes = "cstat-no") {
                                                    id = "not-covered-line-$index"
                                                    +"$it\n"
                                                }
                                            else
                                                +"$it\n"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            })
    }

    protected fun Document.saveToFile(fileName: String) {
        val reportFile = File(outputDir, fileName)
        reportFile.parentFile.mkdirs()
        reportFile.createNewFile()
        reportFile.writeText(serialize(prettyPrint = false))
    }

    private fun htmlDocument(
        statusCssClass: String,
        summary: DIV.() -> Unit = {},
        body: DIV.() -> Unit = {}): Document {

        return createHTMLDocument().html {
            attributes["lang"] = "en"
            head {
                title(reportTitle)
                style("text/css", javaClass.getResourceAsString("/coverage-report.css"))
                style("text/css", javaClass.getResourceAsString("/prettify.css"))
            }
            body {
                div("wrapper") {
                    div("pad1") { summary() }
                    div("status-line $statusCssClass")
                    body()
                    div("push")
                }
                version()
                script(type = ScriptType.textJavaScript) { +javaClass.getResourceAsString("/sorter.js") }
                script(type = ScriptType.textJavaScript) { +javaClass.getResourceAsString("/prettify.js") }
                script(type = ScriptType.textJavaScript) {
                    +"window.onload = function () { if (typeof prettyPrint === 'function') { prettyPrint(); } };"
                }
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
                        attributes["data-fmt"] = "html"
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
                val coverageLevel = it.coverage.toTestLevel()
                tr {
                    td(coverageLevel) {
                        +type
                        attributes["data-value"] = type
                    }
                    td(coverageLevel) {
                        if (sourceDir == null || !codeHighlighting)
                            +it.qualifiedName
                        else
                            a(href = "${it.classFileName}.html") { +it.qualifiedName }
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

    private fun Double.toTestLevel() = when {
        this < 0 -> ""
        0 <= this && this < 0.75 -> "low"
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
        if (result.codeCoverageWarnings.orEmpty().size == 0)
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
                        span("coverage-warning-message") { +it.message.orEmpty() }
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

class ZipRootHtmlCoverageReporter(
    sourceDir: File?,
    outputDir: File,
    codeHighlighting: Boolean = false,
    dateTimeProvider: () -> LocalDateTime = LocalDateTime::now)
    : HtmlCoverageReporter(sourceDir, outputDir, codeHighlighting, dateTimeProvider) {

    override fun createClassCoverageReports(testResult: RunTestsResult) {
        val zip = ZipFile(sourceDir)
        if (!zip.containsEntry("classes") || !zip.containsEntry("triggers"))
            return

        val coverageMap = testResult.codeCoverage.associateBy { it.classFileName }
        val zipEntries = zip.entries()
        while (zipEntries.hasMoreElements()) {
            val entry = zipEntries.nextElement()
            if (!entry.name.startsWith("classes/")
                && !entry.name.startsWith("triggers/")
                && !entry.name.endsWith(APEX_CLASS_FILE_EXTENSION))
                continue

            val coverage = coverageMap[entry.name] ?: continue

            zip.getInputStream(entry).bufferedReader().use { reader ->
                val report = createClassCoverageReport(coverage, reader)
                report.saveToFile("${coverage.classFileName}.html")
            }
        }
    }
}