package com.aquivalabs.force.ant

import com.sforce.soap.metadata.CodeCoverageResult
import com.sforce.soap.metadata.RunTestFailure
import com.sforce.soap.metadata.RunTestSuccess
import com.sforce.soap.metadata.RunTestsResult
import java.io.File

fun qualifiedClassName(name: String?, namespace: String?) =
    if (namespace.isNullOrEmpty())
        name ?: ""
    else "$namespace.${name ?: ""}"

val CodeCoverageResult.numLocationsCovered: Int
    get() = numLocations - numLocationsNotCovered
val CodeCoverageResult.coverage: Double
    get() = if (numLocations == 0) 1.0 else numLocationsCovered.toDouble() / numLocations
val CodeCoverageResult.coveragePercentage: Double
    get() = coverage * 100
val CodeCoverageResult.qualifiedClassName: String
    get() = qualifiedClassName(name, namespace)
val CodeCoverageResult.classFileName: String
    get() = if (name.isNullOrEmpty()) ""
    else if (!namespace.isNullOrEmpty()) ""
    else when (type) {
        "Class" -> "classes${File.separator}$name${Constants.APEX_CLASS_FILE_EXTENSION}"
        "Trigger" -> "triggers${File.separator}$name${Constants.APEX_TRIGGER_FILE_EXTENSION}"
        else -> ""
    }

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
    get() = qualifiedClassName(name, namespace)

val RunTestFailure.qualifiedClassName: String
    get() = qualifiedClassName(name, namespace)