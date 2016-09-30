package com.aquivalabs.force.ant

import com.sforce.soap.metadata.*
import java.io.File


fun qualifiedName(name: String?, namespace: String?) =
    if (namespace.isNullOrEmpty())
        name ?: ""
    else "$namespace.${name ?: ""}"

fun coverage(covered: Int, total: Int) = if (total == 0) 1.0 else covered.toDouble() / total


val CodeCoverageResult.numLocationsCovered: Int
    get() = numLocations - numLocationsNotCovered

val CodeCoverageResult.coverage: Double
    get() = coverage(numLocationsCovered, numLocations)

val CodeCoverageResult.coveragePercentage: Double
    get() = coverage * 100

val CodeCoverageResult.qualifiedName: String
    get() = qualifiedName(name, namespace)

val CodeCoverageResult.classFileName: String
    get() = if (name.isNullOrEmpty()) ""
    else when (type) {
        "Class" -> "classes${File.separator}$name$APEX_CLASS_FILE_EXTENSION"
        "Trigger" -> "triggers${File.separator}$name$APEX_TRIGGER_FILE_EXTENSION"
        else -> ""
    }


val CodeCoverageWarning.qualifiedName: String
    get() = qualifiedName(name, namespace)

val RunTestSuccess.qualifiedClassName: String
    get() = qualifiedName(name, namespace)

val RunTestFailure.qualifiedClassName: String
    get() = qualifiedName(name, namespace)


val RunTestsResult.numSuccesses: Int
    get() = numTestsRun - numFailures

// Line Coverage
val RunTestsResult.totalCoverage: Double
    get() = coverage(totalNumLocationsCovered, totalNumLocations)

val RunTestsResult.totalCoveragePercentage: Double
    get() = totalCoverage * 100

val RunTestsResult.totalNumLocationsCovered: Int
    get() = codeCoverage.map { it.numLocationsCovered }.sum()

val RunTestsResult.totalNumLocationsNotCovered: Int
    get() = codeCoverage.map { it.numLocationsNotCovered }.sum()

val RunTestsResult.totalNumLocations: Int
    get() = codeCoverage.map { it.numLocations }.sum()

// Class Coverage
val RunTestsResult.coveredClasses: Set<String>
    get() = coveredTypes("Class")

val RunTestsResult.notCoveredClasses: Set<String>
    get() = notCoveredTypes("Class")

val RunTestsResult.numClasses: Int
    get() = numClassesCovered + numClassesNotCovered

val RunTestsResult.numClassesCovered: Int
    get() = coveredClasses.size

val RunTestsResult.numClassesNotCovered: Int
    get() = notCoveredClasses.size

val RunTestsResult.classCoverage: Double
    get() = coverage(numClassesCovered, numClasses)

val RunTestsResult.classCoveragePercentage: Double
    get() = classCoverage * 100

// Trigger Coverage
val RunTestsResult.coveredTriggers: Set<String>
    get() = coveredTypes("Trigger")

val RunTestsResult.notCoveredTriggers: Set<String>
    get() = notCoveredTypes("Trigger")

val RunTestsResult.numTriggers: Int
    get() = numTriggersCovered + numTriggersNotCovered

val RunTestsResult.numTriggersCovered: Int
    get() = coveredTriggers.size

val RunTestsResult.numTriggersNotCovered: Int
    get() = notCoveredTriggers.size

val RunTestsResult.triggerCoverage: Double
    get() = coverage(numTriggersCovered, numTriggers)

val RunTestsResult.triggerCoveragePercentage: Double
    get() = triggerCoverage * 100

private fun RunTestsResult.coveredTypes(type: String) =
    if (codeCoverage == null) setOf()
    else codeCoverage
        .filter { it.coverage > 0.0 && it.type == type }
        .map { it.qualifiedName }
        .toSet()

private fun RunTestsResult.notCoveredTypes(type: String) =
    if (codeCoverageWarnings == null) setOf()
    else codeCoverageWarnings
        .filter { it.message != null && it.message.contains("$type is 0%", true) }
        .map { it.qualifiedName }
        .toSet()