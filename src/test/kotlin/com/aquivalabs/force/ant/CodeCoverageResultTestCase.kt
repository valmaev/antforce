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

        val sut = codeCoverageResult(
            numLocations = numLocations,
            numLocationsNotCovered = numLocationsNotCovered)
        assertEquals(sut.numLocationsCovered, expected)
    }

    @DataProvider
    fun numLocationsCoveredTestData(): Array<Array<Any>> = arrayOf(
        arrayOf(0, 0, 0),
        arrayOf(1, 1, 0),
        arrayOf(10, 5, 5))

    @Test
    fun coverage_ifNumLocationsEquals0_shouldReturn1() {
        val sut = codeCoverageResult(numLocations = 0)
        assertEquals(sut.coverage, 1.0)
    }

    @Test(dataProvider = "coverageTestData")
    fun coverage_ifNumLocationsNotEquals0_shouldReturnQuotientOfNumLocationsCoveredAndNumLocations(
        numLocations: Int,
        numLocationsNotCovered: Int) {

        val sut = codeCoverageResult(
            numLocations = numLocations,
            numLocationsNotCovered = numLocationsNotCovered)
        assertEquals(sut.coverage, sut.numLocationsCovered.toDouble() / sut.numLocations)
    }

    @DataProvider
    fun coverageTestData(): Array<Array<Any>> = arrayOf(
        arrayOf(1, 1),
        arrayOf(12, 4),
        arrayOf(3, 2))

    @Test(dataProvider = "coverageTestData")
    fun coveragePercentage_always_shouldReturnCoverageMultipliedBy100(
        numLocations: Int,
        numLocationsNotCovered: Int) {

        val sut = codeCoverageResult(
            numLocations = numLocations,
            numLocationsNotCovered = numLocationsNotCovered)
        assertEquals(sut.coveragePercentage, sut.coverage * 100)
    }

    @Test(dataProvider = "qualifiedNameTestData")
    fun qualifiedName_always_shouldReturnExpectedResult(
        namespace: String?,
        name: String?,
        expected: String) {

        val sut = codeCoverageResult(name = name, namespace = namespace)
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

        val sut = codeCoverageResult(name = name, namespace = namespace, type = type)
        assertEquals(sut.classFileName, expected)
    }

    @DataProvider
    fun classFileNameTestData(): Array<Array<Any?>> = arrayOf(
        arrayOf(null, null, null, ""),
        arrayOf("", null, null, ""),
        arrayOf(null, "", null, ""),
        arrayOf(null, null, "", ""),
        arrayOf("", "", null, ""),
        arrayOf(null, "", "", ""),
        arrayOf("", null, "", ""),
        arrayOf("", "", "", ""),
        arrayOf(null, "MyClass", "Class", "classes${File.separator}MyClass$APEX_CLASS_FILE_EXTENSION"),
        arrayOf("", "MyClass", "Class", "classes${File.separator}MyClass$APEX_CLASS_FILE_EXTENSION"),
        arrayOf("foo", "MyClass", "Class", "classes${File.separator}MyClass$APEX_CLASS_FILE_EXTENSION"),
        arrayOf(null, "MyTrigger", "Trigger", "triggers${File.separator}MyTrigger$APEX_TRIGGER_FILE_EXTENSION"),
        arrayOf("", "MyTrigger", "Trigger", "triggers${File.separator}MyTrigger$APEX_TRIGGER_FILE_EXTENSION"),
        arrayOf("foo", "MyTrigger", "Trigger", "triggers${File.separator}MyTrigger$APEX_TRIGGER_FILE_EXTENSION"))
}