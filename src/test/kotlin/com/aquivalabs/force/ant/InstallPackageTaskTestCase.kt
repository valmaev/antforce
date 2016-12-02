package com.aquivalabs.force.ant

import com.nhaarman.mockito_kotlin.*
import com.sforce.soap.metadata.*
import org.apache.tools.ant.BuildException
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.testng.Assert.*
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import org.xmlunit.matchers.HasXPathMatcher.hasXPath


class InstallPackageTaskTestCase {
    private val metadataApiNamespace = mapOf("x" to "http://soap.sforce.com/2006/04/metadata")

    @Test fun maxPoll_byDefault_shouldReturnSameValueAsInDeployTask() {
        val sut = InstallPackageTask()
        val deployTask = DeployWithTestReportsTask()
        assertEquals(sut.maxPoll, deployTask.maxPoll)
    }

    @Test fun pollWaitMillis_byDefault_shouldReturnSameValueAsInDeployTask() {
        val sut = InstallPackageTask()
        val deployTask = DeployWithTestReportsTask()
        assertEquals(sut.pollWaitMillis, deployTask.pollWaitMillis)
    }

    @Test fun addConfiguredPackage_always_shouldFollowAntNamingConventions() {
        assertThat(
            nestedElementConvention("addConfigured"),
            InstallPackageTask::addConfiguredPackage.name,
            startsWith("addConfigured"))
    }

    @Test(dataProvider = "blankStrings")
    fun addConfiguredPackage_ifNamespaceIsBlank_shouldThrowBuildException(blankNamespace: String) {
        val sut = createSystemUnderTest()

        try {
            sut.addConfiguredPackage(`package`(namespace = blankNamespace))
        } catch(actual: BuildException) {
            assertThat(actual.message, containsString("namespace"))
            return
        }

        fail("BuildException isn't thrown")
    }

    @Test(dataProvider = "blankStrings")
    fun addConfiguredPackage_ifVersionIsBlank_shouldThrowBuildException(blankVersion: String) {
        val sut = createSystemUnderTest()

        try {
            sut.addConfiguredPackage(`package`(version = blankVersion))
        } catch(actual: BuildException) {
            assertThat(actual.message, containsString("version"))
            return
        }

        fail("BuildException isn't thrown")
    }

    @DataProvider
    fun blankStrings(): Array<Array<Any>> = arrayOf(arrayOf<Any>(""), arrayOf<Any>("   "))

    @Test fun addConfiguredPackage_withAlreadyAddedPackage_shouldThrowBuildException() {
        val sut = createMockedSystemUnderTest()
        val input = `package`(namespace = "foobar")
        sut.addConfiguredPackage(input)

        try {
            sut.addConfiguredPackage(input)
        } catch(actual: BuildException) {
            assertThat(actual.message, containsString(input.namespace))
            assertThat(actual.message, containsString("already added"))
            return
        }

        fail("BuildException isn't thrown")
    }

    @Test fun execute_always_shouldDeploySinglePackagesOnly() {
        val sut = createMockedSystemUnderTest()
        sut.addConfiguredPackage(`package`(namespace = "foo", mode = PackageInstallMode.INSTALL.name))
        sut.addConfiguredPackage(`package`(namespace = "bar", mode = PackageInstallMode.UNINSTALL.name))

        sut.execute()

        val actualDeployOptions = argumentCaptor<DeployOptions>()
        verify(sut.metadataConnection, atLeast(1)).deploy(any(), actualDeployOptions.capture())
        actualDeployOptions.allValues.forEach {
            assertTrue(it.singlePackage, "Attempt to deploy non-single package")
        }
    }

    @Test(dataProvider = "packagesTestData")
    fun execute_withProperPackage_shouldProperlyGenerateMetadata(vararg packages: Package) {
        val sut = createMockedSystemUnderTest()
        packages.forEach { sut.addConfiguredPackage(it) }

        sut.execute()

        val actualBytes = argumentCaptor<ByteArray>()
        verify(sut.metadataConnection, times(1)).deploy(actualBytes.capture(), any())
        packages.forEach { expected ->
            val actual = actualBytes.firstValue.getEntryContent(expected.metadataPath)
            assertThat(actual, equalTo(expected.metadata))
        }
    }

    @Test(dataProvider = "packagesToInstallTestData")
    fun execute_withPackagesToInstall_shouldGenerateWildcardPackageXml(vararg packages: Package) {
        val sut = createMockedSystemUnderTest()
        packages.forEach { sut.addConfiguredPackage(it) }

        sut.execute()

        val actualBytes = argumentCaptor<ByteArray>()
        verify(sut.metadataConnection, times(1)).deploy(actualBytes.capture(), any())
        val actual = actualBytes.firstValue.getEntryContent("package.xml")
        assertThat(
            actual,
            hasXPath("/x:Package/x:types/x:members[text()=\"*\"]")
                .withNamespaceContext(metadataApiNamespace))
        assertThat(
            actual,
            hasXPath("/x:Package/x:types/x:name[text()=\"InstalledPackage\"]")
                .withNamespaceContext(metadataApiNamespace))
        assertThat(
            actual,
            hasXPath("/x:Package/x:version[text()=\"${sut.apiVersion}\"]")
                .withNamespaceContext(metadataApiNamespace))
    }

    @Test(dataProvider = "packagesToUninstallTestData")
    fun execute_withPackagesToUninstall_shouldGenerateDummyPackageXml(vararg packages: Package) {
        val sut = createMockedSystemUnderTest()
        packages.forEach { sut.addConfiguredPackage(it) }

        sut.execute()

        val actualBytes = argumentCaptor<ByteArray>()
        verify(sut.metadataConnection, times(1)).deploy(actualBytes.capture(), any())
        val actual = actualBytes.firstValue.getEntryContent("package.xml")
        assertThat(actual, equalTo(generatePackage(sut.apiVersion)))
    }

    @Test(dataProvider = "packagesToUninstallTestData")
    fun execute_withPackagesToUninstall_shouldGenerateDestructiveChangesXml(vararg packages: Package) {
        val sut = createMockedSystemUnderTest()
        packages.forEach { sut.addConfiguredPackage(it) }

        sut.execute()

        val actualBytes = argumentCaptor<ByteArray>()
        verify(sut.metadataConnection, times(1)).deploy(actualBytes.capture(), any())
        val actual = actualBytes.firstValue.getEntryContent("destructiveChanges.xml")

        packages.forEach { expected ->
            assertThat(
                actual,
                hasXPath("/x:Package/x:types/x:members[text()=\"${expected.namespace}\"]")
                    .withNamespaceContext(metadataApiNamespace))
        }
        assertThat(
            actual,
            hasXPath("/x:Package/x:types/x:name[text()=\"InstalledPackage\"]")
                .withNamespaceContext(metadataApiNamespace))
        assertThat(
            actual,
            hasXPath("/x:Package/x:version[text()=\"${sut.apiVersion}\"]")
                .withNamespaceContext(metadataApiNamespace))
    }

    @DataProvider
    fun packagesTestData(): Array<Array<Any>> = packagesToInstallTestData() + packagesToUninstallTestData()

    @DataProvider
    fun packagesToInstallTestData(): Array<Array<Any>> = arrayOf(
        arrayOf<Any>(`package`(mode = PackageInstallMode.INSTALL.name)),
        arrayOf<Any>(
            `package`(namespace = "foo", mode = PackageInstallMode.INSTALL.name),
            `package`(namespace = "bar", mode = PackageInstallMode.INSTALL.name)))

    @DataProvider
    fun packagesToUninstallTestData(): Array<Array<Any>> = arrayOf(
        arrayOf<Any>(`package`(mode = PackageInstallMode.UNINSTALL.name)),
        arrayOf<Any>(
            `package`(namespace = "foo", mode = PackageInstallMode.UNINSTALL.name),
            `package`(namespace = "bar", mode = PackageInstallMode.UNINSTALL.name)))

    fun createSystemUnderTest() = InstallPackageTask()
    fun createMockedSystemUnderTest(
        metadataConnection: MetadataConnection = metadataConnection()): InstallPackageTask {

        val sut = spy<InstallPackageTask>()
        doReturn(metadataConnection).whenever(sut).metadataConnection
        return sut
    }

    fun `package`(
        namespace: String = "foo",
        version: String = "1.0",
        password: String = "",
        mode: String = PackageInstallMode.INSTALL.name) =
        Package(namespace = namespace, version = version, password = password, mode = mode)
}