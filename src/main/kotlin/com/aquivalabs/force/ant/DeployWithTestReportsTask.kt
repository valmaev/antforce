package com.aquivalabs.force.ant

import com.aquivalabs.force.ant.reporters.*
import com.salesforce.ant.DeployTaskAdapter
import com.sforce.soap.metadata.*
import kotlinx.html.dom.serialize
import org.apache.tools.ant.BuildException
import java.io.File


class DeployWithTestReportsTask : DeployTaskAdapter() {
    private var _junitReport: JUnitReport? = null
    private var _coberturaReport: CoberturaReport? = null
    private var _htmlCoverageReport: HtmlCoverageReport? = null

    val batchTests = hashSetOf<BatchTest>()

    var jUnitReporter = JUnitReporter()
    var coberturaReporter = CoberturaCoverageReporter()
    var htmlCoverageReporter = HtmlCoverageReporter()
    var teamCityReporter = TeamCityReporter()

    var sourceDir: File? = null
    var reportDir: File? = null

    var enforceCoverageForAllClasses: Boolean? = false

    internal var coverageTestClassName: String = ""
    internal val needToAddCoverageTestClass: Boolean
        get() = !testLevel.isNullOrBlank()
            && testLevel != TestLevel.NoTestRun.name
            && enforceCoverageForAllClasses == true

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
        TestLevel.RunSpecifiedTests.name -> super.getRunTests()
            .union(batchTests.flatMap { it.getFileNames() })
            .toTypedArray()
        else -> emptyArray()
    }

    override fun setZipBytes() {
        val deployDir = getFileForPath(deployRoot)
        if (deployDir != null && deployDir.exists() && deployDir.isDirectory)
            return addCoverageTestClassToDeployRootPackage(deployDir)

        val zip = getFileForPath(zipFile)
        if (zip != null && zip.exists() && zip.isFile)
            return addCoverageTestClassToZipFilePackage(zip)

        throw BuildException(INVALID_ZIP_ROOT)
    }

    override fun handleResponse(metadataConnection: MetadataConnection?, response: AsyncResult?) = try {
        val deployResult = metadataConnection!!.checkDeployStatus(response!!.id, true)
        val testResult = deployResult.details.runTestResult
        sourceDir = sourceDir ?: when {
            deployRoot.isNullOrEmpty() -> null
            else -> File(deployRoot)
        }

        if (testLevel != null && testLevel != TestLevel.NoTestRun.name) {
            removeCoverageTestClassFrom(testResult)

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

    internal fun removeCoverageTestClassFrom(testResult: RunTestsResult) {
        if (!coverageTestClassName.isNullOrBlank()) {
            testResult.successes = testResult.successes
                .filter { it.name != coverageTestClassName }
                .toTypedArray()
            testResult.numTestsRun -= 1
        }
    }

    internal fun saveJUnitReportToFile(testResult: RunTestsResult) {
        if (_junitReport == null)
            return

        with(jUnitReporter) {
            properties = hashMapOf(
                "username" to (username ?: ""),
                "serverURL" to (serverURL ?: ""),
                "apiVersion" to apiVersion.toString())
            suiteName = _junitReport!!.suiteName
            val report = saveReportToFile(testResult, reportDir!!, _junitReport!!.file)
            log("JUnit Report: ${report.absolutePath}")
        }
    }

    internal fun saveCoberturaReportToFile(testResult: RunTestsResult) {
        if (_coberturaReport == null)
            return

        with(coberturaReporter) {
            projectRootPath = sourceDir?.path
            val report = saveReportToFile(testResult, reportDir!!, _coberturaReport!!.file)
            log("Cobertura Report: ${report.absolutePath}")
        }
    }

    internal fun saveHtmlCoverageReportToFile(testResult: RunTestsResult) {
        if (_htmlCoverageReport == null)
            return

        with(htmlCoverageReporter) {
            val report = saveReportToFile(testResult, reportDir!!, _htmlCoverageReport!!.file) {
                it.serialize(true)
            }
            log("HTML Coverage Report: ${report.absolutePath}")
        }
    }
}