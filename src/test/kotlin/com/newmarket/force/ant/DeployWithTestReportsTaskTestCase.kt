package com.newmarket.force.ant

import com.salesforce.ant.DeployTask
import org.apache.tools.ant.Project
import org.apache.tools.ant.types.FileSet
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.hamcrest.core.IsEqual.*
import org.testng.annotations.Test
import org.testng.Assert.*
import java.io.File
import java.time.LocalDateTime


public class DeployWithTestReportsTaskTestCase {

    @Test fun sut_always_shouldDeriveFromTaskClass() =
        assertThat(createSystemUnderTest(), instanceOf(DeployTask::class.java))

    @Test fun createBatchTest_always_shouldAddNewBatchTestToTests() {
        val sut = createSystemUnderTest()
        val actual = sut.createBatchTest()
        assertThat(sut.tests, contains(actual))
        assertThat(sut.getProject(), sameInstance(actual.project))
    }

    @Test fun createBatchTest_always_shouldCreatePrefixInName() {
        assertThat(
            "Prefix 'create' is one of the Ant's conventions for nested elements declaration. " +
                "See the manual: http://ant.apache.org/manual/develop.html#nested-elements",
            DeployWithTestReportsTask::createBatchTest.name,
            startsWith("create"))
    }

    @Test fun getRunTests_always_shouldContainAllNestedRunTestElements() {
        val sut = createSystemUnderTest()
        val expected = listOf("foo", "bar", "baz")
        expected.forEach { sut.addRunTest(createRunTestElement(it)) }

        val actual = sut.runTests

        expected.forEach { assertThat(actual, hasItemInArray(it)) }
    }

    @Test fun getRunTests_always_shouldContainAllFileNamesOfBatchTests() {
        withTestDirectory { testDirectory ->
            val sut = createSystemUnderTest()
            val batchTest = sut.createBatchTest()
            val expected = listOf("foo", "bar", "baz")
            val fileSet = createTestClassesFileSet(testDirectory, expected)
            batchTest.addFileSet(fileSet)

            val actual = sut.runTests

            expected.forEach { assertThat(actual, hasItemInArray(it)) }
        }
    }

    @Test fun saveJUnitReportToFile_ifJUnitReportDirIsNotNull_shouldCreateReportFileWithExpectedContent() {
        withTestDirectory { testDirectory ->
            val sut = createSystemUnderTest()
            sut.reporter = Reporter{ LocalDateTime.MAX }
            sut.junitReportDir = testDirectory
            sut.junitReportName = "TEST-ApexSuite.xml"
            sut.username = "foo"
            sut.serverURL = "bar"
            sut.apiVersion = 35.0
            val input = createRunTestsResult()
            val expectedContent = sut.reporter.createJUnitReport(
                input,
                sut.junitTestSuiteName,
                hashMapOf(
                    "username" to sut.username,
                    "serverURL" to sut.serverURL,
                    "apiVersion" to sut.apiVersion.toString())).toString()

            sut.saveJUnitReportToFile(createRunTestsResult())

            val actual = testDirectory.listFiles().single { it.name == sut.junitReportName }
            assertTrue(actual.exists(), "Report file wasn't found")
            assertEquals(actual.readText(), expectedContent)
        }
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

    fun createSystemUnderTest(project: Project = createProject()): DeployWithTestReportsTask {
        val sut = DeployWithTestReportsTask()
        sut.setProject(project)
        return sut
    }

    fun createProject(name: String = "TestProject"): Project {
        val project = Project()
        project.name = name
        return project
    }

    fun createRunTestElement(text: String = ""): DeployTask.CodeNameElement {
        val runTest = DeployTask.CodeNameElement()
        runTest.addText(text)
        return runTest
    }

    fun createTestClassesFileSet(directory: File, fileNames: Iterable<String>): FileSet =
        createFileSet(
            directory,
            fileNames.map {it + BatchTest.APEX_CLASS_FILE_EXTENSION})
}