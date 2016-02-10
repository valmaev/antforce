package com.newmarket.force.ant

import com.salesforce.ant.DeployTask
import com.sforce.soap.metadata.AsyncResult
import com.sforce.soap.metadata.MetadataConnection
import com.sforce.soap.metadata.RunTestsResult
import java.io.File
import java.time.LocalDateTime
import java.util.*


public class DeployWithTestReportsTask : DeployTask() {
    internal final val tests = HashSet<BatchTest>()

    public val deployRoot: String?
        get() = DeployTask::class.java.getDeclaredField("deployRoot").accessible {
            return it.get(this) as String?
        }

    public var reporter = Reporter() { LocalDateTime.now()}

    public var junitReportDir: File? = null
    public var junitReportName: String = "TEST-Apex.xml"
    public var junitTestSuiteName: String = "Apex"

    public fun createBatchTest(): BatchTest {
        val batch = BatchTest(getProject())
        tests.add(batch)
        return batch
    }

    public override fun getRunTests(): Array<out String>? {
        val allTests = super.getRunTests().toArrayList()
        tests.forEach { allTests.addAll(it.getFileNames()) }
        return allTests.toTypedArray()
    }

    override fun handleResponse(
        metadataConnection: MetadataConnection?,
        response: AsyncResult?) {

        val deployResult = metadataConnection!!.checkDeployStatus(response!!.id)
        val testResult = deployResult.runTestResult
        saveJUnitReportToFile(testResult)
        super.handleResponse(metadataConnection, response)
    }

    public fun saveJUnitReportToFile(testResult: RunTestsResult) {
        if (junitReportDir == null)
            return

        val properties = hashMapOf(
            "username" to (username ?: ""),
            "serverURL" to (serverURL ?: ""),
            "apiVersion" to apiVersion.toString())

        val reportContent = reporter.createJUnitReport(
            testResult,
            junitTestSuiteName,
            properties)

        val report = saveToFile(junitReportDir!!, junitReportName, reportContent.toString())
        log("JUnit report created successfully: ${report.absolutePath}")
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