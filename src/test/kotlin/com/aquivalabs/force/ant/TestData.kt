package com.aquivalabs.force.ant

import com.nhaarman.mockito_kotlin.*
import com.sforce.soap.metadata.*
import org.apache.tools.ant.Project
import org.apache.tools.ant.types.FileSet
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.zip.ZipOutputStream


fun nestedElementConvention(prefix: String) =
    "Prefix '$prefix' is one of the Ant's conventions for nested elements declaration. " +
        "See the manual: http://ant.apache.org/manual/develop.html#nested-elements"

fun fileSet(directory: File, fileNames: Iterable<String>): FileSet {
    val fileSet = FileSet()
    fileSet.dir = directory
    fileNames.forEach {
        val file = File("${directory.path}${File.separator}$it")
        file.createNewFile()
    }
    return fileSet
}

fun fileSet(directory: File, vararg filesNames: String) = fileSet(directory, filesNames.asIterable())

fun deployResult(
    testResult: RunTestsResult = runTestsResult(),
    status: DeployStatus = DeployStatus.Succeeded,
    success: Boolean = true,
    done: Boolean = true): DeployResult {

    val result = DeployResult()
    result.id = UUID.randomUUID().toString()
    result.done = done
    result.details = DeployDetails()
    result.details.runTestResult = testResult
    result.status = status
    result.success = success
    return result
}

fun asyncResult(id: String = randomString(), done: Boolean = true): AsyncResult {
    val result = AsyncResult()
    result.id = id
    result.done = done
    return result
}

fun runTestsResult(
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

fun project(name: String = "TestProject"): Project {
    val project = Project()
    project.name = name
    return project
}

fun codeCoverageResult(
    numLocations: Int = 0,
    numLocationsNotCovered: Int = 0): CodeCoverageResult {
    val result = CodeCoverageResult()
    result.numLocations = numLocations
    result.numLocationsNotCovered = numLocationsNotCovered
    return result
}

fun codeCoverageResult(
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

fun codeCoverageWarning(
    name: String? = null,
    namespace: String? = null,
    message: String? = null): CodeCoverageWarning {
    val warning = CodeCoverageWarning()
    warning.name = name
    warning.namespace = namespace
    warning.message = message
    return warning
}

fun codeLocation(
    line: Int = 0,
    numExecutions: Int = 0): CodeLocation {
    val location = CodeLocation()
    location.line = line
    location.numExecutions = numExecutions
    return location
}

fun Int.toCodeLocation(): CodeLocation = codeLocation(this)

fun runTestSuccess(
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

fun runTestFailure(
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

fun metadataConnection(deployResult: DeployResult = deployResult()): MetadataConnection {
    val connection = mock<MetadataConnection>(withSettings().defaultAnswer(RETURNS_DEEP_STUBS))
    doReturn(deployResult).whenever(connection).checkDeployStatus(any(), any())
    doReturn(asyncResult()).whenever(connection).deploy(any(), any())
    return connection
}

fun qualifiedNameCommonTestData(): Array<Array<Any?>> = arrayOf(
    arrayOf(null, null, ""),
    arrayOf(null, "", ""),
    arrayOf("", null, ""),
    arrayOf("", "", ""),
    arrayOf("foo", "", "foo."),
    arrayOf("", "Class", "Class"),
    arrayOf(null, "Class", "Class"),
    arrayOf("foo", "Class", "foo.Class"))

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
    test: (File) -> Unit) = withTestDirectory { directory ->

    val zip = File(directory, "src.zip")
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