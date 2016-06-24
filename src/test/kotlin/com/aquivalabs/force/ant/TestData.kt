package com.aquivalabs.force.ant

import com.aquivalabs.force.ant.dsl.junit.Property
import com.aquivalabs.force.ant.dsl.junit.TestCase
import com.sforce.soap.metadata.*
import org.apache.tools.ant.Project
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
    codeCoverage: Array<CodeCoverageResult>? = arrayOf(),
    codeCoverageWarnings: Array<CodeCoverageWarning>? = arrayOf()): RunTestsResult {

    val result = RunTestsResult()
    result.numTestsRun = numTestsRun
    result.numFailures = numFailures
    result.totalTime = totalTime
    result.successes = successes
    result.failures = failures
    result.codeCoverage = codeCoverage
    result.codeCoverageWarnings = codeCoverageWarnings
    return result
}

fun createProject(name: String = "TestProject"): Project {
    val project = Project()
    project.name = name
    return project
}

fun createCodeCoverageResult(
    numLocations: Int = 0,
    numLocationsNotCovered: Int = 0): CodeCoverageResult {
    val result = CodeCoverageResult()
    result.numLocations = numLocations
    result.numLocationsNotCovered = numLocationsNotCovered
    return result
}

fun createCodeCoverageResult(
    name: String? = null,
    namespace: String? = null,
    type: String? = null,
    locationsNotCovered: Array<CodeLocation>? = null,
    numLocations: Int = 0): CodeCoverageResult {
    val result = CodeCoverageResult()
    result.name = name
    result.namespace = namespace
    result.type = type
    result.locationsNotCovered = locationsNotCovered
    result.numLocationsNotCovered = locationsNotCovered?.size ?: 0
    result.numLocations = numLocations
    return result
}

fun createCodeCoverageWarning(
    name: String? = null,
    namespace: String? = null,
    message: String? = null): CodeCoverageWarning {
    val warning = CodeCoverageWarning()
    warning.name = name
    warning.namespace = namespace
    warning.message = message
    return warning
}

fun createCodeLocation(
    line: Int = 0,
    numExecutions: Int = 0): CodeLocation {
    val location = CodeLocation()
    location.line = line
    location.numExecutions = numExecutions
    return location
}

fun createRunTestSuccess(
    namespace: String? = "",
    name: String? = "",
    methodName: String? = "",
    time: Double = 0.0): RunTestSuccess {

    val success = RunTestSuccess()
    success.namespace = namespace
    success.name = name
    success.methodName = methodName
    success.time = time
    return success
}

fun createRunTestFailure(
    namespace: String? = "",
    name: String? = "",
    methodName: String? = "",
    message: String? = "",
    type: String? = "",
    stackTrace: String? = "",
    time: Double = 0.0): RunTestFailure {

    val failure = RunTestFailure()
    failure.namespace = namespace
    failure.name = name
    failure.methodName = methodName
    failure.message = message
    failure.type = type
    failure.stackTrace = stackTrace
    failure.time = time
    return failure
}

fun createTestCase(
    className: String = "",
    name: String = "",
    time: Double = 0.0): TestCase {

    val testCase = TestCase()
    testCase.classname = className
    testCase.name = name
    testCase.time = time
    return testCase
}

fun createProperty(name: String = "", value: String = ""): Property {
    val property = Property()
    property.name = name
    property.value = value
    return property
}

fun qualifiedNameCommonTestData(): Array<Array<Any?>> = arrayOf(
    arrayOf<Any?>(null, null, ""),
    arrayOf<Any?>(null, "", ""),
    arrayOf<Any?>("", null, ""),
    arrayOf<Any?>("", "", ""),
    arrayOf<Any?>("foo", "", "foo."),
    arrayOf<Any?>("", "Class", "Class"),
    arrayOf<Any?>(null, "Class", "Class"),
    arrayOf<Any?>("foo", "Class", "foo.Class"))