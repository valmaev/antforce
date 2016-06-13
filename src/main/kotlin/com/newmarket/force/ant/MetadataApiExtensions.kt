package com.newmarket.force.ant

import com.sforce.soap.metadata.CodeCoverageResult
import com.sforce.soap.metadata.RunTestsResult


val CodeCoverageResult.numLocationsCovered: Int
    get() = numLocations - numLocationsNotCovered
val CodeCoverageResult.coverage: Double
    get() = if (numLocations == 0) 1.0 else numLocationsCovered.toDouble() / numLocations
val CodeCoverageResult.coveragePercentage: Double
    get() = coverage * 100
val CodeCoverageResult.qualifiedClassName: String
    get() = if (namespace == null)
        name ?: ""
    else "${namespace}.${name ?: ""}"

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