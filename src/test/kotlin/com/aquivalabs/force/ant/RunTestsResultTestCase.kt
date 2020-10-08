package com.aquivalabs.force.ant

import com.sforce.soap.metadata.CodeCoverageResult
import com.sforce.soap.metadata.CodeCoverageWarning
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import org.testng.Assert.*

class RunTestsResultTestCase {
    @Test(dataProvider = "nonEmptyCoverageTestData")
    fun totalCoverage_ifTotalNumLocationsMoreThan0_shouldReturnAverageCoveragePercentageForAllCodeCoverageResults(
        codeCoverage: Array<CodeCoverageResult>) {

        val sut = runTestsResult(codeCoverage = codeCoverage)
        assertEquals(
            sut.totalCoverage,
            sut.totalNumLocationsCovered.toDouble() / sut.totalNumLocations)
        assertEquals(
            sut.totalCoveragePercentage,
            sut.totalCoverage * 100)
    }

    @Test
    fun totalCoveragePercentage_ifTotalNumLocationsEquals0_shouldReturn100() {
        val sut = runTestsResult()
        assertEquals(sut.totalCoveragePercentage, 100.0)
    }

    @Test(dataProvider = "coverageTestData")
    fun totalNumLocationsCovered_always_shouldReturnSumOfNumLocationsCoveredFromAllCodeCoverageResults(
        codeCoverage: Array<CodeCoverageResult>) {

        val sut = runTestsResult(codeCoverage = codeCoverage)
        assertEquals(
            sut.totalNumLocationsCovered,
            sut.codeCoverage.map { it.numLocationsCovered }.sum())
    }

    @Test(dataProvider = "coverageTestData")
    fun totalNumLocationsNotCovered_always_shouldReturnSumOfNumLocationsNotCoveredFromAllCodeCoverageResults(
        codeCoverage: Array<CodeCoverageResult>) {

        val sut = runTestsResult(codeCoverage = codeCoverage)
        assertEquals(
            sut.totalNumLocationsNotCovered,
            sut.codeCoverage.map { it.numLocationsNotCovered }.sum())
    }

    @Test(dataProvider = "coverageTestData")
    fun totalNumLocations_always_shouldReturnSumOfNumLocationsFromAllCodeCoverageResults(
        codeCoverage: Array<CodeCoverageResult>) {

        val sut = runTestsResult(codeCoverage = codeCoverage)
        assertEquals(
            sut.totalNumLocations,
            sut.codeCoverage.map { it.numLocations }.sum())
    }

    @Test
    fun numSuccesses_always_shouldReturnDifferenceBetweenNumTestsRunAndNumFailures() {
        val sut = runTestsResult(numTestsRun = 10, numFailures = 1)
        assertEquals(sut.numSuccesses, sut.numTestsRun - sut.numFailures)
    }

    @Test(dataProvider = "coveredClassesTestData")
    fun coveredClasses_always_shouldReturnSetOfQualifiedClassNamesWithNonZeroCoverage(
        codeCoverage: Array<CodeCoverageResult>,
        expected: Set<String>) {

        val sut = runTestsResult(codeCoverage = codeCoverage)
        assertEquals(sut.coveredClasses, expected)
        assertEquals(sut.numClassesCovered, expected.size)
    }

    @Test(dataProvider = "coveredTriggersTestData")
    fun coveredTriggers_always_shouldReturnSetOfQualifiedTriggerNamesWithNonZeroCoverage(
        codeCoverage: Array<CodeCoverageResult>,
        expected: Set<String>) {

        val sut = runTestsResult(codeCoverage = codeCoverage)
        assertEquals(sut.coveredTriggers, expected)
        assertEquals(sut.numTriggersCovered, expected.size)
    }

    @Test(dataProvider = "notCoveredClassesTestData")
    fun notCoveredClasses_always_shouldReturnSetOfQualifiedClassNamesWhichHaveZeroCoverageWarnings(
        codeCoverageWarnings: Array<CodeCoverageWarning>,
        expected: Set<String>) {

        val sut = runTestsResult(codeCoverageWarnings = codeCoverageWarnings)
        assertEquals(sut.notCoveredClasses, expected)
        assertEquals(sut.numClassesNotCovered, expected.size)
    }

    @Test(dataProvider = "notCoveredTriggersTestData")
    fun notCoveredTriggers_always_shouldReturnSetOfQualifiedTriggerNamesWhichHaveZeroCoverageWarnings(
        codeCoverageWarnings: Array<CodeCoverageWarning>,
        expected: Set<String>) {

        val sut = runTestsResult(codeCoverageWarnings = codeCoverageWarnings)
        assertEquals(sut.notCoveredTriggers, expected)
        assertEquals(sut.numTriggersNotCovered, expected.size)
    }

    @Test(dataProvider = "numClassesAndTriggersTestData")
    fun numClasses_always_shouldReturnSumOfCoveredClassesSizeAndNotCoveredClassesSize(
        codeCoverage: Array<CodeCoverageResult>,
        codeCoverageWarnings: Array<CodeCoverageWarning>,
        expected: Int) {

        val sut = runTestsResult(
            codeCoverage = codeCoverage,
            codeCoverageWarnings = codeCoverageWarnings)
        assertEquals(sut.numClasses, expected)
        assertEquals(sut.numClasses, sut.numClassesCovered + sut.numClassesNotCovered)
    }

    @Test(dataProvider = "numClassesAndTriggersTestData")
    fun numTriggers_always_shouldReturnSumOfCoveredTriggersSizeAndNotCoveredTriggersSize(
        codeCoverage: Array<CodeCoverageResult>,
        codeCoverageWarnings: Array<CodeCoverageWarning>,
        expected: Int) {

        val sut = runTestsResult(
            codeCoverage = codeCoverage,
            codeCoverageWarnings = codeCoverageWarnings)
        assertEquals(sut.numTriggers, expected)
        assertEquals(sut.numTriggers, sut.numTriggersCovered + sut.numTriggersNotCovered)
    }

    @Test
    fun classCoverage_ifNumClassesEquals0_shouldReturn1() {
        val sut = runTestsResult()
        assertEquals(sut.numClasses, 0)
        assertEquals(sut.classCoverage, 1.0)
        assertEquals(sut.classCoveragePercentage, 100.0)

    }

    @Test
    fun triggerCoverage_ifNumTriggersEquals0_shouldReturn1() {
        val sut = runTestsResult()
        assertEquals(sut.numTriggers, 0)
        assertEquals(sut.triggerCoverage, 1.0)
        assertEquals(sut.triggerCoveragePercentage, 100.0)
    }

    @Test(dataProvider = "classAndTriggerCoverageTestData")
    fun classCoverage_ifNumClassesMoreThan0_shouldReturnQuotientOfNumClassesCoveredAndNumClasses(
        codeCoverage: Array<CodeCoverageResult>,
        codeCoverageWarnings: Array<CodeCoverageWarning>) {

        val sut = runTestsResult(
            codeCoverage = codeCoverage,
            codeCoverageWarnings = codeCoverageWarnings)
        assertEquals(sut.classCoverage, sut.numClassesCovered.toDouble() / sut.numClasses)
        assertEquals(sut.classCoveragePercentage, sut.classCoverage * 100)
    }

    @Test(dataProvider = "classAndTriggerCoverageTestData")
    fun triggerCoverage_ifNumTriggersMoreThan0_shouldReturnQuotientOfNumTriggersCoveredAndNumTriggers(
        codeCoverage: Array<CodeCoverageResult>,
        codeCoverageWarnings: Array<CodeCoverageWarning>) {

        val sut = runTestsResult(
            codeCoverage = codeCoverage,
            codeCoverageWarnings = codeCoverageWarnings)
        assertEquals(sut.triggerCoverage, sut.numTriggersCovered.toDouble() / sut.numTriggers)
        assertEquals(sut.triggerCoveragePercentage, sut.triggerCoverage * 100)
    }

    @DataProvider
    fun coveredClassesTestData(): Array<Array<Any?>> = arrayOf(
        arrayOf(
            arrayOf<CodeCoverageResult>(),
            setOf<String>()),
        arrayOf(
            arrayOf(
                codeCoverageResult(
                    name = "Foo",
                    namespace = "nmspc",
                    type = "Class",
                    numLocations = 10,
                    numLocationsNotCovered = 0),
                codeCoverageResult(
                    name = "Bar",
                    namespace = "nmspc",
                    type = "Class",
                    numLocations = 10,
                    numLocationsNotCovered = 10),
                codeCoverageResult(
                    name = "Baz",
                    namespace = "nmspc",
                    type = "Trigger",
                    numLocations = 10,
                    numLocationsNotCovered = 0)),
            setOf("nmspc.Foo")))

    @DataProvider
    fun coveredTriggersTestData(): Array<Array<Any?>> = arrayOf(
        arrayOf(
            arrayOf<CodeCoverageResult>(),
            setOf<String>()),
        arrayOf(
            arrayOf(
                codeCoverageResult(
                    name = "Foo",
                    namespace = "nmspc",
                    type = "Trigger",
                    numLocations = 10,
                    numLocationsNotCovered = 0),
                codeCoverageResult(
                    name = "Bar",
                    namespace = "nmspc",
                    type = "Trigger",
                    numLocations = 10,
                    numLocationsNotCovered = 10),
                codeCoverageResult(
                    name = "Baz",
                    namespace = "nmspc",
                    type = "Class",
                    numLocations = 10,
                    numLocationsNotCovered = 0)),
            setOf("nmspc.Foo")))

    @DataProvider
    fun notCoveredClassesTestData(): Array<Array<Any?>> = arrayOf(
        arrayOf(
            arrayOf<CodeCoverageWarning>(),
            setOf<String>()),
        arrayOf(
            arrayOf(
                codeCoverageWarning(
                    name = "Foo",
                    namespace = "nmspc",
                    message = "Test coverage of selected Apex Class is 0%, at least 75% test coverage is required"),
                codeCoverageWarning(
                    name = "Bar",
                    namespace = "nmspc",
                    message = "Test coverage of selected Apex Class is 12%, at least 75% test coverage is required"),
                codeCoverageWarning(
                    name = "Baz",
                    namespace = "nmspc",
                    message = "Test coverage of selected Apex Trigger is 0%, at least 75% test coverage is required"),
                codeCoverageWarning(
                    name = "Qux",
                    namespace = "nmspc",
                    message = "Test coverage of selected Apex Trigger is 33%, at least 75% test coverage is required")),
            setOf("nmspc.Foo")))

    @DataProvider
    fun notCoveredTriggersTestData(): Array<Array<Any?>> = arrayOf(
        arrayOf(
            arrayOf<CodeCoverageWarning>(),
            setOf<String>()),
        arrayOf(
            arrayOf(
                codeCoverageWarning(
                    name = "Foo",
                    namespace = "nmspc",
                    message = "Test coverage of selected Apex Trigger is 0%, at least 75% test coverage is required"),
                codeCoverageWarning(
                    name = "Bar",
                    namespace = "nmspc",
                    message = "Test coverage of selected Apex Trigger is 12%, at least 75% test coverage is required"),
                codeCoverageWarning(
                    name = "Baz",
                    namespace = "nmspc",
                    message = "Test coverage of selected Apex Class is 0%, at least 75% test coverage is required"),
                codeCoverageWarning(
                    name = "Qux",
                    namespace = "nmspc",
                    message = "Test coverage of selected Apex Class is 33%, at least 75% test coverage is required")),
            setOf("nmspc.Foo")))

    @DataProvider
    fun numClassesAndTriggersTestData(): Array<Array<Any?>> {
        return arrayOf(
            arrayOf(
                arrayOf<CodeCoverageResult>(),
                arrayOf<CodeCoverageWarning>(),
                0),
            arrayOf(
                arrayOf(
                    codeCoverageResult(
                        name = "Foo",
                        namespace = "nmspc",
                        type = "Trigger",
                        numLocations = 10,
                        numLocationsNotCovered = 0),
                    codeCoverageResult(
                        name = "Bar",
                        namespace = "nmspc",
                        type = "Trigger",
                        numLocations = 10,
                        numLocationsNotCovered = 10),
                    codeCoverageResult(
                        name = "Baz",
                        namespace = "nmspc",
                        type = "Class",
                        numLocations = 10,
                        numLocationsNotCovered = 0)),
                arrayOf(
                    codeCoverageWarning(
                        name = "Foo",
                        namespace = "qwe",
                        message = "Test coverage of selected Apex Trigger is 0%, at least 75% test coverage is required"),
                    codeCoverageWarning(
                        name = "Bar",
                        namespace = "qwe",
                        message = "Test coverage of selected Apex Trigger is 12%, at least 75% test coverage is required"),
                    codeCoverageWarning(
                        name = "Baz",
                        namespace = "qwe",
                        message = "Test coverage of selected Apex Class is 0%, at least 75% test coverage is required"),
                    codeCoverageWarning(
                        name = "Qux",
                        namespace = "qwe",
                        message = "Test coverage of selected Apex Class is 33%, at least 75% test coverage is required")),
                1 + 1),
            arrayOf(
                arrayOf(
                    codeCoverageResult(
                        name = "Foo",
                        namespace = "nmspc",
                        type = "Trigger",
                        numLocations = 10,
                        numLocationsNotCovered = 0),
                    codeCoverageResult(
                        name = "Bar",
                        namespace = "nmspc",
                        type = "Trigger",
                        numLocations = 10,
                        numLocationsNotCovered = 10),
                    codeCoverageResult(
                        name = "Baz",
                        namespace = "nmspc",
                        type = "Class",
                        numLocations = 10,
                        numLocationsNotCovered = 0)),
                arrayOf<CodeCoverageWarning>(),
                1),
            arrayOf(
                arrayOf<CodeCoverageResult>(),
                arrayOf(
                    codeCoverageWarning(
                        name = "Foo",
                        namespace = "qwe",
                        message = "Test coverage of selected Apex Trigger is 0%, at least 75% test coverage is required"),
                    codeCoverageWarning(
                        name = "Bar",
                        namespace = "qwe",
                        message = "Test coverage of selected Apex Trigger is 12%, at least 75% test coverage is required"),
                    codeCoverageWarning(
                        name = "Baz",
                        namespace = "qwe",
                        message = "Test coverage of selected Apex Class is 0%, at least 75% test coverage is required"),
                    codeCoverageWarning(
                        name = "Qux",
                        namespace = "qwe",
                        message = "Test coverage of selected Apex Class is 33%, at least 75% test coverage is required")),
                1))
    }

    @DataProvider
    fun classAndTriggerCoverageTestData(): Array<Array<Any?>> {
        return arrayOf(
            arrayOf(
                arrayOf(
                    codeCoverageResult(
                        name = "Foo",
                        namespace = "nmspc",
                        type = "Trigger",
                        numLocations = 10,
                        numLocationsNotCovered = 0),
                    codeCoverageResult(
                        name = "Bar",
                        namespace = "nmspc",
                        type = "Trigger",
                        numLocations = 10,
                        numLocationsNotCovered = 10),
                    codeCoverageResult(
                        name = "Baz",
                        namespace = "nmspc",
                        type = "Class",
                        numLocations = 10,
                        numLocationsNotCovered = 0)),
                arrayOf(
                    codeCoverageWarning(
                        name = "Foo",
                        namespace = "qwe",
                        message = "Test coverage of selected Apex Trigger is 0%, at least 75% test coverage is required"),
                    codeCoverageWarning(
                        name = "Bar",
                        namespace = "qwe",
                        message = "Test coverage of selected Apex Trigger is 12%, at least 75% test coverage is required"),
                    codeCoverageWarning(
                        name = "Baz",
                        namespace = "qwe",
                        message = "Test coverage of selected Apex Class is 0%, at least 75% test coverage is required"),
                    codeCoverageWarning(
                        name = "Qux",
                        namespace = "qwe",
                        message = "Test coverage of selected Apex Class is 33%, at least 75% test coverage is required"))),
            arrayOf(
                arrayOf(
                    codeCoverageResult(
                        name = "Foo",
                        namespace = "nmspc",
                        type = "Trigger",
                        numLocations = 10,
                        numLocationsNotCovered = 0),
                    codeCoverageResult(
                        name = "Bar",
                        namespace = "nmspc",
                        type = "Trigger",
                        numLocations = 10,
                        numLocationsNotCovered = 10),
                    codeCoverageResult(
                        name = "Baz",
                        namespace = "nmspc",
                        type = "Class",
                        numLocations = 10,
                        numLocationsNotCovered = 0)),
                arrayOf<CodeCoverageWarning>()),
            arrayOf(
                arrayOf<CodeCoverageResult>(),
                arrayOf(
                    codeCoverageWarning(
                        name = "Foo",
                        namespace = "qwe",
                        message = "Test coverage of selected Apex Trigger is 0%, at least 75% test coverage is required"),
                    codeCoverageWarning(
                        name = "Bar",
                        namespace = "qwe",
                        message = "Test coverage of selected Apex Trigger is 12%, at least 75% test coverage is required"),
                    codeCoverageWarning(
                        name = "Baz",
                        namespace = "qwe",
                        message = "Test coverage of selected Apex Class is 0%, at least 75% test coverage is required"),
                    codeCoverageWarning(
                        name = "Qux",
                        namespace = "qwe",
                        message = "Test coverage of selected Apex Class is 33%, at least 75% test coverage is required"))))
    }

    @DataProvider
    fun coverageTestData(): Array<Array<Any>> = nonEmptyCoverageTestData().plus(
        arrayOf(
            arrayOf<CodeCoverageResult>()))

    @DataProvider
    fun nonEmptyCoverageTestData(): Array<Array<Any>> = arrayOf(
        arrayOf(
            arrayOf(
                codeCoverageResult(numLocations = 0, numLocationsNotCovered = 0),
                codeCoverageResult(numLocations = 1, numLocationsNotCovered = 1))),
        arrayOf(
            arrayOf(
                codeCoverageResult(numLocations = 0, numLocationsNotCovered = 0),
                codeCoverageResult(numLocations = 100, numLocationsNotCovered = 56),
                codeCoverageResult(numLocations = 11, numLocationsNotCovered = 4),
                codeCoverageResult(numLocations = 23, numLocationsNotCovered = 16),
                codeCoverageResult(numLocations = 1, numLocationsNotCovered = 1))))
}