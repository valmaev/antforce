package com.aquivalabs.force.ant

import com.aquivalabs.force.ant.reporters.*
import com.salesforce.ant.DeployTask
import com.salesforce.ant.DeployTaskAdapter
import com.sforce.soap.metadata.*
import org.apache.tools.ant.BuildException
import java.io.File


data class JUnitReport(var dir: String = "", var suiteName: String = "Apex", var suiteStrategy: String = "single")
data class CoberturaReport(var file: String = "Apex-Coverage.xml")
data class HtmlCoverageReport(var dir: String = "", var codeHighlighting: Boolean = false)

open class DeployWithTestReportsTask : DeployTaskAdapter() {
    internal val fileReporters = hashMapOf<String, Reporter<File>>()
    internal val consoleReporters = hashMapOf<String, Reporter<Unit>>(
        "TeamCity" to TeamCityReporter())

    fun getDeployRoot(): String? = DeployTask::class.java.getDeclaredFieldValue(this, "deployRoot")
    var zipBytesField: ByteArray
        get() = DeployTask::class.java.getDeclaredFieldValue(this, "zipBytes") ?: ByteArray(0)
        set(value) = DeployTask::class.java.setDeclaredFieldValue(this, "zipBytes", value)

    val batchTests = hashSetOf<BatchTest>()

    var reportDir: File? = null
    var sourceDir: File? = null
        get() =
        if (field != null) field
        else if (getDeployRoot().isNullOrEmpty()) null
        else getFileForPath(getDeployRoot())
        set(value) { field = value }

    var enforceCoverageForAllClasses: Boolean? = false

    internal var coverageTestClassName: String = ""
    val needToAddCoverageTestClass: Boolean
        get() = !testLevel.isNullOrBlank()
            && testLevel != TestLevel.NoTestRun.name
            && enforceCoverageForAllClasses == true

    fun addConfiguredJUnitReport(report: JUnitReport) {
        val properties = hashMapOf(
            "username" to (username ?: ""),
            "serverURL" to (serverURL ?: ""),
            "apiVersion" to apiVersion.toString())
        when (report.suiteStrategy.toLowerCase()) {
            "single" -> fileReporters["JUnit"] = SingleSuiteJUnitReporter(
                outputDir = File(reportDir, report.dir),
                suiteName = report.suiteName,
                properties = properties)
            "onepertestclass" -> fileReporters["JUnit"] = SuitePerTestClassJUnitReporter(
                outputDir = File(reportDir, report.dir),
                properties = properties)
        }
    }

    fun addConfiguredCoberturaReport(report: CoberturaReport) {
        fileReporters["Cobertura"] = CoberturaCoverageReporter(
            outputFile = File(reportDir, report.file),
            projectRootPath = sourceDir?.path)
    }

    fun addConfiguredHtmlCoverageReport(report: HtmlCoverageReport) {
        if (sourceDir != null)
            fileReporters["HtmlCoverage"] = HtmlCoverageReporter(
                sourceDir = sourceDir,
                outputDir = File(reportDir, report.dir),
                codeHighlighting = report.codeHighlighting)
        else if (zipFile != null)
            fileReporters["HtmlCoverage"] = ZipRootHtmlCoverageReporter(
                sourceDir = getFileForPath(zipFile),
                outputDir = File(reportDir, report.dir),
                codeHighlighting = report.codeHighlighting)
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
        val deployDir = getFileForPath(getDeployRoot())
        if (deployDir != null && deployDir.exists() && deployDir.isDirectory)
            return addCoverageTestClassToDeployRootPackage(deployDir)

        val zip = getFileForPath(zipFile)
        if (zip != null && zip.exists() && zip.isFile)
            return addCoverageTestClassToZipFilePackage(zip)

        throw BuildException(INVALID_ZIP_ROOT)
    }

    override fun handleResponse(metadataConnection: MetadataConnection, response: AsyncResult) = try {
        if (testLevel != null && testLevel != TestLevel.NoTestRun.name) {
            val deployResult = metadataConnection.checkDeployStatus(response.id, true)
            removeCoverageTestClassFrom(deployResult.details.runTestResult)

            consoleReporters.values.forEach { it.createReport(deployResult) }

            if (reportDir != null) {
                if (!reportDir!!.exists())
                    reportDir!!.mkdirs()

                fileReporters.forEach { type, reporter ->
                    val file = reporter.createReport(deployResult)
                    log("$type Report: ${file.absolutePath}")
                }
            }
        }
        super.handleResponse(metadataConnection, response)
    } finally {
        removeCoverageTestClassFromOrg()
    }

    internal fun removeCoverageTestClassFrom(testResult: RunTestsResult) {
        if (!coverageTestClassName.isNullOrBlank()) {
            testResult.successes = testResult.successes
                .filter { it.name != coverageTestClassName }
                .toTypedArray()
            testResult.numTestsRun -= 1
        }
    }
}