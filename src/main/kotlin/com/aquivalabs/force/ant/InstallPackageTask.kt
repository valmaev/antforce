package com.aquivalabs.force.ant

import com.salesforce.ant.SFDCAntTask
import org.apache.tools.ant.BuildException
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class PackageInstallMode { INSTALL, UNINSTALL }
data class Package(
    var namespace: String = "",
    var version: String = "",
    var password: String = "",
    var mode: String = PackageInstallMode.INSTALL.name)

val Package.installMode: PackageInstallMode
    get() = PackageInstallMode.valueOf(mode.toUpperCase())
val Package.metadata: String
    get() = """<?xml version="1.0" encoding="UTF-8"?>
<InstalledPackage xmlns="http://soap.sforce.com/2006/04/metadata">
    <versionNumber>$version</versionNumber>
    <password>$password</password>
</InstalledPackage>
"""
val Package.metadataPath: String
    get() = "installedPackages/$namespace.installedPackage"

open class InstallPackageTask : SFDCAntTask() {
    private val packages = hashMapOf<String, Package>()

    var maxPoll: Int = 200
    var pollWaitMillis: Int = 10000

    fun addConfiguredPackage(`package`: Package) {
        if (`package`.namespace.isNullOrBlank())
            throw BuildException("You should provide namespace for package")
        if (`package`.version.isNullOrBlank())
            throw BuildException("You should provide version for package")
        if (packages.containsKey(`package`.namespace))
            throw BuildException("You already added package with namespace '${`package`.namespace}")
        packages[`package`.namespace] = `package`
    }

    override fun execute() {
        val packagesByMode = packages.values.groupBy(Package::installMode)

        val packagesToInstall = packagesByMode[PackageInstallMode.INSTALL].orEmpty()
        if (packagesToInstall.isNotEmpty()) {
            val toInstallBytes = ByteArrayOutputStream()
            ZipOutputStream(toInstallBytes).use { zip ->
                zip.addEntry("package.xml", packageXmlWithInstalledPackage("*"))
                zip.putNextEntry(ZipEntry("installedPackages"))
                packagesToInstall.forEach {
                    zip.addEntry(it.metadataPath, it.metadata)
                }
            }

            log("Installing packages:\n")
            packagesToInstall.forEach { log("\t${it.namespace} v${it.version}\n") }
            waitFor(maxPoll, pollWaitMillis) {
                metadataConnection.deploy(toInstallBytes.toByteArray(), deployOptions(singlePackage = true))
            }
        }

        val packagesToUninstall = packagesByMode[PackageInstallMode.UNINSTALL].orEmpty()
        if (packagesToUninstall.isNotEmpty()) {
            val toUninstallBytes = ByteArrayOutputStream()
            ZipOutputStream(toUninstallBytes).use { zip ->
                zip.addEntry("package.xml", generatePackage(apiVersion))
                zip.addEntry(
                    "destructiveChanges.xml",
                    packageXmlWithInstalledPackage(
                        packagesToUninstall.joinToString(
                            separator = "</members><members>",
                            transform = Package::namespace)))
                zip.putNextEntry(ZipEntry("installedPackages"))
                packagesToUninstall.forEach {
                    zip.addEntry(it.metadataPath, it.metadata)
                }
            }

            log("Uninstalling packages:\n")
            packagesToUninstall.forEach { log("\t${it.namespace} v${it.version}\n") }
            waitFor(maxPoll, pollWaitMillis) {
                metadataConnection.deploy(toUninstallBytes.toByteArray(), deployOptions(singlePackage = true))
            }
        }
    }

    internal fun packageXmlWithInstalledPackage(members: String) =
        """<?xml version="1.0" encoding="UTF-8"?>
<Package xmlns="http://soap.sforce.com/2006/04/metadata">
    <types>
        <members>$members</members>
        <name>InstalledPackage</name>
    </types>
    <version>$apiVersion</version>
</Package>"""
}