package com.newmarket.force.ant

import com.salesforce.ant.DeployTask
import com.sforce.soap.metadata.AsyncResult
import com.sforce.soap.metadata.MetadataConnection
import com.sforce.soap.metadata.RunTestsResult
import java.io.File
import java.time.LocalDateTime
import java.util.*


class DeployWithTestReportsTask : DeployTask() {
    final val batchTests = HashSet<BatchTest>()

    val deployRoot: String?
        get() = DeployTask::class.java.getDeclaredField("deployRoot").accessible {
            return it.get(this) as String?
        }

    var reporter = Reporter() { LocalDateTime.now() }

    var sourceDir: File? = null
    var reportDir: File? = null
    var junitReportName: String = "TEST-Apex.xml"
    var junitTestSuiteName: String = "Apex"
    var coberturaReportName: String = "Apex-Coverage.xml"

    fun createBatchTest(): BatchTest {
        val batch = BatchTest(getProject())
        batchTests.add(batch)
        return batch
    }

    override fun getRunTests(): Array<out String>? {
        val allTests = super.getRunTests().toMutableList()
        batchTests.forEach { allTests.addAll(it.getFileNames()) }
        return allTests.toTypedArray()
    }

    override fun handleResponse(
        metadataConnection: MetadataConnection?,
        response: AsyncResult?) {

        val deployResult = metadataConnection!!.checkDeployStatus(response!!.id)
        val testResult = deployResult.runTestResult
        sourceDir = sourceDir ?: File(deployRoot)

        if (reportDir != null) {
            if (!reportDir!!.exists())
                reportDir!!.mkdirs()

            saveJUnitReportToFile(testResult)
            saveCoberturaReportToFile(testResult)
        }

        super.handleResponse(metadataConnection, response)
    }

    internal fun saveJUnitReportToFile(testResult: RunTestsResult) {
        val properties = hashMapOf(
            "username" to (username ?: ""),
            "serverURL" to (serverURL ?: ""),
            "apiVersion" to apiVersion.toString())

        val reportContent = reporter.createJUnitReport(
            testResult,
            junitTestSuiteName,
            properties)

        val report = saveToFile(reportDir!!, junitReportName, reportContent.toString())
        log("JUnit report created successfully: ${report.absolutePath}")
    }

    internal fun saveCoberturaReportToFile(testResult: RunTestsResult) {
        val reportContent = reporter.createCoberturaReport(testResult, sourceDir?.path)
        val report = saveToFile(reportDir!!, coberturaReportName, reportContent.toString())
        log("Cobertura report created successfully: ${report.absolutePath}")
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