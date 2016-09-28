package com.aquivalabs.force.ant

import com.aquivalabs.force.ant.reporters.junit.Property
import com.aquivalabs.force.ant.reporters.junit.TestCase
import com.sforce.soap.metadata.*
import org.apache.tools.ant.Project
import org.apache.tools.ant.types.FileSet
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.zip.ZipOutputStream

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

fun createDeployResult(
    testResult: RunTestsResult = createRunTestsResult(),
    done: Boolean = true): DeployResult {

    val result = DeployResult()
    result.id = UUID.randomUUID().toString()
    result.done = done
    result.details = DeployDetails()
    result.details.runTestResult = testResult
    return result
}

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
    numLocationsNotCovered: Int = 0,
    numLocations: Int = 0): CodeCoverageResult {
    val result = CodeCoverageResult()
    result.name = name
    result.namespace = namespace
    result.type = type
    result.locationsNotCovered = locationsNotCovered
    result.numLocationsNotCovered = locationsNotCovered?.size ?: numLocationsNotCovered
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

fun Int.toCodeLocation(): CodeLocation = createCodeLocation(this)

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

private fun calleeClass() = Thread.currentThread().stackTrace.first {
    it.fileName != "TestData.kt"
        && it.className != Thread::class.java.name
}

fun withTestDirectory(
    directoryNamePrefix: String = calleeClass()?.className ?: randomString(),
    test: (File) -> Unit) {

    var testDirectory: File? = null
    try {
        testDirectory = createTempDir(directoryNamePrefix)
        test(testDirectory)
    } finally {
        testDirectory?.deleteRecursively()
    }
}

fun withDeployRoot(
    packageXml: String = generateDestructiveChanges("*", 37.0),
    classes: Set<String> = setOf("Foobar"),
    test: (File) -> Unit) = withTestDirectory {

    File(it, "package.xml").appendText(packageXml)
    File(it, "classes").mkdir()
    classes.forEach { className ->
        File(it, "classes/$className$APEX_CLASS_FILE_EXTENSION")
            .appendText("public with sharing class $it { }")
    }
    test(it)
}

fun withZipFile(
    packageXml: String? = generateDestructiveChanges("*", 37.0),
    classes: Set<String>? = setOf("Foobar"),
    triggers: Set<String>? = null,
    test: (File) -> Unit) = withTestDirectory {

    val zip = File(it, "src.zip")
    val fileOutput = FileOutputStream(zip)
    ZipOutputStream(fileOutput).use { zipOutput ->
        if (packageXml != null)
            zipOutput.addEntry("package.xml", packageXml)
        if (classes != null) {
            zipOutput.addEntry("classes", "")
            classes.forEach {
                zipOutput.addEntry(
                    "classes/$it$APEX_CLASS_FILE_EXTENSION",
                    "public with sharing class $it { }")
            }
        }

        if (triggers != null) {
            zipOutput.addEntry("triggers", "")
            triggers.forEach {
                zipOutput.addEntry(
                    "triggers/$it$APEX_TRIGGER_FILE_EXTENSION",
                    "trigger $it on $it (before insert){ }")
            }
        }
    }
    test(zip)
}

fun randomString() = UUID.randomUUID().toString()