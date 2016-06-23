package com.aquivalabs.force.ant

import com.sforce.soap.metadata.*
import java.io.File

fun qualifiedName(name: String?, namespace: String?) =
    if (namespace.isNullOrEmpty())
        name ?: ""
    else "$namespace.${name ?: ""}"

val CodeCoverageResult.numLocationsCovered: Int
    get() = numLocations - numLocationsNotCovered
val CodeCoverageResult.coverage: Double
    get() = if (numLocations == 0) 1.0 else numLocationsCovered.toDouble() / numLocations
val CodeCoverageResult.coveragePercentage: Double
    get() = coverage * 100
val CodeCoverageResult.qualifiedName: String
    get() = qualifiedName(name, namespace)
val CodeCoverageResult.classFileName: String
    get() = if (name.isNullOrEmpty()) ""
    else if (!namespace.isNullOrEmpty()) ""
    else when (type) {
        "Class" -> "classes${File.separator}$name${Constants.APEX_CLASS_FILE_EXTENSION}"
        "Trigger" -> "triggers${File.separator}$name${Constants.APEX_TRIGGER_FILE_EXTENSION}"
        else -> ""
    }

val CodeCoverageWarning.qualifiedName: String
    get() = qualifiedName(name, namespace)

val RunTestsResult.numSuccesses: Int
    get() = numTestsRun - numFailures
val RunTestsResult.averageCoverage: Double
    get() = codeCoverage.map { it.coverage }.average()
val RunTestsResult.averageCoveragePercentage: Double
    get() = codeCoverage.map { it.coveragePercentage }.average()
val RunTestsResult.totalNumLocationsCovered: Int
    get() = codeCoverage.map { it.numLocationsCovered }.sum()
val RunTestsResult.totalNumLocationsNotCovered: Int
    get() = codeCoverage.map { it.numLocationsNotCovered }.sum()
val RunTestsResult.totalNumLocations: Int
    get() = codeCoverage.map { it.numLocations }.sum()

val RunTestSuccess.qualifiedClassName: String
    get() = qualifiedName(name, namespace)

val RunTestFailure.qualifiedClassName: String
    get() = qualifiedName(name, namespace)