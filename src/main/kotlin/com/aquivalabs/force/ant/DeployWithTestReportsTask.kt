package com.aquivalabs.force.ant

import com.salesforce.ant.DeployTaskAdapter
import com.sforce.soap.metadata.AsyncResult
import com.sforce.soap.metadata.MetadataConnection
import com.sforce.soap.metadata.RunTestsResult
import com.sforce.soap.metadata.TestLevel
import java.io.File
import java.time.LocalDateTime


class DeployWithTestReportsTask : DeployTaskAdapter() {
    private var _junitReport: JUnitReport? = null
    private var _coberturaReport: CoberturaReport? = null
    private var _htmlCoverageReport: HtmlCoverageReport? = null

    final val batchTests = java.util.HashSet<BatchTest>()

    val deployRoot: String?
        get() = com.salesforce.ant.DeployTask::class.java.getDeclaredField("deployRoot").accessible {
            return it.get(this) as String?
        }

    var reporter = Reporter() { LocalDateTime.now() }

    var sourceDir: File? = null
    var reportDir: File? = null


    fun addJUnitReport(report: JUnitReport) {
        _junitReport = report
    }

    fun addCoberturaReport(report: CoberturaReport) {
        _coberturaReport = report
    }

    fun addHtmlCoverageReport(report: HtmlCoverageReport) {
        _htmlCoverageReport = report
    }

    fun createBatchTest(): BatchTest {
        val batch = BatchTest(getProject())
        batchTests.add(batch)
        return batch
    }

    override fun getRunTests(): Array<out String>? = when (testLevel) {
        TestLevel.RunSpecifiedTests.name ->
            super.getRunTests().union(batchTests.flatMap { it.getFileNames() }).toTypedArray()
        else -> emptyArray()
    }

    override fun handleResponse(metadataConnection: MetadataConnection?, response: AsyncResult?) {
        val deployResult = metadataConnection!!.checkDeployStatus(response!!.id, true)
        val testResult = deployResult.details.runTestResult
        sourceDir = sourceDir ?: File(deployRoot)
        if (reportDir != null && testLevel != null && testLevel != TestLevel.NoTestRun.name) {
            if (!reportDir!!.exists())
                reportDir!!.mkdirs()

            saveJUnitReportToFile(testResult)
            saveCoberturaReportToFile(testResult)
            saveHtmlCoverageReportToFile(testResult)
        }

        super.handleResponse(metadataConnection, response)
    }

    internal fun saveJUnitReportToFile(testResult: RunTestsResult) {
        if (_junitReport == null)
            return

        val properties = hashMapOf(
            "username" to (username ?: ""),
            "serverURL" to (serverURL ?: ""),
            "apiVersion" to apiVersion.toString())

        val reportContent = reporter.createJUnitReport(
            testResult,
            _junitReport!!.suiteName,
            properties)

        val report = saveToFile(reportDir!!, _junitReport!!.file, reportContent.toString())
        log("JUnit report created successfully: ${report.absolutePath}")
    }

    internal fun saveCoberturaReportToFile(testResult: RunTestsResult) {
        if (_coberturaReport == null)
            return

        val reportContent = reporter.createCoberturaReport(testResult, sourceDir?.path)
        val report = saveToFile(reportDir!!, _coberturaReport!!.file, reportContent.toString())
        log("Cobertura report created successfully: ${report.absolutePath}")
    }

    internal fun saveHtmlCoverageReportToFile(testResult: RunTestsResult) {
        if (_htmlCoverageReport == null)
            return

        val reportContent = reporter.createHtmlCoverageReport(testResult)
        val report = saveToFile(reportDir!!, _htmlCoverageReport!!.file, reportContent.toString())
        log("HTML Coverage report created successfully: ${report.absolutePath}")
    }

    private fun saveToFile(
        directory: File,
        fileName: String,
        fileContent: String): File {

        val reportFile = File("${directory.absolutePath}${File.separator}$fileName")
        reportFile.createNewFile()
        reportFile.writeText(fileContent)
        return reportFile
    }
}