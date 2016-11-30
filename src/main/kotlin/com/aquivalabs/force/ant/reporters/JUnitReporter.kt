package com.aquivalabs.force.ant.reporters

import com.aquivalabs.force.ant.qualifiedClassName
import com.aquivalabs.force.ant.reporters.junit.JUnitReportRoot
import com.sforce.soap.metadata.DeployResult
import com.sforce.soap.metadata.RunTestFailure
import com.sforce.soap.metadata.RunTestSuccess
import java.io.File
import java.time.LocalDateTime
import java.util.*


abstract class JUnitReporter(
    var properties: Map<String, String>? = null,
    val dateTimeProvider: () -> LocalDateTime = LocalDateTime::now) : Reporter<File> {

    protected class JUnitTestSuite(
        val name: String,
        val successes: HashSet<RunTestSuccess> = HashSet(),
        val failures: HashSet<RunTestFailure> = HashSet())

    protected fun createReport(testSuite: JUnitTestSuite): JUnitReportRoot {
        val successesTime = testSuite.successes
            .fold(0.0, operation = { total, success -> total + success.time })
        val failuresTime = testSuite.failures
            .fold(0.0, operation = { total, failure -> total + failure.time })

        val report = JUnitReportRoot()
        report.testSuite(
            name = testSuite.name,
            tests = testSuite.successes.size,
            failures = testSuite.failures.size,
            time = (successesTime + failuresTime) / 1000,
            timestamp = dateTimeProvider()) {

            if (properties != null) {
                properties {
                    properties?.forEach { property(name = it.key, value = it.value) }
                }
            }

            testSuite.successes.forEach {
                testCase(
                    classname = it.qualifiedClassName,
                    name = it.methodName,
                    time = it.time / 1000)
            }

            testSuite.failures.forEach {
                testCase(
                    classname = it.qualifiedClassName,
                    name = it.methodName,
                    time = it.time / 1000) {

                    failure(message = it.message, type = it.type) { +it.stackTrace }
                }
            }
        }
        return report
    }
}

class SingleSuiteJUnitReporter(
    var outputDir: File,
    var suiteName: String = "Apex",
    properties: Map<String, String>? = null,
    dateTimeProvider: () -> LocalDateTime = LocalDateTime::now)
: JUnitReporter(properties, dateTimeProvider) {

    override fun createReport(deployResult: DeployResult): File {
        if (!outputDir.exists())
            outputDir.mkdirs()

        val report = createReport(
            JUnitTestSuite(
                suiteName,
                deployResult.details.runTestResult.successes.toHashSet(),
                deployResult.details.runTestResult.failures.toHashSet()))

        val file = File(outputDir, "TEST-$suiteName.xml")
        file.writeText(report.toString())
        return outputDir
    }
}

class SuitePerTestClassJUnitReporter(
    var outputDir: File,
    properties: Map<String, String>? = null,
    dateTimeProvider: () -> LocalDateTime = LocalDateTime::now)
: JUnitReporter(properties, dateTimeProvider) {

    override fun createReport(deployResult: DeployResult): File {
        if (!outputDir.exists())
            outputDir.mkdirs()

        val testResult = deployResult.details.runTestResult
        val suites = hashMapOf<String, JUnitTestSuite>()

        testResult.successes.forEach {
            val suite = suites
                .getOrPut(it.qualifiedClassName) { JUnitTestSuite(it.qualifiedClassName) }
            suite.successes.add(it)
        }

        testResult.failures.forEach {
            val suite = suites
                .getOrPut(it.qualifiedClassName) { JUnitTestSuite(it.qualifiedClassName) }
            suite.failures.add(it)
        }

        suites.values.forEach {
            saveSuiteToFile(it, outputDir)
        }
        return outputDir
    }

    private fun saveSuiteToFile(it: JUnitTestSuite, outputDir: File) {
        val report = createReport(it)
        val file = File(outputDir, "TEST-${it.name}.xml")
        file.writeText(report.toString())
    }
}