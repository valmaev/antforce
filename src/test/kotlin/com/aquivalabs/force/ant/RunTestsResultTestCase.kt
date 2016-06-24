package com.aquivalabs.force.ant

import com.sforce.soap.metadata.CodeCoverageResult
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import org.testng.Assert.*

class RunTestsResultTestCase {
    @Test(dataProvider = "nonEmptyCoverageTestData")
    fun totalCoveragePercentage_ifTotalNumLocationsMoreThan0_shouldReturnAverageCoveragePercentageForAllCodeCoverageResults(
        codeCoverage: Array<CodeCoverageResult>) {

        val sut = createRunTestsResult(codeCoverage = codeCoverage)
        assertEquals(
            sut.totalCoveragePercentage,
            sut.totalNumLocationsCovered.toDouble() * 100 / sut.totalNumLocations)
    }

    @Test
    fun totalCoveragePercentage_ifTotalNumLocationsEquals0_shouldReturn100() {
        val sut = createRunTestsResult()
        assertEquals(sut.totalCoveragePercentage, 100.0)
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
    fun coverageTestData(): Array<Array<Any>> = nonEmptyCoverageTestData().plus(
        arrayOf<Any>(
            arrayOf<CodeCoverageResult>()))

    @DataProvider
    fun nonEmptyCoverageTestData(): Array<Array<Any>> = arrayOf(
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