package com.newmarket.force.ant

import org.testng.annotations.Test
import org.testng.Assert.*
import org.testng.annotations.DataProvider


class CodeCoverageResultExtensionsTestCase() {
    @Test(dataProvider = "numLocationsCoveredTestData")
    fun numLocationsCovered_always_shouldReturnDifferenceBetweenNumLocationsAndNumLocationsNotCovered(
        numLocations: Int,
        numLocationsNotCovered: Int,
        expected: Int) {

        val sut = createCodeCoverageResult(
            numLocations = numLocations,
            numLocationsNotCovered = numLocationsNotCovered)
        assertEquals(sut.numLocationsCovered, expected)
    }

    @DataProvider
    fun numLocationsCoveredTestData(): Array<Array<Any>> = arrayOf(
        arrayOf<Any>(0, 0, 0),
        arrayOf<Any>(1, 1, 0),
        arrayOf<Any>(10, 5, 5))

    @Test
    fun coverage_ifNumLocationsEquals0_shouldReturn1() {
        val sut = createCodeCoverageResult(numLocations = 0)
        assertEquals(sut.coverage, 1.0)
    }

    @Test(dataProvider = "coverageTestData")
    fun coverage_ifNumLocationsNotEquals0_shouldReturnQuotientOfNumLocationsCoveredAndNumLocations(
        numLocations: Int,
        numLocationsNotCovered: Int) {

        val sut = createCodeCoverageResult(
            numLocations = numLocations,
            numLocationsNotCovered = numLocationsNotCovered)
        assertEquals(sut.coverage, sut.numLocationsCovered.toDouble() / sut.numLocations)
    }

    @DataProvider
    fun coverageTestData(): Array<Array<Any>> = arrayOf(
        arrayOf<Any>(1, 1),
        arrayOf<Any>(12, 4),
        arrayOf<Any>(3, 2))

    @Test(dataProvider = "coverageTestData")
    fun coveragePercentage_always_shouldReturnCoverageMultipliedBy100(
        numLocations: Int,
        numLocationsNotCovered: Int) {

        val sut = createCodeCoverageResult(
            numLocations = numLocations,
            numLocationsNotCovered = numLocationsNotCovered)
        assertEquals(sut.coveragePercentage, sut.coverage * 100)
    }
}