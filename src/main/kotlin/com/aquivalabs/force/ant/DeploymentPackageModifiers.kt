package com.aquivalabs.force.ant

import com.salesforce.ant.DeployTask
import com.salesforce.ant.ZipUtil
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
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

internal fun generateTestClass(testClassName: String, classesToTouch: Iterable<String>) =
    """@IsTest
private class $testClassName {
    @IsTest
    private static void touchEveryClassFromDeploymentPackage() {
        try {
            Set<String> classNames = new Set<String> { ${classesToTouch.joinToString()} };
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


data class ApexClass(val classFile: File, val metadataFile: File, val apiVersion: Double)

fun ApexClass.exists() = classFile.exists() && metadataFile.exists()
fun ApexClass.deleteOnExit() {
    classFile.deleteOnExit()
    metadataFile.deleteOnExit()
}

fun ApexClass.toCodeNameElement(): DeployTask.CodeNameElement {
    val result = DeployTask.CodeNameElement()
    result.addText(classFile.nameWithoutExtension)
    return result
}

fun ApexClass.destructiveChangesPackage(): ByteArray {
    val byteArrayStream = ByteArrayOutputStream()
    ZipOutputStream(byteArrayStream).use {
        it.addEntry(
            "destructiveChanges.xml",
            generateDestructiveChanges(classFile.nameWithoutExtension, apiVersion))
        it.addEntry(
            "package.xml",
            generatePackage(apiVersion))
    }
    return byteArrayStream.toByteArray()
}

fun ZipOutputStream.addEntry(name: String, content: String) {
    val entry = ZipEntry(name)
    entry.size = content.length.toLong()
    putNextEntry(entry)
    write(content.toByteArray())
    closeEntry()
}

fun Document.saveToString(): String {
    val stringWriter = StringWriter()
    val transformer = TransformerFactory.newInstance().newTransformer()
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
    transformer.setOutputProperty(OutputKeys.METHOD, "xml")
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")

    transformer.transform(DOMSource(this), StreamResult(stringWriter))
    return stringWriter.toString()
}

fun File.zip(packageXmlContent: String): ByteArray {
    val byteArrayStream = ByteArrayOutputStream()
    ZipOutputStream(byteArrayStream).use {
        val topLevelFiles = listFiles(FileFilter { it.name != "package.xml" })

        if (topLevelFiles == null || !topLevelFiles.any())
            throw IOException("No files found in $path")

        ZipUtil.zipFiles("", topLevelFiles, it)
        it.addEntry("package.xml", packageXmlContent)
    }
    return byteArrayStream.toByteArray()
}

class DeployRootPackageModifier(val deployDir: File, val apiVersion: Double) {

    fun generateTestClassThatTouchesEveryClassFromDeploymentPackage(): ApexClass? {
        val classesDir = File(deployDir, "classes")
        if (!classesDir.exists())
            return null

        var testClassName: String
        var apexTestClass: ApexClass

        do {
            testClassName = generateTestClassName()
            apexTestClass = ApexClass(
                classFile = File(classesDir, "$testClassName${Constants.APEX_CLASS_FILE_EXTENSION}"),
                metadataFile = File(classesDir, "$testClassName${Constants.APEX_CLASS_FILE_EXTENSION}-meta.xml"),
                apiVersion = apiVersion)
        } while (apexTestClass.exists())

        apexTestClass.deleteOnExit()

        val classes: List<String> = classesDir.listFiles { file, name ->
            name.endsWith(Constants.APEX_CLASS_FILE_EXTENSION)
        }.map { "'${it.nameWithoutExtension}'" }

        apexTestClass.classFile.appendText(generateTestClass(testClassName, classes))
        apexTestClass.metadataFile.appendText(generateTestClassMetadata(apiVersion))

        return apexTestClass
    }

    fun modifyPackage(apexClass: ApexClass?): ByteArray {
        if (apexClass == null)
            return ZipUtil.zipRoot(deployDir)

        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.parse(File(deployDir, "package.xml"))
        val xpath = XPathFactory.newInstance().newXPath()
        val expression = xpath.compile("/Package/types/name[text()='ApexClass']")
        val nodes = expression.evaluate(doc, XPathConstants.NODESET) as NodeList
        if (nodes.length == 0)
            return ZipUtil.zipRoot(deployDir)

        val newTestClassNode = doc.createElement("members")
        newTestClassNode.textContent = apexClass.classFile.nameWithoutExtension

        val parentTypesNode = nodes.item(0).parentNode
        parentTypesNode.insertBefore(newTestClassNode, parentTypesNode.firstChild)

        return deployDir.zip(doc.saveToString())
    }
}