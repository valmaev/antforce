package com.aquivalabs.force.ant

import com.aquivalabs.force.ant.reporters.*
import com.salesforce.ant.DeployTask
import com.salesforce.ant.ZipUtil
import com.sforce.soap.metadata.DeployOptions
import com.sforce.soap.metadata.MetadataConnection
import com.sforce.soap.metadata.TestLevel
import kotlinx.html.dom.serialize
import org.apache.tools.ant.Project
import org.apache.tools.ant.types.FileSet
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.testng.annotations.*
import org.testng.Assert.*
import org.xmlmatchers.XmlMatchers.*
import org.xmlmatchers.transform.XmlConverters.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDateTime
import java.util.zip.*
import java.io.FileOutputStream
import java.util.*

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
        val sut = createSystemUnderTest(testLevel = testLevel.name)
        assertThat(sut.runTests!!.asList(), hasSize(equalTo(0)))
    }

    @DataProvider
    fun getRunTestsEmptyArrayTestLevels(): Array<Array<Any>> = TestLevel.values()
        .filter { it != TestLevel.RunSpecifiedTests }
        .map { arrayOf<Any>(it) }
        .toTypedArray()

    @Test fun getRunTests_forRunSpecifiedTestsTestLevel_shouldContainAllNestedRunTestElements() {
        val sut = createSystemUnderTest(testLevel = TestLevel.RunSpecifiedTests.name)
        val expected = listOf("foo", "bar", "baz")
        expected.forEach { sut.addRunTest(createRunTestElement(it)) }

        val actual = sut.runTests

        expected.forEach { assertThat(actual, hasItemInArray(it)) }
    }

    @Test fun getRunTests_forRunSpecifiedTestsTestLevel_shouldContainAllFileNamesOfBatchTests() {
        withTestDirectory { testDirectory ->
            val sut = createSystemUnderTest(testLevel = TestLevel.RunSpecifiedTests.name)
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
            sut.jUnitReporter = JUnitReporter(dateTimeProvider = { LocalDateTime.MAX })
            sut.reportDir = testDirectory
            sut.username = "foo"
            sut.serverURL = "bar"
            sut.apiVersion = 35.0

            val report = JUnitReport(file = "TEST-ApexSuite.xml", suiteName = "TestSuite")
            sut.addJUnitReport(report)

            val input = createRunTestsResult()

            // Act
            sut.saveJUnitReportToFile(input)

            // Assert
            val expectedContent = sut.jUnitReporter.createReport(input).toString()
            val actual = testDirectory.listFiles().single { it.name == report.file }
            assertTrue(actual.exists(), "Report file wasn't found")
            assertEquals(actual.readText(), expectedContent)
        }
    }

    @Test fun saveCoberturaReportToFile_ifReportDirIsNotNull_shouldCreateReportFileWithExpectedContent() {
        withTestDirectory { testDirectory ->
            // Arrange
            val sut = createSystemUnderTest()
            sut.coberturaReporter = CoberturaCoverageReporter()
            sut.reportDir = testDirectory

            val report = CoberturaReport(file = "Cobertura.xml")
            sut.addCoberturaReport(report)

            val input = createRunTestsResult()
            val expectedContent = sut.coberturaReporter.createReport(input).toString()

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
            sut.htmlCoverageReporter = HtmlCoverageReporter(dateTimeProvider = { LocalDateTime.MAX })
            sut.reportDir = testDirectory

            val report = HtmlCoverageReport(file = "Coverage.html")
            sut.addHtmlCoverageReport(report)

            val input = createRunTestsResult()
            val expectedContent = sut.htmlCoverageReporter.createReport(input).serialize(true)

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
        sut.deployRoot = expected
        assertEquals(sut.deployRoot, expected)
    }

    @Test(dataProvider = "improperTestLevelForCoverageTestClassData")
    fun addCoverageTestClassToDeployRootPackage_withImproperTestLevel_shouldNotGenerateCoverageTestClass(
        improperTestLevel: String?,
        enforceCoverageForAllClasses: Boolean?) {
        withDeployRoot {
            // Arrange
            val sut = createSystemUnderTest(
                testLevel = improperTestLevel,
                deployRoot = it.absolutePath,
                enforceCoverageForAllClasses = enforceCoverageForAllClasses)

            // Act
            sut.addCoverageTestClassToDeployRootPackage(it)

            // Assert
            val expected = ZipUtil.zipRoot(it)
            assertEquals(sut.zipBytes, expected)
        }
    }

    @DataProvider
    fun improperTestLevelForCoverageTestClassData(): Array<Array<out Any?>> = arrayOf(
        arrayOf<Any?>(null, null),
        arrayOf<Any?>("", null),
        arrayOf<Any?>(TestLevel.NoTestRun.name, null),
        arrayOf<Any?>(null, false),
        arrayOf<Any?>("", false),
        arrayOf<Any?>(TestLevel.NoTestRun.name, false),
        arrayOf<Any?>(null, true),
        arrayOf<Any?>("", true),
        arrayOf<Any?>(TestLevel.NoTestRun.name, true),
        arrayOf<Any?>(TestLevel.RunSpecifiedTests.name, null),
        arrayOf<Any?>(TestLevel.RunSpecifiedTests.name, false))

    @Test(dataProvider = "absenceOfFilesForCoverageTestClassData")
    fun addCoverageTestClassToDeployRootPackage_withoutImportantFilesInDeployRoot_shouldNotGenerateCoverageTestClass(
        fileToRemove: String) {
        withDeployRoot {
            // Arrange
            File(it, fileToRemove).deleteRecursively()
            val sut = createSystemUnderTest(
                deployRoot = it.absolutePath,
                enforceCoverageForAllClasses = true)

            // Act
            sut.addCoverageTestClassToDeployRootPackage(it)

            // Assert
            val expected = ZipUtil.zipRoot(it)
            assertEquals(
                sut.zipBytes,
                expected,
                "Should not generate coverage test class if deployRoot didn't contain $fileToRemove")
        }
    }

    @DataProvider
    fun absenceOfFilesForCoverageTestClassData(): Array<Array<out Any?>> = arrayOf(
        arrayOf<Any?>("classes"),
        arrayOf<Any?>("package.xml"))

    @Test(dataProvider = "packageXmlContainsApexClassTypeForCoverageTestClassData")
    fun addCoverageTestClassToDeployRootPackage_ifPackageXmlContainsApexClassType_shouldGenerateCoverageTestClassAndModifyPackageXml(
        packageXmlClassMembers: LinkedHashSet<String>,
        classFileNames: Set<String>) {

        val packageXml = generatePackageWithApexClasses(packageXmlClassMembers)

        withDeployRoot(packageXml, classFileNames) {
            // Arrange
            val sut = createSystemUnderTest(
                deployRoot = it.absolutePath,
                enforceCoverageForAllClasses = true)

            // Act
            sut.addCoverageTestClassToDeployRootPackage(it)

            // Assert
            val actualPackageXml = sut.zipBytes.getEntryContent("package.xml")
            val actualCoverageTestClass = sut.zipBytes.getEntryContent(
                "classes/${sut.coverageTestClassName}${Constants.APEX_CLASS_FILE_EXTENSION}")
            val actualCoverageTestClassMetadata = sut.zipBytes.getEntryContent(
                "classes/${sut.coverageTestClassName}${Constants.APEX_CLASS_FILE_EXTENSION}-meta.xml")

            val expectedClassMembers = linkedSetOf(sut.coverageTestClassName)
            expectedClassMembers.addAll(packageXmlClassMembers)
            val expectedPackageXml = generatePackageWithApexClasses(expectedClassMembers)
            val expectedCoverageTestClass = generateTestClass(sut.coverageTestClassName, classFileNames)
            val expectedCoverageTestClassMetadata = generateTestClassMetadata(sut.apiVersion)

            assertThat(the(actualPackageXml), isEquivalentTo(the(expectedPackageXml)))
            assertThat(actualCoverageTestClass, equalTo(expectedCoverageTestClass))
            assertThat(actualCoverageTestClassMetadata, equalTo(expectedCoverageTestClassMetadata))
            assertTrue(sut.runTests!!.contains(sut.coverageTestClassName))
        }
    }

    @DataProvider
    fun packageXmlContainsApexClassTypeForCoverageTestClassData(): Array<Array<out Any?>> = arrayOf(
        arrayOf<Any?>(
            linkedSetOf("*"),
            setOf("ArgumentNullException", "BookTestClass", "Program")),
        arrayOf<Any?>(
            linkedSetOf("ArgumentNullException", "BookTestClass", "Program"),
            setOf("ArgumentNullException", "BookTestClass", "Program")))

    @Test
    fun addCoverageTestClassToDeployRootPackage_ifPackageXmlDidNotContainApexClassType_shouldNotGenerateCoverageTestClassAndNotModifyPackageXml() {
        val packageXml = generatePackageWithoutApexClasses()
        withDeployRoot(packageXml) {
            // Arrange
            val sut = createSystemUnderTest(
                deployRoot = it.absolutePath,
                enforceCoverageForAllClasses = true)

            // Act
            sut.addCoverageTestClassToDeployRootPackage(it)

            // Assert
            val actualPackageXml = sut.zipBytes.getEntryContent("package.xml")
            val actualCoverageTestClass = sut.zipBytes.getEntryContent(
                "classes/${sut.coverageTestClassName}${Constants.APEX_CLASS_FILE_EXTENSION}")
            val actualCoverageTestClassMetadata = sut.zipBytes.getEntryContent(
                "classes/${sut.coverageTestClassName}${Constants.APEX_CLASS_FILE_EXTENSION}-meta.xml")

            assertThat(the(actualPackageXml), isEquivalentTo(the(packageXml)))
            assertEquals(actualCoverageTestClass, null)
            assertEquals(actualCoverageTestClassMetadata, null)
            assertFalse(sut.runTests!!.contains(sut.coverageTestClassName))
        }
    }

    @Test(dataProvider = "improperTestLevelForCoverageTestClassData")
    fun addCoverageTestClassToZipFilePackage_withImproperTestLevel_shouldNotGenerateCoverageTestClass(
        improperTestLevel: String?,
        enforceCoverageForAllClasses: Boolean?) {

        withZipFile {
            // Arrange
            val sut = createSystemUnderTest(
                testLevel = improperTestLevel,
                zipFile = it.absolutePath,
                enforceCoverageForAllClasses = enforceCoverageForAllClasses)

            // Act
            sut.addCoverageTestClassToZipFilePackage(it)

            // Assert
            val expected = ZipUtil.readZip(it)
            assertEquals(sut.zipBytes, expected)
        }
    }

    @Test(dataProvider = "absenceOfZipEntriesForCoverageTestClassData")
    fun addCoverageTestClassToZipFilePackage_withoutImportantZipEntries_shouldNotGenerateCoverageTestClass(
        packageXml: String?,
        classes: Set<String>?) {

        withZipFile(packageXml, classes) {
            // Arrange
            val sut = createSystemUnderTest(
                zipFile = it.absolutePath,
                enforceCoverageForAllClasses = true)

            // Act
            sut.addCoverageTestClassToZipFilePackage(it)

            // Assert
            val expected = ZipUtil.readZip(it)
            assertEquals(sut.zipBytes, expected)
        }
    }

    @DataProvider
    fun absenceOfZipEntriesForCoverageTestClassData(): Array<Array<out Any?>> = arrayOf(
        arrayOf<Any?>(null, setOf("Foobar")),
        arrayOf<Any?>(generatePackage(37.0), null))

    @Test(dataProvider = "packageXmlContainsApexClassTypeForCoverageTestClassData")
    fun addCoverageTestClassToZipFilePackage_ifPackageXmlContainsApexClassType_shouldGenerateCoverageTestClassAndModifyPackageXml(
        packageXmlClassMembers: LinkedHashSet<String>,
        classFileNames: Set<String>) {

        val packageXml = generatePackageWithApexClasses(packageXmlClassMembers)

        withZipFile(packageXml, classFileNames) {
            // Arrange
            val sut = createSystemUnderTest(
                zipFile = it.absolutePath,
                enforceCoverageForAllClasses = true)

            // Act
            sut.addCoverageTestClassToZipFilePackage(it)

            // Assert
            val actualPackageXml = sut.zipBytes.getEntryContent("package.xml")
            val actualCoverageTestClass = sut.zipBytes.getEntryContent(
                "classes/${sut.coverageTestClassName}${Constants.APEX_CLASS_FILE_EXTENSION}")
            val actualCoverageTestClassMetadata = sut.zipBytes.getEntryContent(
                "classes/${sut.coverageTestClassName}${Constants.APEX_CLASS_FILE_EXTENSION}-meta.xml")

            val expectedClassMembers = linkedSetOf(sut.coverageTestClassName)
            expectedClassMembers.addAll(packageXmlClassMembers)
            val expectedPackageXml = generatePackageWithApexClasses(expectedClassMembers)
            val expectedCoverageTestClass = generateTestClass(sut.coverageTestClassName, classFileNames)
            val expectedCoverageTestClassMetadata = generateTestClassMetadata(sut.apiVersion)

            assertThat(the(actualPackageXml), isEquivalentTo(the(expectedPackageXml)))
            assertThat(actualCoverageTestClass, equalTo(expectedCoverageTestClass))
            assertThat(actualCoverageTestClassMetadata, equalTo(expectedCoverageTestClassMetadata))
            assertTrue(sut.runTests!!.contains(sut.coverageTestClassName))
        }
    }

    @Test
    fun addCoverageTestClassToZipFilePackage_ifPackageXmlDidNotContainApexClassType_shouldNotGenerateCoverageTestClassAndNotModifyPackageXml() {
        val packageXml = generatePackageWithoutApexClasses()
        withZipFile(packageXml) {
            // Arrange
            val sut = createSystemUnderTest(
                zipFile = it.absolutePath,
                enforceCoverageForAllClasses = true)

            // Act
            sut.addCoverageTestClassToZipFilePackage(it)

            // Assert
            val actualPackageXml = sut.zipBytes.getEntryContent("package.xml")
            val actualCoverageTestClass = sut.zipBytes.getEntryContent(
                "classes/${sut.coverageTestClassName}${Constants.APEX_CLASS_FILE_EXTENSION}")
            val actualCoverageTestClassMetadata = sut.zipBytes.getEntryContent(
                "classes/${sut.coverageTestClassName}${Constants.APEX_CLASS_FILE_EXTENSION}-meta.xml")

            assertThat(the(actualPackageXml), isEquivalentTo(the(packageXml)))
            assertEquals(actualCoverageTestClass, null)
            assertEquals(actualCoverageTestClassMetadata, null)
            assertFalse(sut.runTests!!.contains(sut.coverageTestClassName))
        }
    }

    @Test(dataProvider = "blankCoverageTestNameData")
    fun removeCoverageTestClassFromOrg_withBlankCoverageTestClassName_shouldNotDeployAnything(
        blankTestName: String) {
        val sut = createSystemUnderTest()
        sut.coverageTestClassName = blankTestName
        val connectionMock = mock(MetadataConnection::class.java)

        sut.removeCoverageTestClassFromOrg(connectionMock)

        verifyZeroInteractions(connectionMock)
    }

    @DataProvider
    fun blankCoverageTestNameData(): Array<Array<out Any?>> = arrayOf(
        arrayOf<Any?>(""),
        arrayOf<Any?>("   "))

    @Test
    fun removeCoverageTestClassFromOrg_withNonBlankCoverageTestClassName_shouldDeployDestructiveChanges() {
        // Arrange
        val sut = createSystemUnderTest()
        sut.coverageTestClassName = generateTestClassName()
        val connectionMock = mock(MetadataConnection::class.java)

        // Act
        sut.removeCoverageTestClassFromOrg(connectionMock)

        // Assert
        val actualBytes = ArgumentCaptor.forClass(ByteArray::class.java)
        val actualOptions = ArgumentCaptor.forClass(DeployOptions::class.java)
        verify(connectionMock).deploy(actualBytes.capture(), actualOptions.capture())

        assertTrue(actualOptions.value.singlePackage)
        assertTrue(actualOptions.value.ignoreWarnings)

        val actualDestructiveChanges = actualBytes.value.getEntryContent("destructiveChanges.xml")
        val actualPackage = actualBytes.value.getEntryContent("package.xml")

        val expectedDestructiveChanges = generateDestructiveChanges(sut.coverageTestClassName, sut.apiVersion)
        val expectedPackage = generatePackage(sut.apiVersion)

        assertEquals(actualDestructiveChanges, expectedDestructiveChanges)
        assertEquals(actualPackage, expectedPackage)
    }

    private fun ByteArray.getEntryContent(name: String): String? {
        ZipInputStream(ByteArrayInputStream(this)).use { zipInput ->
            var entry: ZipEntry?
            do {
                entry = zipInput.nextEntry
                if (entry == null)
                    break
                if (entry.name != name)
                    continue
                val output = ByteArrayOutputStream()
                output.use {
                    val buffer = ByteArray(8192)
                    var length: Int
                    do {
                        length = zipInput.read(buffer, 0, buffer.size)
                        if (length > 0)
                            output.write(buffer, 0, length)
                    } while (length > 0)
                    return String(output.toByteArray())
                }

            } while (entry != null)
        }
        return null
    }

    fun withTestDirectory(test: (File) -> Unit) = withTestDirectory(javaClass.name, test)

    fun withDeployRoot(
        packageXml: String = generateDestructiveChanges("*", 37.0),
        classes: Set<String> = setOf("Foobar"),
        test: (File) -> Unit) = withTestDirectory(javaClass.name) {

        File(it, "package.xml").appendText(packageXml)
        File(it, "classes").mkdir()
        classes.forEach { className ->
            File(it, "classes/$className${Constants.APEX_CLASS_FILE_EXTENSION}")
                .appendText("public with sharing class $it { }")
        }
        test(it)
    }

    fun withZipFile(
        packageXml: String? = generateDestructiveChanges("*", 37.0),
        classes: Set<String>? = setOf("Foobar"),
        test: (File) -> Unit) = withTestDirectory(javaClass.name) {

        val zip = File(it, "src.zip")
        val fileOutput = FileOutputStream(zip)
        ZipOutputStream(fileOutput).use { zipOutput ->
            if (packageXml != null)
                zipOutput.addEntry("package.xml", packageXml)
            if (classes != null) {
                zipOutput.addEntry("classes", "")
                classes.forEach {
                    zipOutput.addEntry(
                        "classes/$it${Constants.APEX_CLASS_FILE_EXTENSION}",
                        "public with sharing class $it { }")
                }
            }
        }
        test(zip)
    }

    fun generatePackageWithApexClasses(classNames: LinkedHashSet<String>) =
        """<?xml version="1.0" encoding="UTF-8"?>
<Package xmlns="http://soap.sforce.com/2006/04/metadata">
    <types>
        <members>${classNames.joinToString(separator = "</members>\n<members>")}</members>
        <name>ApexClass</name>
    </types>
    <version>37.0</version>
</Package>"""

    fun generatePackageWithoutApexClasses() = generatePackage(37.0)

    fun createSystemUnderTest(
        project: Project = createProject(),
        testLevel: String? = TestLevel.RunSpecifiedTests.name,
        deployRoot: String? = null,
        zipFile: String? = null,
        enforceCoverageForAllClasses: Boolean? = false): DeployWithTestReportsTask {
        val sut = DeployWithTestReportsTask()
        sut.project = project
        sut.testLevel = testLevel
        sut.deployRoot = deployRoot
        sut.zipFile = zipFile
        sut.enforceCoverageForAllClasses = enforceCoverageForAllClasses
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