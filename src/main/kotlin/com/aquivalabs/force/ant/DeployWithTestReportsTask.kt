package com.aquivalabs.force.ant

import com.aquivalabs.force.ant.reporters.*
import com.salesforce.ant.DeployTask
import com.salesforce.ant.DeployTaskAdapter
import com.salesforce.ant.ZipUtil
import com.sforce.soap.metadata.*
import org.apache.tools.ant.BuildException
import java.io.File


class DeployWithTestReportsTask : DeployTaskAdapter() {
    private var _junitReport: JUnitReport? = null
    private var _coberturaReport: CoberturaReport? = null
    private var _htmlCoverageReport: HtmlCoverageReport? = null

    internal var coverageTestClassName: String = ""

    val batchTests = java.util.HashSet<BatchTest>()

    val deployRoot: String?
        get() = DeployTask::class.java.getDeclaredField("deployRoot").accessible {
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

    override fun setZipBytes() {
        val deployDir = getFileForPath(deployRoot)
        if (deployDir != null && deployDir.exists() && deployDir.isDirectory) return when {
            testLevel != null && testLevel != TestLevel.NoTestRun.name ->
                addCoverageTestClassToDeployRootPackage(deployDir)
            else -> setZipBytesField(ZipUtil.zipRoot(deployDir))
        }

        val zip = getFileForPath(zipFile)
        if (zip != null && zip.exists() && zip.isFile) return when {
            testLevel != null && testLevel != TestLevel.NoTestRun.name ->
                addCoverageTestClassToZipFilePackage(zip)
            else -> setZipBytesField(ZipUtil.readZip(zip))
        }

        throw BuildException("Should provide a valid directory 'deployRoot' or a zip file 'zipFile'.")
    }

    override fun handleResponse(metadataConnection: MetadataConnection?, response: AsyncResult?) = try {
        val deployResult = metadataConnection!!.checkDeployStatus(response!!.id, true)
        val testResult = deployResult.details.runTestResult
        sourceDir = sourceDir ?: when {
            deployRoot.isNullOrEmpty() -> null
            else -> File(deployRoot)
        }

        if (testLevel != null && testLevel != TestLevel.NoTestRun.name) {
            teamCityReporter.createReport(testResult)

            if (reportDir != null) {
                if (!reportDir!!.exists())
                    reportDir!!.mkdirs()

                saveJUnitReportToFile(testResult)
                saveCoberturaReportToFile(testResult)
                saveHtmlCoverageReportToFile(testResult)
            }
        }

        super.handleResponse(metadataConnection, response)
    } finally {
        removeCoverageTestClassFromOrg(metadataConnection!!)
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
