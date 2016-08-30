package com.aquivalabs.force.ant

import org.apache.tools.ant.Project
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.testng.Assert.assertEquals
import org.testng.Assert.assertTrue
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.File


class BatchTestTestCase() {
    private var testDirectory: File? = null

    @BeforeMethod
    fun createTestDirectory() {
        testDirectory = createTempDir(javaClass.name)
    }

    @AfterMethod
    fun removeTestDirectory() = testDirectory?.deleteRecursively()

    @Test fun addFileSet_always_shouldFollowAntNamingConventions() {
        MatcherAssert.assertThat(
            "Prefix 'add' is one of the Ant's conventions for nested elements declaration. " +
                "See the manual: http://ant.apache.org/manual/develop.html#nested-elements",
            BatchTest::addFileSet.name,
            Matchers.startsWith("add"))
    }

    @Test fun addFileSet_always_shouldFillProjectPropertyOfPassedValue() {
        val sut = createSystemUnderTest()
        val input = createFileSet(testDirectory!!)

        sut.addFileSet(input)

        assertEquals(input.project, sut.project)
    }

    @Test fun addFileSet_always_shouldAddFileSetToResources() {
        val sut = createSystemUnderTest()
        val input = createFileSet(testDirectory!!, "foo", "bar")

        sut.addFileSet(input)

        input.forEach {
            assertTrue(sut.resources.contains(it))
        }
    }

    @Test(dataProvider = "getFileNamesTestData")
    fun getFileNames_always_shouldReturnCorrectResult(
        namespace: String,
        inputFileNames: List<String>,
        expected: List<String>,
        message: String) {

        val sut = createSystemUnderTest()
        sut.namespace = namespace
        val fileSet = createFileSet(testDirectory!!, inputFileNames)
        sut.addFileSet(fileSet)

        assertEquals(sut.getFileNames(), expected, message)
    }

    @DataProvider
    fun getFileNamesTestData(): Array<Array<Any>> = arrayOf(
        arrayOf(
            "",
            listOf<String>(),
            listOf<String>(),
            "Should return empty list for empty fileSet"
        ),
        arrayOf(
            "",
            listOf("foo.pdf", "bar.trigger", "baz$APEX_CLASS_FILE_EXTENSION"),
            listOf("baz"),
            "Should return only names (without extensions) of files that have $APEX_CLASS_FILE_EXTENSION extension"
        ),
        arrayOf(
            "namespace",
            listOf("foo$APEX_CLASS_FILE_EXTENSION"),
            listOf("namespace${NAMESPACE_SEPARATOR}foo"),
            "Should add namespace to file names"
        ))

    fun createSystemUnderTest(): BatchTest {
        val project = Project()
        project.name = "TestProject"
        return BatchTest(project)
    }
}