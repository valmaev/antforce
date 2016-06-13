package com.newmarket.force.ant

import com.salesforce.ant.DeployTask
import com.sforce.soap.metadata.TestLevel
import org.apache.tools.ant.Project
import org.apache.tools.ant.types.FileSet
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.testng.annotations.Test
import org.testng.Assert.*
import org.testng.annotations.DataProvider
import java.io.File
import java.time.LocalDateTime


class DeployWithTestReportsTaskTestCase {

    private fun nestedElementConvention(prefix: String) =
        "Prefix '$prefix' is one of the Ant's conventions for nested elements declaration. See the manual: http://ant.apache.org/manual/develop.html#nested-elements"

    @Test fun sut_always_shouldDeriveFromProperBaseClass() =
        assertThat(createSystemUnderTest(), instanceOf(DeployTask::class.java))

    @Test fun createBatchTest_always_shouldAddNewBatchTestToTests() {
        val sut = createSystemUnderTest()
        val actual = sut.createBatchTest()
        assertThat(sut.batchTests, contains(actual))
        assertThat(sut.project, sameInstance(actual.project))
    }

    @Test fun createBatchTest_always_shouldFollowAntNamingConventions() {
        assertThat(
            nestedElementConvention("create"),
            DeployWithTestReportsTask::createBatchTest.name,
            startsWith("create"))
    }

    @Test fun addJUnitReport_always_shouldFollowAntNamingConventions() {
        assertThat(
            nestedElementConvention("add"),
            DeployWithTestReportsTask::addJUnitReport.name,
            startsWith("add"))
    }

    @Test fun addCoberturaReport_always_shouldFollowAntNamingConventions() {
        assertThat(
            nestedElementConvention("add"),
            DeployWithTestReportsTask::addCoberturaReport.name,
            startsWith("add"))
    }

    @Test fun addHtmlCoverageReport_always_shouldFollowAntNamingConventions() {
        assertThat(
            nestedElementConvention("add"),
            DeployWithTestReportsTask::addHtmlCoverageReport.name,
            startsWith("add"))
    }

    @Test(dataProvider = "getRunTestsEmptyArrayTestLevels")
    fun getRunTests_forAnyOtherTestLevel_shouldReturnEmptyArray(testLevel: TestLevel) {
        val sut = createSystemUnderTest(testLevel = testLevel)
        assertThat(sut.runTests!!.asList(), hasSize(equalTo(0)))
    }

    @DataProvider
    fun getRunTestsEmptyArrayTestLevels(): Array<Array<Any>> = TestLevel.values()
        .filter { it != TestLevel.RunSpecifiedTests }
        .map { arrayOf<Any>(it) }
        .toTypedArray()

    @Test fun getRunTests_forRunSpecifiedTestsTestLevel_shouldContainAllNestedRunTestElements() {
        val sut = createSystemUnderTest(testLevel = TestLevel.RunSpecifiedTests)
        val expected = listOf("foo", "bar", "baz")
        expected.forEach { sut.addRunTest(createRunTestElement(it)) }

        val actual = sut.runTests

        expected.forEach { assertThat(actual, hasItemInArray(it)) }
    }

    @Test fun getRunTests_forRunSpecifiedTestsTestLevel_shouldContainAllFileNamesOfBatchTests() {
        withTestDirectory { testDirectory ->
            val sut = createSystemUnderTest(testLevel = TestLevel.RunSpecifiedTests)
            val batchTest = sut.createBatchTest()
            val expected = listOf("foo", "bar", "baz")
            val fileSet = createTestClassesFileSet(testDirectory, expected)
            batchTest.addFileSet(fileSet)

            val actual = sut.runTests

            expected.forEach { assertThat(actual, hasItemInArray(it)) }
        }
    }

    @Test fun saveJUnitReportToFile_ifReportDirIsNotNull_shouldCreateReportFileWithExpectedContent() {
        withTestDirectory { testDirectory ->
            // Arrange
            val sut = createSystemUnderTest()
            sut.reporter = Reporter { LocalDateTime.MAX }
            sut.reportDir = testDirectory
            sut.username = "foo"
            sut.serverURL = "bar"
            sut.apiVersion = 35.0

            val report = JUnitReport(file = "TEST-ApexSuite.xml", suiteName = "TestSuite")
            sut.addJUnitReport(report)

            val input = createRunTestsResult()
            val expectedContent = sut.reporter.createJUnitReport(
                input,
                report.suiteName,
                hashMapOf(
                    "username" to sut.username,
                    "serverURL" to sut.serverURL,
                    "apiVersion" to sut.apiVersion.toString())).toString()

            // Act
            sut.saveJUnitReportToFile(input)

            // Assert
            val actual = testDirectory.listFiles().single { it.name == report.file }
            assertTrue(actual.exists(), "Report file wasn't found")
            assertEquals(actual.readText(), expectedContent)
        }
    }

    @Test fun saveCoberturaReportToFile_ifReportDirIsNotNull_shouldCreateReportFileWithExpectedContent() {
        withTestDirectory { testDirectory ->
            // Arrange
            val sut = createSystemUnderTest()
            sut.reporter = Reporter { LocalDateTime.MAX }
            sut.reportDir = testDirectory

            val report = CoberturaReport(file = "Cobertura.xml")
            sut.addCoberturaReport(report)

            val input = createRunTestsResult()
            val expectedContent = sut.reporter.createCoberturaReport(input).toString()

            // Act
            sut.saveCoberturaReportToFile(input)

            // Assert
            val actual = testDirectory.listFiles().single { it.name == report.file }
            assertTrue(actual.exists(), "Report file wasn't found")
            assertEquals(actual.readText(), expectedContent)
        }
    }

    @Test fun saveHtmlCoverageReportToFile_ifReportDirIsNotNull_shouldCreateReportFileWithExpectedContent() {
        withTestDirectory { testDirectory ->
            // Arrange
            val sut = createSystemUnderTest()
            sut.reporter = Reporter { LocalDateTime.MAX }
            sut.reportDir = testDirectory

            val report = HtmlCoverageReport(file = "Coverage.html")
            sut.addHtmlCoverageReport(report)

            val input = createRunTestsResult()
            val expectedContent = sut.reporter.createHtmlCoverageReport(input).toString()

            // Act
            sut.saveHtmlCoverageReportToFile(input)

            // Assert
            val actual = testDirectory.listFiles().single { it.name == report.file }
            assertTrue(actual.exists(), "Report file wasn't found")
            assertEquals(actual.readText(), expectedContent)
        }
    }

    @Test fun deployRoot_always_shouldReturnValueFromCorrespondingBaseClassPrivateField() {
        val sut = createSystemUnderTest()
        val expected = "foobar"
        sut.setDeployRoot(expected)
        assertEquals(sut.deployRoot, expected)
    }

    fun withTestDirectory(directoryNamePrefix: String = javaClass.name, test: (File) -> Unit) {
        var testDirectory: File? = null
        try {
            testDirectory = createTempDir(directoryNamePrefix)
            test(testDirectory)
        } finally {
            testDirectory?.deleteRecursively()
        }
    }

    fun createSystemUnderTest(
        project: Project = createProject(),
        testLevel: TestLevel = TestLevel.RunSpecifiedTests): DeployWithTestReportsTask {
        val sut = DeployWithTestReportsTask()
        sut.project = project
        sut.testLevel = testLevel.name
        return sut
    }

    fun createRunTestElement(text: String = ""): DeployTask.CodeNameElement {
        val runTest = DeployTask.CodeNameElement()
        runTest.addText(text)
        return runTest
    }

    fun createTestClassesFileSet(directory: File, fileNames: Iterable<String>): FileSet =
        createFileSet(
            directory,
            fileNames.map { it + Constants.APEX_CLASS_FILE_EXTENSION })
}