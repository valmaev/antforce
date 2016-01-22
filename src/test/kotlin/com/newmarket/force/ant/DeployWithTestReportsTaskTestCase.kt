package com.newmarket.force.ant

import com.salesforce.ant.DeployTask
import org.apache.tools.ant.Project
import org.apache.tools.ant.types.FileSet
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.testng.annotations.Test
import java.io.File


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
        val sut = createSystemUnderTest()
        var testDirectory: File? = null
        try {
            testDirectory = createTempDir(javaClass.name)
            val batchTest = sut.createBatchTest()
            val expected = listOf("foo", "bar", "baz")
            val fileSet = createTestClassesFileSet(testDirectory, expected)
            batchTest.addFileSet(fileSet)

            val actual = sut.runTests

            expected.forEach { assertThat(actual, hasItemInArray(it)) }
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