package com.aquivalabs.force.ant

import com.aquivalabs.force.ant.reporters.*
import com.nhaarman.mockito_kotlin.*
import com.salesforce.ant.DeployTask
import com.salesforce.ant.ZipUtil
import com.sforce.soap.metadata.*
import org.apache.tools.ant.Project
import org.apache.tools.ant.types.FileSet
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.testng.annotations.*
import org.testng.Assert.*
import org.xmlunit.matchers.CompareMatcher.isSimilarTo
import java.io.File
import java.nio.file.Files
import java.util.*

class DeployWithTestReportsTaskTestCase {

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

    @Test fun addConfiguredJUnitReport_always_shouldFollowAntNamingConventions() {
        assertThat(
            nestedElementConvention("addConfigured"),
            DeployWithTestReportsTask::addConfiguredJUnitReport.name,
            startsWith("addConfigured"))
    }

    @Test fun addConfiguredCoberturaReport_always_shouldFollowAntNamingConventions() {
        assertThat(
            nestedElementConvention("addConfigured"),
            DeployWithTestReportsTask::addConfiguredCoberturaReport.name,
            startsWith("addConfigured"))
    }

    @Test fun addConfiguredHtmlCoverageReport_always_shouldFollowAntNamingConventions() {
        assertThat(
            nestedElementConvention("addConfigured"),
            DeployWithTestReportsTask::addConfiguredHtmlCoverageReport.name,
            startsWith("addConfigured"))
    }

    @Test fun addConfiguredCoverageFilter_always_shouldFollowAntNamingConventions() {
        assertThat(
            nestedElementConvention("addConfigured"),
            DeployWithTestReportsTask::addConfiguredCoverageFilter.name,
            startsWith("addConfigured"))
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

    @Test fun deployRoot_always_shouldReturnValueFromCorrespondingBaseClassPrivateField() {
        val sut = createSystemUnderTest()
        val expected = "foobar"
        sut.setDeployRoot(expected)
        assertEquals(sut.getDeployRoot(), expected)
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
            assertEquals(sut.zipBytesField, expected)
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
                sut.zipBytesField,
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
            val actualPackageXml = sut.zipBytesField.getEntryContent("package.xml")
            val actualCoverageTestClass = sut.zipBytesField.getEntryContent(
                "classes/${sut.coverageTestClassName}$APEX_CLASS_FILE_EXTENSION")
            val actualCoverageTestClassMetadata = sut.zipBytesField.getEntryContent(
                "classes/${sut.coverageTestClassName}$APEX_CLASS_FILE_EXTENSION$META_FILE_EXTENSION")

            val expectedClassMembers = linkedSetOf(sut.coverageTestClassName)
            expectedClassMembers.addAll(packageXmlClassMembers)
            val expectedPackageXml = generatePackageWithApexClasses(expectedClassMembers)
            val expectedCoverageTestClass = generateTestClass(sut.coverageTestClassName, classFileNames)
            val expectedCoverageTestClassMetadata = generateTestClassMetadata(sut.apiVersion)

            assertThat(actualPackageXml, isSimilarTo(expectedPackageXml).ignoreWhitespace())
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
            val actualPackageXml = sut.zipBytesField.getEntryContent("package.xml")
            val actualCoverageTestClass = sut.zipBytesField.getEntryContent(
                "classes/${sut.coverageTestClassName}$APEX_CLASS_FILE_EXTENSION")
            val actualCoverageTestClassMetadata = sut.zipBytesField.getEntryContent(
                "classes/${sut.coverageTestClassName}$APEX_CLASS_FILE_EXTENSION$META_FILE_EXTENSION")

            assertThat(actualPackageXml, isSimilarTo(packageXml).ignoreWhitespace())
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
            assertEquals(sut.zipBytesField, expected)
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
            assertEquals(sut.zipBytesField, expected)
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
            val actualPackageXml = sut.zipBytesField.getEntryContent("package.xml")
            val actualCoverageTestClass = sut.zipBytesField.getEntryContent(
                "classes/${sut.coverageTestClassName}$APEX_CLASS_FILE_EXTENSION")
            val actualCoverageTestClassMetadata = sut.zipBytesField.getEntryContent(
                "classes/${sut.coverageTestClassName}$APEX_CLASS_FILE_EXTENSION$META_FILE_EXTENSION")

            val expectedClassMembers = linkedSetOf(sut.coverageTestClassName)
            expectedClassMembers.addAll(packageXmlClassMembers)
            val expectedPackageXml = generatePackageWithApexClasses(expectedClassMembers)
            val expectedCoverageTestClass = generateTestClass(sut.coverageTestClassName, classFileNames)
            val expectedCoverageTestClassMetadata = generateTestClassMetadata(sut.apiVersion)

            assertThat(actualPackageXml, isSimilarTo(expectedPackageXml).ignoreWhitespace())
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
            val actualPackageXml = sut.zipBytesField.getEntryContent("package.xml")
            val actualCoverageTestClass = sut.zipBytesField.getEntryContent(
                "classes/${sut.coverageTestClassName}$APEX_CLASS_FILE_EXTENSION")
            val actualCoverageTestClassMetadata = sut.zipBytesField.getEntryContent(
                "classes/${sut.coverageTestClassName}$APEX_CLASS_FILE_EXTENSION$META_FILE_EXTENSION")

            assertThat(actualPackageXml, isSimilarTo(packageXml).ignoreWhitespace())
            assertEquals(actualCoverageTestClass, null)
            assertEquals(actualCoverageTestClassMetadata, null)
            assertFalse(sut.runTests!!.contains(sut.coverageTestClassName))
        }
    }

    @Test(dataProvider = "blankCoverageTestNameData")
    fun removeCoverageTestClassFromOrg_withBlankCoverageTestClassName_shouldNotDeployAnything(
        blankTestName: String) {
        val sut = createMockedSystemUnderTest()
        sut.coverageTestClassName = blankTestName

        sut.removeCoverageTestClassFromOrg()

        verifyZeroInteractions(sut.metadataConnection)
    }

    @DataProvider
    fun blankCoverageTestNameData(): Array<Array<out Any?>> = arrayOf(
        arrayOf<Any?>(""),
        arrayOf<Any?>("   "))

    @Test
    fun removeCoverageTestClassFromOrg_withNonBlankCoverageTestClassName_shouldDeployDestructiveChanges() {
        // Arrange
        val sut = createMockedSystemUnderTest()
        sut.coverageTestClassName = generateTestClassName()

        // Act
        sut.removeCoverageTestClassFromOrg()

        // Assert
        val actualBytes = argumentCaptor<ByteArray>()
        verify(sut.metadataConnection).deploy(
            actualBytes.capture(),
            argThat { singlePackage && ignoreWarnings })

        val actualDestructiveChanges = actualBytes.lastValue.getEntryContent("destructiveChanges.xml")
        val actualPackage = actualBytes.lastValue.getEntryContent("package.xml")

        val expectedDestructiveChanges = generateDestructiveChanges(sut.coverageTestClassName, sut.apiVersion)
        val expectedPackage = generatePackage(sut.apiVersion)

        assertEquals(actualDestructiveChanges, expectedDestructiveChanges)
        assertEquals(actualPackage, expectedPackage)
    }

    @Test fun handleResponse_ifReportDirIsNotNullAndJUnitReport_shouldCreateReportFile() {
        withTestDirectory { testDirectory ->
            // Arrange
            val sut = createSystemUnderTest()
            sut.reportDir = testDirectory
            sut.username = "foo"
            sut.serverURL = "bar"
            sut.apiVersion = 35.0

            val report = JUnitReport(suiteName = "TestSuite")
            sut.addConfiguredJUnitReport(report)

            // Act
            sut.handleResponse(metadataConnection(), asyncResult())

            // Assert
            val actual = testDirectory.listFiles().single { it.name == "TEST-TestSuite.xml"}
            assertTrue(actual.exists(), "Report file wasn't found")
        }
    }

    @Test fun handleResponse_ifReportDirIsNotNullAndCoberturaReport_shouldCreateReportFile() {
        withTestDirectory { testDirectory ->
            // Arrange
            val sut = createSystemUnderTest()
            sut.reportDir = testDirectory

            val report = CoberturaReport(file = "Cobertura.xml")
            sut.addConfiguredCoberturaReport(report)

            // Act
            sut.handleResponse(metadataConnection(), asyncResult())

            // Assert
            val actual = testDirectory.listFiles().single { it.name == report.file }
            assertTrue(actual.exists(), "Report file wasn't found")
        }
    }

    @Test fun handleResponse_ifReportDirIsNotNullAndDeployRootIsNotNullAndHtmlCoverageReport_shouldCreateReportFile() {
        withDeployRoot { testDirectory ->
            // Arrange
            val sut = createSystemUnderTest(deployRoot = testDirectory.absolutePath)
            sut.reportDir = testDirectory

            val report = HtmlCoverageReport(dir = "html-coverage")
            sut.addConfiguredHtmlCoverageReport(report)

            // Act
            sut.handleResponse(metadataConnection(), asyncResult())

            // Assert
            val actual = testDirectory.listFiles().single { it.name == report.dir }
            assertTrue(actual.exists(), "Report file wasn't found")
        }
    }

    @Test fun handleResponse_ifReportDirIsNotNullAndZipFileIsNotNullAndHtmlCoverageReport_shouldCreateReportFile() {
        withZipFile { zipFile ->
            // Arrange
            val sut = createSystemUnderTest(zipFile = zipFile.absolutePath)
            sut.reportDir = zipFile.parentFile

            val report = HtmlCoverageReport(dir = "html-coverage")
            sut.addConfiguredHtmlCoverageReport(report)

            // Act
            sut.handleResponse(metadataConnection(), asyncResult())

            // Assert
            val actual = zipFile.parentFile.listFiles().single { it.name == report.dir }
            assertTrue(actual.exists(), "Report file wasn't found")
        }
    }

    @Test
    fun handleResponse_withNonBlankCoverageTestClassName_shouldRemoveItFromTestResult() {
        withTestDirectory {
            // Arrange
            val numTestsRun = 3
            val coverageTestClassName = generateTestClassName()
            val deployResult = deployResult(
                testResult = runTestsResult(
                    numTestsRun = numTestsRun,
                    successes = arrayOf(
                        runTestSuccess(name = coverageTestClassName),
                        runTestSuccess(name = "foo"),
                        runTestSuccess(name = "bar"))))

            val sut = createMockedSystemUnderTest(metadataConnection = metadataConnection(deployResult))
            sut.reportDir = it
            sut.sourceDir = it
            sut.coverageTestClassName = coverageTestClassName
            sut.addConfiguredJUnitReport(JUnitReport())
            sut.addConfiguredCoberturaReport(CoberturaReport(file = "Cobertura.xml"))
            sut.addConfiguredHtmlCoverageReport(HtmlCoverageReport(dir = "coverage"))
            val teamcityLog = mutableListOf<String>()
            sut.consoleReporters["TeamCity"] = createTeamCityReporter { teamcityLog.add(it) }

            // Act
            sut.handleResponse(sut.metadataConnection, asyncResult())

            // Assert
            Files.walk(it.toPath())
                .filter { Files.isRegularFile(it) }
                .forEach { assertFalse(it.toFile().readText().contains(sut.coverageTestClassName)) }
            assertEquals(teamcityLog.filter { it.contains(sut.coverageTestClassName) }.size, 0)
        }
    }

    @Test
    fun handleResponse_withDefaultCoverageFilter_shouldNotModifyTestResult() {
        withTestDirectory { testDirectory ->
            // Arrange
            val expectedCoverage = arrayOf(codeCoverageResult(name = "foo"))
            val expectedWarnings = arrayOf(codeCoverageWarning(name = "foo"))
            val deployResult = deployResult(
                testResult = runTestsResult(
                    codeCoverage = expectedCoverage,
                    codeCoverageWarnings = expectedWarnings))
            val sut = createMockedSystemUnderTest(metadataConnection = metadataConnection(deployResult))
            sut.reportDir = testDirectory

            val mockConsoleReporter = mock<Reporter<Unit>>()
            val mockFileReporter = mock<Reporter<File>>()
            whenever(mockFileReporter.createReport(any())).thenReturn(testDirectory)

            sut.consoleReporters.put("TestConsoleReporter", mockConsoleReporter)
            sut.fileReporters.put("TestFileReporter", mockFileReporter)

            // Act
            sut.handleResponse(sut.metadataConnection, asyncResult())

            // Assert
            verify(mockConsoleReporter).createReport(argThat {
                Arrays.equals(expectedCoverage, details.runTestResult.codeCoverage)
                    && Arrays.equals(expectedWarnings, details.runTestResult.codeCoverageWarnings)})
            verify(mockFileReporter).createReport(argThat {
                Arrays.equals(expectedCoverage, details.runTestResult.codeCoverage)
                    && Arrays.equals(expectedWarnings, details.runTestResult.codeCoverageWarnings)})
        }
    }

    @Test(dataProvider = "coverageFilterTestData")
    fun handleResponse_always_shouldFilterCoverageDataAccordingToFilter(
        inputCoverageResult: Array<CodeCoverageResult>,
        coverageFilter: CoverageFilter,
        expectedClassNames: Array<String>) = withTestDirectory { testDirectory ->
        // Arrange
        val deployResult = deployResult(
            testResult = runTestsResult(
                codeCoverage = inputCoverageResult,
                codeCoverageWarnings = inputCoverageResult
                    .map { codeCoverageWarning(name = it.name, namespace = it.namespace) }
                    .toTypedArray()))
        val sut = createMockedSystemUnderTest(metadataConnection = metadataConnection(deployResult))
        sut.reportDir = testDirectory
        sut.addConfiguredCoverageFilter(coverageFilter)

        val mockConsoleReporter = mock<Reporter<Unit>>()
        val mockFileReporter = mock<Reporter<File>>()
        whenever(mockFileReporter.createReport(any())).thenReturn(testDirectory)

        sut.consoleReporters.put("TestConsoleReporter", mockConsoleReporter)
        sut.fileReporters.put("TestFileReporter", mockFileReporter)

        // Act
        sut.handleResponse(sut.metadataConnection, asyncResult())

        // Assert
        verify(mockConsoleReporter).createReport(argThat {
            Arrays.equals(expectedClassNames, coverageClassNames())
                && Arrays.equals(expectedClassNames, coverageWarningsClassNames())})
        verify(mockFileReporter).createReport(argThat {
            Arrays.equals(expectedClassNames, coverageClassNames())
                && Arrays.equals(expectedClassNames, coverageWarningsClassNames())})
    }

    @Test
    fun handleResponse_withNullNamesInCodeCoverage_shouldNotThrow() = withTestDirectory { testDirectory ->
        // Arrange
        val deployResult = deployResult(
            testResult = runTestsResult(
                codeCoverage = arrayOf(codeCoverageResult(name = null)),
                codeCoverageWarnings = arrayOf(codeCoverageWarning(name = null))))
        val sut = createMockedSystemUnderTest(metadataConnection = metadataConnection(deployResult))
        sut.reportDir = testDirectory

        // Act & Assert
        sut.handleResponse(sut.metadataConnection, asyncResult())
    }

    @DataProvider
    fun coverageFilterTestData(): Array<Array<Any>> {
        return arrayOf(
            arrayOf(
                arrayOf(codeCoverageResult(name = "foo")),
                CoverageFilter(excludes = "foo"),
                arrayOf<String>()),
            arrayOf(
                arrayOf(codeCoverageResult(name = "foo"), codeCoverageResult(name = "bar")),
                CoverageFilter(excludes = "foo"),
                arrayOf("bar")),
            arrayOf(
                arrayOf(
                    codeCoverageResult(name = "foo"),
                    codeCoverageResult(name = "foobar"),
                    codeCoverageResult(name = "bar")),
                CoverageFilter(excludes = "fo*"),
                arrayOf("bar")),
            arrayOf(
                arrayOf(
                    codeCoverageResult(name = "foo"),
                    codeCoverageResult(name = "foobar"),
                    codeCoverageResult(name = "bar")),
                CoverageFilter(excludes = "fo*, fo*, fo*"),
                arrayOf("bar")),
            arrayOf(
                arrayOf(
                    codeCoverageResult(name = "foo"),
                    codeCoverageResult(name = "foobar"),
                    codeCoverageResult(name = "bar")),
                CoverageFilter(excludes = "*oo*"),
                arrayOf("bar")),
            arrayOf(
                arrayOf(
                    codeCoverageResult(name = "foo"),
                    codeCoverageResult(name = "bar")),
                CoverageFilter(excludes = ""),
                arrayOf("foo", "bar")),
            arrayOf(
                arrayOf(
                    codeCoverageResult(name = "foo"),
                    codeCoverageResult(name = "foobar"),
                    codeCoverageResult(name = "bar")),
                CoverageFilter(excludes = "foo,bar"),
                arrayOf("foobar")),
            arrayOf(
                arrayOf(
                    codeCoverageResult(name = "foo"),
                    codeCoverageResult(name = "foobar"),
                    codeCoverageResult(name = "bar"),
                    codeCoverageResult(name = "baz")),
                CoverageFilter(excludes = "foo*,*az"),
                arrayOf("bar")),
            arrayOf(
                arrayOf(
                    codeCoverageResult(name = "foo"),
                    codeCoverageResult(name = "foobar"),
                    codeCoverageResult(name = "bar"),
                    codeCoverageResult(name = "baz")),
                CoverageFilter(excludes = "foo*, *az"),
                arrayOf("bar")),
            arrayOf(
                arrayOf(
                    codeCoverageResult(name = "foo"),
                    codeCoverageResult(name = "foobar"),
                    codeCoverageResult(name = "bar"),
                    codeCoverageResult(name = "baz")),
                CoverageFilter(excludes = "foo*, *az,bar"),
                arrayOf<String>()),
            arrayOf(
                arrayOf(
                    codeCoverageResult(name = "foo"),
                    codeCoverageResult(name = "foobar"),
                    codeCoverageResult(name = "bar", namespace = "a"),
                    codeCoverageResult(name = "baz")),
                CoverageFilter(excludes = "foo*", excludeNamespaces = "a"),
                arrayOf("baz")),
            arrayOf(
                arrayOf(
                    codeCoverageResult(name = "foo", namespace = "a"),
                    codeCoverageResult(name = "foobar", namespace = "b"),
                    codeCoverageResult(name = "bar"),
                    codeCoverageResult(name = "baz", namespace = "c")),
                CoverageFilter(excludeNamespaces = "a,b, c"),
                arrayOf("bar")),
            arrayOf(
                arrayOf(
                    codeCoverageResult(name = "foo", namespace = "a"),
                    codeCoverageResult(name = "foobar", namespace = "b"),
                    codeCoverageResult(name = "bar"),
                    codeCoverageResult(name = "baz", namespace = "c")),
                CoverageFilter(excludeNamespaces = "a"),
                arrayOf("b.foobar", "bar", "c.baz")),
            arrayOf(
                arrayOf(
                    codeCoverageResult(name = "foo", namespace = "a"),
                    codeCoverageResult(name = "foobar", namespace = "b"),
                    codeCoverageResult(name = "bar"),
                    codeCoverageResult(name = "baz", namespace = "c")),
                CoverageFilter(excludeNamespaces = "a, a, a,a,a"),
                arrayOf("b.foobar", "bar", "c.baz")))
    }

    private fun DeployResult.coverageClassNames() =
        details.runTestResult.codeCoverage.map { it.qualifiedName }.toTypedArray()
    private fun DeployResult.coverageWarningsClassNames() =
        details.runTestResult.codeCoverageWarnings.map { it.qualifiedName }.toTypedArray()

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
        project: Project = project(),
        testLevel: String? = TestLevel.RunSpecifiedTests.name,
        deployRoot: String? = null,
        zipFile: String? = null,
        enforceCoverageForAllClasses: Boolean? = false): DeployWithTestReportsTask {

        val sut = DeployWithTestReportsTask()
        sut.project = project
        sut.testLevel = testLevel
        sut.setDeployRoot(deployRoot)
        sut.zipFile = zipFile
        sut.enforceCoverageForAllClasses = enforceCoverageForAllClasses
        return sut
    }

    fun createMockedSystemUnderTest(
        metadataConnection: MetadataConnection = metadataConnection()): DeployWithTestReportsTask {

        val sut = spy(createSystemUnderTest())
        doReturn(metadataConnection).whenever(sut).metadataConnection
        return sut
    }

    fun createRunTestElement(text: String = ""): DeployTask.CodeNameElement {
        val runTest = DeployTask.CodeNameElement()
        runTest.addText(text)
        return runTest
    }

    fun createTestClassesFileSet(directory: File, fileNames: Iterable<String>): FileSet =
        fileSet(
            directory,
            fileNames.map { it + APEX_CLASS_FILE_EXTENSION })

    fun createTeamCityReporter(log: (String) -> Unit = ::println): TeamCityReporter {
        val env = hashMapOf("TEAMCITY_PROJECT_NAME" to "foo")
        return TeamCityReporter({ env[it] }, log)
    }
}