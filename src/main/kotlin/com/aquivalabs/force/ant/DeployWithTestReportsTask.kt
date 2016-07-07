package com.aquivalabs.force.ant

import com.aquivalabs.force.ant.reporters.*
import com.salesforce.ant.DeployTaskAdapter
import com.sforce.soap.metadata.AsyncResult
import com.sforce.soap.metadata.MetadataConnection
import com.sforce.soap.metadata.RunTestsResult
import com.sforce.soap.metadata.TestLevel
import java.io.File


class DeployWithTestReportsTask : DeployTaskAdapter() {
    private var _junitReport: JUnitReport? = null
    private var _coberturaReport: CoberturaReport? = null
    private var _htmlCoverageReport: HtmlCoverageReport? = null

    val batchTests = java.util.HashSet<BatchTest>()

    val deployRoot: String?
        get() = com.salesforce.ant.DeployTask::class.java.getDeclaredField("deployRoot").accessible {
            return it.get(this) as String?
        }

    var jUnitReporter = JUnitReporter()
    var coberturaReporter = CoberturaCoverageReporter()
    var htmlCoverageReporter = HtmlCoverageReporter()
    var teamCityReporter = TeamCityReporter()

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

        teamCityReporter.createReport(testResult)
        super.handleResponse(metadataConnection, response)
    }

    internal fun saveJUnitReportToFile(testResult: RunTestsResult) {
        if (_junitReport == null)
            return

        jUnitReporter.properties = hashMapOf(
            "username" to (username ?: ""),
            "serverURL" to (serverURL ?: ""),
            "apiVersion" to apiVersion.toString())
        jUnitReporter.suiteName = _junitReport!!.suiteName

        val report = jUnitReporter.saveReportToFile(testResult, reportDir!!, _junitReport!!.file)
        log("JUnit report created successfully: ${report.absolutePath}")
    }

    internal fun saveCoberturaReportToFile(testResult: RunTestsResult) {
        if (_coberturaReport == null)
            return

        coberturaReporter.projectRootPath = sourceDir?.path
        val report = coberturaReporter.saveReportToFile(testResult, reportDir!!, _coberturaReport!!.file)
        log("Cobertura report created successfully: ${report.absolutePath}")
    }

    internal fun saveHtmlCoverageReportToFile(testResult: RunTestsResult) {
        if (_htmlCoverageReport == null)
            return

        val report = htmlCoverageReporter.saveReportToFile(testResult, reportDir!!, _htmlCoverageReport!!.file)
        log("HTML Coverage report created successfully: ${report.absolutePath}")
    }
}