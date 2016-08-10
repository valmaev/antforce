package com.aquivalabs.force.ant

import com.salesforce.ant.DeployTask
import com.salesforce.ant.ZipUtil
import com.sforce.soap.metadata.DeployOptions
import com.sforce.soap.metadata.MetadataConnection
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

internal fun generateTestClassName() =
    "antforce${UUID.randomUUID().toString().replace("-", "")}"

internal fun generateTestClassName(existingClasses: Set<String>): String {
    var testClassName: String
    do testClassName = generateTestClassName()
    while (existingClasses.contains(testClassName))
    return testClassName
}

internal fun generateTestClass(testClassName: String, classesToTouch: Iterable<String>) =
    """@IsTest
private class $testClassName {
    @IsTest
    private static void touchEveryClassFromDeploymentPackage() {
        try {
            Set<String> classNames = new Set<String> {
                ${classesToTouch.joinToString(transform = { it -> "'$it'" })}
            };
            for (String className : classNames)
                Type.forName(className);
        } catch(Exception ex) {
            // try-catch block is only for guarantee that this test always passes
            System.debug(ex);
        }
    }
}"""

internal fun generateTestClassMetadata(apiVersion: Double) =
    """<?xml version="1.0" encoding="UTF-8"?>
<ApexClass xmlns="http://soap.sforce.com/2006/04/metadata">
    <apiVersion>$apiVersion</apiVersion>
</ApexClass>"""

internal fun generateDestructiveChanges(className: String, apiVersion: Double) =
    """<?xml version="1.0" encoding="UTF-8"?>
<Package xmlns="http://soap.sforce.com/2006/04/metadata">
    <types>
        <members>$className</members>
        <name>ApexClass</name>
    </types>
    <version>$apiVersion</version>
</Package>"""

internal fun generatePackage(apiVersion: Double) =
    """<?xml version="1.0" encoding="UTF-8"?>
<Package xmlns="http://soap.sforce.com/2006/04/metadata">
    <version>$apiVersion</version>
</Package>"""

internal fun String.toCodeNameElement(): DeployTask.CodeNameElement {
    val result = DeployTask.CodeNameElement()
    result.addText(this)
    return result
}

internal fun ZipOutputStream.addEntry(name: String, content: String) {
    val entry = ZipEntry(name)
    entry.size = content.length.toLong()
    putNextEntry(entry)
    write(content.toByteArray())
    closeEntry()
}

internal fun ZipFile.containsEntry(name: String) = getEntry(name) != null

internal fun addClassToPackageXml(packageXmlContent: String, className: String): String {
    packageXmlContent.byteInputStream().use {
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.parse(packageXmlContent.byteInputStream())
        val xpath = XPathFactory.newInstance().newXPath()
        val expression = xpath.compile("/Package/types/name[text()='ApexClass']")
        val nodes = expression.evaluate(doc, XPathConstants.NODESET) as NodeList
        if (nodes.length == 0)
            return packageXmlContent

        val newTestClassNode = doc.createElement("members")
        newTestClassNode.textContent = className

        val parentTypesNode = nodes.item(0).parentNode
        parentTypesNode.insertBefore(newTestClassNode, parentTypesNode.firstChild)

        return doc.saveToString()
    }
}

internal fun Document.saveToString(): String {
    val stringWriter = StringWriter()
    val transformer = TransformerFactory.newInstance().newTransformer()
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
    transformer.setOutputProperty(OutputKeys.METHOD, "xml")
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")

    transformer.transform(DOMSource(this), StreamResult(stringWriter))
    return stringWriter.toString()
}

fun DeployWithTestReportsTask.addCoverageTestClassToDeployRootPackage(deployDir: File) {
    val classesDir = File(deployDir, "classes")
    val packageXml = File(deployDir, "package.xml")
    if (!classesDir.exists() || !packageXml.exists())
        return setZipBytesField(ZipUtil.zipRoot(deployDir))

    val byteArrayStream = ByteArrayOutputStream()
    ZipOutputStream(byteArrayStream).use { output ->
        val topLevelFiles = deployDir.listFiles(FileFilter { it.name != "package.xml" })

        if (topLevelFiles == null || !topLevelFiles.any())
            throw IOException("No files found in ${deployDir.path}")

        ZipUtil.zipFiles("", topLevelFiles, output)

        val classes = classesDir
            .listFiles { file, name -> name.endsWith(Constants.APEX_CLASS_FILE_EXTENSION) }
            .map { it.nameWithoutExtension }
            .toSet()

        coverageTestClassName = generateTestClassName(existingClasses = classes)

        addRunTest(coverageTestClassName.toCodeNameElement())

        output.addEntry(
            "package.xml",
            addClassToPackageXml(packageXml.readText(), coverageTestClassName))
        output.addEntry(
            "classes/$coverageTestClassName${Constants.APEX_CLASS_FILE_EXTENSION}",
            generateTestClass(coverageTestClassName, classes))
        output.addEntry(
            "classes/$coverageTestClassName${Constants.APEX_CLASS_FILE_EXTENSION}-meta.xml",
            generateTestClassMetadata(apiVersion))
    }
    setZipBytesField(byteArrayStream.toByteArray())
}

fun DeployWithTestReportsTask.addCoverageTestClassToZipFilePackage(zipFile: File) {
    val zip = ZipFile(zipFile)
    if (!zip.containsEntry("package.xml") || !zip.containsEntry("classes"))
        return setZipBytesField(ZipUtil.readZip(zipFile))

    val zipEntries = zip.entries()

    val byteArrayStream = ByteArrayOutputStream(8192)
    ZipOutputStream(byteArrayStream).use { output ->
        val classes = hashSetOf<String>()
        var packageXmlContent: String = ""

        while (zipEntries.hasMoreElements()) {
            val entry = zipEntries.nextElement()
            if (entry.name == "package.xml") {
                zip.getInputStream(entry).use {
                    packageXmlContent = String(it.readBytes())
                }
                continue
            }

            if (entry.name.startsWith("classes/") && (entry.name.endsWith(Constants.APEX_CLASS_FILE_EXTENSION)))
                classes.add(entry.name.removePrefix("classes/").removeSuffix(Constants.APEX_CLASS_FILE_EXTENSION))

            val newEntry = ZipEntry(entry.name)
            output.putNextEntry(newEntry)
            zip.getInputStream(entry).use { input ->
                while (input.available() > 0) {
                    output.write(input.readBytes())
                }
            }
        }

        coverageTestClassName = generateTestClassName(existingClasses = classes)
        addRunTest(coverageTestClassName.toCodeNameElement())

        output.addEntry(
            "package.xml",
            addClassToPackageXml(packageXmlContent, coverageTestClassName))
        output.addEntry(
            "classes/$coverageTestClassName${Constants.APEX_CLASS_FILE_EXTENSION}",
            generateTestClass(coverageTestClassName, classes))
        output.addEntry(
            "classes/$coverageTestClassName${Constants.APEX_CLASS_FILE_EXTENSION}-meta.xml",
            generateTestClassMetadata(apiVersion))
    }
    setZipBytesField(byteArrayStream.toByteArray())
}

fun DeployWithTestReportsTask.removeCoverageTestClassFromOrg(metadataConnection: MetadataConnection) {
    if (coverageTestClassName.isNullOrEmpty())
        return

    val byteArrayStream = ByteArrayOutputStream()
    ZipOutputStream(byteArrayStream).use {
        it.addEntry(
            "destructiveChanges.xml",
            generateDestructiveChanges(coverageTestClassName, apiVersion))
        it.addEntry(
            "package.xml",
            generatePackage(apiVersion))
    }

    val deployOptions = DeployOptions()
    deployOptions.singlePackage = true
    metadataConnection.deploy(byteArrayStream.toByteArray(), deployOptions)
}