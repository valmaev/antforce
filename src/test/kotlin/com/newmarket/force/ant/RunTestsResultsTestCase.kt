package com.newmarket.force.ant

import com.sforce.soap.metadata.CodeCoverageResult
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import org.testng.Assert.*

class RunTestsResultsTestCase() {
    @Test(dataProvider = "coverageTestData")
    fun averageCoverage_always_shouldReturnAverageCoverageForAllCodeCoverageResults(
        codeCoverage: Array<CodeCoverageResult>) {

        val sut = createRunTestsResult(codeCoverage = codeCoverage)
        assertEquals(
            sut.averageCoverage,
            sut.codeCoverage.map { it.coverage }.average())
    }

    @Test(dataProvider = "coverageTestData")
    fun averageCoveragePercentage_always_shouldReturnAverageCoveragePercentageForAllCodeCoverageResults(
        codeCoverage: Array<CodeCoverageResult>) {

        val sut = createRunTestsResult(codeCoverage = codeCoverage)
        assertEquals(
            sut.averageCoveragePercentage,
            sut.codeCoverage.map { it.coveragePercentage }.average())
    }

    @Test(dataProvider = "coverageTestData")
    fun totalNumLocationsCovered_always_shouldReturnSumOfNumLocationsCoveredFromAllCodeCoverageResults(
        codeCoverage: Array<CodeCoverageResult>) {

        val sut = createRunTestsResult(codeCoverage = codeCoverage)
        assertEquals(
            sut.totalNumLocationsCovered,
            sut.codeCoverage.map { it.numLocationsCovered }.sum())
    }

    @Test(dataProvider = "coverageTestData")
    fun totalNumLocationsNotCovered_always_shouldReturnSumOfNumLocationsNotCoveredFromAllCodeCoverageResults(
        codeCoverage: Array<CodeCoverageResult>) {

        val sut = createRunTestsResult(codeCoverage = codeCoverage)
        assertEquals(
            sut.totalNumLocationsNotCovered,
            sut.codeCoverage.map { it.numLocationsNotCovered }.sum())
    }

    @Test(dataProvider = "coverageTestData")
    fun totalNumLocations_always_shouldReturnSumOfNumLocationsFromAllCodeCoverageResults(
        codeCoverage: Array<CodeCoverageResult>) {

        val sut = createRunTestsResult(codeCoverage = codeCoverage)
        assertEquals(
            sut.totalNumLocations,
            sut.codeCoverage.map { it.numLocations }.sum())
    }

    @DataProvider
    fun coverageTestData(): Array<Array<Any>> = arrayOf(
        arrayOf<Any>(
            arrayOf<CodeCoverageResult>()),
        arrayOf<Any>(
            arrayOf(
                createCodeCoverageResult(numLocations = 0, numLocationsNotCovered = 0),
                createCodeCoverageResult(numLocations = 1, numLocationsNotCovered = 1))),
        arrayOf<Any>(
            arrayOf(
                createCodeCoverageResult(numLocations = 0, numLocationsNotCovered = 0),
                createCodeCoverageResult(numLocations = 100, numLocationsNotCovered = 56),
                createCodeCoverageResult(numLocations = 11, numLocationsNotCovered = 4),
                createCodeCoverageResult(numLocations = 23, numLocationsNotCovered = 16),
                createCodeCoverageResult(numLocations = 1, numLocationsNotCovered = 1))))
}