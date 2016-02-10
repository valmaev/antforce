package com.newmarket.force.ant

import com.sforce.soap.metadata.CodeCoverageResult
import com.sforce.soap.metadata.RunTestFailure
import com.sforce.soap.metadata.RunTestSuccess
import com.sforce.soap.metadata.RunTestsResult
import org.apache.tools.ant.types.FileSet
import java.io.File

fun createFileSet(directory: File, fileNames: Iterable<String>): FileSet {
    val fileSet = FileSet()
    fileSet.dir = directory
    fileNames.forEach {
        val file = File("${directory.path}${File.separator}$it")
        file.createNewFile()
    }
    return fileSet
}

fun createFileSet(directory: File, vararg filesNames: String) =
    createFileSet(directory, filesNames.asIterable())

fun createRunTestsResult(
    numTestsRun: Int = 0,
    numFailures: Int = 0,
    totalTime: Double = 0.0,
    successes: Array<RunTestSuccess>? = arrayOf(),
    failures: Array<RunTestFailure>? = arrayOf(),
    codeCoverage: Array<CodeCoverageResult>? = arrayOf()): RunTestsResult {

    val result = RunTestsResult()
    result.numTestsRun = numTestsRun
    result.numFailures = numFailures
    result.totalTime = totalTime
    result.successes = successes
    result.failures = failures
    result.codeCoverage = codeCoverage
    return result
}