package com.aquivalabs.force.ant

import org.apache.tools.ant.Project
import org.apache.tools.ant.types.ZipFileSet
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.testng.Assert.assertEquals
import org.testng.Assert.assertTrue
import org.testng.annotations.DataProvider
import org.testng.annotations.Test


class BatchTestTestCase {
    @Test
    fun addFileSet_always_shouldFollowAntNamingConventions() {
        assertThat(
            nestedElementConvention("add"),
            BatchTest::addFileSet.name,
            startsWith("add"))
    }

    @Test
    fun addZipFileSet_always_shouldFollowAntNamingConventions() {
        assertThat(
            nestedElementConvention("add"),
            BatchTest::addZipFileSet.name,
            startsWith("add"))
    }

    @Test
    fun addFileSet_always_shouldFillProjectPropertyOfPassedValue() = withTestDirectory {
        val sut = createSystemUnderTest()
        val input = fileSet(it)

        sut.addFileSet(input)

        assertEquals(input.project, sut.project)
    }

    @Test
    fun addFileSet_always_shouldAddFileSetToResources() = withTestDirectory {
        val sut = createSystemUnderTest()
        val input = fileSet(it, "foo", "bar")

        sut.addFileSet(input)

        input.forEach {
            assertTrue(sut.resources.contains(it))
        }
    }

    @Test
    fun addZipFileSet_always_shouldFillProjectPropertyOfPassedValue() = withZipFile { zipFile ->
        val sut = createSystemUnderTest()
        val input = ZipFileSet().apply { src = zipFile }

        sut.addZipFileSet(input)

        assertEquals(input.project, sut.project)
    }

    @Test
    fun addZipFileSet_always_shouldAddFileSetToResources() {
        withZipFile(classes = setOf("foo", "bar")) { zipFile ->
            val sut = createSystemUnderTest()
            val input = ZipFileSet().apply { src = zipFile }

            sut.addZipFileSet(input)

            input.forEach {
                assertTrue(sut.resources.contains(it))
            }
        }
    }

    @Test(dataProvider = "getFileNamesFileSetTestData")
    fun getFileNames_withFileSet_shouldReturnCorrectResult(
        namespace: String,
        inputFileNames: Set<String>,
        expected: Set<String>,
        message: String) = withTestDirectory {

        val sut = createSystemUnderTest()
        sut.namespace = namespace
        val fileSet = fileSet(it, inputFileNames)
        sut.addFileSet(fileSet)

        assertEquals(sut.getFileNames(), expected, message)
    }

    @DataProvider
    fun getFileNamesFileSetTestData(): Array<Array<Any>> {
        return arrayOf(
            arrayOf(
                "",
                setOf<String>(),
                setOf<String>(),
                "Should return empty list for empty fileSet"),
            arrayOf(
                "",
                setOf("foo.pdf", "bar.trigger", "baz$APEX_CLASS_FILE_EXTENSION"),
                setOf("baz"),
                "Should return only names (without extensions) of files that have $APEX_CLASS_FILE_EXTENSION extension"),
            arrayOf(
                "namespace",
                setOf("foo$APEX_CLASS_FILE_EXTENSION"),
                setOf("namespace${NAMESPACE_SEPARATOR}foo"),
                "Should add namespace to file names"))
    }

    @Test(dataProvider = "getFileNamesZipFileSetTestData")
    fun getFileNames_withZipFileSet_shouldReturnCorrectResult(
        namespace: String,
        classNames: Set<String>,
        triggerNames: Set<String>,
        expected: Set<String>,
        message: String) = withZipFile(classes = classNames, triggers = triggerNames) { zipFile ->

        val sut = createSystemUnderTest()
        sut.namespace = namespace
        val input = ZipFileSet().apply { src = zipFile }
        sut.addZipFileSet(input)

        assertEquals(sut.getFileNames(), expected, message)
    }

    @DataProvider
    fun getFileNamesZipFileSetTestData(): Array<Array<Any>> {
        return arrayOf(
            arrayOf(
                "",
                setOf<String>(),
                setOf<String>(),
                setOf<String>(),
                "Should return empty list for empty zipFileSet"),
            arrayOf(
                "",
                setOf<String>(),
                setOf("foo"),
                setOf<String>(),
                "Should ignore triggers from zipFileSet"),
            arrayOf(
                "",
                setOf("foo"),
                setOf("foo"),
                setOf("foo"),
                "Should ignore triggers from zipFileSet (same class and trigger name)"),
            arrayOf(
                "",
                setOf("foo", "bar", "baz"),
                setOf<String>(),
                setOf("foo", "bar", "baz"),
                "Should return only names (without extensions) of files that have $APEX_CLASS_FILE_EXTENSION extension"),
            arrayOf(
                "namespace",
                setOf("foo"),
                setOf<String>(),
                setOf("namespace${NAMESPACE_SEPARATOR}foo"),
                "Should add namespace to file names"))
    }

    fun createSystemUnderTest(): BatchTest = BatchTest(Project().apply { name = "TestProject" })
}