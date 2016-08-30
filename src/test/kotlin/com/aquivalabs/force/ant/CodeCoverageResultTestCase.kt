package com.aquivalabs.force.ant

import org.testng.annotations.Test
import org.testng.Assert.*
import org.testng.annotations.DataProvider
import java.io.File


class CodeCoverageResultTestCase {
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

    @Test(dataProvider = "qualifiedNameTestData")
    fun qualifiedName_always_shouldReturnExpectedResult(
        namespace: String?,
        name: String?,
        expected: String) {

        val sut = createCodeCoverageResult(name = name, namespace = namespace)
        assertEquals(sut.qualifiedName, expected)
    }

    @DataProvider
    fun qualifiedNameTestData() = qualifiedNameCommonTestData()

    @Test(dataProvider = "classFileNameTestData")
    fun classFileName_always_shouldReturnExpectedResult(
        namespace: String?,
        name: String?,
        type: String?,
        expected: String) {

        val sut = createCodeCoverageResult(name = name, namespace = namespace, type = type)
        assertEquals(sut.classFileName, expected)
    }

    @DataProvider
    fun classFileNameTestData(): Array<Array<Any?>> = arrayOf(
        arrayOf<Any?>(null, null, null, ""),
        arrayOf<Any?>("", null, null, ""),
        arrayOf<Any?>(null, "", null, ""),
        arrayOf<Any?>(null, null, "", ""),
        arrayOf<Any?>("", "", null, ""),
        arrayOf<Any?>(null, "", "", ""),
        arrayOf<Any?>("", null, "", ""),
        arrayOf<Any?>("", "", "", ""),
        arrayOf<Any?>(null, "MyClass", "Class", "classes${File.separator}MyClass$APEX_CLASS_FILE_EXTENSION"),
        arrayOf<Any?>("", "MyClass", "Class", "classes${File.separator}MyClass$APEX_CLASS_FILE_EXTENSION"),
        arrayOf<Any?>("foo", "MyClass", "Class", ""),
        arrayOf<Any?>(null, "MyTrigger", "Trigger", "triggers${File.separator}MyTrigger$APEX_TRIGGER_FILE_EXTENSION"),
        arrayOf<Any?>("", "MyTrigger", "Trigger", "triggers${File.separator}MyTrigger$APEX_TRIGGER_FILE_EXTENSION"),
        arrayOf<Any?>("foo", "MyTrigger", "Trigger", ""))
}